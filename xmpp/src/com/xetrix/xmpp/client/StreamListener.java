package com.xetrix.xmpp.client;

import com.xetrix.xmpp.payload.Bind;
import com.xetrix.xmpp.payload.Session;

public interface StreamListener {
  // Event Handlers
  public void onStreamOpened(String from);
  public void onStreamClosed();
  public void onStreamError(XMPPError e);

  public void onAuthenticated();
  public void onReadyForBindResource(Boolean required);
  public void onResourceBinded(Bind bind);
  public void onReadyForStartSession();
  public void onSessionStarted(Session session);
}