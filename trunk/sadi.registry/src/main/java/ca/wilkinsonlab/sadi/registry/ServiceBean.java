package ca.wilkinsonlab.sadi.registry;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 
 * @author Luke McCarthy
 */
public class ServiceBean
{
	private String serviceURI;
	private String inputClassURI;
	private String outputClassURI;
	private Collection<RestrictionBean> restrictions;
	private String name;
	private String description;
	
	public ServiceBean()
	{
		setRestrictions(new ArrayList<RestrictionBean>());
	}

	/**
	 * @return the serviceURI
	 */
	public String getServiceURI()
	{
		return serviceURI;
	}

	/**
	 * @param serviceURI the serviceURI to set
	 */
	public void setServiceURI(String serviceURI)
	{
		this.serviceURI = serviceURI;
	}

	/**
	 * @return the inputClassURI
	 */
	public String getInputClassURI()
	{
		return inputClassURI;
	}

	/**
	 * @param inputClassURI the inputClassURI to set
	 */
	public void setInputClassURI(String inputClassURI)
	{
		this.inputClassURI = inputClassURI;
	}

	/**
	 * @return the outputClassURI
	 */
	public String getOutputClassURI()
	{
		return outputClassURI;
	}

	/**
	 * @param outputClassURI the outputClassURI to set
	 */
	public void setOutputClassURI(String outputClassURI)
	{
		this.outputClassURI = outputClassURI;
	}

	/**
	 * @param restrictions the restrictions to set
	 */
	public void setRestrictions(Collection<RestrictionBean> restrictions)
	{
		this.restrictions = restrictions;
	}

	/**
	 * @return the restrictions
	 */
	public Collection<RestrictionBean> getRestrictions()
	{
		return restrictions;
	}
	
	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription()
	{
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description)
	{
		this.description = description;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return serviceURI;
	}
}
