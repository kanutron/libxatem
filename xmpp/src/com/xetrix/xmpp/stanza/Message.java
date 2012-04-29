package com.xetrix.xmpp.stanza;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.client.XMPPError;
import com.xetrix.xmpp.payload.Payload;
import com.xetrix.xmpp.util.StringUtils;

public class Message extends Stanza {
  // Message Stanza data
  private Type               type = Type.normal;
  private Map<String,String> subjects = new HashMap<String,String>();
  private Map<String,String> bodies = new HashMap<String,String>();
  private String             thread = null;
  private String             parentThread = null;
  private List<Payload>      payloads = new CopyOnWriteArrayList<Payload>();

  // Constructors
  public Message() {
    setName("message");
  }
  public Message(Message msg) {
    super(msg);
    setType(msg.getType());
    setSubject(msg.getSubjectsMap());
    setBody(msg.getBodiesMap());
    setThread(msg.getThread());
    setParentThread(msg.getParentThread());
    setPayloads(msg.getPayloads());
  }
  public Message(XmlPullParser parser) throws Exception {
    parse(parser);
  }
  public Message(Type t) {
    setName("message");
    setType(t);
  }
  public Message(Type t, String b) {
    setName("message");
    setType(t);
    setBody(b);
  }
  public Message(Type t, String b, String s) {
    setName("message");
    setType(t);
    setBody(b);
    setSubject(s);
  }

  // Public methods
  public Type getType() {
    return type;
  }

  public void setType(Type t) {
    if (t != null) {
      type = t;
    } else {
      type = Type.normal;
    }
  }

  public void setType(String t) {
    try {
      if (t != null) {
        setType(Type.fromString(t));
      }
    } catch (Exception e) {
      type = Type.normal;
    }
  }

  public Map<String,String> getSubjectsMap() {
    return subjects;
  }

  public String getSubject(String lang) {
    if (subjects.containsKey(lang)) {
      return subjects.get(lang);
    }
    return "";
  }

  public String getSubject() {
    if (subjects.containsKey(getLang())) {
      return subjects.get(getLang());
    } else if (subjects.containsKey(DEFAULT_LANG)) {
      return subjects.get(DEFAULT_LANG);
    } else if (subjects.size()>0) {
      for (String s : subjects.values()) {
        return s;
      }
    }
    return "";
  }

  public void setSubject(Map<String,String> sm) {
    subjects.putAll(sm);
  }

  public void setSubject(String subject) {
    if (subject!=null) {
      subjects.put(getLang(), subject);
    }
  }

  public void setSubject(String lang, String subject) {
    if (subject!=null) {
      if (lang == null) {
        subjects.put(getLang(), subject);
      } else {
        subjects.put(lang, subject);
      }
    }
  }

  public Map<String,String> getBodiesMap() {
    return bodies;
  }

  public String getBody(String lang) {
    if (bodies.containsKey(lang)) {
      return bodies.get(lang);
    }
    return "";
  }

  public String getBody() {
    if (bodies.containsKey(getLang())) {
      return bodies.get(getLang());
    } else if (bodies.containsKey(DEFAULT_LANG)) {
      return bodies.get(DEFAULT_LANG);
    } else if (bodies.size()>0) {
      for (String s : bodies.values()) {
        return s;
      }
    }
    return "";
  }

  public void setBody(Map<String,String> sm) {
    bodies.putAll(sm);
  }

  public void setBody(String body) {
    if (body!=null) {
      bodies.put(getLang(), body);
    }
  }

  public void setBody(String lang, String body) {
    if (body!=null) {
      if (lang == null) {
        bodies.put(getLang(), body);
      } else {
        bodies.put(lang, body);
      }
    }
  }

  public String getThread() {
    return thread;
  }

  public void setThread(String t) {
    thread = t;
  }

  public String getParentThread() {
    return parentThread;
  }

  public void setParentThread(String t) {
    parentThread = t;
  }

  public boolean hasPayloads() {
    return payloads.size() > 0;
  }

  public void addPayload(Payload e) {
    payloads.add(e);
  }

  public void removePayload(Payload e) {
    payloads.remove(e);
  }

  public void clearPayloads() {
    payloads.clear();
  }

  public void setPayloads(List<Payload> ps) {
    payloads = ps;
  }

  public List<Payload> getPayloads() {
    return payloads;
  }

  public String toXML() {
    StringBuilder buf = new StringBuilder();
    buf.append("<message");
    if (getType() != Type.normal) {
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

    // Subjects
    if (getSubjectsMap().size() > 0) {
      Iterator it = getSubjectsMap().entrySet().iterator();
      String defaultSubject = "";
      while (it.hasNext()) {
        Map.Entry e = (Map.Entry)it.next();
        if (e.getKey() == getLang()) {
          defaultSubject = StringUtils.escapeForXML((String)e.getValue());
        } else {
          buf.append("<subject xml:lang=\"");
          buf.append(StringUtils.escapeForXML((String)e.getKey()));
          buf.append("\">");
          buf.append(StringUtils.escapeForXML((String)e.getValue()));
          buf.append("</subject>");
        }
      }
      // Default subject goes last to prevent buggy clients take another one.
      if (!defaultSubject.equals("")) {
        buf.append("<subject>").append(defaultSubject).append("</subject>");
      }
    }

    // Bodies
    if (getBodiesMap().size() > 0) {
      Iterator it = getBodiesMap().entrySet().iterator();
      String defaultBody = "";
      while (it.hasNext()) {
        Map.Entry e = (Map.Entry)it.next();
        if (e.getKey() == getLang()) {
          defaultBody = StringUtils.escapeForXML((String)e.getValue());
        } else {
          buf.append("<body xml:lang=\"");
          buf.append(StringUtils.escapeForXML((String)e.getKey()));
          buf.append("\">");
          buf.append(StringUtils.escapeForXML((String)e.getValue()));
          buf.append("</body>");
        }
      }
      // Default body goes last to prevent buggy clients take another one.
      if (!defaultBody.equals("")) {
        buf.append("<body>").append(defaultBody).append("</body>");
      }
    }

    // Thread
    if (getThread() != null) {
      buf.append("<thread");
      if (getParentThread() != null) {
        buf.append(" parent=\"");
        buf.append(getParentThread());
        buf.append("\">");
      } else {
        buf.append(">");
      }
      buf.append(getThread());
      buf.append("</thread>");
    }

    // Add the payloads XML if there are at least one.
    if (hasPayloads()) {
      buf.append(getPayloadsXML());
    }

    // Add the error sub-packet, if there is one.
    if (getError() != null) {
      buf.append(getError().toXML());
    }

    buf.append("</message>");
    return buf.toString();
  }

  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("MSG [");
    buf.append(getId());
    buf.append("] ");
    buf.append(getFrom());
    buf.append(" ");
    buf.append(getType());
    buf.append(" ");
    buf.append(getThread());
    buf.append(" ");
    buf.append(getSubject());
    buf.append("\n");
    buf.append(getBody());
    return buf.toString();
  }

  public String getPayloadsXML() {
    StringBuilder buf = new StringBuilder();
    for (Payload e: payloads) {
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
    chat,
    error,
    groupchat,
    headline,
    normal;

    public static Type fromString(String name) {
      try {
        return Type.valueOf(name);
      }
      catch (Exception e) {
        return normal;
      }
    }

    public String toString() {
      return super.toString();
    }
  }

}