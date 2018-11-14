package com.example;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.ConnectionFactory;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.ReactorRabbitMq;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

public class RabbitInitializer {

	private final ConnectionFactory connectionFactory;

	private Receiver receiver;

	private Sender sender;

	public RabbitInitializer() {
		this.connectionFactory = new ConnectionFactory();
		try {
			String uri = StreamSupport
				.stream(new ObjectMapper().readValue(
					Objects.toString(System.getenv("VCAP_SERVICES"), "{}"),
					JsonNode.class).spliterator(), false)
				.flatMap(n -> StreamSupport.stream(n.spliterator(), false))
				.filter(n -> StreamSupport.stream(n.get("tags").spliterator(), false)
					.anyMatch(x -> "rabbitmq".endsWith(x.asText())))
				.findAny().map(n -> n.get("credentials").get("uri").asText())
				.orElseGet(() -> Optional.ofNullable(System.getenv("RABBIT_URI"))
					.orElse("amqp://localhost"));
			this.connectionFactory.setUri(uri);
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		this.connectionFactory.useNio();
	}

	public Receiver receiver() {
		ReceiverOptions receiverOptions = new ReceiverOptions()
				.connectionFactory(this.connectionFactory)
				.connectionSubscriptionScheduler(Schedulers.elastic());
		return ReactorRabbitMq.createReceiver(receiverOptions);
	}

	public Sender sender() {
		SenderOptions senderOptions = new SenderOptions()
				.connectionFactory(this.connectionFactory)
				.resourceManagementScheduler(Schedulers.elastic());
		return ReactorRabbitMq.createSender(senderOptions);
	}

}
