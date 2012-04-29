package com.xetrix.xmpp.stanza.parser;

import java.util.List;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.client.Stream;
import com.xetrix.xmpp.client.XMPPError;

import com.xetrix.xmpp.stanza.Stanza;
import com.xetrix.xmpp.stanza.Presence;

import com.xetrix.xmpp.payload.parser.PayloadParser;

public class PresenceParser implements StanzaParser {
  private static final String NAME = "presence";
  private static final String XMLNS = "jabber:client";

  private List<PayloadParser> payloadParsers = new CopyOnWriteArrayList<PayloadParser>();

  private boolean hasStanza = false;
  private Stanza stanza = null;

  public boolean wantsStanza(XmlPullParser parser) throws Exception {
    // Only accept stanzas that are immediate child of <stream>
    if (parser.getDepth() != 2) {
      return false;
    }

    if (NAME.equals(parser.getName()) &&
        XMLNS.equals(parser.getNamespace())) {
      return true;
    }

    // Otherwise
    return false;
  }

  public boolean parseStanza(Stream stream, XmlPullParser parser) throws Exception {
    hasStanza = false;
    stanza = null;

    if (NAME.equals(parser.getName()) &&
        XMLNS.equals(parser.getNamespace())) {
      stanza = parsePresence(parser);
      if (stanza instanceof Presence) {
        hasStanza = true;
      }
      return true;
    }
    return false;
  }

  public boolean hasStanza() {
    return hasStanza;
  }

  public Stanza getStanza() {
    return stanza;
  }

  public boolean finished() {
    // Never finish
    return false;
  }

  public void addPayloadParser(PayloadParser p) {
    payloadParsers.add(p);
  }

  public void removePayloadParser(PayloadParser p) {
    payloadParsers.remove(p);
  }

  public void clearPayloadParsers() {
    payloadParsers.clear();
  }

  // Private methods

  private Stanza parsePresence(XmlPullParser parser) throws Exception {
    Iterator itr;
    Presence presence = new Presence(parser);
    boolean done = false;

    while (!done) {
      int eventType = parser.next();

      if (eventType == XmlPullParser.START_TAG) {
        if (parser.getName().equals("show")) {
          presence.setShow(parser.nextText());
        } else if (parser.getName().equals("priority")) {
          presence.setPriority(Integer.parseInt(parser.nextText()));
        } else if (parser.getName().equals("status")) {
          String l = presence.getLanguageAttribute(parser);
          presence.setStatus(l, parser.nextText());
        } else if (parser.getName().equals("error")) {
          presence.setError(new XMPPError(parser));
          presence.getError().setType(XMPPError.Type.CONTINUE); // Force type=continue
        } else {
          itr = payloadParsers.iterator();
          while(itr.hasNext()) {
            PayloadParser ep = (PayloadParser)itr.next();
            if (ep.wantsPayload(parser)) {
              if (!ep.parsePayload(parser)) {
                break;
              } else if (ep.hasPayload()) {
                presence.addPayload(ep.getPayload());
              }
              if (ep.finished()) {
                removePayloadParser(ep);
              }
              break; // Only first parser can process
            }
          }
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (NAME.equals(parser.getName()) &&
            XMLNS.equals(parser.getNamespace())) {
          done = true;
        }
      }
    }
    return presence;
  }
}