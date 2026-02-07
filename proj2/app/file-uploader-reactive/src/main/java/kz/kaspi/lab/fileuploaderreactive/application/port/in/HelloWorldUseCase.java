package kz.kaspi.lab.fileuploaderreactive.application.port.in;

import reactor.core.publisher.Mono;

public interface HelloWorldUseCase {

    Mono<String> hello();
}
