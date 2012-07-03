package ca.unbsj.cbakerlab.information_extraction_sadi;

import java.util.*;
import java.util.Map.Entry;
import java.net.*;
import java.io.*;


import org.apache.log4j.Logger;

//import org.nlp2rdf.core.ErrorHandling;
//import org.nlp2rdf.core.URIGenerator;
//import org.nlp2rdf.core.util.URIGeneratorHelper;
//import org.nlp2rdf.implementation.snowball.SnowballStemmer;


import ca.unbsj.cbakerlab.nlp2rdf.gate.Gate2RDF;
import ca.wilkinsonlab.sadi.service.annotations.Name;
import ca.wilkinsonlab.sadi.service.annotations.Description;
import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.InputClass;
import ca.wilkinsonlab.sadi.service.annotations.OutputClass;
import ca.wilkinsonlab.sadi.service.simple.SimpleSynchronousServiceServlet;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import eu.lod2.nlp2rdf.schema.sso.Sentence;
import gate.util.GateException;

/**
 * Gate based.
 * 
 * @author artjomk
 * 
 */

@Name("extractDrugNamesFromTextV4")
@Description("Extracts drug names from text (based on Gate Annie Gazetteer extracted from DrugBank)")
@ContactEmail("artjom.unb@gmail.com")
@InputClass("http://unbsj.biordf.net/information-extraction/ie-sadi-service-ontology.owl#extractDrugNamesFromText_Input")
@OutputClass("http://unbsj.biordf.net/information-extraction/ie-sadi-service-ontology.owl#extractDrugNamesFromTextV4_Output")
public class ExtractDrugNamesV4 extends SimpleSynchronousServiceServlet {

	private static final Logger log = Logger.getLogger(ExtractDrugNamesV4.class);
	private static String DrugBankNS = "http://www.drugbank.ca/drugs/";
	private static String LSRNDrugBankNS = "http://lsrn.org/DRUG_BANK";

	private static final long timestamp = new Date().getTime();
	private static long instanceCounter = 0;

	
	private static final Gate2RDF g2r;

	
	public ExtractDrugNamesV4() {
		super();
		log.info("Service instance created.");
	}
	
	static 
	{
		 g2r = new Gate2RDF();

	    log.info("Initialised a gate pipeline");
	};

	public void processInput(Resource input, Resource output) {
		log.info("New service invocation.");

		Model outputModel = output.getModel();
		// This will hold the text of the input document.
		String text = null;

		Statement textPropValue = input.getProperty(Vocab.sourceString);
		if (textPropValue == null)
			textPropValue = input.getProperty(Vocab.content);

		if (textPropValue != null) {
			text = textPropValue.getString();
			log.info("I have read input text as a value specified with nlp2rdf:sourceString or bibo:content.");
		} else {
			log.fatal("Input node has no text associated with it by rdf:value, bibo:content or rss:link.");
			throw new IllegalArgumentException("Input node has no text associated with it by rdf:value, bibo:content or rss:link.");
		}

		log.info("Text: " + text);
		//
		// Annotate text with DrugNameAnnieGazetteer.
		//
		
		OntModel model = null;
		synchronized (g2r) 
		{
			try {
				model = g2r.processTextWithDrugNameAnnieGazetteer(text,input.getURI());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			} catch (GateException e) {
				e.printStackTrace();
			}
		};
		
		
		log.info("Returning " + model.size() + " triples");
		log.info(model);


		//
		// Extract drugs and their identifiers from model.
		//
		Map<String, List<String>> results = null;
		try {
			results = extractDrugsFromModel(model);
		} catch (Exception e) {
			e.printStackTrace();
		}

		log.info("results: " + results);

		//
		// Generate output RDF.
		//
		Iterator<Entry<String, List<String>>> entryIter = results.entrySet().iterator();

		while (entryIter.hasNext()) {
			Entry<String, List<String>> entry = entryIter.next();
			log.info("Looking at drug " + entry.getKey());

			Resource chemIdent = outputModel.createResource();
			chemIdent.addProperty(Vocab.type, Vocab.ChemicalIdentifier);
			chemIdent.addProperty(Vocab.hasValue, entry.getKey());

			Resource chemEntity = outputModel.createResource(LSRNDrugBankNS + ":" + entry.getKey());
			chemEntity.addProperty(Vocab.type, Vocab.ChemicalEntity);
			chemEntity.addProperty(Vocab.hasAttribute, chemIdent);
			chemEntity.addProperty(Vocab.label, entry.getValue().get(0));

			Set<String> names = new HashSet<String>(entry.getValue().subList(1, entry.getValue().size()));
			log.info("names: " + names);
			for (String name : names) {
				//Resource nameAttr = createTimestampedInstance(Vocab.Name, outputModel);
				Resource nameAttr = outputModel.createResource();
				nameAttr.addProperty(Vocab.type, Vocab.Name);
				nameAttr.addProperty(Vocab.hasValue, name);
				chemEntity.addProperty(Vocab.hasAttribute, nameAttr);
			}

			output.addProperty(Vocab.topic, chemEntity);
			log.info("Processed drug " + entry.getValue());
		}

		log.info("Finished processing the text.");

	}

