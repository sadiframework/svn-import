package ca.wilkinsonlab.sadi.share;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.client.MultiRegistry;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceInvocationException;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLServiceWrapper;
import ca.wilkinsonlab.sadi.common.SADIException;
import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.ResourceTyper;
import ca.wilkinsonlab.sadi.utils.OwlUtils.PropertyRestrictionAdapter;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.core.TriplePath;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;
import com.hp.hpl.jena.sparql.syntax.ElementVisitorBase;
import com.hp.hpl.jena.sparql.syntax.ElementWalker;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class SHAREKnowledgeBase
{
	private static final Logger log = Logger.getLogger( SHAREKnowledgeBase.class );
	
	private MultiRegistry registry;
	
	private OntModel reasoningModel;
	private Model dataModel;
	
	private Map<String, PotentialValues> variableBindings;
	
	private Tracker tracker;
	private Set<String> deadServices;
	/** allow ARQ-specific extensions to SPARQL query syntax (e.g. GROUP BY, HAVING, arithmetic expressions) */ 
	protected boolean allowARQSyntax;
	/** decide ordering of query patterns as the query runs */
	protected boolean useAdaptiveQueryPlanning;
	
	// TODO rename to something less unwieldy?
	private boolean dynamicInputInstanceClassification;
	
	public SHAREKnowledgeBase()
	{
		this(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF), false);
	}

	public SHAREKnowledgeBase(boolean allowARQSyntax) {
		this(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF), allowARQSyntax);
	}
	
	public SHAREKnowledgeBase(OntModel reasoningModel) {
		this(reasoningModel, false);
	}
	
	public SHAREKnowledgeBase(OntModel reasoningModel, boolean allowARQSyntax)
	{
		this(reasoningModel, ModelFactory.createDefaultModel(), allowARQSyntax);
	}
	
	public SHAREKnowledgeBase(OntModel reasoningModel, Model dataModel, boolean allowARQSyntax)
	{
		log.debug("new ca.wilkinsonlab.sadi.share.DynamicKnowledgeBase instantiated");

		setAllowARQSyntax(allowARQSyntax);
		
		this.registry = Config.getConfiguration().getMasterRegistry();
		
		this.reasoningModel = reasoningModel;
		this.dataModel = dataModel;
		if (!reasoningModel.listSubModels().toSet().contains(dataModel)) {
			log.debug("adding data-only model as a sub-model of reasoning model");
			reasoningModel.addSubModel(dataModel);
		}
		
		variableBindings = new HashMap<String, PotentialValues>();
		
		tracker = new Tracker();
		
		Configuration config = ca.wilkinsonlab.sadi.share.Config.getConfiguration();
		deadServices = Collections.synchronizedSet(new HashSet<String>());
		for (Object serviceUri: config.getList("share.deadService"))
			deadServices.add((String)serviceUri);
		
		dynamicInputInstanceClassification = config.getBoolean("share.dynamicInputInstanceClassification", false);
		useAdaptiveQueryPlanning = config.getBoolean("share.useAdaptiveQueryPlanning", false);
//		skipPropertiesPresentInKB = config.getBoolean("share.skipPropertiesPresentInKB", false);
		
	}
	
	protected void setAllowARQSyntax(boolean allowARQSyntax) {
		this.allowARQSyntax = allowARQSyntax;
	}
	
	public boolean allowARQSyntax() {
		return this.allowARQSyntax;
	}
	
	public Syntax getQuerySyntax() {
		return (allowARQSyntax() ? Syntax.syntaxARQ : Syntax.syntaxSPARQL);
	}

	public void dispose()
	{
		/* this is not working because (unbelievably) it closes the root OWL ontology model...
		 * clearly Jena is not imagining a world where multiple reasoners coexist in the
		 * same VM long-term...
		 */
//		/* before we close our reasoning model, we have to remove all sub-
//		 * models, or they'll be closed too (this is mostly a problem because
//		 * it includes imported models, which are reused by Jena later...)
//		 * (remove with no rebind so that it's quicker, since we're about to
//		 * close this reasoning model anyway...)
//		 */
//		for (OntModel subModel: reasoningModel.listSubModels().toList()) {
//			reasoningModel.removeSubModel(subModel);
//		}
//		reasoningModel.close();
		
		dataModel.close();
		
		/* this shouldn't actually be necessary, as closing the models should
		 * free up the resources associated with the RDFNodes in the variable
		 * bindings...
		 */
		variableBindings.clear();
	}
	
	public Model getDataModel()
	{
		return dataModel;
	}
	
	public OntModel getReasoningModel()
	{
		return reasoningModel;
	}
	
	public void executeQuery(String query)
	{
		executeQuery(QueryFactory.create(query, getQuerySyntax()));
	}
	
	public void executeQuery(String query, QueryPatternOrderingStrategy strategy)
	{
		executeQuery(QueryFactory.create(query, getQuerySyntax()), strategy);
	}
	
	public void executeQuery(Query query)
	{
		if (useAdaptiveQueryPlanning) {
			executeQueryAdaptive(query);
		} else {
			executeQuery(query, new DefaultQueryPatternOrderingStrategy());
		}
	}
	
	public void executeQuery(Query query, QueryPatternOrderingStrategy strategy)
	{
		loadFromClauses(query);
		
		List<Triple> queryPatterns = new QueryPatternEnumerator(query).getQueryPatterns();
		try {
			queryPatterns = strategy.orderPatterns(queryPatterns);
		} catch (UnresolvableQueryException e) {
			log.error(String.format("failed to order query %s with strategy %s", query, strategy), e);
		}
		
		for (Triple pattern: queryPatterns) {
			processPattern(pattern);
		}
	}
	
	protected void executeQueryAdaptive(Query query)
	{
		loadFromClauses(query);

		log.trace("using adaptive query planning");
		
		List<Triple> queryPatterns = new QueryPatternEnumerator(query).getQueryPatterns();
		Set<Triple> visitedPatterns = new HashSet<Triple>();
		QueryPatternComparator comparator = new QueryPatternComparator();
		
		while (visitedPatterns.size() < queryPatterns.size()) {	
			Triple bestPattern = null;

			for (Triple pattern : queryPatterns) {
				if (visitedPatterns.contains(pattern)) {
					continue;
				}
				if (bestPattern == null || comparator.compare(pattern, bestPattern) < 0) {
					bestPattern = pattern;
				}
			}
			
			processPattern(bestPattern);
			visitedPatterns.add(bestPattern);
		}
	}
	
	protected void loadFromClauses(Query query) 
	{
		/* load all of the graphs referenced in the FROM clause into the kb
		 * TODO might have to make this configurable or turn it off, if people
		 * are using the FROM clause in other ways...
		 */
		for (String sourceURI: query.getGraphURIs()) {
			try{ 
				dataModel.read( sourceURI );
			} catch (Exception e) {
				log.error(String.format("failed to read FROM graph %s", sourceURI));
			}
		}
	}
	
	private void processPattern(Triple pattern)
	{	
		log.trace(String.format("query pattern %s", pattern));
		
		PotentialValues subjects = expandQueryNode(pattern.getSubject());
		PotentialValues predicates = expandQueryNode(pattern.getPredicate());
		PotentialValues objects = expandQueryNode(pattern.getObject());
		
		Set<OntProperty> properties = getOntProperties(RdfUtils.extractResources(predicates.values));
		
		/* if the predicate position has bindings, but none of them are URIs, 
		 * then this pattern has no solutions. 
		 */
		if(!predicates.isEmpty() && properties.size() == 0) {
			log.trace(String.format("pattern has no solution, all bindings for predicate var %s are non-URI values", predicates.key));
			return;
		}
			
		if (!subjects.isEmpty()) { // bound subject...
			if (!objects.isEmpty()) { // bound subject and object...
				if (bestDirectionIsForward(pattern)) {
					gatherTriples(subjects, properties, objects);
				} else {
					gatherTriples(objects, getInverseProperties(properties), objects);
				}
			} else { // bound subject, unbound object...
				gatherTriples(subjects, properties, objects);
			}
		} else if (!objects.isEmpty()) { // unbound subject, bound object...
			gatherTriples(objects, getInverseProperties(properties), subjects);
		} else { // unbound subject, unbound object...mygrid.org.uk/ontology#bioi
			/* TODO try to find subjects by looking for instances of input
			 * classes for services that generate the required property...
			 */
			log.warn(String.format("encountered a pattern whose subject and object are both unbound variables %s", pattern));
		}

		populateVariableBinding(subjects, predicates, objects);

		/* note: this must come after normal processing of the triple pattern,
		 * so that rdf:type patterns are also resolved against SPARQL endpoints.
		 * --BV
		 */
		Node p = pattern.getPredicate();
		if (p.isURI() && p.getURI().equals(RDF.type.getURI())) {
			processTypePattern(pattern);
		}

	}
	
	protected boolean bestDirectionIsForward(Triple pattern) 
	{
		PotentialValues s = expandQueryNode(pattern.getSubject());
		PotentialValues o = expandQueryNode(pattern.getObject());
		
		if (s.isEmpty() || o.isEmpty()) {
			throw new RuntimeException("expected both subject and object positions to be bound");
		}
		
		return (s.values.size() < o.values.size());
	}
	
	protected Set<OntProperty> getOntProperties(Collection<Resource> predicates) 
	{
		Set<OntProperty> predicateSet = new HashSet<OntProperty>();
		for (Resource predicate : predicates) {
			predicateSet.add(getOntProperty(predicate.getURI()));
		}
		return predicateSet;
	}

	protected Set<OntProperty> getInverseProperties(Collection<OntProperty> properties) 
	{
		Set<OntProperty> inverseProperties = new HashSet<OntProperty>();
		for (OntProperty property : properties) {
			inverseProperties.add(getInverseProperty(property));
		}
		return inverseProperties;
	}		

	
	/* this now expands a class definition into the triple patterns
	 * that describe it, so we should be able to use it to convert from
	 * a class description to a SPARQL query...
	 */
	private void processTypePattern(final Triple pattern)
	{
		/* if this starts happening (meaning we can have anonymous class
		 * expressions in queries), we'll have to do something about it...
		 */
		if (!pattern.getObject().isURI()) {
			log.warn(String.format("skipping non-URI object of rdf:type %s", pattern.getObject()));
			return;
		}
		OntClass c = getOntClass(pattern.getObject().getURI());
		
		final PotentialValues subjects = expandQueryNode(pattern.getSubject());
		if (tracker.beenThere(subjects, c))
			return;
		
		log.debug(String.format("gathering triples to find instances of %s", c));
		
		OwlUtils.decompose(c, new PropertyRestrictionAdapter() {
			public void onProperty(OntProperty onProperty)
			{
				PotentialValues objects = getNewVariableBinding();
				Triple newPattern = Triple.create(pattern.getSubject(), onProperty.asNode(), objects.variable);
				processPattern(newPattern);
			}
			public void hasValue(OntProperty onProperty, RDFNode hasValue)
			{
				Triple newPattern = Triple.create(pattern.getSubject(), onProperty.asNode(), hasValue.asNode());
				processPattern(newPattern);
			}
			public void valuesFrom(OntProperty onProperty, OntResource valuesFrom)
			{
				PotentialValues objects = getNewVariableBinding();
				Triple newPattern = Triple.create(pattern.getSubject(), onProperty.asNode(), objects.variable);
				processPattern(newPattern);
				Triple typePattern = Triple.create(objects.variable, RDF.type.asNode(), valuesFrom.asNode());
				processPattern(typePattern);
			}
		});
	}
	
	/* return a set of potential values this query node represents;
	 * this will be empty if the node is an unbound variable.
	 */
	private PotentialValues expandQueryNode(Node queryPatternNode)
	{
		if (queryPatternNode.isVariable()) {
			return getVariableBinding(queryPatternNode);
		} else {
			return new PotentialValues(dataModel.getRDFNode(queryPatternNode));
		}
	}
	
	private PotentialValues getVariableBinding(Node variable)
	{
		if (!variableBindings.containsKey(variable.getName())) {
			log.trace(String.format("first time encountering variable %s", variable));
			variableBindings.put(variable.getName(), new PotentialValues(variable));
		}
		return variableBindings.get(variable.getName());
	}

	private PotentialValues getNewVariableBinding()
	{
		String varName = null;
		do {
			varName = getNextVariableName(varName);
		} while (variableBindings.containsKey(varName));
		
		return getVariableBinding(Node.createVariable(varName));
	}

	/* horribly hacky and inefficient; find some library to generate a
	 * pronounceable word...
	 */
	private static String alpha = "abcdefghijklmnopqrstuvwxyz";
	private String getNextVariableName(String variable)
	{
		if (StringUtils.isEmpty(variable))
			return "" + alpha.charAt(0); // not String.valueOf because I need a new string...
		
		String lastLetter = StringUtils.substring(variable, -1);
		int i = alpha.indexOf(lastLetter) + 1;
		if (i >= alpha.length() || i < 0) {
			return variable + alpha.charAt(0);
		} else {
			return StringUtils.substring(variable, 0, -1) + alpha.charAt(i);
		}
	}
	
	/* look up the specified URI in the reasoning model; if it's not there,
	 * try to resolve the URI into the model; if that doesn't work, just
	 * create it.
	 */
	private OntClass getOntClass(String uri)
	{
		OntClass c = null;
		try {
			OwlUtils.getOntClassWithLoad(reasoningModel, uri);
		} catch (SADIException e) {
			log.error(e.getMessage());
		}
		
		if (c != null) {
			return c;
		} else {
			log.warn(String.format("creating undefined and unresolvable class %s", uri));
			return reasoningModel.createClass(uri);
		}
	}

	/* look up the specified URI in the reasoning model; if it's not there,
	 * try to resolve the URI into the model; if that doesn't work, just
	 * create it.
	 * TODO maybe we should create a wrapped OntModel that just does this;
	 * it might be worthwhile given the number of different places we have
	 * to implement this behaviour...
	 */
	private OntProperty getOntProperty(String uri)
	{
		OntProperty p = null;
		try {
			p = OwlUtils.getOntPropertyWithLoad(reasoningModel, uri);
		} catch (SADIException e) {
			log.error(e.getMessage());
		}
		
		if (p != null) {
			return p;
		} else {
			log.warn(String.format("creating undefined and unresolvable property %s", uri));
			return reasoningModel.createOntProperty(uri);
		}
	}
	
	private OntProperty getInverseProperty(OntProperty p)
	{
		OntProperty inverse = p.getInverse();
		if (inverse == null) {
			log.warn(String.format("creating inverse property of %s", p.getURI()));
			inverse = reasoningModel.createOntProperty(p.getURI() + "-inverse");
			inverse.addInverseOf(p);
			p.addInverseOf(inverse);
		}
		return inverse;
	}
	
	private void attachTypes(Set<RDFNode> nodes)
	{
		for (RDFNode node: nodes) {
			if (node.isResource()) {
				attachType(node.as(Resource.class));
			}
		}
	}
	
	private void attachType(Resource resource)
	{
		/* NOTE: Resources from OntModels with reasoning (such as reasoningModel) may
		 * infer rdf:types such as rdf:Resource and owl:Thing for every resource,
		 * so this check for an existing rdf:type is not safe. (See Bug 14 in Bugzilla.) --BV
		 */
		//if ( !RdfUtils.isTyped(resource) ) {
		//log.trace(String.format("attaching type to untyped node %s", resource));
		ResourceTyper.getResourceTyper().attachType(resource);
		//}
	}
	
	/* gather triples matching a predicate, optionally storing the subjects in an accumulator.
	 */
	private void gatherTriples(PotentialValues subjects, Collection<OntProperty> predicates, PotentialValues objects)
	{
		log.debug(String.format("gathering triples with predicates %s", predicates));
		log.trace(String.format("potential subjects %s", subjects.values));

		if (subjects == null) {
			log.warn("attempt to call gatherTriplesByPredicate with null subject");
			return;
		} else if (subjects.isEmpty()) {
			log.trace("nothing to do");
			return;
		}
		
		Collection<Service> services;
		if (predicates.size() == 0) {
			log.trace("predicate is unbound variable, testing inputs against *all* registered services");
			services = registry.getAllServices();
		} else {
			log.trace(String.format("finding services by predicate(s) %s", predicates));
			services = getServicesByPredicate(predicates);
		}

		log.debug(String.format("found %d service%s total", services.size(), services.size() == 1 ? "" : "s"));
		
		for (Service service : services) {
			/* copy the collection of potential inputs as they'll be filtered
			 * for each service...
			 */
			
			Set<RDFNode> inputs = new HashSet<RDFNode>(subjects.values);
			Collection<Triple> output = maybeCallService(service, inputs);
			for (Triple triple: output) {
				log.trace(String.format("adding triple to data model %s", triple));
				RdfUtils.addTripleToModel(dataModel, triple);
			}
			
			/* load minimal ontologies for any undefined properties 
			 * that appear in the output data
			 */
			for (Triple triple: output) {
				if (triple.getPredicate().isURI()) {
					try {
						OwlUtils.getOntPropertyWithLoad(reasoningModel, triple.getPredicate().getURI());
					} catch (SADIException e) {
						log.error(e.getMessage());
					}
				}
			}
		}
		
	}
	
	protected void populateVariableBinding(PotentialValues subjects, PotentialValues predicates, PotentialValues objects) 
	{
		boolean sIsUnboundVar = subjects.isEmpty();
		boolean pIsUnboundVar = predicates.isEmpty();
		boolean oIsUnboundVar = objects.isEmpty();

		// null represents a wildcard in call to reasoningModel.listStatements() below
		Collection<Resource> subjectSet = sIsUnboundVar ? Collections.singleton((Resource) null) : RdfUtils.extractResources(subjects.values);
		Set<? extends Property> predicateSet = pIsUnboundVar ? Collections.singleton((Property) null) : getOntProperties(RdfUtils.extractResources(predicates.values));
		Set<RDFNode> objectSet = oIsUnboundVar ? Collections.singleton((RDFNode) null) : objects.values;

		for (Resource s : subjectSet) {
			for (Property p : predicateSet) {
				for (RDFNode o : objectSet) {
					for (Statement statement : reasoningModel.listStatements(s, p, o).toList()) {
						if (sIsUnboundVar) {
							subjects.add(statement.getSubject());
						}
						if (pIsUnboundVar) {
							predicates.add(statement.getPredicate());
						}
						if (oIsUnboundVar) {
							objects.add(statement.getObject());
						}
					}
				}
			}
		}

		if (sIsUnboundVar) {
			log.trace(String.format("assigned %d bindings to variable %s", subjects.values.size(), subjects.variable));
		}
		if (pIsUnboundVar) {
			log.trace(String.format("assigned %d bindings to variable %s", predicates.values.size(), predicates.variable));
		}
		if (oIsUnboundVar) {
			log.trace(String.format("assigned %d bindings to variable %s", objects.values.size(), objects.variable));
		}
	}


	private Collection<Triple> maybeCallService(Service service, Set<RDFNode> subjects)
	{
		log.trace(String.format("found service %s", service));
		if (deadServices.contains(service.getURI())) {
			log.debug(String.format("skipping dead service %s", service));
			return Collections.emptyList();
		}
		
		log.trace(String.format("filtering inputs previously sent to service %s", service));
		for (Iterator<? extends RDFNode> i = subjects.iterator(); i.hasNext(); ) {
			RDFNode input = i.next();
			if (tracker.beenThere(service, input)) {
				log.trace(String.format("skipping input %s (been there)", input));
				i.remove();
			}
		}
		if (subjects.isEmpty()) {
			log.trace("nothing left to do");
			return Collections.emptyList();
		}
		
		filterByInputClass(subjects, service);
		if (subjects.isEmpty()) {
			log.trace("nothing left to do");
			return Collections.emptyList();
		}
		
		return invokeService(service, subjects);
	}

	/* TODO make this less clunky; probably the service interface should take
	 * the OntProperty instead of just the URI, then it can return services
	 * that match the synonyms as well...
	 */
	private Set<Service> getServicesByPredicate(Collection<OntProperty> predicates)
	{
		Set<OntProperty> equivalentProperties = new HashSet<OntProperty>();
		for(OntProperty predicate : predicates) {
			equivalentProperties.addAll(getEquivalentProperties(predicate));
		}
		
		Set<Service> services = new HashSet<Service>();
		for (OntProperty equivalentProperty: equivalentProperties) {
			log.trace(String.format("finding services for equivalent property %s", equivalentProperty));
			Collection<Service> equivalentPropertyServices = registry.findServicesByPredicate(equivalentProperty.getURI());
			log.debug(String.format("found %d service%s for property %s", equivalentPropertyServices.size(), equivalentPropertyServices.size() == 1 ? "" : "s", equivalentProperty));
			services.addAll(equivalentPropertyServices);
		}
		
		return services;
	}	
	
	private Set<OntProperty> getEquivalentProperties(OntProperty p)
	{
		/* in some reasoners, listEquivalentProperties doesn't include the
		 * property itself; also, some reasoners return an immutable list here,
		 * so we need to create our own copy (incidentally solving an issue
		 * with generics...)
		 */
		log.trace(String.format("finding all properties equivalent to %s", p));
		Set<OntProperty> equivalentProperties = new HashSet<OntProperty>();
		for (OntProperty q: p.listEquivalentProperties().toList()) {
			log.trace(String.format("found equivalent property %s", q));
			equivalentProperties.add(q);
		}
		log.trace(String.format("adding original property %s", p));
		equivalentProperties.add(p);
		
		return equivalentProperties;
	}
	
	/* TODO fix this redundancy by making it so that Services can return 
	 * null for getInputClass (in which case, we'll call isInputInstance
	 * on the collection), or an OntClass (in which case, we'll load it
	 * in our own ontology and check in bulk...)
	 * allow isInputInstance on collection without be cumbersome by
	 * implementing AbstractService, which others can extend to implement
	 * the multiple methods of the interface without all the tedium...
	 */
	private void filterByInputClass(Set<RDFNode> subjects, Service service)
	{
		log.trace(String.format("filtering inputs to service %s by input class", service));
		
		/* keep an unfiltered copy around so we can try using SADI to try and
		 * satisfy the input class description (if so configured...)
		 */
		Set<RDFNode> unfiltered = new HashSet<RDFNode>(subjects.size());
		
		/* attach types to untyped nodes where we can infer the type from the
		 * URI...
		 */
		attachTypes(subjects);

		filterByInputClassInBulk(subjects, service);
		
		/* if so configured, use SADI to attempt to dynamically attach
		 * properties to the failed nodes so that they will pass...
		 */
		OntClass inputClass = null;
		try {
			inputClass = service.getInputClass();
		} catch (SADIException e) {
			log.error(String.format("error loading input class for service %s", service), e);
		}
		if (dynamicInputInstanceClassification && inputClass != null) {
			log.trace(String.format("using SADI to test membership in %s", inputClass));
			
			PotentialValues s = getNewVariableBinding();
			for (Iterator<RDFNode> i = unfiltered.iterator(); i.hasNext(); ) {
				RDFNode node = i.next();
				if (!subjects.contains(node))
					s.add(node);
			}
			Triple typePattern = Triple.create(s.variable, RDF.type.asNode(), inputClass.asNode());
			processPattern(typePattern);
		}
	}

	private void filterByInputClassInBulk(Set<RDFNode> subjects, Service service)
	{
		OntClass inputClass = null;
		try {
			inputClass = service.getInputClass();
		} catch (SADIException e) {
			log.error(String.format("error loading input class for service %s", service), e);
		}
		
		/* I really, really hate this special case coding, but if we want
		 * to be able to use literals as input to the SPARQL services 
		 * (even though the SADI spec explicitly disallows this), we have
		 * to do it this way...
		 */
		if (inputClass == null || inputClass.equals(OWL.Nothing)) {
			filterByInputClassIndividually(subjects, service);
			return;
		}

		log.trace(String.format("finding instances of %s", inputClass));

		/* TODO this will cause a problem if different ontologies accessed in
		 * the same query have conflicting definitions; it might be worth
		 * changing OwlUtils.getOnt(Class|Property)WithLoad to only load the
		 * reachable closure of each requested URI...  Also, we'll need a
		 * really descriptive error message for when this happens...
		 */
		reasoningModel.addSubModel(inputClass.getOntModel());
		OntClass inOurModel = reasoningModel.getOntClass(inputClass.getURI());		

//		Set<? extends OntResource> instances = inputClass.listInstances().toSet();
		Set<String> instanceURIs = new HashSet<String>();
		for (OntResource r: inOurModel.listInstances().toList()) {
			instanceURIs.add(r.getURI());
		}
		for (Iterator<? extends RDFNode> i = subjects.iterator(); i.hasNext(); ) {
			RDFNode node = i.next();
//			if (instances.contains(node.inModel(reasoningModel))) {
			if (node.isResource() && instanceURIs.contains(node.as(Resource.class).getURI())) {
				log.trace(String.format("%s is a valid input to %s", node, service));
			} else {
				log.trace(String.format("%s is an invalid input to %s", node, service));
				i.remove();
			}
		}
		
		reasoningModel.removeSubModel(inputClass.getOntModel());
	}
	
	private void filterByInputClassIndividually(Set<RDFNode> subjects, Service service)
	{
		for (Iterator<? extends RDFNode> i = subjects.iterator(); i.hasNext(); ) {
			RDFNode node = i.next();
			log.trace(String.format("checking if %s is a valid input to %s", node, service));
			if (isValidInput(node, service)) {
				log.trace(String.format("%s is a valid input to %s", node, service));
			} else {
				log.trace(String.format("%s is not a valid input to %s", node, service));
				i.remove();
			}
		}
	}
	
	private boolean isValidInput(RDFNode input, Service service)
	{
		/* I really, really hate this special case coding, but if we want
		 * to be able to use literals as input to the SPARQL services 
		 * (even though the SADI spec explicitly disallows this), we have
		 * to do it this way...
		 */
		if (input.isResource()) {
			return service.isInputInstance(input.as(Resource.class));
		} else if (input.isLiteral() && service instanceof SPARQLServiceWrapper) {
			// a literal is only allowed as input if it is an "inverted" SPARQL service
			return ((SPARQLServiceWrapper)service).mapInputsToObjectPosition();
		} else {
			return false;
		}
	}

	private Collection<Triple> invokeService(Service service, Set<? extends RDFNode> inputs)
	{
		log.info(getServiceCallString(service, inputs));
		try {
			/* see above about special-case coding...
			 */
			if (service instanceof SPARQLServiceWrapper) {
				return ((SPARQLServiceWrapper)service).invokeServiceOnRDFNodes(inputs);
			} else {
				/* generate a list of the OntModel views of each resource
				 * so that the minimal model extractor can used inferred
				 * properties...
				 */
				Collection<Resource> inputResources = new ArrayList<Resource>(inputs.size());
				for (RDFNode input: inputs) {
					/* only resources should be in the collection by now, but
					 * let's be safe...
					 */
					if (!input.isResource())
						continue;
					
					Resource inputResource = input.inModel(reasoningModel).as(Resource.class);
					inputResources.add(inputResource);

					/* explicitly attach the input type of the service, since
					 * we know it's been dynamically classified as such...
					 * TODO this shouldn't be necessary and is probably a bug
					 */
					try {
						inputResource.addProperty(RDF.type, service.getInputClass());
					} catch (SADIException e) {
						log.error(String.format("error loading input class for service %s", service), e);
					}
				}
				return service.invokeService(inputResources);
			}
		} catch (ServiceInvocationException e) {
			log.error(String.format("failed to invoke service %s", service), e);
			
			if (e.isServiceDead()) {
				String serviceURI = service.getURI();
				log.warn(String.format("adding %s to dead services", serviceURI));
				deadServices.add(serviceURI);
			}
			
			return Collections.emptyList();
		}
	}
	
	private String getServiceCallString(Service service, Collection<? extends RDFNode> inputs)
	{
//		return String.format("calling %s %s (%s)", service.getClass().getSimpleName(), service, inputs);
		return String.format("calling service %s (%s)", service.getName(), inputs);
	}

	/* I'm not 100% sure we don't have to care exactly how we encounter a
	 * particular query pattern, but I suspect worrying about it now would
	 * fall under the heading of premature optimization...
	 */
	private static class QueryPatternEnumerator extends ElementVisitorBase
	{
		List<Triple> queryPatterns;
		
		public QueryPatternEnumerator(Query query)
		{
			queryPatterns = new ArrayList<Triple>();
			ElementWalker.walk(query.getQueryPattern(), this);
		}
		
		public List<Triple> getQueryPatterns()
		{
			return queryPatterns;
		}
		
		public void visit(ElementTriplesBlock el)
		{
			for (Iterator<Triple> i = el.patternElts(); i.hasNext(); ) {
				queryPatterns.add((Triple)i.next());
			}
		}
		
		/* 
		 * If the ARQ extensions to SPARQL are turned on (i.e. if the variable "share.sparql.useARQSyntax" 
		 * not set to false in the share.properties file), then a basic triple pattern consists of list of
		 * "TriplePaths" rather than Triples.  TriplePaths are like Triples, but they allow XPath-like 
		 * expressions in the predicate position, as described at http://jena.sourceforge.net/ARQ/property_paths.html.
		 * -- BV
		 */
		public void visit(ElementPathBlock el) {
			for (TriplePath triplePath : el.getPattern()) {
				if(triplePath.asTriple() == null) {
					log.error(String.format("this version of SHARE does not support ARQ TriplePaths, ignoring TriplePath: %s", triplePath));
					continue;
				} 
				queryPatterns.add(triplePath.asTriple());
			}
		}
	}
	
	private static class PotentialValues
	{
		Node variable;
		Set<RDFNode> values;
		String key;
		
		public PotentialValues(Node variable)
		{
			this.variable = variable;
			values = new HashSet<RDFNode>();
			key = "?" + variable.getName();
		}
		
		public PotentialValues(RDFNode value)
		{
			this.variable = null;
			values = new HashSet<RDFNode>();
			values.add(value);
			key = value.toString();
		}
		
		public boolean isVariable()
		{
			return variable != null;
		}
		
		public boolean isEmpty()
		{
			return values.isEmpty();
		}
		
		public void add(RDFNode node)
		{
			log.trace(String.format("adding %s to variable %s", node, variable));
			values.add(node);
		}
		
		public String toString()
		{
			if (isVariable()) {
				return String.format("?%s %s", variable.getName(), values);
			} else {
				return values.toString();
			}
		}
	}
	
	private static class Tracker
	{
		private Set<String> visited;
		
		public Tracker()
		{
			visited = new HashSet<String>();
		}
		
		public synchronized boolean beenThere(Service service, RDFNode input)
		{
			String key = getHashKey(service, input);
			if (visited.contains(key)) {
				return true;
			} else {
				visited.add(key);
				return false;
			}
		}
		
		public synchronized boolean beenThere(PotentialValues instances, OntClass asClass)
		{
			String key = getHashKey(instances, asClass);
			if (visited.contains(key)) {
				return true;
			} else {
				visited.add(key);
				return false;
			}
		}
		
		private String getHashKey(Service service, RDFNode input)
		{
			StringBuilder builder = new StringBuilder();
			
			builder.append(service.getURI());
			
			// more special casing: must differentiate between
			// inverted and non-inverted sparql services
			if(service instanceof SPARQLServiceWrapper) {
				builder.append("(inverted=");
				builder.append(String.valueOf(((SPARQLServiceWrapper)service).mapInputsToObjectPosition()));
				builder.append(")");
			}
			
			builder.append(input.toString());

			return builder.toString();
		}

		private String getHashKey(PotentialValues instances, OntClass asClass)
		{
			// one variable name or URI, one URI, so this should be safe...
			return String.format("%s %s", instances.key, asClass.getURI());
		}
	}
	
	/* rankings for expensive query patterns */
	protected static int BAD = -1;
	protected static int WORST = -2;

	protected class QueryPatternComparator implements Comparator<Triple>
	{
		public int compare(Triple pattern1, Triple pattern2) 
		{
			int cost1 = cost(pattern1);
			int cost2 = cost(pattern2);
			
			/* sanity check */
			if (cost1 == 0 || cost2 == 0) {
				throw new RuntimeException("query pattern cost should never be zero");
			}
			
			/* Note: the first case handles when both numbers are both BAD, both WORST, etc. */ 
			if (cost1 == cost2) {
				return 0;
			} else if(cost1 > 0 && cost2 < 0) {
				return -1;
			} else if(cost1 < 0 && cost2 > 0) {
				return 1;
			} else {
				/* CASE: both costs have the same sign */
				if (Math.abs(cost1) < Math.abs(cost2)) {
					return -1;
				} else {
					return 1;
				}
			}
		}
		
		protected int cost(Triple pattern) 
		{
			PotentialValues s = expandQueryNode(pattern.getSubject());
			PotentialValues p = expandQueryNode(pattern.getPredicate());
			PotentialValues o = expandQueryNode(pattern.getObject());
			
			if (s.isEmpty() && o.isEmpty()) {
				// CASES: (?s, ?p, ?o) or (?s, bound, ?o)
				return WORST;
			} else if (p.isEmpty()) {
				// CASES: (bound, ?p, ?o) or (?s, ?p, bound)				
				return BAD;
			} else if (o.isEmpty()) {
				// CASE: (bound, bound, ?o)
				return s.values.size() * p.values.size();
			} else {
				// CASE: (?s, bound, bound)
				return o.values.size() * p.values.size();
			}
		}
	}
}
