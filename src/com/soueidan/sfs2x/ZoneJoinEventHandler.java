package com.soueidan.sfs2x;

import java.util.Arrays;
import java.util.List;

import com.smartfoxserver.v2.core.*;
import com.smartfoxserver.v2.entities.*;
import com.smartfoxserver.v2.entities.variables.SFSUserVariable;
import com.smartfoxserver.v2.entities.variables.UserVariable;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

public class ZoneJoinEventHandler extends BaseServerEventHandler {

	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
		User user = (User) event.getParameter(SFSEventParam.USER); 
		
		UserVariable isRegistered = new SFSUserVariable("isRegistered", user.getSession().getProperty("isRegistered"));
		UserVariable session = new SFSUserVariable("session", user.getSession().getProperty("session"));
		
		List<UserVariable> userVariables = Arrays.asList(isRegistered, session);
		getApi().setUserVariables(user, userVariables);

		Room lobby = getParentExtension().getParentZone().getRoomByName("Lobby");
		
		if (lobby == null)
			throw new SFSException("The Lobby Room was not found! Make sure a Room called 'The Lobby' exists in the Zone to make this example work correctly.");
		
		getApi().joinRoom(user, lobby);
	}

}
