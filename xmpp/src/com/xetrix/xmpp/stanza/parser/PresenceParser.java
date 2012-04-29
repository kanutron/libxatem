package com.xetrix.xmpp.stanza.parser;

import java.util.List;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.client.Stream;
import com.xetrix.xmpp.client.XMPPError;

import com.xetrix.xmpp.stanza.Stanza;
import com.xetrix.xmpp.stanza.Presence;

import com.xetrix.xmpp.extension.parser.ExtensionParser;

public class PresenceParser implements StanzaParser {
  private static final String NAME = "presence";
  private static final String XMLNS = "jabber:client";

  private List<ExtensionParser> extensionParsers = new CopyOnWriteArrayList<ExtensionParser>();

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

  public void addExtensionParser(ExtensionParser p) {
    extensionParsers.add(p);
  }

  public void removeExtensionParser(ExtensionParser p) {
    extensionParsers.remove(p);
  }

  public void clearExtensionParsers() {
    extensionParsers.clear();
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
          itr = extensionParsers.iterator();
          while(itr.hasNext()) {
            ExtensionParser ep = (ExtensionParser)itr.next();
            if (ep.wantsExtension(parser)) {
              if (!ep.parseExtension(parser)) {
                break;
              } else if (ep.hasExtension()) {
                presence.addExtension(ep.getExtension());
              }
              if (ep.finished()) {
                removeExtensionParser(ep);
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