package com.xetrix.xmpp.client;

import com.xetrix.xmpp.client.listener.StreamListener;

import com.xetrix.xmpp.stanza.Stanza;
import com.xetrix.xmpp.stanza.parser.StanzaParser;
import com.xetrix.xmpp.stanza.listener.StanzaListener;

public interface Stream {
  public Auth getAuth();
  public void setAuth(Auth a);

  public void setConnection(Connection c);
  public Connection getConnection();

  public void setListener(StreamListener l);
  public StreamListener getListener();

  public void addStanzaParser(StanzaParser p);
  public void removeStanzaParser(StanzaParser p);
  public void clearStanzaParsers();

  public void addStanzaInListener(StanzaListener l);
  public void removeStanzaInListener(StanzaListener l);
  public void clearStanzaInListeners();

  public void addStanzaOutListener(StanzaListener l);
  public void removeStanzaOutListener(StanzaListener l);
  public void clearStanzaOutListeners();

  public String getStreamId();
  public String getNextStanzaId();

  public boolean isOpened();
  public boolean isAuthed();
  public boolean isBinded();
  public void setBinded(String j, String r);
  public boolean isSessionStarted();
  public void setSessionStarded();

  public void pushStanza(String s);
  public void pushStanza(Stanza s);

  public void initStream(String service);
  public void initStream();
  public void finishStream();

}
