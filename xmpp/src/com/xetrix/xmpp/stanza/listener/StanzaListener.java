package com.xetrix.xmpp.stanza.listener;

import org.xmlpull.v1.XmlPullParser;
import com.xetrix.xmpp.stanza.Stanza;

public interface StanzaListener {
  public boolean wantsStanza(Stanza s);
  public boolean processStanza(Stanza s);
  public boolean finished();
}