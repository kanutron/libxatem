package com.xetrix.xmpp.client.listener;

import com.xetrix.xmpp.client.XMPPError;

public interface StreamListener {
  // Event Handlers
  public void onStreamOpened(String from);
  public void onStreamClosed();
  public void onStreamError(XMPPError e);

  public void onBindRequested(Boolean required);
  public void onSessionRequested();

  public void onAuthenticated();
  public void onResourceBinded(String j, String r);
  public void onSessionStarted();
}