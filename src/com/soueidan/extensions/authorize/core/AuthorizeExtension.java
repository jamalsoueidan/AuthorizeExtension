package com.soueidan.extensions.authorize.core;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.mongodb.*;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.ISFSExtension;
import com.smartfoxserver.v2.extensions.SFSExtension;
import com.soueidan.extensions.authorize.eventHandlers.UserLoginEventHandler;
import com.soueidan.extensions.authorize.eventHandlers.UserZoneJoinEventHandler;
import com.soueidan.extensions.authorize.requestHandlers.CreateCustomRoomRequestHandler;

public class AuthorizeExtension extends SFSExtension implements ISFSExtension {
	
	static public String LOBBY = "Lobby";
	
	static public Mongo mongo;
	static public DB db;
	static public DBCollection users;

    // Keeps a reference to the task execution
    ScheduledFuture<?> taskHandle;
    
	@Override
	public void init() {
		
		try {
			mongo = new Mongo("localhost");
			db = mongo.getDB( "games_development" );
			users = db.getCollection("users");
			
		} catch( Exception err ) {
			trace(err.getMessage());
		}
		
		trace("Listning to events...");
		
		addEventHandlers();
		
		addRequestHandlers();
		
		//addTasks();
	}
	
	public class GameGroupUpdateListTask implements Runnable {
		
		private Room lobbyRoom = getParentZone().getRoomByName(LOBBY);
		private Boolean keepSendingExtensionUpdate = true;
		
		@Override
	    public void run()
	    {	
			if ( lobbyRoom.getSize().getUserCount() == 0 ){
				return;
			}
			
			ISFSArray rooms = new SFSArray();
			
			ISFSObject room;
			ISFSObject object;
			
			User user;

	        List<Room> allGameRooms = getParentZone().getRoomListFromGroup(CreateCustomRoomRequestHandler.GROUP_GAME);
	        
	        for( Room gameRoom : allGameRooms ) {
	        	if ( gameRoom.getPlayersList().size() > 1 ) {
	        		room = new SFSObject();
	        		room.putInt("id", gameRoom.getId());
	        		room.putUtfString("name", gameRoom.getName());
	        		
	        		user = gameRoom.getUserByName(gameRoom.getVariable("invitee").getStringValue());

	        		object = new SFSObject();
	        		object.putUtfString("name", user.getName() );
	        		object.putBool("isRegistered", user.getVariable("isRegistered").getBoolValue());
	        		room.putSFSObject("invitee", object);
	        		
	        		user = gameRoom.getUserByName(gameRoom.getVariable("inviter").getStringValue());
	        		object = new SFSObject();
	        		object.putUtfString("name", user.getName() );
	        		object.putBool("isRegistered", user.getVariable("isRegistered").getBoolValue());
	        		
	        		room.putSFSObject("inviter", object);
	        		rooms.addSFSObject(room);
	        		
	        		keepSendingExtensionUpdate = true;
	        	}
	        }
	        
	        if ( keepSendingExtensionUpdate ) {
	        	trace("New object to send...");
	        	ISFSObject params = new SFSObject();
	        	params.putSFSArray("list", rooms);
	        
	        	getApi().sendExtensionResponse(RequestHandler.GAME_LIST_UPDATE, params, lobbyRoom.getUserList(), lobbyRoom, false);
	        }
	        	
	        if ( rooms.size() == 0 ) {
	        	keepSendingExtensionUpdate = false;
		    } 
	    }
	}
	
	private void addRequestHandlers() {
		addRequestHandler(RequestHandler.CREATE_CUSTOM_ROOM, CreateCustomRoomRequestHandler.class);	
	}


	private void addEventHandlers() {
		addEventHandler(SFSEventType.USER_LOGIN, UserLoginEventHandler.class);
		addEventHandler(SFSEventType.USER_JOIN_ZONE, UserZoneJoinEventHandler.class);
		//addEventHandler(SFSEventType.USER_JOIN_ROOM, UserJoinRoomEventHandler.class);
	}


	private void addTasks() {
		SmartFoxServer sfs = SmartFoxServer.getInstance();
		taskHandle = sfs.getTaskScheduler().scheduleAtFixedRate(new GameGroupUpdateListTask(), 0, 8, TimeUnit.SECONDS);
	}

	@Override
	public void destroy()
	{
	    super.destroy();
	    mongo.close();
	    trace("Database Login Extension -- stopped");
	}
}
