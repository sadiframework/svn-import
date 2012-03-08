/**
 * Copyright (c) 2012 Jose Cruz-Toledo, Ben Vandervalk
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
package org.semanticscience.SADI.DDIdiscovery;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.semanticscience.SADI.DDIdiscovery.helper.DiscoverHelper;
import org.semanticscience.SADI.DDIdiscovery.helper.DiscoverHelper.Vocabulary;
import org.semanticscience.SADI.DDIdiscovery.helper.DrugDrugInteraction;

import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.Description;
import ca.wilkinsonlab.sadi.service.annotations.InputClass;
import ca.wilkinsonlab.sadi.service.annotations.Name;
import ca.wilkinsonlab.sadi.service.annotations.OutputClass;
import ca.wilkinsonlab.sadi.service.simple.SimpleSynchronousServiceServlet;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

@Name("DrugDrugInteractionDiscovery")
@Description("This service takes in as input a 'chemical entity' that 'has attribute' some 'chemical identifier' and outputs an 'Annotated chemical entity' that 'is participant in' some 'drug drug interaction'")
@ContactEmail("josemiguelcruztoledo@gmail.com")
@InputClass("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_10000")
@OutputClass("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00005")
public class Discover extends SimpleSynchronousServiceServlet {
	private static final Logger log = Logger.getLogger(Discover.class);
	private static final long serialVersionUID = 1L;

	@Override
	public void processInput(Resource input, Resource output) {

		Model outputModel = output.getModel();
		// get the drugbank id from the input
		DiscoverHelper dh = new DiscoverHelper();
		InputStream is = Discover.class.getClassLoader().getResourceAsStream(
				"ddi.csv");
		
		ArrayList<DrugDrugInteraction> ddis = dh.findDDIs(is, input);
		Iterator<DrugDrugInteraction> itr = ddis.iterator();
		while (itr.hasNext()) {
			DrugDrugInteraction addi = itr.next();
			// create a ddi resource
			Resource ddir = outputModel.createResource(RdfUtils
					.createUniqueURI());
			ddir.addProperty(Vocabulary.rdftype, Vocabulary.DDI_00000);

			// add the participants
			Resource drugB = addi.createResourceFromDrugBankId(outputModel,
					addi.getDrugBId());
			// add the label of the drugs
			Statement stm = outputModel.createStatement(drugB, Vocab.rdfslabel,
					addi.getDrugBLabel());
			outputModel.add(stm);
			Statement stm2 = outputModel.createStatement(output,
					Vocab.rdfslabel, addi.getDrugALabel());
			outputModel.add(stm2);

			// directionality is ascribed in the predicate label
			ddir.addProperty(Vocab.SIO_000132, drugB);
			// connect drugA and drugB using the drugaeffectondrugB
			if (addi.getDrugBEffectOnDrugA().equalsIgnoreCase("DDI_00051")) {
				drugB.addProperty(Vocab.DDI_00051, output);
			} else if (addi.getDrugBEffectOnDrugA().equalsIgnoreCase(
					"DDI_00055")) {
				drugB.addProperty(Vocab.DDI_00055, output);
			} else if (addi.getDrugBEffectOnDrugA().equalsIgnoreCase(
					"DDI_00014")) {
				drugB.addProperty(Vocab.DDI_00014, output);
			}

			// directionality is ascribed by the use of special predicates
			// has_actor & has_target

			// directionality is ascribed through other typed resources

			// add the input as a participant
			ddir.addProperty(Vocab.SIO_000132, output);

			// create a publication resourcce
			Resource pub = outputModel.createResource(RdfUtils
					.createUniqueURI());
			pub.addProperty(Vocabulary.rdftype, Vocab.SIO_000087);
			// create a PMID_identifier resource
			Resource pmidIdRes = outputModel.createResource(RdfUtils
					.createUniqueURI());
			// add the value of the pmid to the pmidres
			pmidIdRes.addProperty(Vocab.SIO_000300, addi.getPmid());
			pmidIdRes.addProperty(Vocab.rdftype, Vocab.PMID_Identifier);
			// add the PMID_identifier attribute
			pub.addProperty(Vocab.SIO_000008, pmidIdRes);

			// connect the ddi with the publication
			ddir.addProperty(Vocab.SIO_000253, pub);

			// add the 'results in' class and label
			String resultingCondition = addi.getResultingCondition();
			if (resultingCondition.equalsIgnoreCase("DDI_00054")) {
				Statement stm3 = outputModel.createStatement(Vocab.DDI_00054,
						Vocab.rdfslabel, "decreased efficacy of drug");
				outputModel.add(stm3);
				ddir.addProperty(Vocab.SIO_000554, Vocab.DDI_00054);

			} else if (resultingCondition.equalsIgnoreCase("DDI_00007")) {
				Statement stm4 = outputModel.createStatement(Vocab.DDI_00007,
						Vocab.rdfslabel, "gastrointestinal bleeding");
				outputModel.add(stm4);
				ddir.addProperty(Vocab.SIO_000554, Vocab.DDI_00007);
			} else if (resultingCondition.equalsIgnoreCase("DDI_00061")) {
				Statement stm5 = outputModel.createStatement(Vocab.DDI_00061,
						Vocab.rdfslabel, "serotonin syndrome");
				outputModel.add(stm5);
			} else if (resultingCondition.equalsIgnoreCase("DDI_00050")) {
				Statement stm5 = outputModel.createStatement(Vocab.DDI_00050,
						Vocab.rdfslabel, "breakthrough bleeding");
				outputModel.add(stm5);
			} else if (resultingCondition.equalsIgnoreCase("DDI_00057")) {
				Statement stm5 = outputModel.createStatement(Vocab.DDI_00057,
						Vocab.rdfslabel, "decreased plasma levels of drug");
				outputModel.add(stm5);
			} else if (resultingCondition.equalsIgnoreCase("DDI_00059")) {
				Statement stm5 = outputModel.createStatement(Vocab.DDI_00059,
						Vocab.rdfslabel, "increased oral clearance");
				outputModel.add(stm5);
			} else if (resultingCondition.equalsIgnoreCase("DDI_00060")) {
				Statement stm5 = outputModel.createStatement(Vocab.DDI_00060,
						Vocab.rdfslabel,
						"increased oral clearence of nevirapine");
				outputModel.add(stm5);
			} else if (resultingCondition.equalsIgnoreCase("DDI_00058")) {
				Statement stm5 = outputModel.createStatement(Vocab.DDI_00058,
						Vocab.rdfslabel, "mania");
				outputModel.add(stm5);
			}

			// the annotated chemical entity is participant in some ddi
			output.addProperty(Vocab.SIO_000062, ddir);
		}

	}

	@SuppressWarnings("unused")
	private static final class Vocab {
		private static Model m_model = ModelFactory.createDefaultModel();
		public static Property rdftype = m_model
				.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		public static Property rdfslabel = m_model
				.createProperty("http://www.w3.org/2000/01/rdf-schema#label");
		public static final Resource PMID_Identifier = m_model
				.createResource("http://purl.oclc.org/SADI/LSRN/PMID_Identifier");
		public static final Property SIO_000145 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000145");
		public static final Property DDI_00008 = m_model
				.createProperty("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00008");
		public static final Property SIO_000053 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000053");
		public static final Property SIO_000355 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000355");
		public static final Property SIO_000563 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000563");
		public static final Property SIO_000253 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000253");
		public static final Property SIO_000028 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000028");
		public static final Property SIO_000283 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000283");
		public static final Property SIO_000341 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000341");
		public static final Property SIO_000217 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000217");
		public static final Property DDI_00001 = m_model
				.createProperty("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00001");
		public static final Property DDI_00002 = m_model
				.createProperty("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00002");
		public static final Property DDI_00018 = m_model
				.createProperty("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00018");
		public static final Property SIO_000672 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000672");
		public static final Property SIO_000059 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000059");
		public static final Property SIO_000272 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000272");
		public static final Property SIO_000300 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000300");
		public static final Property DDI_00019 = m_model
				.createProperty("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00019");
		public static final Property SIO_000554 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000554");
		public static final Resource DDI_00054 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00054");

		public static final Property SIO_000008 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000008");
		public static final Property SIO_000132 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000132");
		public static final Property SIO_000062 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000062");
		public static final Resource SIO_000087 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000087");
		public static final Property SIO_000364 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000364");
		public static final Property SIO_000338 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000338");
		public static Resource DDI_00007 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00007");

		public static final Property DDI_00051 = m_model
				.createProperty("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00051");
		public static final Property DDI_00055 = m_model
				.createProperty("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00055");

		public static final Property SIO_000369 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000369");
		public static final Property SIO_000668 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000668");
		public static final Property SIO_000643 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000643");
		public static final Property SIO_000011 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000011");
		public static final Property SIO_000218 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000218");
		public static final Property SIO_000420 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000420");
		public static final Property SIO_000273 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000273");
		public static final Property SIO_000687 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000687");
		public static final Property SIO_000061 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000061");
		public static final Property SIO_000356 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000356");
		public static final Property SIO_000339 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000339");
		public static final Resource Double = m_model
				.createResource("http://www.w3.org/2001/XMLSchema#double");
		public static final Property DDI_00006 = m_model
				.createProperty("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00006");
		public static final Property DDI_00014 = m_model
				.createProperty("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00014");
		public static final Resource Float = m_model
				.createResource("http://www.w3.org/2001/XMLSchema#float");
		public static final Resource SIO_000417 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000417");
		public static final Resource SIO_000026 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000026");
		public static final Resource Literal = m_model
				.createResource("http://www.w3.org/2000/01/rdf-schema#Literal");
		public static final Resource DDI_00033 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00033");
		public static final Resource DDI_00003 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00003");
		public static final Resource DDI_00009 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00009");
		public static final Resource DDI_00061 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00061");
		public static final Resource DDI_00050 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00050");
		public static final Resource DDI_00057 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00057");
		public static final Resource DDI_00058 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00058");
		public static final Resource DDI_00059 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00059");
		public static final Resource DDI_00060 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00060");

		public static final Resource SIO_000090 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000090");
		public static final Resource SIO_000122 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000122");
		public static final Resource dateTime = m_model
				.createResource("http://www.w3.org/2001/XMLSchema#dateTime");
		public static final Resource SIO_000003 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000003");
		public static final Resource SIO_000370 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000370");
		public static final Resource SIO_000006 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000006");
		public static final Resource SIO_000015 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000015");
		public static final Resource DDI_00015 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00015");
		public static final Resource SIO_000108 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000108");
		public static final Resource SIO_000416 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000416");
		public static final Resource string = m_model
				.createResource("http://www.w3.org/2001/XMLSchema#string");
		public static final Resource SIO_000340 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000340");
		public static final Resource SIO_000027 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000027");
		public static final Resource SIO_000000 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000000");
		public static final Resource SIO_000279 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000279");
		public static final Resource DDI_00000 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00000");
		public static final Resource DDI_00020 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00020");
		public static final Resource SIO_000275 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000275");
		public static final Resource SIO_000337 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000337");
		public static final Resource SIO_000004 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000004");
		public static final Resource integer = m_model
				.createResource("http://www.w3.org/2001/XMLSchema#integer");
		public static final Resource SIO_000002 = m_model
				.createResource("http://semanticscience.org/resource/SIO_000002");
	}

}
