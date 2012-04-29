package com.xetrix.xmpp.stanza;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.client.XMPPError;
import com.xetrix.xmpp.extension.Extension;
import com.xetrix.xmpp.util.StringUtils;

public class Presence extends Stanza {
  // Presence Stanza data
  private Type               type = Type.available;
  private Show               show = Show.available;
  private Map<String,String> statusmap = new HashMap<String,String>();
  private int                priority = 0;
  private List<Extension>    extensions = new CopyOnWriteArrayList<Extension>();

  // Constructors
  public Presence() {
    setName("presence");
  }
  public Presence(Presence pr) {
    super(pr);
    setType(pr.getType());
    setShow(pr.getShow());
    setPriority(pr.getPriority());
    setStatus(pr.getStatusMap());
  }
  public Presence(XmlPullParser parser) throws Exception {
    parse(parser);
  }
  public Presence(Type t) {
    setName("presence");
    setType(t);
  }
  public Presence(Type t, Show s) {
    setName("presence");
    setType(t);
    setShow(s);
  }
  public Presence(Type t, Show s, String status) {
    setName("presence");
    setType(t);
    setShow(s);
    setStatus(status);
  }
  public Presence(Type t, Show s, String status, int p) {
    setName("presence");
    setType(t);
    setShow(s);
    setStatus(status);
    setPriority(p);
  }

  // Public methods
  public Type getType() {
    return type;
  }

  public void setType(Type t) {
    if (t != null) {
      type = t;
    } else {
      type = Type.available;
    }
  }

  public void setType(String t) {
    try {
      if (t != null) {
        setType(Type.fromString(t));
      }
    } catch (Exception e) {
      type = Type.available;
    }
  }

  public Show getShow() {
    return show;
  }

  public void setShow(Show s) {
    if (s != null) {
      show = s;
    } else {
      show = Show.available;
    }
  }

  public void setShow(String s) {
    try {
      if (s != null) {
        setShow(Show.fromString(s));
      }
    } catch (Exception e) {
      show = Show.available;
    }
  }

  public Map<String,String> getStatusMap() {
    return statusmap;
  }

  public String getStatus(String lang) {
    if (statusmap.containsKey(lang)) {
      return statusmap.get(lang);
    }
    return "";
  }

  public String getStatus() {
    if (statusmap.containsKey(getLang())) {
      return statusmap.get(getLang());
    } else if (statusmap.containsKey(DEFAULT_LANG)) {
      return statusmap.get(DEFAULT_LANG);
    } else if (statusmap.size()>0) {
      for (String s : statusmap.values()) {
        return s;
      }
    }
    return "";
  }

  public void setStatus(Map<String,String> sm) {
    statusmap.putAll(sm);
  }

  public void setStatus(String status) {
    if (status!=null) {
      statusmap.put(getLang(), status);
    }
  }

  public void setStatus(String lang, String status) {
    if (status!=null) {
      if (lang == null) {
        statusmap.put(getLang(), status);
      } else {
        statusmap.put(lang, status);
      }
    }
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int p) {
    if (p < -128) {
      priority = -128;
    } else if (p > 127) {
      priority = 127;
    } else {
      priority = p;
    }
  }

  public boolean hasExtensions() {
    return extensions.size() > 0;
  }

  public void addExtension(Extension e) {
    extensions.add(e);
  }

  public void removeExtension(Extension e) {
    extensions.remove(e);
  }

  public void clearExtensions() {
    extensions.clear();
  }

  public String toXML() {
    StringBuilder buf = new StringBuilder();
    buf.append("<presence");
    if (getType() != Type.available) {
      buf.append(" type=\"" + getType() + "\"");
    }
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

    // Contents
    if (getPriority() != 0) {
      buf.append("<priority>").append(getPriority()).append("</priority>");
    }
    if (getShow() != Show.available) {
      buf.append("<show>").append(getShow()).append("</show>");
    }

    if (getStatusMap().size() > 0) {
      Iterator it = getStatusMap().entrySet().iterator();
      String defaultStatus = "";
      while (it.hasNext()) {
        Map.Entry e = (Map.Entry)it.next();
        if (e.getKey() == getLang()) {
          defaultStatus = StringUtils.escapeForXML((String)e.getValue());
        } else {
          buf.append("<status xml:lang=\"");
          buf.append(StringUtils.escapeForXML((String)e.getKey()));
          buf.append("\">");
          buf.append(StringUtils.escapeForXML((String)e.getValue()));
          buf.append("</status>");
        }
      }
      // Default status goes last to prevent buggy clients take another one.
      if (!defaultStatus.equals("")) {
        buf.append("<status>").append(defaultStatus).append("</status>");
      }
    }

    // Add the extensions XML if there are at least one.
    if (hasExtensions()) {
      buf.append(getExtensionsXML());
    }

    // Add the error sub-packet, if there is one.
    if (getError() != null) {
      buf.append(getError().toXML());
    }

    buf.append("</presence>");
    return buf.toString();
  }


  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("PRS [");
    buf.append(getId());
    buf.append("] ");
    buf.append(getFrom());
    buf.append(" ");
    buf.append(getType());
    buf.append(" ");
    buf.append(getShow());
    buf.append(" ");
    buf.append(getPriority());
    buf.append(" ");
    buf.append(getStatus());
    return buf.toString();
  }

  public String getExtensionsXML() {
    StringBuilder buf = new StringBuilder();
    for (Extension e: extensions) {
      buf.append(e.toXML());
    }
    return buf.toString();
  }

  public void parse(XmlPullParser parser) throws Exception {
    super.parse(parser);
    setType(parser.getAttributeValue(null, "type"));
  }

  // Enums
  public enum Type {
    error,
    probe,
    subscribe,
    subscribed,
    available,
    unavailable,
    unsubscribe,
    unsubscribed;

    public static Type fromString(String name) {
      try {
        return Type.valueOf(name);
      }
      catch (Exception e) {
        return available;
      }
    }

    public String toString() {
      return super.toString();
    }
  }

  public enum Show {
    away,
    chat,
    dnd,
    xa,
    available;

    public static Show fromString(String name) {
      try {
        return Show.valueOf(name);
      }
      catch (Exception e) {
        return available;
      }
    }

    public String toString() {
      return super.toString();
    }
  }

}