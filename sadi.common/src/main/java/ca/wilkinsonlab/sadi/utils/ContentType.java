package ca.wilkinsonlab.sadi.utils;

import java.io.OutputStream;
import java.io.Writer;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFWriter;

public enum ContentType
{
	RDFXML	("application/rdf+xml", "RDF/XML-ABBREV"),
	N3 ("text/rdf+n3", "N3");
	
	public static ContentType getContentType(String httpHeader) 
	{
		for (ContentType type: ContentType.values()) {
			if (type.getHTTPHeader().equals(httpHeader))
				return type;
		}
		return null;
	}
	
	private final String httpHeader;
	private final String jenaLanguage;
	
	ContentType(String httpHeader, String jenaLanguage)
	{
		this.httpHeader = httpHeader;
		this.jenaLanguage = jenaLanguage;
	}
	
	public String getHTTPHeader()
	{
		return httpHeader;
	}
	
	public String getJenaLanguage()
	{
		return jenaLanguage;
	}
	
	public void writeModel(Model model, Writer writer, String base)
	{
		RDFWriter rdf = model.getWriter(getJenaLanguage());
		rdf.write(model, writer, base);
	}
	
	public void writeModel(Model model, OutputStream out, String base)
	{

		RDFWriter rdf = model.getWriter(getJenaLanguage());
		rdf.write(model, out, base);
	}
}
