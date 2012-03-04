package com.xetrix.xmpp.client;

import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.util.Log; // DUBG

public class XMPPStreamParsers {

  static void parseFeatures(XmlPullParser parser, XMPPStream stream) throws Exception {
    boolean compressionReceived = false;
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
          stream.client.saslSetServerMechanisms(parseMechanisms(parser));
        } else if (parser.getName().equals("bind")) {
          stream.doBind();
        } else if (parser.getName().equals("session")) {
          // TODO
        } else if (parser.getName().equals("compression")) {
          compressionReceived = true;
          stream.client.notifyCompressionMethods(parseMethods(parser));
        } else if (parser.getName().equals("register")) {
          // TODO
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("starttls")) {
          stream.requestStartTLS(startTLSRequired);
        } else if (parser.getName().equals("required") && startTLSReceived) {
          startTLSRequired = true;
        } else if (parser.getName().equals("features")) {
          parserDone = true;
          if (compressionReceived) {
            stream.requestCompression();
          }
          stream.client.notifyReadyToLogin();
        }
      }
    }
  }

  static List<String> parseMechanisms(XmlPullParser parser) throws Exception {
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

  static List<String> parseMethods(XmlPullParser parser) throws Exception {
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
  public static String repeat(char c, int i) {
    String tst = "";
    for(int j = 0; j < i; j++) {
      tst = tst+c;
    }
    return tst;
  }
}