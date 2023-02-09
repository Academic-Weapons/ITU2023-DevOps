package dk.itu.minitwit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class MiniTwitController {

    public static void main(String[] args) {
        SpringApplication.run(MiniTwitController.class, args);
    }

    @GetMapping("/")
    public String timeline() {
        return "default";
    }

    @GetMapping("/public")
    public String publicTimeline() {
        return "public";
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