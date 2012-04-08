package com.xetrix.xmpp.client;

public abstract class XMPPStanzaIQ extends XMPPStanza {

  // Class data

  // IQ Stanza data
  private Type type = Type.get;

  // Constructors
  public XMPPStanzaIQ() {}
  public XMPPStanzaIQ(Type t) {
    this.type = t;
  }
  public XMPPStanzaIQ(String id, XMPPError e) {
    this.setId(id);
    this.setError(e);
    this.type = Type.error;
  }

  // Public methods
  public Type getType() {
    return this.type;
  }

  public void setType(Type t) {
    if (t != null) {
      this.type = t;
    }
  }

  public String toXML() {
    StringBuilder buf = new StringBuilder();
    buf.append("<iq ");
    buf.append("type=\"").append(this.type).append("\"");
    if (this.getId() != null) {
      buf.append(" id=\"" + this.getId() + "\"");
    }
    if (this.getTo() != null) {
      buf.append(" to=\"").append(StringUtils.escapeForXML(this.getTo())).append("\"");
    }
    if (this.getFrom() != null) {
      buf.append(" from=\"").append(StringUtils.escapeForXML(this.getFrom())).append("\"");
    }
    buf.append(">");

    // Add the payload section if there is one.
    String payloadXML = getPayloadXML();
    if (payloadXML != null) {
      buf.append(payloadXML);
    }

    // Add the error sub-packet, if there is one.
    if (this.type == Type.error &&
        this.getError() != null) {
      buf.append(this.getError().toXML());
    }

    buf.append("</iq>");
    return buf.toString();
  }

  public abstract String getPayloadXML();

  // Enums
  public enum Type {
    get,
    set,
    result,
    error;

    public static Type fromString(String name) {
      try {
        return Type.valueOf(name);
      }
      catch (Exception e) {
        return get;
      }
    }

    public String toString() {
      return super.toString();
    }
  }
}