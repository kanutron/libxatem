package com.xetrix.xmpp.stanza;

import org.xmlpull.v1.XmlPullParser;

public interface StanzaListener {
  public boolean wantsStanza(Stanza s);
  public boolean processStanza(Stanza s);
  public boolean finished();
}