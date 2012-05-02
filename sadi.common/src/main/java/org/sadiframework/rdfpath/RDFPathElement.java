package org.sadiframework.rdfpath;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sadiframework.beans.RestrictionBean;
import org.sadiframework.utils.LabelUtils;
import org.sadiframework.utils.OwlUtils;


import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class RDFPathElement
{
	Property property;
	Resource type;
	
	public RDFPathElement()
	{
		this(null, null);
	}
	
	public RDFPathElement(Property property)
	{
		this(property, null);
	}
	
	public RDFPathElement(Property property, Resource type)
	{
		this.property = property;
		this.type = type;
	}
	
	public RDFPathElement(Restriction r)
	{
		this(r.getOnProperty(), OwlUtils.getValuesFrom(r));
	}

	public Property getProperty()
	{
		return property;
	}

	public void setProperty(Property property)
	{
		this.property = property;
	}

	public Resource getType()
	{
		return type;
	}

	public void setType(Resource type)
	{
		this.type = type;
	}
	
	public RestrictionBean toRestrictionBean()
	{
		RestrictionBean bean = new RestrictionBean();
		bean.setOnPropertyURI(property.getURI());
		bean.setOnPropertyLabel(LabelUtils.getLabel(property));
		if (type != null) {
			bean.setValuesFromURI(type.getURI());
			bean.setValuesFromLabel(LabelUtils.getLabel(type));
		}
		return bean;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return String.format("%s some %s", property, ObjectUtils.defaultIfNull(type, "*"));
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
		
		RDFPathElement that = (RDFPathElement)o;
		return new EqualsBuilder()
			.append(this.property, that.property)
			.append(this.type, that.type)
			.isEquals();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(19, 11)
				.append(property)
				.append(type)
				.toHashCode();
	}
}
