package ca.wilkinsonlab.daggoo.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ca.wilkinsonlab.daggoo.LiftingMap;
import ca.wilkinsonlab.daggoo.OwlDatatypeMapping;
import ca.wilkinsonlab.daggoo.SAWSDLService;

public class IOUtils{

    private static DocumentBuilder docBuilder;
    private static Logger logger = Logger.getLogger(IOUtils.class.getName());    

    private static DocumentBuilder getDocBuilder(){
	if(docBuilder == null){
	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    dbf.setNamespaceAware(true);
	    try{
		docBuilder = dbf.newDocumentBuilder();
	    } catch(Exception e){
		e.printStackTrace();
	    }
	}
	return docBuilder;
    }

    /**
     * Returns the response payload from a SOAP request
     */
    public static String readFromConnection(HttpURLConnection conn) throws Exception {
	byte[] byteBufferChunk = new byte[100000];
	ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
	InputStream in = conn.getErrorStream();
	if (in == null) {
	    in = conn.getInputStream();
	}
	try{
	    for(int r = in.read(byteBufferChunk, 0, 100000);
		r != -1; 
		r = in.read(byteBufferChunk, 0, 100000)){
		byteBuffer.write(byteBufferChunk, 0, r);
	    }
	}catch(Exception e){
 	    System.err.println("Exception while reading from URL, will try to parse anyway: " + e.getMessage()); 
 	}
	try{
	    if(conn.getResponseCode() >= 400){
		throw new IOException("HTTP Error ("+conn.getResponseCode()+"): "+conn.getResponseMessage());
	    }
	} catch(Exception e){
	    if(e instanceof IOException){
		throw e;
	    }
	    else{
		logger.log(Level.SEVERE, "Could not get response code, but attempting to retrieve response", e);
	    }
	}
	return byteBuffer.toString();
    }
    
    /**
     * Returns the response payload from a SOAP request
     */
    public static String readFromConnection(URLConnection conn) throws Exception {
	byte[] byteBufferChunk = new byte[100000];
	ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
	InputStream in = conn.getInputStream();
	
	try{
	    for(int r = in.read(byteBufferChunk, 0, 100000);
		r != -1; 
		r = in.read(byteBufferChunk, 0, 100000)){
		byteBuffer.write(byteBufferChunk, 0, r);
	    }
	}catch(Exception e){
 	    System.err.println("Exception while reading from URL, will try to parse anyway: " + e.getMessage()); 
 	}
	
	return byteBuffer.toString();
    }
    
    /**
     * Returns the response payload from a SOAP request
     */
    public static String readFromStream(InputStream in) throws Exception {
	byte[] byteBufferChunk = new byte[100000];
	ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
	
	try{
	    for(int r = in.read(byteBufferChunk, 0, 100000);
		r != -1; 
		r = in.read(byteBufferChunk, 0, 100000)){
		byteBuffer.write(byteBufferChunk, 0, r);
	    }
	}catch(Exception e){
 	    System.err.println("Exception while reading from InputStream, will try to parse anyway: " + e.getMessage()); 
 	}	
	return byteBuffer.toString();
    }
    
