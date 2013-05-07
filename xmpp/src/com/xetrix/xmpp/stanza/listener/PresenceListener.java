package com.xetrix.xmpp.stanza.listener;

import org.xmlpull.v1.XmlPullParser;
import com.xetrix.xmpp.stanza.Stanza;
import com.xetrix.xmpp.stanza.Presence;

public class PresenceListener implements StanzaListener {
  // TODO: Should be a queue!
  private Presence presence = null;

  private boolean exclusive = false;
  private boolean finished = false;
  private boolean onlyonce = false;

  // Constructors
  public PresenceListener() {}
  public PresenceListener(boolean exclusiveAccess) {
    exclusive = exclusiveAccess;
  }
  public PresenceListener(boolean exclusiveAccess, boolean onlyOnce) {
    exclusive = exclusiveAccess;
    onlyonce = onlyOnce;
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
    finished = onlyonce;
    notifyAll();
    return exclusive;
  }

  public void setProcessed() {
    presence = null;
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