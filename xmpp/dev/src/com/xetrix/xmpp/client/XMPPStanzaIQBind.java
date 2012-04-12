package com.xetrix.xmpp.client;

import org.xmlpull.v1.XmlPullParser;

public class XMPPStanzaIQBind extends XMPPStanzaIQ {

  // Bind data
  private String resource = null;
  private String jid = null;

  // Constructors
  public XMPPStanzaIQBind() {
    setType(Type.set);
  }
  public XMPPStanzaIQBind(String j) {
    setType(Type.set);
    setJid(j);
  }
  public XMPPStanzaIQBind(String j, String r) {
    setType(Type.set);
    setJid(j);
    setResource(r);
  }
  public XMPPStanzaIQBind(XmlPullParser parser, XMPPStanzaIQ iq) throws Exception {
    super(iq);
    parseStanzaIQBind(parser);
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

  public String getPayloadXML() {
    StringBuilder buf = new StringBuilder();
    buf.append("<bind xmlns=\"urn:ietf:params:xml:ns:xmpp-bind\">");
    if (resource != null) {
      buf.append("<resource>").append(resource).append("</resource>");
    }
    if (jid != null) {
      buf.append("<jid>").append(jid).append("</jid>");
    }
    buf.append("</bind>");
    return buf.toString();
  }

  // Private mehtods
  public final void parseStanzaIQBind(XmlPullParser parser) throws Exception {
    if (!"bind".equals(parser.getName())) {
      return;
    }
    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        if (parser.getName().equals("resource")) {
          setResource(parser.nextText());
        } else if (parser.getName().equals("jid")) {
          setJid(parser.nextText());
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("bind")) {
          return;
        }
      }
    }
  }

}
