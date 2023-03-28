package dk.itu.minitwit.controller;

import dk.itu.minitwit.database.SQLite;
import dk.itu.minitwit.domain.AddMessage;
import dk.itu.minitwit.domain.Login;
import dk.itu.minitwit.domain.Register;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class MiniTwitController {

    @Autowired
    SQLite sqLite;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final int PER_PAGE = 30;

    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
    Logger logger = LoggerFactory.getLogger(MiniTwitController.class);


    @GetMapping("/")
    public String timeline(Model model, HttpServletRequest request) {
        logger.info("Request ID: %s -- Received %s request on path: '%s'"
                .formatted(model.getAttribute("requestID"), request.getMethod(), request.getRequestURI()));
        HttpSession session = request.getSession(false);
        boolean loggedIn = addUserToModel(model, session);
        if (!loggedIn) {
            logger.info("Request ID: %s -- User not logged in, redirecting to '/public'".formatted(model.getAttribute("requestID")));
            return "redirect:/public";
        }

        List<Object> args = new ArrayList<>();

        args.add(session.getAttribute("user_id"));
        args.add(session.getAttribute("user_id"));
        args.add(PER_PAGE);
        List<Map<String, Object>> messages = null;
        long before = System.currentTimeMillis();
        logger.info("Request ID: %s -- Querying database...".formatted(model.getAttribute("requestID")));
        try {
            messages = sqLite.queryDb(
                    "select message.*, " +
                            "user.* from message, " +
                            "user where message.flagged = 0 " +
                            "and message.author_id = user.user_id " +
                            "and (user.user_id = ? or user.user_id in (select whom_id from follower where who_id = ?))" +
                            "order by message.pub_date desc limit ?"
                    , args);
        } catch (SQLException e) {
            logger.error("Request ID: %s -- Encountered error while querying database: " + e.getMessage() +
                    "\n" + Arrays.toString(e.getStackTrace()).formatted(model.getAttribute("requestID")));
        }
        long after = System.currentTimeMillis();
        logger.info("Request ID: %s -- Queried database in %.2f seconds"
                .formatted(model.getAttribute("requestID"), getDuration(before, after)));
        addDatesAndGravatarURLs(messages);

        model.addAttribute("messages", messages);
        model.addAttribute("messagesSize", messages.size());
        String template = "timeline.html";
        logger.info("Request ID: %s -- Returning template: %s".formatted(model.getAttribute("requestID"), template));
        return template;
    }

    @RequestMapping(value = "/public", method = RequestMethod.GET)
    public Object publicTimeline(Model model, HttpServletRequest request) {
        logger.info("Request ID: %s -- Received %s request on path: '%s'"
                .formatted(model.getAttribute("requestID"), request.getMethod(), request.getRequestURI()));
        model.addAttribute("public", "true");
        HttpSession session = request.getSession(false);
        addUserToModel(model, session);

        List<Object> args = new ArrayList<>();
        args.add(PER_PAGE);
        List<Map<String, Object>> messages = null;
        long before = System.currentTimeMillis();
        logger.info("Request ID: %s -- Querying database...".formatted(model.getAttribute("requestID")));
        try {
            messages = sqLite.queryDb(
                    "select message.*, user.* from message, user " +
                            "where message.flagged = 0 and message.author_id = user.user_id " +
                            "order by message.pub_date desc limit ?", args);
        } catch (SQLException e) {
            logger.error(
                    "Request ID: %s -- Encountered error while querying database: " + e.getMessage() +
                            "\n" + Arrays.toString(e.getStackTrace()).formatted(model.getAttribute("requestID")));
        }
        long after = System.currentTimeMillis();
        logger.info("Request ID: %s -- Queried database in %.2f seconds"
                .formatted(model.getAttribute("requestID"), getDuration(before, after)));
        addDatesAndGravatarURLs(messages);

        model.addAttribute("messages", messages);
        model.addAttribute("messagesSize", messages.size());
        String template = "timeline.html";
        logger.info("Request ID: %s -- Returning template: %s".formatted(model.getAttribute("requestID"), template));
        return template;
    }

    @GetMapping("/favourites")
    public String favourites(HttpServletRequest request, Model model) {
        logger.info("Request ID: %s -- Received %s request on path: '%s'"
                .formatted(model.getAttribute("requestID"), request.getMethod(), request.getRequestURI()));
        HttpSession session = request.getSession(false);
        model.addAttribute("public", "false");
        boolean loggedIn = addUserToModel(model, session);

        if (!loggedIn) {
            logger.info("Request ID: %s -- User not logged in, redirecting to '/public'");
            return "redirect:/public";
        }

        List<Map<String, Object>> messages = null;
        long before = System.currentTimeMillis();
        logger.info("Request ID: %s -- Querying database...".formatted(model.getAttribute("requestID")));
        try {
            List<Object> args = new ArrayList<>();
            args.add(getUserID((String) session.getAttribute("user")));
            args.add(PER_PAGE);
            messages = sqLite.queryDb(
                    "select message.*, user.* " +
                            "from message inner join user " +
                            "on message.author_id = user.user_id " +
                            "inner join favourite on message.message_id = favourite.message_id " +
                            "where favourite.user_id = ? " +
                            "order by message.pub_date desc limit ?"
                    , args);
        } catch (SQLException e) {
            logger.error("Request ID: %s -- Encountered error while querying database: " + e.getMessage() +
                    "\n" + Arrays.toString(e.getStackTrace()).formatted(model.getAttribute("requestID")));
        }
        long after = System.currentTimeMillis();
        logger.info("Request ID: %s -- Queried database in %.2f seconds"
                .formatted(model.getAttribute("requestID"), getDuration(before, after)));
        addDatesAndGravatarURLs(messages);

        model.addAttribute("messages", messages);
        model.addAttribute("messagesSize", messages.size());
        String template = "favourites.html";
        logger.info("Request ID: %s -- Returning template: %s".formatted(model.getAttribute("requestID"), template));
        return template;
    }


    @GetMapping("/{username}")
    public String userTimeLine(@PathVariable("username") String username, HttpServletRequest request, Model model) {
        logger.info("Request ID: %s -- Received %s request on path: '%s'"
                .formatted(model.getAttribute("requestID"), request.getMethod(), request.getRequestURI()));
        HttpSession session = request.getSession(false);
        model.addAttribute("public", "false");
        model.addAttribute("username", username);
        boolean loggedIn = addUserToModel(model, session);

        List<Object> arg = new ArrayList<>();
        arg.add(username);
        List<Map<String, Object>> users = null;
        long before = System.currentTimeMillis();
        logger.info("Request ID: %s -- Querying database...".formatted(model.getAttribute("requestID")));
        try {
            users = sqLite.queryDb("select * from user where user.username = ?", arg);
        } catch (SQLException e) {
            logger.error("Request ID: %s -- Encountered error while querying database: " + e.getMessage() +
                    "\n" + Arrays.toString(e.getStackTrace()).formatted(model.getAttribute("requestID")));
        }
        long after = System.currentTimeMillis();
        logger.info("Request ID: %s -- Queried database in %.2f seconds"
                .formatted(model.getAttribute("requestID"), getDuration(before, after)));

        if (users.size() == 0){
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found"
            ); 
        }

        if (loggedIn) {
            int otherId = 0;
            before = System.currentTimeMillis();
            logger.info("Request ID: %s -- Querying database...".formatted(model.getAttribute("requestID")));
            try {

                otherId = getUserID(username);
            } catch (SQLException e) {
                logger.error("Request ID: %s -- Encountered error while querying database: " + e.getMessage() +
                        "\n" + Arrays.toString(e.getStackTrace()).formatted(model.getAttribute("requestID")));
            }
            after = System.currentTimeMillis();
            logger.info("Request ID: %s -- Queried database in %.2f seconds"
                    .formatted(model.getAttribute("requestID"), getDuration(before, after)));

            if ((int) session.getAttribute("user_id") == otherId) {
                model.addAttribute("self", "true");
                model.addAttribute("followed", "false");
            } else {
                List<Object> args = new ArrayList<>();
                args.add(session.getAttribute("user_id"));
                args.add(otherId);
                List<Map<String, Object>> followed = null;
                before = System.currentTimeMillis();
                logger.info("Request ID: %s -- Querying database...".formatted(model.getAttribute("requestID")));
                try {
                    followed = sqLite.queryDb("select * from follower where follower.who_id = ? and follower.whom_id = ?", args);
                } catch (SQLException e) {
                    logger.error("Request ID: %s -- Encountered error while querying database: " + e.getMessage() +
                            "\n" + Arrays.toString(e.getStackTrace()).formatted(model.getAttribute("requestID")));
                }
                after = System.currentTimeMillis();
                logger.info("Request ID: %s -- Queried database in %.2f seconds"
                        .formatted(model.getAttribute("requestID"), getDuration(before, after)));
                model.addAttribute("followed", followed.size() > 0 ? "true" : "false");
            }
        }
        List<Object> args = new ArrayList<>();
        args.add(username);
        args.add(PER_PAGE);
        List<Map<String, Object>> messages = null;
        before = System.currentTimeMillis();
        logger.info("Request ID: %s -- Querying database...".formatted(model.getAttribute("requestID")));
        try {
            messages = sqLite.queryDb(
                    "select message.*, " +
                            "user.* from message, user " +
                            "where user.username = ? " +
                            "and message.flagged = 0 " +
                            "and message.author_id = user.user_id " +
                            "order by message.pub_date desc limit ?"
                    , args);
        } catch (SQLException e) {
            logger.error("Request ID: %s -- Encountered error while querying database: " + e.getMessage() +
                    "\n" + Arrays.toString(e.getStackTrace()).formatted(model.getAttribute("requestID")));
        }
        after = System.currentTimeMillis();
        logger.info("Request ID: %s -- Queried database in %.2f seconds"
                .formatted(model.getAttribute("requestID"), getDuration(before, after)));
        addDatesAndGravatarURLs(messages);

        model.addAttribute("messages", messages);
        model.addAttribute("messagesSize", messages.size());

        String template = "timeline.html";
        logger.info("Request ID: %s -- Returning template: %s".formatted(model.getAttribute("requestID"), template));
        return template;
    }


    @GetMapping("/{username}/follow")
    public String followUser(@PathVariable("username") String username, HttpServletRequest request, Model model) {
        logger.info("Request ID: %s -- Received %s request on path: '%s'"
                .formatted(model.getAttribute("requestID"), request.getMethod(), request.getRequestURI()));
        HttpSession session = request.getSession(false);
        boolean loggedIn = addUserToModel(model, session);

        if (!loggedIn) {
            logger.info("Request ID: %s -- User not logged in, redirecting to '/login'");
            return "redirect:/login";
        }

        Integer whomId;
        try {
            whomId = getUserID(username);
        } catch (SQLException e) {
            logger.error("Request ID: %s -- Encountered error while querying database: " + e.getMessage() +
                    "\n" + Arrays.toString(e.getStackTrace()).formatted(model.getAttribute("requestID")));
            return "";
        }
        if (whomId == null) {
            logger.info("Request ID: %s -- User: %s not found - cannot follow - redirecting to public"
                    .formatted(model.getAttribute("requestID"), username));
            return "redirect:/public";
        }

        List<Object> args = new ArrayList<>();
        args.add(session.getAttribute("user_id"));
        args.add(whomId);
        long before = System.currentTimeMillis();
        logger.info("Request ID: %s -- Querying database...".formatted(model.getAttribute("requestID")));
        try {
            sqLite.updateDb("insert into follower (who_id, whom_id) values (?, ?)", args);
        } catch (SQLException e) {
            logger.error("Request ID: %s -- Encountered error while querying database: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()).formatted(model.getAttribute("requestID")));
        }
        long after = System.currentTimeMillis();
        logger.info("Request ID: %s -- Queried database in %.2f seconds"
                .formatted(model.getAttribute("requestID"), getDuration(before, after)));
        String template = "redirect:/" + username;
        logger.info("Request ID: %s -- Returning template: %s".formatted(model.getAttribute("requestID"), template));
        return template;
    }

    @GetMapping("/{username}/unfollow")
    public String unfollowUser(@PathVariable("username") String username, HttpServletRequest request, Model model) {
        logger.info("Request ID: %s -- Received %s request on path: '%s'"
                .formatted(model.getAttribute("requestID"), request.getMethod(), request.getRequestURI()));

        HttpSession session = request.getSession(false);
        boolean loggedIn = addUserToModel(model, session);

        if (!loggedIn) {
            logger.info("Request ID: %s -- User not logged in, redirecting to '/login'");
            return "redirect:/login";
        }
        Integer whomId;
        try {
            whomId = getUserID(username);
        } catch (SQLException e) {
            logger.error("Request ID: %s -- Encountered error while querying database: " + e.getMessage() +
                    "\n" + Arrays.toString(e.getStackTrace()).formatted(model.getAttribute("requestID")));
            return "";
        }
        if (whomId == null) {
            logger.info("Request ID: %s -- User: %s not found - cannot follow - redirecting to public"
                    .formatted(model.getAttribute("requestID"), username));
            return "redirect:/public";
        }


        List<Object> args = new ArrayList<>();
        args.add(session.getAttribute("user_id"));
        args.add(whomId);
        long before = System.currentTimeMillis();
        logger.info("Request ID: %s -- Querying database...".formatted(model.getAttribute("requestID")));
        try {
            sqLite.updateDb("delete from follower where who_id=? and whom_id=?", args);
        } catch (SQLException e) {
            logger.error("Request ID: %s -- Encountered error while querying database: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()).formatted(model.getAttribute("requestID")));
        }
        long after = System.currentTimeMillis();
        logger.info("Request ID: %s -- Queried database in %.2f seconds"
                .formatted(model.getAttribute("requestID"), getDuration(before, after)));
        String template = "redirect:/" + username;
        logger.info("Request ID: %s -- Returning template: %s".formatted(model.getAttribute("requestID"), template));
        return template;
    }

    @PostMapping("/add_message")
    public String addMessage(AddMessage text, HttpServletRequest request, Model model) {
        logger.info("Request ID: %s -- Received %s request on path: '%s'"
                .formatted(model.getAttribute("requestID"), request.getMethod(), request.getRequestURI()));
        HttpSession session = request.getSession(false);
        boolean loggedIn = addUserToModel(model, session);
        if (!loggedIn) {
            logger.info("Request ID: %s -- User not logged in, redirecting to '/login'");
            return "redirect:/login";
        }

        if (text.getText() != null && !text.getText().isEmpty()) {
            List<Object> args = new ArrayList<>();
            args.add(session.getAttribute("user_id"));
            args.add(text.getText());
            args.add(System.currentTimeMillis() / 1000);
            long before = System.currentTimeMillis();
            logger.info("Request ID: %s -- Querying database...".formatted(model.getAttribute("requestID")));
            try {
                sqLite.updateDb("insert into message (author_id, text, pub_date, flagged) values (?, ?, ?, 0)", args);
            } catch (SQLException e) {
                logger.error("Request ID: %s -- Encountered error while querying database: " + e.getMessage() +
                        "\n" + Arrays.toString(e.getStackTrace()).formatted(model.getAttribute("requestID")));
            }
            long after = System.currentTimeMillis();
            logger.info("Request ID: %s -- Queried database in %.2f seconds"
                    .formatted(model.getAttribute("requestID"), getDuration(before, after)));
        }

        String template = "redirect:/public";
        logger.info("Request ID: %s -- Returning template: %s".formatted(model.getAttribute("requestID"), template));
        return template;
    }

    @GetMapping("/addMessageToFavourites/{messageID}")
    public String addMessageToFavourites(@PathVariable("messageID") String messageID, HttpServletRequest request, Model model) {
        logger.info("Request ID: %s -- Received %s request on path: '%s'"
                .formatted(model.getAttribute("requestID"), request.getMethod(), request.getRequestURI()));

        HttpSession session = request.getSession(false);
        boolean loggedIn = addUserToModel(model, session);
        if (!loggedIn) {
            logger.info("Request ID: %s -- User not logged in, redirecting to '/login'");
            return "redirect:/login";
        }

        List<Object> args = new ArrayList<>();
        args.add(session.getAttribute("user_id"));
        args.add(messageID);
        long before = System.currentTimeMillis();
        logger.info("Request ID: %s -- Querying database...".formatted(model.getAttribute("requestID")));
        try {
            sqLite.updateDb("insert ignore into favourite (user_id, message_id) values (?, ?)", args);
        } catch (SQLException e) {
            logger.error("Request ID: %s -- Encountered error while querying database: " + e.getMessage() +
                    "\n" + Arrays.toString(e.getStackTrace()).formatted(model.getAttribute("requestID")));
        }
        long after = System.currentTimeMillis();
        logger.info("Request ID: %s -- Queried database in %.2f seconds"
                .formatted(model.getAttribute("requestID"), getDuration(before, after)));

        String template = "redirect:/public";
        logger.info("Request ID: %s -- Returning template: %s".formatted(model.getAttribute("requestID"), template));
        return template;
    }

    @GetMapping("/removeMessageToFavourites/{messageID}")
    public String removeMessageToFavourites(@PathVariable("messageID") String messageID, HttpServletRequest request, Model model) {
        logger.info("Request ID: %s -- Received %s request on path: '%s'"
                .formatted(model.getAttribute("requestID"), request.getMethod(), request.getRequestURI()));
        HttpSession session = request.getSession(false);

        boolean loggedIn = addUserToModel(model, session);
        if (!loggedIn) {
            logger.info("Request ID: %s -- User not logged in, redirecting to '/login'");
            return "redirect:/login";
        }

        List<Object> args = new ArrayList<>();
        args.add(session.getAttribute("user_id"));
        args.add(messageID);
        long before = System.currentTimeMillis();
        logger.info("Request ID: %s -- Querying database...".formatted(model.getAttribute("requestID")));
        try {
            sqLite.updateDb("delete from favourite where favourite.user_id = ? and favourite.message_id = ?", args);
        } catch (SQLException e) {
            logger.error("Request ID: %s -- Encountered error while querying database: " + e.getMessage() +
                    "\n" + Arrays.toString(e.getStackTrace()).formatted(model.getAttribute("requestID")));
        }
        long after = System.currentTimeMillis();
        logger.info("Request ID: %s -- Queried database in %.2f seconds"
                .formatted(model.getAttribute("requestID"), getDuration(before, after)));

        String template = "redirect:/favourites";
        logger.info("Request ID: %s -- Returning template: %s".formatted(model.getAttribute("requestID"), template));
        return template;
    }


    @RequestMapping(value = "/login", method = {RequestMethod.GET, RequestMethod.POST})
    public String login(@ModelAttribute Login login, Model model, HttpServletRequest request) {
        logger.info("Request ID: %s -- Received %s request on path: '%s'"
                .formatted(model.getAttribute("requestID"), request.getMethod(), request.getRequestURI()));


        if ("POST".equals(request.getMethod())) {
            //query db med user/pass fra login objekt
            List<Object> args = new ArrayList<>();
            args.add(login.getUsername());
            List<Map<String, Object>> s = null;
            long before = System.currentTimeMillis();
            logger.info("Request ID: %s -- Querying database...".formatted(model.getAttribute("requestID")));
            try {
                s = sqLite.queryDb("select * from user where username = ?", args);
            } catch (SQLException e) {
                logger.error("Request ID: %s -- Encountered error while querying database: " + e.getMessage() +
                        "\n" + Arrays.toString(e.getStackTrace()).formatted(model.getAttribute("requestID")));
            }
            long after = System.currentTimeMillis();
            logger.info("Request ID: %s -- Queried database in %.2f seconds"
                    .formatted(model.getAttribute("requestID"), getDuration(before, after)));
            if (s.isEmpty()) {
                logger.info("Request ID: %s -- User not logged in, invalid username");
                model.addAttribute("error", "Invalid username");
                return "login.html";
            } else if (!passwordEncoder.matches(login.getPassword(), (String) s.get(0).get("pw_hash"))) {
                logger.info("Request ID: %s -- User not logged in, invalid password ");
                model.addAttribute("error", "Invalid password");
                return "login.html";
            } else {
                // Session
                logger.info("Request ID: %s -- User logged in, redirecting to '/public'");
                request.getSession().setAttribute("user", login.getUsername());
                request.getSession().setAttribute("user_id", s.get(0).get("user_id"));

                String template = "redirect:/public";
                logger.info("Request ID: %s -- Returning template: %s".formatted(model.getAttribute("requestID"), template));
                return template;
            }
        }

        String template = "login.html";
        logger.info("Request ID: %s -- Returning template: %s".formatted(model.getAttribute("requestID"), template));
        return template;
    }


    @RequestMapping(value = "/register", method = {RequestMethod.GET, RequestMethod.POST})
    public String register(@ModelAttribute Register register, Model model, HttpServletRequest request) {
        logger.info("Request ID: %s -- Received %s request on path: '%s'"
                .formatted(model.getAttribute("requestID"), request.getMethod(), request.getRequestURI()));

        if ("POST".equals(request.getMethod())) {
            if ("".equals(register.getUsername())) {
                logger.info("Request ID: %s -- User not registered - no username in input");
                model.addAttribute("error", "You have to enter a username");
                return "register.html";
            } else if ("".equals(register.getEmail())) {
                logger.info("Request ID: %s -- User not registered - no email in input");
                model.addAttribute("error", "You have to enter a email");
                return "register.html";
            } else if ("".equals(register.getPassword())) {
                logger.info("Request ID: %s -- User not registered - no password in input");
                model.addAttribute("error", "You have to enter a password");
                return "register.html";
            } else if (!register.getPassword2().equals(register.getPassword())) {
                model.addAttribute("error", "Passwords don't match");
                return "register.html";
            } else {
                List<Object> args = new ArrayList<>();
                args.add(register.getUsername());
                args.add(register.getEmail());
                args.add(passwordEncoder.encode(register.getPassword()));

                long before = System.currentTimeMillis();
                logger.info("Request ID: %s -- Querying database...".formatted(model.getAttribute("requestID")));
                try {
                    sqLite.updateDb("insert into user (username, email, pw_hash) values (?, ?, ?)", args);
                } catch (SQLException e) {
                    logger.error("Request ID: %s -- Encountered error while querying database: " + e.getMessage() +
                            "\n" + Arrays.toString(e.getStackTrace()).formatted(model.getAttribute("requestID")));
                }
                long after = System.currentTimeMillis();
                logger.info("Request ID: %s -- Queried database in %.2f seconds"
                        .formatted(model.getAttribute("requestID"), getDuration(before, after)));

                String template = "redirect:/login";
                logger.info("Request ID: %s -- User registered - Returning template: %s".formatted(model.getAttribute("requestID"), template));
                return template;
            }
        }

        String template = "register.html";
        logger.info("Request ID: %s -- Returning template: %s".formatted(model.getAttribute("requestID"), template));
        return template;

    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, Model model) {
        logger.info("Request ID: %s -- Received %s request on path: '%s'"
                .formatted(model.getAttribute("requestID"), request.getMethod(), request.getRequestURI()));
        request.getSession().invalidate();
        logger.info("Request ID: %s -- Session invalidated".formatted(model.getAttribute("requestID")));
        String template = "redirect:/public";
        logger.info("Request ID: %s -- Returning template: %s".formatted(model.getAttribute("requestID"), template));
        return template;
    }

    @ModelAttribute("requestID")
    public String requestId() {
        return UUID.randomUUID().toString();
    }

    private int getUserID(String username) throws SQLException {
        List<Object> args = new ArrayList<>();
        args.add(username);
        List<Map<String, Object>> userIDs;
        userIDs = sqLite.queryDb("select user_id from user where username = ?", args);
        return ((int) userIDs.get(0).get("user_id"));
    }

    private static boolean addUserToModel(Model model, HttpSession session) {
        if (session != null) {
            model.addAttribute("user", session.getAttribute("user"));
            return true;
        } else {
            model.addAttribute("user", "false");
            return false;
        }
    }

    private void addDatesAndGravatarURLs(List<Map<String, Object>> messages) {
        messages.forEach(obj -> {
            String email = (String) obj.get("email");
            Long created = ((Number) obj.get("pub_date")).longValue();
            obj.put("gravatar_url", "https://www.gravatar.com/avatar/" + getMD5Hash(email.toLowerCase().strip()) + "?d=identicon&s=80");
            Date d = new Date((created) * 1000);
            obj.put("date_time", sdf.format(d));
        });
    }

    public String getMD5Hash(String email) {
        return DigestUtils.md5Hex(email.toLowerCase());
    }

    public double getDuration(long before, long after) {
        return ((double) after - (double) before) / 1000;
    }
}
