package org.sadiframework.editor.documents;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * This class implements the functionality needed to create a Stylized view of
 * perl scripts in the editor sub panel that is part of the
 * sadi panel.
 * 
 * @author Eddie Kawas
 * 
 */
public class PerlSyntaxDocument extends DefaultStyledDocument {

	private static final long serialVersionUID = -3430148858039659388L;

	private DefaultStyledDocument doc;

	private Element rootElement;

	private boolean multiLineComment;

	private MutableAttributeSet normal;

	private MutableAttributeSet keyword;

	private MutableAttributeSet variable;

	private MutableAttributeSet function;

	private MutableAttributeSet comment;

	private MutableAttributeSet quote;

	private HashSet<String> keywords;

	private HashSet<String> functions;

	private HashMap<String, String> token_map;

	/**
	 * Default Constructor
	 */
	public PerlSyntaxDocument() {
		doc = this;
		rootElement = doc.getDefaultRootElement();
		putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
		// this is the font for plain text
		normal = new SimpleAttributeSet();
		StyleConstants.setForeground(normal, Color.black);

		// style for comments
		comment = new SimpleAttributeSet();
		StyleConstants.setForeground(comment, Color.gray);
		StyleConstants.setItalic(comment, true);

		// style for keywords
		keyword = new SimpleAttributeSet();
		StyleConstants.setForeground(keyword, Color.blue);

		// style for variables
		variable = new SimpleAttributeSet();
		StyleConstants.setForeground(variable, Color.black);
		StyleConstants.setBold(variable, true);

		// style for functions
		function = new SimpleAttributeSet();
		StyleConstants.setForeground(function, Color.magenta);

		// style for quotes
		quote = new SimpleAttributeSet();
		StyleConstants.setForeground(quote, Color.red);

		// the functions that we recognize and style
		String[] perl_functions = new String[] { "abs", "accept", "alarm",
				"atan2", "bind", "binmode", "bless", "caller", "chdir",
				"chmod", "chomp", "chop", "chown", "chr", "chroot", "close",
				"closedir", "connect", "continue", "cos", "croak", "crypt",
				"defined", "delete", "die", "do", "dump", "each", "endgrent",
				"endhostent", "endnetent", "endprotoent", "endpwent",
				"endservent", "eof", "eval", "exec", "exists", "exit", "exp",
				"fcntl", "fileno", "flock", "fork", "format", "getc",
				"getgrent", "getgrgid", "getgrnam", "gethostbyaddr",
				"gethostbyname", "gethostent", "getlogin", "getnetbyaddr",
				"getnetbyname", "getnetent", "getpeername", "getpgrp",
				"getppid", "getpriority", "getprotobyname", "getprotobynumber",
				"getprotoent", "getpwent", "getpwnam", "getpwuid",
				"getservbyname", "getservbyport", "getservent", "getsockname",
				"getsockopt", "glob", "gmtime", "goto", "grep", "hex", "index",
				"int", "ioctl", "join", "keys", "kill", "last", "lc",
				"lcfirst", "length", "link", "listen", "local", "localtime",
				"log", "lstat", "m", "map", "mkdir", "msgctl", "msgget",
				"msgrcv", "msgsnd", "next", "no", "oct", "open", "opendir",
				"ord", "overload", "pack", "pipe", "pop", "pos", "print",
				"printf", "prototype", "push", "q", "qq", "quotemeta", "qw",
				"qx", "rand", "read", "readdir", "readline", "readlink",
				"readpipe", "recv", "redo", "ref", "rename", "require",
				"reset", "return", "reverse", "rewinddir", "rindex", "rmdir",
				"s", "scalar", "seek", "seekdir", "select", "semctl", "semget",
				"semop", "send", "setgrent", "sethostent", "setnetent",
				"setpgrp", "setpriority", "setprotoent", "setpwent",
				"setservent", "setsockopt", "shift", "shmctl", "shmget",
				"shmread", "shmwrite", "shutdown", "sin", "sleep", "socket",
				"socketpair", "sort", "splice", "split", "sprintf", "sqrt",
				"srand", "stat", "study", "substr", "symlink", "syscall",
				"sysopen", "sysread", "sysseek", "system", "syswrite", "tell",
				"telldir", "tie", "tied", "time", "times", "tr", "truncate",
				"uc", "ucfirst", "umask", "undef", "unlink", "unpack",
				"unshift", "untie", "utime", "values", "vec", "wait",
				"waitpid", "wantarray", "warn", "write",
				"y",
				// file tests
				"-r", "-w", "-x", "-o", "-R", "-W", "-O", "-e", "-z", "-s",
				"-f", "-d", "-l", "-p", "-S", "-b", "-c", "-t", "-u", "-g",
				"-u", "-k", "-T", "-B", "-M", "-A", "-C",

		};
		functions = new HashSet<String>();
		functions.addAll(Arrays.asList(perl_functions));

		// keywords that we recognize and style
		String[] perl_keywords = new String[] { "BEGIN", "cmp", "constant",
				"__DATA__", "DESTROY", "do", "else", "elsif", "END", "eq",
				"__FILE__", "for", "foreach", "ge", "goto", "gt", "if", "INIT",
				"last", "le", "lib", "__LINE__", "lt", "my", "ne", "new",
				"next", "not", "our", "package", "__PACKAGE__", "strict",
				"sub", "undef", "unless", "until", "use", "var", "vars",
				"while" };

		keywords = new HashSet<String>();
		keywords.addAll(Arrays.asList(perl_keywords));

		// a map of tokens that we recognize and automatically insert
		token_map = new HashMap<String, String>();
		token_map.put("{", "}");
		token_map.put("[", "]");
		token_map.put("(", ")");
		token_map.put("<", ">");
	}

