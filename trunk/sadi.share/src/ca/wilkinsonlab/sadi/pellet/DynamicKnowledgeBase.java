package ca.wilkinsonlab.sadi.pellet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.Individual;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.datatypes.Datatype;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;
import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.rdf.RdfService;
import ca.wilkinsonlab.sadi.sparql.SPARQLService;
import ca.wilkinsonlab.sadi.sparql.SPARQLServiceWrapper;
import ca.wilkinsonlab.sadi.utils.HttpUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.ResourceTyper;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class DynamicKnowledgeBase extends KnowledgeBase
{
	public final static Log log = LogFactory.getLog(DynamicKnowledgeBase.class);
	
	Model model;

	Tracker tracker;
	Set<String> deadServices;
	
	boolean skipPropertiesPresentInKB;
	
	public DynamicKnowledgeBase() 
	{
		log.debug("new ca.wilkinsonlab.sadi.pellet.DynamicKnowledgeBase instantiated");
		
		tracker = new Tracker();
		
		Configuration config = ca.wilkinsonlab.sadi.share.Config.getConfiguration();
		
		deadServices = Collections.synchronizedSet(new HashSet<String>());
		for (Object serviceUri: config.getList("share.deadService"))
			deadServices.add((String)serviceUri);
		
		skipPropertiesPresentInKB = config.getBoolean("share.skipPropertiesPresentInKB", false);
		
		/* TODO figure out where the KB is deciding to iterate over all individuals
		 * and stop it from doing so, since that only happens to us if the query is
		 * unsolvable...
		 */
	}
	
	/**
	 * Set the link back to the Jena view of the knowledge base.
	 * This is useful to minimize the amount of conversion done when
	 * communicating with the SADI client.
	 * @param model the Jena view of the knowledge base
	 */
	public void setModel(Model model)
	{
		this.model = model;
	}
	
//	@Override
//	public boolean isProperty(ATerm p) {
//		if (!super.isProperty(p)) {
//			if (p.toString().endsWith("name"))
//				this.addDatatypeProperty(p);
//			else
//				this.addObjectProperty(p);
//		}
//		return true;
//	}
	
	@Override
	public boolean isType(ATermAppl x, ATermAppl c) {
		log.trace(String.format("isType(%s, %s)", x, c));
		
		/* if we already know this individual has this class, we never
		 * want to do anything dynamic...
		 */
		if (super.isType(x, c))
			return true;
		
		/* TODO try to unify this code with that from getInstances() below...
		 * also need to account for max cardinality, all values from, and probably others...
		 */
		for (ATermAppl term: tbox.getAxioms(c)) {
			// TODO need to do this for subclasses as well; probably others...
			if (term.getAFun().getName().equals("equivalentClasses") && term.getArgument(0).equals(c)) {
				ATermAppl equivalent = (ATermAppl)term.getArgument(1);
				log.trace( "found equivalent class " + equivalent );
				String name = equivalent.getAFun().getName();
				if (name.equals("some")) {
					ATermAppl property = (ATermAppl)equivalent.getArgument(0);
					ATermAppl value = (ATermAppl)equivalent.getArgument(1);
					if (value.getAFun().getName().equals("value")) {
						value = (ATermAppl)value.getArgument(0);
						getPropertyValues(x, property);
					} else {
						for (ATermAppl object: getPropertyValues(x, property))
							isType(object, value);
					}
				} else if (name.equals("min")) {
					ATermAppl property = (ATermAppl)equivalent.getArgument(0);
					gatherTriples(property);
				}
			}
		}

		return super.isType(x, c);
	}
	
	@Override
	public Set<ATermAppl> getInstances(ATermAppl c) {
		log.trace(String.format("getInstances(%s)", c));
		
		if (skipPropertiesPresentInKB) {
			Set<ATermAppl> predynamicResult = super.getInstances(c);
			if (!predynamicResult.isEmpty())
				return predynamicResult;
		}
		
		if (c.getAFun().getName().equals("not"))
			return super.getInstances(c);
		
		log.info(String.format("looking for dynamic instances of %s", c));
		for (ATermAppl term: tbox.getAxioms(c)) {
			if (term.getAFun().getName().equals("equivalentClasses") && term.getArgument(0).equals(c)) {
				ATermAppl equivalent = (ATermAppl)term.getArgument(1);
				log.trace( "found equivalent class " + equivalent );
				String name = equivalent.getAFun().getName();
				if (name.equals("some")) {
					ATermAppl property = (ATermAppl)equivalent.getArgument(0);
					ATermAppl value = (ATermAppl)equivalent.getArgument(1);
					if (value.getAFun().getName().equals("value")) {
						value = (ATermAppl)value.getArgument(0);
						getIndividualsWithProperty(property, value);
					} else {
						getInstances(value);
					}
				} else if (name.equals("min")) {
					ATermAppl property = (ATermAppl)equivalent.getArgument(0);
					gatherTriples(property);
				}
			}
		}
		
		return super.getInstances(c);
	}

	@Override
	public boolean hasPropertyValue(ATermAppl s, ATermAppl p, ATermAppl o)
	{
		log.trace(String.format("hasPropertyValue(%s, %s, %s)", s, p, o));
		
		/* it's possible this individual doesn't exist in the KB yet, so we
		 * need to add it before calling this method on the superclass...
		 */
		if (!nodeExists(s))
			addIndividual(s);
		
		if (skipPropertiesPresentInKB) {
			if (super.hasPropertyValue(s, p, o))
				return true;
		}
		
		gatherTriples(s, p);
		return super.hasPropertyValue(s, p, o);
	}


	@Override
	public List getIndividualsWithProperty(ATermAppl r, ATermAppl x)
	{
		// r is predicate, x is object
		log.trace(String.format("getIndividualsWithProperty(%s, %s)", r, x));
		
		/* it's possible this individual doesn't exist in the KB yet, so we
		 * need to add it before calling this method on the superclass...
		 */
		if (isObjectProperty(r) && !nodeExists(x))
			addIndividual(x);
		
		if (skipPropertiesPresentInKB) {
			List predynamicResult = super.getIndividualsWithProperty(r, x);
		 	if (!predynamicResult.isEmpty())
		 		return predynamicResult;
		}
		
		// if r is an object property, super.getIndividualsWithProperty() will invert and call getObjectPropertyValues() -- BV
		if (isDatatypeProperty(r))
			gatherTriples(x, ATermUtils.makeInv(r));

		return super.getIndividualsWithProperty(r, x);
	}

	
	@Override
	public List<ATermAppl> getObjectPropertyValues(ATermAppl r, ATermAppl x)
	{
		// x is subject, r is predicate
		log.trace( "getObjectPropertyValues( " + r + ", " + x + " )" );
		
		/* it's possible this individual doesn't exist in the KB yet, so we
		 * need to add it before calling this method on the superclass...
		 */
		if (!nodeExists(x))
			addIndividual(x);
		
		if (skipPropertiesPresentInKB) {
			List<ATermAppl> predynamicResult = super.getObjectPropertyValues(r, x);
			if (!predynamicResult.isEmpty())
				return predynamicResult;
		}
		
		gatherTriples(x, r);
		return super.getObjectPropertyValues(r, x);
	}
	
	@Override
	public List<ATermAppl> getDataPropertyValues(ATermAppl r, ATermAppl x, Datatype datatype)
	{
		// x is subject, r is predicate
		log.trace( "getDataPropertyValues( " + r + ", " + x + ", " + datatype + " )" );
		
		/* it's possible this individual doesn't exist in the KB yet, so we
		 * need to add it before calling this method on the superclass...
		 */
		if (!nodeExists(x))
			addIndividual(x);
		
		if (skipPropertiesPresentInKB) {
			List<ATermAppl> predynamicResult = super.getDataPropertyValues(r, x, datatype);
			if (!predynamicResult.isEmpty())
				return predynamicResult;
		}
		
		gatherTriples(x, r);
		return super.getDataPropertyValues(r, x, datatype);
	}

	private void gatherTriples(ATermAppl subject, ATermAppl predicate)
	{
		String p = predicate.toString();

//		if (p.equals(RDFS.label.getURI()) || p.equals(RDFS.comment.getURI()))
//			return;
		
		/* If the predicate is of the form "inv(URI)", try to translate
		 * this to an actual inverse predicate.  It's okay if there
		 * is no such predicate though, because some services (i.e. SPARQL
		 * endpoints) can resolve "inv(URI)" as is.  
		 */
		boolean failedToFindInverse = false;
		if(ATermUtils.isInv(predicate)) {
			String inverse = translateInverseToPredicate(predicate);
			if(inverse != null)
				p = inverse;
			else
				failedToFindInverse = true; 
		}

		Collection<String> predicateSynonyms;
		
		if(failedToFindInverse) {
			/* We are about to retrieve/invoke services for "inv(predicate)".
			 * We also want to find all services for "inv(synonym)", where synonym is 
			 * an equivalent property of predicate.
			 */
			predicateSynonyms = new ArrayList<String>();
			// NOTE: makeInv applied applied to "inv(pred)" yields "pred"
			ATermAppl barePredicate = ATermUtils.makeInv(predicate);
			Collection<String> inverseSynonyms = getPredicateSynonyms(barePredicate.toString());
			for(String inverseSynonym : inverseSynonyms) {
				ATermAppl inv = ATermUtils.makeTermAppl(inverseSynonym);
				predicateSynonyms.add(ATermUtils.makeInv(inv).toString());
			}
		}
		else {
			predicateSynonyms = getPredicateSynonyms(p);
		}

		Collection<Triple> triples = new ArrayList<Triple>();
		for (String predicateSynonym: predicateSynonyms)
			gatherTriples(subject, predicateSynonym, triples);
		
		addTriplesToKB(triples, true);
	}
	
	private void gatherTriples(ATermAppl predicate)
	{
		log.info(String.format("gathering triples matching ? <%s> ?", predicate));
		
		/* TODO might this be mangled in some way by the ATerm library?
		 */
		String p = predicate.toString();
		
		/* find services that can attach this property and
		 * look for individuals in the model that we can call
		 * that service on...
		 */
		Collection<Triple> triples = new ArrayList<Triple>();
		for (Service service : Config.getMasterRegistry().findServicesByPredicate(p)) {
			log.trace(String.format("found service %s", service));
			
			OntClass inputClass = getInputClass(service);
			if (inputClass != null) {
				for (ATermAppl input: getInstances(makeATermAppl(inputClass.asNode()))) {
					/* at this point, we know the input instance is of the
					 * appropriate type, so we can add it explicitly...
					 */
					String subject = input.toString();
					Resource subjectAsResource = model.createResource(subject);
					subjectAsResource.addProperty(RDF.type, inputClass);
					invokeService(service, subjectAsResource, p, triples);
				}
			}
		}
		addTriplesToKB(triples, true);
	}
	
	/* TODO unhack this by making getInputClass() part of the general
	 * service interface...
	 */
	private OntClass getInputClass(Service service)
	{
		if (service instanceof RdfService)
			return ((RdfService)service).getInputClass();
		else
			return null;
	}
	
	private void gatherTriples(ATermAppl subject, String predicate, Collection<Triple> accum)
	{
		log.info(String.format("gathering triples matching <%s> <%s> ?", subject, predicate));

		String s = subject.toString();
		
		/* this is a hack to support unknown URIs in the SPARQL client...
		 * this is also probably broken now that I've changed the way services
		 * are discovered...
		 */
		Resource subjectAsResource = model.createResource(s);
		if ( !RdfUtils.isTyped(subjectAsResource) )
			attachType(subjectAsResource);
		
		for (Service service : Config.getMasterRegistry().findServicesByPredicate(predicate)) {
			OntClass inputClass = getInputClass(service);
			if (inputClass != null) {
				/* first, ask the service if this input has the appropriate
				 * input type; this wouldn't be necessary if we loaded the
				 * service's input class definition into our KB, but that
				 * means we'll suddenly care about ontological inconsistency
				 * between service definitions...
				 * if that fails, check our own (dynamic) KB to see if we can
				 * add properties to make the subject a valid input to this
				 * service...
				 */
				if (service.isInputInstance(subjectAsResource)) {
					invokeService(service, subjectAsResource, predicate, accum);
				} else if (isType(subject, makeATermAppl(inputClass.asNode()))) {
					/* at this point, we know the input instance is of the
					 * appropriate type, so we can add it explicitly...
					 */
					subjectAsResource.addProperty(RDF.type, inputClass);
					invokeService(service, subjectAsResource, predicate, accum);
				}
			} else {
				log.debug(String.format("no input class for service %s; calling without checking type of subject %s", service, subject));
				invokeService(service, subjectAsResource, predicate, accum);
			}
		}
	}
	
	private void invokeService(Service service, Resource subject, String predicate, Collection<Triple> accum)
	{
		log.trace(String.format("found service %s", service));
		if (deadServices.contains(service.getServiceURI()))
			return;
		
		/* TODO fix SPARQL registry so that it returns properly proxied
		 * services so that this isn't necessary...
		 * At the moment, we must invoke SPARQL endpoints with both subject and predicate,
		 * so we can't use the tracker. --BV
		 */
		if (service instanceof SPARQLService || service instanceof SPARQLServiceWrapper) {
			try {
				log.info(getServiceCallString(service, subject));
				accum.addAll(service.invokeService(subject, predicate));
			} catch (Exception e) {
				log.error(String.format("failed to invoke service %s", service), e);
				if (HttpUtils.isHttpError(e))
					deadServices.add(service.getServiceURI());
			}
		} else {
			if (tracker.beenThere(service, subject.getURI()))
				return;
			
			try {
				log.info(getServiceCallString(service, subject));
				accum.addAll(service.invokeService(subject));
			} catch (Exception e) {
				log.error(String.format("failed to invoke service %s", service), e);
				if (HttpUtils.isHttpError(e))
					deadServices.add(service.getServiceURI());
			}
		}
	}

	private String getServiceCallString(Service service, Resource subject)
	{
		return String.format("calling service %s (%s)", service, subject);
	}
		
	private void attachType(Resource resource)
	{
		ResourceTyper.getResourceTyper().attachType(resource);
	}

	/**
	 * Test whether a given node exists in the knowledgebase in
	 * at least one triple. 
	 * 
	 * @param x - the node to test for
	 * @return True if it's in there, false if it's not.
	 */
	private boolean nodeExists(ATermAppl x)
	{
		// I changed this line because nodeExists(x) was returning
		// false, immediately after adding a new individual x
		// to the knowledgebase inside addTriplesToKB().
		// 
		// As far as I understand, the ABox.pseudoModel only gets updated
		// after a consistency check.  So just use the real ABox here
		// instead. 
		//
		// -- B.V.
		//Individual subj = (getABox().getPseudoModel() != null)
		//? getABox().getPseudoModel().getIndividual(x) : getABox().getIndividual(x);
		
		Individual subj = getABox().getIndividual(x);
		return subj != null;
	}
	
	private void addTriplesToKB(Collection<Triple> triples, boolean addWithInverse)
	{
		for (Triple triple: triples) {
			String predicate = triple.getPredicate().toString();
			for (String predicateSynonym: getPredicateSynonyms(predicate)) {
				Triple synonymousTriple = new Triple(
						triple.getSubject(),
						NodeCreateUtils.create(predicateSynonym),
						triple.getObject()
				);
				addTripleToKB(synonymousTriple);
				if (addWithInverse && !isDatatypeProperty(ATermUtils.makeTermAppl(predicate))) {
					String inversePredicate = getInversePredicate(predicate);
					if (inversePredicate == null) {
						log.debug(String.format("no inverse predicate for <%s>", predicate));
					} else {
						Triple inverseTriple = new Triple(
								triple.getObject(),
								NodeCreateUtils.create(predicateSynonym),
								triple.getSubject()
						);
						addTripleToKB(inverseTriple);
					}
				}
			}
		}
	}
	
	private void addTripleToKB(Triple triple)
	{
		log.trace( "storing triple " + triple );
		
		ATermAppl s = makeATermAppl(triple.getSubject());
		ATermAppl p = makeATermAppl(triple.getPredicate());
		ATermAppl o = makeATermAppl(triple.getObject());
		
		if (abox.getIndividual(s) == null)
			addIndividual(s);
		if (rbox.getRole( p ) == null)
			addObjectProperty(p);
		if (!triple.getObject().isLiteral() && abox.getIndividual(o) == null)
			addIndividual(o);
		
		addPropertyValue(p, s, o);
		
		/* have to add it this way as well, even though it's redundant,
		 * or some of the higher-level reasoning doesn't work...
		 */
		RdfUtils.addTripleToModel(model, triple);
	}
	
	private ATermAppl makeATermAppl(Node node)
	{
		if (node.isURI())
			return ATermUtils.makeTermAppl(node.getURI());
		else if (node.isBlank())
			return ATermUtils.makeAnonNominal(node.getBlankNodeId().getLabelString());
		else if (node.isLiteral()) {
			LiteralLabel literal = node.getLiteral();
			RDFDatatype datatype = literal.getDatatype();
			if (datatype == null)
				return ATermUtils.makePlainLiteral(String.valueOf(literal.getValue()));
			else
				return ATermUtils.makeTypedLiteral(String.valueOf(literal.getValue()), datatype.getURI());
		}
		throw new IllegalArgumentException("attempt to add non-URI, non-blank, non-literal node");
	}


	private String translateInverseToPredicate(ATermAppl p)
	{
		if (!ATermUtils.isInv(p))
			throw new RuntimeException("attempt to invert non-inverse predicate");
		String predicate = p.getArguments().getFirst().toString();
		String inversePredicate = getInversePredicate(predicate.toString());
		return inversePredicate;
	}

	/**
	 * Returns null if an inverse doesn't exist. -- B.V.
	 */
	private String getInversePredicate(String predicate)
	{
		Set<ATermAppl> inverses = getInverses(ATermUtils.makeTermAppl(predicate));
		if (inverses.isEmpty())
			return null;
		else
			return inverses.iterator().next().toString();
	}
	
	/**
	 * Returns a list of predicates that are equivalent to the argument,
	 * including the argument itself.
	 * 
	 * @param predicate
	 * @return a list of equivalent predicates
	 */
	private Set<String> getPredicateSynonyms(String predicate)
	{
		ATermAppl p = ATermUtils.makeTermAppl(predicate);
		if (!isProperty(p)|| isAnnotationProperty(p) || isOntologyProperty(p))
			return Collections.singleton(predicate);
		
		// If inverse property, remove "inv()", but remember that we did so.
		boolean invert = false;
		if (ATermUtils.isInv(p)) {
			invert = true;
			p = ATermUtils.makeInv(p);
		}
		
		/* TODO if we call getAllEquivalentProperties on a property that
		 * doesn't exist in the knowledge base, we'll get an error; once we
		 * can dynamically load definitions as we can encounter them, this
		 * shouldn't be a problem...
		 * TODO this should be fixed by the isProperty() check above, but 
		 * I'm leaving the try/catch just in case...
		 */
		try {
			Set<String> predList = new HashSet<String>();
			for (ATermAppl synonym : getAllEquivalentProperties(p)) {
				if (invert)
					predList.add(ATermUtils.makeInv(synonym).toString());
				else
					predList.add(synonym.toString());
			}
			return predList;
		} catch (RuntimeException e) {
			log.warn(e);
			return Collections.singleton(predicate);
		}
		
	}

	private static class Tracker
	{
		private Set<String> visited;
		
		public Tracker()
		{
			visited = new HashSet<String>();
		}
		
		public synchronized boolean beenThere(Service service, String subject)
		{
			String key = getHashKey(service, subject);
			if (visited.contains(key)) {
				return true;
			} else {
				visited.add(key);
				return false;
			}
		}
		
		private String getHashKey(Service service, String subject)
		{
			// both URIs, so this should be safe
			return String.format("%s %s", service.getServiceURI(), subject);
		}
	}
}
