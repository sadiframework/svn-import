package ca.wilkinsonlab.sadi.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.RDFWriter;

public enum ContentType
{
	RDF_XML	("application/rdf+xml", "RDF/XML-ABBREV"),
	N3 ("text/rdf+n3", "N3"),
	N3_1 ("text/n3", "N3");
	
	public static ContentType getContentType(String httpHeader) 
	{
		for (ContentType type: ContentType.values()) {
			if (type.getHTTPHeader().equals(httpHeader))
				return type;
		}
		return null;
	}
	
	private static Collection<ContentType> uniqueContentTypes = null;
	public static Collection<ContentType> getUniqueContentTypes()
	{
		if (uniqueContentTypes == null) {
			uniqueContentTypes = new ArrayList<ContentType>(ContentType.values().length);
			Set<String> seen = new HashSet<String>();
			for (ContentType type: ContentType.values()) {
				if (!seen.contains(type.getJenaLanguage())) {
					uniqueContentTypes.add(type);
					seen.add(type.getJenaLanguage());
				}
			}
		}
		return uniqueContentTypes;
	}
	
	private static Set<String> uniqueJenaLanguages = null;
	public static Iterable<String> getUniqueJenaLanguages()
	{
		if (uniqueJenaLanguages == null) {
			Set<String> langs = new HashSet<String>();
			for (ContentType type: ContentType.values())
				langs.add(type.getJenaLanguage());
			uniqueJenaLanguages = Collections.unmodifiableSet(langs);
		}
		return uniqueJenaLanguages;
	}
	
	private final String httpHeader;
	private final String jenaLanguage;
	
	private ContentType(String httpHeader, String jenaLanguage)
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
	
	public void readModel(Model model, InputStream in, String base)
	{
		RDFReader rdf = model.getReader(getJenaLanguage());
		rdf.read(model, in, base);
	}
}
