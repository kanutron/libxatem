package com.xetrix.xmpp.client;

import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.xetrix.xmpp.util.Log; // DUBG

public class XMPPStream {
  private XMPPClient client;

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
    if (this.client.socket.isConnected()) {
      this.pushStanza("</stream:stream>");
    }
    this.parserDone = true;
    this.readThread = new Thread();
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
        this.client.socket.writer.write(stanza );
        this.client.socket.writer.flush();
      }
    } catch (Exception e) {
      this.client.notifyStreamException(e);
    }
  }

  private void parsePackets(Thread thread) {
    try {
      int eventType = parser.getEventType();
      do {
        if (eventType == XmlPullParser.START_TAG) {
          Log.write(">>> " + repeat(' ', parser.getDepth()) + "<" + parser.getName() + ">", 7); // DEBUG

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
            this.parseFeatures(parser);
          } else if (parser.getName().equals("failure")) {
            String namespace = parser.getNamespace(null);
            if ("urn:ietf:params:xml:ns:xmpp-tls".equals(namespace)) {
              // TODO
            } else if ("http://jabber.org/protocol/compress".equals(namespace)) {
              // TODO
            } else {
              // TODO
            }
          } else if (parser.getName().equals("challenge")) {
            // TODO
          } else if (parser.getName().equals("success")) {
            // TODO
          } else if (parser.getName().equals("compressed")) {
            // TODO
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

  private void parseFeatures(XmlPullParser parser) throws Exception {
    boolean startTLSReceived = false;
    boolean startTLSRequired = false;
    boolean parserDone = false;

    while (!parserDone) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        Log.write(">>> " + repeat(' ', parser.getDepth()) + "<" + parser.getName() + ">", 7); // DEBUG

        if (parser.getName().equals("starttls")) {
          startTLSReceived = true;
        } else if (parser.getName().equals("mechanisms")) {
          // TODO
        } else if (parser.getName().equals("bind")) {
          // TODO
        } else if (parser.getName().equals("session")) {
          // TODO
        } else if (parser.getName().equals("compression")) {
          // TODO
        } else if (parser.getName().equals("register")) {
          // TODO
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("starttls")) {
          this.requestStartTLS(startTLSRequired);
        } else if (parser.getName().equals("required") && startTLSReceived) {
          startTLSRequired = true;
        } else if (parser.getName().equals("features")) {
          parserDone = true;
        }
      }
    }
  }

  private void requestStartTLS(boolean required) throws Exception {
    if (this.client.socket.getSecurity() == XMPPSocket.Security.tls) {
      this.pushStanza("<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>");
    } else if (required) {
      this.client.notifyStreamException(
        new Exception("TLS required by server but not allowed by this actual client settings."));
    }
  }

  private void startTLS() {
    if (this.client.socket.enableTLS()) {
      this.initStream();
    } else {
      this.client.notifyStreamException(new Exception("Start TLS failed."));
    }
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
