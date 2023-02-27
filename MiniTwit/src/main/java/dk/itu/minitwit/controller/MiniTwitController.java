package dk.itu.minitwit.controller;


import dk.itu.minitwit.database.SQLite;
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

    @GetMapping("/")
    public String timeline() {
        return "timeline.html";
    }

    @RequestMapping(value = "/public", method = RequestMethod.GET)
    public Object publicTimeline(Model model, HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        if (session != null){

            System.out.println(session.getAttribute("user").toString());
        } else {
            System.out.println("no sesh");
        }
        List<Object> args = new ArrayList<>();
        args.add(PER_PAGE);
        List<Map<String, Object>> messages;
        try {
            messages = sqLite.queryDb("select message.*, user.* from message, user where message.flagged = 0 and message.author_id = user.user_id order by message.pub_date desc limit ?", args);
            System.out.println(messages);
        } catch (SQLException e) {
            System.out.println("ERROR_" + e);
            return "Error";

        }
//        System.out.println(messages);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        messages.forEach(obj -> {
            String email = (String)obj.get("email");
            Long created = Long.valueOf((int) obj.get("pub_date"));
            obj.put("gravatar_url", "https://www.gravatar.com/avatar/" + getMD5Hash(email.toLowerCase().strip()) + "?d=identicon&s=80");
            Date d = new Date(created*1000);
            obj.put("date_time", sdf.format(d));
        });

        model.addAttribute("messages", messages);
        model.addAttribute("messagesSize", messages.size());

        return "timeline.html";
    }


    @GetMapping("/{username}")
    public String userTimeLine(@PathVariable("username") String username, HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session != null){

            System.out.println(session.getAttribute("user").toString());
        } else {
            System.out.println("no sesh");
        }

        List<Object> args = new ArrayList<>();
        args.add(username);
        args.add(PER_PAGE);
        List<Map<String, Object>> messages;
        try {
            messages = sqLite.queryDb("select message.*, user.* from message, user where user.username = ? and message.flagged = 0 and message.author_id = user.user_id order by message.pub_date desc limit ?", args);
            System.out.println(messages);
        } catch (SQLException e) {
            System.out.println("ERROR_" + e);
            return "Error";

        }
//        System.out.println(messages);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        messages.forEach(obj -> {
            String email = (String)obj.get("email");
            Long created = Long.valueOf((int) obj.get("pub_date"));
            obj.put("gravatar_url", "https://www.gravatar.com/avatar/" + getMD5Hash(email.toLowerCase().strip()) + "?d=identicon&s=80");
            Date d = new Date(created*1000);
            obj.put("date_time", sdf.format(d));
        });

        model.addAttribute("messages", messages);
        model.addAttribute("messagesSize", messages.size());
        return "timeline.html";
    }

    @GetMapping("/{username}/follow")
    public String followUser(@PathVariable("username") String username) {
        return "user_timeline.html";
    }

    @GetMapping("/{username}/unfollow")
    public String unfollowUser(@PathVariable("username") String username) {
        return "user_timeline.html";
    }

    @PostMapping("/add_message")
    public String addMessage() {
        return "timeline.html";
    }

    @RequestMapping(value = "/login", method = {RequestMethod.GET, RequestMethod.POST})
    public String login(@ModelAttribute Login login, Model model, HttpServletRequest request) throws SQLException {
//        System.out.println(login);
//        System.out.println(request.getMethod());
        if ("POST".equals(request.getMethod())) {
            //query db med user/pass fra login objekt
            List<Object> args = new ArrayList<>();
            args.add(login.getUsername());
            List<Map<String, Object>> s = sqLite.queryDb("select * from user where username = ?", args);

            if (s.isEmpty()) {
                model.addAttribute("error", "Invalid username");
                return "login.html";
            } else if (!passwordEncoder.matches(login.getPassword(), (String)s.get(0).get("pw_hash"))) {
                model.addAttribute("error", "Invalid password");
                return "login.html";
            } else {
                // Session
                request.getSession().setAttribute("user", login.getUsername());
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
            if("".equals(register.getUsername())){
                model.addAttribute("error", "You have to enter a username");
                return "register.html";
            } else if("".equals(register.getEmail())){
                model.addAttribute("error", "You have to enter a email");
                return "register.html";
            } else if("".equals(register.getPassword())){
                model.addAttribute("error", "You have to enter a password");
                return "register.html";
            } else if(!register.getPassword2().equals(register.getPassword())){
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

    @PostMapping("/logout")
    public String logout() {
        return "timeline.html";
    }

    public String getMD5Hash(String email){
        return DigestUtils.md5Hex(email.toLowerCase());
    }
}