	/**
	 * Extracts drugs and their identifiers from RDF model.
	 * @param model
	 * @return
	 */
	private Map<String, List<String>> extractDrugsFromModel(OntModel model) {
		Map<String, List<String>> results = new TreeMap<String, List<String>>();
		Resource drugClass = model.getResource("http://gate.ac.uk/Drug");
		Property pType = model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Property pIds = model.getProperty("http://gate.ac.uk/dbid");
		Property pAnchorOf = model.getProperty("http://nlp2rdf.lod2.eu/schema/string/anchorOf");
		Property pOriginalName = model.getProperty("http://gate.ac.uk/originalName");

		StmtIterator iter = model.listStatements(null, pType, drugClass);
		while (iter.hasNext()) {
			Resource drug = iter.nextStatement().getSubject();
			String id = drug.getProperty(pIds).getObject().toString();
			String nameInText = drug.getProperty(pAnchorOf).getObject().toString();
			String originalName = drug.getProperty(pOriginalName).getObject().toString();

			if (results.containsKey(id.toString())) {
				results.get(id.toString()).add(nameInText.toString().trim().toLowerCase());
				//if (originalName.trim().equalsIgnoreCase((results.get(id.toString())).iterator().next().toString()))
					//log.warn("Original names of " + "" + " are not the same: " + " vs. " + "");
			} else {
				List<String> set = new ArrayList<String>();
				set.add(originalName.trim());
				set.add(nameInText.toString().trim().toLowerCase());
				results.put(id.toString(), set);
			}
		}
		return results;
	}


/*
	public static void main(String[] args) throws Exception {

		// String text =
		// "Should it be used with Indinavir? I worked at an HIV clinic for two years. We used St. John's with great results right alongside conventional anti-virals with no adverse interaction. The major side-effects of Indinavir are headaches, nausea, vomiting, diarrhea, dizziness and insomnia. Again, after consulting with your physician, you may consider trying a good quality St. John's Wort with regular monitoring. What about Digoxin and St. John's? This is probably referring to a small (162) study of in which St. John's demonstrated its ability to promote stable heart function while decreasing depression. So, yes, it's an herb trying to stabilize the organism and may tonify the heart. This is a beneficial side effect. Why is Theophylline, primarily an asthmatic drug, listed? Probably for the same reason as digoxin. Theophylline can cause life-threatening ventricular arrythmias. (And frankly, don't use St. John's when using Theophylline because if the ventricular arrythmias occur, St. John's will be blamed.)	Should St. John's be used with immune suppressants such as Cyclosporin? Absolutely not. The overwhelming majority of medicinal herbs should be avoided with immune suppressants. Herbs strengthen the human organism. They would work directly against drugs designed to weaken the human organism. 	What about using St. John's with chemotherapy? In my practice, I tend to avoid most herbs during the actual process of chemotherapy. I use them before hand to build up the body and afterward to repair the damage to the normal cells.   		Periodically, the pharmaceutical industry feels threatened by a natural product and goes after it in a very deliberate way. About 10 years ago, it was L-Tryptophan. L-Tryptophan was an inexpensive amino acid that was extremely effective in increasing seratonin levels and was virtually without side effects. A Japanese company produced a single bad batch after failing to complete the chemical process and several deaths occurred. The FDA was able to trace the bad batch back to the Japanese company, but still decided to ban the supplement.  Two years ago, the media flooded the public with warnings that St. John's Wort would cause birth control pills to fail resulting in a host of unwanted pregnancies. When the pregnancies didn't occur, the pharmaceuticals tried a different angle. If it were patentable and the pharmaceutical companies could make money off of it, this Internet email warning about herbs would not have occurred. Consider Viagra. It has caused over 60 deaths and continues to be on the market.";
		String text = "Should it be used with Indinavir? I worked at an HIV clinic for two years. We used St. John's with great results right alongside conventional anti-virals with no adverse interaction. The major side-effects of Indinavir are headaches, nausea, vomiting, diarrhea, dizziness and insomnia. Again, after consulting with your physician, you may consider trying a good quality St. John's Wort with regular monitoring. What about Digoxin and St. John's? This is probably referring to a small (162) study of in which St. John's demonstrated its ability to promote stable heart function while decreasing depression. So, yes, it's an herb trying to stabilize the organism and may tonify the heart. This is a beneficial side effect. Why is Theophylline, primarily an asthmatic drug, listed? Probably for the same reason as digoxin. Theophylline can cause life-threatening ventricular arrythmias. (And frankly, don't use St. John's when using Theophylline because if the ventricular arrythmias occur, St. John's will be blamed.) Should St. John's be used with immune suppressants such as Cyclosporin? Absolutely not. The overwhelming majority of medicinal herbs should be avoided with immune suppressants. Herbs strengthen the human organism. They would work directly against drugs designed to weaken the human organism. What about using St. John's with chemotherapy? In my practice, I tend to avoid most herbs during the actual process of chemotherapy. I use them before hand to build up the body and afterward to repair the damage to the normal cells. Periodically, the pharmaceutical industry feels threatened by a natural product and goes after it in a very deliberate way. About 10 years ago, it was L-Tryptophan. L-Tryptophan was an inexpensive amino acid that was extremely effective in increasing seratonin levels and was virtually without side effects. A Japanese company produced a single bad batch after failing to complete the chemical process and several deaths occurred. The FDA was able to trace the bad batch back to the Japanese company, but still decided to ban the supplement. Two years ago, the media flooded the public with warnings that St. John's Wort would cause birth control pills to fail resulting in a host of unwanted pregnancies. When the pregnancies didn't occur, the pharmaceuticals tried a different angle. If it were patentable and the pharmaceutical companies could make money off of it, this Internet email warning about herbs would not have occurred. Consider Viagra. It has caused over 60 deaths and continues to be on the market.";
		String documentUri = "http://example.com/page.html";

		Gate2RDF g2r = new Gate2RDF();		OntModel model = null;
		try {
			model = g2r.processTextWithDrugNameAnnieGazetteer(text,documentUri);
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		} catch (URISyntaxException e2) {
			e2.printStackTrace();
		} catch (GateException e2) {
			e2.printStackTrace();
		}

		log.info("returning " + model.size() + " triples");
		log.info(model);

		// For debugging.
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("txt2rdf_model.rdf")));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		model.write(out, "RDF/XML");
		try {
			out.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		System.out.println("Finished.");

	}
*/
}
