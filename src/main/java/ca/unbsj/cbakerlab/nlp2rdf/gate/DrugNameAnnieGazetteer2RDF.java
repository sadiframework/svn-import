package ca.unbsj.cbakerlab.nlp2rdf.gate;


import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import eu.lod2.nlp2rdf.schema.sso.Sentence;
import eu.lod2.nlp2rdf.schema.sso.Word;
import eu.lod2.nlp2rdf.schema.str.Document;
import eu.lod2.nlp2rdf.schema.str.OffsetBasedString;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;

import org.nlp2rdf.core.Span;
import org.nlp2rdf.core.Text2RDF;
import org.nlp2rdf.core.URIGenerator;
import org.nlp2rdf.core.util.URIComparator;
import org.nlp2rdf.core.util.URIGeneratorHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

/**
 */
public class DrugNameAnnieGazetteer2RDF {
    private static Logger log = LoggerFactory.getLogger(DrugNameAnnieGazetteer2RDF.class);
    
   
      
	/**
	 * Process nlp2rdf document with ...
	 * @param text
	 * @param model
	 * @return model from arguments
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws GateException
	 */

	private OntModel processText(String text, Resource documentResource, OntModel model) throws FileNotFoundException, IOException, URISyntaxException,
			GateException {
		
		//set basic prefixes
        model.setNsPrefix("sso", Utils.structuredSentenceOntologyUrl);
        model.setNsPrefix("str", Utils.stringOntologyUrl);
        model.setNsPrefix("gate", Utils.gateUrl);
        
        // create required resources
		Resource gateDrug = model.createResource(Utils.gateUrl+"Drug");		
		Resource nerdDrug = model.createResource(Utils.nerdOntology+"Drug");
		
		String documentNs = Utils.getDocumentNs(documentResource);
		
		URIGenerator uriGenerator = URIGeneratorHelper.determineGenerator("offset", 0); // "context-hash",
		// 0);

		
		//
		// Step 1. Process text with GATE Annie Gazetteer Drug Names
		//
		// Initialise GATE.
		Utils.initGate();
		
		// Load ANNIE plugin.
		File gateHomeDir = Gate.getGateHome();
		File pluginsHome = new File(gateHomeDir, "plugins");
		Gate.getCreoleRegister().registerDirectories(new File(pluginsHome, "ANNIE").toURI().toURL());

		// Create controller.
		SerialAnalyserController annieController = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController", Factory
				.newFeatureMap(), Factory.newFeatureMap(), "ANNIE_" + Gate.genSym());
		log.info("add gazetteer");

		FeatureMap owParams = Factory.newFeatureMap();
		owParams.put("caseSensitive", true);
		owParams.put("encoding", "UTF-8");
		owParams.put("gazetteerFeatureSeparator", "@");
		//owParams.put("listsURL", new URL(owDir.toURI().toURL().toString() + "/lists.def"));
		owParams.put("listsURL", new URL(log.getClass().getResource("/gate/Gazetteers/DrugNames/lists.def").toURI().toString()));
		//owParams.put("listsURL", owDir.toURI().toURL());		
		ProcessingResource owPr = (ProcessingResource) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer", owParams);
		owPr.setParameterValue("longestMatchOnly", true);

		annieController.add(owPr);
		log.info("drug names gazetteer added");

		Corpus gateCorpus = Factory.newCorpus("corpus");
		gate.Document doc = Factory.newDocument(text);
		doc.setName("documents");
		gateCorpus.add(doc);
		annieController.setCorpus(gateCorpus);

		log.info("\n\nExecuting pipeline...");
		try {
			annieController.execute();
			log.info("Executing pipeline... Done.\n\n");
		} catch (Exception ex) {
			ex.printStackTrace();
		}	
		
	
		//
		// Step 2. Convert to RDF.
		//
		
		AnnotationSet drugNames = doc.getAnnotations().get("Lookup");
		for(Annotation ann : drugNames){
			Long s = ann.getStartNode().getOffset();
			Long e = ann.getEndNode().getOffset();
			String drugNameInText = doc.getContent().getContent(s, e).toString().toLowerCase();						
			
			// skip names shorter than 4
			if(drugNameInText.length() < 4)continue;
			
			log.debug(drugNameInText);
			
			OffsetBasedString string =  new Text2RDF().createStringAnnotationForClass(
					OffsetBasedString.class, 
					documentNs+"#", 
					text, 
					new Span(s.intValue(), e.intValue()), 
					uriGenerator, 
					model);
			string.addProperty(model.getProperty(Utils.rdf+"type"), nerdDrug);
			string.addProperty(model.getProperty(Utils.rdf+"type"), gateDrug);
			documentResource.addProperty(model.getProperty(Utils.stringOntologyUrl+"subString"), string);

			
			FeatureMap features = ann.getFeatures();
			Utils.gateFeaturesToRdf(string, features, Utils.gateUrl, model);
		}
		
		
		return model;
	}

	/**
	 * Process nlp2rdf document with whatizit drug service. As result, a new model is created and populated with drug names and DrugBank IDs. 
	 * @param text
	 * @return model
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws GateException
	 */
	public OntModel processText(String text, String documentUri) throws FileNotFoundException, IOException, URISyntaxException, GateException{
	     OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, ModelFactory.createDefaultModel());
	  // Read rdf from file or generate from text string (here)
			//documentUrl.
			Text2RDF txt2rdf = new Text2RDF();
			URIGenerator uriGenerator = URIGeneratorHelper.determineGenerator("offset", 0); 
			Document document = txt2rdf.createDocumentAnnotation(documentUri+"#", text, uriGenerator, model);

	     processText(text, document, model);
		 return model;
	 }
   
    
	public void processModel(OntModel model, Resource documentResource, String text) throws FileNotFoundException, IOException, URISyntaxException,	GateException {
		processText(text, documentResource, model);
	}
	
	
    /**
     * MAIN
     * @param args
     * @throws Exception
     */
	public static void main(String[] args) throws Exception {


		DrugNameAnnieGazetteer2RDF g2r = new DrugNameAnnieGazetteer2RDF();
		OntModel model = ModelFactory.createOntologyModel();
		Property p = model.getProperty("http://nlp2rdf.lod2.eu/schema/string/sourceString");
		model.read(new FileReader(new File("sentence_model.rdf")), "UTF8");

		ResIterator documentIter = model.listSubjectsWithProperty(RDF.type, model.getResource("http://nlp2rdf.lod2.eu/schema/string/Document"));
	    Resource documentResource = null;
	    while(documentIter.hasNext()){
	    	documentResource = documentIter.next();
	    	log.info("Document in processing: "+documentResource);
	    	Statement textPropValue = model.getProperty(documentResource,p);
			String text = textPropValue.getString();
			
			log.info("text: "+text);
			
			g2r.processModel(model,documentResource,text);
			log.info("returning " + model.size() + " triples");
	    }
		
		
	    // For debugging.
	    Utils.saveModel(model, "drug_model.rdf");

		RDF2Gate.rdf2gateXml(model,false);		

		System.out.println("Finished.");

	}

}
