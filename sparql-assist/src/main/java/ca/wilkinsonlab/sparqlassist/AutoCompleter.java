package ca.wilkinsonlab.sparqlassist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
	
	private static final Pattern FROM_PATTERN = Pattern.compile("FROM\\s*<(._?)>", Pattern.CASE_INSENSITIVE);
	private static final int FROM_PATTERN_URL_GROUP = 1;
	
	OntModel model;
	long lastAccess;
	
	public AutoCompleter()
	{
		model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		updateLastAccess();
	}
	
	public List<AutoCompleteMatch> processRequest(AutoCompleteRequest request)
	{
		updateLastAccess();
		loadFromClauses(request.getSPARQL());
		List<AutoCompleteMatch> results = new ArrayList<AutoCompleteMatch>();
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

	public void destroy()
	{
		model.close();
	}
	
	public long getLastAccess()
	{
		return lastAccess;
	}
	
	private void updateLastAccess()
	{
		lastAccess = System.currentTimeMillis();
	}
	
	private void loadFromClauses(String sparql)
	{
		for (Matcher matcher = FROM_PATTERN.matcher(sparql); matcher.find(); ) {
			String url = matcher.group(FROM_PATTERN_URL_GROUP);
			try {
				model.enterCriticalSection(Lock.WRITE);
				if (!model.hasLoadedImport(url)) {
					log.debug(String.format("loading ontology from %s", url));
					model.read(url);
				}
			} finally {
				model.leaveCriticalSection();
			}
		}
	}
	
	
	private void addMatchingProperties(Pattern query, List<AutoCompleteMatch> results)
	{
		try {
			model.enterCriticalSection(Lock.READ);
			addMatchingOntResources(model.listAllOntProperties(), query, results);
		} finally {
			model.leaveCriticalSection();
		}
	}

	private void addMatchingIndividuals(Pattern query, List<AutoCompleteMatch> results)
	{
		try {
			model.enterCriticalSection(Lock.READ);
			addMatchingOntResources(model.listIndividuals(), query, results);
		} finally {
			model.leaveCriticalSection();
		}
	}
	
	private void addMatchingOntResources(Iterator<? extends OntResource> i, Pattern query, List<AutoCompleteMatch> results)
	{
		while (i.hasNext()) {
			OntResource subject = i.next();
			if (subject.isURIResource()) {
				for (Iterator<RDFNode> labels = subject.listLabels(null); labels.hasNext(); ) {
					Literal label = labels.next().asLiteral(); // listLabels guarantees Literal...
					String text = label.getLexicalForm();
					if (query.matcher(text).find()) {
						results.add(new AutoCompleteMatch()
							.setUri(subject.getURI())
							.setLabel(text)
							.setDescription(getDescription(subject, label.getLanguage()))
						);
					}
				}
			}
		}
	}
	
	private void addMatchingNamespaces(Pattern query, List<AutoCompleteMatch> results)
	{
		for (Map.Entry<String, String> entry: model.getNsPrefixMap().entrySet()) {
			if (query.matcher(entry.getKey()).find()) {
				results.add(new AutoCompleteMatch()
					.setUri(entry.getValue())
					.setLabel(entry.getKey())
				);
			}
		}
	}
	
	private static String getDescription(OntResource subject, String language)
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
