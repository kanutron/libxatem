package com.xetrix.xmpp.client;

public abstract class XMPPStanzaIQ extends XMPPStanza {

  // Class data

  // IQ Stanza data
  private Type type = Type.get;

  // Constructors
  public XMPPStanzaIQ() {}
  public XMPPStanzaIQ(Type t) {
    type = t;
  }
  public XMPPStanzaIQ(String id, XMPPError e) {
    setId(id);
    setError(e);
    type = Type.error;
  }

  // Public methods
  public Type getType() {
    return type;
  }

  public void setType(Type t) {
    if (t != null) {
      type = t;
    }
  }

  public String toXML() {
    StringBuilder buf = new StringBuilder();
    buf.append("<iq ");
    buf.append("type=\"").append(type).append("\"");
    if (getId() != null) {
      buf.append(" id=\"" + getId() + "\"");
    }
    if (getTo() != null) {
      buf.append(" to=\"").append(StringUtils.escapeForXML(getTo())).append("\"");
    }
    if (getFrom() != null) {
      buf.append(" from=\"").append(StringUtils.escapeForXML(getFrom())).append("\"");
    }
    buf.append(">");

    // Add the payload section if there is one.
    String payloadXML = getPayloadXML();
    if (payloadXML != null) {
      buf.append(payloadXML);
    }

    // Add the error sub-packet, if there is one.
    if (type == Type.error &&
        getError() != null) {
      buf.append(getError().toXML());
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