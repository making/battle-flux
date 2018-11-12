package com.example;

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
		connectionFactory.useNio();
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
