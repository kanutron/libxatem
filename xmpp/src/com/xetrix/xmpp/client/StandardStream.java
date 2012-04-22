package com.xetrix.xmpp.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.stanza.Stanza;
import com.xetrix.xmpp.stanza.IQ;
import com.xetrix.xmpp.payload.Bind;
import com.xetrix.xmpp.payload.Session;

public class StandardStream implements Stream {
  private Connection            conn;
  private Auth                  auth;
  private StreamListener        listener;

  // Stanza Handlers
  private List<StanzaHandler>   handlers = new CopyOnWriteArrayList<StanzaHandler>();

  // Life cycle control
  private boolean               opened = false;
  private boolean               binded = false;
  private boolean               sessionStarted = false;

  private String                streamId = "";
  private String                streamFrom = "";
  private Integer               stanzaId = 0;

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

  public Auth getAuth() {
    return auth;
  }

  public void setConnection(Connection c) {
    conn = c;
  }

  public Connection getConnection() {
    return conn;
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
    streamId = "";
    stanzaId = 0;
    streamFrom = service;

    initParser();
    initWriter();

    pushStanza("<stream:stream to=\"" + streamFrom +
      "\" xmlns:stream=\"http://etherx.jabber.org/streams\" " +
      "xmlns=\"jabber:client\" version=\"1.0\">");
  }

  public void initStream() {
    if (streamFrom.equals("")) {
      listener.onStreamError(new XMPPError(XMPPError.Type.CANCEL, "bad-request",
        "Can't open a stream without knowing the service name."));
    } else {
      initStream(streamFrom);
    }
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

  private void parseStanzas(Thread thread) {
    handlers.clear();
    handlers.add(new StreamNegotiationHandler());

    try {
      int eventType = parser.getEventType();
      do {
        if (eventType == XmlPullParser.START_TAG) {

          for (StanzaHandler h: handlers) {
            if (h.wantsStanza(parser)) {
              if (!h.handleStanza(this, parser)) {
                return; // Either error handling or stream reset needed
              } else if (h.hasStanza()) {
                Stanza s = h.getStanza();
                // TODO: Send stanza to interested parties
              }
              if (h.finished()) {
                //handlers.remove(h);
              }
            }
          }

          if (parser.getName().equals("iq")) {
            parseIQ(parser);
          } else if (parser.getName().equals("error")) {
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


  public void parseIQ(XmlPullParser parser) throws Exception {
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
          sessionStarted = true; // TODO: check if type==result
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


}
