package com.xetrix.xmpp.client;

import java.util.List;
import java.util.ArrayList;
import java.io.Reader;
import java.io.Writer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class XMPPSocket {
  private static final Integer SSL_HANDSHAKE_MAX_TIME = 5000; // Milliseconds
  private XMPPClient client;

  private String          host;
  private Integer         port;
  private Security        security = Security.none;
  private Compression     compression = Compression.none;

  private Socket          socket;
  protected Reader        reader;
  protected Writer        writer;

  boolean                 securized = false;
  boolean                 compressed = false;

  // Constructors
  public XMPPSocket(XMPPClient c) {
    this.client = c;
  }

  // Pulic methods
  public boolean setSecurity(Security s) {
    if (this.securized) {
      return false;
    }
    this.security = s;
    return true;
  }

  public Security getSecurity() {
    return this.security;
  }

  public boolean setCompression(Compression c) {
    if (this.compressed) {
      return false;
    }
    this.compression = c;
    return true;
  }

  public Compression getCompression() {
    return this.compression;
  }

  public boolean connect(String h, Integer p) {
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

  public boolean disconnect() {
    this.securized = false;
    this.compressed = false;
    try {
      if (this.socket.isConnected()) {
        this.socket.close();
      }
    } catch (Exception e) {
      this.client.notifySocketException(e);
    }
    return true;
  }

  public boolean isConnected() {
    return this.socket.isConnected();
  }

  public boolean enableTLS() {
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
    } catch (Exception e) {
      this.client.notifySocketException(e);
    }
    return false;
  }

  public boolean enableCompression() {
    if (!this.compressed && this.socket.isConnected()) {
      if (this.client.isAuthed()) {
        this.client.notifySocketException(
          new Exception("Compression should be negotiated before authentication."));
      }
      return this.initIO();
    }
    return this.compressed;
  }

  public void compressionSetServerMethods(List<String> methods) {
    if (methods.contains(Compression.zlib.toString())) {
      try {
        Class.forName("com.jcraft.jzlib.ZOutputStream");
        this.compression = Compression.zlib;
        return;
      } catch (ClassNotFoundException e) {
        this.compression = Compression.none;
      }
    }
    this.compression = Compression.none;
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
  private boolean openPlain() {
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
    } catch (Exception e) {
      this.client.notifySocketException(e);
    }
    return false;
  }

  private boolean initIO() {
    if (this.compression == Compression.none) {
      try {
        this.reader = new BufferedReader(new InputStreamReader(
          this.socket.getInputStream(), "UTF-8"));
        this.writer = new BufferedWriter(new OutputStreamWriter(
          this.socket.getOutputStream(), "UTF-8"));
        return true;
      } catch (Exception e) {
        this.client.notifySocketException(e);
      }
    } else if (this.compression == Compression.zlib) {
      try {
        Class<?> ziClass = Class.forName("com.jcraft.jzlib.ZInputStream");
        Constructor<?> constructor = ziClass.getConstructor(InputStream.class);
        Object in = constructor.newInstance(socket.getInputStream());
        Method method = ziClass.getMethod("setFlushMode", Integer.TYPE);
        method.invoke(in, 2);
        reader = new BufferedReader(new InputStreamReader((InputStream) in, "UTF-8"));

        Class<?> zoClass = Class.forName("com.jcraft.jzlib.ZOutputStream");
        constructor = zoClass.getConstructor(OutputStream.class, Integer.TYPE);
        Object out = constructor.newInstance(socket.getOutputStream(), 9);
        method = zoClass.getMethod("setFlushMode", Integer.TYPE);
        method.invoke(out, 2);
        writer = new BufferedWriter(new OutputStreamWriter((OutputStream) out, "UTF-8"));
        return true;
      } catch (Exception e) {
        this.client.notifySocketException(e);
      }
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
