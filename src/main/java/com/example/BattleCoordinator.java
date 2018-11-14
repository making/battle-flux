package com.example;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.rabbitmq.client.AMQP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.RpcClient;

public class BattleCoordinator {

	private static final Logger log = LoggerFactory.getLogger(BattleCoordinator.class);

	private final RpcClient rpcClient;

	public BattleCoordinator(RpcClient rpcClient) {
		this.rpcClient = rpcClient;
	}

	public Flux<String> join(User user) {
		AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder() //
				.headers(new HashMap<String, Object>() {

					{
						put("X-UserName", user.getUserName());
						put("X-OffensivePower", user.getOffensivePower());
						put("X-DefensivePower", user.getDefensivePower());
					}
				}) //
				.build();
		RpcClient.RpcRequest request = new RpcClient.RpcRequest(properties,
				UUID.randomUUID().toString().getBytes());
		AtomicBoolean winner = new AtomicBoolean(false);
		AtomicInteger n = new AtomicInteger();
		return rpcClient.rpc(Mono.just(request)) //
				.doOnSuccess(d -> {
					Object xWin = d.getProperties().getHeaders().get("X-Win");
					boolean win = Boolean.parseBoolean(Objects.toString(xWin, ""));
					winner.set(win);
					if (win) {
						n.incrementAndGet();
					}
				}) //
				.doOnCancel(() -> log.info("{} canceled!", user.getUserName())) //
				.doOnError(e -> log.error(user.getUserName() + " error!", e)) //
				.flatMapIterable(d -> Arrays.asList(new String(d.getBody()).split(","))) //
				.concatWith(Flux.defer(() -> {
					List<String> messages = new ArrayList<>();
					if (winner.get()) {
						messages.add("😉You win.");
						if (n.get() > 1) {
							messages.add("🎉" + n + " wins in a row!");
						}
					}
					else {
						messages.add("😇You lose.");
					}
					return Flux.fromIterable(messages);
				})).repeat(winner::get);
	}
}
