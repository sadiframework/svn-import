package ca.unbsj.cbakerlab.nlp2rdf.gate;

import gate.Factory;
import gate.util.GateException;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class RDF2Gate {
	
    private static Logger log = LoggerFactory.getLogger(RDF2Gate.class);

    static int count = 0;
    
    
	public static final Map<String,String> OLIA2GATE = createOLIA2GATEMap();	

    private static Map<String, String> createOLIA2GATEMap() {
        Map<String, String> result = new HashMap<String, String>();
        result.put(Utils.oliaOntology+"Token", "Token");
        result.put(Utils.oliaOntology+"SpaceToken", "SpaceToken");
        result.put(Utils.oliaOntology+"Sentence", "Sentence");
        result.put(Utils.oliaOntology+"Split", "Split");
        result.put(Utils.oliaSystemOntology+"hasTag", "category");
        return Collections.unmodifiableMap(result);
    }
    
    
	public static gate.Document rdf2gate(Model model, Resource documentResource) throws FileNotFoundException, GateException, IOException, URISyntaxException {
		Resource document = documentResource;

		String content = model.listObjectsOfProperty(document, model.getProperty(Utils.stringOntologyUrl + "sourceString")).next().toString();
		gate.Document gateDoc = (gate.Document) Factory.newDocument(content);
		gateDoc.setName(document.getURI());

		List<Resource> substrings = new ArrayList<Resource>();
		Utils.getSubstrings(document, substrings, model);
		
		
		for (Resource r : substrings) {
			String[] parseDocumentUri = Utils.parseOffsetStringResource(r);
			StmtIterator iter = r.listProperties(RDF.type);
			while (iter.hasNext()) {
				Statement s = iter.next();
				gate.FeatureMap features = Factory.newFeatureMap();
				
				if(OLIA2GATE.containsKey(s.getObject().toString())) {
					StmtIterator iter2 = r.listProperties();

					while (iter2.hasNext()) {
						Statement s2 = iter2.next();
						//System.err.println(s2.getPredicate().getNameSpace());
						//System.err.println(s2.getPredicate().getLocalName());
						
						/*if(s2.getPredicate().toString().startsWith(Utils.gateUrl)) {
							features.put(s2.getPredicate().getLocalName(), s2.getObject().toString());
						}*/
						
						if(s2.getPredicate().toString().startsWith(Utils.oliaOntology) ||
								s2.getPredicate().toString().startsWith(Utils.pennOntology)) {
							features.put(OLIA2GATE.get(s2.getPredicate().toString()), s2.getObject().toString());
						}else if (s2.getPredicate().toString().startsWith(Utils.gateUrl)){
							features.put(s2.getPredicate().getLocalName(), s2.getObject().toString());
						}
					}
					gateDoc.getAnnotations().add(
							new Long(parseDocumentUri[1]), 
							new Long(parseDocumentUri[2]),
							OLIA2GATE.get( s.getObject().toString()), 
							features);
					
				}
				
				
			}
		}
		return gateDoc;
	}

    
	public static Collection<gate.Document> rdf2gateDocuments(Model model) throws FileNotFoundException, GateException, IOException, URISyntaxException {
		List<gate.Document> collectionOfGateDocuments = new ArrayList<gate.Document>();
		Utils.initGate();

		Map<Resource, List<Resource>> collectionOfDocuments = Utils.getCollectionOfDocuments(model);
		log.debug(Utils.mapAsString(collectionOfDocuments));

		for (Entry<Resource, List<Resource>> e : collectionOfDocuments.entrySet()) {			
			gate.Document gateDoc = rdf2gate(model, e.getKey());
			collectionOfGateDocuments.add(gateDoc);
		}

		return collectionOfGateDocuments;
	}


	
	
	
	
	
	
	
	/**
	 * Used for representeation of original (rdf) annotations in GATE XML. Called by rdf2gateXmlOriginal method.
	 * @param model
	 * @return
	 * @throws FileNotFoundException
	 * @throws GateException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private static Collection<gate.Document> rdf2gateOriginal(Model model) throws FileNotFoundException, GateException, IOException, URISyntaxException {
		List<gate.Document> collectionOfGateDocuments = new ArrayList<gate.Document>();
		Utils.initGate();

		Map<Resource, List<Resource>> collectionOfDocuments = Utils.getCollectionOfDocuments(model);
		log.debug(Utils.mapAsString(collectionOfDocuments));

		for (Entry<Resource, List<Resource>> e : collectionOfDocuments.entrySet()) {
			Resource document = (Resource) e.getKey();

			String content = model.listObjectsOfProperty(document, model.getProperty(Utils.stringOntologyUrl + "sourceString")).next().toString();
			gate.Document gateDoc = (gate.Document) Factory.newDocument(content);
			gateDoc.setName(document.getURI());

			for (Resource r : e.getValue()) {
				String[] parseDocumentUri = Utils.parseOffsetStringResource(r);
				StmtIterator iter = r.listProperties(RDF.type);
				//System.err.println(r);
				while (iter.hasNext()) {
					Statement s = iter.next();
					gate.FeatureMap features = Factory.newFeatureMap();
					
					StmtIterator iter2 = r.listProperties();
					
						while (iter2.hasNext()) {
							Statement s2 = iter2.next();
							features.put(s2.getPredicate().toString(), s2.getObject().toString());
						}
						gateDoc.getAnnotations().add(
								new Long(parseDocumentUri[1]), 
								new Long(parseDocumentUri[2]), 
								s.getObject().toString(), 
								features);				
				}
			}

			collectionOfGateDocuments.add(gateDoc);
		}

		return collectionOfGateDocuments;
	}


	static void rdf2gateXml(Model model, boolean preservingFormat) throws FileNotFoundException, GateException, IOException, URISyntaxException {
		
		for (gate.Document gateDocument : rdf2gateDocuments(model)) {
			//log.debug(gateDocument.toXml(gateDocument.getAnnotations(), true));
					
			FileWriter fstream = new FileWriter("document_" + (count++) + ".xml");
			BufferedWriter out = new BufferedWriter(fstream);
			if(preservingFormat){
				out.write(gateDocument.toXml(gateDocument.getAnnotations(), true));
			} else {
				out.write(gateDocument.toXml());
			}						
			out.close();	
		}
	}
	
	
	static void rdf2gateXmlOriginal(Model model, boolean preservingFormat) throws FileNotFoundException, GateException, IOException, URISyntaxException {
		
		for (gate.Document gateDocument : rdf2gateOriginal(model)) {
			//log.debug(gateDocument.toXml(gateDocument.getAnnotations(), true));
					
			FileWriter fstream = new FileWriter("document_" + (count++) + ".xml");
			BufferedWriter out = new BufferedWriter(fstream);
			if(preservingFormat){
				out.write(gateDocument.toXml(gateDocument.getAnnotations(), true));
			} else {
				out.write(gateDocument.toXml());
			}						
			out.close();	
		}
	}
	

}
