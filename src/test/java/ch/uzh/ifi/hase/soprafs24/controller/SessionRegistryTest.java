package ch.uzh.ifi.hase.soprafs24.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SessionRegistryTest {

    private SessionRegistry sessionRegistry;
    private WebSocketSession mockSession1;
    private WebSocketSession mockSession2;
    private WebSocketSession mockSession3;

    @BeforeEach
    public void setup() {
        sessionRegistry = new SessionRegistry();
        
        // Create mock WebSocketSessions
        mockSession1 = mock(WebSocketSession.class);
        mockSession2 = mock(WebSocketSession.class);
        mockSession3 = mock(WebSocketSession.class);
        
        // Set up identifiable session IDs for easier debugging
        when(mockSession1.getId()).thenReturn("session1");
        when(mockSession2.getId()).thenReturn("session2");
        when(mockSession3.getId()).thenReturn("session3");
    }

    @Test
    public void testAddSession_NewLobby() {
        // Add a session to a new lobby
        sessionRegistry.addSession(1L, mockSession1);
        
        // Verify the session was added correctly
        Set<WebSocketSession> sessions = sessionRegistry.getSessions(1L);
        assertEquals(1, sessions.size());
        assertTrue(sessions.contains(mockSession1));
    }

    @Test
    public void testAddSession_ExistingLobby() {
        // Add a session to a lobby
        sessionRegistry.addSession(1L, mockSession1);
        
        // Add another session to the same lobby
        sessionRegistry.addSession(1L, mockSession2);
        
        // Verify both sessions are in the lobby
        Set<WebSocketSession> sessions = sessionRegistry.getSessions(1L);
        assertEquals(2, sessions.size());
        assertTrue(sessions.contains(mockSession1));
        assertTrue(sessions.contains(mockSession2));
    }

    @Test
    public void testAddSession_MultipleLobby() {
        // Add sessions to different lobbies
        sessionRegistry.addSession(1L, mockSession1);
        sessionRegistry.addSession(2L, mockSession2);
        
        // Verify sessions are in the correct lobbies
        Set<WebSocketSession> lobby1Sessions = sessionRegistry.getSessions(1L);
        assertEquals(1, lobby1Sessions.size());
        assertTrue(lobby1Sessions.contains(mockSession1));
        
        Set<WebSocketSession> lobby2Sessions = sessionRegistry.getSessions(2L);
        assertEquals(1, lobby2Sessions.size());
        assertTrue(lobby2Sessions.contains(mockSession2));
    }

    @Test
    public void testRemoveSession_ExistingSession() {
        // Add sessions
        sessionRegistry.addSession(1L, mockSession1);
        sessionRegistry.addSession(1L, mockSession2);
        
        // Remove one session
        sessionRegistry.removeSession(1L, mockSession1);
        
        // Verify the session was removed
        Set<WebSocketSession> sessions = sessionRegistry.getSessions(1L);
        assertEquals(1, sessions.size());
        assertFalse(sessions.contains(mockSession1));
        assertTrue(sessions.contains(mockSession2));
    }

    @Test
    public void testRemoveSession_LastSessionInLobby() {
        // Add a single session to a lobby
        sessionRegistry.addSession(1L, mockSession1);
        
        // Remove the session
        sessionRegistry.removeSession(1L, mockSession1);
        
        // Verify the lobby was removed (returns empty set)
        Set<WebSocketSession> sessions = sessionRegistry.getSessions(1L);
        assertTrue(sessions.isEmpty());
    }

    @Test
    public void testRemoveSession_NonExistentLobby() {
        // Try to remove a session from a non-existent lobby
        sessionRegistry.removeSession(999L, mockSession1);
        
        // Verify no errors occur
        Set<WebSocketSession> sessions = sessionRegistry.getSessions(999L);
        assertTrue(sessions.isEmpty());
    }

    @Test
    public void testRemoveSession_NonExistentSession() {
        // Add a session
        sessionRegistry.addSession(1L, mockSession1);
        
        // Try to remove a different session
        sessionRegistry.removeSession(1L, mockSession2);
        
        // Verify the original session is still there
        Set<WebSocketSession> sessions = sessionRegistry.getSessions(1L);
        assertEquals(1, sessions.size());
        assertTrue(sessions.contains(mockSession1));
    }

    @Test
    public void testGetSessions_ExistingLobby() {
        // Add sessions
        sessionRegistry.addSession(1L, mockSession1);
        sessionRegistry.addSession(1L, mockSession2);
        
        // Get sessions for the lobby
        Set<WebSocketSession> sessions = sessionRegistry.getSessions(1L);
        
        // Verify correct sessions are returned
        assertEquals(2, sessions.size());
        assertTrue(sessions.contains(mockSession1));
        assertTrue(sessions.contains(mockSession2));
    }

    @Test
    public void testGetSessions_NonExistentLobby() {
        // Get sessions for a non-existent lobby
        Set<WebSocketSession> sessions = sessionRegistry.getSessions(999L);
        
        // Verify an empty set is returned
        assertNotNull(sessions);
        assertTrue(sessions.isEmpty());
    }

    @Test
    public void testConcurrentOperations() {
        // Add sessions to different lobbies
        sessionRegistry.addSession(1L, mockSession1);
        sessionRegistry.addSession(2L, mockSession2);
        sessionRegistry.addSession(3L, mockSession3);
        
        // Perform multiple operations
        sessionRegistry.addSession(1L, mockSession3);
        sessionRegistry.removeSession(2L, mockSession2);
        
        // Verify lobby 1 has two sessions
        Set<WebSocketSession> lobby1Sessions = sessionRegistry.getSessions(1L);
        assertEquals(2, lobby1Sessions.size());
        assertTrue(lobby1Sessions.contains(mockSession1));
        assertTrue(lobby1Sessions.contains(mockSession3));
        
        // Verify lobby 2 is empty
        Set<WebSocketSession> lobby2Sessions = sessionRegistry.getSessions(2L);
        assertTrue(lobby2Sessions.isEmpty());
        
        // Verify lobby 3 has one session
        Set<WebSocketSession> lobby3Sessions = sessionRegistry.getSessions(3L);
        assertEquals(1, lobby3Sessions.size());
        assertTrue(lobby3Sessions.contains(mockSession3));
    }
}
