package ca.wilkinsonlab.sadi.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import org.apache.commons.io.IOUtils;

public class SPARQLResultsXMLUtils
{
	public final static Logger log = Logger.getLogger(SPARQLResultsXMLUtils.class);
	
	public static List<Map<String, String>> getResultsFromSPARQLXML(String input) throws IOException
	{
		InputStream inputStream = IOUtils.toInputStream(input);
		return getResultsFromSPARQLXML(inputStream);
	}

	public static List<Map<String, String>> getResultsFromSPARQLXML(InputStream input) throws IOException
	{
		/*
		String resultsXML = IOUtils.toString(input);
		log.debug("resultsXML:" + resultsXML);
		input = IOUtils.toInputStream(resultsXML);
		*/
		
		SPARQLResultsXMLHandler handler = new SPARQLResultsXMLHandler();
		try {
			XMLReader parser = XMLReaderFactory.createXMLReader();
			parser.setContentHandler(handler);
			parser.parse(new InputSource(input));
		}
		catch(SAXException e) {
			IOException e2 = new IOException("error parsing SPARQL results XML");
			e2.initCause(e);
			throw e2;
		}
		return handler.getResults();
	}
	
	private static class SPARQLResultsXMLHandler extends DefaultHandler
	{
		List<Map<String,String>> results;

		StringBuffer characterData = null;
		Map<String,String> currentBindings = null;
		String currentVariable = null;
		boolean insideURIorLiteralorBNodeElement = false; 
		
		private static final String SPARQL_RESULTS_NS = "http://www.w3.org/2005/sparql-results#";

		private static final String RESULT_TAG = "result";
		private static final String BINDING_TAG = "binding";
		private static final String VARNAME_ATTR = "name";
		// These tags enclose a value that is assigned to a variable.
		private static final String URI_TAG = "uri";
		private static final String LITERAL_TAG = "literal";
		private static final String BNODE_TAG = "bnode";
		
		public SPARQLResultsXMLHandler() 
		{
			results = new ArrayList<Map<String,String>>();
		}
		
		public List<Map<String,String>> getResults () 
		{
			return results;
		}
		
		@Override
		public void startElement(String namespaceURI, String localName, String qName, Attributes attribs)
		{
//			log.debug("start tag: " + namespaceURI + localName);
			
			if(namespaceURI.equals(SPARQL_RESULTS_NS)) {
				if(localName.equals(RESULT_TAG)) {
					currentBindings = new HashMap<String,String>();
				}
				else if(localName.equals(BINDING_TAG)) {
					currentVariable = attribs.getValue(VARNAME_ATTR);
				}
				else if(localName.equals(URI_TAG) || localName.equals(LITERAL_TAG) || localName.equals(BNODE_TAG)) {
					insideURIorLiteralorBNodeElement = true;
					characterData = new StringBuffer();
				}
			}
		}
		
		@Override
		public void characters(char[] text, int start, int length)
		{
			if(insideURIorLiteralorBNodeElement)
				characterData.append(text, start, length);
		}
		
		@Override
		public void endElement(String namespaceURI, String localName, String qName) throws SAXException 
		{
//			log.debug("end tag: " + namespaceURI + localName);

			if(namespaceURI.equals(SPARQL_RESULTS_NS)) {
				if(localName.equals(RESULT_TAG)) {
					/* Virtuoso has a bug where (in rare circumstances) it will generate 
					 * empty result rows (e.g. <result></result>). However, if we add an 
					 * empty set of bindings to the result set, it will cause problems higher 
					 * up (null pointers). -- BV */ 
					if(currentBindings.size() > 0) {
						results.add(currentBindings);
					}
				}
				else if(localName.equals(BINDING_TAG)) {
					currentBindings.put(currentVariable, characterData.toString());
				}
				else if(localName.equals(URI_TAG) || localName.equals(LITERAL_TAG) || localName.equals(BNODE_TAG)) {
					insideURIorLiteralorBNodeElement = false;
				}
			}
		
		}
	}
}
