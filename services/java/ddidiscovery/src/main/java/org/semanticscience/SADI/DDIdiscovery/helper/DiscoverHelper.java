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
import java.net.URL;
import java.util.ArrayList;

import au.com.bytecode.opencsv.CSVReader;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class DiscoverHelper {
	private Model inputModel;
	private Model outputModel;
	private String base;

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

	/**
	 * Create a chemical entity that has some pharmgkb identifier resource
	 * 
	 * @param anId
	 * @return
	 */
	public Resource createResourceFromPharmGKBId(String anId) {
		Resource chemicalEntity = outputModel.createResource(RdfUtils
				.createUniqueURI());
		chemicalEntity.addProperty(Vocabulary.rdftype, Vocabulary.SIO_010004);
		Resource chemicalIdentifier = outputModel.createResource(RdfUtils
				.createUniqueURI());
		chemicalIdentifier
				.addProperty(Vocabulary.rdftype, Vocabulary.DDI_00011);
		chemicalIdentifier.addProperty(Vocabulary.SIO_000300, anId);
		chemicalEntity.addProperty(Vocabulary.SIO_000008, chemicalIdentifier);
		return chemicalEntity;
	}

	public static ArrayList<DrugDrugInteraction> findDDIs(InputStream is,
			Resource input) {
		CSVReader r;
		ArrayList<DrugDrugInteraction> rM = new ArrayList<DrugDrugInteraction>();
		String inputId = getChemicalIdentifier(input,
				Vocabulary.SIO_000008.toString(),
				Vocabulary.SIO_000300.toString());

		if (validatePharmgkbIdentifier(inputId)) {
			r = new CSVReader(new InputStreamReader(is));
			String[] nextLine;
			try {
				String drugB = "";
				String drugBEffectOnDrugA = "";
				String resultngCondition = "";
				String pmid = "";
				String description = "";
				String drugAlabel ="";
				String drugBlabel= "";
				// if true this ddi is directed between drug A and drug B
				boolean drugADirectedB = false;
				//if true this ddi is directed between drug B and drug A
				boolean drugBDirectedA = false;

				while ((nextLine = r.readNext()) != null) {
					if (nextLine[0].equalsIgnoreCase(inputId)) {
						drugAlabel = nextLine[1];
						drugBlabel = nextLine[3];
						drugB = nextLine[2];
						drugBEffectOnDrugA = nextLine[6];
						resultngCondition = nextLine[8];
						pmid = nextLine[9];
						description = nextLine[7];
						//if ddi direction is AtoB
						if(nextLine[12].equalsIgnoreCase("Y") && nextLine[13].equalsIgnoreCase("N")){
							drugADirectedB = true;
							drugBDirectedA = false;
						}else if(nextLine[12].equalsIgnoreCase("N") && nextLine[13].equalsIgnoreCase("Y")){
							//if ddi direction is BtoA
							drugADirectedB = false;
							drugBDirectedA = true;
						}
						DrugDrugInteraction ddi = new DrugDrugInteraction(
								input, drugB, drugBEffectOnDrugA,
								resultngCondition, pmid, description, drugAlabel, drugBlabel, drugADirectedB, drugBDirectedA);
						if (!rM.contains(ddi)) {
							rM.add(ddi);
						}
					}
					if (nextLine[2].equalsIgnoreCase(inputId)) {
						drugAlabel = nextLine[3];
						drugBlabel = nextLine[1];
						drugB = nextLine[0];
						drugBEffectOnDrugA = nextLine[6];
						resultngCondition = nextLine[8];
						pmid = nextLine[9];
						description = nextLine[7];
						//if ddi direction is AtoB
						if(nextLine[12].equalsIgnoreCase("Y") && nextLine[13].equalsIgnoreCase("N")){
							drugADirectedB = true;
							drugBDirectedA = false;
						}else if(nextLine[12].equalsIgnoreCase("N") && nextLine[13].equalsIgnoreCase("Y")){
							//if ddi direction is BtoA
							drugADirectedB = false;
							drugBDirectedA = true;
						}
						DrugDrugInteraction ddi = new DrugDrugInteraction(
								input, drugB, drugBEffectOnDrugA,
								resultngCondition, pmid, description, drugAlabel, drugBlabel, drugADirectedB, drugBDirectedA );
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
