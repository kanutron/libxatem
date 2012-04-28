package com.xetrix;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.xetrix.xmpp.client.Client;
import com.xetrix.xmpp.client.listener.StreamListener;
import com.xetrix.xmpp.client.listener.ConnectionListener;
import com.xetrix.xmpp.client.XMPPError;
import com.xetrix.xmpp.stanza.IQ;
import com.xetrix.xmpp.payload.Payload;

public class XatemTest implements ConnectionListener, StreamListener {
  // Constants
  private static final String    PROG_NAME = "XAT'EM Tester - A Jabber client by XETRIX";
  private static final String    VERSION = "0.1";
  private static final String    COPYRIGHT = "(c) 2011 - XETRIX";

  private String     account;

  private String     username;
  private String     password;
  private String     resource;
  private String     server;
  private Integer    port;
  private Integer    socksec;
  private Client     xc;

  public XatemTest(String ac, Properties prop) {
    account = ac;
    username = prop.getProperty(ac + ".username");
    password = prop.getProperty(ac + ".password");
    resource = prop.getProperty(ac + ".resource");
    server   = prop.getProperty(ac + ".host");
    port     = Integer.parseInt(prop.getProperty(ac + ".port"));
    socksec  = Integer.parseInt(prop.getProperty(ac + ".securitymode"));
  }

  public void start() {
    if ("".equals(account)) {
      return;
    }
    init();
    connect();
    while (true) {
      try {
        if (!xc.isConnected()) {
          return;
        }
        Thread.currentThread().sleep(1000);
      } catch (InterruptedException e) {
      }
    }
  }

  public void connect() {
    xc.connect(this.socksec);
  }

  public void init() {
    Log.write("Initiating client: " + account, 6);
    this.xc = new Client();
    if (this.resource!="") {
      this.xc.setUserData(username, password, resource, 24, server, port);
    } else {
      this.xc.setUserData(username, password);
    }
    this.xc.setConnectionListener(this);
    this.xc.setStreamListener(this);
  }

  // Event handlers
  public void onConnect() {
    Log.write(account + ": " + "Connected.",7);
  }

  public void onDisconnect() {
    Log.write(account + ": " + "Disconnected.",3);
  }

  public void onSecurized() {
    Log.write(account + ": " + "SSL handshake done.",7);
  }

  public void onCompressed() {
    Log.write(account + ": " + "Compression enabled.",7);
  }

  public void onConnectionError(XMPPError e) {
    if (e.getType() == XMPPError.Type.AUTH || e.getType() == XMPPError.Type.CANCEL) {
      Log.write(account + ": Conn: " + e.toString(),3);
    } else {
      Log.write(account + ": Conn: " + e.toString(),5);
    }
  }

  public void onStreamOpened(String from) {
    Log.write(account + ": " + "Stream opened; from: " + from,7);
  }

  public void onBindRequested(Boolean required) {
    Log.write(account + ": " + "Bind requested.",7);
  }

  public void onSessionRequested() {
    Log.write(account + ": " + "Session requested.",7);
  }

  public void onResourceBinded(String j, String r) {
    Log.write(account + ": " + "Binded as " + xc.getFullJid(),7);
  }

  public void onSessionStarted() {
    Log.write(account + ": " + "Session started.",6);
    // DEBUG
    IQ iq = new IQ(IQ.Type.get, new Payload() {
      public String toXML() {
        return "<query xmlns=\"jabber:iq:roster\"></query>";
      };
    });
    iq.setFrom(xc.getFullJid());
    iq.setId(xc.getNextStanzaId());
    xc.pushStanza(iq);
    xc.pushStanza("<presence></presence>");
  }

  public void onStreamClosed() {
    Log.write(account + ": " + "Stream closed.",7);
  }

  public void onStreamError(XMPPError e) {
    if (e.getType() == XMPPError.Type.AUTH || e.getType() == XMPPError.Type.CANCEL) {
      Log.write(account + ": Stream: " + e.toString(),3);
    } else {
      Log.write(account + ": Stream: " + e.toString(),5);
    }
  }

  public void onAuthenticated() {
    Log.write(account + ": " + "Authenticated",6);
  }

  // ////////////////////////////////////////////////

  public static void banner() {
    System.out.println(PROG_NAME + " " + VERSION);
    System.out.println(COPYRIGHT + "\n");
  }

  public static void main(String args[]) {
    final Properties prop = new Properties();
    try {
      prop.load(new FileInputStream("test/xatemtester.properties"));
    } catch (Exception e) {
      Log.write("No properties file found for account test/xatemtester.properties", 1);
      return;
    }

    String a = prop.getProperty("accounts");
    final String[] accounts = a.split(",", 10);

    Thread[] threads = new Thread[accounts.length];

    for (int i=0; i<accounts.length; i++) {
      final String account = accounts[i];
      threads[i] = new Thread() {
        public void run() {
          XatemTest x = new XatemTest(account, prop);
          x.start();
        }
      };
      threads[i].setName("XatemTester-" + accounts[i]);
      threads[i].start();
    }
  }
}
