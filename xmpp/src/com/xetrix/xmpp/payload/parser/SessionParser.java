package com.xetrix.xmpp.payload.parser;

import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.payload.Payload;
import com.xetrix.xmpp.payload.Session;

public class SessionParser implements PayloadParser {
  private static final String NAME = "session";
  private static final String XMLNS = "urn:ietf:params:xml:ns:xmpp-session";

  private boolean finished = false;
  private boolean hasPayload = false;
  private Payload payload = null;

  public boolean wantsPayload(XmlPullParser parser) throws Exception {
    // Only accept payloads that are immediate child of <iq|message|presence>
    if (parser.getDepth() != 3) {
      return false;
    }

    if (NAME.equals(parser.getName()) &&
        XMLNS.equals(parser.getNamespace())) {
      return true;
    }

    // Otherwise
    return false;
  }

  public boolean parsePayload(XmlPullParser parser) throws Exception {
    hasPayload = false;
    payload = null;

    if (NAME.equals(parser.getName()) &&
        XMLNS.equals(parser.getNamespace())) {
      payload = new Session();
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