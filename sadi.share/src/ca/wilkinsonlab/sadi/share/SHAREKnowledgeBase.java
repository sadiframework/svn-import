package ca.wilkinsonlab.sadi.share;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceInvocationException;
import ca.wilkinsonlab.sadi.sparql.SPARQLServiceWrapper;
import ca.wilkinsonlab.sadi.utils.HttpUtils;
import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.ResourceTyper;
import ca.wilkinsonlab.sadi.utils.OwlUtils.PropertyRestrictionVisitor;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.syntax.ElementAssign;
import com.hp.hpl.jena.sparql.syntax.ElementDataset;
import com.hp.hpl.jena.sparql.syntax.ElementFetch;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementNamedGraph;
import com.hp.hpl.jena.sparql.syntax.ElementOptional;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;
import com.hp.hpl.jena.sparql.syntax.ElementService;
import com.hp.hpl.jena.sparql.syntax.ElementSubQuery;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;
import com.hp.hpl.jena.sparql.syntax.ElementUnion;
import com.hp.hpl.jena.sparql.syntax.ElementUnsaid;
import com.hp.hpl.jena.sparql.syntax.ElementVisitor;
import com.hp.hpl.jena.sparql.syntax.ElementWalker;
import com.hp.hpl.jena.vocabulary.RDF;

public class SHAREKnowledgeBase
{
	private static final Log log = LogFactory.getLog( SHAREKnowledgeBase.class );
	
	private OntModel reasoningModel;
	private Model dataModel;
	
	private Map<Node, Set<RDFNode>> variableBindings;
	
	private StopWatch inputClassificationWatch;
	
	private Tracker tracker;
	private Set<String> deadServices;

	// TODO rename to something less unwieldy?
	private boolean dynamicInputInstanceClassification;
	
