package ir.ac.ui.uitwitterclient.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendTweet {

    String message;
}
