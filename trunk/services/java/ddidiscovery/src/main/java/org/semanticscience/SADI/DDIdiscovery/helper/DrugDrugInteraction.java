package org.semanticscience.SADI.DDIdiscovery.helper;

import java.net.URL;

import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class DrugDrugInteraction {

	private Model aModel;
	private String base;
	private String drugAId;
	private String drugBId;
	private String drugBEffectOnDrugA;
	private String resultingCondition;
	private String description;
	private String pmid;

	private Resource drugARes;

	/**
	 * A jena resource description of this ddi
	 */
	public Resource resourceDescription;

	public DrugDrugInteraction() {
		aModel = ModelFactory.createDefaultModel();
		base = RdfUtils.createUniqueURI();
		drugBEffectOnDrugA = "pico";
	}

	public DrugDrugInteraction(Resource aDrugARes, String aDrugBId,
			String aDrugAEffectOnDrugB, String aResultingCondition,
			String aPmid, String aDescription) {
		this();
		drugARes = aDrugARes;
		drugBId = aDrugBId;
		drugBEffectOnDrugA = aDrugAEffectOnDrugB;
		resultingCondition = aResultingCondition;
		description = aDescription;
		pmid = aPmid;
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

	public Resource createResourceFromDrugBankId(Model aM, String anId) {
		Resource chemicalEntity = aM.createResource(RdfUtils.createUniqueURI());
		chemicalEntity.addProperty(Vocabulary.rdftype, Vocabulary.SIO_010004);
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

	public String getDrugAId() {
		return drugAId;
	}

	public void setDrugAId(String drugAId) {
		this.drugAId = drugAId;
	}

	public String getDrugBId() {
		return drugBId;
	}

	public void setDrugBId(String drugBId) {
		this.drugBId = drugBId;
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

	public String getDrugBEffectOnDrugA() {
		return drugBEffectOnDrugA;
	}

	public void setDrugBEffectOnDrugA(String drugBEffectOnDrugA) {
		this.drugBEffectOnDrugA = drugBEffectOnDrugA;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((drugAId == null) ? 0 : drugAId.hashCode());
		result = prime * result
				+ ((drugARes == null) ? 0 : drugARes.hashCode());
		result = prime
				* result
				+ ((drugBEffectOnDrugA == null) ? 0 : drugBEffectOnDrugA
						.hashCode());
		result = prime * result + ((drugBId == null) ? 0 : drugBId.hashCode());
		result = prime * result + ((pmid == null) ? 0 : pmid.hashCode());
		result = prime
				* result
				+ ((resultingCondition == null) ? 0 : resultingCondition
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DrugDrugInteraction other = (DrugDrugInteraction) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (drugAId == null) {
			if (other.drugAId != null)
				return false;
		} else if (!drugAId.equals(other.drugAId))
			return false;
		if (drugARes == null) {
			if (other.drugARes != null)
				return false;
		} else if (!drugARes.equals(other.drugARes))
			return false;
		if (drugBEffectOnDrugA == null) {
			if (other.drugBEffectOnDrugA != null)
				return false;
		} else if (!drugBEffectOnDrugA.equals(other.drugBEffectOnDrugA))
			return false;
		if (drugBId == null) {
			if (other.drugBId != null)
				return false;
		} else if (!drugBId.equals(other.drugBId))
			return false;
		if (pmid == null) {
			if (other.pmid != null)
				return false;
		} else if (!pmid.equals(other.pmid))
			return false;
		if (resultingCondition == null) {
			if (other.resultingCondition != null)
				return false;
		} else if (!resultingCondition.equals(other.resultingCondition))
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
	
		public static final Resource SIO_010004 = m_model
				.createResource("http://semanticscience.org/resource/SIO_010004");
		
		public static final Resource DrugBankIdentifier = m_model
				.createResource("http://purl.oclc.org/SADI/LSRN/DrugBank_Identifier");
		

	}

}
