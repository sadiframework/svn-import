package ca.wilkinsonlab.sparqlassist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.vocabulary.DC;

public class AutoCompleter
{
	private static final Logger log = Logger.getLogger(AutoCompleter.class);
	
	private static final Pattern FROM_PATTERN = Pattern.compile("FROM\\s*<(.+?)>", Pattern.CASE_INSENSITIVE);
	private static final int FROM_PATTERN_URL_GROUP = 1;
	
	OntModel model;
	long lastAccess;
	
	public AutoCompleter()
	{
		model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		updateLastAccess();
	}

	public void destroy()
	{
		model.close();
	}
	
	public List<AutoCompleteMatch> processRequest(AutoCompleteRequest request)
	{
		updateLastAccess();
		loadFromClauses(request.getSPARQL());
		List<AutoCompleteMatch> results = getMatches(request);
		
		// remove duplicates...
		Set<String> seen = new HashSet<String>();
		for (Iterator<AutoCompleteMatch> matches = results.iterator(); matches.hasNext(); ) {
			AutoCompleteMatch match = matches.next();
			String key = String.format("%s %s", match.getURI(), match.getLabel());
				// works because URIs can't have spaces...
			if (seen.contains(key))
				matches.remove();
			else
				seen.add(key);
		}
		return results;
	}
	
	public long getLastAccess()
	{
		return lastAccess;
	}
	
	protected void updateLastAccess()
	{
		lastAccess = System.currentTimeMillis();
	}
	
	protected OntModel getModel()
	{
		return model;
	}
	
	protected void loadFromClauses(String sparql)
	{
		if (log.isTraceEnabled())
			log.trace(String.format("looking for FROM clauses in %s", sparql));
		for (Matcher matcher = FROM_PATTERN.matcher(sparql); matcher.find(); ) {
			String url = matcher.group(FROM_PATTERN_URL_GROUP);
			try {
				model.enterCriticalSection(Lock.WRITE);
				if (!model.hasLoadedImport(url)) {
					log.debug(String.format("loading ontology %s", url));
					model.read(url);
				}
			} catch (Exception e) {
				log.warn(String.format("error reading ontology %s: %s", url, e.toString()));
			} finally {
				model.leaveCriticalSection();
			}
		}
	}

	protected List<AutoCompleteMatch> getMatches(AutoCompleteRequest request)
	{
		List<AutoCompleteMatch> results;
		results = new ArrayList<AutoCompleteMatch>();
		Pattern query = Pattern.compile(Pattern.quote(request.getQuery()), Pattern.CASE_INSENSITIVE);
		switch (request.getCategory()) {
			case PROPERTY:
				addMatchingProperties(query, results);
				break;
			case INDIVIDUAL:
				addMatchingIndividuals(query, results);
				break;
			case NAMESPACE:
				addMatchingNamespaces(query, results);
				break;
			case DEFAULT:
				addMatchingIndividuals(query, results);
				addMatchingProperties(query, results);
				break;
		}
		return results;
	}
	
	protected void addMatchingProperties(Pattern query, List<AutoCompleteMatch> results)
	{
		try {
			model.enterCriticalSection(Lock.READ);
			addMatchingOntResources(model.listAllOntProperties(), query, results);
		} finally {
			model.leaveCriticalSection();
		}
	}

	protected void addMatchingIndividuals(Pattern query, List<AutoCompleteMatch> results)
	{
		try {
			model.enterCriticalSection(Lock.READ);
			addMatchingOntResources(model.listIndividuals(), query, results);
		} finally {
			model.leaveCriticalSection();
		}
	}
	
	protected void addMatchingOntResources(Iterator<? extends OntResource> i, Pattern query, List<AutoCompleteMatch> results)
	{
		while (i.hasNext()) {
			OntResource subject = i.next();
			if (subject.isURIResource()) {
				for (Iterator<RDFNode> labels = subject.listLabels(null); labels.hasNext(); ) {
					Literal label = labels.next().asLiteral(); // listLabels guarantees Literal...
					String text = label.getLexicalForm();
					if (query.matcher(text).find()) {
						results.add(new AutoCompleteMatch()
							.setURI(subject.getURI())
							.setLabel(text)
							.setDescription(getDescription(subject, label.getLanguage()))
						);
					}
				}
			}
		}
	}
	
	protected void addMatchingNamespaces(Pattern query, List<AutoCompleteMatch> results)
	{
		try {
			model.enterCriticalSection(Lock.READ);
			for (Map.Entry<String, String> entry: model.getNsPrefixMap().entrySet()) {
				if (query.matcher(entry.getKey()).find()) {
					results.add(new AutoCompleteMatch()
						.setURI(entry.getValue())
						.setLabel(entry.getKey())
					);
				}
			}
		} finally {
			model.leaveCriticalSection();
		}
	}
	
	protected static String getDescription(OntResource subject, String language)
	{
		// try specified language...
		String description = subject.getComment(language);
		if (description != null)
			return description;

		// try dc:description... (SIO uses this instead of rdfs:comment)
		for (Iterator<RDFNode> i = subject.listPropertyValues(DC.description); i.hasNext(); ) {
			RDFNode value = i.next();
			if (!value.isLiteral())
				continue;
			
			Literal literal = value.asLiteral();
			if (langTagMatch(language, literal.getLanguage()))
				return literal.getLexicalForm();
		}
		
		// try any language...
		if (language != null)
			return getDescription(subject, null);
		
		// no description to be found...
		return null;
	}
		
	/**
	 * Answer true if the desired lang tag matches the target lang tag.
	 * {@link com.hp.hpl.jena.ontology.impl.OntResourceImpl#langTagMatch(String, String)}
	 * is protected, so we have to re-implement here...
	 */
    private static boolean langTagMatch( String desired, String target ) {
        return (desired == null) ||
               (desired.equalsIgnoreCase( target )) ||
               (target != null && target.length() > desired.length() && desired.equalsIgnoreCase( target.substring( desired.length() ) ));
    }
}
