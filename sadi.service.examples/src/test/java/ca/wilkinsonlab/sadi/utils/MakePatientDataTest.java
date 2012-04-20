package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.elmonline.util.CountingMap;
import ca.wilkinsonlab.sadi.vocab.Patients;
import ca.wilkinsonlab.sadi.vocab.Regression;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class MakePatientDataTest
{
	private static OntModel model;
	
	private static final String LONG_QUERY = 
		"PREFIX patients: <http://sadiframework.org/ontologies/patients.owl#> \n" + 
		"PREFIX regress: <http://sadiframework.org/examples/regression.owl#> \n" + 
		"PREFIX sio: <http://semanticscience.org/resource/> \n" + 
		"SELECT ?patient ?bun ?creat \n" + 
		"FROM <http://sadiframework.org/ontologies/patients.rdf>\n" + 
		"FROM <http://sadiframework.org/ontologies/patients.owl>\n" + 
		"WHERE {\n" + 
		"	?patient patients:creatinineLevels ?creats . \n" + 
		"	?creats regress:hasRegressionModel ?model . \n" + 
		"	?model regress:slope ?slope\n" + 
		"		FILTER ( ?slope > 0 ) .\n" + 
		"	?creats sio:SIO_000059 ?creatEvent . \n" +
		"	?creatEvent sio:SIO_000008 ?creatMeasurementNode . \n" +
		"	?creatMeasurementNode a patients:Measurement . \n" +
		"	?creatMeasurementNode sio:SIO_000300 ?creat . \n" +
		"	?creatEvent sio:SIO_000008 ?creatOffNode . \n" +
		"	?creatOffNode a patients:Offset . \n" +
		"	?creatOffNode sio:SIO_000300 ?creatOff . \n" + 
		"	OPTIONAL { \n" + 
		"		?creats sio:SIO_000059 ?creatEvent2 . \n" + 
		"		?creatEvent2 sio:SIO_000008 ?creatOffNode2 . \n" + 
		"		?creatOffNode2 a patients:Offset . \n" + 
		"		?creatOffNode2 sio:SIO_000300 ?creatOff2 . \n" + 
		"		FILTER( ?creatOff2 > ?creatOff ) . \n" + 
		"   } . FILTER ( !bound(?creatOff2) ) . \n" + 
		"	?patient patients:BUNLevels ?buns . \n" + 
		"	?buns sio:SIO_000059 ?bunEvent . \n" +
		"	?bunEvent sio:SIO_000008 ?bunMeasurementNode . \n" +
		"	?bunMeasurementNode a patients:Measurement . \n" +
		"	?bunMeasurementNode sio:SIO_000300 ?bun . \n" +
		"	?bunEvent sio:SIO_000008 ?bunOffNode . \n" +
		"	?bunOffNode a patients:Offset . \n" +
		"	?bunOffNode sio:SIO_000300 ?bunOff . \n" + 
		"	OPTIONAL { \n" + 
		"		?buns sio:SIO_000059 ?bunEvent2 . \n" + 
		"		?bunEvent2 sio:SIO_000008 ?bunOffNode2 . \n" + 
		"		?bunOffNode2 a patients:Offset . \n" + 
		"		?bunOffNode2 sio:SIO_000300 ?bunOff2 . \n" + 
		"		FILTER( ?bunOff2 > ?bunOff ) . \n" + 
		"   } . FILTER ( !bound(?bunOff2) ) . \n" + 
		"}";

	@SuppressWarnings("unused")
	private static final String SHORT_QUERY = 
		"PREFIX patients: <http://sadiframework.org/ontologies/patients.owl#> \n" + 
		"PREFIX regress: <http://sadiframework.org/examples/regression.owl#> \n" + 
		"SELECT ?patient ?bun ?creat \n" + 
		"FROM <http://sadiframework.org/ontologies/patients.rdf> \n" + 
		"FROM <http://sadiframework.org/ontologies/patients.owl> \n" + 
		"WHERE {\n" + 
		"	?patient a patients:LikelyRejector . \n" + 
		"	?patient patients:creatinineLevels ?creats . \n" + 
		"	?creats regress:yForLargestX ?creat . \n" + 
		"	?patient patients:BUNLevels ?buns . \n" + 
		"	?buns regress:yForLargestX ?bun . \n" + 
		"}";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
//		LocationMapper.get().addAltPrefix("http://sadiframework.org/examples/", "file:../sadi.service.examples/src/main/webapp/");
//		LocationMapper.get().addAltPrefix("http://sadiframework.org/ontologies/", "file:../sadiframework.org/ontologies/");
		model = OwlUtils.createDefaultReasoningModel();
		model.read("http://sadiframework.org/ontologies/patients.rdf");
		model.read("http://sadiframework.org/ontologies/patients.owl");
		
		// add slopes...
		for (RDFNode o: model.listObjectsOfProperty(Patients.creatinineLevels).toList()) {
			Resource collection = o.asResource();
			Resource regressionModel = model.createResource(Regression.LinearRegressionModel);
			regressionModel.addLiteral(Regression.slope, 1.0);
			collection.addProperty(Regression.hasRegressionModel, regressionModel);
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
//		LocationMapper.get().removeAltPrefix("http://sadiframework.org/examples/");
//		LocationMapper.get().removeAltPrefix("http://sadiframework.org/ontologies/");
		model.close();
		model = null;
	}

	@Before
	public void setUp() throws Exception
	{
	}

	@After
	public void tearDown() throws Exception
	{
	}
	
	@Test
	public void testQuery()
	{
		QueryExecution qe = QueryExecutionFactory.create(LONG_QUERY, model);
		ResultSet resultSet = qe.execSelect();
		assertTrue("no results", resultSet.hasNext());
		CountingMap<RDFNode> seen = new CountingMap<RDFNode>();
		while (resultSet.hasNext()) {
			QuerySolution binding = resultSet.nextSolution();
			Resource patient = (Resource)binding.get("patient");
			assertFalse(String.format("more than one binding for patient %s", patient), seen.seen(patient));
			assertEquals(String.format("sub-maximal value bound to ?creat for patient %s", patient),
					latest(patient.getPropertyResourceValue(Patients.creatinineLevels)), binding.get("creat"));
			assertEquals(String.format("sub-maximal value bound to ?bun for patient %s", patient),
					latest(patient.getPropertyResourceValue(Patients.bunLevels)), binding.get("bun"));
		}
	}
	private RDFNode latest(Resource collection)
	{
		Resource latest = collection.getPropertyResourceValue(SIO.has_member);
		for (Iterator<Resource> events = RdfUtils.getPropertyValues(collection, SIO.has_member, null); events.hasNext(); ) {
			Resource event = events.next();
			if (getOffset(event) > getOffset(latest))
				latest = event;
		}
		return RdfUtils.getPropertyValue(latest, SIO.has_attribute, Patients.Measurement).getRequiredProperty(SIO.has_value).getObject();
	}
	private int getOffset(Resource event)
	{
		return RdfUtils.getPropertyValue(event, SIO.has_attribute, Patients.Offset).getRequiredProperty(SIO.has_value).getInt();
	}
}
