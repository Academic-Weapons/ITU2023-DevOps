package dk.itu.minitwit.controller;

import dk.itu.minitwit.database.SQLite;
import dk.itu.minitwit.domain.AddMessage;
import dk.itu.minitwit.domain.Login;
import dk.itu.minitwit.domain.Register;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Controller
public class MiniTwitController {

    @Autowired
    SQLite sqLite;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final int PER_PAGE = 30;

    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");


    @GetMapping("/")
    public String timeline() {
        return "redirect:/public";
    }

    @RequestMapping(value = "/public", method = RequestMethod.GET)
    public Object publicTimeline(Model model, HttpServletRequest request) {

        model.addAttribute("public", "true");
        HttpSession session = request.getSession(false);
        addUserToModel(model, session);

//        System.out.println("model things: " + model.getAttribute("user"));
        List<Object> args = new ArrayList<>();
        args.add(PER_PAGE);
        List<Map<String, Object>> messages;
        try {
            messages = sqLite.queryDb("select message.*, user.* from message, user where message.flagged = 0 and message.author_id = user.user_id order by message.pub_date desc limit ?", args);
            System.out.println(messages);
        } catch (SQLException e) {
            System.out.println("ERROR_" + e);
            return "Error";

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
//        System.out.println(messages);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        addDatesAndGravatarURLs(messages);

        model.addAttribute("messages", messages);
        model.addAttribute("messagesSize", messages.size());

        return "timeline.html";
    }


    @GetMapping("/{username}")
    public String userTimeLine(@PathVariable("username") String username, HttpServletRequest request, Model model) throws SQLException, ClassNotFoundException {
        HttpSession session = request.getSession(false);
        model.addAttribute("public", "false");
        model.addAttribute("username", username);
        Boolean loggedIn = addUserToModel(model, session);

        if (loggedIn) {
            //TODO bliver kaldt af favicon ??
//            System.out.println("username: " + username);
            if (username.equals("favicon.ico")) return "redirect:/public";


            int otherId = getUserID(username);
            if ((int) session.getAttribute("user_id") == otherId) {
                model.addAttribute("self", "true");
                model.addAttribute("followed", "false");
            } else {

                List<Object> args = new ArrayList<>();
                args.add(session.getAttribute("user_id"));
                args.add(otherId);

                List<Map<String, Object>> followed;
                followed = sqLite.queryDb("select * from follower where follower.who_id = ? and follower.whom_id = ?", args);
                model.addAttribute("followed", followed.size() > 0 ? "true" : "false");
            }
        }
        List<Object> args = new ArrayList<>();
        args.add(username);
        args.add(PER_PAGE);
        List<Map<String, Object>> messages;
        try {
            messages = sqLite.queryDb("select message.*, user.* from message, user where user.username = ? and message.flagged = 0 and message.author_id = user.user_id order by message.pub_date desc limit ?", args);
            System.out.println(messages);
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("ERROR_" + e);
            return "Error";
        }
//        System.out.println(messages);
        addDatesAndGravatarURLs(messages);

        model.addAttribute("messages", messages);
        model.addAttribute("messagesSize", messages.size());
        return "timeline.html";
    }

    private int getUserID(String username) throws SQLException, ClassNotFoundException {
        List<Object> args = new ArrayList<>();
        args.add(username);
        List<Map<String, Object>> userIDs;
        userIDs = sqLite.queryDb("select user_id from user where username = ?", args);
//        System.out.println("ids: " + userIDs.get(0).get("user_id"));
        return ((int) userIDs.get(0).get("user_id"));
    }

    private static boolean addUserToModel(Model model, HttpSession session) {
        if (session != null) {
            model.addAttribute("user", session.getAttribute("user"));
            return true;
        } else {
            model.addAttribute("user", "false");
            System.out.println("no sesh");
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

    @GetMapping("/{username}/follow")
    public String followUser(@PathVariable("username") String username, HttpServletRequest request, Model model) throws SQLException, ClassNotFoundException {
        HttpSession session = request.getSession(false);
//        model.addAttribute("public", "false");
        Boolean loggedIn = addUserToModel(model, session);

        if (!loggedIn) return "redirect:/login";
        Integer whomId = getUserID(username);
        //TODO lav noget 404?
        if (whomId == null) return "redirect:/public";

        List<Object> args = new ArrayList<>();
        args.add(session.getAttribute("user_id"));
        args.add(whomId);
        int result = sqLite.updateDb("insert into follower (who_id, whom_id) values (?, ?)", args);
        return "redirect:/" + username;
    }

    @GetMapping("/{username}/unfollow")
    public String unfollowUser(@PathVariable("username") String username, HttpServletRequest request, Model model) throws SQLException, ClassNotFoundException {
        HttpSession session = request.getSession(false);
//        model.addAttribute("public", "false");
        Boolean loggedIn = addUserToModel(model, session);

        if (!loggedIn) return "redirect:/login";
        Integer whomId = getUserID(username);
        //TODO lav noget 404?
        if (whomId == null) return "redirect:/public";

        List<Object> args = new ArrayList<>();
        args.add(session.getAttribute("user_id"));
        args.add(whomId);
        int result = sqLite.updateDb("delete from follower where who_id=? and whom_id=?", args);
        return "redirect:/" + username;

    }

    @PostMapping("/add_message")
    public String addMessage(AddMessage text, HttpServletRequest request, Model model) throws SQLException {
        HttpSession session = request.getSession(false);


        if (session != null && session.getAttribute("user_id") == null) {
            //abort 401
            return "401";
        }
        if (text.getText() != null && !text.getText().isEmpty()) {
            List<Object> args = new ArrayList<>();
            args.add(session.getAttribute("user_id"));
            args.add(text.getText());
            args.add(System.currentTimeMillis() / 1000);
            int result = sqLite.updateDb("insert into message (author_id, text, pub_date, flagged) values (?, ?, ?, 0)", args);

        }

        return "redirect:/public";
    }

    @RequestMapping(value = "/login", method = {RequestMethod.GET, RequestMethod.POST})
    public String login(@ModelAttribute Login login, Model model, HttpServletRequest request) throws SQLException, ClassNotFoundException {
//        System.out.println(login);
//        System.out.println(request.getMethod());
        if ("POST".equals(request.getMethod())) {
            //query db med user/pass fra login objekt
            List<Object> args = new ArrayList<>();
            args.add(login.getUsername());
            List<Map<String, Object>> s = sqLite.queryDb("select * from user where username = ?", args);
            System.out.println(s);
            if (s.isEmpty()) {
                model.addAttribute("error", "Invalid username");
                return "login.html";
            } else if (!passwordEncoder.matches(login.getPassword(), (String) s.get(0).get("pw_hash"))) {
                model.addAttribute("error", "Invalid password");
                return "login.html";
            } else {
                // Session
                request.getSession().setAttribute("user", login.getUsername());
                request.getSession().setAttribute("user_id", s.get(0).get("user_id"));
                return "redirect:/public";
            }
        }
        return "login.html";
    }


    @RequestMapping(value = "/register", method = {RequestMethod.GET, RequestMethod.POST})
    public String register(@ModelAttribute Register register, Model model, HttpServletRequest request) throws SQLException {
        System.out.println(register);

        if ("POST".equals(request.getMethod())) {
            System.out.println(register);
            if ("".equals(register.getUsername())) {
                model.addAttribute("error", "You have to enter a username");
                return "register.html";
            } else if ("".equals(register.getEmail())) {
                model.addAttribute("error", "You have to enter a email");
                return "register.html";
            } else if ("".equals(register.getPassword())) {
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
                int result = sqLite.updateDb("insert into user (username, email, pw_hash) values (?, ?, ?)", args);
                System.out.println(result);
                return ("redirect:/login");
            }
        }
//        model.addAttribute("error",     "");
        return "register.html";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        request.getSession().invalidate();

        return "redirect:/public";
    }

    public String getMD5Hash(String email) {
        return DigestUtils.md5Hex(email.toLowerCase());
    }
}