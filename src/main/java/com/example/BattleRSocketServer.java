package com.example;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Scanner;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.RpcClient;

public class BattleRSocketServer {

	public static void main(String[] args) {
		int port = Optional.ofNullable(System.getenv("RSOCKET_PORT")) //
				.map(Integer::parseInt) //
				.orElse(8800);

		RpcClient rpcClient = new RabbitInitializer().sender().rpcClient("rpc", "#");
		BattleCoordinator battleCoordinator = new BattleCoordinator(rpcClient);
		BattleRpcServer.start() //
				.subscribe();
		RSocketFactory.receive().acceptor(
				(connectionSetupPayload, rSocket) -> Mono.just(new AbstractRSocket() {

					@Override
					public Flux<Payload> requestStream(Payload payload) {
						ByteBuffer data = payload.getData();
						byte[] bytes = new byte[data.remaining()];
						data.get(bytes);
						Scanner scanner = new Scanner(new ByteArrayInputStream(bytes));
						User user = User.scan(scanner);
						Flux<String> battle = battleCoordinator.join(user);
						return battle //
								.map(DefaultPayload::create);
					}
				})) //
				.transport(TcpServerTransport.create("0.0.0.0", port)) //
				.start() //
				.log("rsocket:start") //
				.block() //
				.onClose() //
				.log("onClose") //
				.block();

	}
}
