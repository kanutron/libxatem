package com.xetrix.xmpp.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.util.Log; // DUBG

public class XMPPStream {
  XMPPClient client;

  // Life cycle
  private String connectionID = "";
  private Integer stanzaId = 0;

  // XML Parser
  private XmlPullParser parser;
  private boolean parserDone = false;

  // Stream Threads
  private Thread readThread;
  private Thread writeThread;
  private final BlockingQueue<String> stanzaOutQueue = new ArrayBlockingQueue<String>(500);

  // Constructors
  public XMPPStream(XMPPClient c) {
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

  void pushStanza(XMPPStanza s) {
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
    parserDone = true;
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

  boolean requestStartTLS(boolean required) throws Exception {
    if (client.socket.getSecurity() == XMPPSocket.Security.tls) {
      pushStanza("<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>");
      return true;
    } else if (required) {
      client.onStreamError(new XMPPError(XMPPError.Type.CANCEL, "not-allowed",
        "TLS required by server but not allowed by this actual client settings."));
      return true; // Connection will be terminated. Notify the parser.
    }
    return false;
  }

  boolean requestCompression() {
    if (client.socket.getSecurity() != XMPPSocket.Security.none &&
        client.socket.securized == false) {
      // We should wait for TLS before compression.
      // Once negotiated TLS, server could not offer compression.
      client.socket.setCompression(XMPPSocket.Compression.none);
      return false;
    }
    if (!client.isAuthed()) {
      // We should wait for SASL authentication before compression.
      return false;
    }
    if (client.socket.getCompression() != XMPPSocket.Compression.none &&
        !client.socket.compressed) {
      pushStanza("<compress xmlns='http://jabber.org/protocol/compress'>" +
                 "<method>" + client.socket.getCompression().toString() +
                 "</method></compress>");
    }
    return true;
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

  void doBind() {
    if (client.socket.getCompression() != XMPPSocket.Compression.none &&
        !client.socket.compressed) {
      // We should wait for compression before resource binding.
      return;
    }

    XMPPStanzaIQBind bind = new XMPPStanzaIQBind(client.getFullJid());
    pushStanza(bind);
  }

  // Private methods
  private void initParser() {
    parserDone = false;
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
        Log.write("<<< " + stanza , 7); // DEBUG
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
          Log.write(">>> " + repeat(' ', parser.getDepth()) + "<" + parser.getName() + ">", 7); // DEBUG

          if (parser.getName().equals("presence")) {
            // TODO:
          } else if (parser.getName().equals("iq")) {
            parseIQ(parser);
          } else if (parser.getName().equals("message")) {
            // TODO
          } else if (parser.getName().equals("features")) {
            parseFeatures(parser);
          } else if (parser.getName().equals("error")) {
            client.onStreamError(new XMPPError(parser));
            return;
          } else if (parser.getName().equals("failure")) {
            client.onStreamError(new XMPPError(parser));
            return;
          } else if (parser.getName().equals("challenge")) {
            String challengeData = parser.nextText();
            client.auth.processResponse(challengeData);
          } else if (parser.getName().equals("compressed")) {
            startCompression();
            return;
          } else if (parser.getName().equals("success")) {
            String namespace = parser.getNamespace(null);
            if ("urn:ietf:params:xml:ns:xmpp-sasl".equals(namespace)) {
              client.onAuthenticated();
              initStream();
              return;
            }
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
          if (parser.getName().equals("proceed")) {
            startTLS();
            return;
          } else if (parser.getName().equals("stream")) {
            client.onStreamClosed();
            return;
          }
        }
        eventType = parser.next();
      } while (!parserDone && eventType != XmlPullParser.END_DOCUMENT && thread == readThread);
    } catch (Exception e) {
      client.onStreamError(new XMPPError(XMPPError.Type.CANCEL,
        "gone", "Error parsing input stream."));
    }
  }

  private void parseIQ(XmlPullParser parser) throws Exception {
    XMPPStanzaIQ iq = new XMPPStanzaIQ(parser);

    boolean done = false;
    while (!done) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        String e = parser.getName();
        String n = parser.getNamespace();

        if (e.equals("error")) {
          iq.setError(new XMPPError(parser));
        } else if (e.equals("bind") && n.equals("urn:ietf:params:xml:ns:xmpp-bind")) {
          iq = new XMPPStanzaIQBind(parser, iq);
          client.onResourceBinded((XMPPStanzaIQBind)iq);
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
    if (iq.getType() == XMPPStanzaIQ.Type.get || iq.getType() == XMPPStanzaIQ.Type.set) {
      XMPPStanzaIQ eiq = iq.toErrorIQ(
        new XMPPError(XMPPError.Type.CANCEL, "feature-not-implemented"));
      pushStanza(eiq);
      return;
    } else {
      // TODO: Unhandled ERROR or RESULT IQ received.
      Log.write("!!! " + repeat(' ', parser.getDepth()) + iq.toXML(), 4); // DEBUG
    }

    return;
  }

  private void parseFeatures(XmlPullParser parser) throws Exception {
    boolean compressionReceived = false;
    boolean startTLSReceived = false;
    boolean startTLSRequired = false;

    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        if (parser.getName().equals("starttls")) {
          startTLSReceived = true;
        } else if (parser.getName().equals("mechanisms")) {
          client.onReceiveSASLMechanisms(parseMechanisms(parser));
        } else if (parser.getName().equals("compression")) {
          compressionReceived = true;
          client.onReceiveCompressionMethods(parseMethods(parser));
        } else if (parser.getName().equals("bind")) {
          doBind();
        } else if (parser.getName().equals("session")) {
          // TODO
        } else if (parser.getName().equals("register")) {
          // TODO
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("required") && startTLSReceived) {
          startTLSRequired = true;
        } else if (parser.getName().equals("features")) {
          if (startTLSReceived && requestStartTLS(startTLSRequired)) {
            return;
          }
          if (!client.isAuthed()) {
            // Wait for secure socket
            if (client.socket.getSecurity() != XMPPSocket.Security.none &&
                !client.socket.securized) {
              return;
            }
            client.onReadyforAuthentication();
            return;
          }
          if (compressionReceived && requestCompression()) {
            return;
          }
          return;
        }
      }
    }
  }

  private static List<String> parseMechanisms(XmlPullParser parser) throws Exception {
    List<String> mechanisms = new ArrayList<String>();
    boolean done = false;
    while (!done) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        String elementName = parser.getName();
        if (elementName.equals("mechanism")) {
          mechanisms.add(parser.nextText());
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("mechanisms")) {
          done = true;
        }
      }
    }
    return mechanisms;
  }

  private static List<String> parseMethods(XmlPullParser parser) throws Exception {
    List<String> methods = new ArrayList<String>();
    boolean done = false;
    while (!done) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        String elementName = parser.getName();
        if (elementName.equals("method")) {
          methods.add(parser.nextText());
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("compression")) {
          done = true;
        }
      }
    }
    return methods;
  }

  // DEBUG METHOD
  private static String repeat(char c, int i) {
    String tst = "";
    for(int j = 0; j < i; j++) {
      tst = tst+c;
    }
    return tst;
  }
}
