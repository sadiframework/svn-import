package ca.wilkinsonlab.sadi.client.virtual.biomoby;

import java.util.Collection;

import junit.framework.TestCase;

import org.biomoby.shared.data.MobyDataObject;
import org.junit.Before;
import org.junit.Test;

import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.client.Service;

public class BioMobyRegistryTest extends TestCase
{
	private static final String PREDICATE_URI = "http://ontology.dumontierlab.com/hasParticipant";
	private static final String SERVICE_NAME = "getKeggIdsByKeggPathway";
//	private static final String SubjNamespaces[] = { "KEGG" };
//	private static final String ObjNamespaces[] = { "KEGG_PATHWAY" };
	
	private BioMobyRegistry registry;
	
	@Before
	public void setUp()
	{
		registry = (BioMobyRegistry)Config.getConfiguration().getRegistry("biomoby");
	}
	
//	@Test
//	public final void testGetReferencedPredicates() throws Exception
//	{
//		Set<String> predicates = registry.getReferencedPredicates();
//		assertFalse(predicates.isEmpty());
//	}
	
	@Test
	public final void testGetServicesByPredicate() throws Exception
	{
		Collection<Service> services = registry.findServicesByPredicate(PREDICATE_URI);
		for (Service service: services) {
			BioMobyService mobyService = (BioMobyService)service;
			if (mobyService.getName().equals(SERVICE_NAME))
				return;
		}
		
		fail(String.format("getServiceByPredicate(%s) failed to find %s", PREDICATE_URI, SERVICE_NAME));
	}
	
	@Test
	public final void testGetPredicatesByInputNamespace() throws Exception
	{
		Collection<String> predicates = registry.findPredicatesByInputNamespace("UniProt");
		assertFalse(predicates.isEmpty());
	}
	
	@Test
	public final void testSPARQLInjection() throws Exception
	{
		/////////////////////////////////////////////////////////////////
		// I can't think of anything malicious that could be done by 
		// injection inside the braces of a WHERE clause, which is
		// where all our variable substitutions take place.  So I'm just
		// doing the funkiest thing I can think of: embedding a SELECT 
		// query that selects everything in the database.  This 'attack' 
		// would only work with Virtuoso because other SPARQL engines don't 
		// support nested SPARQL queries (as far as I know).
		//
		// (Even without the appropriate string escaping, this trick would be 
		// unlikely to work.  For our query interface, it would
		// be impossible for the user to specify a string like "injectionPred"
		// and have it treated as a single predicate; the Pellet SPARQL
		// parser would break the string up across whitespaces.)
		/////////////////////////////////////////////////////////////////
			
		final String injectionPred = 
			"http://moby/hasPathwayGene> ?output . " +
			"{" +
				"SELECT * WHERE { ?s ?p ?o }" +
			"}" +
			"?input <http://moby/hasPathwayGene";
			
		Collection<Service> services = registry.findServicesByPredicate(injectionPred);
		
		// If the injection works, then we will get services that
		// are mapped to <http://moby/hasPathwayGene>.
		assertTrue(services.isEmpty());
	}
	
	@Test
	public final void testConvertUriToMobyDataObject() throws Exception
	{
		MobyDataObject data;
		data = registry.convertUriToMobyDataObject("http://biordf.net/moby/UniProt/P12345");
		assertEquals("incorrect namespace", "UniProt", data.getNamespaces()[0].getName());
		assertEquals("incorrect id", "P12345", data.getId());
		
		data = registry.convertUriToMobyDataObject("http://lsrn.org/UniProt:P12345");
		assertEquals("incorrect namespace", "UniProt", data.getNamespaces()[0].getName());
		assertEquals("incorrect id", "P12345", data.getId());
	}
	
	@Test
	public final void testConvertMobyDataObjectToUri() throws Exception
	{
		MobyDataObject data = new MobyDataObject("UniProt", "P12345");
		String uri = registry.convertMobyDataObjectToUri(data);
		assertEquals("incorrect uri", "http://lsrn.org/UniProt:P12345", uri);
	}
}
