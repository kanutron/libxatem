package com.xetrix.xmpp;
import org.xmlpull.v1.XmlPullParser;

public interface Parseable {
  public void parse(XmlPullParser parser) throws Exception;
}