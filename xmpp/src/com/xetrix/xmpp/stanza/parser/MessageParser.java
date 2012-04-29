package com.xetrix.xmpp.stanza.parser;

import java.util.List;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.client.Stream;
import com.xetrix.xmpp.client.XMPPError;

import com.xetrix.xmpp.stanza.Stanza;
import com.xetrix.xmpp.stanza.Message;

import com.xetrix.xmpp.payload.parser.PayloadParser;

public class MessageParser implements StanzaParser {
  private static final String NAME = "message";
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
      stanza = parseMessage(parser);
      if (stanza instanceof Message) {
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

  private Stanza parseMessage(XmlPullParser parser) throws Exception {
    Iterator itr;
    Message msg = new Message(parser);
    boolean done = false;

    while (!done) {
      int eventType = parser.next();

      if (eventType == XmlPullParser.START_TAG) {
        if (parser.getName().equals("body")) {
          String bl = msg.getLanguageAttribute(parser);
          msg.setBody(bl, parser.nextText());
        } else if (parser.getName().equals("subject")) {
          String sl = msg.getLanguageAttribute(parser);
          msg.setSubject(sl, parser.nextText());
        } else if (parser.getName().equals("thread")) {
          String parent = parser.getAttributeValue(null, "parent");
          if (parent!=null) {
            msg.setParentThread(parent);
          }
          msg.setThread(parser.nextText());
        } else if (parser.getName().equals("error")) {
          msg.setError(new XMPPError(parser));
          msg.getError().setType(XMPPError.Type.CONTINUE); // Force type=continue
        } else {
          itr = payloadParsers.iterator();
          while(itr.hasNext()) {
            PayloadParser ep = (PayloadParser)itr.next();
            if (ep.wantsPayload(parser)) {
              if (!ep.parsePayload(parser)) {
                break;
              } else if (ep.hasPayload()) {
                msg.addPayload(ep.getPayload());
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
    return msg;
  }
}