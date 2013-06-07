/**
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
* 
* Copyright (c) 2012 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License as published by the Free Software
* Foundation; either version 3.0 of the License, or (at your option) any later
* version.
* 
* BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
* PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License along
* with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
*
*/
package org.bigbluebutton.conference;

import org.slf4j.Logger;
import org.bigbluebutton.conference.meeting.messaging.MessagePublisher;
import org.bigbluebutton.conference.service.messaging.MessagingService;
import org.bigbluebutton.conference.service.messaging.redis.MessageHandler;
import org.bigbluebutton.conference.service.poll.PollApplication;
import org.bigbluebutton.conference.service.presentation.ConversionUpdatesMessageListener;
import org.red5.logging.Red5LoggerFactory;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MeetingsManager {
	private static Logger log = Red5LoggerFactory.getLogger(MeetingsManager.class, "bigbluebutton");
	
	private final Map <String, Meeting> meetings;

	private MessagePublisher publisher;
	
	public MeetingsManager() {
		meetings = new ConcurrentHashMap<String, Meeting>();		
	}
	
	public void addRoom(Meeting room) {
		room.addRoomListener(new ParticipantUpdatingRoomListener(room, messagingService)); 	
		
		publisher.meetingStarted(room.getName());
		
		meetings.put(room.getName(), room);
	}
	
	public void removeRoom(String name) {
		log.debug("Remove room " + name);
		Meeting room = meetings.remove(name);
		if (room != null) {
			room.endAndKickAll();
			publisher.meetingEnded(name);
		}
	}

	public void destroyAllRooms() {
		for (Map.Entry<String, Meeting> entry : meetings.entrySet()) {
		    Meeting room = entry.getValue();
		    room.endAndKickAll();
		}
	}
			
	public boolean hasRoom(String name) {
		return meetings.containsKey(name);
	}
	
	public int numberOfRooms() {
		return meetings.size();
	}
	
	/**
	 * Keeping getRoom private so that all access to Room goes through here.
	 */
	//TODO: this method becomes public for ParticipantsApplication, ask if it's right? 
	public Meeting getRoom(String name) {
		log.debug("Get room " + name);
		return meetings.get(name);
	}
	
	public Map getParticipants(String roomName) {
		Meeting r = getRoom(roomName);
		if (r != null) {
			return r.getParticipants();
		}
		log.warn("Getting participants from a non-existing room " + roomName);
		return null;
	}
	
	public void addRoomListener(String roomName, IRoomListener listener) {
		Meeting r = getRoom(roomName);
		if (r != null) {
			r.addRoomListener(listener);
			return;
		}
		log.warn("Adding listener to a non-existing room " + roomName);
	}
	
	public void addParticipant(String roomName, User participant) {
		log.debug("Add participant " + participant.getName());
		Meeting r = getRoom(roomName);
		if (r != null) {
			r.addParticipant(participant);
			return;
		}
	}
	
	public void removeParticipant(String roomName, String userid) {
		log.debug("Remove participant " + userid + " from " + roomName);
		Meeting r = getRoom(roomName);
		if (r != null) {
			if (checkPublisher()) {
				//conferenceEventListener.participantsUpdated(r);
				//missing method()?
			}
			r.removeParticipant(userid);

			return;
		}
		log.warn("Removing listener from a non-existing room " + roomName);
	}
	
	public void changeParticipantStatus(String roomName, String userid, String status, Object value) {
		Meeting r = getRoom(roomName);
		if (r != null) {
			r.changeParticipantStatus(userid, status, value);
			return;
		}		
	}
	
	public ArrayList<String> getCurrentPresenter( String room){
		Meeting r = getRoom(room);
		if (r != null) {
			return r.getCurrentPresenter();		
		}	
		
		return null;
	}
	
	public void assignPresenter(String room, ArrayList<String> presenter){
		Meeting r = getRoom(room);
		if (r != null) {
			r.assignPresenter(presenter);
			return;
		}	
	}
	
	public void endMeeting(String meetingID) {
		Meeting room = getRoom(meetingID); 
		if (room != null) {
			room.endAndKickAll();
		} 		
	}	
	
	public void setPublisher(MessagePublisher publisher) {
		this.publisher = publisher;
	}
}