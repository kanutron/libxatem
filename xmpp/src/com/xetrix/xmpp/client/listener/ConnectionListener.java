package com.xetrix.xmpp.client.listener;

import com.xetrix.xmpp.client.XMPPError;

public interface ConnectionListener {
  // Event Handlers
  public void onConnect();
  public void onDisconnect();
  public void onSecurized();
  public void onCompressed();
  public void onConnectionError(XMPPError e);
}