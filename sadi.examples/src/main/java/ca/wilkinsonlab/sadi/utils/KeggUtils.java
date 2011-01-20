package ca.wilkinsonlab.sadi.utils;

import java.util.regex.Pattern;

public class KeggUtils 
{
	static public final Pattern[] GENE_URI_PATTERNS = {
		Pattern.compile("http://lsrn.org/KEGG:(\\S+)"),
		Pattern.compile(".*[/:#](\\S{3}:\\S+)") // failsafe best-guess pattern
	};

	static public final Pattern[] PATHWAY_URI_PATTERNS = {
		Pattern.compile("http://lsrn.org/KEGG_PATHWAY:(\\S+)"),
		Pattern.compile(".*[/:#]([^\\s\\.]+)") // failsafe best-guess pattern
	};

	static public final Pattern[] COMPOUND_URI_PATTERNS = {
		Pattern.compile("http://lsrn.org/KEGG:(\\S+)"),
		Pattern.compile(".*[/:#](cpd:\\S+)", Pattern.CASE_INSENSITIVE) // failsafe best-guess pattern
	};
}
