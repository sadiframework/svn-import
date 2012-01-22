package ca.wilkinsonlab.sadi.share;

import static org.junit.Assert.assertEquals;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SHAREKnowledgeBaseTest
{
    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception
    {
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
    public void testConfig() throws Exception
    {
        Configuration config = new BaseConfiguration();
        config.setProperty(SHAREKnowledgeBase.REASONER_SPEC_CONFIG_KEY, "com.hp.hpl.jena.ontology.OntModelSpec.OWL_MEM");
        SHAREKnowledgeBase kb = new SHAREKnowledgeBase(config);
        assertEquals(kb.getReasoningModel().getSpecification(), com.hp.hpl.jena.ontology.OntModelSpec.OWL_MEM);
    }
}
