package com.xetrix.xmpp.client;

public abstract class XMPPStanza {

  // Class data
  private static String DEFAULT_XMLNS = "jabber:client";
  protected static final String DEFAULT_LANG =
    java.util.Locale.getDefault().getLanguage().toLowerCase();

  // Common Stanza data
  private String     xmlns = DEFAULT_XMLNS;
  private String     from = null;
  private String     id = null;
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

  public abstract String toXML();

}