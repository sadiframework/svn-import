package ca.wilkinsonlab.sadi.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit.Parser;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;

//import org.xml.sax.SAXException;
//import org.xml.sax.helpers.DefaultHandler;

public class ExampleQueries
{
	// in the default run configuration, path is relative to the project directory
//	private static final URL exampleQueriesPath = URI.create("http://biordf.net/cardioSHARE/queries.html").toURL();
	private static final String exampleQueriesPath = "../cardioSHARE/src/main/webapp/queries.html";
	
	private static final ExampleQueries theInstance = new ExampleQueries();

	private List<String> queries;
//	private DefaultHandler saxHandler;
	private ParserCallback swingHandler;
	
	private ExampleQueries()
	{
		queries = new ArrayList<String>();
//		saxHandler = new SaxQueryHandler();
		swingHandler = new SwingQueryHandler();
		
		Parser parser = new HTMLEditorKit().getParser();
		try {
//			InputStreamReader reader = new InputStreamReader(exampleQueriesPath.openStream());
			InputStreamReader reader = new InputStreamReader(new FileInputStream(exampleQueriesPath));
			parser.parse(reader, swingHandler, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		try {
//			SAXParserFactory.newInstance().newSAXParser().parse(exampleQueryPath, saxHandler);
//		} catch (ParserConfigurationException e) {
//			e.printStackTrace();
//		} catch (SAXException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
	
	public static List<String> getQueries()
	{
		return theInstance.queries;
	}
	
	public static String getQueryByHtmlListIndex(int index)
	{
		return theInstance.queries.get(index-1);
	}
	
	@SuppressWarnings("serial")
	private static class HTMLEditorKit extends javax.swing.text.html.HTMLEditorKit
	{
		public Parser getParser()
		{
			return super.getParser();
		}
	}
	
//	private class SaxQueryHandler extends DefaultHandler
//	{
//		String query;
//		
//		@Override
//		public void endElement(String uri, String localName, String qName) throws SAXException
//		{
//			if (qName.equalsIgnoreCase("xmp")) {
//				queries.add(query);
//			}
//		}
//		
//		@Override
//		public void characters(char[] ch, int start, int length) throws SAXException
//		{
//			query = new String(ch, start, length);
//		}
//	}
	
	private class SwingQueryHandler extends ParserCallback
	{
		String query;
		
		public void handleEndTag(HTML.Tag t, int pos)
		{
			if (t.toString().equalsIgnoreCase("xmp")) {
				queries.add(query);
			}
		}
		
		public void handleText(char[] data, int pos)
		{
			query = new String(data);
		}
	}
}
