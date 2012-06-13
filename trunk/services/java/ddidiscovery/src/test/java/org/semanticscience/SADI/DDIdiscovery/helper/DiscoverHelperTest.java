package org.semanticscience.SADI.DDIdiscovery.helper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.semanticscience.SADI.DDIdiscovery.Discover;

import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import junit.framework.TestCase;

public class DiscoverHelperTest  {

	@Test
	public void getDDIsFromEnd2pointTest(){
		List<DrugDrugInteraction> ddil = DiscoverHelper.findDDIInEndpoint("DB02648");
		System.out.println(ddil);
	}
	/*
	@Test
	public void testFindDDIs(){
		final String ddiFilename = "ddi-0.0.3.csv";
		//create a sample input resource
		Model m = ModelFactory.createDefaultModel();
		Resource chemEnt = m.createResource(RdfUtils.createUniqueURI());
		Resource dbId = m.createResource(RdfUtils.createUniqueURI());
		chemEnt.addProperty(Vocab.SIO_000008, dbId);
		dbId.addProperty(Vocab.SIO_000300, "DB0323");
		
		//create an InputStream from the csv file
		InputStream is = Discover.class.getClassLoader().getResourceAsStream(ddiFilename);
		
		ArrayList<DrugDrugInteraction> ddis = DiscoverHelper.findDDISInCSVFile(is, chemEnt);
		System.out.println(ddis);
		
	}*/
	
	public static final class Vocab{
		private static Model m_model = ModelFactory.createDefaultModel();
		public static final Property SIO_000300 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000300");
		public static final Property SIO_000008 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000008");
	}
}
