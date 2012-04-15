package com.xetrix.xmpp.client;

import java.util.List;

public interface Auth {
  public boolean isAuthed();
  public XMPPError getError();

  public boolean setServerMechanisms(List<String> mechs);
  public List<String> getServerMechanisms();
  public List<String> getClientMechanisms();
  public List<String> getAvailableMechanisms();
  public String getBestMechanism();

  public void initAuthData(String u, String p, String r, String s);
  public String startAuthWith(String mech);

  public String processChallenge(String response);
  public boolean processSuccess();
}