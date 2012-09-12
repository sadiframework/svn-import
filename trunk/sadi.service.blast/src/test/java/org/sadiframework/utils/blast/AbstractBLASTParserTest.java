package org.sadiframework.utils.blast;

import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class AbstractBLASTParserTest
{
	private static final Logger log = Logger.getLogger(AbstractBLASTParserTest.class);
	
	@Before
	public void setUp() throws Exception
	{
	}

	@After
	public void tearDown() throws Exception
	{
	}

	@Test
	public void testParseBLAST() throws Exception
	{
		Model model = ModelFactory.createDefaultModel();
		try {
			new AbstractBLASTParser() {
				@Override
				protected Resource getQuerySequence(Model model, Node iteration)
				{
					return model.createResource();
				}
				@Override
				protected Resource getHitSequence(Model model, Node iteration) {
					return model.createResource();
				}
			}.parseBLAST(model, AbstractBLASTParserTest.class.getResourceAsStream("/dragon-blast-report.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		// try writing to RDF/XML to catch any potential serialization errors...
		StringWriter buf = new StringWriter();
		model.write(buf, "RDF/XML");
		if (log.isDebugEnabled())
			log.debug(String.format("BLAST output\n%s", buf.toString()));
	}
}
