package ca.unbsj.cbakerlab.nlp2rdf.gate;

import eu.lod2.nlp2rdf.schema.sso.Phrase;
import eu.lod2.nlp2rdf.schema.sso.Sentence;
import eu.lod2.nlp2rdf.schema.sso.Word;
import eu.lod2.nlp2rdf.schema.str.ContextHashBasedString;
import eu.lod2.nlp2rdf.schema.str.Document;
import eu.lod2.nlp2rdf.schema.str.OffsetBasedString;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;
import gate.util.InvalidOffsetException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.nlp2rdf.core.Span;
import org.nlp2rdf.core.Text2RDF;
import org.nlp2rdf.core.URIGenerator;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class Utils {

	private static final Logger log = Logger.getLogger(Utils.class);

	public static final String stringOntologyUrl = "http://nlp2rdf.lod2.eu/schema/string/";
	public static final String structuredSentenceOntologyUrl = "http://nlp2rdf.lod2.eu/schema/sso/";
	public static final String scmsUrl = "http://ns.aksw.org/scms/";
	public static final String ebiUrl = "http://www.ebi.ac.uk/";
	public static final String gateUrl = "http://gate.ac.uk/";
	public static final String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String nerdOntology = "http://nerd.eurecom.fr/ontology#";
	public static final String oliaOntology = "http://purl.org/olia/olia.owl#";
	public static final String oliaOntologyUrl = "http://purl.org/olia/olia.owl";
	public static final String oliaSystemOntology = "http://purl.org/olia/system.owl#";
	public static final String pennOntology = "http://purl.org/olia/penn.owl#";

	
	static void setDefaultNifNSs(Model model){
        model.setNsPrefix("sso", Utils.structuredSentenceOntologyUrl);
        model.setNsPrefix("str", Utils.stringOntologyUrl);
        model.setNsPrefix("olia-ont", Utils.oliaOntology);
        model.setNsPrefix("olia-sys", Utils.oliaSystemOntology);      
	}


	
	public static void initGate() throws GateException, FileNotFoundException, IOException, URISyntaxException {
		if (Gate.isInitialised())
			return;
		// Initialise GATE.
		//Properties pro = new Properties();
		// pro.load(new FileInputStream(new
		// File(ClassLoader.getSystemClassLoader().getResource("project.properties").toURI())));
		///pro.load(log.getClass().getResourceAsStream("/gate.properties"));
		///String gateHome = pro.getProperty("GATE_HOME");

		
		log.info("Initializing GATE...");
		try {
			//Gate.setGateHome(new File(gateHome));
			//Gate.setGateHome(new File(ClassLoader.getSystemClassLoader().getResource(gateHome).toURI()));
			//Gate.setGateHome(new File(log.getClass().getResource(gateHome).toURI()));


		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String PROGRAM_DIRECTORY=null;
		//log.info(log.getClass().getClassLoader().);
		try {
		    //Attempt to get the path of the actual JAR file, because the working directory is frequently not where the file is.
		    //Example: file:/D:/all/Java/TitanWaterworks/TitanWaterworks-en.jar!/TitanWaterworks.class
		    //Another example: /D:/all/Java/TitanWaterworks/TitanWaterworks.class
		    PROGRAM_DIRECTORY = log.getClass().getClassLoader().getResource("gate").getPath(); // Gets the path of the class or jar.

		    //Find the last ! and cut it off at that location. If this isn't being run from a jar, there is no !, so it'll cause an exception, which is fine.
		    try {
		        PROGRAM_DIRECTORY = PROGRAM_DIRECTORY.substring(0, PROGRAM_DIRECTORY.lastIndexOf('!'));
		    } catch (Exception e) { }

		    //Find the last / and cut it off at that location.
		    PROGRAM_DIRECTORY = PROGRAM_DIRECTORY.substring(0, PROGRAM_DIRECTORY.lastIndexOf('/') + 1);
		    //If it starts with /, cut it off.
		    if (PROGRAM_DIRECTORY.startsWith("/")) PROGRAM_DIRECTORY = PROGRAM_DIRECTORY.substring(1, PROGRAM_DIRECTORY.length());
		    //If it starts with file:/, cut that off, too.
		    if (PROGRAM_DIRECTORY.startsWith("file:/")) PROGRAM_DIRECTORY = PROGRAM_DIRECTORY.substring(6, PROGRAM_DIRECTORY.length());
		} catch (Exception e) {
		    PROGRAM_DIRECTORY = ""; //Current working directory instead.
		}

		log.info("PROGRAM_DIRECTORY: "+PROGRAM_DIRECTORY);
		
		Gate.setGateHome(new File(log.getClass().getClassLoader().getResource("gate").getPath()));
		Gate.setPluginsHome(new File(log.getClass().getClassLoader().getResource("gate/plugins").getPath()));
		//Gate.setSiteConfigFile(configFile);
		
		Gate.init();
		log.info("Initializing GATE... Done.");
	}

	static String[] parseOffsetStringResource(Resource offsetStringResource) {
		String[] result = new String[4];
		String regex = ".*(offset|hash)_([0-9]+)_([0-9]+)_(.+)$";
		Pattern pattern;
		Matcher matcher;
		pattern = Pattern.compile(regex);
		matcher = pattern.matcher(offsetStringResource.toString());
		while (matcher.find()) {
			result[0] = matcher.group(1);
			result[1] = matcher.group(2);
			result[2] = matcher.group(3);
			result[3] = matcher.group(4);
		}
		return result;
	}

	static int[] getBeginAndEnd(Resource offsetStringResource) {
		String[] parsed = parseOffsetStringResource(offsetStringResource);
		return new int[] { Integer.parseInt(parsed[1]), Integer.parseInt(parsed[2]) };
	}

	static boolean sameOffset(Resource offsetStringResource, Resource offsetStringResource2) {
		int[] beginAndEnd = getBeginAndEnd(offsetStringResource);
		int[] beginAndEnd2 = getBeginAndEnd(offsetStringResource2);
		if (beginAndEnd[0] == beginAndEnd2[0] && beginAndEnd[1] == beginAndEnd2[1]) {
			return true;
		}
		return false;
	}

	void addBeginIndexAndEndIndexProperties(Resource offsetStringResource, Model model) {
		int[] beginAndEnd = getBeginAndEnd(offsetStringResource);

		if (offsetStringResource instanceof Sentence) {
			((Sentence) offsetStringResource).addBeginIndex(Integer.toString(beginAndEnd[0]));
			((Sentence) offsetStringResource).addEndIndex(Integer.toString(beginAndEnd[1]));
		} else if (offsetStringResource instanceof Phrase) {
			((Phrase) offsetStringResource).addBeginIndex(Integer.toString(beginAndEnd[0]));
			((Phrase) offsetStringResource).addEndIndex(Integer.toString(beginAndEnd[1]));
		} else if (offsetStringResource instanceof Word) {
			((Word) offsetStringResource).addBeginIndex(Integer.toString(beginAndEnd[0]));
			((Word) offsetStringResource).addEndIndex(Integer.toString(beginAndEnd[1]));
		} else if (offsetStringResource instanceof OffsetBasedString) {
			((OffsetBasedString) offsetStringResource).addBeginIndex(Integer.toString(beginAndEnd[0]));
			((OffsetBasedString) offsetStringResource).addEndIndex(Integer.toString(beginAndEnd[1]));
		} else if (offsetStringResource instanceof ContextHashBasedString) {
			((ContextHashBasedString) offsetStringResource).addBeginIndex(Integer.toString(beginAndEnd[0]));
			((ContextHashBasedString) offsetStringResource).addEndIndex(Integer.toString(beginAndEnd[1]));
		} else {
			String message = "Class was not Word, Phrase or Sentence";
			log.error(message);
			throw new InvalidParameterException(message);
		}

		log.debug("Finished addition of beginIndex/endIndex properties " + (model.size()) + " triples added, ");
	}

	/**
	 * 
	 * @param resource
	 * @param features
	 * @param ns
	 * @param model
	 */
	static void gateFeaturesToRdf(Resource resource, FeatureMap features, String ns, OntModel model) {
		for (Iterator<Object> iter = features.keySet().iterator(); iter.hasNext();) {
			String f = (String) iter.next();
			
			if(Gate2RDF.GATE2OLIA.containsKey(f)){
				Property property = model.createProperty(Gate2RDF.GATE2OLIA.get(f));
				String value = null;
				try {
					value = (String) features.get(f);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				if (value != null) {
					resource.addProperty(property, value);
				}
			}else{//log.info(f);
				Property property = model.createProperty(ns+f);
				String value = null;
				try {
					value = (String) features.get(f);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				if (value != null) {
					resource.addProperty(property, value);
				}
			}
			

		}
	}
/*
	static gate.Document modelToGateDocument(OntModel inputModel, Resource documentResource, String text) throws ResourceInstantiationException, NumberFormatException, InvalidOffsetException {
		gate.Document gateDocument = Factory.newDocument(text);
		ResIterator tokenIter = inputModel.listSubjectsWithProperty(RDF.type, inputModel.getResource(Utils.gateUrl + "Token"));
		Resource token = null;
		while (tokenIter.hasNext()) {
			token = tokenIter.next();
			FeatureMap tokParams = Utils.rdf2gateFeatures(token, Utils.gateUrl,inputModel);
			String[] parsed = Utils.parseOffsetStringResource(token);
			gateDocument.getAnnotations().add(new Long(Long.parseLong(parsed[1])), new Long(Long.parseLong(parsed[2])), "Token", tokParams);
		}
		
		ResIterator spaceTokenIter = inputModel.listSubjectsWithProperty(RDF.type, inputModel.getResource(Utils.gateUrl + "SpaceToken"));
		Resource spaceToken = null;
		while (spaceTokenIter.hasNext()) {
			spaceToken = spaceTokenIter.next();
			FeatureMap tokParams = Utils.rdf2gateFeatures(spaceToken, Utils.gateUrl,inputModel);
			String[] parsed = Utils.parseOffsetStringResource(spaceToken);
			gateDocument.getAnnotations().add(new Long(Long.parseLong(parsed[1])), new Long(Long.parseLong(parsed[2])), "SpaceToken", tokParams);
		}
		
		return gateDocument;
	}
	*/
	
	/*
	static gate.Document modelToGateDocument(OntModel inputModel, Resource documentResource, String text) throws ResourceInstantiationException, NumberFormatException, InvalidOffsetException {
		gate.Document gateDocument = Factory.newDocument(text);
		//log.info("text: "+text.length()+text);	
		List<String> annotationTypes = new ArrayList<String>();
		
		NodeIterator iter = inputModel.listObjectsOfProperty(RDF.type);
		//if(!iter.hasNext())log.error("AAA");
		RDFNode node = null;
		while (iter.hasNext()) {
			node = iter.next();
			//log.info("@@@: "+node.asResource());
			if(node.isURIResource()){
				if(node.asResource().getURI().contains(Utils.oliaOntology)){
					//log.info("@@@3333: "+node.asResource());
					String[] split = node.asResource().getURI().split("#");
					String annotationType = split[split.length-1];
					annotationTypes.add(annotationType);
				}
			}
		}
		
		
		if(annotationTypes.isEmpty()){
			log.info("No GATE related annotations in "+documentResource);
		}
		
		String documentNs =Utils.getDocumentNs(documentResource);
		
		for(String annotationType : annotationTypes){
			log.info("annotationType: "+annotationType+RDF2Gate.OLIA2GATE.get(annotationType));
			ResIterator tokenIter = inputModel.listSubjectsWithProperty(RDF.type, inputModel.getResource(Utils.oliaOntology + annotationType));
			Resource token = null;
			while (tokenIter.hasNext()) {
				token = tokenIter.next();
				FeatureMap tokParams = Utils.rdf2gateFeatures(token, Utils.gateUrl,inputModel);
				
				String[] parsed = Utils.parseOffsetStringResource(token);
				
				String docNs = Utils.getDocumentNs(token);
				if(docNs.equals(documentNs)){
					gateDocument.getAnnotations().add(
							new Long(Long.parseLong(parsed[1])), 
							new Long(Long.parseLong(parsed[2])), 
							RDF2Gate.OLIA2GATE.get(annotationType), tokParams);					
				}
				
			}
		}
		
		return gateDocument;
	}*/
	
	static FeatureMap  rdf2gateFeatures(Resource resource, String ns, OntModel model) {
		FeatureMap features = Factory.newFeatureMap();

		StmtIterator tokenIter = resource.listProperties();
		while (tokenIter.hasNext()) {
			Statement stmt = tokenIter.next();
			if(!stmt.getPredicate().getNameSpace().equals(Utils.gateUrl))continue;
			String feature = stmt.getPredicate().getLocalName();
			features.put(feature, stmt.getObject().toString());
		}	
		return features;
	}
	
	static void getSubstrings(Resource offsetBasedString, List<Resource> substrings, Model model) {
		Property p = model.getProperty("http://nlp2rdf.lod2.eu/schema/string/subString");
		NodeIterator iter = model.listObjectsOfProperty(offsetBasedString, p);// System.out.println("@2");
		while (iter.hasNext()) {
			RDFNode node = iter.next();// System.out.println(node.asResource());
			substrings.add(node.asResource());
			getSubstrings(node.asResource(), substrings, model);
		}
	}

	

	static String mapAsString(Map<Resource, List<Resource>> map) {
		StringBuilder sb = new StringBuilder();
		for (Entry<Resource, List<Resource>> e : map.entrySet()) {
			sb.append(e.getKey() + "\n");
			for (Resource r : e.getValue()) {
				sb.append("\t" + r + "\n");
			}
		}
		return sb.toString();
	}

	static Map<Resource, List<Resource>> getCollectionOfDocuments(Model model) {
		Map<Resource, List<Resource>> collection = new HashMap<Resource, List<Resource>>();
		Resource documentConcept = model.getResource("http://nlp2rdf.lod2.eu/schema/string/Document");
		ResIterator iter = model.listResourcesWithProperty(RDF.type, documentConcept);
		while (iter.hasNext()) {// System.out.println("@1");
			Resource document = iter.next();
			List<Resource> substrings = new ArrayList<Resource>();
			getSubstrings(document, substrings, model);
			collection.put(document, substrings);
		}
		return collection;
	}

	/**
	 * TODO change: "offset_" is a split mark
	 * @param offsetBasedString
	 * @return
	 */
	static String getDocumentNs(Resource offsetBasedString){
		if(offsetBasedString.getURI().contains("#")){
			return offsetBasedString.getURI().split("#")[0];
		}else{
			return offsetBasedString.getURI();
		}
	}
	
	
	static void saveModel(OntModel model, String fileName){
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName)));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		model.write(out, "RDF/XML");
		try {
			out.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}
	 /*
	public static void xml2rdf(String prefix, String xmlAsText, URIGenerator urigenerator, Document doc, OntModel model) throws FileNotFoundException, GateException, IOException, URISyntaxException {
		System.out.println("Initializing GATE ...");
		initGate();
		System.out.println("Initializing GATE DONE");
		//set basic prefixes
        model.setNsPrefix("sso", structuredSentenceOntologyUrl);
        model.setNsPrefix("str", stringOntologyUrl);
        model.setNsPrefix("scms", scmsUrl);
        model.setNsPrefix("ebi", ebiUrl);
     
        // create required resources
		Resource ebiAnnotation = model.createResource(ebiUrl+"Annotation");
		Resource ebiSentence = model.createResource(ebiUrl+"Sentence");
		Resource ebiWord = model.createResource(ebiUrl+"Word");
		Resource ebiDrug = model.createResource(ebiUrl+"Drug");		
		Property smcsMeans = model.createProperty(scmsUrl+"means");
		
		//*
		// * This part may be replaced with Gate2RDF
		// *
        gate.Document document =  (gate.Document)gate.Factory.createResource("gate.corpora.DocumentImpl",
        		 gate.Utils.featureMap(gate.Document.DOCUMENT_STRING_CONTENT_PARAMETER_NAME,
        		 xmlAsText,
        		 gate.Document.DOCUMENT_MIME_TYPE_PARAMETER_NAME, "text/xml")); 
    

       // System.out.println(document);
		String content = document.getContent().toString();	

		AnnotationSet originals = document.getAnnotations("Original markups"); 
		AnnotationSet sentences = originals.get("SENT", new TreeSet());
		for (Annotation sentenceAnn : sentences) {

			Sentence sentence =  new Text2RDF().createStringAnnotationForClass(
					Sentence.class, 
					prefix, 
					content, 
					new Span(sentenceAnn.getStartNode().getOffset().intValue(), sentenceAnn.getEndNode().getOffset().intValue()), 
					urigenerator, 
					model);
			
			
            //assign str:substring to document
            if (doc != null) {
                doc.addSubString(sentence);
            }
            
           sentence.addProperty(model.getProperty(rdfNs+"type"), ebiAnnotation);
           sentence.addProperty(model.getProperty(rdfNs+"type"), ebiSentence);
           
           
           // add drugs
			AnnotationSet drugMentions = originals.get("z:drug",sentenceAnn.getStartNode().getOffset(),sentenceAnn.getEndNode().getOffset());
			for(Annotation drugMentionAnn : drugMentions){
				Long s = drugMentionAnn.getStartNode().getOffset();
				Long e = drugMentionAnn.getEndNode().getOffset();
				String drugMentionString = document.getContent().getContent(s, e).toString().toLowerCase();						
				log.debug(drugMentionString);
								
				OffsetBasedString word =  new Text2RDF().createStringAnnotationForClass(
						OffsetBasedString.class, 
						prefix, 
						content, 
						new Span(s.intValue(), e.intValue()), 
						urigenerator, 
						model);
				word.addProperty(model.getProperty(rdfNs+"type"), ebiAnnotation);
				//word.addProperty(model.getProperty(rdfNs+"type"), ebiWord);
				word.addProperty(model.getProperty(rdfNs+"type"), ebiDrug);
				
				FeatureMap features = drugMentionAnn.getFeatures();
				gateFeaturesToRdf(word, features, ebiUrl, model);
				
				//sentence.addWord(word);
				sentence.addSubString(word);
			}		
            
		}	
	
		if (eu.lod2.nlp2rdf.schema.str.Document.list(model).isEmpty()) {
			doc = new Text2RDF().createDocumentAnnotation(prefix, content, urigenerator, model);
		}				
		
		// optional
		// addNextAndPreviousProperties(prefix, content, urigenerator, model);
	}*/
}