	public SHAREKnowledgeBase()
	{
		this(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF));
	}
	
	public SHAREKnowledgeBase(OntModel reasoningModel)
	{
		this(reasoningModel, ModelFactory.createDefaultModel());
	}
	
	public SHAREKnowledgeBase(OntModel reasoningModel, Model dataModel)
	{
		log.debug("new ca.wilkinsonlab.sadi.share.DynamicKnowledgeBase instantiated");
		
		this.reasoningModel = reasoningModel;
		this.dataModel = dataModel;
		if (!reasoningModel.listSubModels().toSet().contains(dataModel)) {
			log.debug("Adding data-only model as a sub-model of reasoning model");
			reasoningModel.addSubModel(dataModel);
		}
		
		variableBindings = new HashMap<Node, Set<RDFNode>>();
		
		inputClassificationWatch = new StopWatch();
		
		tracker = new Tracker();
		
		Configuration config = ca.wilkinsonlab.sadi.share.Config.getConfiguration();
		deadServices = Collections.synchronizedSet(new HashSet<String>());
		for (Object serviceUri: config.getList("share.deadService"))
			deadServices.add((String)serviceUri);
		
		dynamicInputInstanceClassification = config.getBoolean("share.dynamicInputInstanceClassification", false);
//		skipPropertiesPresentInKB = config.getBoolean("share.skipPropertiesPresentInKB", false);
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
		executeQuery(QueryFactory.create(query));
	}
	
	public void executeQuery(String query, QueryPatternOrderingStrategy strategy)
	{
		executeQuery(QueryFactory.create(query), strategy);
	}
	
	public void executeQuery(Query query)
	{
		executeQuery(query, new DefaultQueryPatternOrderingStrategy());
	}
	
	public void executeQuery(Query query, QueryPatternOrderingStrategy strategy)
	{
		// this is really stupid; is this really the way it has to be done?
		inputClassificationWatch.reset();
		inputClassificationWatch.start();
		inputClassificationWatch.suspend();
		
		/* load all of the graphs referenced in the FROM clause into the kb
		 * TODO might have to make this configurable or turn it off, if people
		 * are using the FROM clause in other ways...
		 */
		for (String sourceURI: query.getGraphURIs()) {
			reasoningModel.read( sourceURI );
		}
		
		try {
			for (Triple pattern: strategy.orderPatterns(new QueryPatternEnumerator(query).getQueryPatterns())) {
				if (!pattern.getPredicate().isURI()) {
					log.warn(String.format("skipping non-URI predicate %s", pattern.getPredicate()));
					continue;
				}
				OntProperty p = getOntProperty(pattern.getPredicate().getURI());
				
				Set<RDFNode> subjects = expandQueryNode(pattern.getSubject());
				Set<RDFNode> objects = expandQueryNode(pattern.getObject());
				
				if (p.equals(RDF.type)) {
					if (!pattern.getObject().isURI()) {
						log.warn(String.format("object of rdf:type is not a URI"));
						continue;
					}
					OntClass c = getOntClass(pattern.getObject().getURI());
					
					if (subjects == null) {
						gatherTriplesByClass(subjects, c, getVariableBinding(pattern.getSubject()));
					} else {
						gatherTriplesByClass(subjects, c, null);
					}
				} else if (subjects != null) { // bound subject...
					if (objects != null) { // bound subject and object...
						/* now we have a choice; this is where Ben's optimizer
						 * will plug in somehow to figure out which way to go,
						 * but for now just go in the forward direction...
						 */
						gatherTriplesByPredicate(subjects, p, null);
					} else { // bound subject, unbound object...
						gatherTriplesByPredicate(subjects, p, getVariableBinding(pattern.getObject()));
					}
				} else if (objects != null) { // unbound subject, bound object...
					OntProperty inverse = getInverseProperty(p);
					gatherTriplesByPredicate(objects, inverse, getVariableBinding(pattern.getSubject()));
				} else { // unbound subject, unbound object...
					log.warn(String.format("encountered a pattern whose subject and object are both unbound variables %s", pattern));
					continue;
				}
			}
		} catch (UnresolvableQueryException e) {
			log.error(String.format("failed to order query %s with strategy %s", query, strategy), e);
		}
		
		inputClassificationWatch.stop();
		log.info(String.format("spent %s on input classification", DurationFormatUtils.formatDurationHMS(inputClassificationWatch.getTime())));
	}

	private Set<RDFNode> getVariableBinding(Node variable)
	{
		if (!variableBindings.containsKey(variable)) {
			variableBindings.put(variable, new HashSet<RDFNode>());
		}
		return variableBindings.get(variable);
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
		OntProperty p = OwlUtils.getOntPropertyWithLoad(reasoningModel, uri);
		if (p != null)
			return p;
		
		log.warn(String.format("creating undefined and unresolvable property %s", uri));
		return reasoningModel.createOntProperty(uri);
	}
	
	/* look up the specified URI in the reasoning model; if it's not there,
	 * try to resolve the URI into the model; if that doesn't work, just
	 * create it.
	 */
	private OntClass getOntClass(String uri)
	{
		OntClass c = OwlUtils.getOntClassWithLoad(reasoningModel, uri);
		if (c != null)
			return c;
		
		log.warn(String.format("creating undefined and unresolvable property %s", uri));
		return reasoningModel.createClass(uri);
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
	
	/* return a list of potential values this query node represents; this will
	 * be null if the node is an unbound variable.
	 */
	private Set<RDFNode> expandQueryNode(Node node)
	{
		if (node.isVariable()) {
			return variableBindings.get(node);
		} else if (node.isURI()) {
			RDFNode resource = getTypedResource(node.getURI());
			return Collections.singleton(resource);
		} else if (node.isLiteral()) {
			RDFNode literal = getTypedLiteral(node);
			return Collections.singleton(literal);
		} else {
			log.warn(String.format("unknown node %s", node));
			return Collections.emptySet();
		}
	}
	
	/* returns a resource in the data model with the specified URI, creating
	 * one if necessary; if the resource is untyped, attempt to attach a type
	 * based on the URI.
	 */
	private Resource getTypedResource(String uri)
	{
		Resource resource = dataModel.createResource(uri);
		if ( !RdfUtils.isTyped(resource) ) {
			ResourceTyper.getResourceTyper().attachType(resource);
		}
		return resource;
	}

	private Literal getTypedLiteral(Node node)
	{
		return dataModel.createTypedLiteral(node.getLiteralValue(), node.getLiteralDatatype());
	}
	
	/* gather triples matching a class, optionally storing subjects that are
	 * class instances in an accumulator.
	 */
	private void gatherTriplesByClass(final Set<? extends RDFNode> subjects, final OntClass c, final Set<RDFNode> variableBinding)
	{
		log.debug(String.format("gathering triples to find instances of %s", c));
		
		OwlUtils.decompose(c, new PropertyRestrictionVisitor() {
			public void onProperty(OntProperty onProperty)
			{
				gatherTriplesByPredicate(subjects, onProperty, variableBinding);
			}
			public void hasValue(OntProperty onProperty, RDFNode hasValue)
			{
				if (subjects != null) {
					gatherTriplesByPredicate(subjects, onProperty, variableBinding);
				} else {
					if (!hasValue.isURIResource()) {
						log.warn(String.format("unable to process property restriction on %s in %s", onProperty, c));
						return;
					}
					Resource object = getTypedResource(hasValue.toString());
					Set<Resource> objects = Collections.singleton(object);
					OntProperty inverse = getInverseProperty(onProperty);
					gatherTriplesByPredicate(objects, inverse, variableBinding);
				}
			}
			public void valuesFrom(OntProperty onProperty, OntResource valuesFrom)
			{
				gatherTriplesByPredicate(subjects, onProperty, variableBinding);
				if (valuesFrom.isClass()) {
					Set<Resource> nextSubjects = new HashSet<Resource>();
					for (RDFNode node: subjects) {
						if (!node.isResource())
							continue;
						Resource subject = (Resource)node.as(Resource.class);
						for (StmtIterator statements = subject.listProperties(onProperty); statements.hasNext(); ) {
							Statement statement = statements.nextStatement();
							if (statement.getObject().isResource()) {
								nextSubjects.add(statement.getResource());
							}
						}
					}
					// TODO do we need to add these to a variable binding?
					gatherTriplesByClass(nextSubjects, valuesFrom.asClass(), null);
				}
			}
		});
	}
	
	/* gather triples matching a predicate, optionally storing the subjects in an accumulator.
	 */
	private void gatherTriplesByPredicate(Set<? extends RDFNode> subjects, OntProperty predicate, Set<RDFNode> variableBinding)
	{
		log.debug(String.format("gathering triples with predicate %s", predicate));
		log.trace(String.format("potential subjects %s", subjects));
		if (subjects == null) {
			log.warn("attempt to call gatherTriplesByPredicate with null subject");
			return;
		} else if (subjects.isEmpty()) {
			log.trace("nothing to do");
			return;
		}
		
		Set<Service> services = getServicesByPredicate(predicate);
		log.debug(String.format("found %d service%s", services.size(), services.size() == 1 ? "" : "s"));
		for (Service service : services) {
			Collection<Triple> output = maybeCallService(service, subjects);
			for (Triple triple: output) {
				log.trace(String.format("adding triple to data model %s", triple));
				RdfUtils.addTripleToModel(dataModel, triple);
				
				if (triple.getPredicate().getURI().equals(RDF.type.getURI()))
					continue; // performance-enhancing shortcut...
				
				/* this has the important side-effect of adding a type to
				 * untyped objects; probably we should move this somewhere
				 * more explicit... (probably only attach the type when the
				 * resource becomes the potential subject of a triple...)
				 */ 
				RDFNode o = null;
				if (triple.getObject().isURI()) {
					o = getTypedResource(triple.getObject().getURI());
				} else if (triple.getObject().isLiteral()) {
					o = getTypedLiteral(triple.getObject());
				}
				
				/* this will load any properties not-yet defined, but I
				 * think that's a good thing; we're likely to need them
				 * eventually...
				 */
				OntProperty p = getOntProperty(triple.getPredicate().getURI());
				OntProperty inverse = getInverseProperty(predicate);
				
				try {
					Resource s = dataModel.getRDFNode(triple.getSubject()).as(Resource.class);
					if ( variableBinding != null ) {
						if ( ( predicate.equals(p) || predicate.hasEquivalentProperty(p) ) && subjects.contains(s) ) {
							log.trace(String.format("adding object %s to variable binding", o));
							variableBinding.add(o);
						} else if ( inverse.equals(p) || inverse.hasEquivalentProperty(p) && subjects.contains(o) ) {
							log.trace(String.format("adding subject %s to variable binding", s));
							variableBinding.add(s);
						}
					}
				} catch (Exception e) {
					log.error("error adding variable binding", e);
				}
			}
		}
	}
	
	private Collection<Triple> maybeCallService(Service service, Set<? extends RDFNode> subjects)
	{
		log.trace(String.format("found service %s", service));
		if (deadServices.contains(service.getServiceURI())) {
			log.debug(String.format("skipping dead service %s", service));
			return Collections.emptyList();
		}
		
		log.trace(String.format("filtering duplicate service calls"));
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
		
		log.trace(String.format("filtering potential subjects for service %s", service));
		Set<RDFNode> subjectsMatchingInputClass = filterByInputClass(subjects, service);
		if (subjectsMatchingInputClass.isEmpty()) {
			log.trace("nothing left to do");
			return Collections.emptyList();
		}
		
		return invokeService(service, subjectsMatchingInputClass);
	}

	/* TODO make this less clunky; probably the service interface should take
	 * the OntProperty instead of just the URI, then it can return services
	 * that match the synonyms as well...
	 */
	private Set<Service> getServicesByPredicate(OntProperty p)
	{
		Set<Service> services = new HashSet<Service>(Config.getMasterRegistry().findServicesByPredicate(p.getURI()));
		
		// listEquivalentProperties doesn't include the property itself
		for (Iterator<? extends OntProperty> i = p.listEquivalentProperties(); i.hasNext(); ) {
			OntProperty equivalentProperty = i.next();
			log.trace(String.format("found equivalent property %s", equivalentProperty));
			services.addAll(Config.getMasterRegistry().findServicesByPredicate(equivalentProperty.getURI()));
		}
		
		/* FIXME pass around the predicate object so that the
		 * registries can check its inverse explicitly; for now
		 * just call using Ben's old syntax so that the SPARQL
		 * services are found...
		 */
		services.addAll(Config.getMasterRegistry().findServicesByPredicate(String.format("inv(%s)", getInverseProperty(p).getURI())));
		
		return services;
	}
	
	private Set<RDFNode> filterByInputClass(Set<? extends RDFNode> subjects, Service service)
	{
		inputClassificationWatch.resume();
//		Set<RDFNode> passed = filterByInputClassIndividually(subjects, service);
		Set<RDFNode> passed = filterByInputClassInBulk(subjects, service);
		Set<Resource> failed = new HashSet<Resource>(subjects.size());
		for (RDFNode subject: subjects) {
			if (!passed.contains(subject) && subject.isResource())
				failed.add(subject.as(Resource.class));
		}
		
		/* if so configured, use SADI to attempt to dynamically attach
		 * properties to the failed nodes so that they will pass...
		 */
		if (dynamicInputInstanceClassification && service.getInputClass() != null) {
			log.trace(String.format("Using SADI to test membership in %s", service.getInputClass()));
			gatherTriplesByClass(failed, service.getInputClass(), null);
			for (Resource node: failed) {
				if (service.isInputInstance(node))
					passed.add(node);
			}
		}
		inputClassificationWatch.suspend();
		return passed;
	}
	
	private Set<RDFNode> filterByInputClassInBulk(Set<? extends RDFNode> subjects, Service service)
	{
		// TODO SPARQLServiceWrapper doesn't implement discoverInputInstances
		if (service instanceof SPARQLServiceWrapper)
			return filterByInputClassIndividually(subjects, service);
		
		if (subjects.isEmpty())
			return Collections.emptySet();

		log.trace(String.format("filtering invalid inputs to %s in bulk", service));
		Set<RDFNode> passed = new HashSet<RDFNode>(subjects.size());
		for (Resource r: service.discoverInputInstances(dataModel)) {
			if (subjects.contains(r))
				passed.add(r);
		}
		return passed;
	}
	
	private Set<RDFNode> filterByInputClassIndividually(Set<? extends RDFNode> subjects, Service service)
	{
		Set<RDFNode> passed = new HashSet<RDFNode>(subjects.size());
		for (RDFNode node: subjects) {
			log.trace(String.format("Checking if %s is a valid input to %s", node, service));
			
			/* I really, really hate this special case coding, but if we want
			 * to be able to use literals as input to the SPARQL services 
			 * (even though the SADI spec explicitly disallows this), we have
			 * to do it this way...
			 */
			if (node.isResource()) {
				Resource r = (Resource)node.as(Resource.class);
				if (service.isInputInstance(r)) {
					passed.add(r);
				}
			} else if (node.isLiteral() && service instanceof SPARQLServiceWrapper) {
				passed.add(node);
			}
		}
		return passed;
	}

	@SuppressWarnings("unchecked")
	private Collection<Triple> invokeService(Service service, Set<? extends RDFNode> inputs)
	{
		log.info(getServiceCallString(service, inputs));
		try {
			/* see above about special-case coding...
			 */
			if (service instanceof SPARQLServiceWrapper) {
				return ((SPARQLServiceWrapper)service).invokeServiceOnRDFNodes(inputs);
			} else {
				/* I know this generates a type warning, but I guarantee that
				 * only Resources are in this collection at this point...
				 */
				return service.invokeService((Collection<Resource>)inputs);
			}
		} catch (ServiceInvocationException e) {
			log.error(String.format("failed to invoke service %s", service), e);
			
			/* TODO there are probably other cases where we want to mark a
			 * service as dead...
			 */
			if (HttpUtils.isHttpError(e))
				deadServices.add(service.getServiceURI());

			return Collections.emptyList();
		}
	}
	
	private String getServiceCallString(Service service, Collection<? extends RDFNode> inputs)
	{
		return String.format("calling %s %s (%s)", service.getClass().getSimpleName(), service, inputs);
	}

	/* I'm not 100% sure we don't have to care exactly how we encounter a
	 * particular query pattern, but I suspect worrying about it now would
	 * fall under the heading of premature optimization...
	 */
	private static class QueryPatternEnumerator implements ElementVisitor
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

		public void visit(ElementFilter el)
		{
		}

		public void visit(ElementUnion el)
		{
		}

		public void visit(ElementOptional el)
		{
		}

		public void visit(ElementGroup el)
		{
		}

		public void visit(ElementDataset el)
		{
		}

		public void visit(ElementNamedGraph el)
		{	
		}

		public void visit(ElementUnsaid el)
		{	
		}

		public void visit(ElementService el)
		{
		}

		public void visit(ElementPathBlock el)
		{
		}

		public void visit(ElementAssign el)
		{
		}

		public void visit(ElementFetch el)
		{
		}

		public void visit(ElementSubQuery el)
		{
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
		
		private String getHashKey(Service service, RDFNode input)
		{
			// two URIS, or one URI and one literal, so this should be safe...
			return String.format("%s %s", service.getServiceURI(), input.toString());
		}
	}
}
