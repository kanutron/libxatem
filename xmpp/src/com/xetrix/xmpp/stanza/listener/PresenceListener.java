package com.xetrix.xmpp.stanza.listener;

import org.xmlpull.v1.XmlPullParser;
import com.xetrix.xmpp.stanza.Stanza;
import com.xetrix.xmpp.stanza.Presence;

public class PresenceListener implements StanzaListener {
  // TODO: Should be a queue!
  private Presence presence = null;

  private boolean exclusive = false;
  private boolean finished = false;

  // Constructors
  public PresenceListener() {}
  public PresenceListener(boolean exclusiveAccess) {
    exclusive = exclusiveAccess;
  }

  // Interface methods
  public boolean wantsStanza(Stanza s) {
    if (s.getName().equals("presence")) {
      return true;
    }
    return false;
  }

  public synchronized boolean processStanza(Stanza s) {
    presence = (Presence)s;
    finished = true;
    notifyAll();
    return exclusive;
  }

  public boolean finished() {
    return finished;
  }

  // Specific methods
  public synchronized Presence waitStanza() {
    while (presence==null) {
      try {
        wait();
      }
      catch (InterruptedException ie) {
      }
    }
    return presence;
  }
}