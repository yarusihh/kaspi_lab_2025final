package kz.kaspi.lab.fileuploaderreactive.application.service;

import kz.kaspi.lab.fileuploaderreactive.application.port.in.HelloWorldUseCase;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class HelloWorldService implements HelloWorldUseCase {

    @Override
    public Mono<String> hello() {
        return Mono.just("hello world");
    }
}
