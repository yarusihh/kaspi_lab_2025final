package kz.kaspi.lab.fileuploaderreactive.api.controller;

import kz.kaspi.lab.fileuploaderreactive.application.port.in.HelloWorldUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
public class HelloWorldController {

    private final HelloWorldUseCase helloWorldUseCase;

    public HelloWorldController(HelloWorldUseCase helloWorldUseCase) {
        this.helloWorldUseCase = helloWorldUseCase;
    }

    @GetMapping("/hellorworld")
    public Mono<String> helloWorld() {
        return helloWorldUseCase.hello();
    }
}
