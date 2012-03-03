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
  private Integer packetId = 0;

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

  Integer getNextPacketId() {
    return ++packetId; // TODO: Should be more complex than a serial number
  }

  void initStream() {
    if (this.client.socket.isConnected()) {
      this.connectionID = "";
      this.packetId = 0;

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

  void requestStartTLS(boolean required) throws Exception {
    if (this.client.socket.getSecurity() == XMPPSocket.Security.tls) {
      this.pushStanza("<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>");
    } else if (required) {
      this.client.notifyStreamException(
        new Exception("TLS required by server but not allowed by this actual client settings."));
    }
  }

  void startTLS() {
    if (this.client.socket.enableTLS()) {
      this.initStream();
    } else {
      this.client.notifyStreamException(new Exception("Start TLS failed."));
    }
  }

  void requestCompression() throws Exception {
    if (this.client.socket.getSecurity() != XMPPSocket.Security.none &&
        this.client.socket.securized == false) {
      return;
    }
    if (this.client.socket.getCompression() != XMPPSocket.Compression.none) {
      this.pushStanza("<compress xmlns='http://jabber.org/protocol/compress'>" +
                      "<method>" + this.client.socket.getCompression().toString() +
                      "</method></compress>");
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
    this.pushStanza("<iq xmlns=\"jabber:client\" type=\"set\" id=\"" +
      this.getNextPacketId() + "\"><bind xmlns=\"urn:ietf:params:xml:ns:xmpp-bind\">" +
      "<resource>" + this.client.getResource() + "</resource></bind></iq>");
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
        parsePackets(this);
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
    Log.write("Proc out queue start", 7); // DEBUG
    try {
      String stanza;
      while ((stanza = stanzaOutQueue.take()) != "") {
        Log.write("<<< " + stanza , 7); // DEBUG
        this.client.socket.writer.write(stanza);
        this.client.socket.writer.flush();
      }
    } catch (InterruptedException e) {
      // do nothing.
    } catch (Exception e) {
      this.client.notifyStreamException(e);
    }
  }

  private void parsePackets(Thread thread) {
    try {
      int eventType = parser.getEventType();
      do {
        if (eventType == XmlPullParser.START_TAG) {
          Log.write(">>> " + XMPPStreamParsers.repeat(' ', parser.getDepth()) + "<" + parser.getName() + ">", 7); // DEBUG

          if (parser.getName().equals("message")) {
            // TODO:
          } else if (parser.getName().equals("iq")) {
            // TODO
          } else if (parser.getName().equals("presence")) {
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
          } else if (parser.getName().equals("error")) {
            // TODO
          } else if (parser.getName().equals("features")) {
            XMPPStreamParsers.parseFeatures(parser, this);
          } else if (parser.getName().equals("failure")) {
            String namespace = parser.getNamespace(null);
            if ("urn:ietf:params:xml:ns:xmpp-tls".equals(namespace)) {
              this.client.notifySocketException(new Exception("TLS failure received."));
            } else if ("http://jabber.org/protocol/compress".equals(namespace)) {
              this.client.notifySocketException(new Exception("Compression failure received."));
            } else if ("urn:ietf:params:xml:ns:xmpp-sasl".equals(namespace)) {
              this.client.notifyLoginFailed(new Exception("Login process failed."));
            } else {
              this.client.notifyStreamException(new Exception("Unknown failure received."));
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
          if (parser.getName().equals("stream")) {
            this.client.disconnect();
          } else if (parser.getName().equals("proceed")) {
            this.startTLS();
            return;
          }
        }
        eventType = parser.next();
      } while (!this.parserDone && eventType != XmlPullParser.END_DOCUMENT && thread == readThread);
    } catch (Exception e) {
      this.client.notifyStreamException(e);
    }
  }

}
