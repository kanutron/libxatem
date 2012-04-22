package com.xetrix.xmpp.client;

import com.xetrix.xmpp.stanza.Stanza;

public interface Stream {
  public Auth getAuth();
  public void setAuth(Auth a);
  public void setConnection(Connection c);
  public Connection getConnection();
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
  public void initStream();
  public void finishStream();
  public String getNextStanzaId();
}
