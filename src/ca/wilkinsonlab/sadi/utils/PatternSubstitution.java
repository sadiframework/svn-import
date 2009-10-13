package ca.wilkinsonlab.sadi.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stole this from elmutils until I figure out how best to link the library.
 * @author Luke McCarthy
 */
public class PatternSubstitution
{
	private Pattern inPattern;
	
	private String outPattern;
	
	public PatternSubstitution(String inPattern, String outPattern)
	{
		this(Pattern.compile(inPattern), outPattern);
	}
	
	public PatternSubstitution(Pattern inPattern, String outPattern)
	{
		this.inPattern = inPattern;
		this.outPattern = outPattern;
	}
	
	public boolean matches(String s)
	{
		/* use find() and not matches() to better emulate perl semantics by
		 * not forcing a match to start at the beginning of the string...
		 */
		return inPattern.matcher(s).find();
	}
	
	public String execute(String s)
	{
		/* use find() and not matches() to better emulate perl semantics by
		 * not forcing a match to start at the beginning of the string...
		 */
		Matcher match = inPattern.matcher(s);
		match.find();
		
		/* replace groups in out pattern according to perl semantics...
		 */
		String result = new String(outPattern);
		for (int i=1; i<=match.groupCount(); ++i)
			result = result.replaceAll("[\\\\$]" + String.valueOf(i), match.group(i));
		return result;
	}
}
