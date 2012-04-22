package com.xetrix.xmpp.client;

import com.xetrix.xmpp.stanza.Stanza;
import com.xetrix.xmpp.stanza.StanzaHandler;

public interface Stream {
  public Auth getAuth();
  public void setAuth(Auth a);

  public void setConnection(Connection c);
  public Connection getConnection();

  public void setListener(StreamListener l);
  public StreamListener getListener();

  public void addStanzaHandler(StanzaHandler h);
  public void removeStanzaHandler(StanzaHandler h);
  public void clearStanzaHandlers();

  public String getStreamId();
  public String getNextStanzaId();

  public boolean isOpened();
  public boolean isAuthed();
  public boolean isBinded();
  public void setBinded(Boolean b);
  public boolean isSessionStarted();
  public void setSessionStarded(Boolean s);

  public void pushStanza(String s);
  public void pushStanza(Stanza s);

  public void initStream(String service);
  public void initStream();
  public void finishStream();

}
