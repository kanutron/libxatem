package com.xetrix.xmpp.client;

import java.util.ArrayList;
import java.util.List;
import com.xetrix.xmpp.Parseable;
import org.xmlpull.v1.XmlPullParser;

public class StreamFeatures implements Parseable {
  public boolean compression = false;
  public boolean tls = false;
  public boolean tlsRequired = false;
  public boolean bind = false;
  public boolean bindRequired = false;
  public boolean session = false;
  public boolean register = false;
  public List<String> saslMechs = new ArrayList<String>();
  public List<String> compMethods = new ArrayList<String>();

  // Constructors
  public StreamFeatures() {}
  public StreamFeatures(XmlPullParser parser) throws Exception {
    parse(parser);
  }

  public void parse(XmlPullParser parser) throws Exception {
    if (!"features".equals(parser.getName())) {
      return;
    }
    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        if (parser.getName().equals("starttls")) {
          parseStartTLS(parser);
        } else if (parser.getName().equals("mechanisms")) {
          parseMechanisms(parser);
        } else if (parser.getName().equals("compression")) {
          compression = true;
          parseMethods(parser);
        } else if (parser.getName().equals("bind")) {
          parseBind(parser);
        } else if (parser.getName().equals("session")) {
          parseSession(parser);
        } else if (parser.getName().equals("register")) {
          register = true;
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("features")) {
          return;
        }
      }
    }
  }

  // Private methos
  private void parseMechanisms(XmlPullParser parser) throws Exception {
    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        String elementName = parser.getName();
        if (elementName.equals("mechanism")) {
          saslMechs.add(parser.nextText());
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("mechanisms")) {
          return;
        }
      }
    }
  }

  private void parseMethods(XmlPullParser parser) throws Exception {
    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        String elementName = parser.getName();
        if (elementName.equals("method")) {
          compMethods.add(parser.nextText());
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("compression")) {
          return;
        }
      }
    }
  }

  private void parseStartTLS(XmlPullParser parser) throws Exception {
    tls = true;
    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        String elementName = parser.getName();
        if (elementName.equals("required")) {
          tlsRequired = true;
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("starttls")) {
          return;
        }
      }
    }
  }

  private void parseBind(XmlPullParser parser) throws Exception {
    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        String elementName = parser.getName();
        if (elementName.equals("required")) {
          bindRequired = true;
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        bind = true;
        if (parser.getName().equals("bind")) {
          return;
        }
      }
    }
  }

  private void parseSession(XmlPullParser parser) throws Exception {
    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("session")) {
          session = true;
          return;
        }
      }
    }
  }

}