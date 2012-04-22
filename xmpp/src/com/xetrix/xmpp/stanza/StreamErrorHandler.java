package com.xetrix.xmpp.stanza;

import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.client.Stream;
import com.xetrix.xmpp.client.StreamListener;
import com.xetrix.xmpp.client.XMPPError;

public class StreamErrorHandler implements StanzaHandler {
  private String e = "";
  private String n = "";

  public boolean wantsStanza(XmlPullParser parser) throws Exception {
    // Only accept stanzas that are immediate child of <stream>
    if (parser.getDepth() != 2) {
      return false;
    }

    e = parser.getName();
    n = parser.getNamespace();

    if (n.equals("http://etherx.jabber.org/streams") &&
        e.equals("error")) {
      return true;
    }

    // Otherwise
    return false;
  }

  public boolean handleStanza(Stream stream, XmlPullParser parser) throws Exception {
    if (n.equals("http://etherx.jabber.org/streams") &&
        e.equals("error")) {
      StreamListener l = stream.getListener();
      l.onStreamError(new XMPPError(parser));
      return false;
    }
    return true;
  }

  public boolean hasStanza() {
    // Never return an stanza representation
    return false;
  }


  public Stanza getStanza() {
    // Never return an stanza representation
    return null;
  }

  public boolean finished() {
    // Never finish
    return false;
  }

}