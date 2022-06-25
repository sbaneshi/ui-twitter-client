package ir.ac.ui.uitwitterclient.http;

import akka.Done;
import akka.actor.typed.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.HostConnectionPool;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.*;
import akka.http.javadsl.settings.ConnectionPoolSettings;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.japi.Pair;
import akka.japi.tuple.Tuple3;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ir.ac.ui.uitwitterclient.api.*;
import ir.ac.ui.uitwitterclient.model.BaseMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import scala.concurrent.duration.Duration;
import scala.util.Try;

import javax.annotation.PostConstruct;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(HttpClientProperties.class)
public class HttpClientConfig {

    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return mapper;
    }

    private static final Random random = new SecureRandom();
    private static final long userIdSeed = Math.abs(random.nextLong());
    private static final ObjectMapper objectMapper = createObjectMapper();

    private final ActorSystem<BaseMessage> actorSystem;
    private final HttpClientProperties props;

    private static final List<Long> userIds = Collections.synchronizedList(new ArrayList<>());

    @PostConstruct
    public void setup() {
        Http http = Http.get(actorSystem);

        final long beforeStart = System.currentTimeMillis();
        Flow<Pair<HttpRequest, Long>, Pair<Try<HttpResponse>, Long>, HostConnectionPool> connectionPool = http.cachedHostConnectionPool(
                ConnectHttp.toHost(props.getEndpointUrl(), props.getEndpointPort()),
                ConnectionPoolSettings.create(actorSystem.classicSystem())
                        .withMaxConnections(props.getMaxConnections())
                        .withResponseEntitySubscriptionTimeout(Duration.fromNanos(60000000000L)),
                actorSystem.classicSystem().log());

        performStage(connectionPool, "SIGNUP", props.getSignUpCount(),
                SignupResponse.class, this::signupRequest, x -> userIds.add(x.getUserId()))
                .thenCompose(done -> performStage(connectionPool, "FOLLOW", props.getFollowCount(),
                        DefaultResponse.class, this::followRequest, x -> {
                        }))
                .thenCompose(done -> performStage(connectionPool, "SEND_TWEET", props.getTweetCount(),
                        SendTweetResponse.class, this::sendTweetRequest, x -> {
                        }))
                .thenCompose(done -> performStage(connectionPool, "GET_HISTORY", userIds.size(),
                        GetHistoryResponse.class, this::getHistoryRequest, x -> {
                        }))
                .thenAccept(x -> {
                    long now = System.currentTimeMillis();
                    log.info("Finished scenario after [{}] milliseconds; start=[{}], now=[{}]",
                            now - beforeStart, beforeStart, now);
                });
    }

    private <T> CompletionStage<Done> performStage(Flow<Pair<HttpRequest, Long>, Pair<Try<HttpResponse>, Long>, HostConnectionPool> connectionPool, String stageName,
                                                   int requestCount, Class<T> responseClass, IntFunction<Pair<HttpRequest, Long>> requestFunction, Consumer<T> responseConsumer) {
        final long beforeStage = System.currentTimeMillis();
        return Source.fromJavaStream(() -> IntStream.range(0, requestCount).mapToObj(requestFunction))
                .via(connectionPool)
                .map(x -> {
                    HttpResponse response = x.first().getOrElse(() ->
                            HttpResponse.create().withStatus(500));
                    return Pair.create(response, x.second());
                })
                .mapAsyncUnordered(props.getMaxUnmarshallerConcurrency(), x -> Unmarshaller.entityToString()
                        .unmarshal(x.first().entity(), actorSystem)
                        .thenApply(s -> Tuple3.create(
                                readResponse(s, responseClass), 200, x.second())
                        )
                        .exceptionally(e -> Tuple3.create(
                                null, x.first().status().intValue(), x.second())
                        )
                )
                .map(x -> {
                    final long startedAt = x.t3() == null ? System.currentTimeMillis() : x.t3();
                    final int statusCode = x.t2();
                    log.info("Stage[{}]; Resp=[{}]; duration=[{}]", stageName,
                            statusCode, System.currentTimeMillis() - startedAt);

                    if (x.t1() != null)
                        responseConsumer.accept(x.t1());
                    return x;
                })
                .runWith(Sink.ignore(), actorSystem)
                .thenApply(x -> {
                    log.info("Stage[{}] finished in [{}]ms", stageName, System.currentTimeMillis() - beforeStage);
                    return x;
                });
    }

    @SneakyThrows
    private <T> T readResponse(String s, Class<T> clazz) {
        return objectMapper.readValue(s, clazz);
    }

    @SneakyThrows
    private Pair<HttpRequest, Long> signupRequest(int i) {
        return Pair.create(HttpRequest.POST("/api")
                .addHeader(HttpHeader.parse("Request-Type", "1"))
                .withEntity(HttpEntities.create(ContentTypes.APPLICATION_JSON, objectMapper.writeValueAsBytes(Signup.builder()
                        .name("NAME: " + i)
                        .phone("98" + userIdSeed + i)
                        .build()))), System.currentTimeMillis());
    }

    @SneakyThrows
    private Pair<HttpRequest, Long> followRequest(int i) {
        return Pair.create(HttpRequest.POST("/api")
                .addHeader(HttpHeader.parse("Request-Type", "2"))
                .addHeader(HttpHeader.parse("User-Id", String.valueOf(randomUserId())))
                .withEntity(HttpEntities.create(ContentTypes.APPLICATION_JSON, objectMapper.writeValueAsBytes(Follow.builder()
                        .targetUserId(randomUserId())
                        .build()))), System.currentTimeMillis());
    }

    @SneakyThrows
    private Pair<HttpRequest, Long> sendTweetRequest(int i) {
        return Pair.create(HttpRequest.POST("/api")
                .addHeader(HttpHeader.parse("Request-Type", "3"))
                .addHeader(HttpHeader.parse("User-Id", String.valueOf(randomUserId())))
                .withEntity(HttpEntities.create(ContentTypes.APPLICATION_JSON, objectMapper.writeValueAsBytes(SendTweet.builder()
                        .message("OI: " + System.currentTimeMillis())
                        .build()))), System.currentTimeMillis());
    }

    @SneakyThrows
    private Pair<HttpRequest, Long> getHistoryRequest(int i) {
        return Pair.create(HttpRequest.POST("/api")
                .addHeader(HttpHeader.parse("Request-Type", "4"))
                .addHeader(HttpHeader.parse("User-Id", String.valueOf(userIds.get(i))))
                .withEntity(HttpEntities.create(ContentTypes.APPLICATION_JSON, objectMapper.writeValueAsBytes(GetHistory.builder()
                        .build()))), System.currentTimeMillis());
    }

    private long randomUserId() {
        return userIds.get(random.nextInt(userIds.size()));
    }
}
