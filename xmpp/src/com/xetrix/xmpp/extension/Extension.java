package com.xetrix.xmpp.extension;

public abstract class Extension {
  public abstract String getName();
  public abstract String getXmlns();
  public abstract String toXML();
}