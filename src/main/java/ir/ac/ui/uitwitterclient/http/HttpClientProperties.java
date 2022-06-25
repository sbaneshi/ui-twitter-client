package ir.ac.ui.uitwitterclient.http;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ui.twitter.client")
public class HttpClientProperties {

    private String endpointUrl = "localhost";
    private int endpointPort = 8888;

    private int maxConnections = 300;
    private int maxUnmarshallerConcurrency = 1000;

    private int signUpCount = 1000;
    private int followCount = 1000;
    private int tweetCount = 1000;
}
