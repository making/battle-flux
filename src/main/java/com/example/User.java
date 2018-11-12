package com.example;

import java.util.Random;
import java.util.Scanner;

import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class User {

	private int defensivePower;

	private int offensivePower;

	private int power;

	private String userName;

	private boolean winner;

	public User(String userName, int offensivePower, int defensivePower) {
		this();
		this.userName = userName;
		this.offensivePower = offensivePower;
		this.defensivePower = defensivePower;
	}

	public User() {
		this.init();
	}

	public static User scan(Scanner scanner) {
		try (Scanner s = scanner) {
			String userName = s.next();
			int offensivePower = min(200, max(s.nextInt(), 0));
			int defensivePower = 200 - offensivePower;
			return new User(userName, offensivePower, defensivePower);
		}
	}

	public boolean alive() {
		return this.power > 0;
	}

	public Tuple2<Integer, Boolean> attack(User defenser, Random random) {
		boolean isCritical = random.nextInt(100) < 5;
		int damage = (isCritical ? this.criticalDamage(defenser)
				: this.normalDamage(defenser)) + random.nextInt(10);
		defenser.damaged(damage);
		return Tuples.of(damage, isCritical);
	}

	public int currentPower() {
		return this.power;
	}

	public void damaged(int damage) {
		this.power = Math.max(this.power - damage, 0);
	}

	public int getDefensivePower() {
		return defensivePower;
	}

	public void setDefensivePower(int defensivePower) {
		this.defensivePower = defensivePower;
	}

	public int getOffensivePower() {
		return offensivePower;
	}

	public void setOffensivePower(int offensivePower) {
		this.offensivePower = offensivePower;
	}

	public String getUserName() {
		return userName;
	}

	// Visible for Jackson

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void init() {
		this.power = 300;
		this.winner = false;
	}

	@Override
	public String toString() {
		return "User{" + "userName='" + userName + '\'' + ", offensivePower="
				+ offensivePower + ", defensivePower=" + defensivePower + ", power="
				+ power + '}';
	}

	public void win() {
		this.winner = true;
	}

	public boolean winner() {
		return this.winner;
	}

	int criticalDamage(User defenser) {
		return Math.max((int) (30
				+ (this.getOffensivePower() * 1.4 - defenser.getDefensivePower()) * 0.8
				+ this.getOffensivePower() * 0.03), 25);
	}

	int normalDamage(User defenser) {
		return Math.max((int) (18
				+ (this.getOffensivePower() - defenser.getDefensivePower() * 1.2) * 0.4
				+ this.getOffensivePower() * 0.03), 1);
	}
}
