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
    client = c;
  }

  // Pulic methods
  public boolean setSecurity(Security s) {
    if (securized) {
      return false;
    }
    security = s;
    return true;
  }

  public Security getSecurity() {
    return security;
  }

  public boolean setCompression(Compression c) {
    if (compressed) {
      return false;
    }
    compression = c;
    return true;
  }

  public Compression getCompression() {
    return compression;
  }

  public boolean connect(String h, Integer p) {
    securized = false;
    compressed = false;
    host = h;
    port = p;

    if (security == Security.ssl) {
      return openPlain() && enableTLS();
    } else {
      return openPlain();
    }
  }

  public boolean disconnect() {
    securized = false;
    compressed = false;
    try {
      if (socket.isConnected()) {
        socket.close();
      }
    } catch (Exception e) {
      client.onConnectionError(new XMPPError(XMPPError.Type.CANCEL, "gone",
        "Error clossing socket: " + e.getMessage()));
    } finally {
      client.onDisconnect();
    }
    return true;
  }

  public boolean isConnected() {
    return socket.isConnected();
  }

  public boolean enableTLS() {
    if (securized || !socket.isConnected()) {
      return false;
    }
    try {
      // TODO: Verify server certs.
      SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
      Socket plain = socket;
      socket = socketFactory.createSocket(
        plain, plain.getInetAddress().getHostName(), plain.getPort(), true);
      ((SSLSocket) socket).addHandshakeCompletedListener(new HSListener());
      socket.setSoTimeout(0);
      socket.setKeepAlive(true);
      ((SSLSocket) socket).startHandshake();

      Integer whaitHS = 0;
      try {
        while (whaitHS < SSL_HANDSHAKE_MAX_TIME) {
          if (securized == true) {
            return initIO();
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
      client.onConnectionError(new XMPPError(XMPPError.Type.CANCEL, "bad-request",
        "Error on SSL Handshake: " + e.getMessage()));
      if (!socket.isConnected()) {
        client.onDisconnect();
      }
    }
    return false;
  }

  public boolean enableCompression() {
    if (!compressed && compression != Compression.none && socket.isConnected()) {
      compressed = initIO();
    }
    if (compressed) {
      client.onCompressed();
    }
    return compressed;
  }

  public void compressionSetServerMethods(List<String> methods) {
    if (methods.contains(Compression.zlib.toString())) {
      try {
        Class.forName("com.jcraft.jzlib.ZOutputStream");
        compression = Compression.zlib;
        return;
      } catch (ClassNotFoundException e) {
        compression = Compression.none;
      }
    }
    compression = Compression.none;
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
      socket = new Socket(host, port);
      while (true) {
        try {
          if (socket.isConnected()) {
            client.onConnect();
            if (security != Security.ssl) {
              return initIO();
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
      e.printStackTrace();
      client.onConnectionError(new XMPPError(XMPPError.Type.CANCEL,
        "undefined-condition", "Error opening socket: " + e.getMessage()));
    }
    return false;
  }

  private boolean initIO() {
    if (compression == Compression.none) {
      try {
        reader = new BufferedReader(new InputStreamReader(
          socket.getInputStream(), "UTF-8"));
        writer = new BufferedWriter(new OutputStreamWriter(
          socket.getOutputStream(), "UTF-8"));
        return true;
      } catch (Exception e) {
        client.onConnectionError(new XMPPError(XMPPError.Type.CANCEL,
          "undefined-condition", "Error initializing buffers: " + e.getMessage()));
      }
    } else if (compression == Compression.zlib) {
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
        client.onConnectionError(new XMPPError(XMPPError.Type.CONTINUE,
          "feature-not-implemented", "Compression not available: " + e.getMessage()));
      }
    }
    return false;
  }

  // Listeners
  class HSListener implements HandshakeCompletedListener {
    public void handshakeCompleted(HandshakeCompletedEvent e) {
      client.onSecurized();
      securized = true;
    }
  }
}
