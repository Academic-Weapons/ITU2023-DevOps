package dk.itu.minitwit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class MiniTwitController {

    @GetMapping("/")
    public String index() {
        return "";
    }

}