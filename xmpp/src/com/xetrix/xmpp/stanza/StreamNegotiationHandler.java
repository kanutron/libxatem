package com.xetrix.xmpp.client;

import java.util.ArrayList;
import java.util.List;
import com.xetrix.xmpp.stanza.Stanza;
import com.xetrix.xmpp.payload.Session;
import org.xmlpull.v1.XmlPullParser;

public class StreamNegotiationHandler implements StanzaHandler {
  public boolean compression = false;
  public boolean tls = false;
  public boolean tlsRequired = false;
  public boolean bind = false;
  public boolean bindRequired = false;
  public boolean session = false;
  public boolean register = false;
  public List<String> saslMechs = new ArrayList<String>();
  public List<String> compMethods = new ArrayList<String>();

  private String e = "";
  private String n = "";

  public boolean wantsStanza(XmlPullParser parser) throws Exception {
    // Only accept stanzas that are immediate child of <stream>
    if (parser.getDepth() != 2) {
      return false;
    }

    e = parser.getName();
    n = parser.getNamespace();

    // Stream Features
    if (n.equals("http://etherx.jabber.org/streams") &&
        e.equals("features")) {
      return true;
    }

    // TLS
    if (n.equals("urn:ietf:params:xml:ns:xmpp-tls")) {
      if (e.equals("proceed") || e.equals("failure")) {
        return true;
      }
    }

    // Compression
    if (n.equals("http://jabber.org/protocol/compress")) {
      if (e.equals("compressed") || e.equals("failure")) {
        return true;
      }
    }

    // Authentication
    if (n.equals("urn:ietf:params:xml:ns:xmpp-sasl")) {
      if (e.equals("challenge") || e.equals("success") || e.equals("failure")) {
        return true;
      }
    }

    // Otherwise
    return false;
  }

  public boolean handleStanza(Stream stream, XmlPullParser parser) throws Exception {
    StreamListener l;

    // Stream Features
    if (n.equals("http://etherx.jabber.org/streams") &&
        e.equals("features")) {
      parseFeatures(parser);
      return processFeatures(stream);
    }

    l = stream.getListener();

    // TLS
    if (n.equals("urn:ietf:params:xml:ns:xmpp-tls")) {
      if (e.equals("proceed")) {
        return processProceed(stream);
      } else if (e.equals("failure")) {
        l.onStreamError(new XMPPError(parser));
        return false;
      }
    }

    // Compression
    if (n.equals("http://jabber.org/protocol/compress")) {
      if (e.equals("compressed")) {
        return processCompressed(stream);
      } else if (e.equals("failure")) {
        l.onStreamError(new XMPPError(parser));
        return false;
      }
    }

    // Authentication
    if (n.equals("urn:ietf:params:xml:ns:xmpp-sasl")) {
      Auth a = stream.getAuth();

      if (e.equals("challenge")) {
        String response = a.processChallenge(parser.nextText()); // TODO
        if (!response.equals("")) {
          stream.pushStanza(response);
          return true;
        } else {
          l.onStreamError(a.getError());
          return false;
        }
      } else if (e.equals("success")) {
        if (a.processSuccess()) {
          l.onAuthenticated();
          stream.initStream();
          return false;
        } else {
          l.onStreamError(a.getError());
          return false;
        }
      } else if (e.equals("failure")) {
        l.onStreamError(new XMPPError(parser));
        return false;
      }
    }

    return true;
  }

  public boolean hasStanza() {
    // Never return an stanza representation
    return false;
  }


  public Stanza getStanza() {
    // Never return an stanza representation
    return null;
  }

  public boolean finished() {
    // TODO: return true when no more work todo
    return false;
  }

