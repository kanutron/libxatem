package com.xetrix.xmpp.stanza;

import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.client.XMPPError;
import com.xetrix.xmpp.payload.Payload;
import com.xetrix.xmpp.util.StringUtils;

public class IQ extends Stanza {
  // IQ Stanza data
  private Type       type = Type.get;
  private Payload    payload = null;
  private boolean    hasPayload = false;

  // Constructors
  public IQ() {
    setName("iq");
  }
  public IQ(IQ iq) {
    super(iq);
    setType(iq.getType());
    setPayload(iq.getPayload());
  }
  public IQ(XmlPullParser parser) throws Exception {
    parse(parser);
  }
  public IQ(Type t, Payload p) {
    setName("iq");
    setType(t);
    setPayload(p);
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

  public Payload getPayload() {
    return payload;
  }

  public void setPayload(Payload p) {
    if (p instanceof Payload || p == null) {
      payload = p;
      hasPayload = true;
    }
  }

  public boolean hasPayload() {
    return hasPayload;
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

  public IQ toErrorIQ(XMPPError e) {
    IQ iq = new IQ(this);
    iq.setFrom(getTo());
    iq.setTo(getFrom());
    iq.setType(Type.error);
    iq.setError(e);
    return iq;
  }

  public String getPayloadXML() {
    if (payload instanceof Payload) {
      return payload.toXML();
    }
    return null;
  }

  public void parse(XmlPullParser parser) throws Exception {
    super.parse(parser);
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