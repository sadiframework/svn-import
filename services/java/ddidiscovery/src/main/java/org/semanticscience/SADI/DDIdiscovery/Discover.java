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
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.semanticscience.SADI.DDIdiscovery.helper.DiscoverHelper;
import org.semanticscience.SADI.DDIdiscovery.helper.DrugDrugInteraction;
import org.semanticscience.SADI.DDIdiscovery.helper.DiscoverHelper.Vocabulary;

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
	private static final long serialVersionUID = 1L;

	@Override
	public void processInput(Resource input, Resource output) {
		final String ddiFilename = "ddi-0.0.3.csv";
		Model outputModel = output.getModel();
		// read in the ddi.csv file
		InputStream is = Discover.class.getClassLoader().getResourceAsStream(
				ddiFilename);
		// get the input id
		String inputId = DiscoverHelper.getChemicalIdentifier(input,
				Vocabulary.SIO_000008.toString(),
				Vocabulary.SIO_000300.toString());

		// find the ddis from CSVFile
		ArrayList<DrugDrugInteraction> ddis = DiscoverHelper.findDDISInCSVFile(
				is, input);
		Iterator<DrugDrugInteraction> itr = ddis.iterator();
		while (itr.hasNext()) {
			DrugDrugInteraction addi = itr.next();
			// create a ddi resource
			Resource ddir = outputModel.createResource(RdfUtils
					.createUniqueURI());
			// create a publication resource
			// check if there is a publication associated with this ddi
			if (addi.getPmid() == "" || addi.getPmid() == null) {
				Resource pub = DiscoverHelper.createPublicationResource(
						outputModel, addi.getPmid());
				// connect the ddi with the publication
				ddir.addProperty(Vocab.SIO_000253, pub);
			}

			// add the 'results in' class and label
			String resultingCondition = addi.getRCDDIId();
			//
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
			} else if (resultingCondition.equalsIgnoreCase("DDI_00031")) {
				Statement stm5 = outputModel.createStatement(Vocab.DDI_00031,
						Vocab.rdfslabel,
						"adverse drug interaction induced phenotype");
				outputModel.add(stm5);
			}

			// check if the interaction is directed or not
			if (addi.isDirected()) {
				// type ddir as a directed interaction
				ddir.addProperty(Vocab.rdftype, Vocab.DDI_00062);
				// find out if the output has the target role
				if (addi.hasTarget(inputId)) {
					// create a target resource
					Resource target = DrugDrugInteraction
							.createTargetResource(outputModel);
					// connect the target to the ddir
					// realizes / is realized in
					target.addProperty(Vocab.SIO_000356, ddir);
					ddir.addProperty(Vocab.SIO_000355, target);
					// is role of/ has role
					target.addProperty(Vocab.SIO_000227, output);
					output.addProperty(Vocab.SIO_000228, target);
					// create the chemical entity with which the input was found
					// to interact with
					Resource chemEnt = DrugDrugInteraction
							.createResourceFromDrugBankId(outputModel,
									addi.getActorId(), addi.getActorLabel());
					// create an actor resource
					Resource actor = DrugDrugInteraction
							.createActorResource(outputModel);
					// connect the target to the ddir
					// is role of/ has role
					chemEnt.addProperty(Vocab.SIO_000228, actor);
					actor.addProperty(Vocab.SIO_000227, chemEnt);
					// connect chemEnt to the ddir
					chemEnt.addProperty(Vocab.SIO_000062, ddir);
					ddir.addProperty(Vocab.SIO_000132, chemEnt);

					// realizes / is realized in
					actor.addProperty(Vocab.SIO_000356, ddir);
					ddir.addProperty(Vocab.SIO_000355, actor);
					Statement stm2 = outputModel.createStatement(output,
							Vocab.rdfslabel, addi.getTargetLabel());

					ddir.addProperty(Vocab.SIO_000132, output);
					output.addProperty(Vocab.SIO_000062, ddir);
				} else {
					// the input drug has the actor role
					// create the chemical entity with which the input was found
					// to interact with
					Resource chemEnt = DrugDrugInteraction
							.createResourceFromDrugBankId(outputModel,
									addi.getTargetId(), addi.getTargetLabel());
					// create a target resource
					Resource actor = DrugDrugInteraction
							.createActorResource(outputModel);
					// connect the target to the ddir
					// is role of/ has role
					actor.addProperty(Vocab.SIO_000227, output);
					output.addProperty(Vocab.SIO_000228, actor);
					// realizes / is realized in
					actor.addProperty(Vocab.SIO_000356, ddir);
					ddir.addProperty(Vocab.SIO_000355, actor);
					// connect chemEnt to the ddir
					output.addProperty(Vocab.SIO_000062, ddir);
					ddir.addProperty(Vocab.SIO_000132, output);
					// create an actor resource
					Resource target = DrugDrugInteraction
							.createTargetResource(outputModel);
					// connect the target to the ddir
					// realizes / is realized in
					target.addProperty(Vocab.SIO_000356, ddir);
					ddir.addProperty(Vocab.SIO_000355, target);
					// is role of/ has role
					target.addProperty(Vocab.SIO_000227, chemEnt);
					chemEnt.addProperty(Vocab.SIO_000228, target);

					chemEnt.addProperty(Vocab.SIO_000062, ddir);
					ddir.addProperty(Vocab.SIO_000132, chemEnt);

					Statement stm2 = outputModel.createStatement(output,
							Vocab.rdfslabel, addi.getActorLabel());
					ddir.addProperty(Vocab.SIO_000132, output);
					output.addProperty(Vocab.SIO_000062, ddir);
				}

			} else {
				// type ddir as an undirected interaction
				ddir.addProperty(Vocab.rdftype, Vocab.DDI_00000);
				// find out if the output has the "target" role
				if (addi.hasTarget(inputId)) {
					// create a target resource
					Resource interactant1 = DrugDrugInteraction
							.createInteractantResource(outputModel);
					// connect the target to the ddir
					// realizes / is realized in
					interactant1.addProperty(Vocab.SIO_000356, ddir);
					ddir.addProperty(Vocab.SIO_000355, interactant1);
					// is role of/ has role
					interactant1.addProperty(Vocab.SIO_000227, output);
					output.addProperty(Vocab.SIO_000228, interactant1);
					// create the chemical entity with which the input was found
					// to interact with
					Resource chemEnt = DrugDrugInteraction
							.createResourceFromDrugBankId(outputModel,
									addi.getActorId(), addi.getActorLabel());
					// create an actor resource
					Resource interactant2 = DrugDrugInteraction
							.createInteractantResource(outputModel);
					// connect the target to the ddir
					// is role of/ has role
					chemEnt.addProperty(Vocab.SIO_000228, interactant2);
					interactant2.addProperty(Vocab.SIO_000227, chemEnt);
					// connect chemEnt to the ddir
					chemEnt.addProperty(Vocab.SIO_000062, ddir);
					ddir.addProperty(Vocab.SIO_000132, chemEnt);

					// realizes / is realized in
					interactant2.addProperty(Vocab.SIO_000356, ddir);
					ddir.addProperty(Vocab.SIO_000355, interactant2);
					Statement stm2 = outputModel.createStatement(output,
							Vocab.rdfslabel, addi.getTargetLabel());

					ddir.addProperty(Vocab.SIO_000132, output);
					output.addProperty(Vocab.SIO_000062, ddir);

				} else {

					// the input drug has the actor role
					// create the chemical entity with which the input was found
					// to interact with
					Resource chemEnt = DrugDrugInteraction
							.createResourceFromDrugBankId(outputModel,
									addi.getTargetId(), addi.getTargetLabel());
					// create a target resource
					Resource interactant1 = DrugDrugInteraction
							.createInteractantResource(outputModel);
					// connect the target to the ddir
					// is role of/ has role
					interactant1.addProperty(Vocab.SIO_000227, output);
					output.addProperty(Vocab.SIO_000228, interactant1);
					// realizes / is realized in
					interactant1.addProperty(Vocab.SIO_000356, ddir);
					ddir.addProperty(Vocab.SIO_000355, interactant1);
					// connect chemEnt to the ddir
					output.addProperty(Vocab.SIO_000062, ddir);
					ddir.addProperty(Vocab.SIO_000132, output);
					// create an actor resource
					Resource interactant2 = DrugDrugInteraction
							.createInteractantResource(outputModel);
					// connect the target to the ddir
					// realizes / is realized in
					interactant2.addProperty(Vocab.SIO_000356, ddir);
					ddir.addProperty(Vocab.SIO_000355, interactant2);
					// is role of/ has role
					interactant2.addProperty(Vocab.SIO_000227, chemEnt);
					chemEnt.addProperty(Vocab.SIO_000228, interactant2);

					chemEnt.addProperty(Vocab.SIO_000062, ddir);
					ddir.addProperty(Vocab.SIO_000132, chemEnt);

					Statement stm2 = outputModel.createStatement(output,
							Vocab.rdfslabel, addi.getActorLabel());

					ddir.addProperty(Vocab.SIO_000132, output);
					output.addProperty(Vocab.SIO_000062, ddir);
				}
			}
			// the annotated chemical entity is participant in some ddi
			output.addProperty(Vocab.SIO_000062, ddir);
		}// while

		// now query the endpoint to find more DDIs
		List<DrugDrugInteraction> endpointInteractions = DiscoverHelper
				.findDDIInEndpoint(input);
		
		Iterator<DrugDrugInteraction> itr2 = endpointInteractions.iterator();
		while(itr2.hasNext()){
			DrugDrugInteraction aDDI = itr2.next();
			output = makeDDIResource(aDDI, input, output, outputModel);
		}//while
		

	}

	public Resource makeDDIResource(DrugDrugInteraction aDDI, Resource input, Resource output, Model outputModel) {
		// get the input id
		String inputId = DiscoverHelper.getChemicalIdentifier(input,
				Vocabulary.SIO_000008.toString(),
				Vocabulary.SIO_000300.toString());
		// create a ddir resource
		Resource ddir = outputModel.createResource(RdfUtils.createUniqueURI());

		// add the results in condition
		Resource resultsIn = outputModel.createResource(RdfUtils.createUniqueURI());
		Resource umlsResource = outputModel.createResource(aDDI
				.getResultingConditionURL().toString());
		umlsResource.addProperty(Vocab.rdfsSubClassOf, Vocab.DDI_00031);
		resultsIn.addProperty(Vocab.rdftype, umlsResource);
		resultsIn.addProperty(Vocab.rdfslabel, aDDI.getResultingConditionLabel());
		
		//connect it to the ddir
		ddir.addProperty(Vocab.SIO_000554, resultsIn);
		
		//check if the ddir is directed
		if(aDDI.isDirected()){
			// type ddir as a directed interaction
			ddir.addProperty(Vocab.rdftype, Vocab.DDI_00062);
			// find out if the output has the target role
			if (aDDI.hasTarget(inputId)) {
				// create a target resource
				Resource target = DrugDrugInteraction
						.createTargetResource(outputModel);
				// connect the target to the ddir
				// realizes / is realized in
				target.addProperty(Vocab.SIO_000356, ddir);
				ddir.addProperty(Vocab.SIO_000355, target);
				// is role of/ has role
				target.addProperty(Vocab.SIO_000227, output);
				output.addProperty(Vocab.SIO_000228, target);
				// create the chemical entity with which the input was found
				// to interact with
				Resource chemEnt = DrugDrugInteraction
						.createResourceFromDrugBankId(outputModel,
								aDDI.getActorId(), aDDI.getActorLabel());
				// create an actor resource
				Resource actor = DrugDrugInteraction
						.createActorResource(outputModel);
				// connect the target to the ddir
				// is role of/ has role
				chemEnt.addProperty(Vocab.SIO_000228, actor);
				actor.addProperty(Vocab.SIO_000227, chemEnt);
				// connect chemEnt to the ddir
				chemEnt.addProperty(Vocab.SIO_000062, ddir);
				ddir.addProperty(Vocab.SIO_000132, chemEnt);

				// realizes / is realized in
				actor.addProperty(Vocab.SIO_000356, ddir);
				ddir.addProperty(Vocab.SIO_000355, actor);
				
				output.addProperty(Vocab.rdfslabel,	aDDI.getTargetLabel());
				ddir.addProperty(Vocab.SIO_000132, output);
				output.addProperty(Vocab.SIO_000062, ddir);
			} else {
				// the input drug has the actor role
				// create the chemical entity with which the input was found
				// to interact with
				Resource chemEnt = DrugDrugInteraction
						.createResourceFromDrugBankId(outputModel,
								aDDI.getTargetId(), aDDI.getTargetLabel());
				// create a target resource
				Resource actor = DrugDrugInteraction
						.createActorResource(outputModel);
				// connect the target to the ddir
				// is role of/ has role
				actor.addProperty(Vocab.SIO_000227, output);
				output.addProperty(Vocab.SIO_000228, actor);
				// realizes / is realized in
				actor.addProperty(Vocab.SIO_000356, ddir);
				ddir.addProperty(Vocab.SIO_000355, actor);
				// connect chemEnt to the ddir
				output.addProperty(Vocab.SIO_000062, ddir);
				ddir.addProperty(Vocab.SIO_000132, output);
				// create an actor resource
				Resource target = DrugDrugInteraction
						.createTargetResource(outputModel);
				// connect the target to the ddir
				// realizes / is realized in
				target.addProperty(Vocab.SIO_000356, ddir);
				ddir.addProperty(Vocab.SIO_000355, target);
				// is role of/ has role
				target.addProperty(Vocab.SIO_000227, chemEnt);
				chemEnt.addProperty(Vocab.SIO_000228, target);

				chemEnt.addProperty(Vocab.SIO_000062, ddir);
				ddir.addProperty(Vocab.SIO_000132, chemEnt);

				
				output.addProperty(Vocab.rdfslabel,	aDDI.getTargetLabel());
				ddir.addProperty(Vocab.SIO_000132, output);
				output.addProperty(Vocab.SIO_000062, ddir);
			}
			
		}else if(!aDDI.isDirected()){
			// type ddir as a directed interaction
			ddir.addProperty(Vocab.rdftype, Vocab.DDI_00000);
			//find out if the ddi has the "target" role
			if(aDDI.hasTarget(inputId)){
				Resource interactant1 = DrugDrugInteraction.createTargetResource(outputModel);
				// connect the target to the ddir
				// realizes / is realized in
				interactant1.addProperty(Vocab.SIO_000356, ddir);
				ddir.addProperty(Vocab.SIO_000355, interactant1);
				// is role of/ has role
				interactant1.addProperty(Vocab.SIO_000227, output);
				output.addProperty(Vocab.SIO_000228, interactant1);
				// create the chemical entity with which the input was found
				// to interact with
				Resource chemEnt = DrugDrugInteraction
						.createResourceFromDrugBankId(outputModel,
								aDDI.getActorId(), aDDI.getActorLabel());
				// create an actor resource
				Resource interactant2 = DrugDrugInteraction
						.createInteractantResource(outputModel);
				// connect the target to the ddir
				// is role of/ has role
				chemEnt.addProperty(Vocab.SIO_000228, interactant2);
				interactant2.addProperty(Vocab.SIO_000227, chemEnt);
				// connect chemEnt to the ddir
				chemEnt.addProperty(Vocab.SIO_000062, ddir);
				ddir.addProperty(Vocab.SIO_000132, chemEnt);
				// realizes / is realized in
				interactant2.addProperty(Vocab.SIO_000356, ddir);
				ddir.addProperty(Vocab.SIO_000355, interactant2);
				
				output.addProperty(Vocab.rdfslabel,	aDDI.getTargetLabel());
				ddir.addProperty(Vocab.SIO_000132, output);
				output.addProperty(Vocab.SIO_000062, ddir);	
			}
			else{
				// the input drug has the actor role
				// create the chemical entity with which the input was found
				// to interact with
				Resource chemEnt = DrugDrugInteraction
						.createResourceFromDrugBankId(outputModel,
								aDDI.getTargetId(), aDDI.getTargetLabel());
				// create a target resource
				Resource interactant1 = DrugDrugInteraction
						.createInteractantResource(outputModel);
				// connect the target to the ddir
				// is role of/ has role
				interactant1.addProperty(Vocab.SIO_000227, output);
				output.addProperty(Vocab.SIO_000228, interactant1);
				// realizes / is realized in
				interactant1.addProperty(Vocab.SIO_000356, ddir);
				ddir.addProperty(Vocab.SIO_000355, interactant1);
				// connect chemEnt to the ddir
				output.addProperty(Vocab.SIO_000062, ddir);
				ddir.addProperty(Vocab.SIO_000132, output);
				// create an actor resource
				Resource interactant2 = DrugDrugInteraction
						.createInteractantResource(outputModel);
				// connect the target to the ddir
				// realizes / is realized in
				interactant2.addProperty(Vocab.SIO_000356, ddir);
				ddir.addProperty(Vocab.SIO_000355, interactant2);
				// is role of/ has role
				interactant2.addProperty(Vocab.SIO_000227, chemEnt);
				chemEnt.addProperty(Vocab.SIO_000228, interactant2);

				chemEnt.addProperty(Vocab.SIO_000062, ddir);
				ddir.addProperty(Vocab.SIO_000132, chemEnt);
				output.addProperty(Vocab.rdfslabel,	aDDI.getTargetLabel());
				ddir.addProperty(Vocab.SIO_000132, output);
				output.addProperty(Vocab.SIO_000062, ddir);
			}
		}
		return output;
	}

	@SuppressWarnings("unused")
	private static final class Vocab {
		private static Model m_model = ModelFactory.createDefaultModel();
		public static Property rdftype = m_model
				.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		public static Property rdfslabel = m_model
				.createProperty("http://www.w3.org/2000/01/rdf-schema#label");
		public static Property rdfsSubClassOf = m_model
				.createProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf");
		public static final Resource PMID_Identifier = m_model
				.createResource("http://purl.oclc.org/SADI/LSRN/PMID_Identifier");
		public static final Property SIO_000145 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000145");
		public static final Resource DDI_00008 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00008");
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
		public static final Property SIO_000001 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000001");
		public static final Property DDI_00019 = m_model
				.createProperty("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00019");
		public static final Property SIO_000554 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000554");
		public static final Property SIO_000228 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000228");
		public static final Property SIO_000227 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000227");
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
		public static final Resource DDI_00062 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00062");
		public static final Resource DDI_00063 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00063");
		public static final Resource DDI_00010 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00010");
		public static final Resource DDI_00031 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00031");

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
