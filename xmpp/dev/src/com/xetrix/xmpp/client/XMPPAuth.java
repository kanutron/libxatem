package com.xetrix.xmpp.client;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.xetrix.xmpp.util.Base64;

public class XMPPAuth {
  private XMPPClient client;
  private static List<String> clientMechs = new ArrayList<String>();
  private List<String> serverMechs = new ArrayList<String>();

  // Common class data
  static {
    // Order is important. Most secure first
    clientMechs.add("DIGEST-MD5");
    clientMechs.add("PLAIN");
    clientMechs.add("ANONYMOUS");
  }

  // Life cycle
  private String   currentMech = "";
  private Integer  currentStep = 0;

  // Auth data
  private String  username;
  private String  password;
  private String  resource;
  private String  service;

  // Constructors
  public XMPPAuth(XMPPClient c) {
    client = c;
  }

  void initAuthData(String u, String p, String r, String s) {
    username = u;
    password = p;
    resource = r;
    service  = s;
  }

  void setServerMechanisms(List<String> mechs) {
    serverMechs = mechs;
  }

  List<String> getServerMechanisms() {
    return serverMechs;
  }

  List<String> getClientMechanisms() {
    return clientMechs;
  }

  List<String> getAvailableMechanisms() {
    List<String> mechs = new ArrayList<String>();
    // We iterate trough client mechs because we assume they are sortered by strongest first
    Iterator itr = clientMechs.iterator();
    while(itr.hasNext()) {
      Object m = itr.next();
      if (serverMechs.contains(m)) {
        mechs.add((String)m);
      }
    }
    return mechs;
  }

  String getBestMechanism() {
    // We iterate trough client mechs because we assume they are sortered by strongest first
    Iterator itr = clientMechs.iterator();
    while(itr.hasNext()) {
      Object m = itr.next();
      if (serverMechs.contains(m)) {
        return (String)m;
      }
    }
    return "";
  }

  void startAuthWith(String mech) {
    currentMech = mech;
    if (mech=="DIGEST-MD5") {
      client.stream.pushStanza(
        "<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='DIGEST-MD5'/>");
      currentStep++;
    } else if (mech=="PLAIN") {
      String plaindata = Base64.encodeString(
        (char)0 + username + (char)0 + password);
      client.stream.pushStanza(
        "<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='PLAIN'>" +
        plaindata + "</auth>");
    } else if (mech=="ANONYMOUS") {
      client.stream.pushStanza(
        "<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='ANONYMOUS'/>");
    }
  }

  void processResponse(String response) {
    if (currentMech=="DIGEST-MD5") {
      switch (currentStep) {
        case 1:
          processDIGESTMD5(Base64.decodeString(response));
          break;
        case 2:
          client.stream.pushStanza(
            "<response xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\"/>");
          break;
      }
    }
  }

  // Private stuff

  private void processDIGESTMD5(String data) {
    String currentKey = "";
    Map<String,String> mapChallenge = new HashMap<String, String>();
    Map<String,String> mapResponse = new HashMap<String, String>();

    String[] items = data.split(",");

    // Decode challenge
    for(int i=0; i < items.length ; i++) {
      String[] kv = new String[2];
      if (items[i].indexOf("=")>0) {
        kv[0] = items[i].substring(0, items[i].indexOf("="));
        kv[1] = items[i].substring(items[i].indexOf("=")+1);
        currentKey = kv[0];
        if (kv[1].substring(0,1).equals("\"")) {
          kv[1] = kv[1].substring(1,kv[1].length()-1);
        }
        mapChallenge.put(kv[0],kv[1]);
      } else if (currentKey!="") {
        mapChallenge.put(currentKey,mapChallenge.get(currentKey) + "," + items[i]);
      }
    }

    // Fix challenge
    if (!mapChallenge.containsKey("digest-uri")) {
      mapChallenge.put("digest-uri", "xmpp/" + service);
    }
    if (mapChallenge.containsKey("qop")) {
      String qop = mapChallenge.get("qop");
      if (qop != "auth" && qop.indexOf("auth") == 0) {
        mapChallenge.put("qop","auth");
      }
    }

    // Put cnonce to challenge
    byte[] tmpRand = new byte[32];
    Random r = new Random();
    for(int i=0; i<32; i++) {
      tmpRand[i] = (byte)r.nextInt(255);
    }
    mapChallenge.put("cnonce", new String(Base64.encode(tmpRand)));

    // Prepare response
    String cPass = processDIGESTMD5cryptpassword(mapChallenge);
    if (cPass.equals("")) {
      return;
    }

    mapResponse.put("username",username);
    mapResponse.put("response",cPass);
    mapResponse.put("charset","utf-8");
    mapResponse.put("nc","00000001");
    mapResponse.put("qop","auth");
    mapResponse.put("digest-uri",mapChallenge.get("digest-uri"));
    mapResponse.put("cnonce",mapChallenge.get("cnonce"));
    if (mapChallenge.containsKey("nonce")) {
      mapResponse.put("nonce",mapChallenge.get("nonce"));
    }
    if (mapChallenge.containsKey("realm")) {
      mapResponse.put("realm",mapChallenge.get("realm"));
    }

    // Pack response
    StringBuilder tmp = new StringBuilder();
    StringBuilder response = new StringBuilder();

    Iterator itr = mapResponse.entrySet().iterator();
    while(itr.hasNext()) {
      Map.Entry e = (Map.Entry)itr.next();
      tmp.append(e.getKey() + "=\"" + e.getValue() + "\"");
      if (itr.hasNext()) {
        tmp.append(",");
      }
    }

    response.append("<response xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
    response.append(Base64.encodeString(tmp.toString()));
    response.append("</response>");

    // Send response
    currentStep++;
    client.stream.pushStanza(response.toString());
  }

  private String processDIGESTMD5cryptpassword(Map<String,String> mapChallenge) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      String pack = "";

      mapChallenge.put("nc", "00000001");
      if (!mapChallenge.containsKey("realm")) {
        mapChallenge.put("realm","");
      }
      if (!mapChallenge.containsKey("cnonce")) {
        mapChallenge.put("cnonce","");
      }

      // A1
      pack = username + ":" + mapChallenge.get("realm") + ":" + password;
      md.update(md.digest(pack.getBytes()));
      pack = ":" + mapChallenge.get("nonce") + ":" + mapChallenge.get("cnonce");
      if (mapChallenge.containsKey("authzid")) {
        pack += ":" + mapChallenge.get("authzid");
      }
      md.update(pack.getBytes());
      String a1md5 = getHexString(md.digest());

      // A2
      pack = "AUTHENTICATE:" + mapChallenge.get("digest-uri");
      String a2md5 = getHexString(md.digest(pack.getBytes()));

      // Hash response
      pack = a1md5 + ":" +
             mapChallenge.get("nonce") + ":" +
             mapChallenge.get("nc") + ":" +
             mapChallenge.get("cnonce") + ":" +
             mapChallenge.get("qop") + ":" +
             a2md5;
      return getHexString(md.digest(pack.getBytes()));

    } catch (NoSuchAlgorithmException e2) {
      client.onStreamError(new XMPPError(XMPPError.Type.CANCEL,
        "feature-not-implemented", "No susch algorithm MD5 implemented."));
      return "";
    }
  }

  private static String getHexString(byte[] b) {
    String result = "";
    for (int i=0; i < b.length; i++) {
      result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
    }
    return result;
  }

}