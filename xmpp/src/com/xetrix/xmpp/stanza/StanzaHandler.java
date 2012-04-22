package com.xetrix.xmpp.client;

import com.xetrix.xmpp.client.Stream;
import com.xetrix.xmpp.stanza.Stanza;
import org.xmlpull.v1.XmlPullParser;

public interface StanzaHandler {
  public boolean wantsStanza(XmlPullParser parser) throws Exception;
  public boolean handleStanza(Stream stream, XmlPullParser parser) throws Exception;
  public boolean hasStanza();
  public Stanza getStanza();
  public boolean finished();
}