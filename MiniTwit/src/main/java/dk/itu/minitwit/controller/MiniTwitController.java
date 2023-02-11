package dk.itu.minitwit.controller;


import dk.itu.minitwit.database.SQLite;

import dk.itu.minitwit.domain.Login;
import dk.itu.minitwit.domain.Register;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.sql.SQLException;
import java.util.*;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@SpringBootApplication
@Controller
public class MiniTwitController {

    @Autowired
    SQLite sqLite;

    private final int PER_PAGE = 30;

    @GetMapping("/")
    public String timeline() {
        return "timeline.html";
    }

    @RequestMapping(value = "/public", method = RequestMethod.GET)
    public Object publicTimeline(Model model) {
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
        System.out.println(messages);
        model.addAttribute("messages", messages);
        model.addAttribute("messagesSize", messages.size());
        return "timeline.html";
    }


    @GetMapping("/{username}")
    public String userTimeLine(@PathVariable("username") String username) {

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
        System.out.println(login);
        System.out.println(request.getMethod());
        if ("POST".equals(request.getMethod())) {
            //query db med user/pass fra login objekt
            List<Object> args = new ArrayList<>();
            args.add(login.getUsername());
            List<Map<String, Object>> s = sqLite.queryDb("select * from user where username = ?", args);
            System.out.println(s);
            if (s.isEmpty()) {
                model.addAttribute("error", "Invalid username");
            } else if (checkPasswordHash(login.getPassword())) {
                model.addAttribute("error", "Invalid password");
            } else {
                // Session
                return "redirect:/public";
            }
        }
        return "login.html";
    }

    private boolean checkPasswordHash(String password) {
        return false;
    }


    @RequestMapping(value = "/register", method = {RequestMethod.GET, RequestMethod.POST})
    public String register(@ModelAttribute Register register, Model model) {
        System.out.println(register);

        model.addAttribute("error", "errormessage");
        return "register.html";
    }

    @PostMapping("/logout")
    public String logout() {
        return "public_timeline.html";
    }


}