package dk.itu.minitwit.controller;

import dk.itu.minitwit.database.SQLite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@RestController
public class MiniTwitController {

    @Autowired
    SQLite sqLite;

    private final int PER_PAGE = 30;

    @GetMapping("/")
    public String timeline() {
        return "default";
    }

    SpringTemplateEngine ste;

    @GetMapping("/public")
    public Object publicTimeline(Model model) {

        List<Object> args = new ArrayList<>();
        args.add(PER_PAGE);
        List<Map<String, Object>> messages;
        try {
            messages = sqLite.queryDb("select message.*, user.* from message, user where message.flagged = 0 and message.author_id = user.user_id order by message.pub_date desc limit ?", args);
        } catch (SQLException e) {
            return "Error";
        }
        model.addAttribute("messages",messages);
        return model;
    }

    @GetMapping("/{username}")
    public String userTimeLine(@PathVariable("username") String username) {
        return username + " username";
    }

    @GetMapping("/{username}/follow")
    public String followUser(@PathVariable("username") String username) {
        return username + " username follow";
    }

    @GetMapping("/{username}/unfollow")
    public String unfollowUser(@PathVariable("username") String username) {
        return username + " unfollow";
    }

    @PostMapping("/add_message")
    public String addMessage() {
        return "add_message";
    }

    @PostMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/logout")
    public String logout() {
        return "logout";
    }


}