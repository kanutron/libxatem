package com.xetrix.xmpp.stanza;

import com.xetrix.xmpp.client.Stream;
import org.xmlpull.v1.XmlPullParser;

public interface StanzaParser {
  public boolean wantsStanza(XmlPullParser parser) throws Exception;
  public boolean parseStanza(Stream stream, XmlPullParser parser) throws Exception;
  public boolean hasStanza();
  public Stanza getStanza();
  public boolean finished();
}