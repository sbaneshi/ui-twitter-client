package ir.ac.ui.uitwitterclient.system;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import ir.ac.ui.uitwitterclient.model.BaseMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SystemGuardianActor extends AbstractBehavior<BaseMessage> {

    public SystemGuardianActor(ActorContext<BaseMessage> context) {
        super(context);
    }

    @Override
    public Receive<BaseMessage> createReceive() {
        return newReceiveBuilder()
                .onAnyMessage(this::logReceivedMessage)
                .build();
    }

    private Behavior<BaseMessage> logReceivedMessage(BaseMessage x) {
        log.info("Received guardian message=[{}]", x);
        return Behaviors.same();
    }
}
