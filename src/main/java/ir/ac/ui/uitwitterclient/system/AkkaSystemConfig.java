package ir.ac.ui.uitwitterclient.system;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import ir.ac.ui.uitwitterclient.model.BaseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AkkaSystemConfig {

    private static final String ACTOR_SYSTEM_NAME = "ui-twitter-client";

    @Bean
    public ActorSystem<BaseMessage> actorSystem() {
        log.info("Setting up ActorSystem[{}]...", ACTOR_SYSTEM_NAME);
        return ActorSystem.create(Behaviors.setup(SystemGuardianActor::new), ACTOR_SYSTEM_NAME);
    }
}
