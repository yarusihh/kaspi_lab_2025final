package kz.kaspi.lab.fileuploader.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class StubController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @ResponseBody
    @GetMapping(value = "/api/v1/", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> apiV1Stub() {
        return Map.of(
                "status", "ok",
                "message", "API v1 stub"
        );
    }
}
