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
import ca.wilkinsonlab.sadi.service.SynchronousServiceServlet;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class ErmineJServiceServlet extends SynchronousServiceServlet
{
	public static final String ELEMENT_URI = "http://sadiframework.org/examples/common.owl#element";
	public static final String EXPRESSION_LEVEL_URI = "http://sadiframework.org/examples/ermineJ.owl#expressionLevel";
	public static final String MAPPED_TO_URI = "http://sadiframework.org/examples/ermineJ.owl#mappedTo";
	public static final String HAS_INPUT_TERM_URI = "http://sadiframework.org/ontologies/predicates.owl#hasGOTerm";
	public static final String HAS_OUTPUT_TERM_URI = "http://sadiframework.org/examples/ermineJ.owl#term";
	public static final String HAS_SIGNIFICANCE_URI = "http://sadiframework.org/examples/ermineJ.owl#p";
	public static final String HAS_OVERREPRESENTED_TERM = "http://sadiframework.org/examples/ermineJ.owl#hasOverrepresentedTerm";
	public static final String OVERREPRESENTED_TERM_URI = "http://sadiframework.org/examples/ermineJ.owl#OverrepresentedTerm";

	private static final Log log = LogFactory.getLog(ErmineJServiceServlet.class);
	
	private final Property element;
	private final Property expressionLevel;
	private final Property mappedTo;
	private final Property hasInputTerm;
	private final Property hasOutputTerm;
	private final Property hasSignificance;
	private final Property hasOverrepresentedTerm;
	private final Resource OverrepresentedTerm;
	
	public ErmineJServiceServlet()
	{
		super();

		/* TODO the exact input term property here will change depending on
		 * whether the GO term service, MESH term service, etc. has been called...
		 */ 
		element = ontologyModel.getProperty(ELEMENT_URI);
		expressionLevel = ontologyModel.getProperty(EXPRESSION_LEVEL_URI);
		mappedTo = ontologyModel.getProperty(MAPPED_TO_URI);
		hasInputTerm = ontologyModel.getProperty(HAS_INPUT_TERM_URI);
		hasOutputTerm = ontologyModel.getProperty(HAS_OUTPUT_TERM_URI);
		hasSignificance = ontologyModel.getProperty(HAS_SIGNIFICANCE_URI);
		hasOverrepresentedTerm = ontologyModel.getProperty(HAS_OVERREPRESENTED_TERM);
		OverrepresentedTerm = ontologyModel.getResource(OVERREPRESENTED_TERM_URI);
	}

	public void processInput(Resource input, Resource output)
	{
		List<String> probeIds = new ArrayList<String>();
		List<Double> expressionLevels = new ArrayList<Double>();
		List<String> geneIds = new ArrayList<String>();
		List<Collection<String>> termCollections = new ArrayList<Collection<String>>();
		Set<String> allTerms = new HashSet<String>();
		for (StmtIterator i = input.listProperties(element); i.hasNext(); ) {
			Statement s = i.nextStatement();
			try {
				Resource probe = s.getResource();
				String probeId = getProbeId(probe);
				Double expressionLevel = getExpressionLevel(probe);
				// there should only be one mapped_to gene...
				Resource gene = probe.getProperty(mappedTo).getResource();
				String geneId = getGeneId(gene);
				// each gene can be mapped to many terms...
				Collection<String> terms = new ArrayList<String>();
				for (StmtIterator j = gene.listProperties(hasInputTerm); j.hasNext(); ) {
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
			Resource orTerm = output.getModel().createResource(OverrepresentedTerm);
			orTerm.addProperty(hasOutputTerm, output.getModel().createResource(term));
			orTerm.addLiteral(hasSignificance, p);
			output.addProperty(hasOverrepresentedTerm, orTerm);
		}
	}

	private String getProbeId(Resource probe)
	{
		return probe.getURI();
	}

	private Double getExpressionLevel(Resource probe)
	{
		return probe.getProperty(expressionLevel).getDouble();
	}

	private String getGeneId(Resource gene)
	{
		return gene.getURI();
	}

	private String getTerm(Resource term)
	{
//		return term.getProperty(RDFS.label).getString();
		return term.getURI();
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
}
