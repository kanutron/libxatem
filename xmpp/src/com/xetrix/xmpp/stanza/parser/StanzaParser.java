package com.xetrix.xmpp.stanza.parser;

import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.client.Stream;

import com.xetrix.xmpp.stanza.Stanza;

public interface StanzaParser {
  public boolean wantsStanza(XmlPullParser parser) throws Exception;
  public boolean parseStanza(Stream stream, XmlPullParser parser) throws Exception;
  public boolean hasStanza();
  public Stanza getStanza();
  public boolean finished();
}