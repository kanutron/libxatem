package com.xetrix.xmpp.client;

import java.io.Reader;
import java.io.Writer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.ConnectException;
import java.net.SocketException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLException;

public class XMPPSocket {
  private XMPPClient client;

  private String          host;
  private Integer         port;
  private Security        security = Security.none;
  private Compression     compression = Compression.zlib;

  private Socket          socket;
  protected Reader        reader;
  protected Writer        writer;

  Boolean                 connecting = false;
  Boolean                 securized = false;
  Boolean                 compressed = false;

  // Constructors
  public XMPPSocket(XMPPClient c) {
    this.client = c;
  }

  // Pulic methods
  public Boolean setSecurity(Security s) {
    if (this.securized) {
      return false;
    }
    this.security = s;
    return true;
  }

  public Security getSecurity() {
    return this.security;
  }

  public Boolean setCompression(Compression c) {
    if (this.compressed) {
      return false;
    }
    this.compression = c;
    return true;
  }

  public Compression getCompression() {
    return this.compression;
  }

  public Boolean connect(String h, Integer p) {
    Boolean t = false;
    this.connecting = true;
    this.securized = false;
    this.host = h;
    this.port = p;
    switch (this.security) {
      case ssl:
        t = this.openSSL();
      default:
        t = this.openPlain();
    }
    this.connecting = false;
    return t;
  }

  public Boolean disconnect() {
    this.securized = false;
    this.compressed = false;
    this.connecting = false;
    try {
      if (this.socket.isConnected()) {
        this.socket.close();
      }
    } catch (IOException ioe) {
    }
    return !this.socket.isConnected();
  }

  public Boolean isConnected() {
    return this.socket.isConnected();
  }

  public Boolean enableTLS() {
    if (!this.securized && this.socket.isConnected()) {
      return switchToTLS();
    }
    return false;
  }

  public Boolean enableCompression() {
    if (!compressed && this.socket.isConnected()) {
      this.compressed = switchToCompressed(this.compression);
    }
    return this.compressed;
  }

  public enum Security {
    none,
    ssl,
    tls;
    public static Security fromString(String name) {
      try {
        return Security.valueOf(name);
      }
      catch (Exception e) {
        return none;
      }
    }
  }

  public enum Compression {
    none,
    zlib;
    public static Compression fromString(String name) {
      try {
        return Compression.valueOf(name);
      }
      catch (Exception e) {
        return none;
      }
    }
  }

  // Private methods
  private Boolean openSSL() {
    if (this.openPlain() && this.enableTLS()) {
      return true;
    } else {
      return false;
    }
  }

  private Boolean openPlain() {
    try {
      this.socket = new Socket(this.host, this.port);
      while (true) {
        try {
          if (this.socket.isConnected()) {
            this.initIO();
            return true;
          } else {
            Thread.currentThread().sleep(125);
          }
        } catch (InterruptedException e) {
        }
      }
    } catch (UnknownHostException uhe) {
      uhe.printStackTrace(); // DEBUG
    } catch (ConnectException ce) {
      ce.printStackTrace(); // DEBUG
    } catch (IOException ioe) {
      ioe.printStackTrace(); // DEBUG
    }
    return false;
  }

  private Boolean switchToTLS() {
    try {
      // TODO: Verify server certs.
      SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
      Socket plain = this.socket;
      this.socket = socketFactory.createSocket(plain, plain.getInetAddress().getHostName(),
                                               plain.getPort(), true);
      this.socket.setSoTimeout(0);
      this.socket.setKeepAlive(true);
      this.initIO();
      ((SSLSocket) socket).startHandshake();
      this.securized = true;
    } catch (SSLException ssle) {
      // This could happen when trying to connect to a TLS server asuming SSL
      // We can try to switch to TLS
      this.securized = false;
      if (this.connecting) {
        this.client.notifySSLExceptionDuringHandshake(ssle);
      }
    } catch (SocketException se) {
      this.securized = false;
      se.printStackTrace(); // DEBUG
    } catch (IOException ioe) {
      this.securized = false;
      ioe.printStackTrace(); // DEBUG
    }
    return this.securized;
  }

  private Boolean switchToCompressed(Compression c) {
    return false;
  }

  private Boolean initIO() {
    try {
      this.reader = new BufferedReader(new InputStreamReader(
        this.socket.getInputStream(), "UTF-8"));
      this.writer = new BufferedWriter(new OutputStreamWriter(
        this.socket.getOutputStream(), "UTF-8"));
      return true;
    } catch (IOException ioe) {
      ioe.printStackTrace(); // DEBUG
    }
    return false;
  }
}
