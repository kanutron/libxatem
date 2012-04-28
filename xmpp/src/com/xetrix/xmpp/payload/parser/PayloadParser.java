package com.xetrix.xmpp.payload.parser;

import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.payload.Payload;

public interface PayloadParser {
  public boolean wantsPayload(XmlPullParser parser) throws Exception;
  public boolean parsePayload(XmlPullParser parser) throws Exception;
  public boolean hasPayload();
  public Payload getPayload();
  public boolean finished();
}