package com.xetrix.xmpp.payload;

import org.xmlpull.v1.XmlPullParser;

public interface PayloadParser {
  public boolean wantsPayload(XmlPullParser parser) throws Exception;
  public boolean parsePayload(XmlPullParser parser) throws Exception;
  public boolean hasPayload();
  public Payload getPayload();
  public boolean finished();
}