package com.xetrix.xmpp.client;

import java.util.List;
import java.io.IOException;

import com.xetrix.xmpp.payload.Bind;
import com.xetrix.xmpp.payload.Session;

public class Client {
  private static final String    CLIENT_NAME = "xatem";

  // Externally inmutable configuration
  private String                 username;
  private String                 password;
  private String                 resource;
  private Integer                priority;
  private String                 host;
  private Integer                port;
  private String                 service;

  // Life cycle control
  private boolean               connected = false;
  private boolean               authenticated = false;
  private boolean               binded = false;
  private boolean               sessionStarted = false;

  // XMPP Client components
  protected Connection           socket = new Connection(this);
  protected Stream               stream = new Stream(this);
  protected Auth                 auth;

  // Event listeners
  private ClientListener     listener;

  // Constructors
  public Client(String u, String p, String r, Integer pr, String h, Integer prt, String s) {
    username = u;
    password = p;
    resource = r;
    priority = pr;
    host = h;
    port = prt;
    service = s;
  }

  public Client(String u, String p, String r, Integer pr, String h, Integer prt) {
    username = u;
    password = p;
    resource = r;
    priority = pr;
    host = h;
    port = prt;
    if (username.indexOf("@")>0) {
      service = username.substring(username.indexOf("@")+1);
    } else {
      service = host;
    }
  }

  public Client(String u, String p, String r, Integer pr) {
    username = u;
    password = p;
    resource = r;
    priority = pr;
    host = username.substring(username.indexOf("@")+1);
    port = 5222;
    service = host;
  }

  public Client(String u, String p) {
    username = u;
    password = p;
    resource = CLIENT_NAME;
    priority = 24;
    host = username.substring(username.indexOf("@")+1);
    port = 5222;
    service = host;
  }

  // Public methods
  public void setListener(ClientListener l) {
    listener = l;
  }

  public String getJid() {
    if (username.indexOf("@")>0) {
      return username;
    } else {
      return username + "@" + host;
    }
  }
  public String getFullJid() {
    return getJid() + "/" + getResource();
  }
  public String getUsername() {
    return username;
  }
  public String getResource() {
    return resource;
  }
  public Integer getPriority() {
    return priority;
  }
  public String getHost() {
    return host;
  }
  public Integer getPort() {
    return port;
  }
  public String getService() {
    return service;
  }
  public String getConnectionID() {
    return stream.getConnectionID();
  }

  // Life cycle
  public boolean isConnected() {
    return connected && socket.isConnected();
  }
  public boolean isSecurized() {
    return socket.securized;
  }
  public boolean isCompressed() {
    return socket.compressed;
  }
  public boolean isAuthed() {
    return authenticated;
  }
  public boolean isBinded() {
    return binded;
  }
  public boolean isSessionStarted() {
    return sessionStarted;
  }

  // Mehtods
  public boolean connect(Connection.Security s) {
    socket = new Connection(this);
    if (socket.setSecurity(s)) {
      if (socket.connect(host, port)) {
        connected = true;
        stream.initStream();
        return true;
      }
    }
    return false;
  }

  public boolean connect(Integer s) {
    switch (s) {
      case 0: return connect(Connection.Security.none);
      case 1: return connect(Connection.Security.ssl);
      case 2: return connect(Connection.Security.tls);
    }
    return false;
  }

  public boolean connect(String s) {
    return connect(Connection.Security.fromString(s));
  }

  public boolean connect() {
    return connect(Connection.Security.none);
  }

  public boolean disconnect() {
    stream.finishStream();
    socket.disconnect();

    connected = false;
    authenticated = false;
    binded = false;

    stream = new Stream(this);
    socket = new Connection(this);

    return true;
  }

  // Event Handlers
  void onConnect() {
    if (listener instanceof ClientListener) {
      listener.onConnect();
    }
  }

  void onDisconnect() {
    if (listener instanceof ClientListener) {
      listener.onDisconnect();
    }
  }

  void onSecurized() {
    if (listener instanceof ClientListener) {
      listener.onSecurized();
    }
  }

  void onCompressed() {
    if (listener instanceof ClientListener) {
      listener.onCompressed();
    }
  }

  void onConnectionError(XMPPError e) {
    if (listener instanceof ClientListener) {
      listener.onConnectionError(e);
    }
    if (connected) {
      if (e.getType() == XMPPError.Type.AUTH || e.getType() == XMPPError.Type.CANCEL) {
        disconnect();
      }
    }
  }

  void onStreamOpened(String cid, String from) {
    if (cid != null) {
      stream.setConnectionID(cid);
    }
    if (from != null) {
      service = from;
    }
    if (listener instanceof ClientListener) {
      listener.onStreamOpened(cid, from);
    }
  }

  void onStreamClosed() {
    if (listener instanceof ClientListener) {
      listener.onStreamClosed();
    }
    if (connected) {
      disconnect();
    }
  }

  void onStreamError(XMPPError e) {
    if (listener instanceof ClientListener) {
      listener.onStreamError(e);
    }
    if (connected) {
      if (e.getType() == XMPPError.Type.AUTH || e.getType() == XMPPError.Type.CANCEL) {
        disconnect();
      }
    }
  }

  void onReceiveSASLMechanisms(List<String> mechs) {
    auth = new Auth(this);
    auth.setServerMechanisms(mechs);
    if (listener instanceof ClientListener) {
      listener.onReceiveSASLMechanisms(mechs);
    }
  }

  void onReceiveCompressionMethods(List<String> methods) {
    socket.compressionSetServerMethods(methods);
    if (listener instanceof ClientListener) {
      listener.onReceiveCompressionMethods(methods);
    }
  }

  void onReadyforAuthentication() {
    if (listener instanceof ClientListener) {
      listener.onReadyforAuthentication();
    }
    String mech = auth.getBestMechanism();
    if (mech != "") {
      auth.initAuthData(username, password, resource, service);
      auth.startAuthWith(mech);
    } else {
      onStreamError(new XMPPError(XMPPError.Type.AUTH, "feature-not-implemented",
        "No suitable SASL mechanisms found. Can't login."));
    }
  }

  void onAuthenticated() {
    authenticated = true;
    auth = null;
    if (listener instanceof ClientListener) {
      listener.onAuthenticated();
    }
  }

  void onResourceBinded(Bind bind) {
    binded = true;
    username = bind.getJid();
    if (bind.getResource() != null) {
      resource = bind.getResource();
    }
    if (listener instanceof ClientListener) {
      listener.onResourceBinded();
    }
  }

  void onSessionStarted(Session session) {
    sessionStarted = true;
    if (listener instanceof ClientListener) {
      listener.onSessionStarted();
    }
  }
}
