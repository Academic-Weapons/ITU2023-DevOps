package dk.itu.minitwit.controller;

import dk.itu.minitwit.database.SQLite;
import dk.itu.minitwit.domain.Register;
import dk.itu.minitwit.domain.SimData;
import dk.itu.minitwit.domain.SimMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class SimulatorController {

    @Autowired
    SQLite sqLite;

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

    @RequestMapping(
            value = "sim/latest",
            method = RequestMethod.GET,
            produces = "application/json")
    public ResponseEntity<Object> getLatest() {
        return ResponseEntity.ok("{\"latest\":" + LATEST + "}");
    }

    @RequestMapping(
            value = "sim/register",
            method = RequestMethod.POST,
            produces = "application/json")
    public ResponseEntity<Object> register(@RequestBody Register register,
                                           @RequestParam(value = "latest", required = false, defaultValue = "-1") int latest) throws SQLException, ClassNotFoundException {
        updateLatest(latest);
        if (register.getUsername() == null) {
            return ResponseEntity.badRequest().body("{\"status\":400, \"error_msg\":\"You have to enter a username\"}");
        } else if (register.getEmail() == null || !register.getEmail().contains("@")) {
            return ResponseEntity.badRequest().body("{\"status\":400, \"error_msg\":\"You have to enter a valid email address\"}");
        } else if (register.getPwd() == null) {
            return ResponseEntity.badRequest().body("{\"status\":400, \"error_msg\":\"You have to enter a password\"}");
        } else if (sqLite.getUserId(register.getUsername()) != -1) {
            return ResponseEntity.badRequest().body("{\"status\":400, \"error_msg\":\"The username is already taken\"}");
        } else {
            try {
                sqLite.register(register);
            } catch (SQLException e) {
                return internalErrorResponse(e);
            }
            return ResponseEntity.noContent().build();
        }
    }

    @RequestMapping(
            value = "sim/msgs",
            method = RequestMethod.GET,
            produces = "application/json")
    public ResponseEntity<Object> messages(HttpServletRequest request, @RequestParam(value = "no", defaultValue = "100", required = false) int noMsgs,
                                           @RequestParam(value = "latest", required = false, defaultValue = "-1") int latest) throws SQLException {
        updateLatest(latest);

        ResponseEntity<Object> notFromSimResponse = notReqFromSimulator(request);
        if (notFromSimResponse != null) {
            return notFromSimResponse;
        }

        try {
            String query = "SELECT message.*, user.* FROM message, user WHERE message.flagged = 0 AND message.author_id = user.user_id ORDER BY message.pub_date DESC LIMIT ?";
            List<SimMessage> messages = sqLite.queryDb(query, List.of(new Object[]{noMsgs}))
                    .stream().map(msg -> {
                        return new SimMessage((String) msg.get("text"), (int) msg.get("pub_date"), (String) msg.get("username"));
                    }).collect(Collectors.toList());
            return ResponseEntity.ok(messages);
        } catch (SQLException e) {
            return internalErrorResponse(e);
        }
    }

    @RequestMapping(value = "sim/msgs/{username}",
            method = RequestMethod.GET,
            produces = "application/json")
    public ResponseEntity<Object> messagesPerUserGet(HttpServletRequest request,
                                                     @PathVariable("username") String username,
                                                     @RequestParam(value = "no", defaultValue = "100", required = false) int noMsgs,
                                                     @RequestParam(value = "latest", required = false, defaultValue = "-1") int latest) throws SQLException, ClassNotFoundException {
        updateLatest(latest);
        ResponseEntity<Object> notFromSimResponse = notReqFromSimulator(request);
        if (notFromSimResponse != null) {
            return notFromSimResponse;
        }
        int userId = sqLite.getUserId(username);
        if (userId == -1) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        String query = "SELECT message.*, user.* FROM message, user "
                + "WHERE message.flagged = 0 AND user.user_id = message.author_id AND user.user_id = ? "
                + "ORDER BY message.pub_date DESC LIMIT ?";
        List<Object> args = new ArrayList<>();
        args.add(userId);
        args.add(noMsgs);
        List<SimMessage> messages = sqLite.queryDb(query, args).stream().map(msg -> {
            return new SimMessage((String) msg.get("text"), (int) msg.get("pub_date"), (String) msg.get("username"));
        }).collect(Collectors.toList());
        return ResponseEntity.ok(messages);
    }

    @RequestMapping(value = "sim/msgs/{username}",
            method = RequestMethod.POST,
            produces = "application/json")
    public ResponseEntity<Object> messagesPerUserPost(HttpServletRequest request,
                                                      @RequestBody SimData data,
                                                      @PathVariable("username") String username,
                                                      @RequestParam(value = "no", defaultValue = "100", required = false) int noMsgs,
                                                      @RequestParam(value = "latest", required = false, defaultValue = "-1") int latest) throws SQLException, ClassNotFoundException {
        updateLatest(latest);
        ResponseEntity<Object> notFromSimResponse = notReqFromSimulator(request);
        if (notFromSimResponse != null) {
            return notFromSimResponse;
        }
        int userId = sqLite.getUserId(username);

        try {
            sqLite.insertMessage(userId, data);
        } catch (SQLException | ClassNotFoundException e) {
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "sim/fllws/{username}",
            method = RequestMethod.POST,
            produces = "application/json")
    public ResponseEntity<Object> followPost(HttpServletRequest request,
                                             @RequestBody SimData data,
                                             @PathVariable String username,
                                             @RequestParam(value = "no", defaultValue = "100", required = false) int noMsgs,
                                             @RequestParam(value = "latest", required = false, defaultValue = "-1") int latest) {
        updateLatest(latest);
        ResponseEntity<Object> notFromSimResponse = notReqFromSimulator(request);
        if (notFromSimResponse != null) {
            return notFromSimResponse;
        }

        int userId;
        try {
            userId = sqLite.getUserId(username);
            if (userId == 0) {
                return ResponseEntity.notFound().build();
            }
        } catch (SQLException | ClassNotFoundException e) {
            return ResponseEntity.internalServerError().body(e);
        }


        if (request.getMethod().equals("POST") && data.getFollow() != null) {
            return follow(data, userId);
        } else if (request.getMethod().equals("POST") && data.getUnfollow() != null) {
            return unfollow(data, userId);
        }
        return ResponseEntity.badRequest().build();
    }

    @RequestMapping(value = "sim/fllws/{username}",
            method = RequestMethod.GET,
            produces = "application/json")
    public ResponseEntity<Object> followGet(HttpServletRequest request,
                                            @PathVariable String username,
                                            @RequestParam(value = "no", defaultValue = "100", required = false) int noMsgs,
                                            @RequestParam(value = "latest", required = false, defaultValue = "-1") int latest) {
        updateLatest(latest);
        ResponseEntity<Object> notFromSimResponse = notReqFromSimulator(request);
        if (notFromSimResponse != null) {
            return notFromSimResponse;
        }

        int userId;
        try {
            userId = sqLite.getUserId(username);
            if (userId == 0) {
                return ResponseEntity.notFound().build();
            }
        } catch (SQLException | ClassNotFoundException e) {
            return ResponseEntity.internalServerError().body(e);
        }
        return follows(noMsgs, userId);
    }

    private static ResponseEntity<Object> internalErrorResponse(SQLException e) {
        return ResponseEntity.internalServerError().body(e);
    }

    private ResponseEntity<Object> follow(SimData data, int userId) {
        try {
            int followsUserId = sqLite.getUserId(data.getFollow());
            if (followsUserId == 0) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            sqLite.follow(userId, followsUserId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (SQLException | ClassNotFoundException e) {
            return ResponseEntity.internalServerError().body(e);
        }
    }

    private ResponseEntity<Object> unfollow(SimData data, int userId) {
        try {
            int unfollowsUserId = sqLite.getUserId(data.getUnfollow());
            if (unfollowsUserId == 0) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            sqLite.unfollow(userId, unfollowsUserId);
            return ResponseEntity.noContent().build();
        } catch (SQLException | ClassNotFoundException e) {
            return ResponseEntity.internalServerError().body(e);
        }
    }

    private ResponseEntity<Object> follows(int noMsgs, int userId) {
        try {
            String query = "SELECT user.username FROM user INNER JOIN follower ON follower.whom_id=user.user_id WHERE follower.who_id=? LIMIT ?";
            List<Object> args = new ArrayList<>();
            args.add(userId);
            args.add(noMsgs);
            List<Map<String, Object>> followers = sqLite.queryDb(query, args);
            List<String> followerNames = followers.stream().map(f -> f.get("username").toString()).collect(Collectors.toList());
            Map<String, Object> followersResponse = new HashMap<>();
            followersResponse.put("follows", followerNames);

            if(followerNames.isEmpty()){
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.ok().body(followersResponse);
            }
        } catch (SQLException  e) {

            return ResponseEntity.internalServerError().body(e);
        }
    }

}
