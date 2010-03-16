package org.sadiframework.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Checksum {

	/**
	 * 
	 * @param text
	 *            the text to do MD5 checksum on
	 * @return an MD5 checksum for the given text or empty string if there were
	 *         any problems.
	 */
	public static String getMD5Checksum(String text) {
		MessageDigest complete;
		try {
			complete = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			return "";
		}
		byte[] b = complete.digest(text.getBytes());
		String result = "";
		for (int i = 0; i < b.length; i++) {
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}
}
