package com.xetrix.xmpp.client;

import org.xmlpull.v1.XmlPullParser;

public class XMPPStanzaIQ extends XMPPStanza {

  // IQ Stanza data
  private Type type = Type.get;

  // Constructors
  public XMPPStanzaIQ() {}
  public XMPPStanzaIQ(XMPPStanzaIQ iq) throws Exception {
    super(iq);
    setType(iq.getType());
  }
  public XMPPStanzaIQ(XmlPullParser parser) throws Exception {
    parseStanza(parser);
    parseStanzaIQ(parser);
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
  public void setType(String t) {
    try {
      if (t != null) {
        setType(Type.fromString(t));
      }
    } catch (Exception e) {
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

  public XMPPStanzaIQ toErrorIQ(XMPPError e) throws Exception {
    XMPPStanzaIQ iq = new XMPPStanzaIQ(this);
    iq.setFrom(getTo());
    iq.setTo(getFrom());
    iq.setType(Type.error);
    iq.setError(e);
    return iq;
  }

  public String getPayloadXML() {
    return null;
  }

  public final void parseStanzaIQ(XmlPullParser parser) throws Exception {
    if (!"iq".equals(parser.getName())) {
      return;
    }

    setType(parser.getAttributeValue(null, "type"));
  }

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