package ir.ac.ui.uitwitterclient.model;

import akka.actor.typed.ActorRef;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;
import org.springframework.lang.Nullable;

@Value
@NonFinal
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class BaseRequest<R> extends BaseMessage {

    @Nullable
    ActorRef<R> replyTo;

    public void replyIfPresent(R reply) {
        if (replyTo != null) replyTo.tell(reply);
    }
}
