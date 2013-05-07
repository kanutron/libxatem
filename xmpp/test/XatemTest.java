package com.xetrix;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Date;
import java.text.SimpleDateFormat;

import java.net.URL;
import java.net.URLEncoder;
import java.net.URLConnection;

import com.xetrix.xmpp.client.Client;
import com.xetrix.xmpp.client.listener.StreamListener;
import com.xetrix.xmpp.client.listener.ConnectionListener;
import com.xetrix.xmpp.client.XMPPError;
import com.xetrix.xmpp.stanza.IQ;
import com.xetrix.xmpp.stanza.Presence;
import com.xetrix.xmpp.stanza.listener.PresenceListener;
import com.xetrix.xmpp.payload.Payload;


public class XatemTest implements ConnectionListener, StreamListener {
  // Constants
  private static final String    PROG_NAME = "XAT'EM Tester - A Jabber client by XETRIX";
  private static final String    VERSION = "0.1";
  private static final String    COPYRIGHT = "(c) 2011 - XETRIX";

  private static final SimpleDateFormat
                                 DATE_FORMATTER = new SimpleDateFormat("dd-MM HH:mm:ss");

  private String     account;

  private String     username;
  private String     password;
  private String     resource;
  private String     server;
  private Integer    port;
  private Integer    socksec;
  private Client     xc;

  private String     webServiceURL;

  public XatemTest(String ac, Properties prop) {
    webServiceURL = prop.getProperty("webservice");
    account       = ac;
    username      = prop.getProperty(ac + ".username");
    password      = prop.getProperty(ac + ".password");
    resource      = prop.getProperty(ac + ".resource");
    server        = prop.getProperty(ac + ".host");
    port          = Integer.parseInt(prop.getProperty(ac + ".port"));
    socksec       = Integer.parseInt(prop.getProperty(ac + ".securitymode"));
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
    Log.write("Connecting: " + account, 6);
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
      public String getXmlns() {return null;}
      public String getName() {return null;}
    });
    iq.setFrom(xc.getFullJid());
    iq.setId(xc.getNextStanzaId());
    xc.pushStanza(iq);

    // Set own presence
    Presence p = new Presence(null, Presence.Show.away);
    xc.pushStanza(p);
    Log.write(account + ": " + "Presence sent: " + p.toXML(),7);

    // Listen for presences and act accordingly
    PresenceListener plis = new PresenceListener();
    xc.addStanzaInListener(plis);

    // Process incoming presence stanzas
    HashMap<String,String> hmData = new HashMap<String,String>();
    Date todaysDate = new java.util.Date();
    String formattedDate;

    while (true) {
      p = plis.waitStanza(); // Blocking
      Log.write(p.toString(),7);

      todaysDate = new java.util.Date();
      formattedDate = DATE_FORMATTER.format(todaysDate);

      hmData.put("_packetType", "presence");
      hmData.put("date", formattedDate);
      hmData.put("from", p.getFrom());
      hmData.put("type", p.getType().toString());
      try {
        hmData.put("mode", p.getShow().toString());
      } catch (Exception e) {
        hmData.put("mode", "null");
      }
      hmData.put("priority", Integer.toString(p.getPriority()));
      hmData.put("status", p.getStatus());
      hmData.put("xml", p.toXML());

      PostData(hmData);
      plis.setProcessed();
      hmData.clear();
    }
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

	public String PackHashMap(HashMap<String,String> hmData) {
		String data = "";
		hmData.put("accountUsername", username);
		hmData.put("accountResource", resource);
		hmData.put("accountPriority", "0");
		hmData.put("accountServer", server);
		hmData.put("accountPort", port.toString());
		int n = hmData.size();

		Set set = hmData.entrySet();
		Iterator i = set.iterator();
		while(i.hasNext()) {
			Map.Entry me = (Map.Entry)i.next();
			String key = (String)me.getKey();
			String value = "n/a";
			try {
				value = URLEncoder.encode((String)me.getValue(), "UTF-8");
			} catch (UnsupportedEncodingException e ) {
				value = "";
			} catch (Exception e) {
				value = "";
			}
			data += key + "=" + value + "&";
		}
		return data;
	}

	public void PostData(HashMap<String,String> hmData) {
		String data = PackHashMap(hmData);
		try {
			URL url = new URL(webServiceURL);
			URLConnection uconn = url.openConnection();
			uconn.setDoOutput(true);
			OutputStreamWriter wr = new OutputStreamWriter(uconn.getOutputStream());
			wr.write(data);
			wr.flush();
			BufferedReader rd = new BufferedReader(new InputStreamReader(uconn.getInputStream()));
			String line;
			Integer line_n=0;
			Boolean isok = false;
			while ((line = rd.readLine()) != null) {
				line_n++;
				if (line_n==1 && Integer.parseInt(line.substring(0,3)) == 200) {
					isok = true;
				} else if (!isok) {
					Log.write(line,3);
				} else {
					Log.write(line,7);
				}
			}
			wr.close();
			rd.close();
		} catch (Exception e) {
			Log.write("POST RESULTS: " + e.toString(),3);
		}
	}

  // ////////////////////////////////////////////////

  public static void main(String args[]) {
    final Properties prop = new Properties();
    try {
      prop.load(new FileInputStream("test/xatemtester.properties"));
    } catch (Exception e) {
      Log.write("No properties file found: test/xatemtester.properties", 1);
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
