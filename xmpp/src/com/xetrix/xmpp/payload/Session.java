package com.xetrix.xmpp.payload;

import org.xmlpull.v1.XmlPullParser;

public class Session extends Payload {
  private static String NAME = "session";
  private static String XMLNS = "urn:ietf:params:xml:ns:xmpp-session";

  // Constructors
  public Session() {}

  // Public methods
  public String getName() {
    return NAME;
  }

  public String getXmlns() {
    return XMLNS;
  }

  public String toXML() {
    StringBuilder buf = new StringBuilder();
    buf.append("<" + NAME + " xmlns=\"" + XMLNS + "\">");
    // contents here
    buf.append("</" + NAME + ">");
    return buf.toString();
  }

}
