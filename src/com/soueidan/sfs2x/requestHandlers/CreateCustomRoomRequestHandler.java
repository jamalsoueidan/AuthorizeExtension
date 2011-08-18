package com.soueidan.sfs2x.requestHandlers;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.smartfoxserver.v2.api.*;
import com.smartfoxserver.v2.entities.*;
import com.smartfoxserver.v2.entities.data.*;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.exceptions.*;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.soueidan.sfs2x.AuthorizeExtension;

public class CreateCustomRoomRequestHandler extends BaseClientRequestHandler {

	static public String GROUP_GAME = "games";
	static public int MAX_USERS = 2;
	
	private User userInviter;
	private User userInvitee;
	
	private String roomName;
	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		String invitee = params.getUtfString("invitee");
		
		roomName = generateRandomWords(1)[0];
		
		userInviter = user;
		userInvitee = getApi().getUserByName(invitee);				

		createRoom();
		
		ISFSObject data = new SFSObject();
		data.putUtfString("roomName", roomName);
		
		List<User> users = Arrays.asList(userInviter, userInvitee);
		
		AuthorizeExtension extension = (AuthorizeExtension) getParentExtension();
		extension.send("createCustomRoom", data, users); 
	}

	private void createRoom() {
		trace("Create room name:", roomName);
		
		CreateRoomSettings setting = new CreateRoomSettings();
		setting.setGroupId(GROUP_GAME);
		setting.setGame(true);
		setting.setMaxUsers(MAX_USERS);
		setting.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY_AND_CREATOR_IS_GONE);
		setting.setUseWordsFilter(true);
		setting.setName(roomName);
		setting.setRoomVariables(getRoomVariables());
		
		try {			
			AuthorizeExtension extension = (AuthorizeExtension) getParentExtension();
			getApi().createRoom(extension.getParentZone(), setting, userInviter, false, null, true, false);
		} catch ( SFSCreateRoomException err ) {
			trace(err.getMessage());
		}
	}
	
	private List<RoomVariable> getRoomVariables() {
		
		RoomVariable variableInvitee = new SFSRoomVariable("invitee", userInvitee.getName());
		RoomVariable variableInviter = new SFSRoomVariable("inviter", userInviter.getName());
		
		return  Arrays.asList(variableInviter, variableInvitee);
	}
	
	private static String[] generateRandomWords(int numberOfWords)
	{
	    String[] randomStrings = new String[numberOfWords];
	    Random random = new Random();
	    for(int i = 0; i < numberOfWords; i++)
	    {
	        char[] word = new char[random.nextInt(8)+3]; // words of length 3 through 10. (1 and 2 letter words are boring.)
	        for(int j = 0; j < word.length; j++)
	        {
	            word[j] = (char)('a' + random.nextInt(26));
	        }
	        randomStrings[i] = new String(word);
	    }
	    return randomStrings;
	}
}
