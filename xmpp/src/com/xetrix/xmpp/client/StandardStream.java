package com.xetrix.xmpp.client;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.stanza.Stanza;
import com.xetrix.xmpp.stanza.IQ;
import com.xetrix.xmpp.payload.Bind;
import com.xetrix.xmpp.payload.Session;

public class StandardStream implements Stream {
  Connection                      conn;
  Auth                            auth;
  StreamListener                  listener;
  StreamFeatures                  features = new StreamFeatures();

  // Life cycle control
  private boolean               opened = false;
  private boolean               binded = false;
  private boolean               sessionStarted = false;

  private String                 streamId = "";
  private String                 streamFrom = "";
  private Integer                stanzaId = 0;

  // XML Parser
  private XmlPullParser parser;

  // Stream Threads
  private Thread readThread;
  private Thread writeThread;
  private final BlockingQueue<String> stanzaOutQueue = new ArrayBlockingQueue<String>(500);

  // Constructors
  public StandardStream(StreamListener l, Auth a, Connection c) {
    listener = l;
    auth = a;
    conn = c;
  }

  public void setAuth(Auth a) {
    auth = a;
  }

  public void setConnection(Connection c) {
    conn = c;
  }

  public void setListener(StreamListener l) {
    listener = l;
  }

  public StreamListener getListener() {
    return listener;
  }

  public boolean isOpened() {
    return opened;
  }

  public boolean isAuthed() {
    return auth.isAuthed();
  }

  public boolean isBinded() {
    return binded;
  }

  public boolean isSessionStarted() {
    return sessionStarted;
  }

  public void pushStanza(String s) {
    try {
      stanzaOutQueue.put(s);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      listener.onStreamError(new XMPPError(XMPPError.Type.CANCEL,
        "service-unavailable", e.getMessage()));
    }
  }

  public void pushStanza(Stanza s) {
    if (s.getId() == null) {
      s.setId(getNextStanzaId());
    }
    pushStanza(s.toXML());
  }

  public String getStreamId() {
    return streamId;
  }

  public void initStream(String service) {
    StreamFeatures features = new StreamFeatures();
    streamId = "";
    stanzaId = 0;
    streamFrom = service;

    initParser();
    initWriter();

    pushStanza("<stream:stream to=\"" + streamFrom +
      "\" xmlns:stream=\"http://etherx.jabber.org/streams\" " +
      "xmlns=\"jabber:client\" version=\"1.0\">");
  }

  public void finishStream() {
    binded = false;
    sessionStarted = false;

    readThread.interrupt();
    readThread = new Thread();
    writeThread.interrupt();
    writeThread = new Thread();

    try {
      conn.getWriter().write("</stream:stream>");
      conn.getWriter().flush();
    } catch (Exception e) {
    }
  }

  public String getNextStanzaId() {
    stanzaId++;
    return stanzaId.toString(); // TODO: Should be more complex than a serial number
  }

  // Private methods
  private void initStream() {
    initStream(streamFrom);
  }

  private void startTLS() {
    if (conn.enableTLS()) {
      initStream();
    } else {
      listener.onStreamError(new XMPPError(XMPPError.Type.CANCEL, "bad-request",
        "Start TLS failed."));
    }
  }

  private void startCompression() {
    if (conn.enableCompression()) {
      initStream();
    } else {
      listener.onStreamError(new XMPPError(XMPPError.Type.CANCEL, "bad-request",
        "Start compression failed."));
    }
  }

  private void initParser() {
    try {
      parser = new MXParser();
      parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
      parser.setInput(conn.getReader());
    } catch (Exception e) {
      listener.onStreamError(new XMPPError(XMPPError.Type.CANCEL,
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
        conn.getWriter().write(stanza);
        conn.getWriter().flush();
      }
    } catch (InterruptedException e) {
    } catch (Exception e) {
      listener.onStreamError(new XMPPError(XMPPError.Type.CANCEL, "gone",
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
              return;
            }
          } else if (parser.getName().equals("proceed")) {
            startTLS();
            return;
          } else if (parser.getName().equals("compressed")) {
            startCompression();
            return;
          } else if (parser.getName().equals("challenge")) {
            String response = auth.processChallenge(parser.nextText());
            if (!"".equals(response)) {
              pushStanza(response);
            } else {
              listener.onStreamError(auth.getError());
            }
          } else if (parser.getName().equals("success")) {
            if ("urn:ietf:params:xml:ns:xmpp-sasl".equals(parser.getNamespace(null))) {
              if (auth.processSuccess()) {
                listener.onAuthenticated();
                initStream();
                return;
              } else {
                listener.onStreamError(auth.getError());
              }
            }
          } else if (parser.getName().equals("error")) {
            listener.onStreamError(new XMPPError(parser));
            return;
          } else if (parser.getName().equals("failure")) {
            listener.onStreamError(new XMPPError(parser));
            return;
          } else if (parser.getName().equals("stream")) {
            if ("jabber:client".equals(parser.getNamespace(null))) {
              streamId = parser.getAttributeValue("", "id");
              String from = parser.getAttributeValue("", "from");
              if ("".equals(from)) {
                streamFrom = from;
              }
              opened = true;
              listener.onStreamOpened(streamFrom);
            }
          }
        } else if (eventType == XmlPullParser.END_TAG) {
          if (parser.getName().equals("stream")) {
            opened = false;
            listener.onStreamClosed();
            finishStream();
            return;
          }
        }
        eventType = parser.next();
      } while (eventType != XmlPullParser.END_DOCUMENT && thread == readThread);
    } catch (Exception e) {
      listener.onStreamError(new XMPPError(XMPPError.Type.CANCEL,
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
          binded = true; // TODO: check if type==result
          listener.onResourceBinded((Bind)iq.getPayload());
          return;
        } else if (e.equals("session") && n.equals("urn:ietf:params:xml:ns:xmpp-session")) {
          iq.setPayload(new Session(parser));
          sessionStarted = true;// TODO: check if type==result
          listener.onSessionStarted((Session)iq.getPayload());
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

    // TODO: if IQ error, notify listener (continue)

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
    if (features.tls && !conn.isSecurized()) {
      if (conn.getSecurity() == Connection.Security.tls) {
        pushStanza("<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>");
        return false;
      } else if (features.tlsRequired) {
        // We cannot request TLS but we should
        listener.onStreamError(new XMPPError(XMPPError.Type.CANCEL, "not-allowed",
          "TLS required by server but not allowed by this actual client settings."));
        return true;
      }
    }

    if (!isAuthed()) {
      if (auth.setServerMechanisms(features.saslMechs)) {
        String s = auth.startAuthWith(auth.getBestMechanism());
        if (!"".equals(s)) {
          pushStanza(s);
          return false;
        } else {
          listener.onStreamError(auth.getError());
          return true;
        }
      } else {
        listener.onStreamError(auth.getError());
        return true;
      }
    }

    if (features.compression && !conn.isCompressed()) {
      if (conn.compressionSetServerMethods(features.compMethods)) {
        pushStanza("<compress xmlns='http://jabber.org/protocol/compress'>" +
                   "<method>" + conn.getCompression().toString() + "</method></compress>");
        return false;
      }
    }

    if (features.bind) {
      listener.onReadyForBindResource();
    }

    if (features.session) {
      listener.onReadyForStartSession();
    }

    return false;
  }
}
