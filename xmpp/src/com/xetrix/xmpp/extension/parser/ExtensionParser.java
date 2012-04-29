package com.xetrix.xmpp.extension.parser;

import org.xmlpull.v1.XmlPullParser;

import com.xetrix.xmpp.extension.Extension;

public interface ExtensionParser {
  public boolean wantsExtension(XmlPullParser parser) throws Exception;
  public boolean parseExtension(XmlPullParser parser) throws Exception;
  public boolean hasExtension();
  public Extension getExtension();
  public boolean finished();
}