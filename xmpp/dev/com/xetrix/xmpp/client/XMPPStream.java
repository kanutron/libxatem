package com.xetrix.xmpp.client;

import java.io.IOException;
import java.io.EOFException;

import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.xetrix.xmpp.util.Log; // DUBG

public class XMPPStream {
  private XMPPClient client;
  private XmlPullParser parser;
  private Thread readThread;
  private String connectionID = "";

  // Constructors
  public XMPPStream(XMPPClient c) {
    this.client = c;
  }

  // Public methods
  public void write(String s) { // DEBUG METHOD
    try {
      Log.write("<<< " + s, 7); // DEBUG
      this.client.socket.writer.write(s);
      this.client.socket.writer.flush();
    } catch (IOException ioe) {
      ioe.printStackTrace(); // DEBUG
    } catch (Exception e) {
      e.printStackTrace(); // DEBUG
    }
  }

  public String getConnectionID() {
    return this.connectionID;
  }

  public void initStream() {
    if (this.client.socket.isConnected()) {
      try {
        parser = new MXParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(this.client.socket.reader);
      }
      catch (XmlPullParserException xppe) {
        xppe.printStackTrace();
      }

      readThread = new Thread() {
        public void run() {
          parsePackets(this);
        }
      };
      readThread.setName("ThreadXMLParser");
      readThread.setDaemon(true);
      readThread.start();

      this.write(
        "<stream:stream to=\"" + this.client.getService() +
        "\" xmlns:stream=\"http://etherx.jabber.org/streams\" " +
           "xmlns=\"jabber:client\" version=\"1.0\">");

    } else {
      // Reset parser
      parser = new MXParser();
      readThread = new Thread();
    }
  }

  // Private methods
  private void parsePackets(Thread thread) {
    Boolean exit = false;
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
            // TODO
          } else if (parser.getName().equals("starttls")) {
            // FIXME: do it in parse features
            this.client.requestStartTLS(true); // TODO: required?
          } else if (parser.getName().equals("proceed")) {
            exit = true;
            this.parser = null;
            this.client.startTLS();
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
          }
        }
        if (!exit) {
          eventType = parser.next();
        }
      } while (!exit && eventType != XmlPullParser.END_DOCUMENT && thread == readThread);

    } catch (XmlPullParserException xe) {
      xe.printStackTrace();
    } catch (EOFException eofe) {
      // COULD HAPPENS
      // * when connection is interrupted (socket is dead)
      // * when connected to ssl host asuming plain/tls (socket is alive)
      if (!this.client.socket.isConnected()) {
        this.client.notifySocketClosed(eofe);
      } else {
        this.client.notifyNonXMLReceibed(eofe);
      }
    } catch (Exception e) {
      this.client.notifySocketClosed(e);
    }
  }

  private static String repeat(char c, int i) {
    String tst = "";
    for(int j = 0; j < i; j++) {
      tst = tst+c;
    }
    return tst;
  }

}