	@Override
	public void insertString(int offset, String str, AttributeSet a)
			throws BadLocationException {
		boolean replaced = false;
		if (str.equals("{") || str.equals("[") || str.equals("(")) {
			str = addMatchingToken(offset, str);
			replaced = true;
		}

		super.insertString(offset, str, a);
		processChangedLines(offset, str.length());
		if (replaced) {
			// TODO move the cursor to within the brackets
		}
	}

	@Override
	public void remove(int offset, int length) throws BadLocationException {
		super.remove(offset, length);
		processChangedLines(offset, 0);
	}

	/*
	 * Determine how many lines have been changed, then apply highlighting to
	 * each line
	 */
	public void processChangedLines(int offset, int length)
			throws BadLocationException {
		String content = doc.getText(0, doc.getLength());

		// The lines affected by the latest document update

		int startLine = rootElement.getElementIndex(offset);
		int endLine = rootElement.getElementIndex(offset + length);

		// Make sure all comment lines prior to the start line are commented
		// and determine if the start line is still in a multi line comment

		setMultiLineComment(commentLinesBefore(content, startLine));

		// Do the actual highlighting

		for (int i = startLine; i <= endLine; i++) {
			applyHighlighting(content, i);
		}

		// Resolve highlighting to the next end multi line delimiter

		if (!isMultiLineComment())
			highlightLinesAfter(content, endLine);
	}

	private boolean commentLinesBefore(String content, int line) {
		int offset = rootElement.getElement(line).getStartOffset();

		int startDelimiter = lastIndexOf(content, getStartDelimiter(),
				offset - 2);

		if (startDelimiter < 0)
			return false;

		// Matching start/end of comment found, nothing to do

		int endDelimiter = -1;// indexOf(content, getEndDelimiter(),
		// startDelimiter);

		if (endDelimiter < offset & endDelimiter != -1)
			return false;

		// End of comment not found, highlight the lines
		doc.setCharacterAttributes(startDelimiter, offset - startDelimiter + 1,
				comment, false);
		return true;
	}

	/*
	 * Highlight lines to start or end delimiter
	 */
	private void highlightLinesAfter(String content, int line)
			throws BadLocationException {
		int offset = rootElement.getElement(line).getEndOffset();

		// Start/End delimiter not found, nothing to do

		int startDelimiter = indexOf(content, getStartDelimiter(), offset);
		int endDelimiter = -1;// indexOf(content, getEndDelimiter(), offset);

		if (startDelimiter < 0)
			startDelimiter = content.length();

		if (endDelimiter < 0)
			endDelimiter = content.length();

		int delimiter = Math.min(startDelimiter, endDelimiter);

		if (delimiter < offset)
			return;

		// Start/End delimiter found, reapply highlighting

		int endLine = rootElement.getElementIndex(delimiter);

		for (int i = line + 1; i < endLine; i++) {
			Element branch = rootElement.getElement(i);
			Element leaf = doc.getCharacterElement(branch.getStartOffset());
			AttributeSet as = leaf.getAttributes();

			if (as.isEqual(comment))
				applyHighlighting(content, i);
		}
	}

