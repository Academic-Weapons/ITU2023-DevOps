package dk.itu.minitwit.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.Model;

import dk.itu.minitwit.database.SQLite;
import dk.itu.minitwit.domain.AddMessage;
import dk.itu.minitwit.domain.Login;
import dk.itu.minitwit.domain.Register;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@SpringBootTest
public class MiniTwitControllerTests {
    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");

    MiniTwitController miniTwitController;
    HttpServletRequest request;
    Model model;
    HttpSession session;
    SQLite sqLiteMock;
    Login login;
    Register register;

    @BeforeEach
    public void setUp() {
        miniTwitController = new MiniTwitController();
        request = mock(HttpServletRequest.class);
        login = mock(Login.class);
        model = mock(Model.class);
        session = mock(HttpSession.class);
        sqLiteMock = mock(SQLite.class);
        register = mock(Register.class);
        miniTwitController.sqLite = sqLiteMock;
    }

    @Test
    void testFollowUserNotLoggedIn() throws SQLException {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(null);
    
        String username = "testUser";
    
        // Add mock response for sqLiteMock.queryDb
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("user_id", 2);
        when(sqLiteMock.queryDb(anyString(), anyList())).thenReturn(List.of(userMap));
    
        // Act
        String result = miniTwitController.followUser(username, request, model);
    
        // Assert
        assertEquals("redirect:/testUser", result);
    }
    

    @Test
    void testFollowUserNotFound() throws SQLException {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn("loggedInUser");
        when(session.getAttribute("user_id")).thenReturn(1);
        when(model.getAttribute("requestID")).thenReturn(UUID.randomUUID().toString());
    
        String username = "testUserNotFound";
        when(sqLiteMock.queryDb(anyString(), anyList())).thenThrow(SQLException.class);
    
        // Act
        String result = miniTwitController.followUser(username, request, model);
    
        // Assert
        assertEquals("", result);
    }

    @Test
    void testFollowUserSuccess() throws SQLException {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn("loggedInUser");
        when(session.getAttribute("user_id")).thenReturn(1);
        when(model.getAttribute("requestID")).thenReturn(UUID.randomUUID().toString());

        String username = "testUser";
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("user_id", 2);
        when(sqLiteMock.queryDb(anyString(), anyList())).thenReturn(List.of(userMap));

        when(sqLiteMock.updateDb(anyString(), anyList())).thenReturn(1);

        // Act
        String result = miniTwitController.followUser(username, request, model);

        // Assert
        assertEquals("redirect:/" + username, result);
        verify(sqLiteMock, times(1)).updateDb(anyString(), anyList());
    }
    
    
    @Test
    void testUnfollowUserLoggedIn() throws SQLException {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn("loggedInUser");
        when(session.getAttribute("user_id")).thenReturn(1);
        when(model.getAttribute("requestID")).thenReturn(UUID.randomUUID().toString());
    
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("user_id", 2);
        when(sqLiteMock.queryDb(anyString(), anyList())).thenReturn(List.of(userMap));
    
        when(sqLiteMock.updateDb(anyString(), anyList())).thenReturn(1);
    
        // Act
        String template = miniTwitController.unfollowUser("testUser", request, model);
    
        // Assert
        assertEquals("redirect:/testUser", template);
        verify(sqLiteMock, times(1)).updateDb(anyString(), anyList());
    }
    
    

    

    @Test
    void testAddMessageNotLoggedIn() {
        when(request.getSession(false)).thenReturn(session);
        when(model.getAttribute("requestID")).thenReturn(UUID.randomUUID().toString());

        AddMessage text = new AddMessage(null);
        text.setText("Test message");

        // Act
        String template = miniTwitController.addMessage(text, request, model);

        // Assert
        assertEquals("redirect:/public", template);
    }

    @Test
    void testAddMessageLoggedIn() throws SQLException {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn("testUser");
        when(session.getAttribute("user_id")).thenReturn(1);
        when(model.getAttribute("requestID")).thenReturn(UUID.randomUUID().toString());

        AddMessage text = new AddMessage(null);
        text.setText("Test message");

        when(sqLiteMock.updateDb(anyString(), anyList())).thenReturn(1);

        // Act
        String template = miniTwitController.addMessage(text, request, model);

        // Assert
        assertEquals("redirect:/public", template);
        verify(sqLiteMock, times(1)).updateDb(anyString(), anyList());
    }

    @Test
    public void testAddMessageToFavouritesNotLoggedIn() {
        when(request.getSession(false)).thenReturn(session);
        when(model.getAttribute("requestID")).thenReturn(UUID.randomUUID().toString());

        // Act
        String template = miniTwitController.addMessageToFavourites("1", request, model);

        // Assert
        assertEquals("redirect:/public", template);
    }

    @Test
    public void testAddMessageToFavouritesLoggedIn() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn("testUser");
        when(session.getAttribute("user_id")).thenReturn(1);
        when(model.getAttribute("requestID")).thenReturn(UUID.randomUUID().toString());