    public static String getLoweringOrLiftingTemplateFromXML(InputStream XML) throws Exception {
	Document respDoc = getDocBuilder().parse(XML);
	if (respDoc != null) {
	    NodeList nodes = respDoc.getDocumentElement().getChildNodes();
	    for (int x = 0; x < nodes.getLength(); x++) {
		Node n = nodes.item(x);
		if (n != null && n.getNodeType() == Node.ELEMENT_NODE && n.getLocalName().equals("template")) {
		    if (n.hasChildNodes()) {
			NodeList children = n.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
			    Node c = children.item(i);
			    // there should only be at most one node below the template element
			    if (c != null && c.getNodeType() == Node.ELEMENT_NODE ) {
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				//initialize StreamResult with File object to save to file
				StreamResult result = new StreamResult(new StringWriter());
				DOMSource source = new DOMSource(c);
				transformer.transform(source, result);
				String xmlString = result.getWriter().toString();
				return xmlString;
			    }
			}
			// hmm, we got here, so probably not RDF template, but a textual one?
			return n.getTextContent() != null ? n.getTextContent().trim() : "";
		    }
		}
	    }
	}	
	throw new Exception("Could not find the RDF child element in the lowering or lifting schema:\n" + XML);
    }
    
    public static String getLoweringOrLiftingTemplateFromXML(File XML) throws Exception {
	return getLoweringOrLiftingTemplateFromXML(new FileInputStream(XML));
    }
    
    public static String getLoweringOrLiftingTemplateFromXML(String XML) throws Exception {
	return getLoweringOrLiftingTemplateFromXML(new ByteArrayInputStream(XML.getBytes()));
    }
    
    public static String getSPARQLFromXML(InputStream XML) throws Exception {
	Document respDoc = getDocBuilder().parse(XML);
	if (respDoc != null) {
	    NodeList nodes = respDoc.getDocumentElement().getChildNodes();
	    for (int x = 0; x < nodes.getLength(); x++) {
		Node n = nodes.item(x);
		if (n != null && n.getNodeType() == Node.ELEMENT_NODE && n.getLocalName().equals("sparqlQuery")) {
		    return n.getTextContent() != null ? n.getTextContent().trim() : "";
		}
	    }
	}
	throw new Exception("Could not find the SPARQL query in the lowering or lifting schema:\n" + XML);
    }
    
    public static String getSPARQLTemplateFromXML(File XML) throws Exception {
	return getSPARQLFromXML(new FileInputStream(XML));
    }
    
    public static String getSPARQLFromXML(String XML) throws Exception {
	return getSPARQLFromXML(new ByteArrayInputStream(XML.getBytes()));
    }
    
    public static List<LiftingMap> getLiftingMappings(String XML) throws Exception {
	return getLiftingMappings(new ByteArrayInputStream(XML.getBytes()));
    }
    
    public static List<LiftingMap> getLiftingMappings(InputStream XML) throws Exception {
	ArrayList<LiftingMap> mappings = new ArrayList<LiftingMap>();
	Document respDoc = getDocBuilder().parse(XML);
	if (respDoc != null) {
	    NodeList nodes = respDoc.getDocumentElement().getChildNodes();
	    for (int x = 0; x < nodes.getLength(); x++) {
		Node n = nodes.item(x);
		if (n != null && n.getNodeType() == Node.ELEMENT_NODE && n.getLocalName().equals("mapping")) {
		    if (n.hasChildNodes()) {
			NodeList children = n.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
			    Node c = children.item(i);
			    if (c != null && c.getNodeType() == Node.ELEMENT_NODE && c.getLocalName().equals("map")) {
				String id, type, value;
				id = c.getAttributes().getNamedItem("id").getNodeValue();
				type = c.getAttributes().getNamedItem("type").getNodeValue();
				value = c.getTextContent();
				LiftingMap lm = new LiftingMap(id, type, value);
				mappings.add(lm);
			    }
			}
		    }
		}
	    }
	}
	return mappings;
    }
    
    public static SAWSDLService[] getSAWSDLServices(File file) throws SAXException, IOException {
	ArrayList<SAWSDLService> services = new ArrayList<SAWSDLService>();
	
	Document respDoc = getDocBuilder().parse(file);
	if (respDoc != null) {
	    NodeList nodes = respDoc.getElementsByTagName("service");
	    for (int x = 0; x < nodes.getLength(); x++) {
		Node n = nodes.item(x);
		if (n != null && n.getNodeType() == Node.ELEMENT_NODE) {
		    String name, wsdl, lifting, lowering, owl;
		    name = n.getAttributes().getNamedItem("name").getNodeValue();
		    wsdl = n.getAttributes().getNamedItem("sawsdl").getNodeValue();
		    owl = n.getAttributes().getNamedItem("owl").getNodeValue();
		    lifting = n.getAttributes().getNamedItem("lifting").getNodeValue();
		    lowering = n.getAttributes().getNamedItem("lowering").getNodeValue();
		    if (name != null && wsdl != null) {
			if (!name.isEmpty() && !wsdl.isEmpty()) {
			    SAWSDLService s = new SAWSDLService();
			    s.setName(name);
			    s.setWsdlLocation(wsdl);
			    s.setLiftingSchemaLocation(lifting);
			    s.setLoweringSchemaLocation(lowering);
			    s.setOwlClassLocation(owl);
			    services.add(s);
			}
		    }
		}
	    }
	}
	
	return services.toArray(new SAWSDLService[]{});
    }
    
    public static boolean addSAWSDLService(File file, String name, String sawsdl, String owl, String lowering, String lifting) {
	System.out.println(String.format("ServiceMapping: %s", file.getAbsolutePath()));
	Document respDoc = null;
	try {
	    respDoc = getDocBuilder().parse(file);
	} catch (SAXException e1) {
	    e1.printStackTrace();
	    return false;
	} catch (IOException e1) {
	    e1.printStackTrace();
	    return false;
	}
	if (respDoc != null) {
	    Element e = respDoc.createElement("service");
	    e.setAttribute("name", name);
	    e.setAttribute("owl", owl);
	    e.setAttribute("sawsdl", sawsdl);
	    e.setAttribute("lowering", lowering);
	    e.setAttribute("lifting", lifting);
	    respDoc.getDocumentElement().appendChild(e);
	    try {
		serialize(respDoc, new FileOutputStream(file));
	    } catch (FileNotFoundException e1) {
		e1.printStackTrace();
		return false;
	    } catch (Exception e1) {
		e1.printStackTrace();
		return false;
	    }
	}	
	return true;
    }
    
    public static void returnFile(String filename, OutputStream out) throws FileNotFoundException, IOException {
	InputStream in = null;
	try {
	    in = new BufferedInputStream(new FileInputStream(filename));
	    byte[  ] buf = new byte[4 * 1024];  // 4K buffer
	    int bytesRead;
	    while ((bytesRead = in.read(buf)) != -1) {
		out.write(buf, 0, bytesRead);
	    }
	}
	finally {
	    if (in != null) in.close(  );
	}
    }

    public static void serialize(Document doc, OutputStream out) throws Exception {

	TransformerFactory tfactory = TransformerFactory.newInstance();
	Transformer serializer;
	try {
	    serializer = tfactory.newTransformer();
	    // Setup indenting to "pretty print"
	    serializer.setOutputProperty(OutputKeys.INDENT, "yes");
	    serializer.setOutputProperty(
		    "{http://xml.apache.org/xslt}indent-amount", "2");

	    serializer.transform(new DOMSource(doc), new StreamResult(out));
	} catch (TransformerException e) {
	    // this is fatal, just dump the stack and throw a runtime exception
	    e.printStackTrace();

	    throw new RuntimeException(e);
	}
    }
    
    public static String GenerateLoweringSchema(String sparql, String template) {
	Document doc = getDocBuilder().newDocument();
	Element lowering = doc.createElement("lowering");
	Element sparqlQuery = doc.createElement("sparqlQuery");
	sparqlQuery.setTextContent(sparql);
	lowering.appendChild(sparqlQuery);
	Element templateElement = doc.createElement("template");
	templateElement.setTextContent(template);
	lowering.appendChild(templateElement);
	doc.appendChild(lowering);
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	String s = null;
	try {
	    serialize(doc, baos);
	    s = baos.toString();
	    if (baos != null) {
		baos.close();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    return "<lowering><sparqlQuery/><template/></lowering>";
	}
	return s;
    }
    
    public static String GenerateLiftingSchema(ArrayList<OwlDatatypeMapping> mappings, String template) {
	Document doc = getDocBuilder().newDocument();
	Element lifting = doc.createElement("lifting");
	Element tem = doc.createElement("template");
	Element mapping = doc.createElement("mapping");
	
	tem.setTextContent(template);
	mapping.appendChild(createMappingElement(doc.createElement("map"), "string", "inputNodeURI", null));
	for (OwlDatatypeMapping m : mappings) {
	    if (m.isArray()) {
		mapping.appendChild(createMappingElement(doc.createElement("map"), "xpath", m.getSoapId(), String.format("//%s/item/text()", m.getSoapId())));
	    } else {
		mapping.appendChild(createMappingElement(doc.createElement("map"), "xpath", m.getSoapId(), String.format("//%s/text()",m.getSoapId())));
	    }
	}
	lifting.appendChild(mapping);
	lifting.appendChild(tem);
	doc.appendChild(lifting);
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	String s = null;
	try {
	    serialize(doc, baos);
	    s = baos.toString();
	    if (baos != null) {
		baos.close();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    return "<lifting><mapping/><template/></lifting>";
	}
	return s;
    }
    
    private static Element createMappingElement(Element e, String type, String id, String text) {
	if (e == null) {
	    return e;
	}
	e.setAttribute("id", id);
	e.setAttribute("type", type);
	if (text != null && !text.trim().equals("")) {
	    e.setTextContent(text);
	}
	return e;
    }

    public static Map<String, Object> ParseSoapMappings(String xml) throws Exception{
	HashMap<String, Object> map = new HashMap<String, Object>();
	
	Document respDoc = getDocBuilder().parse(new ByteArrayInputStream(xml.getBytes()));
	if (respDoc != null) {
	    NodeList nodes = respDoc.getElementsByTagName("template");
	    // only one sadiSoapMapping
	    for (int x = 0; x < nodes.getLength(); x++) {
		Node n = nodes.item(x);
		if (n != null && n.getNodeType() == Node.ELEMENT_NODE) {
		    NodeList children = n.getChildNodes();
		    for (int y = 0; y <children.getLength(); y++) {
			Node child = children.item(y);
			// direct children of template only!
			if (child != null && child.getNodeType() == Node.ELEMENT_NODE && child.getParentNode() != null && child.getParentNode().equals(n)) {
			    String name = child.getLocalName(); // the soap name and the key into our map!
			    // is this input an array of items?
			    XPath xPath = null;
			    try {
				if (xPath == null) {
				    xPath = (new org.apache.xpath.jaxp.XPathFactoryImpl()).newXPath();
				}
			    } catch (Exception e) {
				logger.log(Level.SEVERE,"Could not create an XPath: " + e.getMessage(), e);
				continue;
			    }
			    NodeList resultNodes = null;
			    XPathExpression xPathExp;
			    try {
				xPathExp = xPath.compile(String.format("//%s/item/text()", name));
				resultNodes = (NodeList) xPathExp.evaluate(n, XPathConstants.NODESET);
			    } catch (Exception e) {
				e.printStackTrace();
				continue;
			    }
			    if (resultNodes.getLength() > 0) {
				String[] _nodes = new String[resultNodes.getLength()];
				for (int i = 0; i < resultNodes.getLength(); i++) {
				    _nodes[i] = resultNodes.item(i).getTextContent();
				}
				map.put(name, _nodes);
			    } else {
				// not an array of items
				map.put(name, child.getTextContent().trim());
			    }
			}
		    }
		}
	    }
	}
	return map;
    }
    public static void main(String[] args) throws Exception{
	Map<String, Object> map = IOUtils.ParseSoapMappings("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + 
			"<template>\r\n" + 
			"<pathway_id>hsa:000987</pathway_id>\r\n" + 
			"<pathway_ids><item>hsa:000987</item><item>hsa:000988</item><item>hsa:000989</item></pathway_ids>\r\n" + 
			"<id>some single id</id>\r\n" + 
			"<ids><item>1</item><item>2</item><item>3</item></ids>\r\n" + 
			"</template>");
	for (String key : map.keySet()) {
	    Object o = map.get(key);
	    if (o instanceof String[]) {
		System.out.println(key + " values:");
		for (String s : (String[])o) {
		    System.out.println("\t" + s);
		}
	    } else {
		System.out.println(key + " " + o);
	    }
	}
	
    }
}
