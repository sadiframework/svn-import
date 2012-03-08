package org.semanticscience.SADI.DDIdiscovery.helper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

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

	/**
	 * This method will parse the DDI CSV file in search for all interacting
	 * ids. It will return an array of chemical entities that have a pharmgkb
	 * identifier that interact with the input id
	 * 
	 * @param aCSVFile
	 * @param aQuery
	 * @return
	 */
	public ArrayList<Resource> findInteractingIDs(File aCSVFile, String aQuery) {
		CSVReader r;
		ArrayList<Resource> returnMe = new ArrayList<Resource>();
		ArrayList<String> ids = new ArrayList<String>();
		if (this.validatePharmgkbIdentifier(aQuery)) {
			try {
				r = new CSVReader(new FileReader(aCSVFile));
				String[] nextLine;
				try {
					while ((nextLine = r.readNext()) != null) {
						if (nextLine[0].equalsIgnoreCase(aQuery)) {
							// check if the Id was already in returnMe
							if (!ids.contains(nextLine[2])) {
								ids.add(nextLine[2]);
							}
						}
						if (nextLine[2].equalsIgnoreCase(aQuery)) {
							// check if the Id was already in returnMe
							if (!ids.contains(nextLine[0])) {
								ids.add(nextLine[0]);
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			// now convert the interacting ids to chemical entities that have
			// some pharmgkb identifier
			Iterator<String> itr = ids.iterator();
			while (itr.hasNext()) {
				String anId = itr.next();
				if (validatePharmgkbIdentifier(anId)) {
					returnMe.add(this.createResourceFromPharmGKBId(anId));
				}
			}
		}
		return returnMe;
	}

	/**
	 * This method will parse the DDI CSV file in search for all interacting
	 * ids. It will return an array of chemical entities that have a pharmgkb
	 * identifier that interact with the input id
	 * 
	 * @param aCSVFile
	 * @param aQuery
	 * @return
	 */
	/*public ArrayList<DrugDrugInteraction> findDDIs(File aCSVFile,
			Resource aQuery) {
		CSVReader r;
		ArrayList<DrugDrugInteraction> rM = new ArrayList<DrugDrugInteraction>();
		String inputId = getChemicalIdentifier(aQuery,
				Vocabulary.SIO_000008.toString(),
				Vocabulary.SIO_000300.toString());

		if (this.validatePharmgkbIdentifier(inputId)) {
			try {
				r = new CSVReader(new FileReader(aCSVFile));
				String[] nextLine;
				try {
					String drugB = "";
					String drugAEffectOnDrugB = "";
					String resultngCondition = "";
					String pmid = "";
					String description = "";

					while ((nextLine = r.readNext()) != null) {
						if (nextLine[0].equalsIgnoreCase(inputId)) {
							drugB = nextLine[2];
							drugAEffectOnDrugB = nextLine[5];
							resultngCondition = nextLine[8];
							pmid = nextLine[9];
							description = nextLine[7];
							DrugDrugInteraction ddi = new DrugDrugInteraction(
									aQuery, drugB, drugAEffectOnDrugB,
									resultngCondition, pmid, description);
							if (!rM.contains(ddi)) {
								rM.add(ddi);
							}
						}
						if (nextLine[2].equalsIgnoreCase(inputId)) {
							drugB = nextLine[0];
							drugAEffectOnDrugB = nextLine[6];
							resultngCondition = nextLine[8];
							pmid = nextLine[9];
							description = nextLine[7];
							DrugDrugInteraction ddi = new DrugDrugInteraction(
									aQuery, drugB, drugAEffectOnDrugB,
									resultngCondition, pmid, description);
							if (!rM.contains(ddi)) {
								rM.add(ddi);
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

		}

		return rM;
	}*/

	public ArrayList<DrugDrugInteraction> findDDIs(InputStream is,
			Resource input) {
		CSVReader r;
		ArrayList<DrugDrugInteraction> rM = new ArrayList<DrugDrugInteraction>();
		String inputId = getChemicalIdentifier(input,
				Vocabulary.SIO_000008.toString(),
				Vocabulary.SIO_000300.toString());

		if (this.validatePharmgkbIdentifier(inputId)) {
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

				while ((nextLine = r.readNext()) != null) {
					if (nextLine[0].equalsIgnoreCase(inputId)) {
						drugAlabel = nextLine[1];
						drugBlabel = nextLine[3];
						drugB = nextLine[2];
						drugBEffectOnDrugA = nextLine[6];
						resultngCondition = nextLine[8];
						pmid = nextLine[9];
						description = nextLine[7];
						DrugDrugInteraction ddi = new DrugDrugInteraction(
								input, drugB, drugBEffectOnDrugA,
								resultngCondition, pmid, description, drugAlabel, drugBlabel);
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
						DrugDrugInteraction ddi = new DrugDrugInteraction(
								input, drugB, drugBEffectOnDrugA,
								resultngCondition, pmid, description, drugAlabel, drugBlabel);
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
	public String getChemicalIdentifier(Resource someResource,
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
	public boolean validatePharmgkbIdentifier(String anId) {
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
