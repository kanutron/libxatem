package com.xetrix.xmpp.payload;

import com.xetrix.xmpp.Parseable;
import org.xmlpull.v1.XmlPullParser;

public abstract class IQPayload implements Parseable {
  public abstract String toXML();
  public abstract void parse(XmlPullParser parser) throws Exception;
}