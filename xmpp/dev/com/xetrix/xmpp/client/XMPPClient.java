package com.xetrix.xmpp.client;

import com.xetrix.xmpp.util.Log; // DUBG

public class XMPPClient {
  private static final String    CLIENT_NAME = "xatem";

  // Externally inmutable configuration
  private String                 username;
  private String                 password;
  private String                 resource;
  private Integer                priority;
  private String                 service;
  private String                 host;
  private Integer                port;

  // Life cycle control
  private Integer                packetId = 0;
  private Boolean                connected = false;
  private Boolean                authenticated = false;
  private Boolean                binded = false;

  // XMPP Client components
  protected XMPPSocket           socket = new XMPPSocket(this);
  protected XMPPStream           stream;

  // Constructors
  public XMPPClient(String u, String p, String r, Integer pr, String host, Integer port, String serv) {
    this.username = u;
    this.password = p;
    this.resource = r;
    this.priority = pr;
    this.host = host;
    this.port = port;
    this.service = serv;
  }

  public XMPPClient(String u, String p, String r, Integer pr, String host, Integer port) {
    this.username = u;
    this.password = p;
    this.resource = r;
    this.priority = pr;
    this.host = host;
    this.port = port;
    this.service = this.username.substring(this.username.indexOf("@")+1);
  }

  public XMPPClient(String u, String p, String r, Integer pr) {
    this.username = u;
    this.password = p;
    this.resource = r;
    this.priority = pr;
    this.host = this.username.substring(this.username.indexOf("@")+1);
    this.port = 5222;
    this.service = this.host;
  }

  public XMPPClient(String u, String p) {
    this.username = u;
    this.password = p;
    this.resource = CLIENT_NAME;
    this.priority = 24;
    this.host = this.username.substring(this.username.indexOf("@")+1);
    this.port = 5222;
    this.service = this.host;
  }

  // Public methods
  public String getUsername() {
    return this.username;
  }
  public String getResource() {
    return this.resource;
  }
  public Integer getPriority() {
    return this.priority;
  }
  public String getService() {
    return this.service;
  }
  public String getHost() {
    return this.host;
  }
  public Integer getPort() {
    return this.port;
  }
  public Boolean isConnected() {
    return this.connected && this.socket.isConnected();
  }
  public Boolean isAuthed() {
    return this.authenticated;
  }
  public Boolean isBinded() {
    return this.binded;
  }
  public Boolean isSecurized() {
    return this.socket.securized;
  }
  public Boolean isCompressed() {
    return this.socket.compressed;
  }

  public Boolean connect(XMPPSocket.Security s) {
    socket = new XMPPSocket(this);
    if (this.socket.setSecurity(s)) {
      if (this.socket.connect(this.host, this.port)) {
        this.connected = true;
        this.initStream();
        return true;
      }
    }
    return false;
  }

  public Boolean connect(Integer s) {
    switch (s) {
      case 0: return this.connect(XMPPSocket.Security.none);
      case 1: return this.connect(XMPPSocket.Security.ssl);
      case 2: return this.connect(XMPPSocket.Security.tls);
    }
    return false;
  }

  public Boolean connect(String s) {
    return this.connect(XMPPSocket.Security.fromString(s));
  }

  public Boolean connect() {
    return this.connect(XMPPSocket.Security.none);
  }

  public Boolean disconnect() {
    this.connected = false;
    this.authenticated = false;
    this.binded = false;
    this.stream = null;
    this.socket = new XMPPSocket(this);
    return true;
  }

  public Boolean startTLS() {
    Boolean allowed = this.socket.getSecurity() == XMPPSocket.Security.tls;
    if (allowed && this.socket.enableTLS()) {
      this.initStream();
      return true;
    } else {
      return false;
    }
  }

  public Integer getPacketId() {
    return ++packetId;
  }

  // Package methods
  void setService(String s) {
    this.service = s;
  }

  void requestStartTLS(Boolean required) {
    Boolean allowed = this.socket.getSecurity() == XMPPSocket.Security.tls;
    if (allowed) {
      this.stream.write("<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>");
    } else if (required) {
      this.socket.disconnect();
    }
  }

  void notifySocketClosed(Exception e) {
    Log.write("Socket closed unexpectedly",3);
    Log.write(e.getMessage(),7);
    // TODO: reconnection stuff
    this.disconnect();
  }

  void notifyNonXMLReceibed(Exception e) {
    // Could happens when connected to a SSL host expecting a TLS
    // Try to start TLS before XML stream
    if (!this.socket.securized && !this.authenticated && !this.binded) {
      Log.write("Bad XML receibed. SSL host?",3);
      if (this.socket.getSecurity() == XMPPSocket.Security.tls) {
        this.disconnect();
        if (!this.connect(XMPPSocket.Security.ssl)) {
          this.disconnect();
        }
      }
    } else {
      // TODO: Reconnect stuff
      this.disconnect();
    }
  }

  void notifySSLExceptionDuringHandshake(Exception e) {
    // Could happens when connected to a TLS host expecting a SSL
    // Try to use TLS instead
    // FIXME: does not works!
    if (!this.socket.securized && !this.authenticated && !this.binded) {
      Log.write("Socket SSL Exception during handshake",3);
      Log.write(e.getMessage(),7);
      if (this.socket.getSecurity() == XMPPSocket.Security.ssl) {
        this.disconnect();
        if (!this.connect(XMPPSocket.Security.tls)) {
          this.disconnect();
        }
      }
    } else {
      // TODO: Reconnect stuff
      this.disconnect();
    }
  }

  // Private methods
  private void initStream() {
    this.stream = new XMPPStream(this);
    this.stream.write(
      "<stream:stream to=\"" + this.service +
      "\" xmlns:stream=\"http://etherx.jabber.org/streams\" xmlns=\"jabber:client\" version=\"1.0\">"
    );
  }

}
