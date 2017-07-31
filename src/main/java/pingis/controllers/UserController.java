package pingis.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class UserController {
    @Value("${production}")
    boolean production;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(Model model) {
        if(production) {
            return "redirect:/oauth2/authorize/code/tmc";
        } else {
            return "login";
        }
    }

    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public String user(Model model) {
        return "user";
    }

    @RequestMapping(value = "/admin", method = RequestMethod.GET)
    public String admin(Model model) {
        return "admin";
    }
}
