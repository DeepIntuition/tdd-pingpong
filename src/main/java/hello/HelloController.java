package hello;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class HelloController {

    @RequestMapping("/")
    public String index() {
        String out = "<link rel='stylesheet' href='/webjars/bootstrap/3.3.0/css/bootstrap.min.css'></link>\n" +
"    <link rel='stylesheet' href='/css/site.css'></link>";
        out+="<p class=\"lead\">Hello bootstrap</p>";
        return out;
    }

}
