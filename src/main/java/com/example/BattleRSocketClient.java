package com.example;

import java.time.Duration;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.util.DefaultPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

public class BattleRSocketClient {
	private static final Logger log = LoggerFactory.getLogger(BattleRSocketClient.class);

	public static void main(String[] args) throws Exception {

		int port = Optional.ofNullable(System.getenv("RSOCKET_PORT")) //
				.map(Integer::parseInt) //
				.orElse(8800);

		RSocket rSocket = RSocketFactory.connect() //
				.keepAliveTickPeriod(Duration.ofSeconds(1)) //
				.transport(WebsocketClientTransport.create(port)) //
				.start() //
				.block();

		System.out.print("Input <user name> <offensive power (0-200)>: ");
		Scanner scanner = new Scanner(System.in);
		String userName = scanner.next();
		int offensivePower = scanner.nextInt();

		Payload payload = DefaultPayload.create(userName + " " + offensivePower);
		Flux<Payload> stream = rSocket.requestStream(payload);

		CountDownLatch latch = new CountDownLatch(1);
		Disposable disposable = stream //
				.map(Payload::getDataUtf8) //
				.doOnCancel(() -> log.info("Cancel")) //
				.doOnComplete(() -> log.info("Complete")) //
				.doOnError(e -> log.error("Error", e)) //
				.doOnTerminate(latch::countDown) //
				.log("payload") //
				.subscribe();
		Runtime.getRuntime().addShutdownHook(new Thread(disposable::dispose));
		latch.await();
	}
}
