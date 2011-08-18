package com.soueidan.sfs2x.eventHandlers;

import java.util.Arrays;
import java.util.List;

import com.smartfoxserver.v2.core.*;
import com.smartfoxserver.v2.entities.*;
import com.smartfoxserver.v2.entities.variables.SFSUserVariable;
import com.smartfoxserver.v2.entities.variables.UserVariable;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import com.soueidan.sfs2x.requestHandlers.CreateCustomRoomRequestHandler;

public class ZoneJoinEventHandler extends BaseServerEventHandler {

	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {		
		User user = (User) event.getParameter(SFSEventParam.USER); 
		String joinRoomName = (String) user.getSession().getProperty("room");
		
		trace("Joining room:", joinRoomName);
		
		UserVariable isRegistered = new SFSUserVariable("isRegistered", user.getSession().getProperty("isRegistered"));
		UserVariable session = new SFSUserVariable("session", user.getSession().getProperty("session"));
		
		List<UserVariable> userVariables = Arrays.asList(isRegistered, session);
		getApi().setUserVariables(user, userVariables);

		Room room = getParentExtension().getParentZone().getRoomByName(joinRoomName);
		
		if (room == null) {
			throw new SFSException("The " + joinRoomName + " Room was not found!");
		}
		
		//getApi().sendExtensionResponse("", arg1, arg2, arg3, arg4)
		getApi().joinRoom(user, room);

		trace("--------------------------------------------------------");
	}

}
