package ca.wilkinsonlab.sadi;

import java.util.Collection;

import ca.wilkinsonlab.sadi.beans.RestrictionBean;

/**
 * An interface providing programmatic access to a SADI service description.
 * @author Luke McCarthy
 */
public interface ServiceDescription extends HasURI
{
	/**
	 * Returns the service URI.
	 * A SADI service is identified by a URI that is also an HTTP URL. 
	 * This URL is used to invoke the service as well as to identify it.
	 *  
	 * @return the URI of the service
	 */
	String getURI();

	/**
	 * Returns the service name, which may be null.
	 * A SADI service should have a short human-readable name.
	 * This is not required, but is encouraged.
	 * 
	 * @return the service name, which may be null
	 */
	String getName();

//	/**
//	 * @param name the name to set
//	 */
//	void setName(String name);

	/**
	 * Returns the service description, which may be null.
	 * A SADI service should have a detailed human-readable description.
	 * This is not required, but is encouraged.
	 * 
	 * @return the service description, which may be null
	 */
	String getDescription();

//	/**
//	 * @param description the description to set
//	 */
//	void setDescription(String description);

	/**
	 * Returns the service provider, which may be null.
	 * By convention, this should be the domain name of the institution
	 * responsible for the service.
	 * This is not required.
	 * @return the service provider
	 */
	String getServiceProvider();

//	/**
//	 * @param provider the service provider to set
//	 */
//	void setServiceProvider(String provider);

	/**
	 * Returns the service contact email address, which may be null.
	 * This is an email address that can be used to contact the service
	 * provider in the event that there are problems with the service.
	 * This is not required, but is encouraged and may one day be required.
	 * @return the service contact email address
	 */
	String getContactEmail();

//	/**
//	 * @param email the contact email to set
//	 */
//	void setContactEmail(String email);

	/**
	 * Returns true if the service is authoritative and false otherwise.
	 * A service is authoritative of its output if it is not wrapping a
	 * third-party data source.
	 * @return true if the service is authoritative and false otherwise.
	 */
	boolean isAuthoritative();

//	/**
//	 * @param authoritative
//	 */
//	void setAuthoritative(boolean authoritative);

	/**
	 * Returns the URI of the service's input OWL class.
	 * A SADI service has an input OWL class whose property restrictions
	 * describe the data that the service consumes. 
	 * This is required and the URI must resolve to a definition of the class.
	 * 
	 * @return the URI of the service's input OWL class
	 */
	String getInputClassURI();

//	/**
//	 * @param inputClassURI the inputClassURI to set
//	 */
//	void setInputClassURI(String inputClassURI);

	/**
	 * Returns the label of the service's input OWL class.
	 * @return the label of the service's input OWL class
	 */
	String getInputClassLabel();

//	/**
//	 * @param inputClassLabel the inputClassLabel to set
//	 */
//	void setInputClassLabel(String inputClassLabel);

	/**
	 * Returns the URI of the service's output OWL class.
	 * A SADI service has an output OWL class whose property restrictions
	 * describe the data that the service produces.
	 * This is required and the URI must resolve to a definition of the class.
	 * 
	 * @return the URI of the service's output OWL class
	 */
	String getOutputClassURI();

//	/**
//	 * @param outputClassURI the outputClassURI to set
//	 */
//	void setOutputClassURI(String outputClassURI);

	/**
	 * Returns the label of the service's output OWL class.
	 * @return the label of the service's output OWL class
	 */
	String getOutputClassLabel();

//	/**
//	 * @param outputClassLabel the outputClassLabel to set
//	 */
//	void setOutputClassLabel(String outputClassLabel);

	/**
	 * @return the restrictions
	 */
	Collection<RestrictionBean> getRestrictionBeans();

//	/**
//	 * @param restrictions the restrictions to set
//	 */
//	void setRestrictions(Collection<RestrictionBean> restrictions);
	
	/**
	 * Returns the URI of the service's parameter OWL class, 
	 * which may be null.
	 * A SADI service can have a parameter OWL class whose property 
	 * restrictions describe secondary parameters that affect the 
	 * service's behaviour.
	 * This is not require, but if present  the URI must resolve to a
	 * definition of the class.
	 * 
	 * @return the URI of the service's parameter OWL class
	 */
	String getParameterClassURI();
	
	/**
	 * Returns the label of the service's parameter OWL class,
	 * which may be null.
	 * @return the label of the service's parameter OWL class
	 */
	String getParameterClassLabel();
	
//	/**
//	 * @return the secondary parameter default instance URI
//	 */
//	String getParameterDefaultInstanceURI();
}