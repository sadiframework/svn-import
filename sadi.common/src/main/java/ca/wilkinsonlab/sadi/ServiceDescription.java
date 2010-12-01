package ca.wilkinsonlab.sadi;

import java.util.Collection;

import ca.wilkinsonlab.sadi.beans.RestrictionBean;

/**
 * @author Luke McCarthy
 *
 */
public interface ServiceDescription extends URIable
{
	/**
	 * @return the URI
	 */
	String getURI();

//	/**
//	 * @param URI the URI to set
//	 */
//	void setURI(String URI);

	/**
	 * @return the name
	 */
	String getName();

//	/**
//	 * @param name the name to set
//	 */
//	void setName(String name);

	/**
	 * @return the description
	 */
	String getDescription();

//	/**
//	 * @param description the description to set
//	 */
//	void setDescription(String description);

	/**
	 * @return the service provider
	 */
	String getServiceProvider();

//	/**
//	 * @param provider the service provider to set
//	 */
//	void setServiceProvider(String provider);

	/**
	 * @return the contact email
	 */
	String getContactEmail();

//	/**
//	 * @param email the contact email to set
//	 */
//	void setContactEmail(String email);

	/**
	 * @return
	 */
	boolean isAuthoritative();

//	/**
//	 * @param authoritative
//	 */
//	void setAuthoritative(boolean authoritative);

	/**
	 * @return the inputClassURI
	 */
	String getInputClassURI();

//	/**
//	 * @param inputClassURI the inputClassURI to set
//	 */
//	void setInputClassURI(String inputClassURI);

	/**
	 * @return the inputClassLabel
	 */
	String getInputClassLabel();

//	/**
//	 * @param inputClassLabel the inputClassLabel to set
//	 */
//	void setInputClassLabel(String inputClassLabel);

	/**
	 * @return the outputClassURI
	 */
	String getOutputClassURI();

//	/**
//	 * @param outputClassURI the outputClassURI to set
//	 */
//	void setOutputClassURI(String outputClassURI);

	/**
	 * @return the outputClassLabel
	 */
	String getOutputClassLabel();

//	/**
//	 * @param outputClassLabel the outputClassLabel to set
//	 */
//	void setOutputClassLabel(String outputClassLabel);

	/**
	 * @return the restrictions
	 */
	Collection<RestrictionBean> getRestrictions();

//	/**
//	 * @param restrictions the restrictions to set
//	 */
//	void setRestrictions(Collection<RestrictionBean> restrictions);
	
	/**
	 * @return the secondary parameter class URI
	 */
	String getParameterClassURI();
	
	/**
	 * @return the secondary parameter class URI
	 */
	String getParameterClassLabel();
	
//	/**
//	 * @return the secondary parameter default instance URI
//	 */
//	String getParameterDefaultInstanceURI();
}