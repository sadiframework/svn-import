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

import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class DrugDrugInteraction {

	private Model aModel;
	private String base;
	private String actorId;
	private String actorLabel;
	private String targetId;
	private String targetLabel;
	private String targetEffectOnActor;
	private String actorEffectOnTarget;
	private boolean isDirected;
	private String resultingCondition;
	private String description;
	private String pmid;


	/**
	 * A jena resource description of this ddi
	 */
	public Resource resourceDescription;

	public DrugDrugInteraction() {
		aModel = ModelFactory.createDefaultModel();
		base = RdfUtils.createUniqueURI();
		isDirected = false;
	}

	public DrugDrugInteraction(String anActorId, String aTargetId,
			String anActorEffectOnTarget, String aTargetEffectOnActor, String aResultingCondition,
			String aPmid, String aDescription, String anActorLabel,
			String aTargetLabel, boolean aIsDirected) {
		this();
		actorId = anActorId;
		targetId = aTargetId;
		actorEffectOnTarget = anActorEffectOnTarget;
		resultingCondition = aResultingCondition;
		pmid = aPmid;
		targetEffectOnActor = aTargetEffectOnActor;
		description = aDescription;
		actorLabel = anActorLabel;
		targetLabel = aTargetLabel;
		isDirected = aIsDirected;
	}

	public static Resource createTargetResource(Model aM) {
		Resource target = aM.createResource(RdfUtils.createUniqueURI());
		target.addProperty(Vocabulary.rdftype, Vocabulary.DDI_00010);
		return target;

	}

	/**
	 * Check if the passed in DBid is a target or an actor
	 * 
	 * @param anId
	 * @return true if the input id belongs to the target
	 */
	public boolean hasTarget(String anId) {
		if (anId.equalsIgnoreCase(this.getTargetId())) {
			return true;
		}
		return false;

	}

	public static Resource createActorResource(Model aM) {
		Resource actor = aM.createResource(RdfUtils.createUniqueURI());
		actor.addProperty(Vocabulary.rdftype, Vocabulary.DDI_00008);
		return actor;
	}

	public static Resource createInteractantResource(Model aM) {
		Resource interactant = aM.createResource(RdfUtils.createUniqueURI());
		interactant.addProperty(Vocabulary.rdftype, Vocabulary.DDI_00063);
		return interactant;
	}

	public static Resource createResourceFromDrugBankId(Model aM, String anId,
			String aLabel) {
		Resource chemicalEntity = aM.createResource(RdfUtils.createUniqueURI());
		chemicalEntity.addProperty(Vocabulary.rdftype, Vocabulary.SIO_010004);
		// add the label
		if (aLabel != null && aLabel.length() > 0) {
			chemicalEntity.addProperty(Vocabulary.rdfslabel, aLabel);
		}
		Resource drugbankIdentifier = aM.createResource(RdfUtils
				.createUniqueURI());
		drugbankIdentifier.addProperty(Vocabulary.rdftype,
				Vocabulary.DrugBankIdentifier);
		drugbankIdentifier.addProperty(Vocabulary.SIO_000300, anId);
		chemicalEntity.addProperty(Vocabulary.SIO_000008, drugbankIdentifier);
		return chemicalEntity;
	}

	public String getBase() {
		return base;
	}

	public void setBase(String base) {
		this.base = base;
	}

	public String getResultingCondition() {
		return resultingCondition;
	}

	public void setResultingCondition(String resultingCondition) {
		this.resultingCondition = resultingCondition;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPmid() {
		return pmid;
	}

	public void setPmid(String pmid) {
		this.pmid = pmid;
	}

	public Resource getResourceDescription() {
		return resourceDescription;
	}

	public void setResourceDescription(Resource resourceDescription) {
		this.resourceDescription = resourceDescription;
	}

	public String getActorId() {
		return actorId;
	}

	public String getActorLabel() {
		return actorLabel;
	}

	public String getTargetId() {
		return targetId;
	}

	public String getTargetLabel() {
		return targetLabel;
	}

	public String getTargetEffectOnActor() {
		return targetEffectOnActor;
	}

	public String getActorEffectOnTarget() {
		return actorEffectOnTarget;
	}

	public boolean isDirected() {
		return isDirected;
	}

	@SuppressWarnings("unused")
	public static final class Vocabulary {
		private static Model m_model = ModelFactory.createDefaultModel();
		public static Property rdftype = m_model
				.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		public static final Property SIO_000300 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000300");
		public static final Property SIO_000008 = m_model
				.createProperty("http://semanticscience.org/resource/SIO_000008");
		public static Property rdfslabel = m_model
				.createProperty("http://www.w3.org/2000/01/rdf-schema#label");

		public static final Resource SIO_010004 = m_model
				.createResource("http://semanticscience.org/resource/SIO_010004");
		public static final Resource DDI_00010 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00010");
		public static final Resource DDI_00008 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00008");
		public static final Resource DDI_00063 = m_model
				.createResource("http://sadi-ontology.semanticscience.org/ddiv2.owl#DDI_00063");

		public static final Resource DrugBankIdentifier = m_model
				.createResource("http://purl.oclc.org/SADI/LSRN/DrugBank_Identifier");

	}

}
