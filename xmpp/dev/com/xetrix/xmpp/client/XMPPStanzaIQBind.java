package com.xetrix.xmpp.client;

public class XMPPStanzaIQBind extends XMPPStanzaIQ {

  // Bind data
  private String resource = null;
  private String jid = null;

  // Constructors
  public XMPPStanzaIQBind() {
    this.setType(Type.set);
  }
  public XMPPStanzaIQBind(String j) {
    this.setType(Type.set);
    this.setJid(j);
  }
  public XMPPStanzaIQBind(String j, String r) {
    this.setType(Type.set);
    this.setJid(j);
    this.setResource(r);
  }

  // Public methods
  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    if (resource != null) {
      this.resource = resource;
    }
  }

  public String getJid() {
    return jid;
  }

  public void setJid(String j) {
    if (j.indexOf("/")>0) {
      this.jid = j.substring(0, j.indexOf("/")); // JID part
      this.resource = j.substring(j.indexOf("/")+1); // Resource part
    } else {
      this.jid = j;
    }
  }

  public String getPayloadXML() {
    StringBuilder buf = new StringBuilder();
    buf.append("<bind xmlns=\"urn:ietf:params:xml:ns:xmpp-bind\">");
    if (resource != null) {
      buf.append("<resource>").append(this.resource).append("</resource>");
    }
    if (jid != null) {
      buf.append("<jid>").append(this.jid).append("</jid>");
    }
    buf.append("</bind>");
    return buf.toString();
  }
}
