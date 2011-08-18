package com.soueidan.sfs2x;

import com.mongodb.*;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.extensions.ISFSExtension;
import com.smartfoxserver.v2.extensions.SFSExtension;

public class AuthorizeExtension extends SFSExtension implements ISFSExtension {
	
	static public Mongo mongo;
	static public DB db;
	static public DBCollection users;
	@Override
	public void init() {
		// TODO Auto-generated method stub
		
		trace("Connecting to mongoDB...");
		
		try {
			mongo = new Mongo("localhost");
			db = mongo.getDB( "games_development" );
			users = db.getCollection("users");
			
		} catch( Exception err ) {
			trace(err.getMessage());
		}
		
		trace("Listning to events...");
		
		addEventHandler(SFSEventType.USER_LOGIN, LoginEventHandler.class);
		addEventHandler(SFSEventType.USER_JOIN_ZONE, ZoneJoinEventHandler.class);
	}
	
	
	@Override
	public void destroy()
	{
	    super.destroy();
	    trace("Database Login Extension -- stopped");
	}
	
	
}