        miniTwitController.sqLite = sqLiteMock;

        // Act
        String template = miniTwitController.addMessageToFavourites("1", request, model);

        // Assert
        assertEquals("redirect:/public", template);
    }

    @Test
    public void testLoginPostMethodInvalidUsername() throws SQLException {
        when(request.getMethod()).thenReturn("POST");
        when(model.getAttribute("requestID")).thenReturn(UUID.randomUUID().toString());
        when(request.getSession()).thenReturn(session);

        when(login.getUsername()).thenReturn("nonExistentUser");
        when(login.getPassword()).thenReturn("password");

        SQLite sqLiteMock = mock(SQLite.class);
        when(sqLiteMock.queryDb(anyString(), anyList())).thenReturn(Collections.emptyList());
        miniTwitController.sqLite = sqLiteMock;

        // Act
        String template = miniTwitController.login(login, model, request);

        // Assert
        assertEquals("login.html", template);
    }

    @Test
    public void testLoginPostMethodSuccessfulLogin() throws SQLException {
        when(request.getMethod()).thenReturn("POST");
        when(model.getAttribute("requestID")).thenReturn(UUID.randomUUID().toString());
        when(request.getSession()).thenReturn(session);

        when(login.getUsername()).thenReturn("testUser");
        when(login.getPassword()).thenReturn("password");

        Map<String, Object> userRow = new HashMap<>();
        userRow.put("user_id", 1);
        userRow.put("username", "testUser");
        userRow.put("pw_hash", new BCryptPasswordEncoder().encode("password"));

        SQLite sqLiteMock = mock(SQLite.class);
        when(sqLiteMock.queryDb(anyString(), anyList())).thenReturn(Collections.singletonList(userRow));
        miniTwitController.sqLite = sqLiteMock;

        miniTwitController.passwordEncoder = new BCryptPasswordEncoder();

        // Act
        String template = miniTwitController.login(login, model, request);

        // Assert
        assertEquals("redirect:/public", template);
    }

    @Test
    public void testLoginPostMethodInvalidPassword() throws SQLException {
        when(request.getMethod()).thenReturn("POST");
        when(model.getAttribute("requestID")).thenReturn(UUID.randomUUID().toString());
        when(request.getSession()).thenReturn(session);

        when(login.getUsername()).thenReturn("testUser");
        when(login.getPassword()).thenReturn("invalidPassword");

        Map<String, Object> userRow = new HashMap<>();
        userRow.put("user_id", 1);
        userRow.put("username", "testUser");
        userRow.put("pw_hash", new BCryptPasswordEncoder().encode("password"));

        SQLite sqLiteMock = mock(SQLite.class);
        when(sqLiteMock.queryDb(anyString(), anyList())).thenReturn(Collections.singletonList(userRow));
        miniTwitController.sqLite = sqLiteMock;

        miniTwitController.passwordEncoder = new BCryptPasswordEncoder();

        // Act
        String template = miniTwitController.login(login, model, request);

        // Assert
        assertEquals("login.html", template);
    }

    @Test
    public void testRegisterGetMethod() {
        when(request.getMethod()).thenReturn("GET");
        when(model.getAttribute("requestID")).thenReturn(UUID.randomUUID().toString());

        // Act
        String template = miniTwitController.register(register, model, request);

        // Assert
        assertEquals("register.html", template);
    }

    @Test
    public void testRegisterPostMethod() {
        when(request.getMethod()).thenReturn("POST");
        when(model.getAttribute("requestID")).thenReturn(UUID.randomUUID().toString());

        when(register.getUsername()).thenReturn("testUser");
        when(register.getEmail()).thenReturn("test@example.com");
        when(register.getPassword()).thenReturn("password");
        when(register.getPassword2()).thenReturn("password");

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        miniTwitController.passwordEncoder = passwordEncoder;

        SQLite sqLiteMock = mock(SQLite.class);
        miniTwitController.sqLite = sqLiteMock;

        // Act
        String template = miniTwitController.register(register, model, request);

        // Assert
        assertEquals("redirect:/login", template);
    }

    @Test
    public void testRegisterPostMethodEmptyUsername() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        Model model = mock(Model.class);
        Register register = mock(Register.class);

        when(request.getMethod()).thenReturn("POST");
        when(model.getAttribute("requestID")).thenReturn(UUID.randomUUID().toString());

        when(register.getUsername()).thenReturn("");
        when(register.getEmail()).thenReturn("test@example.com");
        when(register.getPassword()).thenReturn("password");
        when(register.getPassword2()).thenReturn("password");

        // Act
        String template = miniTwitController.register(register, model, request);

        // Assert
        assertEquals("register.html", template);
    }

    @Test
    public void testRegisterPostMethodEmptyEmail() {
        when(request.getMethod()).thenReturn("POST");
        when(model.getAttribute("requestID")).thenReturn(UUID.randomUUID().toString());

        when(register.getUsername()).thenReturn("testUser");
        when(register.getEmail()).thenReturn("");
        when(register.getPassword()).thenReturn("password");
        when(register.getPassword2()).thenReturn("password");

        // Act
        String template = miniTwitController.register(register, model, request);

        // Assert
        assertEquals("register.html", template);
    }

    @Test
    public void testRegisterPostMethodEmptyPassword() {
        when(request.getMethod()).thenReturn("POST");
        when(model.getAttribute("requestID")).thenReturn(UUID.randomUUID().toString());

        when(register.getUsername()).thenReturn("testUser");
        when(register.getEmail()).thenReturn("test@example.com");
        when(register.getPassword()).thenReturn("");
        when(register.getPassword2()).thenReturn("");

        // Act
        String template = miniTwitController.register(register, model, request);

        // Assert
        assertEquals("register.html", template);
    }

    @Test
    public void testRegisterPostMethodMismatchedPasswords() {
        when(request.getMethod()).thenReturn("POST");
        when(model.getAttribute("requestID")).thenReturn(UUID.randomUUID().toString());

        when(register.getUsername()).thenReturn("testUser");
        when(register.getEmail()).thenReturn("test@example.com");
        when(register.getPassword()).thenReturn("password");
        when(register.getPassword2()).thenReturn("wrong_password");

        // Act
        String template = miniTwitController.register(register, model, request);

        // Assert
        assertEquals("register.html", template);
    }

    @Test
    public void testLogout() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/logout");
        when(request.getSession()).thenReturn(session);
        when(model.getAttribute("requestID")).thenReturn(UUID.randomUUID().toString());

        // Act
        String template = miniTwitController.logout(request, model);

        // Assert
        verify(session, times(1)).invalidate();
        assertEquals("redirect:/public", template);
    }

    @Test
    public void testRequestId() {
        // Act
        String requestId = miniTwitController.requestId();

        // Assert
        assertNotNull(requestId);
        assertDoesNotThrow(() -> UUID.fromString(requestId));
    }

    @Test
    public void testGetUserID() throws SQLException {
        // Arrange
        String username = "testUser";
        int expectedUserId = 42;

        List<Map<String, Object>> userIDs = new ArrayList<>();
        Map<String, Object> userData = new HashMap<>();
        userData.put("user_id", expectedUserId);
        userIDs.add(userData);

        SQLite sqLiteMock = mock(SQLite.class);
        when(sqLiteMock.queryDb(anyString(), anyList())).thenReturn(userIDs);

        miniTwitController.sqLite = sqLiteMock;

        // Act
        int actualUserId = miniTwitController.getUserID(username);

        // Assert
        assertEquals(expectedUserId, actualUserId);
    }

    @Test
    public void testAddUserToModelWithSession() {
        Model model = mock(Model.class);
        HttpSession session = mock(HttpSession.class);
        String sampleUser = "testUser";

        when(session.getAttribute("user")).thenReturn(sampleUser);

        boolean result = MiniTwitController.addUserToModel(model, session);

        assertEquals(true, result);
        verify(model, times(1)).addAttribute("user", sampleUser);
    }

    @Test
    public void testAddUserToModelWithoutSession() {
        Model model = mock(Model.class);
        HttpSession session = null;

        boolean result = MiniTwitController.addUserToModel(model, session);

        assertEquals(false, result);
        verify(model, times(1)).addAttribute("user", "false");
    }

    @Test
    public void testAddDatesAndGravatarURLs() {
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("email", "test@example.com");
        message.put("pub_date", 1616701200L); // March 25, 2021 12:00:00 AM UTC
        messages.add(message);

        miniTwitController.addDatesAndGravatarURLs(messages);

        assertEquals(1, messages.size());
        Map<String, Object> updatedMessage = messages.get(0);

        assertTrue(updatedMessage.containsKey("gravatar_url"));
        String expectedGravatarUrl = "https://www.gravatar.com/avatar/"
                + miniTwitController.getMD5Hash("test@example.com") + "?d=identicon&s=80";
        assertEquals(expectedGravatarUrl, updatedMessage.get("gravatar_url"));

        assertTrue(updatedMessage.containsKey("date_time"));
        String expectedDateTime = "mar. 25,2021 20:40:00";
        assertEquals(expectedDateTime, updatedMessage.get("date_time"));
    }

    @Test
    public void testGetMD5Hash() {
        String email = "test@example.com";
        String expectedHash = DigestUtils.md5Hex(email.toLowerCase());
        String actualHash = miniTwitController.getMD5Hash(email);
        assertEquals(expectedHash, actualHash);
    }

    @Test
    public void testGetDuration() {
        long before = 1616701200000L; // March 25, 2021 12:00:00 AM UTC
        long after = 1616704800000L; // March 25, 2021 1:00:00 AM UTC
        double expected = 3600.0;
        double actual = miniTwitController.getDuration(before, after);
        assertEquals(expected, actual, 0.01);
    }

}
