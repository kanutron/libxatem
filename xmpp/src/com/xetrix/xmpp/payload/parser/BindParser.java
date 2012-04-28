package com.xetrix.xmpp.payload.parser;

import org.xmlpull.v1.XmlPullParser;
import com.xetrix.xmpp.payload.Payload;
import com.xetrix.xmpp.payload.Bind;

public class BindParser implements PayloadParser {
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

    if (n.equals("urn:ietf:params:xml:ns:xmpp-bind") &&
        e.equals("bind")) {
      return true;
    }

    // Otherwise
    return false;
  }

  public boolean parsePayload(XmlPullParser parser) throws Exception {
    hasPayload = false;
    payload = null;

    if (e.equals("bind") && n.equals("urn:ietf:params:xml:ns:xmpp-bind")) {
      payload = new Bind(parser);
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