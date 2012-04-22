package com.xetrix.xmpp.stanza;

import java.util.List;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.client.Stream;
import com.xetrix.xmpp.client.XMPPError;
import com.xetrix.xmpp.payload.PayloadParser;

public class IQParser implements StanzaParser {
  private List<PayloadParser> payloadParsers = new CopyOnWriteArrayList<PayloadParser>();

  private boolean hasStanza = false;
  private Stanza stanza = null;

  private String e = "";
  private String n = "";

  public boolean wantsStanza(XmlPullParser parser) throws Exception {
    // Only accept stanzas that are immediate child of <stream>
    if (parser.getDepth() != 2) {
      return false;
    }

    e = parser.getName();
    n = parser.getNamespace();

    if (n.equals("jabber:client") &&
        e.equals("iq")) {
      return true;
    }

    // Otherwise
    return false;
  }

  public boolean parseStanza(Stream stream, XmlPullParser parser) throws Exception {
    hasStanza = false;
    stanza = null;

    if (e.equals("iq") && n.equals("jabber:client")) {
      stanza = parseIQ(parser);
      if (stanza instanceof IQ) {
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

  private Stanza parseIQ(XmlPullParser parser) throws Exception {
    Iterator itr;
    IQ iq = new IQ(parser);
    boolean done = false;

    while (!done) {
      int eventType = parser.next();

      if (eventType == XmlPullParser.START_TAG) {
        if (parser.getName().equals("error")) {
          iq.setError(new XMPPError(parser));
          iq.getError().setType(XMPPError.Type.CONTINUE); // Force type=continue
        } else {
          itr = payloadParsers.iterator();
          while(itr.hasNext()) {
            PayloadParser p = (PayloadParser)itr.next();
            if (p.wantsPayload(parser)) {
              if (!p.parsePayload(parser)) {
                break;
              } else if (p.hasPayload()) {
                iq.setPayload(p.getPayload());
              }
              if (p.finished()) {
                removePayloadParser(p);
              }
              break; // Only first parser can process
            }
          }
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        String e = parser.getName();
        String n = parser.getNamespace();
        if (e.equals("iq") && n.equals("jabber:client")) {
          done = true;
        }
      }
    }
    return iq;
  }
}