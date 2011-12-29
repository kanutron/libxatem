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
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLException;

public class XMPPSocket {
  private static final Integer SSL_HANDSHAKE_MAX_TIME = 5000; // Milliseconds
  private XMPPClient client;

  private String          host;
  private Integer         port;
  private Security        security = Security.none;
  private Compression     compression = Compression.zlib;

  private Socket          socket;
  protected Reader        reader;
  protected Writer        writer;

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
    this.securized = false;
    this.compressed = false;
    this.host = h;
    this.port = p;

    if (this.security == Security.ssl) {
      return this.openPlain() && this.enableTLS();
    } else {
      return this.openPlain();
    }
  }

  public Boolean disconnect() {
    this.securized = false;
    this.compressed = false;
    try {
      if (this.socket.isConnected()) {
        this.socket.close();
      }
    } catch (IOException ioe) {
    }
    return true;
  }

  public Boolean isConnected() {
    return this.socket.isConnected();
  }

  public Boolean enableTLS() {
    if (this.securized || !this.socket.isConnected()) {
      return false;
    }
    try {
      // TODO: Verify server certs.
      SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
      Socket plain = this.socket;
      this.socket = socketFactory.createSocket(plain, plain.getInetAddress().getHostName(),
                                               plain.getPort(), true);
      ((SSLSocket) this.socket).addHandshakeCompletedListener(new HSListener());
      this.socket.setSoTimeout(0);
      this.socket.setKeepAlive(true);
      ((SSLSocket) this.socket).startHandshake();

      Integer whaitHS = 0;
      try {
        while (whaitHS < SSL_HANDSHAKE_MAX_TIME) {
          if (this.securized == true) {
            return this.initIO();
          } else {
            whaitHS += 125;
            Thread.currentThread().sleep(125);
          }
        }
        return false;
      } catch (InterruptedException e) {
        return false;
      }
    } catch (SSLException ssle) {
      ssle.printStackTrace(); // DEBUG
    } catch (SocketException se) {
      se.printStackTrace(); // DEBUG
    } catch (IOException ioe) {
      ioe.printStackTrace(); // DEBUG
    }
    return false;
  }

  public Boolean enableCompression() {
    if (!compressed && this.socket.isConnected()) {
      this.compressed = false; // TODO
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
  private Boolean openPlain() {
    try {
      this.socket = new Socket(this.host, this.port);
      while (true) {
        try {
          if (this.socket.isConnected()) {
            if (this.security != Security.ssl) {
              return this.initIO();
            } else {
              return true;
            }
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

  private Boolean initIO() {
    try {
      this.reader = new BufferedReader(new InputStreamReader(
        this.socket.getInputStream(), "UTF-8"));
      this.writer = new BufferedWriter(new OutputStreamWriter(
        this.socket.getOutputStream(), "UTF-8"));
      return true;
    } catch (IOException ioe) {
      ioe.printStackTrace(); // DEBUG
    } catch (Exception e) {
      e.printStackTrace(); // DEBUG
    }
    return false;
  }

  // Listeners
  class HSListener implements HandshakeCompletedListener {
    public void handshakeCompleted(HandshakeCompletedEvent e) {
      securized = true;
    }
  }
}
