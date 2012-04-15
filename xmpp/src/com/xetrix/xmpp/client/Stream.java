package com.xetrix.xmpp.client;

import com.xetrix.xmpp.stanza.Stanza;

public interface Stream {
  public void setAuth(Auth a);
  public void setConnection(Connection c);
  public void setListener(StreamListener l);
  public StreamListener getListener();

  public boolean isOpened();
  public boolean isAuthed();
  public boolean isBinded();
  public boolean isSessionStarted();
  public void pushStanza(String s);
  public void pushStanza(Stanza s);
  public String getStreamId();
  public void initStream(String service);
  public void finishStream();
  public String getNextStanzaId();
}
