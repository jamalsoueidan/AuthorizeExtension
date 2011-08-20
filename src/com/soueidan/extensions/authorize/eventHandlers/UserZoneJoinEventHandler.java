package com.soueidan.extensions.authorize.eventHandlers;

import java.util.Arrays;
import java.util.List;

import com.smartfoxserver.v2.core.*;
import com.smartfoxserver.v2.entities.*;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.variables.SFSUserVariable;
import com.smartfoxserver.v2.entities.variables.UserVariable;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import com.soueidan.extensions.authorize.core.AuthorizeExtension;
import com.soueidan.extensions.authorize.core.RequestHandler;

public class UserZoneJoinEventHandler extends BaseServerEventHandler {

	private User user;
	private Room room;
	private String joinRoomName;
	
	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {		
		user = (User) event.getParameter(SFSEventParam.USER); 
		joinRoomName = (String) user.getSession().getProperty("room");
		
		joinRoomName = "testerne";
		
		trace("Joining room:", joinRoomName);
		
		setUserVariables();
		setRoom();
		
		//subscripbeGameRooms();
		sendSessionKeyToUser();
		
		getApi().joinRoom(user, room); 

		trace("--------------------------------------------------------");
	}

	private void setRoom() throws SFSException {
		room = getParentExtension().getParentZone().getRoomByName(joinRoomName);
		
		if (room == null) {
			throw new SFSException("The " + joinRoomName + " Room was not found!");
		}
	}

	private void setUserVariables() {
		UserVariable isRegistered = new SFSUserVariable("isRegistered", user.getSession().getProperty("isRegistered"));
		UserVariable session = new SFSUserVariable("session", user.getSession().getProperty("session"));
		
		session.setHidden(true);
		
		List<UserVariable> userVariables = Arrays.asList(isRegistered, session);
		getApi().setUserVariables(user, userVariables);
	}

	/*private void subscripbeGameRooms() {
		if ( joinRoomName.equals(AuthorizeExtension.LOBBY)) {
			getApi().subscribeRoomGroup(user, CreateCustomRoomRequestHandler.GROUP_GAME);
		}
	}*/

	private void sendSessionKeyToUser() {
		if ( joinRoomName.equals(AuthorizeExtension.LOBBY)) {
			ISFSObject params = new SFSObject();
			params.putUtfString("session", user.getSession().getProperty("session").toString());
			getApi().sendExtensionResponse(RequestHandler.SESSION_KEY, params, user, room, false);
		}
	}

}
