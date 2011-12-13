package org.sadiframework.utils;

import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class WordUtilsTest {

	@Test
	public void testWrap() {

		String before, after;
		int length;

		length = 5;
		before = "one two three";
		after = "one\ntwo\nthree";
		assertTrue(WordUtils.wrap(before, length).equals(after));

		length = 10;
		before = "one\ntwo\nthree";
		after = "one\ntwo\nthree";
		assertTrue(WordUtils.wrap(before, length).equals(after));

		length = 5;
		before = "one\ntwo three";
		after = "one\ntwo\nthree";
		assertTrue(WordUtils.wrap(before, length).equals(after));

		length = 5;
		before = "one\n\n  two  three";
		after = "one\n\ntwo\nthree";
		assertTrue(WordUtils.wrap(before, length).equals(after));

	}

}
