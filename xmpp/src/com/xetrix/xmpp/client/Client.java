package com.xetrix.xmpp.client;

import java.util.List;
import java.io.IOException;

import com.xetrix.xmpp.stanza.Stanza;
import com.xetrix.xmpp.stanza.IQ;
import com.xetrix.xmpp.payload.Bind;
import com.xetrix.xmpp.payload.Session;

public class Client implements ConnectionListener, StreamListener {
  private static final String    CLIENT_NAME = "xatem";

  // Externally inmutable configuration
  private String                 username;
  private String                 password;
  private String                 resource;
  private Integer                priority;
  private String                 host;
  private Integer                port;
  private String                 service;

  // XMPP Client components
  private Connection             conn;
  private Auth                   auth;
  private Stream                 stream;

  // Event listeners
  private ConnectionListener     connectionListener;
  private StreamListener         streamListener;

  // Constructors
  public Client() {
    auth = new StandardAuth();
    conn = new StandardConnection(this);
    stream = new StandardStream(this, auth, conn);
  }

  // Public methods
  public void setAuth(Auth a) {
    auth = a;
    stream.setAuth(auth);
  }

  public void setConnection(Connection c) {
    conn = c;
    setConnectionListener(conn.getListener());
    conn.setListener(this);
    stream.setConnection(conn);
  }

  public void setStream(Stream s) {
    stream = s;
    setStreamListener(stream.getListener());
    stream.setListener(this);
    stream.setAuth(auth);
    stream.setConnection(conn);
  }

  public void setConnectionListener(ConnectionListener listener) {
    if (listener != this) {
      connectionListener = listener;
    } else {
      connectionListener = null;
    }
  }

  public void setStreamListener(StreamListener listener) {
    if (listener != this) {
      streamListener = listener;
    } else {
      streamListener = null;
    }
  }

  public void setUserData(String user, String pass, String res, Integer prior,
                      String hst, Integer prt, String serv) {
    username = user;
    password = pass;
    resource = res;
    priority = prior;
    host = hst;
    port = prt;
    service = serv;
    auth.initAuthData(username, password, resource, service);
  }

  public void setUserData(String user, String pass, String res, Integer prior,
                      String hst, Integer prt) {
    String serv = hst;
    if (user.indexOf("@")>0) {
      serv = user.substring(user.indexOf("@")+1);
    }
    setUserData(user, pass, res, prior, hst, prt, serv);
  }

  public void setUserData(String user, String pass, String res, Integer prior) {
    String hst = user.substring(user.indexOf("@")+1);
    Integer prt = 5222;
    String serv = hst;
    setUserData(user, pass, res, prior, hst, prt, serv);
  }

  public void setUserData(String user, String pass) {
    String res = CLIENT_NAME;
    Integer prior = 24;
    String hst = user.substring(user.indexOf("@")+1);
    Integer prt = 5222;
    String serv = hst;
    setUserData(user, pass, res, prior, hst, prt, serv);
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
  public String getStreamId() {
    return stream.getStreamId();
  }

  // Life cycle
  public boolean isConnected() {
    return conn.isConnected();
  }
  public boolean isSecurized() {
    return conn.isSecurized();
  }
  public boolean isCompressed() {
    return conn.isCompressed();
  }
  public boolean isStreamOpened() {
    return stream.isOpened();
  }
  public boolean isAuthed() {
    return stream.isAuthed();
  }
  public boolean isBinded() {
    return stream.isBinded();
  }
  public boolean isSessionStarted() {
    return stream.isSessionStarted();
  }

  // Mehtods
  public void connect(Connection.Security s) {
    if (conn.setSecurity(s)) {
      conn.connect(host, port);
    }
  }

  public void connect(Integer s) {
    switch (s) {
      case 0: connect(Connection.Security.none); break;
      case 1: connect(Connection.Security.ssl); break;
      case 2: connect(Connection.Security.tls); break;
    }
  }

  public void connect(String s) {
    connect(Connection.Security.fromString(s));
  }

  public void connect() {
    connect(Connection.Security.none);
  }

  public void disconnect() {
    conn.disconnect();
  }

  public void pushStanza(String s) {
    stream.pushStanza(s);
  }

  public void pushStanza(Stanza s) {
    stream.pushStanza(s);
  }

  public String getNextStanzaId() {
    return stream.getNextStanzaId();
  }

  // Event Handlers
  public void onConnect() {
    stream.initStream(service);
    if (connectionListener instanceof ConnectionListener) {
      connectionListener.onConnect();
    }
  }

  public void onDisconnect() {
    if (connectionListener instanceof ConnectionListener) {
      connectionListener.onDisconnect();
    }
  }

  public void onSecurized() {
    if (connectionListener instanceof ConnectionListener) {
      connectionListener.onSecurized();
    }
  }

  public void onCompressed() {
    if (connectionListener instanceof ConnectionListener) {
      connectionListener.onCompressed();
    }
  }

  public void onConnectionError(XMPPError e) {
    if (connectionListener instanceof ConnectionListener) {
      connectionListener.onConnectionError(e);
    }
    if (isConnected()) {
      if (e.getType() == XMPPError.Type.AUTH || e.getType() == XMPPError.Type.CANCEL) {
        disconnect();
      }
    }
  }

  public void onStreamOpened(String from) {
    if ("".equals(from) && from != null) {
      service = from;
    }
    if (streamListener instanceof StreamListener) {
      streamListener.onStreamOpened(service);
    }
  }

  public void onReadyForBindResource(Boolean required) {
    if (streamListener instanceof StreamListener) {
      streamListener.onReadyForBindResource(required);
    }
    if (!isBinded() && required) {
      // TODO: set IQ handler by ID
      IQ iqb = new IQ(IQ.Type.set, new Bind(getFullJid()));
      stream.pushStanza(iqb);
    }
  }

  public void onResourceBinded(Bind bind) {
    username = bind.getJid();
    if (bind.getResource() != null) {
      resource = bind.getResource();
    }
    if (streamListener instanceof StreamListener) {
      streamListener.onResourceBinded(bind);
    }
  }

  public void onReadyForStartSession() {
    if (streamListener instanceof StreamListener) {
      streamListener.onReadyForStartSession();
    }
    if (!isSessionStarted()) {
      // TODO: set IQ handler by ID
      IQ iqs = new IQ(IQ.Type.set, new Session());
      stream.pushStanza(iqs);
    }
  }

  public void onSessionStarted(Session session) {
    if (streamListener instanceof StreamListener) {
      streamListener.onSessionStarted(session);
    }
  }

  public void onStreamClosed() {
    if (streamListener instanceof StreamListener) {
      streamListener.onStreamClosed();
    }
    if (isConnected()) {
      disconnect();
    }
  }

  public void onStreamError(XMPPError e) {
    if (streamListener instanceof StreamListener) {
      streamListener.onStreamError(e);
    }
    if (isConnected()) {
      if (e.getType() == XMPPError.Type.AUTH || e.getType() == XMPPError.Type.CANCEL) {
        disconnect();
      }
    }
  }

  public void onAuthenticated() {
    if (streamListener instanceof StreamListener) {
      streamListener.onAuthenticated();
    }
  }
}
