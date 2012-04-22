package com.xetrix.xmpp.stanza;

import org.xmlpull.v1.XmlPullParser;
import com.xetrix.xmpp.client.Stream;
import com.xetrix.xmpp.client.XMPPError;
import com.xetrix.xmpp.payload.PayloadParser;
import com.xetrix.xmpp.payload.Bind; // TO BE REMOVED
import com.xetrix.xmpp.payload.Session; // TO BE REMOVED

public class IQParser implements StanzaParser {
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

  // Private methods

  private Stanza parseIQ(XmlPullParser parser) throws Exception {
    IQ iq = new IQ(parser);

    boolean done = false;
    while (!done) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        String e = parser.getName();
        String n = parser.getNamespace();

        // IQ Payload
        // TODO User payloadparsers!
        if (e.equals("error")) {
          iq.setError(new XMPPError(parser));
        } else if (e.equals("bind") && n.equals("urn:ietf:params:xml:ns:xmpp-bind")) {
          iq.setPayload(new Bind(parser));
        } else if (e.equals("session") && n.equals("urn:ietf:params:xml:ns:xmpp-session")) {
          iq.setPayload(new Session(parser));
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

  /*
    // TODO: if IQ error, notify listener (continue)

    // If here, the IQ was unhandled.
    if (iq.getType() == IQ.Type.get || iq.getType() == IQ.Type.set) {
      IQ eiq = iq.toErrorIQ(
        new XMPPError(XMPPError.Type.CANCEL, "feature-not-implemented"));
      pushStanza(eiq);
      return;
    } else {
      // TODO: Unhandled ERROR or RESULT IQ received.
    }
  */
}