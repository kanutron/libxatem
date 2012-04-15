package com.xetrix.xmpp.payload;

import com.xetrix.xmpp.Parseable;
import org.xmlpull.v1.XmlPullParser;

public class Session extends IQPayload implements Parseable {
  // Constructors
  public Session() {}
  public Session(XmlPullParser parser) throws Exception {
    parse(parser);
  }

  // Public methods
  public String toXML() {
    StringBuilder buf = new StringBuilder();
    buf.append("<session xmlns=\"urn:ietf:params:xml:ns:xmpp-session\">");
    // contents here
    buf.append("</session>");
    return buf.toString();
  }

  public void parse(XmlPullParser parser) throws Exception {
    if (!"session".equals(parser.getName())) {
      return;
    }
    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        // do nothing
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("session")) {
          return;
        }
      }
    }
  }

}
