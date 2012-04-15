package com.xetrix.xmpp.client;

import java.util.List;

public abstract class ClientListener {
  // Constructors
  public ClientListener() {}

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

  public void onReadyforAuthentication() {}
  public void onAuthenticated() {}
  public void onResourceBinded() {}
  public void onSessionStarted() {}
}