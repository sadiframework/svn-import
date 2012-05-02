package org.sadiframework.owl2sparql;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.WordUtils;

public class DefaultVariableGenerator implements VariableGenerator
{
	Set<String> seen;
	
	public DefaultVariableGenerator()
	{
		seen = new HashSet<String>();
	}
	
	@Override
	public String nextVariable(String hint)
	{
		String variable;
		if (hint.startsWith("?")) {
			variable = hint;
		} else {
			variable = Pattern.compile("[^\\w-]").matcher(hint).replaceAll(" ");
			if (!variable.equals(hint)) {
				variable = WordUtils.capitalizeFully(variable);
				variable = Pattern.compile("\\s+").matcher(variable).replaceAll("");
			}
			variable = "?" + variable;
		}
		/* this is particularly inefficient; the rest of the method is no
		 * great shakes, either...
		 */
		while (seen.contains(variable)) {
			Matcher matcher = Pattern.compile("_(\\d+)$").matcher(variable);
			if (matcher.find()) {
				variable = matcher.replaceAll(Integer.valueOf(Integer.valueOf(matcher.group(1)) + 1).toString());
			} else {
				variable = variable + "_2";
			}
		}
		seen.add(variable);
		return variable;
	}
}
