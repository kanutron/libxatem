package com.xetrix.xmpp.client;

import com.xetrix.xmpp.payload.Bind;
import com.xetrix.xmpp.payload.Session;

public interface StreamListener {
  // Event Handlers
  public void onStreamOpened(String from);
  public void onStreamClosed();
  public void onStreamError(XMPPError e);

  public void onBindRequested(Boolean required);
  public void onSessionRequested();

  public void onAuthenticated();
  public void onResourceBinded(Bind bind);
  public void onSessionStarted(Session session);
}