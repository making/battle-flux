package com.example;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
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
				//timeout(Duration.ofSeconds(1)) //
				.doOnSuccess(d -> {
					Object xWin = d.getProperties().getHeaders().get("X-Win");
					boolean win = Boolean.parseBoolean(Objects.toString(xWin, ""));
					winner.set(win);
					if (win) {
						n.incrementAndGet();
					}
				}) //
				.doOnNext(s -> {
					if (winner.get() && n.get() > 1) {
						log.info("{} {} wins in a row!", user.getUserName(), n);
					}
				}) //
				.doOnCancel(() -> log.info("{} canceled!", user.getUserName()))
				.flatMapIterable(d -> Arrays.asList(new String(d.getBody()).split(","))) //
				.repeat(winner::get) //
				.retry(e -> {
					//log.info("{} retry!", user.getUserName());
					return (e instanceof TimeoutException);
				}) //
				.doOnError(e -> {
					if (!(e instanceof TimeoutException)) {
						log.error(user.getUserName() + " error!", e);
					}
				});
	}
}
