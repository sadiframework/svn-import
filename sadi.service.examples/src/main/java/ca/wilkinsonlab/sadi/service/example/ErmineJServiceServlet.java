package ca.wilkinsonlab.sadi.service.example;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.erminej.ClassScoreSimple;
import ubic.erminej.Settings;
import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.service.simple.SimpleSynchronousServiceServlet;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDFS;

@ContactEmail("info@sadiframework.org")
@TestCases(
		@TestCase(
				input = "/t/ermineJ-input.rdf", 
				output = "/t/ermineJ-output.rdf"
		)
)
public class ErmineJServiceServlet extends SimpleSynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(ErmineJServiceServlet.class);

	public void processInput(Resource input, Resource output)
	{
		List<String> probeIds = new ArrayList<String>();
		List<Double> expressionLevels = new ArrayList<Double>();
		List<String> geneIds = new ArrayList<String>();
		List<Collection<String>> termCollections = new ArrayList<Collection<String>>();
		Set<String> allTerms = new HashSet<String>();
		for (StmtIterator i = input.listProperties(Vocab.element); i.hasNext(); ) {
			Statement s = i.nextStatement();
			try {
				Resource probe = s.getResource();
				String probeId = getProbeId(probe);
				Double expressionLevel = getExpressionLevel(probe);
				// there should only be one mapped_to gene...
				Resource gene = probe.getProperty(Vocab.mappedTo).getResource();
				String geneId = getGeneId(gene);
				// each gene can be mapped to many terms...
				Collection<String> terms = new ArrayList<String>();
				for (StmtIterator j = gene.listProperties(Vocab.hasInputTerm); j.hasNext(); ) {
					Statement t = j.nextStatement();
					Resource goTerm = t.getResource();
					String term = getTerm(goTerm);
					terms.add(term);
					allTerms.add(term);
				}
				log.debug(String.format("probe %s (gene %s) expressed at %s: %s", probeId, geneId, expressionLevel, terms));
				probeIds.add(probeId);
				expressionLevels.add(expressionLevel);
				geneIds.add(geneId);
				termCollections.add(terms);
			} catch (Exception e) {
				log.error(e);
			}
		}
		
		ClassScoreSimple scores = runORA(probeIds, expressionLevels, geneIds, termCollections);
		for (String term: allTerms) {
			double p = scores.getGeneSetPvalue(term);
			if (p < 0)
				continue;
			Resource orTerm = output.getModel().createResource(Vocab.OverrepresentedTerm);
			orTerm.addProperty(Vocab.hasOutputTerm, output.getModel().createResource(term));
			orTerm.addLiteral(Vocab.hasSignificance, p);
			output.addProperty(Vocab.hasOverrepresentedTerm, orTerm);
		}
	}

	private String getProbeId(Resource probe)
	{
		if (probe.isURIResource())
			return probe.getURI();
		else
			return probe.getId().toString();
	}

	private Double getExpressionLevel(Resource probe)
	{
		return probe.getProperty(Vocab.expressionLevel).getDouble();
	}

	private String getGeneId(Resource gene)
	{
		if (gene.isURIResource())
			return gene.getURI();
		else
			return gene.getId().toString();
	}

	private String getTerm(Resource term)
	{
		if (term.hasProperty(RDFS.label))
			return term.getProperty(RDFS.label).getString();
		else if (term.isURIResource())
			return term.getURI();
		else
			return term.getId().toString();
	}

	private ClassScoreSimple runORA(List<String> probeIds, List<Double> expressionLevels, List<String> geneIds, List<Collection<String>> termCollections)
	{
		int maxGeneSetSize = 5000;
		int minGeneSetSize = 1 ;
		double geneScoreThreshold = 1.0;
		boolean isBigGeneScoreBetter = true;
		boolean logTransformGeneScore = false;
		int geneReplicateTreatment = ClassScoreSimple.BEST_GENE_SCORE;
		
		ClassScoreSimple scores = new ClassScoreSimple( probeIds, geneIds, termCollections );
	
	    // in our raw data, smaller values are better (like pvalues, unlike fold
	    // change)
		scores.setBigGeneScoreIsBetter( isBigGeneScoreBetter );
		scores.setLogTransformGeneScores( logTransformGeneScore );
	
	    // set range of sizes of gene sets to consider.
		scores.setMaxGeneSetSize( maxGeneSetSize );
		scores.setMinGeneSetSize( minGeneSetSize );
		scores.setGeneReplicateTreatment(geneReplicateTreatment);
	    
	    // css.setGeneScoreSummaryMethod(val)
	    
	    // use this pvalue threshold for selecting genes. (before taking logs)
		scores.setGeneScoreThreshold(geneScoreThreshold);
	    
	    // use over-representation analysis.
		scores.setClassScoreMethod( Settings.ORA );

		// scores.setGodata(goNames);
	
		scores.run( expressionLevels ); // might want to run in a separate thread.
		
	    return scores;
	}
	
	private static class Vocab
	{
		private static Model m_model = ModelFactory.createDefaultModel();
		
		public static final Property element = m_model.getProperty("http://sadiframework.org/examples/common.owl#element");
		public static final Property expressionLevel = m_model.getProperty("http://sadiframework.org/examples/ermineJ.owl#expressionLevel");
		public static final Property mappedTo = m_model.getProperty("http://sadiframework.org/examples/ermineJ.owl#mappedTo");
		public static final Property hasInputTerm = m_model.getProperty("http://sadiframework.org/ontologies/predicates.owl#hasGOTerm");
		public static final Property hasOutputTerm = m_model.getProperty("http://sadiframework.org/examples/ermineJ.owl#term");
		public static final Property hasSignificance = m_model.getProperty("http://sadiframework.org/examples/ermineJ.owl#p");
		public static final Property hasOverrepresentedTerm = m_model.getProperty("http://sadiframework.org/examples/ermineJ.owl#hasOverrepresentedTerm");
		public static final Resource OverrepresentedTerm = m_model.getResource("http://sadiframework.org/examples/ermineJ.owl#OverrepresentedTerm");
	}
}
