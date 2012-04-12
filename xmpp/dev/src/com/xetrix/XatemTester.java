package com.xetrix.xmpp;

import java.io.Console;

import com.xetrix.xmpp.client.XMPPClient;
import com.xetrix.xmpp.client.XMPPClientListener;
import com.xetrix.xmpp.client.XMPPError;
import com.xetrix.xmpp.client.XMPPStanzaIQBind;

import com.xetrix.xmpp.util.Log;

import jargs.gnu.CmdLineParser;

public class XatemTester {
  // Constants
  private static final String    PROG_NAME = "XAT'EM Tester - A Jabber client by XETRIX";
  private static final String    VERSION = "0.1";
  private static final String    COPYRIGHT = "(c) 2011 - XETRIX";

  private XMPPClient             xc;

  private String     username;
  private String     password;
  private String     resource;
  private String     server;
  private Integer    port;
  private Integer    socksec;

  public XatemTester() {
  }

  public void MainLoop() {
    this.initClient();
    this.connectClient();
    while (true) {
      try {
        if (!xc.isConnected()) {
          System.exit(2);
        }
        Thread.currentThread().sleep(2500);
      } catch (InterruptedException e) {
      }
    }
  }

  public void connectClient() {
    if (!xc.connect(this.socksec)) {
      Log.write("Error connecting.", 3);
    }
  }

  public void initClient() {
    Log.write("Initiating client...", 6);
    if (this.resource!="") {
      this.xc = new XMPPClient(this.username, this.password, this.resource, 24,
        this.server, this.port);
    } else {
      this.xc = new XMPPClient(this.username, this.password);
    }

    this.xc.setListener(new XMPPClientListener() {
      // Event Handlers
      public void onConnect() {
        Log.write("Connected.",6);
      }
      public void onDisconnect() {
        Log.write("Disconnected.",1);
      }
      public void onSecurized() {
        Log.write("SSL handshake done.",6);
      }
      public void onCompressed() {
        Log.write("Compression enabled.",6);
      }
      public void onConnectionError(XMPPError e) {
        Log.write(e.toString(),3);
      }
      public void onStreamOpened(String cid, String from) {
        Log.write("Stream opened; from: " + from + ", id: " + cid,6);
      }
      public void onStreamClosed() {
        Log.write("Stream closed.",6);
      }
      public void onStreamError(XMPPError e) {
        Log.write(e.toString(),3);
      }
      public void onReadyforAuthentication() {
        Log.write("Ready to authenticate.",6);
      }
      public void onAuthenticated() {
        Log.write("Authenticated",6);
      }
      public void onResourceBinded(XMPPStanzaIQBind bind) {
        Log.write("Binded as " + xc.getFullJid(),6);
        /*XMPPStanzaIQ iq = new XMPPStanzaIQ(XMPPStanzaIQ.Type.get) {
          public String getPayloadXML() {
            return "<query xmlns=\"jabber:iq:roster\"></query>";
          }
        };
        iq.setFrom(this.getFullJid());
        this.stream.pushStanza(iq);
        this.stream.pushStanza("<presence></presence>");*/
      }
    });
  }

  public static void banner() {
    System.out.println(PROG_NAME + " " + VERSION);
    System.out.println(COPYRIGHT + "\n");
  }

  public static void usage() {
    banner();
    System.out.println("Usage:");
    System.out.println("xatem [options] jabberId [resource] [server] [port] [secmode]\n");
  }

  public static void main(String args[]) {
    String username = "";
    String password = "";
    String resource = "";
    String server = "";
    String port = "";
    String socksec = "0";

    try {
      CmdLineParser parser = new CmdLineParser();
      parser.parse(args);
      try {
        String [] remargs = parser.getRemainingArgs();
        username = remargs[0];
        if (remargs.length>1) {
          resource = remargs[1];
          if (remargs.length>2) {
            server = remargs[2];
            if (remargs.length>3) {
              port = remargs[3];
              if (remargs.length>4) {
                socksec = remargs[4];
              }
            }
          }
        } else {
          resource = "";
        }

        Console cons;
        char[] passwd;
        if ((cons = System.console()) != null &&
          (passwd = cons.readPassword("Password for %s:", username)) != null) {
          password = new String(passwd);
          java.util.Arrays.fill(passwd, ' ');
        }
      } catch ( Exception e ) {
        e.printStackTrace();
        System.err.println("Parse required arguments error.\n\n");
        usage();
        System.exit(2);
      }

      XatemTester xatem = new XatemTester();
      xatem.username = username;
      xatem.password = password;
      xatem.resource = resource;
      xatem.server = server;
      xatem.port = Integer.parseInt(port);
      xatem.socksec = Integer.parseInt(socksec);

      Log.write("All input args processed");
      Log.write("Entering to the main loop", 7);

      xatem.MainLoop();
    } catch (Exception e) {
      System.err.println("!!! Runtime error:");
      System.err.println(e.getMessage()+"\n\n");
      usage();
      System.exit(2);
    }
  }
}
