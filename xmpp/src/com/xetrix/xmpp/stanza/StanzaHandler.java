package com.xetrix.xmpp.stanza;

import com.xetrix.xmpp.client.Stream;
import org.xmlpull.v1.XmlPullParser;

public interface StanzaHandler {
  public boolean wantsStanza(XmlPullParser parser) throws Exception;
  public boolean handleStanza(Stream stream, XmlPullParser parser) throws Exception;
  public boolean hasStanza();
  public Stanza getStanza();
  public boolean finished();
}