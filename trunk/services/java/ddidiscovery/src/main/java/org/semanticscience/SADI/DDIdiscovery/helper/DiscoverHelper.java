/**
 * Copyright (c) 2012 Jose Cruz-Toledo
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.semanticscience.SADI.DDIdiscovery.helper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class DiscoverHelper {
	private Model inputModel;
	private static Model outputModel;
	private String base;
	private static final String service = "http://cu.pharmgkb.bio2rdf.org/sparql";

	public DiscoverHelper(Model anInputModel, Model anOutputModel) {
		this();
		inputModel = anInputModel;
		outputModel = anOutputModel;

	}

	public DiscoverHelper() {
		inputModel = ModelFactory.createDefaultModel();
		outputModel = ModelFactory.createDefaultModel();
		base = RdfUtils.createUniqueURI();
	}

	public String parseDDIs(File aCSVFile) {
		CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(aCSVFile));
			String[] nextLine;
			String returnMe = "";
			try {
				while ((nextLine = reader.readNext()) != null) {
					// nextLine[] is an array of values from the line
					// check if line is a data line
					if (nextLine[0].matches("\\DB\\d+")) {
						returnMe += nextLine[0] + nextLine[1] + "\n";
					} else {
						continue;
					}
				}
				return returnMe;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		return null;
	}

	public static List<String> getDDIUris() {
		List<String> rm = new ArrayList<String>();
		String service = "http://cu.pharmgkb.bio2rdf.org/sparql";

		String query = "select DISTINCT ?ddi where {"
				+ "?ddi <http://bio2rdf.org/pharmgkb_vocabulary:chemical> ?pcc ."
				+ "?ddi <http://bio2rdf.org/pharmgkb_vocabulary:chemical> ?pcc2 ."
				+ "?pcc <http://bio2rdf.org/pubchemcompound_vocabulary:has_tautomer> ?taut ."
				+ "?dbc <http://bio2rdf.org/drugbank_vocabulary:xref> ?taut ."
				+ "?pcc2 <http://bio2rdf.org/pubchemcompound_vocabulary:has_tautomer> ?taut2 ."
				+ "?dbc2 <http://bio2rdf.org/drugbank_vocabulary:xref> ?taut2 ."
				+ "FILTER(?pcc != ?pcc2) ." + "FILTER(?dbc != ?dbc2) .}";

		QueryExecution qe = QueryExecutionFactory.sparqlService(service, query);
		ResultSet rs = qe.execSelect();
		while (rs.hasNext()) {
			QuerySolution qs = rs.next();
			rm.add(qs.get("ddi").toString());
		}
		return rm;
	}

	public static List<DrugDrugInteraction> findDDIInEndpoint(Resource aR) {
		// get the input id
		String inputId = DiscoverHelper.getChemicalIdentifier(aR,
				Vocabulary.SIO_000008.toString(),
				Vocabulary.SIO_000300.toString());
		return findDDIInEndpoint(inputId);
	}

	public static List<DrugDrugInteraction> findDDIInEndpoint(String dbId) {
		ArrayList<DrugDrugInteraction> rm = new ArrayList<DrugDrugInteraction>();
		String uri = "<http://bio2rdf.org/drugbank:" + dbId + ">";
		String query = "select DISTINCT ?inputLabel ?dbc2 ?dbc2Label ?umlsEvent ?umlsLabel where {"
				+ uri
				+ "<http://bio2rdf.org/drugbank_vocabulary:xref> ?pcc."
				+ "?taut <http://bio2rdf.org/pubchemcompound_vocabulary:has_tautomer> ?pcc."
				+ "?taut <http://www.w3.org/2000/01/rdf-schema#label> ?inputLabel."
				+ "?ddi <http://bio2rdf.org/pharmgkb_vocabulary:chemical> ?taut."
				+ "?ddi a <http://bio2rdf.org/pharmgkb_vocabulary:DDI>."
				+ "?ddi <http://bio2rdf.org/pharmgkb_vocabulary:chemical> ?pcc2."
				+ "?ddi <http://bio2rdf.org/pharmgkb_vocabulary:event> ?umlsEvent."
				+ "?umlsEvent <http://www.w3.org/2000/01/rdf-schema#label> ?umlsLabel."
				+ "?pcc2 <http://bio2rdf.org/pubchemcompound_vocabulary:has_tautomer> ?taut2."
				+ "?dbc2 <http://bio2rdf.org/drugbank_vocabulary:xref> ?taut2."
				+ "?taut2 <http://www.w3.org/2000/01/rdf-schema#label> ?dbc2Label.}";
		QueryExecution qe = QueryExecutionFactory.sparqlService(service, query);
		ResultSet rs = qe.execSelect();
		while (rs.hasNext()) {
			QuerySolution qs = rs.next();
			try {
				DrugDrugInteraction ddi = new DrugDrugInteraction(dbId, qs.get(
						"dbc2").toString(), new URL(qs.get("umlsEvent")
						.toString()), qs.get("umlsLabel").toString(), qs.get(
						"inputLabel").toString(), qs.get("dbc2Label")
						.toString(), false);
				rm.add(ddi);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		return rm;
	}

	public static List<String> queryGenerator(List<String> ddiURIs) {
		ArrayList<String> rm = new ArrayList<String>();
		Iterator<String> itr = ddiURIs.iterator();
		while (itr.hasNext()) {
			String ddi = itr.next();
			String query = "select DISTINCT ?dbc ?drugLabel ?dbc2 ?drug2Label ?umlsEvent ?umlsEventLabel where {"
					+ "<"
					+ ddi
					+ "> <http://bio2rdf.org/pharmgkb_vocabulary:chemical> ?pcc ."
					+ "<"
					+ ddi
					+ "> <http://bio2rdf.org/pharmgkb_vocabulary:chemical> ?pcc2 ."
					+ "<"
					+ ddi
					+ "> <http://bio2rdf.org/pharmgkb_vocabulary:event> ?umlsEvent ."
					+ "?umlsEvent <http://www.w3.org/2000/01/rdf-schema#label> ?umlsEventLabel ."
					+ "?pcc <http://bio2rdf.org/pubchemcompound_vocabulary:has_tautomer> ?taut ."
					+ "?dbc <http://bio2rdf.org/drugbank_vocabulary:xref> ?taut ."
					+ "?pcc <http://www.w3.org/2000/01/rdf-schema#label> ?drugLabel ."
					+ "?pcc2 <http://bio2rdf.org/pubchemcompound_vocabulary:has_tautomer> ?taut2 ."
					+ "?dbc2 <http://bio2rdf.org/drugbank_vocabulary:xref> ?taut2 ."
					+ "?pcc2 <http://www.w3.org/2000/01/rdf-schema#label> ?drug2Label ."
					+ "FILTER(?pcc != ?pcc2) ." + "FILTER(?dbc != ?dbc2) .}";
			rm.add(query);
		}

		return rm;
	}

	public static void writeDDICSV(List<String> ddiURIs) {

		String service = "http://cu.pharmgkb.bio2rdf.org/sparql";

		Iterator<String> itr = ddiURIs.iterator();
		while (itr.hasNext()) {
			String ddi = itr.next();
			String query = "select DISTINCT ?dbc ?drugLabel ?dbc2 ?drug2Label ?umlsEvent ?umlsEventLabel where {"
					+ "<"
					+ ddi
					+ "> <http://bio2rdf.org/pharmgkb_vocabulary:chemical> ?pcc ."
					+ "<"
					+ ddi
					+ "> <http://bio2rdf.org/pharmgkb_vocabulary:chemical> ?pcc2 ."
					+ "<"
					+ ddi
					+ "> <http://bio2rdf.org/pharmgkb_vocabulary:event> ?umlsEvent ."
					+ "?umlsEvent <http://www.w3.org/2000/01/rdf-schema#label> ?umlsEventLabel ."
					+ "?pcc <http://bio2rdf.org/pubchemcompound_vocabulary:has_tautomer> ?taut ."
					+ "?dbc <http://bio2rdf.org/drugbank_vocabulary:xref> ?taut ."
					+ "?pcc <http://www.w3.org/2000/01/rdf-schema#label> ?drugLabel ."
					+ "?pcc2 <http://bio2rdf.org/pubchemcompound_vocabulary:has_tautomer> ?taut2 ."
					+ "?dbc2 <http://bio2rdf.org/drugbank_vocabulary:xref> ?taut2 ."
					+ "?pcc2 <http://www.w3.org/2000/01/rdf-schema#label> ?drug2Label ."
					+ "FILTER(?pcc != ?pcc2) ." + "FILTER(?dbc != ?dbc2) .}";

			QueryExecution qe = QueryExecutionFactory.sparqlService(service,
					query);
			ResultSet rs = qe.execSelect();

			while (rs.hasNext()) {
				QuerySolution qs = rs.next();
				System.out.println(qs.get("dbc") + "," + qs.get("drugLabel")
						+ "," + qs.get("dbc2") + "," + qs.get("drug2Label")
						+ "," + qs.get("umlsEvent") + ","
						+ qs.get("umlsEventLabel"));
			}
		}
	}

	public static DrugDrugInteraction getDDIsFromEndpoint(String serviceUrl,
			String query) {
		DrugDrugInteraction rm = new DrugDrugInteraction();
		QueryExecution qe = QueryExecutionFactory.sparqlService(serviceUrl,
				query);
		ResultSet rs = qe.execSelect();
		// get only the first result and create a DDI object from it and add it
		// to rm
		while (rs.hasNext()) {
			QuerySolution qs = rs.next();

			String actorLabel = qs.get("drugLabel").toString();
			String[] actorIdtmp = qs.get("dbc").toString().split(":");
			String actorDBId = actorIdtmp[1];
			String targetLabel = qs.get("drug2Label").toString();
			String[] targetDbtmp = qs.get("dbc2").toString().split(":");
			String targetDBId = targetDbtmp[1];
			String resultingConditionLabel = qs.get("umlsEventLabel")
					.toString();
			URL resultingConditionURL = null;
			try {
				resultingConditionURL = new URL(qs.get("umlsEvent").toString());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			DrugDrugInteraction aDDI = new DrugDrugInteraction(actorDBId,
					targetDBId, resultingConditionURL, resultingConditionLabel,
					actorLabel, targetLabel, false);
			rm = aDDI;
			break;
		}
		return rm;
	}

	public static Resource createPublicationResource(Model aM, String aPmid) {
		Resource pub = aM.createResource(RdfUtils.createUniqueURI());
		pub.addProperty(Vocabulary.rdftype, Vocabulary.SIO_000087);
		// create a PMID_identifier resource
		Resource pmidIdRes = aM.createResource(RdfUtils.createUniqueURI());
		// add the value of the pmid to the pmidres
		pmidIdRes.addProperty(Vocabulary.SIO_000300, aPmid);
		pmidIdRes.addProperty(Vocabulary.rdftype, Vocabulary.PMID_Identifier);
		// add the PMID_identifier attribute
		pub.addProperty(Vocabulary.SIO_000008, pmidIdRes);
		return pub;
	}

	public static ResultSet executeQuery(String aTwoSidesId) {

		Query q = QueryFactory.create(aTwoSidesId);
		// QueryExecution qe = QueryExecutionFactory.create(q, )
		ResultSet results;
		return null;

	}

	/**
	 * Create a chemical entity that has some pharmgkb identifier resource
	 * 
	 * @param anId
	 * @return
	 */
	public static Resource createResourceFromPharmGKBId(Model aM, String anId) {
		Resource chemicalEntity = aM.createResource(RdfUtils.createUniqueURI());
		chemicalEntity.addProperty(Vocabulary.rdftype, Vocabulary.SIO_010004);
		Resource chemicalIdentifier = aM.createResource(RdfUtils
				.createUniqueURI());
		chemicalIdentifier
				.addProperty(Vocabulary.rdftype, Vocabulary.DDI_00011);
		chemicalIdentifier.addProperty(Vocabulary.SIO_000300, anId);
		chemicalEntity.addProperty(Vocabulary.SIO_000008, chemicalIdentifier);
		return chemicalEntity;
	}

	public static ArrayList<DrugDrugInteraction> findDDISInCSVFile(
			InputStream is, Resource input) {
		CSVReader r;
		ArrayList<DrugDrugInteraction> rM = new ArrayList<DrugDrugInteraction>();
		String inputId = getChemicalIdentifier(input,
				Vocabulary.SIO_000008.toString(),
				Vocabulary.SIO_000300.toString());
		if (validatePharmgkbIdentifier(inputId)) {
			r = new CSVReader(new InputStreamReader(is));
			String[] nextLine;
			try {
				String actorId = "";
				String actorLabel = "";
				String targetId = "";
				String targetLabel = "";
				String actorEffectOnTarget = "";
				String targetEffectOnActor = "";
				String resultingCondition = "";
				boolean isDirected = false;
				String pmid = "";
				String description = "";

				while ((nextLine = r.readNext()) != null) {
					if (nextLine[0].equalsIgnoreCase(inputId)
							|| nextLine[2].equalsIgnoreCase(inputId)) {
						actorId = nextLine[0];
						actorLabel = nextLine[1];
						targetLabel = nextLine[3];
						targetId = nextLine[2];
						actorEffectOnTarget = nextLine[5];
						targetEffectOnActor = nextLine[6];
						resultingCondition = nextLine[8];
						pmid = nextLine[9];
						description = nextLine[7];
						// check if the ddi is directed
						if (nextLine[12].equalsIgnoreCase("TRUE")) {
							isDirected = true;
						}
						// DrugDrugInteraction
						DrugDrugInteraction ddi = new DrugDrugInteraction(
								actorId, targetId, actorEffectOnTarget,
								targetEffectOnActor, resultingCondition, pmid,
								description, actorLabel, targetLabel,
								isDirected);

						if (!rM.contains(ddi)) {
							rM.add(ddi);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return rM;
	}

	/**
	 * Get the string representation of the input chebi id. First find the
	 * instance of the attribute class using "hasAttribute" and then get the
	 * value using "hasValue"
	 * 
	 * @param someResource
	 *            the input resource
	 * @param hasAttribute
	 *            a string representation of the has attribute property
	 * @param hasValue
	 *            a string representation of the has value property
	 * @return the name of the input resource
	 */
	public static String getChemicalIdentifier(Resource someResource,
			String hasAttribute, String hasValue) {
		Model m = someResource.getModel();
		Property hasAttr = m.getProperty(hasAttribute);
		Property hasVal = m.getProperty(hasValue);
		Statement st = m.getProperty(someResource, hasAttr);
		RDFNode attribute = st.getObject();
		Resource r = attribute.asResource();
		Statement st2 = m.getProperty(r, hasVal);
		RDFNode value = st2.getObject();
		String returnMe = value.asLiteral().getString();
		return returnMe;
	}

	/**
	 * Returns true if the identifier is valid
	 * 
	 * @param anId
	 * @return
	 */
	public static boolean validatePharmgkbIdentifier(String anId) {
		return anId.matches("DB\\d+");
	}

	/**
	 * Get the base of a URL
	 * 
	 * @param aUrl
	 *            some URL
	 * @return the base of aUrl
	 */
	public String getBaseURL(URL aUrl) {
		String returnMe = "";
		returnMe += "http://" + aUrl.getHost() + "/";
		return returnMe;
	}

	public Model getInputModel() {
		return inputModel;
	}

	public void setInputModel(Model inputModel) {
		this.inputModel = inputModel;
	}

	public Model getOutputModel() {
		return outputModel;
	}

	public void setOutputModel(Model outputModel) {
		this.outputModel = outputModel;
	}

	@SuppressWarnings("unused")
	public static final class Vocabulary {
		private static Model m_model = ModelFactory.createDefaultModel();
		public static Property rdftype = m_model
				.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		public static final Resource DDI_00000 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00000");
		public static final Property SIO_000300 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000300");
		public static final Property SIO_000008 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000008");
		public static final Resource SIO_000087 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000087");
		public static final Resource PMID_Identifier = m_model
				.createResource("http://purl.oclc.org/SADI/LSRN/PMID_Identifier");

		public static final Resource SIO_000728 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000728");
		public static final Resource SIO_010004 = m_model
				.createResource("http://semanticscience.org/resource/SIO_010004");
		public static final Resource DDI_00011 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00011");
		public static final Property SIO_000132 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000132");

	}

}
