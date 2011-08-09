package ca.wilkinsonlab.sadi.beans;

import java.io.Serializable;

/**
 * A simple class describing a property restriction.
 * @author LukeMcCarthy
 */
public class RestrictionBean implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private String onPropertyURI;
	private String onPropertyLabel;
	private String valuesFromURI;
	private String valuesFromLabel;
	
	public RestrictionBean()
	{
		onPropertyURI = null;
		onPropertyLabel = "";
		valuesFromURI = null;
		valuesFromLabel = "";
	}
	
	/**
	 * @return the onPropertyURI
	 */
	public String getOnPropertyURI()
	{
		return onPropertyURI;
	}

	/**
	 * @param onPropertyURI the onPropertyURI to set
	 */
	public void setOnPropertyURI(String onPropertyURI)
	{
		this.onPropertyURI = onPropertyURI;
	}

	/**
	 * @return the onPropertyLabel
	 */
	public String getOnPropertyLabel()
	{
		return onPropertyLabel;
	}

	/**
	 * @param onPropertyLabel the onPropertyLabel to set
	 */
	public void setOnPropertyLabel(String onPropertyLabel)
	{
		this.onPropertyLabel = onPropertyLabel;
	}

	/**
	 * @return the valuesFromURI
	 */
	public String getValuesFromURI()
	{
		return valuesFromURI;
	}

	/**
	 * @param valuesFromURI the valuesFromURI to set
	 */
	public void setValuesFromURI(String valuesFromURI)
	{
		this.valuesFromURI = valuesFromURI;
	}

	/**
	 * @return the valuesFromLabel
	 */
	public String getValuesFromLabel()
	{
		return valuesFromLabel;
	}

	/**
	 * @param valuesFromLabel the valuesFromLabel to set
	 */
	public void setValuesFromLabel(String valuesFromLabel)
	{
		this.valuesFromLabel = valuesFromLabel;
	}
}