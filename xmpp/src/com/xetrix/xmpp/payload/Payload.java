package com.xetrix.xmpp.payload;

public abstract class Payload {
  public abstract String getName();
  public abstract String getXmlns();
  public abstract String toXML();
}