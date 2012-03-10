package com.xetrix.xmpp.client;

public abstract class XMPPStanza {

  // Class data
  private static String DEFAULT_XMLNS = "jabber:client";
  protected static final String DEFAULT_LANG =
    java.util.Locale.getDefault().getLanguage().toLowerCase();

  // Common Stanza data
  private String xmlns = DEFAULT_XMLNS;
  private String from = null;
  private String id = null;
  private String to = null;
  private String lang = DEFAULT_LANG;

  // Constructors
  public XMPPStanza() {}

  public static String getDefaultLanguage() {
    return DEFAULT_LANG;
  }


}