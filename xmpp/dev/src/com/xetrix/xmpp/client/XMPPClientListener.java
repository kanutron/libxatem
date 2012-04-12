package com.xetrix.xmpp.client;

import java.util.List;

public abstract class XMPPClientListener {
  // Constructors
  public XMPPClientListener() {}

  // Event Handlers
  public void onConnect() {}
  public void onDisconnect() {}
  public void onSecurized() {}
  public void onCompressed() {}
  public void onConnectionError(XMPPError e) {}

  public void onStreamOpened(String cid, String from) {}
  public void onStreamClosed() {}
  public void onStreamError(XMPPError e) {}

  public void onReceiveSASLMechanisms(List<String> mechs) {}
  public void onReceiveCompressionMethods(List<String> methods) {}

  public void onAuthenticated() {}
  public void onReadyforAuthentication() {}
  public void onResourceBinded(XMPPStanzaIQBind bind) {}
}