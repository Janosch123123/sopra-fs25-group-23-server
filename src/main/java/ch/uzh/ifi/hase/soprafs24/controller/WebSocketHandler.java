package ch.uzh.ifi.hase.soprafs24.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class WebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    // Add this constructor
    public WebSocketHandler() {
        System.out.println("WebSocketHandler constructor called!");
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // This method is called when a new WebSocket connection is established
        System.out.println("afterConnectionEstablished called with session: " + session.getId());
        logger.info("New WebSocket connection established: {}", session.getId());
        
        // Extract token from URL query parameters
        String query = session.getUri().getQuery();
        String token = null;
        if (query != null && query.contains("token=")) {
            token = query.substring(query.indexOf("token=") + 6);
            // If there are other parameters after the token, trim them
            if (token.contains("&")) {
                token = token.substring(0, token.indexOf("&"));
            }
        }
        
        System.out.println("Connection token: " + token);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode response = mapper.createObjectNode();
        response.put("type", "connection_success");
        response.put("message", "Connection established successfully");
        
        // Send as JSON string
        session.sendMessage(new TextMessage(mapper.writeValueAsString(response)));
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // This method is called when a text message is received from the client
        System.out.println("handleTextMessage called with message: " + message.getPayload());

        logger.info("Received message from {}: {}", session.getId(), message.getPayload());
        
        // Simply echo the message back (you can implement more logic later)
        session.sendMessage(new TextMessage("Server received: " + message.getPayload()));
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("afterConnectionClosed called for session: " + session.getId());

        // This method is called when the WebSocket connection is closed
        logger.info("WebSocket connection closed: {} with status {}", session.getId(), status);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.out.println("handleTransportError called for session: " + session.getId());

        // This method is called when a transport error occurs
        logger.error("Error in WebSocket transport for session: {}", session.getId(), exception);
    }
}