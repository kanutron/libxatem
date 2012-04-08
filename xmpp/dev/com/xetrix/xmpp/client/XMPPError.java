package com.xetrix.xmpp.client;

import org.xmlpull.v1.XmlPullParser;

public class XMPPError {
  // Class data
  private static final String DEFAULT_LANG =
    java.util.Locale.getDefault().getLanguage().toLowerCase();
  private static final String errorNamespace = "urn:ietf:params:xml:ns:xmpp-stanzas";

  // Error data
  private String by = null;
  private Type type = Type.CANCEL;
  private String condition = null;
  private String text = null;
  private String textLang = DEFAULT_LANG;

  // Constructors
  public XMPPError() {}
  public XMPPError(Type t, String c) {
    type = t;
    condition = c;
  }
  public XMPPError(Type t, String c, String tx) {
    type = t;
    condition = c;
    text = tx;
  }
  public XMPPError(XmlPullParser parser) throws Exception {
    parse(parser);
  }

  // Public methods
  public String getBy() {
    return by;
  }

  public void setBy(String b) {
    by = b;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type t) {
    type = t;
  }

  public String getCondition() {
    return condition;
  }

  public void setCondition(String c) {
    condition = c;
  }

  public String getText() {
    return text;
  }

  public void setText(String t) {
    text = t;
  }

  public String getTextLang() {
    return textLang;
  }

  public void setTextLang(String l) {
    textLang = l;
  }

  public String toXML() {
    StringBuilder buf = new StringBuilder();
    buf.append("<error type=\"").append(getType()).append("\"");
    if (by != null) {
      buf.append(" by=\"").append(by).append("\"");
    }
    buf.append(">");

    if (condition != null) {
      buf.append("<").append(condition);
      buf.append(" xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>");
    }

    if (text != null) {
      buf.append("<text xml:lang=\"").append(textLang);
      buf.append("\" xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">");
      buf.append(text);
      buf.append("</text>");
    }

    buf.append("</error>");
    return buf.toString();
  }

  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(getType()).append(": ");
    if (condition != null) {
      buf.append(condition);
    }
    if (text != null) {
      buf.append(": ").append(text);
    }
    if (by != null) {
      buf.append(" (by: ").append(by).append(")");
    }
    return buf.toString();
  }

  // Enums
  public enum Type {
    AUTH,
    CANCEL,
    CONTINUE,
    MODIFY,
    WAIT;

    public static Type fromString(String name) {
      try {
        return Type.valueOf(name.toUpperCase());
      }
      catch (Exception e) {
        return CANCEL;
      }
    }

    public String toString() {
      return super.toString().toLowerCase();
    }
  }

  // Private methods
  private void parse(XmlPullParser parser) throws Exception {
    if (!"error".equals(parser.getName())) {
      if ("failure".equals(parser.getName())) {
        parseFailureAsError(parser);
      }
      return;
    }

    // Parse the error header
    for (int i=0; i<parser.getAttributeCount(); i++) {
      if (parser.getAttributeName(i).equals("type")) {
        try {
          type = Type.valueOf(parser.getAttributeValue("", "type"));
        } catch (Exception e) {
        }
      }
      if (parser.getAttributeName(i).equals("by")) {
        by = parser.getAttributeValue("", "by");
      }
    }

    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        if (parser.getName().equals("text")) {
          textLang = XMPPStream.getLanguageAttribute(parser);
          text = parser.nextText();
        } else {
          String elementName = parser.getName();
          String namespace = parser.getNamespace();
          if (errorNamespace.equals(namespace)) {
            condition = elementName;
          } else {
            condition = namespace + ":" + elementName;
          }
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("error")) {
          return;
        }
      }
    }
  }

  private void parseFailureAsError(XmlPullParser parser) throws Exception {
    if (!"failure".equals(parser.getName())) {
      return;
    }

    String namespace = parser.getNamespace(null);
    if ("urn:ietf:params:xml:ns:xmpp-tls".equals(namespace)) {
      text = "TLS failure.";
    } else if ("http://jabber.org/protocol/compress".equals(namespace)) {
      text = "Compression failure.";
    } else if ("urn:ietf:params:xml:ns:xmpp-sasl".equals(namespace)) {
      text = "Authentication failure.";
    } else {
      text = "Unknown failure.";
    }

    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        condition = parser.getName();
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("failure")) {
          return;
        }
      }
    }
  }
}