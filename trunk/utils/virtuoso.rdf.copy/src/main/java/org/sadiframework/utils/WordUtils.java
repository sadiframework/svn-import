package org.sadiframework.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordUtils {

	protected static final Pattern NEWLINE_PATTERN = Pattern.compile("\\r?\\n");
	protected static final Pattern CHAR_OR_NEWLINE_PATTERN = Pattern.compile("[^ \\t]");
	protected static final Pattern SPACE_PATTERN = Pattern.compile("[ \\t]+");

	// must be -2 to avoid conflict with BreakIterator.DONE
	protected static final int NO_VALUE = -2;

	public static String wrap(String text, int length) {

		StringBuilder wrappedText = new StringBuilder();
		Matcher nextNewline = NEWLINE_PATTERN.matcher(text);
		Matcher nextCharOrNewline = CHAR_OR_NEWLINE_PATTERN.matcher(text);
		Matcher nextSpace = SPACE_PATTERN.matcher(text);

		int lineStartPos = 0;
		int nextSpacePos = NO_VALUE;
		while (true) {

			int nextBreakPos = NO_VALUE;
			boolean hitNewline = false;

			if (nextNewline.find(lineStartPos) && ((nextNewline.start() - lineStartPos) < length)) {
				hitNewline = true;
				nextBreakPos = nextNewline.end();
			} else {
				if (nextSpace.find(lineStartPos)) {
					nextSpacePos = nextSpace.start();
					while ((nextSpacePos - lineStartPos) < length) {
						nextBreakPos = nextSpacePos;
						if (nextSpace.find())
							nextSpacePos = nextSpace.start();
						else
							break;
					}
				}
				// if we couldn't find a space in the legal range,
				// do a line break in the middle of a word
				if (nextBreakPos == NO_VALUE)
					nextBreakPos = Math.min(lineStartPos + length, text.length());
			}

			String line = text.substring(lineStartPos, nextBreakPos);
			wrappedText.append(line);

			if (nextCharOrNewline.find(nextBreakPos)) {
				if (!hitNewline)
					wrappedText.append("\n");
				lineStartPos = nextCharOrNewline.start();
			} else {
				break;
			}
		}

		return wrappedText.toString();
	}

}
