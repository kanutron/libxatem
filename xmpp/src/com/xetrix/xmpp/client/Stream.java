package com.xetrix.xmpp.client;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.stanza.Stanza;
import com.xetrix.xmpp.stanza.IQ;
import com.xetrix.xmpp.payload.Bind;
import com.xetrix.xmpp.payload.Session;

public class Stream {
  Client client;
  StreamFeatures features = new StreamFeatures();

  // Life cycle
  private String connectionID = "";
  private Integer stanzaId = 0;

  // XML Parser
  private XmlPullParser parser;

  // Stream Threads
  private Thread readThread;
  private Thread writeThread;
  private final BlockingQueue<String> stanzaOutQueue = new ArrayBlockingQueue<String>(500);

  // Constructors
  public Stream(Client c) {
    client = c;
  }

  // Package methods
  void pushStanza(String s) {
    try {
      stanzaOutQueue.put(s);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      client.onStreamError(new XMPPError(XMPPError.Type.CANCEL,
        "service-unavailable", e.getMessage()));
    }
  }

  void pushStanza(Stanza s) {
    if (s.getId() == null) {
      s.setId(getNextStanzaId().toString());
    }
    pushStanza(s.toXML());
  }

  String getConnectionID() {
    return connectionID;
  }

  void setConnectionID(String cid) {
    connectionID = cid;
  }

  Integer getNextStanzaId() {
    return ++stanzaId; // TODO: Should be more complex than a serial number
  }

  void initStream() {
    StreamFeatures features = new StreamFeatures();
    if (client.socket.isConnected()) {
      connectionID = "";
      stanzaId = 0;

      initParser();
      initWriter();

      pushStanza("<stream:stream to=\"" + client.getService() +
        "\" xmlns:stream=\"http://etherx.jabber.org/streams\" " +
        "xmlns=\"jabber:client\" version=\"1.0\">");
    }
  }

  void finishStream() {
    readThread.interrupt();
    readThread = new Thread();
    writeThread.interrupt();
    writeThread = new Thread();

    if (client.socket.isConnected()) {
      try {
        client.socket.writer.write("</stream:stream>");
        client.socket.writer.flush();
      } catch (Exception e) {
      }
    }
  }

  void startTLS() {
    if (client.socket.enableTLS()) {
      initStream();
    } else {
      client.onStreamError(new XMPPError(XMPPError.Type.CANCEL, "bad-request",
        "Start TLS failed."));
    }
  }

  void startCompression() {
    if (client.socket.enableCompression()) {
      initStream();
    } else {
      client.onStreamError(new XMPPError(XMPPError.Type.CANCEL, "bad-request",
        "Start compression failed."));
    }
  }

  // Private methods
  private void initParser() {
    try {
      parser = new MXParser();
      parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
      parser.setInput(client.socket.reader);
    } catch (Exception e) {
      client.onStreamError(new XMPPError(XMPPError.Type.CANCEL,
        "undefined-condition", "Parser does not initialize: " + e.getMessage()));
    }

    readThread = new Thread() {
      public void run() {
        parseStanzas(this);
      }
    };
    readThread.setName("ThreadXMLParser");
    readThread.setDaemon(true);
    readThread.start();
  }

  private void initWriter() {
    stanzaOutQueue.clear();
    writeThread = new Thread() {
      public void run() {
        procOutQueue(this);
      }
    };
    writeThread.setName("ThreadStanzaOutQueue");
    writeThread.setDaemon(true);
    writeThread.start();
  }

  private void procOutQueue(Thread thread) {
    try {
      String stanza;
      while ((stanza = stanzaOutQueue.take()) != "") {
        client.socket.writer.write(stanza);
        client.socket.writer.flush();
      }
    } catch (InterruptedException e) {
    } catch (Exception e) {
      client.onStreamError(new XMPPError(XMPPError.Type.CANCEL, "gone",
        "Error sending packets: " + e.getMessage()));
    }
  }

