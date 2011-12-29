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

  // Constructors
  public XMPPStream(XMPPClient c) {
    if (c.socket.isConnected()) {
      this.client = c;
      this.initParser();
    }
  }

  private void initParser() {
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
    readThread.setName("Reader");
    readThread.setDaemon(true);
    readThread.start();
  }

  private void parsePackets(Thread thread) {
    Boolean exit = false;
    try {
      int eventType = parser.getEventType();
      do {
        if (eventType == XmlPullParser.START_TAG) {
          Log.write(">>> " + repeat(' ', parser.getDepth()) + "<" + parser.getName() + ">", 7); // DEBUG
        } else if (eventType == XmlPullParser.END_TAG) {
          if (parser.getName().equals("stream")) {
            this.client.disconnect();
          } else if (parser.getName().equals("starttls")) {
            this.client.requestStartTLS(true); // TODO: required?
          } else if (parser.getName().equals("proceed")) {
            exit = true;
            this.parser = null;
            this.client.startTLS();
          }
        }
        if (!exit) {
          eventType = parser.next();
        }
      } while (!exit && eventType != XmlPullParser.END_DOCUMENT && thread == readThread);
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

  public void write(String s) { // DEBUG METHOD
    try {
      this.client.socket.writer.write(s);
      this.client.socket.writer.flush();
      Log.write("<<< " + s, 7); // DEBUG
    } catch (IOException ioe) {
      ioe.printStackTrace(); // DEBUG
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
