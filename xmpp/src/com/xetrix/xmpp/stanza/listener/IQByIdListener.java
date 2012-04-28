package com.xetrix.xmpp.stanza.listener;

import org.xmlpull.v1.XmlPullParser;
import com.xetrix.xmpp.stanza.Stanza;
import com.xetrix.xmpp.stanza.IQ;

public class IQByIdListener implements StanzaListener {
  private String expectedId = null;
  private IQ iq = null;

  private boolean exclusive = false;
  private boolean finished = false;

  // Constructors
  public IQByIdListener(String id) {
    expectedId = id;
  }

  public IQByIdListener(String id, boolean exclusiveAccess) {
    expectedId = id;
    exclusive = exclusiveAccess;
  }

  // Interface methods
  public boolean wantsStanza(Stanza s) {
    if (s.getName().equals("iq") &&
        s.getId().equals(expectedId)) {
      return true;
    }
    return false;
  }

  public synchronized boolean processStanza(Stanza s) {
    iq = (IQ)s;
    finished = true;
    notifyAll();
    return exclusive;
  }

  public boolean finished() {
    return finished;
  }

  // Specific methods
  public synchronized IQ waitStanza() {
    while (iq==null) {
      try {
        wait();
      }
      catch (InterruptedException ie) {
      }
    }
    return iq;
  }
}