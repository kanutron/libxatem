package com.xetrix.xmpp.payload.parser;

import org.xmlpull.v1.XmlPullParser;
import com.xetrix.xmpp.payload.Payload;
import com.xetrix.xmpp.payload.Session;

public class SessionParser implements PayloadParser {
  private boolean finished = false;
  private boolean hasPayload = false;
  private Payload payload = null;

  private String e = "";
  private String n = "";

  public boolean wantsPayload(XmlPullParser parser) throws Exception {
    // Only accept payloads that are immediate child of <iq|message|presence>
    if (parser.getDepth() != 3) {
      return false;
    }

    e = parser.getName();
    n = parser.getNamespace();

    if (n.equals("urn:ietf:params:xml:ns:xmpp-session") &&
        e.equals("session")) {
      return true;
    }

    // Otherwise
    return false;
  }

  public boolean parsePayload(XmlPullParser parser) throws Exception {
    hasPayload = false;
    payload = null;

    if (e.equals("session") && n.equals("urn:ietf:params:xml:ns:xmpp-session")) {
      payload = new Session(parser);
      hasPayload = true;
      finished = true;
      return true;
    }
    return false;
  }

  public boolean hasPayload() {
    return hasPayload;
  }

  public Payload getPayload() {
    return payload;
  }

  public boolean finished() {
    return finished;
  }
}