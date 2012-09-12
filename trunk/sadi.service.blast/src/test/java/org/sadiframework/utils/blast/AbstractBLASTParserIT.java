package org.sadiframework.utils.blast;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sadiframework.utils.OwlUtils;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class AbstractBLASTParserIT
{
	@Before
	public void setUp() throws Exception
	{
	}

	@After
	public void tearDown() throws Exception
	{
	}

	@Test
	public void testVocab() throws Exception
	{
		/* TODO extract SIO terms used in blast.owl (with labels, hierarchy and inverses)
		 * into a sio-simple.owl and publish that (we can add to it as needed...)
		 */
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		Collection<Resource> undefined = new ArrayList<Resource>();
		Field[] declaredFields = AbstractBLASTParser.Vocab.class.getDeclaredFields();
		for (Field field : declaredFields) {
			if (Resource.class.isAssignableFrom(field.getType())) {
				Resource r = (Resource)field.get(null);
				OntResource defined = OwlUtils.getOntResourceWithLoad(model, r.getURI());
				if (defined == null)
					undefined.add(r);
			}
		}
		assertTrue(String.format("found undefined resources: %s", undefined), undefined.isEmpty());
	}
}
