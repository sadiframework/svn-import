package org.sadiframework.utils;

public class ProcessUtils {

	public static void killAndWait(Process process) {
		process.destroy();
		try {
			process.waitFor();
		} catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
