package com.xetrix.xmpp.client;

import java.util.List;
import java.io.IOException;

import com.xetrix.xmpp.client.listener.ConnectionListener;
import com.xetrix.xmpp.client.listener.StreamListener;
import com.xetrix.xmpp.stanza.Stanza;
import com.xetrix.xmpp.stanza.IQ;
import com.xetrix.xmpp.stanza.Presence;
import com.xetrix.xmpp.stanza.parser.IQParser;
import com.xetrix.xmpp.stanza.parser.PresenceParser;
import com.xetrix.xmpp.stanza.parser.StreamErrorParser;
import com.xetrix.xmpp.stanza.parser.StreamConfigParser;
import com.xetrix.xmpp.stanza.listener.IQByIdListener;
import com.xetrix.xmpp.payload.Bind;
import com.xetrix.xmpp.payload.parser.BindParser;
import com.xetrix.xmpp.payload.Session;
import com.xetrix.xmpp.payload.parser.SessionParser;

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

  // Parsers
  private IQParser               iqParser;
  private PresenceParser         pParser;

  // Session
  private boolean                sessionRequested = false;

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
    stream.clearStanzaParsers();
    // Core parsers
    stream.addStanzaParser(new StreamConfigParser());
    stream.addStanzaParser(new StreamErrorParser());
    // IQ Parser
    iqParser = new IQParser();
    iqParser.addPayloadParser(new BindParser());
    iqParser.addPayloadParser(new SessionParser());
    stream.addStanzaParser(iqParser);
    // Presence parser
    pParser = new PresenceParser();
    stream.addStanzaParser(pParser);

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

  public void onBindRequested(Boolean required) {
    if (streamListener instanceof StreamListener) {
      streamListener.onBindRequested(required);
    }
    if (!isBinded()) {
      startBindingAndSession();
    }
  }

  public void onSessionRequested() {
    sessionRequested = true;
    if (streamListener instanceof StreamListener) {
      streamListener.onSessionRequested();
    }
  }

  public void onResourceBinded(String j, String r) {
    username = j;
    if (r != null) {
      resource = r;
    }
    if (streamListener instanceof StreamListener) {
      streamListener.onResourceBinded(j, r);
    }
  }

  public void onSessionStarted() {
    if (streamListener instanceof StreamListener) {
      streamListener.onSessionStarted();
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

  // Private methods
  private void startBindingAndSession() {
    Thread binder = new Thread() {
      public void run() {
        IQ iq;
        iq = new IQ(IQ.Type.set, new Bind(getFullJid()));
        iq.setId(stream.getNextStanzaId());

        IQByIdListener iqlis = new IQByIdListener(iq.getId(), true);
        stream.addStanzaInListener(iqlis);
        stream.pushStanza(iq);

        Bind b = (Bind) iqlis.waitStanza().getPayload();  // Blocking
        stream.setBinded(b.getJid(), b.getResource());

        if (!isSessionStarted() && sessionRequested) {
          iq = new IQ(IQ.Type.set, new Session());
          iq.setId(stream.getNextStanzaId());

          iqlis = new IQByIdListener(iq.getId(), true);
          stream.addStanzaInListener(iqlis);
          stream.pushStanza(iq);

          if (iqlis.waitStanza() != null) { // Blocking
            stream.setSessionStarded();
          }
        }
      }
    };
    binder.start();
  }
}
