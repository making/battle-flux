package com.example;

import java.util.Random;
import java.util.concurrent.atomic.LongAdder;

import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

public class BattleFlux {
	private final User user1;
	private final User user2;

	public BattleFlux(User user1, User user2) {
		this.user1 = user1;
		this.user2 = user2;
	}

	public void init() {
		this.user1.init();
		this.user2.init();
	}

	public Flux<String> start() {
		return Flux.create(sink -> {
			final int maxLoop = 128;
			LongAdder currentLoop = new LongAdder();
			Random random = new Random(System.nanoTime());
			sink.onRequest(n -> {
				for (int i = 0; i < Math.min(n / 5 + 1, 500); i++) {
					currentLoop.increment();
					sink.next(String.format("Turn %d", currentLoop.intValue()));
					Tuple2<Integer, Boolean> tpl1 = this.user1.attack(this.user2, random);
					Tuple2<Integer, Boolean> tpl2 = this.user2.attack(this.user1, random);

					sink.next(String.format("%s%s did %d damage to %s",
							tpl1.getT2() ? "Critical Hit! " : "",
							this.user1.getUserName(), tpl1.getT1(),
							this.user2.getUserName()));
					sink.next(String.format("%s%s did %d damage to %s",
							tpl2.getT2() ? "Critical Hit! " : "",
							this.user2.getUserName(), tpl2.getT1(),
							this.user1.getUserName()));
					sink.next(String.format("%s's remaining power is %d.",
							this.user1.getUserName(), this.user1.currentPower()));
					sink.next(String.format("%s's remaining power is %d.",
							this.user2.getUserName(), this.user2.currentPower()));

					if (!this.user1.alive() && !this.user2.alive()) {
						sink.next("Draw. Restart.");
						this.init();
						continue;
					}

					if (!this.user2.alive()) {
						this.user1.win();
						break;
					}
					if (!this.user1.alive()) {
						this.user2.win();
						break;
					}
				}

				if (this.user1.winner()) {
					sink.next(this.user1.getUserName() + " won!");
					sink.complete();
				}
				else if (this.user2.winner()) {
					sink.next(this.user2.getUserName() + " won!");
					sink.complete();
				}
				else if (currentLoop.intValue() >= maxLoop) {
					sink.next("Draw.");
					sink.complete();
				}
			});
		});
	}

	// public static void main(String[] args) throws Exception {
	// User user1 = new User("@making", 170, 30);
	// User user2 = new User("@tmaki", 100, 100);
	// CountDownLatch latch = new CountDownLatch(1);
	// Flux<String> battle = new BattleFlux(user1, user2).start()
	// .doOnComplete(latch::countDown).log();
	// Flux.interval(Duration.ofMillis(100)).zipWith(battle).subscribe();
	// latch.await();
	// }
}
