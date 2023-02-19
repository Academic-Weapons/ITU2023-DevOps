package dk.itu.minitwit.controller;

import dk.itu.minitwit.database.SQLite;
import dk.itu.minitwit.domain.Register;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class SimulatorController {

    @Autowired
    SQLite sqLite;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private int LATEST = 0;

    private void updateLatest(int latest) {
        LATEST = latest != -1 ? latest : LATEST;
    }

    public ResponseEntity<Object> notReqFromSimulator(HttpServletRequest request) {
        String fromSimulator = request.getHeader("Authorization");
        if (!"Basic c2ltdWxhdG9yOnN1cGVyX3NhZmUh".equals(fromSimulator)) {
            String error = "You are not authorized to use this resource!";
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
        return null;
    }

    @GetMapping("/latest")
    public ResponseEntity<Object> getLatest() {
        return ResponseEntity.ok("{\"latest\":" + LATEST + "}");
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody Register register,
                                           @RequestParam(value = "latest", required = false, defaultValue = "-1") int latest) throws SQLException {
        updateLatest(latest);
        if (register.getUsername() == null) {
            return ResponseEntity.badRequest().body("{\"status\":400, \"error_msg\":\"You have to enter a username\"}");
        } else if (register.getEmail() == null || !register.getEmail().contains("@")) {
            return ResponseEntity.badRequest().body("{\"status\":400, \"error_msg\":\"You have to enter a valid email address\"}");
        } else if (register.getPassword() == null) {
            return ResponseEntity.badRequest().body("{\"status\":400, \"error_msg\":\"You have to enter a password\"}");
        } else if (sqLite.getUserId(register.getUsername()) != null) {
            return ResponseEntity.badRequest().body("{\"status\":400, \"error_msg\":\"The username is already taken\"}");
        } else {
            List<Object> args = new ArrayList<>();
            args.add(register.getUsername());
            args.add(register.getEmail());
            args.add(passwordEncoder.encode(register.getPassword()));
            sqLite.updateDb("insert into user (username, email, pw_hash) values (?, ?, ?)", args);
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/msgs")
    public ResponseEntity<?> messages(HttpServletRequest request, @RequestParam(value = "no", defaultValue = "100", required = false) int noMsgs,
                                      @RequestParam(value = "latest", required = false, defaultValue = "-1") int latest) throws SQLException {
        updateLatest(latest);

        ResponseEntity<Object> notFromSimResponse = notReqFromSimulator(request);
        if (notFromSimResponse != null) {
            return notFromSimResponse;
        }

        String query = "SELECT message.*, user.* FROM message, user WHERE message.flagged = 0 AND message.author_id = user.user_id ORDER BY message.pub_date DESC LIMIT ?";
        List<Map<String, Object>> messages = sqLite.queryDb(query, List.of(new Object[]{noMsgs}));
        return new ResponseEntity<>(messages, HttpStatus.OK);
    }

    @RequestMapping(value = "/msgs/{username}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Object> messagesPerUser(HttpServletRequest request, @PathVariable("username") String username,
                                                  @RequestParam(value = "no", defaultValue = "100", required = false) int noMsgs,
                                                  @RequestParam(value = "latest", required = false, defaultValue = "-1") int latest) throws SQLException {

        updateLatest(latest);

        ResponseEntity<Object> notFromSimResponse = notReqFromSimulator(request);
        if (notFromSimResponse != null) {
            return notFromSimResponse;
        }

        if ("POST".equals(request.getMethod())) {
            int userId = sqLite.getUserId(username);
            if (userId == 0) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            String query = "SELECT message.*, user.* FROM message, user "
                    + "WHERE message.flagged = 0 AND user.user_id = message.author_id AND user.user_id = ? "
                    + "ORDER BY message.pub_date DESC LIMIT ?";
            List<Object> args = new ArrayList<>();
            args.add(register.getUsername());
            args.add(register.getEmail());
            args.add(passwordEncoder.encode(register.getPassword()));
            List<Map<String, Object>> messages = sqLite.queryDb(query, userId, noMsgs);
            return new ResponseEntity<>(messages, HttpStatus.OK);

        } else if (RequestMethod.POST.toString().equals(request.getMethod())) {
            Map<String, Object> requestData = getRequestData(request);
            String query = "INSERT INTO message (author_id, text, pub_date, flagged) VALUES (?, ?, ?, 0)";
            getJdbcTemplate().update(query, getUserId(username), requestData.get("content"), Instant.now().getEpochSecond());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return null;
    }
}
