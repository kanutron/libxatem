package com.xetrix.xmpp.stanza;

import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.client.XMPPError;

public abstract class Stanza {

  // Class data
  protected static String DEFAULT_XMLNS = "jabber:client";
  protected static final String DEFAULT_LANG =
    java.util.Locale.getDefault().getLanguage().toLowerCase();

  // Common Stanza data
  private String     xmlns = DEFAULT_XMLNS;
  private String     name = null;
  private String     id = null;
  private String     from = null;
  private String     to = null;
  private String     lang = DEFAULT_LANG;
  private XMPPError  error = null;

  // Constructors
  public Stanza() {}
  public Stanza(XmlPullParser parser) throws Exception {
    parse(parser);
  }
  public Stanza(Stanza stanza) {
    setXmlns(stanza.getXmlns());
    setName(stanza.getName());
    setId(stanza.getId());
    setFrom(stanza.getFrom());
    setTo(stanza.getTo());
    setLang(stanza.getLang());
    setError(stanza.getError());
  }

  // Public methods
  public static String getDefaultLanguage() {
    return DEFAULT_LANG;
  }

  public String getXmlns() {
    return xmlns;
  }

  public void setXmlns(String ns) {
    if (ns != null) {
      xmlns = ns;
    }
  }

  public String getName() {
    return name;
  }

  public void setName(String n) {
    name = n;
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

  public void setTo(String t) {
    to = t;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String f) {
    from = f;
  }

  public String getLang() {
    return lang;
  }

  public void setLang(String l) {
    if (l != null) {
      lang = l;
    }
  }

  public XMPPError getError() {
    return error;
  }

  public void setError(XMPPError e) {
    error = e;
  }

  public void parse(XmlPullParser parser) throws Exception {
    setName(parser.getName());
    setId(parser.getAttributeValue(null, "id"));
    setFrom(parser.getAttributeValue(null, "from"));
    setTo(parser.getAttributeValue(null, "to"));
    setLang(getLanguageAttribute(parser));
    setXmlns(parser.getNamespace(null));
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