  // Parsers
  private void parseStanzas(Thread thread) {
    try {
      int eventType = parser.getEventType();
      do {
        if (eventType == XmlPullParser.START_TAG) {
          if (parser.getName().equals("presence")) {
            // TODO:
          } else if (parser.getName().equals("iq")) {
            parseIQ(parser);
          } else if (parser.getName().equals("message")) {
            // TODO
          } else if (parser.getName().equals("features")) {
            features.parse(parser);
            if (processFeatures()) {
              // Stream will be reinitiated
              return;
            }
          } else if (parser.getName().equals("proceed")) {
            startTLS();
            return;
          } else if (parser.getName().equals("compressed")) {
            startCompression();
            return;
          } else if (parser.getName().equals("challenge")) {
            String challengeData = parser.nextText();
            client.auth.processResponse(challengeData);
          } else if (parser.getName().equals("success")) {
            String namespace = parser.getNamespace(null);
            if ("urn:ietf:params:xml:ns:xmpp-sasl".equals(namespace)) {
              client.onAuthenticated();
              initStream();
              return;
            }
          } else if (parser.getName().equals("error")) {
            client.onStreamError(new XMPPError(parser));
            return;
          } else if (parser.getName().equals("failure")) {
            client.onStreamError(new XMPPError(parser));
            return;
          } else if (parser.getName().equals("stream")) {
            if ("jabber:client".equals(parser.getNamespace(null))) {
              String cid = null;
              String from = null;
              for (int i=0; i<parser.getAttributeCount(); i++) {
                if (parser.getAttributeName(i).equals("id")) {
                  cid = parser.getAttributeValue(i);
                } else if (parser.getAttributeName(i).equals("from")) {
                  from = parser.getAttributeValue(i);
                }
              }
              client.onStreamOpened(cid, from);
            }
          }
        } else if (eventType == XmlPullParser.END_TAG) {
          if (parser.getName().equals("stream")) {
            client.onStreamClosed();
            return;
          }
        }
        eventType = parser.next();
      } while (eventType != XmlPullParser.END_DOCUMENT && thread == readThread);
    } catch (Exception e) {
      client.onStreamError(new XMPPError(XMPPError.Type.CANCEL,
        "gone", "Error parsing input stream."));
    }
  }

  private void parseIQ(XmlPullParser parser) throws Exception {
    IQ iq = new IQ(parser);

    boolean done = false;
    while (!done) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        String e = parser.getName();
        String n = parser.getNamespace();

        if (e.equals("error")) {
          iq.setError(new XMPPError(parser));
        } else if (e.equals("bind") && n.equals("urn:ietf:params:xml:ns:xmpp-bind")) {
          iq.setPayload(new Bind(parser));
          client.onResourceBinded((Bind)iq.getPayload());
          return;
        } else if (e.equals("session") && n.equals("urn:ietf:params:xml:ns:xmpp-session")) {
          iq.setPayload(new Session(parser));
          client.onSessionStarted((Session)iq.getPayload());
          return;
        } else if (e.equals("query") && n.equals("jabber:iq:roster")) {
          //iq = parseRoster(parser);
        } else if (e.equals("query") && n.equals("jabber:iq:auth")) {
          //iq = parseAuthentication(parser);
        } else if (e.equals("query") && n.equals("jabber:iq:register")) {
          //iq = parseRegistration(parser);
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("iq")) {
          done = true;
        }
      }
    }

    // If here, the IQ was unhandled.
    if (iq.getType() == IQ.Type.get || iq.getType() == IQ.Type.set) {
      IQ eiq = iq.toErrorIQ(
        new XMPPError(XMPPError.Type.CANCEL, "feature-not-implemented"));
      pushStanza(eiq);
      return;
    } else {
      // TODO: Unhandled ERROR or RESULT IQ received.
    }

    return;
  }

  private boolean processFeatures() {
    if (features.tls && !client.isSecurized()) {
      if (client.socket.getSecurity() == Connection.Security.tls) {
        pushStanza("<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>");
        return false;
      } else if (features.tlsRequired) {
        // We cannot request TLS but we should
        client.onStreamError(new XMPPError(XMPPError.Type.CANCEL, "not-allowed",
          "TLS required by server but not allowed by this actual client settings."));
        return true;
      }
    }

    if (!client.isAuthed()) {
      client.onReceiveSASLMechanisms(features.saslMechs);
      client.onReadyforAuthentication();
      return false;
    }

    if (features.compression && !client.isCompressed()) {
      client.onReceiveCompressionMethods(features.compMethods);
      if (client.socket.getCompression() != Connection.Compression.none &&
          !client.socket.compressed) {
        pushStanza("<compress xmlns='http://jabber.org/protocol/compress'>" +
                   "<method>" + client.socket.getCompression().toString() +
                   "</method></compress>");
      }
      return false;
    }

    if (features.bind) {
      IQ iqb = new IQ(IQ.Type.set, new Bind(client.getFullJid()));
      pushStanza(iqb);
    }

    if (features.session) {
      IQ iqs = new IQ(IQ.Type.set, new Session());
      pushStanza(iqs);
    }

    return false;
  }
}
