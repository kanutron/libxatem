package com.xetrix.xmpp.client;

import java.util.List;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.stanza.Stanza;
import com.xetrix.xmpp.stanza.StanzaParser;
import com.xetrix.xmpp.stanza.StanzaListener;
import com.xetrix.xmpp.stanza.IQ;

public class StandardStream implements Stream {
  private Connection            conn;
  private Auth                  auth;
  private StreamListener        listener;

  // Stanza Parsers/Listeners
  private List<StanzaParser>    stanzaParsers = new CopyOnWriteArrayList<StanzaParser>();
  private List<StanzaListener>  stanzaInListeners = new CopyOnWriteArrayList<StanzaListener>();
  private List<StanzaListener>  stanzaOutListeners = new CopyOnWriteArrayList<StanzaListener>();

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
  private Thread inPublisherThread;
  private final BlockingQueue<String> stanzaOutQueue = new ArrayBlockingQueue<String>(500);
  private final BlockingQueue<Stanza> stanzaInQueue = new ArrayBlockingQueue<Stanza>(500);

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

  public void addStanzaParser(StanzaParser p) {
    stanzaParsers.add(p);
  }

  public void removeStanzaParser(StanzaParser p) {
    stanzaParsers.remove(p);
  }

  public void clearStanzaParsers() {
    stanzaParsers.clear();
  }

  public void addStanzaInListener(StanzaListener l) {
    stanzaInListeners.add(l);
    //System.out.println("Adding inc. listener " + l.getClass().getName() + ". Total: " + stanzaInListeners.size());
  }

  public void removeStanzaInListener(StanzaListener l) {
    stanzaInListeners.remove(l);
    //System.out.println("Removing inc. listener " + l.getClass().getName() + ". Remaining: " + stanzaInListeners.size());
  }

  public void clearStanzaInListeners() {
    stanzaInListeners.clear();
  }

  public void addStanzaOutListener(StanzaListener l) {
    stanzaOutListeners.add(l);
    //System.out.println("Adding out. listener " + l.getClass().getName() + ". Total: " + stanzaOutListeners.size());
  }

  public void removeStanzaOutListener(StanzaListener l) {
    stanzaOutListeners.remove(l);
    //System.out.println("Removing out. listener " + l.getClass().getName() + ". Remaining: " + stanzaOutListeners.size());
  }