	/*
	 * Parse the line to determine the appropriate highlighting
	 */
	private void applyHighlighting(String content, int line)
			throws BadLocationException {
		int startOffset = rootElement.getElement(line).getStartOffset();
		int endOffset = rootElement.getElement(line).getEndOffset() - 1;

		int lineLength = endOffset - startOffset;
		int contentLength = content.length();

		if (endOffset >= contentLength)
			endOffset = contentLength - 1;

		// check for multi line comments
		// (always set the comment attribute for the entire line)

		if (endingMultiLineComment(content, startOffset, endOffset)
				|| isMultiLineComment()
				|| startingMultiLineComment(content, startOffset, endOffset)) {
			doc.setCharacterAttributes(startOffset,
					endOffset - startOffset + 1, comment, false);
			return;
		}

		// set normal attributes for the line

		doc.setCharacterAttributes(startOffset, lineLength, normal, true);

		// check for single line comment

		int index = content.indexOf(getSingleLineDelimiter(), startOffset);

		if ((index > -1) && (index < endOffset)) {
			doc.setCharacterAttributes(index, endOffset - index + 1, comment,
					false);
			endOffset = index - 1;
		}

		// check for tokens

		checkForTokens(content, startOffset, endOffset);
	}

	/*
	 * Does this line contain the start delimiter
	 */
	private boolean startingMultiLineComment(String content, int startOffset,
			int endOffset) throws BadLocationException {
		int index = indexOf(content, getStartDelimiter(), startOffset);

		if ((index < 0) || (index > endOffset))
			return false;
		else {
			setMultiLineComment(true);
			return true;
		}
	}

	/*
	 * Does this line contain the end delimiter
	 */
	private boolean endingMultiLineComment(String content, int startOffset,
			int endOffset) throws BadLocationException {
		int index = -1; // indexOf(content, getEndDelimiter(), startOffset);

		if ((index < 0) || (index > endOffset))
			return false;
		else {
			setMultiLineComment(false);
			return true;
		}
	}

	/*
	 * We have found a start delimiter and are still searching for the end
	 * delimiter
	 */
	private boolean isMultiLineComment() {
		return multiLineComment;
	}

	private void setMultiLineComment(boolean value) {
		multiLineComment = value;
	}

	/*
	 * Parse the line for tokens to highlight
	 */
	boolean inQuote = false;

	private void checkForTokens(String content, int startOffset, int endOffset) {
		while (startOffset <= endOffset) {
			// skip the delimiters to find the start of a new token

			while (isDelimiter(content.substring(startOffset, startOffset + 1))) {
				if (startOffset < endOffset)
					startOffset++;
				else
					return;
			}

			// Extract and process the entire token

			if (isQuoteDelimiter(content
					.substring(startOffset, startOffset + 1)))
				startOffset = getQuoteToken(content, startOffset, endOffset);
			else
				startOffset = getOtherToken(content, startOffset, endOffset);
		}
	}

	/*
     * 
     */
	private int getQuoteToken(String content, int startOffset, int endOffset) {
		String quoteDelimiter = content.substring(startOffset, startOffset + 1);
		String escapeString = getEscapeString(quoteDelimiter);

		int index;
		int endOfQuote = startOffset;

		// skip over the escape quotes in this quote

		index = content.indexOf(escapeString, endOfQuote + 1);

		while ((index > -1) && (index < endOffset)) {
			endOfQuote = index + 1;
			index = content.indexOf(escapeString, endOfQuote);
		}

		// now find the matching delimiter

		index = content.indexOf(quoteDelimiter, endOfQuote + 1);

		if ((index < 0) || (index > endOffset))
			endOfQuote = endOffset;
		else
			endOfQuote = index;

		doc.setCharacterAttributes(startOffset, endOfQuote - startOffset + 1,
				quote, false);

		return endOfQuote + 1;
	}

