package com.xetrix.xmpp.client;

import org.xmlpull.v1.XmlPullParser;

public abstract class XMPPStanza {

  // Class data
  private static String DEFAULT_XMLNS = "jabber:client";
  protected static final String DEFAULT_LANG =
    java.util.Locale.getDefault().getLanguage().toLowerCase();

  // Common Stanza data
  private String     xmlns = DEFAULT_XMLNS;
  private String     id = null;
  private String     from = null;
  private String     to = null;
  private String     lang = DEFAULT_LANG;
  private XMPPError  error = null;

  // Constructors
  public XMPPStanza() {}

  // Public methods
  public static String getDefaultLanguage() {
    return DEFAULT_LANG;
  }

  public String getXmlns() {
    return xmlns;
  }

  public void setXmlns(String ns) {
    xmlns = ns;
  }

  public String getId() {
    return id;
  }

  public void setId(String i) {
    id = i;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    to = to;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    from = from;
  }

  public String getLang() {
    return lang;
  }

  public void setLang(String lang) {
    lang = lang;
  }

  public XMPPError getError() {
    return error;
  }

  public void setError(XMPPError e) {
    error = e;
  }

  public static void parse(XmlPullParser parser, XMPPStanza stanza) throws Exception {
    if (!"presence".equals(parser.getName()) &&
        !"message".equals(parser.getName()) &&
        !"iq".equals(parser.getName())) {
      return;
    }

    stanza.setId(parser.getAttributeValue(null, "id"));
    stanza.setFrom(parser.getAttributeValue(null, "from"));
    stanza.setTo(parser.getAttributeValue(null, "to"));
    stanza.setLang(getLanguageAttribute(parser));
    if (parser.getNamespace(null) != null) {
      stanza.setXmlns(parser.getNamespace(null));
    }
  }

  public static String getLanguageAttribute(XmlPullParser parser) {
    for (int i = 0; i < parser.getAttributeCount(); i++) {
      String attributeName = parser.getAttributeName(i);
      if ("xml:lang".equals(attributeName) || ("lang".equals(attributeName) &&
          "xml".equals(parser.getAttributePrefix(i)))) {
        return parser.getAttributeValue(i);
      }
    }
    return null;
  }

  public abstract String toXML();


}