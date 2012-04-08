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
    return this.xmlns;
  }

  public void setXmlns(String ns) {
    this.xmlns = ns;
  }

  public String getId() {
    return this.id;
  }

  public void setId(String i) {
    this.id = i;
  }

  public String getTo() {
    return this.to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public String getFrom() {
    return this.from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getLang() {
    return this.lang;
  }

  public void setLang(String lang) {
    this.lang = lang;
  }

  public XMPPError getError() {
    return this.error;
  }

  public void setError(XMPPError e) {
    this.error = e;
  }

  public abstract String toXML();

}