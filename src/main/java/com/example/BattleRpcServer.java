package com.example;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Delivery;
import io.netty.util.internal.ConcurrentSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.OutboundMessageResult;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ResourcesSpecification;
import reactor.rabbitmq.Sender;
import reactor.util.function.Tuples;

import org.springframework.util.StringUtils;

import static reactor.rabbitmq.ExchangeSpecification.exchange;
import static reactor.rabbitmq.ResourcesSpecification.queue;

public class BattleRpcServer {

	private static Logger log = LoggerFactory.getLogger(BattleRpcServer.class);

	static OutboundMessage replyMessage(Delivery delivery, String battleLog,
			boolean win) {
		AMQP.BasicProperties props = delivery.getProperties();
		String replyTo = props.getReplyTo();
		AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
				.correlationId(props.getCorrelationId())
				.headers(Collections.singletonMap("X-Win", win)).build();
		return new OutboundMessage("", replyTo, replyProps, battleLog.getBytes());
	}

	static Flux<OutboundMessageResult> start() {
		RabbitInitializer rabbit = new RabbitInitializer();
		Sender sender = rabbit.sender();
		Receiver receiver = rabbit.receiver();

		Mono<AMQP.Queue.BindOk> rpcBind = sender.declare(exchange("rpc").type("topic"))
				.then(sender.declare(queue("rpc.queue").durable(true)))
				.then(sender
						.bind(ResourcesSpecification.binding("rpc", "#", "rpc.queue")))
				.doOnSuccess(x -> log.info("Exchange and queue declared and bound."))
				.doOnError(e -> {
					log.error("Connection failed.", e);
					System.exit(1);
				});

		ConcurrentSet<String> inFlight = new ConcurrentSet<>();
		Flux<OutboundMessageResult> outbound = receiver.consumeAutoAck("rpc.queue") //
				.filter(x -> {
					String username = x.getProperties().getHeaders().get("X-UserName")
							.toString();
					return !inFlight.contains(username);
				}) //
				.doOnNext(x -> {
					String username = x.getProperties().getHeaders().get("X-UserName")
							.toString();
					inFlight.add(username);
				}) //
				.buffer(2) //
				.flatMap(deliveries -> {
					if (deliveries.size() < 2) {
						return Mono.error(
								new IllegalStateException("Can not find the pair."));
					}
					return Mono.just(Tuples.of(deliveries.get(0), deliveries.get(1)));
				}) //
				.flatMap(deliveries -> {
					Delivery d1 = deliveries.getT1();
					Delivery d2 = deliveries.getT2();
					Map<String, Object> headers1 = d1.getProperties().getHeaders();
					Map<String, Object> headers2 = d2.getProperties().getHeaders();

					User user1 = new User(headers1.get("X-UserName").toString(),
							(int) headers1.get("X-OffensivePower"),
							(int) headers1.get("X-DefensivePower"));
					User user2 = new User(headers2.get("X-UserName").toString(),
							(int) headers2.get("X-OffensivePower"),
							(int) headers2.get("X-DefensivePower"));
					Mono<String> battleLogs = new BattleFlux(user1, user2).start()
							.collectList()
							.map(StringUtils::collectionToCommaDelimitedString);
					Flux<OutboundMessage> message = battleLogs
							.flatMapIterable(battleLog -> Arrays.asList(
									replyMessage(d1, battleLog, user1.winner()),
									replyMessage(d2, battleLog, user2.winner())));
					return sender.sendWithPublishConfirms(message) //
							.doOnTerminate(() -> {
								inFlight.remove(user1.getUserName());
								inFlight.remove(user2.getUserName());
							});
				}) //
				.doOnCancel(() -> log.info("canceled!"))
				.doOnError(e -> log.error("error!", e));

		return rpcBind.thenMany(outbound);
	}
}
