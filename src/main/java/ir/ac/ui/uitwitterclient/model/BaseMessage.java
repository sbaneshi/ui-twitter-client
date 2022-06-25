package ir.ac.ui.uitwitterclient.model;

import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Value
@NonFinal
@SuperBuilder(toBuilder = true)
public abstract class BaseMessage implements Serializable {

    Object message;

    @SuppressWarnings("unchecked")
    public <T> T getMessage() {
        return (T) message;
    }
}