  // Private methos
  private void parseFeatures(XmlPullParser parser) throws Exception {
    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        if (parser.getName().equals("starttls")) {
          parseStartTLS(parser);
        } else if (parser.getName().equals("mechanisms")) {
          parseMechanisms(parser);
        } else if (parser.getName().equals("compression")) {
          compression = true;
          parseMethods(parser);
        } else if (parser.getName().equals("bind")) {
          parseBind(parser);
        } else if (parser.getName().equals("session")) {
          parseSession(parser);
        } else if (parser.getName().equals("register")) {
          register = true;
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("features")) {
          break;
        }
      }
    }
  }

  private void parseMechanisms(XmlPullParser parser) throws Exception {
    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        String elementName = parser.getName();
        if (elementName.equals("mechanism")) {
          saslMechs.add(parser.nextText());
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("mechanisms")) {
          return;
        }
      }
    }
  }

  private void parseMethods(XmlPullParser parser) throws Exception {
    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        String elementName = parser.getName();
        if (elementName.equals("method")) {
          compMethods.add(parser.nextText());
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("compression")) {
          return;
        }
      }
    }
  }

  private void parseStartTLS(XmlPullParser parser) throws Exception {
    tls = true;
    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        String elementName = parser.getName();
        if (elementName.equals("required")) {
          tlsRequired = true;
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("starttls")) {
          return;
        }
      }
    }
  }

  private void parseBind(XmlPullParser parser) throws Exception {
    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.START_TAG) {
        String elementName = parser.getName();
        if (elementName.equals("required")) {
          bindRequired = true;
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        bind = true;
        if (parser.getName().equals("bind")) {
          return;
        }
      }
    }
  }

  private void parseSession(XmlPullParser parser) throws Exception {
    while (true) {
      int eventType = parser.next();
      if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("session")) {
          session = true;
          return;
        }
      }
    }
  }

  private boolean processFeatures(Stream stream) {
    Connection conn = stream.getConnection();
    Auth auth = stream.getAuth();
    StreamListener listener = stream.getListener();

    if (tls && !conn.isSecurized()) {
      if (conn.getSecurity() == Connection.Security.tls) {
        stream.pushStanza("<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>");
        return true;
      } else if (tlsRequired) {
        // We cannot request TLS but we should
        listener.onStreamError(new XMPPError(XMPPError.Type.CANCEL, "not-allowed",
          "TLS required by server but not allowed by this actual client settings."));
        return false;
      }
    }

    if (!stream.isAuthed()) {
      if (auth.setServerMechanisms(saslMechs)) {
        String s = auth.startAuthWith(auth.getBestMechanism());
        if (!"".equals(s)) {
          stream.pushStanza(s);
          return true;
        } else {
          listener.onStreamError(auth.getError());
          return false;
        }
      } else {
        listener.onStreamError(auth.getError());
        return false;
      }
    }

    if (compression && !conn.isCompressed()) {
      if (conn.compressionSetServerMethods(compMethods)) {
        stream.pushStanza("<compress xmlns='http://jabber.org/protocol/compress'>" +
                   "<method>" + conn.getCompression().toString() + "</method></compress>");
        return true;
      }
    }

    if (bind) {
      listener.onReadyForBindResource(bindRequired);
    }

    if (session) {
      listener.onReadyForStartSession();
    } else {
      // Does not support session. Assume opened.
      listener.onSessionStarted(new Session());
    }

    return true;
  }

  private boolean processProceed(Stream stream) {
    Connection conn = stream.getConnection();
    StreamListener listener = stream.getListener();
    if (conn.enableTLS()) {
      stream.initStream();
    } else {
      listener.onStreamError(new XMPPError(XMPPError.Type.CANCEL, "bad-request",
        "Start TLS failed."));
    }
    return false;
  }

  private boolean processCompressed(Stream stream) {
    Connection conn = stream.getConnection();
    StreamListener listener = stream.getListener();
    if (conn.enableCompression()) {
      stream.initStream();
    } else {
      listener.onStreamError(new XMPPError(XMPPError.Type.CANCEL, "bad-request",
        "Start compression failed."));
    }
    return false;
  }

}