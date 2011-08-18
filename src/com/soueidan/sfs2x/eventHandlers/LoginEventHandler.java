package com.soueidan.sfs2x.eventHandlers;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.Random;

import com.mongodb.*;
import com.smartfoxserver.bitswarm.sessions.ISession;
import com.smartfoxserver.v2.core.*;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.*;
import com.smartfoxserver.v2.extensions.*;
import com.soueidan.sfs2x.AuthorizeExtension;

public class LoginEventHandler extends BaseServerEventHandler {

	private DBObject document;
	private String userName;
	private String cryptedPass;
	private SFSObject customData;
	
	private String generateSession;
	private Boolean isRegistered;
	
	private ISession session;
	
	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
		trace("--------------------------------------------------------");
		// Grab parameters from client request
		userName = (String) event.getParameter(SFSEventParam.LOGIN_NAME);
		cryptedPass = (String) event.getParameter(SFSEventParam.LOGIN_PASSWORD);
		session = (ISession) event.getParameter(SFSEventParam.SESSION);
		
		customData = (SFSObject) event.getParameter(SFSEventParam.LOGIN_IN_DATA);
		
		String joinRoom = AuthorizeExtension.LOBBY;
		
		if ( customData == null || customData.getUtfString("session") == null ) {
			lobbyLoginAsGuestOrRegistered(event);
		} else {
			joinRoom = customData.getUtfString("room");
			generateSession = customData.getUtfString("session");
			customRoomLogin(event);
		}
		
		trace("Session loggin in: ", generateSession);
		
		session.setProperty("room", joinRoom);
		session.setProperty("isRegistered", isRegistered);
		session.setProperty("session", generateSession);
	}
	
	private void customRoomLogin(ISFSEvent event) throws SFSLoginException {
		trace("Game Login by session");
		
		DBCollection users = AuthorizeExtension.users;
		
		BasicDBObject query = new BasicDBObject();
		query.put("session", generateSession);
		
		DBCursor cursor = users.find(query);

        if ( !cursor.hasNext() ) {
        	trace("Game Login User not found!", generateSession);
        	SFSErrorData data = new SFSErrorData(SFSErrorCode.LOGIN_BAD_PASSWORD);
			data.addParameter(userName);
			
			throw new SFSLoginException("Login failed for user: "  + userName,data);
        } else {
        	trace("Game Login User logged in!", generateSession);
        	document = cursor.next();
        
        	ISFSObject outData = (ISFSObject) event.getParameter(SFSEventParam.LOGIN_OUT_DATA);
            outData.putUtfString(SFSConstants.NEW_LOGIN_NAME, document.get("nickname").toString());
            
        	Boolean isGuest = (Boolean) document.get("is_guest");
        	if ( isGuest == null ) {
        		isRegistered = false;
        	} else {
        		isRegistered = true;	
        	}
        }
	}

	private void lobbyLoginAsGuestOrRegistered(ISFSEvent event) throws SFSLoginException {
		try {
			generateSession = MungPass(userName + session);
	    } catch( NoSuchAlgorithmException err ) {
	    	trace(err.getMessage());
	    }
		
		if ( cryptedPass.isEmpty() ) {	        
			handleGuest(event);
		} else {
			handleUser(event);
			
			if ( document == null ) {
				SFSErrorData data = new SFSErrorData(SFSErrorCode.LOGIN_BAD_PASSWORD);
				data.addParameter(userName);
				
				throw new SFSLoginException("Login failed for user: "  + userName,data);
			}
		}
		
	}

	private void handleUser(ISFSEvent event) {
		trace("Registered user logged in", userName);
		
		DBCollection users = AuthorizeExtension.users;
		
		BasicDBObject query = new BasicDBObject();
		query.put("email", userName);
		
		DBCursor cursor = users.find(query);

        if ( !cursor.hasNext() ) {
        	trace("user not found");
        	return;
        }

		document = cursor.next();
	        
		String password_digest = (String) document.get("password_digest");
			
		if (!getApi().checkSecurePassword(session, password_digest, cryptedPass)) {
			trace("password wrong");
			return;
		}
		
		document.put("session", generateSession);
		users.update(query, document);

		isRegistered = true;
	}
	
	private void handleGuest(ISFSEvent event) throws NoSuchElementException {
		trace("Guest is loggin in", userName);
		
		DBCollection users = AuthorizeExtension.users;
		
		// Create random num 0..99
		Random randomGenerator = new Random();
		userName = userName + randomGenerator.nextInt(100);
		
		// Set new Name
		ISFSObject outData = (ISFSObject) event.getParameter(SFSEventParam.LOGIN_OUT_DATA);
        outData.putUtfString(SFSConstants.NEW_LOGIN_NAME, userName);
        
		// Insert DB
		BasicDBObject newUser = new BasicDBObject();
		newUser.put("nickname", userName);
		newUser.put("session", generateSession);
		newUser.put("is_guest", true);
		
    	users.insert(newUser);
        
		isRegistered = false;
	}

	public static String MungPass(String pass) throws NoSuchAlgorithmException {
		MessageDigest m = MessageDigest.getInstance("MD5");
		byte[] data = pass.getBytes(); 
		m.update(data,0,data.length);
		BigInteger i = new BigInteger(1,m.digest());
		return String.format("%1$032X", i);
	}
}
