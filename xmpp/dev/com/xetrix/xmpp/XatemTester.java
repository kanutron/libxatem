package com.xetrix.xmpp;

import java.io.Console;

import com.xetrix.xmpp.client.XMPPClient;
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
          Log.write("Client died", 1);
          System.exit(2);
        }
        Log.write("XATEM Tester still running", 7);
        Log.write("  Client connected: " + xc.isConnected(), 7);
        Log.write("  Socket securized: " + xc.isSecurized(), 7);
        Log.write("  Socket compression: " + xc.isCompressed(), 7);
        Log.write("  Connection ID: " + xc.getConnectionID(), 7);
        Log.write("  Service: " + xc.getService(), 7);
        Log.write("  Client authed: " + xc.isAuthed(), 7);
        Log.write("  Lang: " + java.util.Locale.getDefault().getLanguage().toLowerCase(), 7);


        Thread.currentThread().sleep(5000);
      } catch (InterruptedException e) {
      }
    }
  }

  public void connectClient() {
    Log.write("Connecting client...", 6);
    if (xc.connect(this.socksec)) {
      Log.write("Client connected", 6);
    } else {
      Log.write("Client not connected", 3);
    }
  }

  public void initClient() {
    Log.write("Initiating client...", 6);
    Log.write("Username: " + this.username, 7);
    Log.write("Resource: " + this.resource, 7);
    Log.write("Server: " + this.server, 7);
    Log.write("Port: " + this.port, 7);
    Log.write("Security: " + this.socksec, 7);

    if (this.resource!="") {
      this.xc = new XMPPClient(this.username, this.password, this.resource, 24,
                               this.server, this.port);
    } else {
      this.xc = new XMPPClient(this.username, this.password);
    }
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
