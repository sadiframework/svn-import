package ca.wilkinsonlab.sadi.beans;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

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
		onPropertyLabel = null;
		valuesFromURI = null;
		valuesFromLabel = null;
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder buf = new StringBuilder();
		if (getOnPropertyLabel() != null)
			buf.append(getOnPropertyLabel());
		else
			buf.append(getOnPropertyURI());
		if (getValuesFromLabel() != null) {
			buf.append(" some ");
			buf.append(getValuesFromLabel());
		} else if (getValuesFromURI() != null) {
			buf.append(" some ");
			buf.append(getValuesFromURI());
		}
		return buf.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(27, 11)
			.append(this.onPropertyURI)
			.append(this.onPropertyLabel)
			.append(this.valuesFromURI)
			.append(this.valuesFromLabel)
			.toHashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null)
			return false;
		if (getClass() != o.getClass())
			return false;
		
		RestrictionBean that = (RestrictionBean)o;
		return new EqualsBuilder()
			.append(this.onPropertyURI, that.onPropertyURI)
			.append(this.onPropertyLabel, that.onPropertyLabel)
			.append(this.valuesFromURI, that.valuesFromURI)
			.append(this.valuesFromLabel, that.valuesFromLabel)
			.isEquals();
	}
}