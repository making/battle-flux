package com.example;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTest {

	@Test
	public void scan() {
		// ByteBuffer buf = ByteBuffer.wrap("@foo 150".getBytes());
		User user = User
				.scan(new Scanner(new ByteArrayInputStream("@foo 150".getBytes())));
		assertThat(user.getUserName()).isEqualTo("@foo");
		assertThat(user.getOffensivePower()).isEqualTo(150);
		assertThat(user.getDefensivePower()).isEqualTo(50);
	}
}