  public void clearStanzaOutListeners() {
    stanzaOutListeners.clear();
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

  public void setBinded(String j, String r) {
    binded = true;
    listener.onResourceBinded(j, r);
  }

  public boolean isSessionStarted() {
    return sessionStarted;
  }

  public void setSessionStarded() {
    sessionStarted = true;
    listener.onSessionStarted();
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
    publishOutgoingStanza(s);
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
    initIncomingPublisher();

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
    inPublisherThread.interrupt();
    inPublisherThread = new Thread();

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
        procOutQueue();
      }
    };
    writeThread.setName("ThreadStanzaOutQueue");
    writeThread.setDaemon(true);
    writeThread.start();
  }

  private void initIncomingPublisher() {
    inPublisherThread = new Thread() {
      public void run() {
        publishIncomingStanzas();
      }
    };
    inPublisherThread.setName("ThreadIncomingPublisher");
    inPublisherThread.setDaemon(true);
    inPublisherThread.start();
  }

  private void parseStanzas(Thread thread) {
    try {
      Iterator itr;
      boolean parsed = false;
      int eventType = parser.getEventType();

      while (eventType != XmlPullParser.END_DOCUMENT && thread == readThread) {
        eventType = parser.next();

        if (eventType == XmlPullParser.START_TAG && parser.getDepth() == 2) {
          itr = stanzaParsers.iterator();
          parsed = false;
          while(itr.hasNext()) {
            StanzaParser p = (StanzaParser)itr.next();
            if (p.wantsStanza(parser)) {
              parsed = true;
              if (!p.parseStanza(this, parser)) {
                // Either error handling stanza or stream reset needed
                return;
              } else if (p.hasStanza()) {
                stanzaInQueue.put(p.getStanza());
              }
              if (p.finished()) {
                removeStanzaParser(p);
              }
              break; // Only first parser can process
            }
          }
          if (!parsed) {
            listener.onStreamError(new XMPPError(XMPPError.Type.CONTINUE, "feature-not-implemented",
              "Received not understood stanza: " + parser.getName()));
          }
        } else if (eventType == XmlPullParser.START_TAG && parser.getDepth() == 1) {
          // Handle opening stream
          if (parser.getDepth() == 1 &&
              parser.getName().equals("stream") &&
              parser.getNamespace().equals("http://etherx.jabber.org/streams")) {
            streamId = parser.getAttributeValue("", "id");
            String from = parser.getAttributeValue("", "from");
            if (!from.equals("")) {
              streamFrom = from;
            }
            opened = true;
            listener.onStreamOpened(streamFrom);
          }
        }
      }

      // End document received
      opened = false;
      binded = false;
      sessionStarted = false;
      listener.onStreamClosed();
      finishStream();
      return;

    } catch (Exception e) {
      opened = false;
      listener.onStreamError(new XMPPError(XMPPError.Type.CANCEL,
        "gone", "Error parsing input stream."));
    }
  }

  private void procOutQueue() {
    try {
      String stanza;
      while ((stanza = stanzaOutQueue.take()) != "") {
        conn.getWriter().write(stanza);
        conn.getWriter().flush();
      }
    } catch (InterruptedException e) {
    } catch (Exception e) {
      listener.onStreamError(new XMPPError(XMPPError.Type.CANCEL, "gone",
        "Error sending stanzas: " + e.getMessage()));
    }
  }

  private void publishIncomingStanzas() {
    Stanza stanza;
    boolean handled;
    try {
      while ((stanza = stanzaInQueue.take()) != null) {
        Iterator itr;
        itr = stanzaInListeners.iterator();
        handled = false;
        while(itr.hasNext()) {
          StanzaListener l = (StanzaListener)itr.next();
          if (l.wantsStanza(stanza)) {
            handled = true;
            if (l.finished()) {
              removeStanzaInListener(l);
            }
            if (l.processStanza(stanza)) {
              break;
            }
          }
        }
        if (!handled) {
          processUnhandledStanza(stanza);
        }
      }
    } catch (InterruptedException e) {
    } catch (Exception e) {
      listener.onStreamError(new XMPPError(XMPPError.Type.CANCEL, "gone",
        "Error processing incoming stanzas: " + e.getMessage()));
    }
  }

  private void publishOutgoingStanza(Stanza stanza) {
    Iterator itr = stanzaOutListeners.iterator();
    while(itr.hasNext()) {
      StanzaListener l = (StanzaListener)itr.next();
      if (l.wantsStanza(stanza)) {
        if (l.finished()) {
          removeStanzaOutListener(l);
        }
        if (l.processStanza(stanza)) {
          break;
        }
      }
    }
  }

  private void processUnhandledStanza(Stanza s) {
    // Process unhandled IQs
    if (s.getName().equals("iq")) {
      IQ iq = new IQ((IQ)s);
      if (iq.getType() == IQ.Type.get || iq.getType() == IQ.Type.set) {
        IQ eiq = iq.toErrorIQ(
          new XMPPError(XMPPError.Type.CONTINUE, "feature-not-implemented"));
        pushStanza(eiq);
        listener.onStreamError(eiq.getError());
      } else if (iq.getType() == IQ.Type.error) {
        listener.onStreamError(iq.getError());
      } else {
        listener.onStreamError(new XMPPError(XMPPError.Type.CONTINUE, "unexpected-result",
          "IQ type=result received, but no body intersted."));
      }
    }
    // TODO: Process other unhandled stanzas
  }
}
