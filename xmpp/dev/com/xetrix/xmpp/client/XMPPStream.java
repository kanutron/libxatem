package com.xetrix.xmpp.client;

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
    this.client = c;
  }

  // Package methods
  void pushStanza(String s) {
    try {
      stanzaOutQueue.put(s);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      this.client.notifyStreamException(e);
    }
  }

  String getConnectionID() {
    return this.connectionID;
  }

  Integer getNextStanzaId() {
    return ++stanzaId; // TODO: Should be more complex than a serial number
  }

  void initStream() {
    if (this.client.socket.isConnected()) {
      this.connectionID = "";
      this.stanzaId = 0;

      this.initParser();
      this.initWriter();

      this.pushStanza(
        "<stream:stream to=\"" + this.client.getService() +
        "\" xmlns:stream=\"http://etherx.jabber.org/streams\" " +
           "xmlns=\"jabber:client\" version=\"1.0\">");
    }
  }

  void finishStream() {
    this.parserDone = true;
    this.readThread.interrupt();
    this.readThread = new Thread();
    this.writeThread.interrupt();
    this.writeThread = new Thread();

    if (this.client.socket.isConnected()) {
      try {
        this.client.socket.writer.write("</stream:stream>");
        this.client.socket.writer.flush();
      } catch (Exception e) {
      }
    }
  }

  boolean requestStartTLS(boolean required) throws Exception {
    if (this.client.socket.getSecurity() == XMPPSocket.Security.tls) {
      this.pushStanza("<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>");
      return true;
    } else if (required) {
      this.client.notifyStreamException(
        new Exception("TLS required by server but not allowed by this actual client settings."));
      return true; // Connection will be terminated. Notify the parser.
    }
    return false;
  }

  boolean requestCompression() {
    if (this.client.socket.getSecurity() != XMPPSocket.Security.none &&
        this.client.socket.securized == false) {
      // We should wait for TLS before compression.
      // Once negotiated TLS, server could not offer compression.
      this.client.socket.setCompression(XMPPSocket.Compression.none);
      return false;
    }
    if (!this.client.isAuthed()) {
      // We should wait for SASL authentication before compression.
      return false;
    }
    if (this.client.socket.getCompression() != XMPPSocket.Compression.none &&
        !this.client.socket.compressed) {
      this.pushStanza("<compress xmlns='http://jabber.org/protocol/compress'>" +
                      "<method>" + this.client.socket.getCompression().toString() +
                      "</method></compress>");
    }
    return true;
  }

  void startTLS() {
    if (this.client.socket.enableTLS()) {
      this.initStream();
    } else {
      this.client.notifyStreamException(new Exception("Start TLS failed."));
    }
  }

  void startCompression() {
    if (this.client.socket.enableCompression()) {
      this.initStream();
    } else {
      this.client.notifyStreamException(new Exception("Start compression failed."));
    }
  }

  void doBind() {
    if (this.client.socket.getCompression() != XMPPSocket.Compression.none &&
        !this.client.socket.compressed) {
      // We should wait for compression before resource binding.
      return;
    }
    this.pushStanza("<iq xmlns=\"jabber:client\" type=\"set\" id=\"" +
      this.getNextStanzaId() + "\"><bind xmlns=\"urn:ietf:params:xml:ns:xmpp-bind\">" +
      "<resource>" + this.client.getResource() + "</resource></bind></iq>");
      // Debug ;-)
      //<iq type='get'><query id='" + this.getNextStanzaId() + "' xmlns='jabber:iq:roster'/></iq><presence />
  }

  // Private methods
  private void initParser() {
    this.parserDone = false;
    try {
      this.parser = new MXParser();
      this.parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
      this.parser.setInput(this.client.socket.reader);
    } catch (Exception e) {
      this.client.notifyStreamException(e);
    }

    this.readThread = new Thread() {
      public void run() {
        parseStanzas(this);
      }
    };
    this.readThread.setName("ThreadXMLParser");
    this.readThread.setDaemon(true);
    this.readThread.start();
  }

  private void initWriter() {
    stanzaOutQueue.clear();
    this.writeThread = new Thread() {
      public void run() {
        procOutQueue(this);
      }
    };
    this.writeThread.setName("ThreadStanzaOutQueue");
    this.writeThread.setDaemon(true);
    this.writeThread.start();
  }

  private void procOutQueue(Thread thread) {
    try {
      String stanza;
      while ((stanza = stanzaOutQueue.take()) != "") {
        Log.write("<<< " + stanza , 7); // DEBUG
        this.client.socket.writer.write(stanza);
        this.client.socket.writer.flush();
      }
    } catch (InterruptedException e) {
    } catch (Exception e) {
      this.client.notifyStreamException(e);
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
            // TODO
          } else if (parser.getName().equals("message")) {
            // TODO
          } else if (parser.getName().equals("stream")) {
            if ("jabber:client".equals(parser.getNamespace(null))) {
              for (int i=0; i<parser.getAttributeCount(); i++) {
                if (parser.getAttributeName(i).equals("id")) {
                  this.connectionID = parser.getAttributeValue(i);
                } else if (parser.getAttributeName(i).equals("from")) {
                  this.client.setService(parser.getAttributeValue(i));
                }
              }
            }
          } else if (parser.getName().equals("features")) {
            XMPPStreamParsers.parseFeatures(parser, this);
          } else if (parser.getName().equals("error")) {
            // TODO: parseError, throw the Exception and return/finish
            parser.next();
            String error = parser.getName();
            this.client.notifyStreamException(new Exception(error));
            return;
          } else if (parser.getName().equals("failure")) {
            String namespace = parser.getNamespace(null);
            if ("urn:ietf:params:xml:ns:xmpp-tls".equals(namespace)) {
              this.client.notifySocketException(new Exception("TLS failure received."));
              return;
            } else if ("http://jabber.org/protocol/compress".equals(namespace)) {
              this.client.notifySocketException(new Exception("Compression failure received."));
              return;
            } else if ("urn:ietf:params:xml:ns:xmpp-sasl".equals(namespace)) {
              this.client.notifyLoginFailed(new Exception("Login process failed."));
              return;
            } else {
              this.client.notifyStreamException(new Exception("Unknown failure received."));
              return;
            }
          } else if (parser.getName().equals("challenge")) {
            String challengeData = parser.nextText();
            this.client.auth.processResponse(challengeData);
          } else if (parser.getName().equals("success")) {
            String namespace = parser.getNamespace(null);
            if ("urn:ietf:params:xml:ns:xmpp-sasl".equals(namespace)) {
              this.client.notifyAuthenticated();
              this.initStream();
              return;
            }
          } else if (parser.getName().equals("compressed")) {
            this.startCompression();
            return;
          }
        } else if (eventType == XmlPullParser.END_TAG) {
          if (parser.getName().equals("proceed")) {
            this.startTLS();
            return;
          } else if (parser.getName().equals("stream")) {
            this.client.disconnect();
            return;
          }
        }
        eventType = parser.next();
      } while (!this.parserDone && eventType != XmlPullParser.END_DOCUMENT && thread == readThread);
    } catch (Exception e) {
      this.client.notifyStreamException(e);
    }
  }

  // DEBUG METHOD
  public static String repeat(char c, int i) {
    String tst = "";
    for(int j = 0; j < i; j++) {
      tst = tst+c;
    }
    return tst;
  }
}
