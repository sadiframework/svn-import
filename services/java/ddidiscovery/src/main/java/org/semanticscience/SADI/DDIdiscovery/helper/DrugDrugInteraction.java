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

import java.net.URL;

import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class DrugDrugInteraction {

	private Model aModel;
	private String actorId;
	private String actorLabel;
	private String targetId;
	private String targetLabel;
	private String targetEffectOnActor;
	private String actorEffectOnTarget;
	private boolean isDirected;
	private String rConditonDDIID;
	private String description;
	private String pmid;
	private URL resultingConditionURL;
	private String resultingConditionLabel;

	/**
	 * A jena resource description of this ddi
	 */
	public Resource resourceDescription;

	public DrugDrugInteraction() {
		aModel = ModelFactory.createDefaultModel();
		isDirected = false;
	}

	public DrugDrugInteraction(String anActorId, String aTargetId,
			String anActorEffectOnTarget, String aTargetEffectOnActor,
			String aResultingCondition, String aPmid, String aDescription,
			String anActorLabel, String aTargetLabel, boolean aIsDirected) {
		this();
		actorId = anActorId;
		targetId = aTargetId;
		actorEffectOnTarget = anActorEffectOnTarget;
		rConditonDDIID = aResultingCondition;
		pmid = aPmid;
		targetEffectOnActor = aTargetEffectOnActor;
		description = aDescription;
		actorLabel = anActorLabel;
		targetLabel = aTargetLabel;
		isDirected = aIsDirected;
	}

	public DrugDrugInteraction(String anActorId, String aTargetId,
			URL aResultingConditionURL, String aResultingConditionLabel,
			String anActorLabel, String aTargetLabel, boolean isDirected) {
		this(anActorId, aTargetId, "", "", "", "", "", anActorLabel,
				aTargetLabel, isDirected);
		resultingConditionLabel = aResultingConditionLabel;
		resultingConditionURL = aResultingConditionURL;
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


	public String getRCDDIId() {
		return rConditonDDIID;
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
	public String getResultingConditionLabel(){
		return resultingConditionLabel;
	}

	public String getActorId() {
		return actorId;
	}

	public URL getResultingConditionURL() {
		return resultingConditionURL;
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
	
	

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((actorEffectOnTarget == null) ? 0 : actorEffectOnTarget
						.hashCode());
		result = prime * result + ((actorId == null) ? 0 : actorId.hashCode());
		result = prime * result
				+ ((actorLabel == null) ? 0 : actorLabel.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + (isDirected ? 1231 : 1237);
		result = prime * result + ((pmid == null) ? 0 : pmid.hashCode());
		result = prime
				* result
				+ ((resourceDescription == null) ? 0 : resourceDescription
						.hashCode());
		result = prime
				* result
				+ ((rConditonDDIID == null) ? 0 : rConditonDDIID
						.hashCode());
		result = prime
				* result
				+ ((resultingConditionLabel == null) ? 0
						: resultingConditionLabel.hashCode());
		result = prime
				* result
				+ ((resultingConditionURL == null) ? 0 : resultingConditionURL
						.hashCode());
		result = prime
				* result
				+ ((targetEffectOnActor == null) ? 0 : targetEffectOnActor
						.hashCode());
		result = prime * result
				+ ((targetId == null) ? 0 : targetId.hashCode());
		result = prime * result
				+ ((targetLabel == null) ? 0 : targetLabel.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "DrugDrugInteraction [actorId=" + actorId + ", actorLabel="
				+ actorLabel + ", targetId=" + targetId + ", targetLabel="
				+ targetLabel + ", targetEffectOnActor=" + targetEffectOnActor
				+ ", actorEffectOnTarget=" + actorEffectOnTarget
				+ ", isDirected=" + isDirected + ", resultingCondition="
				+ rConditonDDIID + ", description=" + description
				+ ", pmid=" + pmid + ", resultingConditionURL="
				+ resultingConditionURL + ", resultingConditionLabel="
				+ resultingConditionLabel + "]\n\n";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DrugDrugInteraction other = (DrugDrugInteraction) obj;
		if (actorEffectOnTarget == null) {
			if (other.actorEffectOnTarget != null)
				return false;
		} else if (!actorEffectOnTarget.equals(other.actorEffectOnTarget))
			return false;
		if (actorId == null) {
			if (other.actorId != null)
				return false;
		} else if (!actorId.equals(other.actorId))
			return false;
		if (actorLabel == null) {
			if (other.actorLabel != null)
				return false;
		} else if (!actorLabel.equals(other.actorLabel))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (isDirected != other.isDirected)
			return false;
		if (pmid == null) {
			if (other.pmid != null)
				return false;
		} else if (!pmid.equals(other.pmid))
			return false;
		if (resourceDescription == null) {
			if (other.resourceDescription != null)
				return false;
		} else if (!resourceDescription.equals(other.resourceDescription))
			return false;
		if (rConditonDDIID == null) {
			if (other.rConditonDDIID != null)
				return false;
		} else if (!rConditonDDIID.equals(other.rConditonDDIID))
			return false;
		if (resultingConditionLabel == null) {
			if (other.resultingConditionLabel != null)
				return false;
		} else if (!resultingConditionLabel
				.equals(other.resultingConditionLabel))
			return false;
		if (resultingConditionURL == null) {
			if (other.resultingConditionURL != null)
				return false;
		} else if (!resultingConditionURL.equals(other.resultingConditionURL))
			return false;
		if (targetEffectOnActor == null) {
			if (other.targetEffectOnActor != null)
				return false;
		} else if (!targetEffectOnActor.equals(other.targetEffectOnActor))
			return false;
		if (targetId == null) {
			if (other.targetId != null)
				return false;
		} else if (!targetId.equals(other.targetId))
			return false;
		if (targetLabel == null) {
			if (other.targetLabel != null)
				return false;
		} else if (!targetLabel.equals(other.targetLabel))
			return false;
		return true;
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
