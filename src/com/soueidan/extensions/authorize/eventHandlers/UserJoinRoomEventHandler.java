package com.soueidan.extensions.authorize.eventHandlers;

import com.smartfoxserver.v2.core.*;
import com.smartfoxserver.v2.entities.*;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import com.soueidan.extensions.authorize.core.AuthorizeExtension;
import com.soueidan.extensions.authorize.core.RequestHandler;
import com.soueidan.extensions.authorize.requestHandlers.CreateCustomRoomRequestHandler;

public class UserJoinRoomEventHandler extends BaseServerEventHandler {

	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
		User user = (User) event.getParameter(SFSEventParam.USER);
		
		Room room = user.getLastJoinedRoom();
		
		trace("IS this correct room group?", room.getGroupId());
		if ( room.getGroupId().equals(CreateCustomRoomRequestHandler.GROUP_GAME )) {
			trace("Goes down", room.getPlayersList().size(), CreateCustomRoomRequestHandler.MAX_USERS);
			if ( room.getPlayersList().size() >= CreateCustomRoomRequestHandler.MAX_USERS ) {
				trace("Dispatch NEW_GAME_STARTED to lobby");
				Room lobby = getParentExtension().getParentZone().getRoomByName(AuthorizeExtension.LOBBY);
				getApi().sendExtensionResponse(RequestHandler.NEW_GAME_STARTED, null, lobby.getUserList(), lobby, false);
			}
		}

	}

}
