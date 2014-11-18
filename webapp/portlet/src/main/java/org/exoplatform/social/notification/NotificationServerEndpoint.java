package org.exoplatform.social.notification;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.exoplatform.commons.notification.impl.service.Message;
import org.exoplatform.commons.notification.impl.service.Message.MessageDecoder;
import org.exoplatform.commons.notification.impl.service.Message.MessageEncoder;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;


@ServerEndpoint(value = "/notify/{remoteId}", encoders = { MessageEncoder.class }, decoders = { MessageDecoder.class })
public class NotificationServerEndpoint {
  
  private static final Log          LOG      = ExoLogger.getLogger(NotificationServerEndpoint.class);

  private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<Session>());

  public NotificationServerEndpoint() {
  }
  
  @OnOpen
  public void onOpen (Session session, @PathParam("remoteId") final String remoteId) {
    session.getUserProperties().put("remoteId", remoteId);
    sessions.add(session);
  }

  @OnMessage
  public void onMessage (Session session, Message message) {
    if (sessions.contains(session)) {
      sessions.remove(session);
    }
    String receiverId = message.getTo();
    if (receiverId == null || receiverId.length() == 0) {
      return;
    }
    //
    try {
      Iterator<Session> iterator = sessions.iterator();
      while (iterator.hasNext()) {
        Session ses = iterator.next();
        if (ses == null || ! ses.isOpen()) {
          sessions.remove(ses);
          continue;
        }
        String userSession = ses.getUserProperties().get("remoteId") != null ? ses.getUserProperties().get("remoteId").toString() : null;
        if (userSession == null || userSession.length() == 0 || ! receiverId.equals(userSession)) {
          continue;
        }
        ses.getBasicRemote().sendObject(message);
      }
    } catch (IOException e) {
      LOG.error("IOException : " + e, e.getMessage());
    } catch (EncodeException e) {
      LOG.error("EncodeException : " + e, e.getMessage());
    } 
  } 
  
  @OnClose
  public void onClose (Session session, CloseReason reason) {
    try {
      sessions.remove(session);
    } catch (Exception e) {
      LOG.warn("Exception during close session");
    } finally {
      LOG.info("Closing a WebSocket due to " + reason.getReasonPhrase());
    }
  }
  
  @OnError
  public void onError (Session session, Throwable throwable) {
    sessions.remove(session);
    LOG.error("Session error", throwable);
  }
}