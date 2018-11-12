package com.example;

import java.time.Duration;
import java.util.Optional;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.netty.http.server.HttpServer;

import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

public class BattleRRocketServerHttpProxy {

	private static Logger log = LoggerFactory.getLogger(BattleRRocketServerHttpProxy.class);

	public static void main(String[] args) throws Exception {
		long begin = System.currentTimeMillis();
		int port = Optional.ofNullable(System.getenv("PORT")) //
				.map(Integer::parseInt) //
				.orElse(8080);
		HttpServer httpServer = HttpServer.create().host("0.0.0.0").port(port);
		httpServer.route(routes -> {
			HttpHandler httpHandler = RouterFunctions.toHttpHandler(
					BattleRRocketServerHttpProxy.routes(), HandlerStrategies.builder().build());
			routes.route(x -> true, new ReactorHttpHandlerAdapter(httpHandler));
		}).bindUntilJavaShutdown(Duration.ofSeconds(3), disposableServer -> {
			long elapsed = System.currentTimeMillis() - begin;
			LoggerFactory.getLogger(BattleRRocketServerHttpProxy.class).info("Started in {} seconds",
					elapsed / 1000.0);
		});
	}

	static RouterFunction<ServerResponse> routes() {
		int port = Optional.ofNullable(System.getenv("RSOCKET_PORT")) //
				.map(Integer::parseInt) //
				.orElse(8800);

		RSocket rSocket = RSocketFactory.connect()
				.transport(TcpClientTransport.create(port)).start().block();
		return route() //
				.POST("/join", req -> {
					Flux<String> reply = req.bodyToMono(User.class) //
							.flatMapMany(user -> {
								Payload payload = DefaultPayload.create(user.getUserName()
										+ " " + user.getOffensivePower());
								return rSocket.requestStream(payload);
							}).map(Payload::getDataUtf8);
					return ok() //
							.contentType(TEXT_EVENT_STREAM) //
							.body(reply, String.class);
				}).build();
	}
}
