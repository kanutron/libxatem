package com.xetrix.xmpp.payload;

import org.xmlpull.v1.XmlPullParser;

public class Bind extends Payload {
  private static String NAME = "bind";
  private static String XMLNS = "urn:ietf:params:xml:ns:xmpp-bind";

  // Bind data
  private String resource = null;
  private String jid = null;

  // Constructors
  public Bind() {}
  public Bind(String j) {
    setJid(j);
  }
  public Bind(String j, String r) {
    setJid(j);
    setResource(r);
  }
  public Bind(XmlPullParser parser) throws Exception {
    parse(parser);
  }

  // Public methods
  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    if (resource != null) {
      resource = resource;
    }
  }

  public String getJid() {
    return jid;
  }

  public void setJid(String j) {
    if (j.indexOf("/")>0) {
      jid = j.substring(0, j.indexOf("/")); // JID part
      resource = j.substring(j.indexOf("/")+1); // Resource part
    } else {
      jid = j;
    }
  }

  public String getName() {
    return NAME;
  }

  public String getXmlns() {
    return XMLNS;
  }

  public String toXML() {
    StringBuilder buf = new StringBuilder();
    buf.append("<" + NAME + " xmlns=\"" + XMLNS + "\">");
    if (resource != null) {
      buf.append("<resource>").append(resource).append("</resource>");
    }
    if (jid != null) {
      buf.append("<jid>").append(jid).append("</jid>");
    }
    buf.append("</" + NAME + ">");
    return buf.toString();
  }

  public void parse(XmlPullParser parser) throws Exception {
    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        if (parser.getName().equals("resource")) {
          setResource(parser.nextText());
        } else if (parser.getName().equals("jid")) {
          setJid(parser.nextText());
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals(NAME) &&
            parser.getNamespace().equals(XMLNS)) {
          return;
        }
      }
    }
  }

}
