package com.andreadelorenzis.videoconferencep2p.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SignalingHandler extends TextWebSocketHandler {

    // Mappa delle sessioni per gestire i peer nelle diverse stanze
    private Map<String, Map<String, WebSocketSession>> rooms = new HashMap<>();
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    private static final int MAX_USERS_PER_ROOM = 3;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        
        System.out.println("JSON ricevuto: " + payload);
        
        JsonNode jsonMessage = objectMapper.readTree(payload);
        String messageType = jsonMessage.get("type").asText();
        String fromSessionId = session.getId();
        
        if ("joinRoom".equals(messageType)) {
        	String roomName = jsonMessage.get("room").asText();
        	joinRoom(session, roomName);
        } else if ("offer".equals(messageType)    		  ||
        		   "answer".equals(messageType)    		  ||
        		   "candidate".equals(messageType) 		  ||
        		   "adminUpdateState".equals(messageType) ||
        		   "adminRemovePeer".equals(messageType)) {
        	String roomName = (String) session.getAttributes().get("room");
        	String targetPeerId = jsonMessage.get("to").asText();
    	    Map<String, WebSocketSession> roomSessions = rooms.get(roomName);
    	    if (roomSessions != null) {
    	    	WebSocketSession targetSession = roomSessions.get(targetPeerId);
    	    	if (targetSession != null) {
    	    		((ObjectNode) jsonMessage).put("from", fromSessionId);
    	    		targetSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(jsonMessage)));
    	    	} else {
    	    		session.sendMessage(new TextMessage("Target peer not found in room: " + roomName));
    	    	}
            } else {
                session.sendMessage(new TextMessage("Target peer not found: " + fromSessionId));
            }
        } else if ("updateState".equals(messageType)) {
        	String roomName = (String) session.getAttributes().get("room");
        	((ObjectNode) jsonMessage).put("from", fromSessionId);
        	broadcastMessageToOtherClients(jsonMessage, roomName, fromSessionId);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    }

    public void joinRoom(WebSocketSession session, String roomName) throws Exception {
    	session.getAttributes().put("room", roomName);
    	
    	rooms.putIfAbsent(roomName, new HashMap<>());
    	Map<String, WebSocketSession> roomSessions = rooms.get(roomName);
    	
    	boolean isAdmin = roomSessions.isEmpty();
    	session.getAttributes().put("isAdmin", isAdmin);
    	
    	if (roomSessions.size() >= MAX_USERS_PER_ROOM) {
            ObjectNode errorMessage = objectMapper.createObjectNode();
            errorMessage.put("type", "error");
            errorMessage.put("message", "Room full: " + roomName);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMessage)));
    		session.close();
            return;
    	}
    
    	roomSessions.put(session.getId(), session);

    	Set<String> activeSessionIds = rooms.get(roomName).keySet().stream()
    			.filter(id -> !id.equals(session.getId()))
    			.collect(Collectors.toSet());
    	
    	Map<String, Object> message = new HashMap<>();
    	message.put("type", "join");
    	message.put("isAdmin", isAdmin);
    	message.put("sessionIds", activeSessionIds);
    	session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomName = (String) session.getAttributes().get("room");
        if (roomName != null) {
        	Map<String, WebSocketSession> roomSessions = rooms.get(roomName);
        	if (roomSessions != null) {
        		roomSessions.remove(session.getId());
        		
        		ObjectNode message = objectMapper.createObjectNode();
        		message.put("type", "disconnected");
        		message.put("id", session.getId());
        		
        		broadcastMessageToOtherClients(message, roomName, session.getId());
        		
        		if (roomSessions.isEmpty()) {
        			rooms.remove(roomName);
        		}
        	}
        }
    }
    
    public void broadcastMessageToOtherClients(JsonNode message, String roomName, String fromSessionId) throws Exception {
    	Map<String, WebSocketSession> roomSessions = rooms.get(roomName);
    	
    	if (roomSessions != null) {
            String messageStr = objectMapper.writeValueAsString(message);
            
            for (Map.Entry<String, WebSocketSession> entry : roomSessions.entrySet()) {
            	if (!entry.getKey().equals(fromSessionId)) {
            		entry.getValue().sendMessage(new TextMessage(messageStr));
            	}
            }
    	}
    	
    }
    
    
}