	/*
     * 
     */
	private int getOtherToken(String content, int startOffset, int endOffset) {
		int endOfToken = startOffset + 1;

		while (endOfToken <= endOffset) {
			if (isDelimiter(content.substring(endOfToken, endOfToken + 1)))
				break;

			endOfToken++;
		}

		String token = content.substring(startOffset, endOfToken);

		if (isKeyword(token)) {
			doc.setCharacterAttributes(startOffset, endOfToken - startOffset,
					keyword, false);
		}

		if (isFunction(token)) {
			doc.setCharacterAttributes(startOffset, endOfToken - startOffset,
					function, false);
		}

		if (isVariable(token)) {
			doc.setCharacterAttributes(startOffset, endOfToken - startOffset,
					variable, false);
		}

		return endOfToken + 1;
	}

	/*
	 * Assume the needle will the found at the start/end of the line
	 */
	private int indexOf(String content, String needle, int offset) {
		int index;

		while ((index = content.indexOf(needle, offset)) != -1) {
			String text = getLine(content, index).trim();

			if (text.startsWith(needle) || text.endsWith(needle))
				break;
			else
				offset = index + 1;
		}

		return index;
	}

	/*
	 * Assume the needle will the found at the start/end of the line
	 */
	private int lastIndexOf(String content, String needle, int offset) {
		int index;

		while ((index = content.lastIndexOf(needle, offset)) != -1) {
			String text = getLine(content, index).trim();

			if (text.startsWith(needle) || text.endsWith(needle))
				break;
			else
				offset = index - 1;
		}

		return index;
	}

	private String getLine(String content, int offset) {
		int line = rootElement.getElementIndex(offset);
		Element lineElement = rootElement.getElement(line);
		int start = lineElement.getStartOffset();
		int end = lineElement.getEndOffset();
		return content.substring(start, end - 1);
	}

	/**
	 * 
	 * @param character
	 * @return
	 */
	protected boolean isDelimiter(String character) {
		String operands = ";[](){}="; // ";:{}()[]+-/%<=>!&|^~*";

		if (Character.isWhitespace(character.charAt(0))
				|| operands.indexOf(character) != -1)
			return true;
		else
			return false;
	}

	/**
	 * 
	 * @param character
	 * @return
	 */
	protected boolean isQuoteDelimiter(String character) {
		String quoteDelimiters = "\"'/";
		if (quoteDelimiters.indexOf(character) < 0)
			return false;
		else
			return true;
	}

	/**
	 * 
	 * @param token
	 * @return
	 */
	protected boolean isKeyword(String token) {
		return keywords.contains(token);
	}

	/**
	 * 
	 * @param token
	 * @return
	 */
	protected boolean isVariable(String token) {
		return token.startsWith("$") || token.startsWith("%")
				|| token.startsWith("@") || token.indexOf("::") != -1
				|| token.indexOf("->") > 0;
	}

	/**
	 * 
	 * @param token
	 * @return
	 */
	protected boolean isFunction(String token) {
		return functions.contains(token);
	}

	/**
	 * 
	 * @return
	 */
	protected String getStartDelimiter() {
		return "__END__";
	}

	/**
	 * 
	 * @return
	 */
	protected String getEndDelimiter() {
		return "";
	}

	/**
	 * 
	 * @return
	 */
	protected String getSingleLineDelimiter() {
		return "#";
	}

	/**
	 * 
	 * @param quoteDelimiter
	 * @return
	 */
	protected String getEscapeString(String quoteDelimiter) {
		return "\\" + quoteDelimiter;
	}

	/**
	 * 
	 * @param offset
	 * @return
	 * @throws BadLocationException
	 */
	protected String addMatchingToken(int offset, String token)
			throws BadLocationException {
		StringBuffer whiteSpace = new StringBuffer();
		int line = rootElement.getElementIndex(offset);
		int i = rootElement.getElement(line).getStartOffset();

		while (true) {
			String temp = doc.getText(i, 1);

			if (temp.equals(" ") || temp.equals("\t")) {
				whiteSpace.append(temp);
				i++;
			} else
				break;
		}

		if (token.equals("{"))
			return token + "\n" + whiteSpace.toString() + "    \n"
					+ whiteSpace.toString() + token_map.get(token);
		return token + "  " + token_map.get(token);
	}
}
