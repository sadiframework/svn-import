package org.sadiframework.utils;

import java.util.Scanner;

public class ConsoleUtils {

	public static void echoOff() {
		try {
			ProcessBuilder pb = new ProcessBuilder("stty", "-echo");
			pb.start().waitFor();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void echoOn() {
		try {
			ProcessBuilder pb = new ProcessBuilder("stty", "echo");
			pb.start().waitFor();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String getPassword(String prompt, boolean confirm) {
		while(true) {
			String password = null;
			String password2 = null;
			System.out.print(prompt);
			echoOff();
			Scanner scanner = new Scanner(System.in);
			password = scanner.nextLine();
			echoOn();
			if (confirm) {
				System.out.print("retype password: ");
				echoOff();
				password2 = scanner.nextLine();
				echoOn();
			}
			if (confirm && (!password.equals(password2)))
				System.out.println("Passwords didn't match. Try again.");
			else if (password.isEmpty())
				System.out.println("Password must not be blank. Try again.");
			else
				return password;
		}
	}

}
