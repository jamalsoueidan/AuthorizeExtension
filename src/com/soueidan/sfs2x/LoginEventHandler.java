package com.soueidan.sfs2x;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;

import com.mongodb.*;
import com.smartfoxserver.bitswarm.sessions.ISession;
import com.smartfoxserver.v2.core.*;
import com.smartfoxserver.v2.exceptions.*;
import com.smartfoxserver.v2.extensions.*;

public class LoginEventHandler extends BaseServerEventHandler {

	private DBObject document;
	private String userName;
	private String cryptedPass;
	
	private String generateSession;
	private Boolean isRegistered;
	
	private 
	ISession session;
	
	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
		trace("LET US TEST EXTENSION");
		
		// Grab parameters from client request
		userName = (String) event.getParameter(SFSEventParam.LOGIN_NAME);
		cryptedPass = (String) event.getParameter(SFSEventParam.LOGIN_PASSWORD);
		session = (ISession) event.getParameter(SFSEventParam.SESSION);
		
		try {
			generateSession = MungPass(userName + session);
	    } catch( NoSuchAlgorithmException err ) {
	    	trace(err.getMessage());
	    }
		
		if ( cryptedPass.isEmpty() ) {	        
			handleGuest(event);
		} else {
			handleUser(event);
		}		
		
		if ( document == null ) {
			SFSErrorData data = new SFSErrorData(SFSErrorCode.LOGIN_BAD_PASSWORD);
			data.addParameter(userName);
			
			throw new SFSLoginException("Login failed for user: "  + userName,data);
		}
		
		session.setProperty("isRegistered", isRegistered);
		session.setProperty("session", generateSession);
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
		
		BasicDBObject query = new BasicDBObject();
		query.put("nickname", userName);
		DBCursor cursor = users.find(query);
		
        if ( !cursor.hasNext() ) {
        	cursor.close();
        	
        	BasicDBObject newUser = new BasicDBObject();
			newUser.put("nickname", userName);
			newUser.put("session", generateSession);
	    	users.insert(newUser);
	    	
	    	cursor = users.find(query);
        }

        document = cursor.next();
        document.put("session", generateSession);
        document.put("is_guest", true);
        
        users.update(query, document);
        
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
