package com.xetrix;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.xetrix.xmpp.client.Client;
import com.xetrix.xmpp.client.StreamListener;
import com.xetrix.xmpp.client.ConnectionListener;
import com.xetrix.xmpp.client.XMPPError;
import com.xetrix.xmpp.payload.Bind;
import com.xetrix.xmpp.payload.Session;

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

  public XatemTest(String ac) {
    account = ac;
    try {
      Properties prop = new Properties();
      prop.load(new FileInputStream("test/" + ac + ".properties"));

      username = prop.getProperty("username");
      password = prop.getProperty("password");
      resource = prop.getProperty("resource");
      server   = prop.getProperty("host");
      port     = Integer.parseInt(prop.getProperty("port"));
      socksec  = Integer.parseInt(prop.getProperty("securitymode"));
    }
    catch (Exception e) {
      Log.write("No properties file found for account " + ac + " (" + ac + ".properties)", 1);
      account = "";
      return;
    }
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
    Log.write(account + ": " + "Disconnected.",6);
  }

  public void onSecurized() {
    Log.write(account + ": " + "SSL handshake done.",7);
  }

  public void onCompressed() {
    Log.write(account + ": " + "Compression enabled.",7);
  }

  public void onConnectionError(XMPPError e) {
    Log.write(account + ": Conn: " + e.toString(),3);
  }

  public void onStreamOpened(String from) {
    Log.write(account + ": " + "Stream opened; from: " + from,7);
  }

  public void onReadyForBindResource() {
  }

  public void onResourceBinded(Bind bind) {
    Log.write(account + ": " + "Binded as " + xc.getFullJid(),6);
  }

  public void onReadyForStartSession() {
  }

  public void onSessionStarted(Session session) {
    Log.write(account + ": " + "Session started.",6);
    /*XMPPStanzaIQ iq = new XMPPStanzaIQ(XMPPStanzaIQ.Type.get) {
      public String getPayloadXML() {
        return "<query xmlns=\"jabber:iq:roster\"></query>";
      }
    };
    iq.setFrom(this.getFullJid());
    this.stream.pushStanza(iq);
    this.stream.pushStanza("<presence></presence>");*/
  }

  public void onStreamClosed() {
    Log.write(account + ": " + "Stream closed.",7);
  }

  public void onStreamError(XMPPError e) {
    Log.write(account + ": Stream: " + e.toString(),3);
  }

  public void onAuthenticated() {
    Log.write(account + ": " + "Authenticated",7);
  }

  // ////////////////////////////////////////////////

  public static void banner() {
    System.out.println(PROG_NAME + " " + VERSION);
    System.out.println(COPYRIGHT + "\n");
  }

  public static void main(String args[]) {
    Thread t1 = new Thread() {
      public void run() {
        XatemTest x = new XatemTest("gtalk");
        x.start();
      }
    };
    Thread t2 = new Thread() {
      public void run() {
        XatemTest x = new XatemTest("facebook");
        x.start();
      }
    };
    Thread t3 = new Thread() {
      public void run() {
        XatemTest x = new XatemTest("jabber");
        x.start();
      }
    };

    t1.setName("XatemTesterGtalk");
    t1.start();
    t2.setName("XatemTesterFacebook");
    t2.start();
    t3.setName("XatemTesterJabber");
    t3.start();
  }
}
