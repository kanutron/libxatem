package com.xetrix.xmpp.client;

import java.util.List;
import java.io.Reader;
import java.io.Writer;

import com.xetrix.xmpp.client.listener.ConnectionListener;

public interface Connection {
  // Pulic methods
  public void setListener(ConnectionListener l);
  public ConnectionListener getListener();

  public boolean setSecurity(Security s);
  public Security getSecurity();
  public boolean setCompression(Compression c);
  public Compression getCompression();

  public boolean connect(String h, Integer p);
  public boolean disconnect();

  public boolean isConnected();
  public boolean isCompressed();
  public boolean isSecurized();

  public Reader getReader();
  public Writer getWriter();

  public boolean enableTLS();
  public boolean enableCompression();

  public boolean compressionSetServerMethods(List<String> methods);

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
}
