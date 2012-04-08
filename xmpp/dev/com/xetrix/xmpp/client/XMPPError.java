package com.xetrix.xmpp.client;

public class XMPPError {
  // Class data
  protected static final String DEFAULT_LANG =
    java.util.Locale.getDefault().getLanguage().toLowerCase();

  // Error data
  private String by = null;
  private Type type = Type.CANCEL;
  private String condition = null;
  private String text = null;
  private String textLang = DEFAULT_LANG;

  // Constructors
  public XMPPError() {}
  public XMPPError(Type t, String c) {
    this.type = t;
    this.condition = c;
  }
  public XMPPError(Type t, String c, String text) {
    this.type = t;
    this.condition = c;
    this.text = text;
  }

  // Public methods
  public String getBy() {
    return this.by;
  }

  public void setBy(String by) {
    this.by = by;
  }

  public Type getType() {
    return this.type;
  }

  public void setType(Type t) {
    this.type = t;
  }

  public String getCondition() {
    return this.condition;
  }

  public void setCondition(String c) {
    this.condition = c;
  }

  public String getText() {
    return this.text;
  }

  public void setText(String t) {
    this.text = t;
  }

  public String getTextLang() {
    return this.textLang;
  }

  public void setTextLang(String l) {
    this.textLang = l;
  }

  public String toXML() {
    StringBuilder buf = new StringBuilder();
    buf.append("<error type=\"").append(this.getType()).append("\"");
    if (this.by != null) {
      buf.append(" by=\"").append(this.by).append("\"");
    }
    buf.append(">");

    if (this.condition != null) {
      buf.append("<").append(this.condition);
      buf.append(" xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>");
    }

    if (this.text != null) {
      buf.append("<text xml:lang=\"").append(this.textLang);
      buf.append("\" xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">");
      buf.append(this.text);
      buf.append("</text>");
    }

    buf.append("</error>");
    return buf.toString();
  }

  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(this.getType()).append(": ");
    if (this.condition != null) {
      buf.append(this.condition.toUpperCase());
    }
    if (this.text != null) {
      buf.append(": ").append(this.text);
    }
    if (this.by != null) {
      buf.append(" (by: ").append(this.by).append(")");
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

}