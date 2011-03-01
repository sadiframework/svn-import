package ca.wilkinsonlab.daggoo.utils;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.wilkinsonlab.daggoo.CachedNamespaceContextImpl;
import ca.wilkinsonlab.daggoo.SADISpecWrapper;
import ca.wilkinsonlab.daggoo.SadiPrefixResolver;

/**
 * Used to expose a WSDL file enhanced with SAWSDL and Sadi markup as a Sadi
 * service.
 * 
 */
@SuppressWarnings({"rawtypes","unused"})
public class WSDLConfig extends SADISpecWrapper {

    public static final String SERVICE_NAME_ATTR = "serviceName";

    public static final String SERVICE_DESC_ATTR = "serviceDesc";

    public static final String SERVICE_AUTH_ATTR = "serviceAuthority";

    public static final String SERVICE_CONTACT_ATTR = "serviceContact";

    public static final String REGISTRY_ATTR = "registryEndpoint";

    public static final String DEFAULT_AUTHORITY = "sadiframework.org";

    public static final String SAWSDL_MODEL_ATTR = "modelReference";

    public static final String SAWSDL_INMAP_ATTR = "loweringSchemaMapping";

    public static final String SAWSDL_OUTMAP_ATTR = "liftingSchemaMapping";

    public static final String SADI_SECONDARY_SOURCE_ATTR = "secondaryParamSource";

    private final String ARRAY_SENTINEL = "applyToEachXSIArrayItem";

    private final String SERVICENAME_ATTR_XPATH = "//sawsdl:attrExtensions";

    // private final String SERVICENAME_ATTR_XPATH =
    // "//*[@sadi:"+SERVICE_NAME_ATTR+"]";
    // note that the XPath above doesn't work, not sure why at the moment, so we
    // don't support WSDL2.0 yet...

    private static XPath xPath;

    private static DocumentBuilder docBuilder;

    private Document wsdlDoc;

    private URL wsdlURL;

    private Map<String, QName> sadiServiceName2Service;

    private Map<String, QName> sadiServiceName2Port;

    private Map<String, QName> sadiServiceName2OpInput;

    private Map<String, QName> sadiServiceName2OpOutput;

    private Map<String, String> sadiServiceName2Op;

    private Map<String, String> sadiServiceName2SoapAction;
    
    private Map<String, URL> sadiServiceName2SoapAddressLocation;

    private Map<String, String> sadiServiceName2TargetNamespaceURI;

    private Map<String, String> sadiServiceName2Style; // document or rpc

    private Map<String, String> sadiServiceName2Encoding; // literal or encoded

    private Map<String, Map<String, String>> sadiServiceName2InputXSDTypes;

    private Map<String, Map<String, String>> sadiServiceName2OutputXSDTypes;

    private static Logger logger = Logger
	    .getLogger(WSDLConfig.class.toString());

    static {

	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	dbf.setNamespaceAware(true);
	try {
	    docBuilder = dbf.newDocumentBuilder();
	} catch (Exception e) {
	    e.printStackTrace();
	}

	// Commented out generic factory due to Google App Engine bug
	// XPathFactory xPathFactory = XPathFactory.newInstance();
	XPathFactory xPathFactory = new org.apache.xpath.jaxp.XPathFactoryImpl();
	try {
	    xPath = xPathFactory.newXPath();
	    // xPath.setNamespaceContext(new NamespaceContextImpl(new
	    // HashMap<String, String>()));
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    /**
     * C-tor to use when you'll programmatically set the values rarther than
     * getting them from a SAWSDL file.
     */
    public WSDLConfig() {
	sadiServiceName2Service = new HashMap<String, QName>();
	sadiServiceName2Port = new HashMap<String, QName>();
	sadiServiceName2SoapAction = new HashMap<String, String>();
	sadiServiceName2Style = new HashMap<String, String>(); // document or
							       // rpc
	sadiServiceName2Encoding = new HashMap<String, String>(); // literal or
								  // encoded
	sadiServiceName2Op = new HashMap<String, String>();
	sadiServiceName2OpInput = new HashMap<String, QName>();
	sadiServiceName2OpOutput = new HashMap<String, QName>();
	sadiServiceName2InputXSDTypes = new HashMap<String, Map<String, String>>();
	sadiServiceName2OutputXSDTypes = new HashMap<String, Map<String, String>>();
	sadiServiceName2TargetNamespaceURI = new HashMap<String, String>();
	sadiServiceName2SoapAddressLocation = new HashMap<String, URL>();

    }

    public WSDLConfig(URL url) throws Exception {
	this();
	wsdlURL = url;

	setSpecURL(wsdlURL);
	parse(wsdlURL.openStream());
    }

    /**
     * General <i>modus operandi</i>:
     * <ul>
     * <li>Find the sadi:serviceName attributes</li>
     * <li>Trace back for each the operation name with which it is associated
     * (and ensure other required sadi: attrs are defined)</li>
     * <li>Trace back the message definitions for the operation (and ensure
     * SAWSDL schema lifting/lower attrs are okay)</li>
     * <li>Trace back the service and port type with which the operation is
     * associated</li>
     * </ul>
     * 
     * This procedure lets us blissfully ignore any non-Sadi SASWDL operations.
     */
    protected void parse(InputStream is) throws Exception {
	wsdlDoc = docBuilder.parse(is);
	xPath.setNamespaceContext(new CachedNamespaceContextImpl(wsdlDoc, false));
	// put wsdl imports in-place as pertinent parts of the definition
	// of the service maybe spread over multiple files.
	doImports(wsdlDoc, wsdlURL);

	// Find the sadi:serviceName attributes
	parseSadiServiceSpecs();
	parseSOAPMessageSpecs();
    }

    // Finds out the semantic mapping data for the I/O of each operation being
    // wrapped as a Sadi service
    public void parseSOAPMessageSpecs() throws Exception {
	for (String serviceName : getServiceNames()) {
	    setCurrentService(serviceName);
	    String currentInputMessage = getOperationInputQName()
		    .getLocalPart();
	    String currentOutputMessage = getOperationOutputQName()
		    .getLocalPart();

	    NodeList messageElements = wsdlDoc.getDocumentElement()
		    .getElementsByTagNameNS(SadiPrefixResolver.WSDL_NAMESPACE,
			    "message");
	    for (int i = 0; i < messageElements.getLength(); i++) {
		String messageName = ((Element) messageElements.item(i))
			.getAttribute("name");
		if (currentInputMessage.equals(messageName)) {
		    parseSOAPMessageSpec((Element) messageElements.item(i),
			    messageName, true);
		} else if (currentOutputMessage.equals(messageName)) {
		    parseSOAPMessageSpec((Element) messageElements.item(i),
			    messageName, false);
		}
	    }
	}
    }

    private URL parseSoapAddressLocation(QName targetPort) throws Exception {
	if(targetPort == null || targetPort.getLocalPart() == null){
	    throw new Exception("Asked for SOAP endpoint with a null port name");
	}
	URL endpoint = null;
	NodeList ports = wsdlDoc.getDocumentElement().getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "port");
	for(int i = 0; i < ports.getLength(); i++){
	    Element portElem = (Element) ports.item(i);
	    String portName = portElem.getAttribute("name");
	    if(targetPort.getLocalPart().equals(portName)){
		NodeList addrs = portElem.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/soap/", "address");
		for(int j = 0; j < addrs.getLength(); j++){
		    Element addrElem = (Element) addrs.item(j);
		    String location = addrElem.getAttribute("location");
		    if(location != null && location.length() != 0){
			endpoint = new URL(location);
			logger.log(Level.SEVERE,
				"####The SOAP endpoint is " + endpoint);
			return endpoint;
		    }
		}
	    }
	}
	return endpoint;
    }
    
    private String parseSoapAction(String operationName) throws Exception {
	if(operationName == null || operationName.trim().isEmpty()){
	    throw new Exception("Asked for SOAP Action header with an empty operation name");
	}
	String endpoint = "";
	NodeList ports = wsdlDoc.getDocumentElement().getElementsByTagNameNS(SadiPrefixResolver.WSDL_NAMESPACE, "operation");
	for(int i = 0; i < ports.getLength(); i++){
	    Element portElem = (Element) ports.item(i);
	    String portName = portElem.getAttribute("name");
	    if(operationName.equals(portName)){
		NodeList addrs = portElem.getElementsByTagNameNS(SadiPrefixResolver.SOAP_NAMESPACE, "operation");
		for(int j = 0; j < addrs.getLength(); j++){
		    Element addrElem = (Element) addrs.item(j);
		    String soapAction = addrElem.getAttribute("soapAction");
		    if(soapAction != null && !soapAction.isEmpty()){
			//if (soapAction.indexOf("#") > 0)
			//    soapAction = soapAction.substring(0, soapAction.indexOf("#"));
			return soapAction;
		    }
		}
	    }
	}
	return endpoint;
    }
    
    
    private void parseSOAPMessageSpec(Element messageElement,
	    String messageName, boolean isInputMessage) throws Exception {
	NodeList partElements = messageElement.getElementsByTagNameNS(
		SadiPrefixResolver.WSDL_NAMESPACE, "part");
	Map<String, String> inputs = new HashMap<String, String>();
	Map<String, String> inputMappings = new HashMap<String, String>();
	Map<String, String> inputTypes = new HashMap<String, String>();
	Map<String, String> outputs = new HashMap<String, String>();
	Map<String, String> outputMappings = new HashMap<String, String>();
	Map<String, String> outputTypes = new HashMap<String, String>();

	for (int i = 0; i < partElements.getLength(); i++) {
	    Element partElement = (Element) partElements.item(i);
	    String partName = partElement.getAttribute("name");
	    if (partName == null || partName.length() == 0) {
		throw new Exception(
			"A part element has no name in the definition of the WSDL message named "
				+ messageName);
	    }
	    String partType = partElement.getAttribute("type");
	    // Here RPC/Encoded and Document Literal start to differ, as Doc/Lit
	    // will refer the element definitions,
	    // whereas RPC/Encoded will define the types right here.
	    if (partType == null || partType.length() == 0) {
		String elementType = partElement.getAttribute("element");
		if (elementType == null || elementType.trim().length() == 0) {
		    throw new Exception(
			    "Part element "
				    + partName
				    + " of WSDL message "
				    + messageName
				    + " has no type (rpc/encoded) or element (doc/lit) attribute defined");
		}
		// A qualified name?
		if (elementType.indexOf(":") != -1) {
		    elementType = elementType.substring(elementType
			    .indexOf(":") + 1);
		}
		// Find the definition, it should have the sawsdl stuff in it
		NodeList elementDefs = (NodeList) xPath.evaluate(
			"//xsd:element[@name='" + elementType + "']", wsdlDoc,
			XPathConstants.NODESET);
		if (elementDefs == null || elementDefs.getLength() == 0) {
		    throw new Exception("The definition of the element '"
			    + elementType + "' (for message " + messageName
			    + ", part " + partName
			    + ") could not be found in the WSDL file.");
		}
		if (elementDefs.getLength() != 1) {
		    throw new Exception(
			    "Found more than one definition for element "
				    + elementType + " in the WSDL file");
		}
		// Find the sub elements that make up the document element
		// contents, each should have its one
		// schema mapping (we don't yet support 1:n sadi:xmlschema
		// mappings, or "deep" mappings of nested subelements).
		NodeList subElements = ((Element) elementDefs.item(0)).getElementsByTagName("element");
		if (subElements.getLength() == 0 && !((Element) elementDefs.item(0)).getLocalName().equals("element")) {
		    for (int j = 0; j < subElements.getLength(); j++) {
			Element subElement = (Element) subElements.item(j);
			String subPartName = subElement.getAttribute("name");
			String subPartType = subElement.getAttribute("type");
			System.out.println("type: " + subPartType);
			if (subPartType == null || subPartType.trim().length() == 0) {
			    throw new Exception("No type attribute was found for "
				    + elementType + " subelement " + subPartName);
			}
			addIOMapping(subElement, messageName, subPartType,
				subPartName, inputs, inputMappings, inputTypes,
				outputs, outputMappings, outputTypes,
				isInputMessage);
		    }
		} else {
		    Element subElement = (Element) elementDefs.item(0);
		    String subPartName = subElement.getAttribute("name");
		    String subPartType = subElement.getAttribute("type");
		    // System.out.println("subPartType{else}: " + subPartType);
		    if (subPartType == null || subPartType.trim().length() == 0) {
			throw new Exception("No type attribute was found for "
				+ elementType + " subelement " + subPartName);
		    }
		    addIOMapping(subElement, messageName, subPartType,
			    subPartName, inputs, inputMappings, inputTypes,
			    outputs, outputMappings, outputTypes,
			    isInputMessage);
		}
	    } else {
		addIOMapping(partElement, messageName, partType, partName,
			inputs, inputMappings, inputTypes, outputs,
			outputMappings, outputTypes, isInputMessage);
	    }
	}
	if (isInputMessage) {
	    setPrimaryInputs(inputs);
	    setPrimaryInputFormats(inputMappings);
	    setInputXSDTypes(inputTypes);
	} else { // it's output
	    setPrimaryOutputs(outputs);
	    setPrimaryOutputFormats(outputMappings);
	    setOutputXSDTypes(outputTypes);
	}
    }

    private void addIOMapping(Element partElement, String messageName,
	    String partType, String partName, Map<String, String> inputs,
	    Map<String, String> inputMappings, Map<String, String> inputTypes,
	    Map<String, String> outputs, Map<String, String> outputMappings,
	    Map<String, String> outputTypes, boolean isInputMessage)
	    throws Exception {
	String modelReference = partElement.getAttributeNS(
		SadiPrefixResolver.SAWSDL_NAMESPACE, SAWSDL_MODEL_ATTR);
	if (modelReference == null || modelReference.length() == 0) {
	    // TODO deal with schema-only level SAWSDL annotation
	    throw new Exception("Part element " + partName
		    + " of WSDL message " + messageName + " has no SAWSDL "
		    + SAWSDL_MODEL_ATTR + " attribute defined");
	}
	String schemaMapping = null;
	if (isInputMessage) {
	    schemaMapping = partElement.getAttributeNS(
		    SadiPrefixResolver.SAWSDL_NAMESPACE, SAWSDL_INMAP_ATTR);
	    if (schemaMapping == null || schemaMapping.length() == 0) {
		throw new Exception("Part element " + partName
			+ " of WSDL message " + messageName + " has no SAWSDL "
			+ SAWSDL_INMAP_ATTR + " attribute defined");
	    }
	    String sadiParam = wsdlParam2SadiParam(partElement, partName,
		    partType, modelReference);
	    if (sadiParam == null) {

	    } else {
		inputs.put(partName, sadiParam);
		inputTypes.put(partName, partType);
		inputMappings.put(partName, schemaMapping);
	    }
	} else {
	    schemaMapping = partElement.getAttributeNS(
		    SadiPrefixResolver.SAWSDL_NAMESPACE, SAWSDL_OUTMAP_ATTR);
	    if (schemaMapping == null || schemaMapping.length() == 0) {
		throw new Exception("Part element " + partName
			+ " of WSDL message " + messageName + " has no SAWSDL "
			+ SAWSDL_OUTMAP_ATTR + " attribute defined");
	    }
	    outputs.put(
		    partName,
		    wsdlParam2SadiParam(partElement, partName, partType,
			    modelReference));
	    outputTypes.put(partName, partType);
	    outputMappings.put(partName, schemaMapping);
	}
    }

    public String wsdlParam2SadiParam(Element element, String paramName,
	    String paramXSDType, String dataTypeLSID) throws Exception {

	// See if the name is namespace qualified, and separate the local part
	// if so
	String nsURI = "";
	if (paramXSDType.contains(":")) {
	    String nsPrefix = paramXSDType.substring(0,
		    paramXSDType.indexOf(":")); // XML NS prefix
	    paramXSDType = paramXSDType
		    .substring(paramXSDType.indexOf(":") + 1); // local part
	    nsURI = element.lookupNamespaceURI(nsPrefix); // prefix->URI
	}
	Element schemaDataTypeElement = isBasicType(paramXSDType) ? null
		: getSchemaElement(element.getOwnerDocument(), nsURI,
			paramXSDType);
	boolean isArray = getArrayType(schemaDataTypeElement) != null;

	// The reference may include multiple values, separated by spaces
	String[] dataTypeLSIDs = dataTypeLSID.split(" ");
	String returnDataType = null;
	String returnNamespaces = null;

	for (String lsid : dataTypeLSIDs) {
	    // use just the first datatype since sadi services consume exactly
	    // one input class
	    returnDataType = lsid;
	    break;
	}
	if (returnDataType == null) {
	    // TODO -set to owl:Class?
	    returnDataType = isArray ? "Collection(Object)" : "Object";
	}
	return paramName + ":" + returnDataType
		+ (returnNamespaces == null ? "" : ":" + returnNamespaces);
    }

    private boolean isBasicType(String xsdType) {
	return "string".equals(xsdType) || "double".equals(xsdType) || "integer".equals(xsdType) || "boolean".equals(xsdType); // TODO: fill out list
    }

    /**
     * Goes from an operation, determines the output datatype (THERE SHOULD BE
     * JUST ONE), then sees if that datatype has a lifting schema that'll turn
     * the response into a sadi secondary param format.
     */
    protected URL getLiftingSchemaFromOperation(Element opElement, String opName)
	    throws Exception {
	QName[] ioMsgNames = getInputAndOutputMessageNamesFromOperation(opElement);
	// ioMsgNames[1] is the output message name
	String targetMessageName = ioMsgNames[1].getLocalPart();

	// Now find the message definition in the WSDL
	Element wsdlRoot = opElement.getOwnerDocument().getDocumentElement();
	NodeList messageElements = wsdlRoot.getElementsByTagNameNS(
		SadiPrefixResolver.WSDL_NAMESPACE, "message");
	Element partElement = null;
	for (int i = 0; i < messageElements.getLength(); i++) {
	    String messageName = ((Element) messageElements.item(i))
		    .getAttribute("name");

	    if (targetMessageName.equals(messageName)) {
		// Find the message part, and its datatype
		NodeList partElements = ((Element) messageElements.item(i))
			.getElementsByTagNameNS(
				SadiPrefixResolver.WSDL_NAMESPACE, "part");
		if (partElements.getLength() != 1) {
		    throw new Exception("The WSDL message (" + messageName
			    + ") containing the values "
			    + "for a Sadi secondary param (operation " + opName
			    + ") did not have one part "
			    + "as expected, but rather "
			    + partElements.getLength());
		}
		partElement = (Element) partElements.item(0);
		break;
	    }
	}
	if (partElement == null) {
	    throw new Exception("Could not find a message definition ("
		    + targetMessageName + ") for operation " + opName);
	}
	// The lifting may be defined at the "part" tag level, as it is for
	// services
	String schemaMapping = partElement.getAttributeNS(
		SadiPrefixResolver.SAWSDL_NAMESPACE, SAWSDL_OUTMAP_ATTR);
	if (schemaMapping != null && schemaMapping.length() != 0) {
	    return new URL(schemaMapping);
	}

	String partType = partElement.getAttribute("type");
	if (partType == null || partType.trim().length() == 0) {
	    // See if it's document/literal, in which case we need the element
	    // reference
	    String elementType = partElement.getAttribute("element");
	    if (elementType == null || elementType.trim().length() == 0) {
		throw new Exception(
			"The WSDL message ("
				+ targetMessageName
				+ ") containing the values "
				+ "for a Sadi secondary param (operation "
				+ opName
				+ ") has a part element "
				+ "as expected, but no defined data type ('type' or 'element' attribute "
				+ "missing, for rpc/encoded or doc/lit respectively)");
	    }
	    // Track back to the element xml schema definition for doc/lit: it
	    // should have the SAWSDL
	    partType = elementType;
	}
	// See if the name is namespace qualified, and separate the local part
	// if so
	String partTypeNamespaceURI = "";
	if (partType.contains(":")) {
	    String nsPrefix = partType.substring(0, partType.indexOf(":")); // XML
									    // NS
									    // prefix
	    partType = partType.substring(partType.indexOf(":") + 1); // local
								      // part
	    partTypeNamespaceURI = partElement.lookupNamespaceURI(nsPrefix); // prefix->URI
	}

	// Now find the definition of the part's data type, and see if it has a
	// schema lifting mapping
	// Usually, <xsd:schema> -> <xsd:complexType name="partType"/> for
	// rpc/encoded,
	// or <element name="partType"><xsd:complexType><sequence><element> for
	// doc/lit, in which case we need to dig further
	Element schemaDefElement = getSchemaElement(
		opElement.getOwnerDocument(), partTypeNamespaceURI, partType);
	schemaMapping = schemaDefElement.getAttributeNS(
		SadiPrefixResolver.SAWSDL_NAMESPACE, SAWSDL_OUTMAP_ATTR);
	if ((schemaMapping == null || schemaMapping.length() == 0)
		&& "element".equals(schemaDefElement.getNodeName())) {
	    NodeList subElements = schemaDefElement.getElementsByTagNameNS(
		    SadiPrefixResolver.XSD_NAMESPACE, "element");
	    if (subElements == null || subElements.getLength() == 0) {
		throw new Exception("The definition of XML Schema type "
			+ partType + " used as the output of " + opName
			+ " has no child elements, nor a SAWSDL "
			+ SAWSDL_OUTMAP_ATTR + " attribute");
	    }
	    schemaDefElement = (Element) subElements.item(0);
	}

	// If it's an XML Schema definition element with the same name as our
	// part type, we're good to go...
	// See if the SAWSDL lifting schema attribute is defined
	schemaMapping = schemaDefElement.getAttributeNS(
		SadiPrefixResolver.SAWSDL_NAMESPACE, SAWSDL_OUTMAP_ATTR);
	if (schemaMapping == null || schemaMapping.length() == 0) {
	    // As a last-ditch effort, if the data type is just an array of
	    // another datatype,
	    // look up the other data type to see if it has a lifting schema
	    // mapping, and
	    // we will take care of the array iteration part of the
	    // transformation
	    String arrayType = getArrayType(schemaDefElement);
	    if (arrayType == null) {
		throw new Exception("The definition of XML Schema type "
			+ partType + " used as the output of " + opName
			+ " has no SAWSDL " + SAWSDL_OUTMAP_ATTR
			+ " attribute defined, nor is"
			+ "it simply an array of a type with a lifting schema");
	    }

	    // We're at the array's data type. This is the last chance to find a
	    // schema lifting mapping
	    Element sde = getSchemaElement(opElement.getOwnerDocument(),
		    partTypeNamespaceURI, arrayType);
	    schemaMapping = sde.getAttributeNS(
		    SadiPrefixResolver.SAWSDL_NAMESPACE, SAWSDL_OUTMAP_ATTR);

	    if (schemaMapping != null && schemaMapping.length() != 0) {
		return new URL(schemaMapping + "#" + ARRAY_SENTINEL);
	    }
	    throw new Exception("Neither the array datatype (" + partType
		    + ") nor the datatype " + "it stores (" + arrayType
		    + ") has a SAWSDL " + SAWSDL_OUTMAP_ATTR + " attribute");
	} else {
	    return new URL(schemaMapping);
	}
    }

    /**
     * @return the data type of the array, or null if the data type is not an
     *         array
     */
    protected String getArrayType(Element schemaDefElement) {
	// The XML must look something like:
	// <xsd:complexType name="ArrayOfThing">
	// <xsd:complexContent>
	// <xsd:restriction base="soapenc:Array">
	// <xsd:attribute ref="soapenc:arrayType"
	// wsdl:arrayType="typens:Thing[]"/>
	// </xsd:restriction></xsd:complexContent></xsd:complexType>
	if (schemaDefElement == null) {
	    return null;
	}
	NodeList contentElements = schemaDefElement.getElementsByTagNameNS(
		SadiPrefixResolver.XSD_NAMESPACE, "complexContent");
	if (contentElements.getLength() == 1) {
	    NodeList restrictionElements = ((Element) contentElements.item(0))
		    .getElementsByTagNameNS(SadiPrefixResolver.XSD_NAMESPACE,
			    "restriction");
	    if (restrictionElements.getLength() == 1) {
		NodeList attributeElements = ((Element) restrictionElements
			.item(0)).getElementsByTagNameNS(
			SadiPrefixResolver.XSD_NAMESPACE, "attribute");
		if (attributeElements.getLength() == 1) {
		    // NOTE: WSDL 1.1 only for now
		    String arrayType = ((Element) attributeElements.item(0))
			    .getAttributeNS(SadiPrefixResolver.WSDL_NAMESPACE,
				    "arrayType");
		    if (arrayType.contains(":")) {
			arrayType = arrayType
				.substring(arrayType.indexOf(":") + 1); // local
									// part
		    }
		    if (arrayType.endsWith("[]")) {
			return arrayType.substring(0, arrayType.length() - 2); // remove
									       // the
									       // array
									       // square
									       // brackets
		    }
		}
	    }
	}
	return null;
    }

    /**
     * Returns the XML DOM element containing the definition of an XML Schema
     * data type, unless that datatype or namespace doesn't exist, in which case
     * an exception is thrown.
     */
    protected Element getSchemaElement(Document wsdlDoc, String nsUri,
	    String name) throws Exception {
	NodeList schemaElements = wsdlDoc.getDocumentElement()
		.getElementsByTagNameNS(SadiPrefixResolver.XSD_NAMESPACE,
			"schema");
	if (schemaElements.getLength() == 0) {
	    throw new Exception(
		    "Could not find XML Schema type definition for "
			    + name
			    + ", cannot find the schema section of the WSDL document");
	}

	for (int i = 0; i < schemaElements.getLength(); i++) {
	    if (!nsUri.equals(((Element) schemaElements.item(i))
		    .getAttribute("targetNamespace"))) {
		continue; // only look as schema definitions in the correct
			  // namespace
	    }

	    NodeList schemaDefElements = ((Element) schemaElements.item(i))
		    .getChildNodes();
	    for (int j = 0; j < schemaDefElements.getLength(); j++) {
		if (!(schemaDefElements.item(j) instanceof Element)) {
		    continue;
		}
		Element e = (Element) schemaDefElements.item(j);
		if ((SadiPrefixResolver.XSD_NAMESPACE.equals(e
			.getNamespaceURI()))
			&& name.equals(e.getAttribute("name"))) {
		    return e;
		}
	    }
	    throw new Exception(
		    "Could not find XML Schema type definition for " + name
			    + ", namespace " + nsUri
			    + " exists, but schema element " + name
			    + " does not");
	}
	throw new Exception("Could not find XML Schema type definition for "
		+ name + ", namespace " + nsUri + " does not exist");
    }

    public static Element renameSoapArrayElements(Element arrayElement,
	    String newName, Document owner) {
	NodeList arrayElements = arrayElement.getChildNodes();
	Element renamedArray = owner.createElement("soap-array");
	for (int j = 0; j < arrayElements.getLength(); j++) {
	    if (!(arrayElements.item(j) instanceof Element)) {
		continue;
	    }
	    // For XPath rules to work properly, we need to rename the items
	    // in the array to the parent name
	    Element renamedArrayItem = owner.createElement(newName);
	    // clone the old element attributes and children into the new one
	    Element oldElement = (Element) arrayElements.item(j);
	    NamedNodeMap attrs = oldElement.getAttributes();
	    for (int k = 0; k < attrs.getLength(); k++) {
		renamedArrayItem.setAttributeNodeNS((Attr) attrs.item(k)
			.cloneNode(true));
	    }
	    NodeList children = oldElement.getChildNodes();
	    for (int k = 0; k < children.getLength(); k++) {
		renamedArrayItem.appendChild(children.item(k).cloneNode(true));
	    }
	    renamedArray.appendChild(renamedArrayItem);
	}
	return renamedArray;
    }

    protected String convertSourceToSadiSecondaryValues(Source source,
	    URL liftingSchema, boolean arrayRule) throws Exception {
	// The source will contain an XML document, lets convert it to a string
	// of the form val1,val2,val3 using a stylesheet
	TransformerFactory transformerFactory = TransformerFactory
		.newInstance();
	Transformer transformer = null;
	// System.err.println("The lifting schema is " + liftingSchema);
	try {
	    // Create the transformer that'll turn the soap payload into a sadi
	    // secondary spec string
	    transformer = transformerFactory.newTransformer(new StreamSource(
		    liftingSchema.openStream()));
	} catch (TransformerConfigurationException tce) {
	    logger.log(Level.SEVERE, "Could not create an XSLT transformer: "
		    + tce, tce);
	}
	StringWriter stringWriter = new StringWriter();
	// Apply the rule to each member of the array, rather than just once to
	// the array
	if (arrayRule) {
	    // To simplify life (at the cost of inefficiency of processing),
	    // turn any source into a stream source (via a null XSLT transform)
	    // so we can parse it uniformily
	    if (!(source instanceof StreamSource)) {
		StringWriter verbatim = new StringWriter();
		transformerFactory.newTransformer().transform(source,
			new StreamResult(verbatim));
		// Need to compensate for the fact that some XSLT transformers
		// don't copy over the
		// namespace declaration properly, so declare it manually for
		// xsi if that's the case
		// otherwise you get a parsing error later on when trying to do
		// the
		// xml schema -> sadi xml transformation. Ditto with soap
		// encoding.
		String verb = verbatim.toString();
		if (verb.indexOf(" xmlns:xsi") == -1
			&& verb.indexOf(" xsi:") != -1) {
		    verb = verb.replaceFirst(" xsi:", " xmlns:xsi=\""
			    + SadiPrefixResolver.XSI_NAMESPACE2001 + "\" xsi:");
		}
		if (verb.indexOf(" xmlns:SOAP-ENC") == -1
			&& verb.indexOf(" SOAP-ENC:") != -1) {
		    verb = verb.replaceFirst(" SOAP-ENC:", " xmlns:SOAP-ENC=\""
			    + SadiPrefixResolver.SOAP_ENC_NAMESPACE
			    + "\" SOAP-ENC:");
		}
		source = new StreamSource(new StringReader(verb));
	    }

	    Document arrayDoc = null;
	    if (((StreamSource) source).getInputStream() != null) {
		arrayDoc = docBuilder.parse(((StreamSource) source)
			.getInputStream());
	    } else if (((StreamSource) source).getReader() != null) {
		arrayDoc = docBuilder.parse(new org.xml.sax.InputSource(
			((StreamSource) source).getReader()));
	    } else {
		throw new Exception(
			"Neither an InputStream nor a Reader was available from "
				+ "the StreamSource, cannot process the source");
	    }
	    Element arrayElement = (Element) arrayDoc.getDocumentElement()
		    .getFirstChild();
	    String dataTypeAttr = arrayElement.getAttributeNS(
		    SadiPrefixResolver.SOAP_ENC_NAMESPACE, "arrayType");
	    String dataType = dataTypeAttr
		    .replaceFirst("^.*:([^\\[]+).*", "$1");
	    // System.err.println("Array Data Type for secondary source is " +
	    // dataType);
	    // for RPC-encoded data, rename item tags to real data type name
	    Element renamedArray = renameSoapArrayElements(arrayElement,
		    dataType, arrayDoc);
	    NodeList arrayElements = renamedArray
		    .getElementsByTagName(dataType);
	    for (int i = 0; i < arrayElements.getLength(); i++) {
		if (i != 0) {
		    stringWriter.write(','); // join values with a comma
		}
		transformer.transform(new DOMSource(arrayElements.item(i)),
			new StreamResult(stringWriter));
	    }
	}
	// Not an array rule, just apply the stylesheet as-is
	else {
	    transformer.transform(source, new StreamResult(stringWriter));
	}
	// System.err.println("Response payload for secondary param:\n"+stringWriter.toString());

	return stringWriter.toString();
    }

    public static String join(Iterable i, String delimiter) {
	StringBuffer buffer = new StringBuffer();
	for (Object item : i) {
	    buffer.append(item.toString());
	    buffer.append(delimiter);
	}
	return buffer.substring(0, buffer.length() - delimiter.length());
    }

    // Finds out the soap operations to wrap and the associated Sadi metadata
    // such as service name, authority, etc.
    public void parseSadiServiceSpecs() throws Exception {
	NodeList serviceNameAttrs = (NodeList) xPath.evaluate(
		SERVICENAME_ATTR_XPATH, wsdlDoc, XPathConstants.NODESET);
	if (serviceNameAttrs == null || serviceNameAttrs.getLength() == 0) {
	    throw new Exception(
		    "There do not appear to be any Sadi serviceName attributes in the WSDL file.  "
			    + "Either this is not a Sadi-oriented SAWSDL file, or the Sadi namespace is not properly defined.");
	}
	for (int i = 0; i < serviceNameAttrs.getLength(); i++) {
	    Element el = (Element) serviceNameAttrs.item(i);
	    String serviceName = el.getAttributeNS(
		    SadiPrefixResolver.SADI_XML_NAMESPACE, SERVICE_NAME_ATTR);
	    if (serviceName == null || serviceName.trim().length() == 0) {
		throw new Exception("The Sadi service attribute "
			+ SERVICE_NAME_ATTR
			+ " is missing or blank for service " + serviceName);
	    }
	    if (!serviceName.trim().matches("[A-Za-z0-9_]+")) {
		throw new Exception("The Sadi service name attribute ("
			+ serviceName
			+ ") contains non-alphanumeric/underscore characters, "
			+ "which are illegal in Sadi names");
	    }

	    // We expect the authority, contact and description in the same tag
	    // Element el = serviceAttr.getOwnerElement();
	    String serviceContact = el
		    .getAttributeNS(SadiPrefixResolver.SADI_XML_NAMESPACE,
			    SERVICE_CONTACT_ATTR);
	    if (serviceContact == null || serviceContact.trim().length() == 0) {
		throw new Exception("The Sadi service attribute "
			+ SERVICE_CONTACT_ATTR
			+ " is missing or blank for service " + serviceName);
	    }
	    String serviceAuthority = el.getAttributeNS(
		    SadiPrefixResolver.SADI_XML_NAMESPACE, SERVICE_AUTH_ATTR);
	    if (serviceAuthority == null
		    || serviceAuthority.trim().length() == 0) {
		throw new Exception("The Sadi service attribute "
			+ SERVICE_AUTH_ATTR
			+ " is missing or blank for service " + serviceName);
	    }
	    String serviceDescription = el.getAttributeNS(
		    SadiPrefixResolver.SADI_XML_NAMESPACE, SERVICE_DESC_ATTR);
	    if (serviceDescription == null
		    || serviceDescription.trim().length() == 0) {
		throw new Exception("The Sadi service attribute "
			+ SERVICE_DESC_ATTR
			+ " is missing or blank for service " + serviceName);
	    }
	    String serviceCategory = el.getAttributeNS(
		    SadiPrefixResolver.SAWSDL_NAMESPACE, SAWSDL_MODEL_ATTR);

	    // The user can optionally pick the registry where the service will
	    // be published
	    // Later, the servlet code will check if the ontology terms used are
	    // valid within that registry
	    String registryEndpoint = el.getAttributeNS(
		    SadiPrefixResolver.SADI_XML_NAMESPACE, REGISTRY_ATTR);

	    // Set the vars for retrieval by the servlet, cleaning up errant
	    // whitespace where possible
	    currentService = serviceName.trim();
	    setContactEmail(serviceContact);
	    setCentralEndpoint(registryEndpoint == null ? null
		    : registryEndpoint.trim());
	    setServiceType(serviceCategory.trim());
	    setProviderURI(serviceAuthority.trim());
	    setServiceDesc(serviceDescription.trim());

	    parseSOAPServiceSpecs(el);

	} // end for Sadi service name attrs
    }

    // el is the tag containing the sadi service attributes. It lives in a WSDL
    // operation hopefully...
    private void parseSOAPServiceSpecs(Element el) throws Exception {
	// Find the messages associated with the operation we're marking up:
	// they will be checked later for semantic attrs.
	// In WSDL 2.0, the operation tag would be the current tag. In WSDL 1.1,
	// it'd be a parent
	// so we ascend the DOM until we find an "operation" element.
	Element opElement = el;
	for (; opElement != null; opElement = (Element) opElement
		.getParentNode()) {
	    if ("operation".equals(opElement.getLocalName())
		    && (SadiPrefixResolver.WSDL_NAMESPACE.equals(opElement
			    .getNamespaceURI()) || SadiPrefixResolver.WSDL20_NAMESPACE
			    .equals(opElement.getNamespaceURI()))) {
		break;
	    }
	}
	if (opElement == null) {
	    throw new Exception(
		    "Could not find a WSDL 'operation' element enclosing the definition "
			    + "of Sadi service " + currentService);
	}

	String soapOpName = opElement.getAttribute("name");
	if (soapOpName == null || soapOpName.trim().length() == 0) {
	    throw new Exception(
		    "The name of the WSDL 'operation' element enclosing the definition "
			    + "of Sadi service " + currentService
			    + " is missing or blank");
	}
	soapOpName = soapOpName.trim();

	String soapAction = null;
	NodeList soapOps = opElement.getElementsByTagNameNS(
		"http://schemas.xmlsoap.org/wsdl/soap/", "operation");
	for (int n = 0; n < soapOps.getLength(); n++) {
	    Element soapOp = (Element) soapOps.item(n);
	    soapAction = soapOp.getAttribute("soapAction");
	    if (soapAction != null && soapAction.trim().length() > 0) {
		break;
	    }
	}
	if (soapAction == null) {
	    soapAction = parseSoapAction(soapOpName) ;
	}

	QName[] names = getServiceAndPortFromOperation(opElement, soapOpName);
	QName[] ioMsgNames = getInputAndOutputMessageNamesFromOperation(opElement);

	String style = names[2].getNamespaceURI(); // we abused the QName
						   // structure to pass back op
						   // style/encoding info
	String encoding = names[2].getLocalPart(); // we abused the QName
						   // structure to pass back op
						   // style/encoding info

	setServiceQName(names[0]);
	setPortQName(names[1]);
	setSoapAddressLocation(parseSoapAddressLocation(names[1]));
	setSoapAction(soapAction);
	setOperationStyle(style);
	setOperationEncoding(encoding);
	setOperationName(soapOpName);
	setOperationInputQName(ioMsgNames[0]);
	setOperationOutputQName(ioMsgNames[1]);
    }

    /**
     * @return a two element array, with input message name, then output message
     *         name
     */
    protected QName[] getInputAndOutputMessageNamesFromOperation(
	    Element opElement) throws Exception {
	// Now that we have the operation, find the input and output children we
	// need to note
	NodeList inputElements = opElement.getElementsByTagNameNS(
		SadiPrefixResolver.WSDL_NAMESPACE, "input");
	if (inputElements == null || inputElements.getLength() == 0) {
	    inputElements = opElement.getElementsByTagNameNS(
		    SadiPrefixResolver.WSDL20_NAMESPACE, "input");
	}
	if (inputElements == null || inputElements.getLength() == 0) {
	    throw new Exception(
		    "Could not find the WSDL input element descendant of the operation "
			    + "element defining the Sadi service "
			    + currentService);
	}
	if (inputElements.getLength() > 1) {
	    throw new Exception(
		    "More than one WSDL input element is defined for the operation "
			    + "element defining the Sadi service "
			    + currentService);
	}

	NodeList outputElements = opElement.getElementsByTagNameNS(
		SadiPrefixResolver.WSDL_NAMESPACE, "output");
	if (outputElements == null || outputElements.getLength() == 0) {
	    outputElements = opElement.getElementsByTagNameNS(
		    SadiPrefixResolver.WSDL20_NAMESPACE, "output");
	}
	if (outputElements == null || outputElements.getLength() == 0) {
	    throw new Exception(
		    "Could not find the WSDL output element descendant of the operation "
			    + "element defining the Sadi service "
			    + currentService);
	}
	if (outputElements.getLength() > 1) {
	    throw new Exception(
		    "More than one WSDL output element is defined for the operation "
			    + "element defining the Sadi service "
			    + currentService);
	}

	// Should have element or message attr for WSDL 2.0 and 1.1 respectively
	String inputName = ((Element) inputElements.item(0))
		.getAttribute("message");
	if (inputName == null || inputName.trim().length() == 0) {
	    inputName = ((Element) inputElements.item(0))
		    .getAttribute("element");
	}
	if (inputName == null || inputName.trim().length() == 0) {
	    throw new Exception(
		    "Could not find the message or element attribute associated with "
			    + "the WSDL input element for the Sadi service "
			    + currentService);
	}
	String nsURI = null;
	if (inputName.contains(":")) { // convert ns:label to QName object
	    String nsPrefix = inputName.substring(0, inputName.indexOf(":")); // XML
									      // NS
									      // prefix
	    inputName = inputName.substring(inputName.indexOf(":") + 1); // local
									 // part
	    nsURI = inputElements.item(0).lookupNamespaceURI(nsPrefix); // prefix->URI
	}
	QName inputQName = new QName(nsURI, inputName);

	// Should have element or message attr for WSDL 2.0 and 1.1 respectively
	String outputName = ((Element) outputElements.item(0))
		.getAttribute("message");
	if (outputName == null || outputName.trim().length() == 0) {
	    outputName = ((Element) outputElements.item(0))
		    .getAttribute("element");
	}
	if (outputName == null || outputName.trim().length() == 0) {
	    throw new Exception(
		    "Could not find the message or element attribute associated with "
			    + "the WSDL output element for the Sadi service "
			    + currentService);
	}
	if (outputName.contains(":")) { // convert ns:label to QName object
	    String nsPrefix = outputName.substring(0, outputName.indexOf(":")); // XML
										// NS
										// prefix
	    outputName = outputName.substring(outputName.indexOf(":") + 1); // local
									    // part
	    nsURI = outputElements.item(0).lookupNamespaceURI(nsPrefix); // prefix->URI
	}
	QName outputQName = new QName(nsURI, outputName);

	return new QName[] { inputQName, outputQName };
    }

    /**
     * @return a two element array, with service, then port, then style
     */
    protected QName[] getServiceAndPortFromOperation(Element opElement,
	    String soapOpName) throws Exception {

	// Now go up the DOM to find the port type (WSDL 1.1) or interface (WSDL
	// 2.0) enclosing the operation
	Element porttypeInterfaceElement = (Element) opElement.getParentNode();
	for (; porttypeInterfaceElement != null; porttypeInterfaceElement = porttypeInterfaceElement
		.getParentNode() instanceof Element ? (Element) porttypeInterfaceElement
		.getParentNode() : null) {
	    if ("portType".equals(porttypeInterfaceElement.getLocalName())
		    && SadiPrefixResolver.WSDL_NAMESPACE.equals(opElement
			    .getNamespaceURI())
		    || "interface".equals(porttypeInterfaceElement
			    .getLocalName())
		    && SadiPrefixResolver.WSDL20_NAMESPACE.equals(opElement
			    .getNamespaceURI())) {
		break;
	    }
	}
	if (porttypeInterfaceElement == null) {
	    throw new Exception(
		    "Could not find a WSDL 1.1 'portType' or WSDL 2.0 "
			    + "'interface' element enclosing the " + soapOpName
			    + " operation");
	}
	String portTypeName = porttypeInterfaceElement.getAttribute("name");
	if (portTypeName == null || portTypeName.length() == 0) {
	    throw new Exception("The port/interface element enclosing the "
		    + soapOpName
		    + " operation in the WSDL file does not have a name");
	}

	// I think the WSDL 2.0 model is very different from this point
	// on...must investigate!

	// Find the binding that use the portType
	NodeList bindingElements = wsdlDoc.getDocumentElement()
		.getElementsByTagNameNS(SadiPrefixResolver.WSDL_NAMESPACE,
			"binding");
	if (bindingElements == null || bindingElements.getLength() == 0) {
	    throw new Exception(
		    "Could not find any WSDL binding elements in the WSDL");
	}
	String bindingName = null;
	String style = null;
	String encoding = null;
	for (int i = 0; i < bindingElements.getLength(); i++) {
	    Element bindingElement = (Element) bindingElements.item(i);
	    String bindingPortType = bindingElement.getAttribute("type");
	    if (portTypeName.equals(bindingPortType)
		    || (bindingPortType != null && bindingPortType.endsWith(":"
			    + portTypeName))) {
		// name matches, or maybe name with a prefix:
		bindingName = bindingElement.getAttribute("name");
		if (bindingName == null || bindingName.length() == 0) {
		    throw new Exception(
			    "The WSDL binding element that uses the "
				    + portTypeName
				    + " port type does not have a name attribute.");
		}

		// find the style of the soap op (document or rpc)
		NodeList opElements = bindingElement.getElementsByTagNameNS(
			"http://schemas.xmlsoap.org/wsdl/soap/", "binding");
		for (int j = 0; j < opElements.getLength(); j++) {
		    Element op = (Element) opElements.item(j);
		    style = op.getAttribute("style");
		    if (style != null && style.length() > 0) {
			break;
		    }
		}

		// find the encoding type (encoded or literal) for the soap op
		opElements = bindingElement.getElementsByTagNameNS(
			SadiPrefixResolver.WSDL_NAMESPACE, "operation");
		for (int j = 0; j < opElements.getLength(); j++) {
		    Element op = (Element) opElements.item(j);
		    String opName = op.getAttribute("name");
		    if (soapOpName.equals(opName)) {
			NodeList inputs = op.getElementsByTagNameNS(
				SadiPrefixResolver.WSDL_NAMESPACE, "input");
			String inputMsgName = null;
			QName inputMsgQName = null;
			for (int n = 0; n < inputs.getLength(); n++) {
			    Element input = (Element) inputs.item(n);
			    inputMsgName = input.getAttribute("name");

			    NodeList soapInputs = input.getElementsByTagNameNS(
				    "http://schemas.xmlsoap.org/wsdl/soap/",
				    "body");
			    if (soapInputs == null
				    || soapInputs.getLength() == 0) {
				throw new Exception(
					"Could not find a SOAP body definition for operation "
						+ opName + "\n");
			    }
			    Element bodyDef = (Element) soapInputs.item(0);
			    encoding = bodyDef.getAttribute("use");
			    if (encoding != null && encoding.length() != 0) {
				break;
			    }
			}
			break;
		    }
		}
	    }
	}
	if (bindingName == null) {
	    throw new Exception(
		    "Could not find the WSDL binding element that uses the "
			    + soapOpName + " operation's " + "port type ("
			    + portTypeName + ")");
	}

	// Find the port that uses the binding
	NodeList portElements = wsdlDoc.getDocumentElement()
		.getElementsByTagNameNS(SadiPrefixResolver.WSDL_NAMESPACE,
			"port");
	if (portElements == null || portElements.getLength() == 0) {
	    throw new Exception(
		    "Could not find any WSDL port elements in the WSDL");
	}
	String portName = null;
	Element portElement = null;
	for (int i = 0; i < portElements.getLength(); i++) {
	    portElement = (Element) portElements.item(i);
	    String bName = portElement.getAttribute("binding");
	    if (bindingName.equals(bName)
		    || (bName != null && bName.endsWith(":" + bindingName))) {
		// name matches, or maybe name with a prefix:
		portName = portElement.getAttribute("name");
		if (portName == null || portName.length() == 0) {
		    throw new Exception("The WSDL port element that uses the "
			    + bindingName
			    + " binding does not have a name attribute.");
		}
		break;
	    }
	}
	if (portName == null) {
	    throw new Exception(
		    "Could not find the WSDL port element that uses the "
			    + soapOpName + " operation's " + "binding ("
			    + bindingName + ")");
	}

	// Find the service that uses the port, it must be a parent element.
	Element serviceElement = (Element) portElement.getParentNode();
	for (; serviceElement != null; serviceElement = (Element) serviceElement
		.getParentNode()) {
	    if ("service".equals(serviceElement.getLocalName())
		    && (SadiPrefixResolver.WSDL_NAMESPACE.equals(opElement
			    .getNamespaceURI()) || SadiPrefixResolver.WSDL20_NAMESPACE
			    .equals(opElement.getNamespaceURI()))) {
		break;
	    }
	}
	if (serviceElement == null) {
	    throw new Exception(
		    "Could not find a WSDL 'service' element enclosing the port "
			    + portName);
	}
	String serviceName = serviceElement.getAttribute("name");
	if (serviceName == null || serviceName.length() == 0) {
	    throw new Exception("The WSDL service element enclosing the port "
		    + portName + " does not have a name attribute");
	}

	// find the target namespace for the service and port, as JAX-WS will
	// need these later
	String targetNamespace = wsdlDoc.getDocumentElement().getAttributeNS(
		SadiPrefixResolver.WSDL_NAMESPACE, "targetNamespace");
	if (targetNamespace == null || targetNamespace.length() == 0) {
	    targetNamespace = wsdlDoc.getDocumentElement().getAttribute(
		    "targetNamespace");
	}
	if (targetNamespace == null || targetNamespace.length() == 0) {
	    throw new Exception(
		    "No targetNamespace attribute was found in the root element of the WSDL document");
	}

	QName[] specs = new QName[3];
	specs[0] = new QName(targetNamespace, serviceName);
	specs[1] = new QName(targetNamespace, portName);
	// not really a QName, but pass it back as such for simplicity (will be
	// either
	// "document" or "rpc", and "literal" or "encoded")
	specs[2] = new QName(style, encoding);
	return specs;
    }

    public String getOperationName() {
	return sadiServiceName2Op.get(currentService);
    }

    public void setOperationName(String opName) {
	sadiServiceName2Op.put(currentService, opName);
    }

    public QName getPortQName() {
	return sadiServiceName2Port.get(currentService);
    }

    public void setPortQName(QName portName) {
	sadiServiceName2Port.put(currentService, portName);
    }

    public QName getServiceQName() {
	return sadiServiceName2Service.get(currentService);
    }

    public void setServiceQName(QName serviceName) {
	sadiServiceName2Service.put(currentService, serviceName);
    }

    public QName getOperationInputQName() {
	return sadiServiceName2OpInput.get(currentService);
    }

    public void setOperationInputQName(QName inputName) {
	sadiServiceName2OpInput.put(currentService, inputName);
    }

    /**
     * If style is not explicitly available, literal is the default
     */
    public String getOperationEncoding() {
	String encoding = sadiServiceName2Encoding.get(currentService);
	if (encoding == null) {
	    return "literal";
	}
	return encoding;
    }

    public void setOperationEncoding(String encoding) {
	sadiServiceName2Encoding.put(currentService, encoding);
    }

    public String getOperationStyle() {
	String style = sadiServiceName2Style.get(currentService);
	if (style == null) {
	    return "document";
	}
	return style;
    }

    public void setOperationStyle(String style) {
	sadiServiceName2Style.put(currentService, style);
    }

    public String getSoapAction() {
	return sadiServiceName2SoapAction.get(currentService);
    }

    public void setSoapAction(String soapAction) {
	sadiServiceName2SoapAction.put(currentService, soapAction);
    }
    
    public URL getSoapAddressLocation() {
	return sadiServiceName2SoapAddressLocation.get(currentService);
    }

    public void setSoapAddressLocation(URL soapAddress) {
	sadiServiceName2SoapAddressLocation.put(currentService, soapAddress);
    }

    public QName getOperationOutputQName() {
	return sadiServiceName2OpOutput.get(currentService);
    }

    public void setOperationOutputQName(QName outputName) {
	sadiServiceName2OpOutput.put(currentService, outputName);
    }

    public Map<String, String> getInputXSDTypes() {
	return sadiServiceName2InputXSDTypes.get(currentService);
    }

    public void setInputXSDTypes(Map<String, String> types) {
	sadiServiceName2InputXSDTypes.put(currentService, types);
    }

    public Map<String, String> getOutputXSDTypes() {
	return sadiServiceName2OutputXSDTypes.get(currentService);
    }

    public void setOutputXSDTypes(Map<String, String> types) {
	sadiServiceName2OutputXSDTypes.put(currentService, types);
    }

    public String getTargetNamespaceURI() {
	return sadiServiceName2TargetNamespaceURI.get(currentService);
    }

    public void setTargetNamespaceURI(String nsURI) {
	sadiServiceName2TargetNamespaceURI.put(currentService, nsURI);
    }

    /**
     * A mutable NodeList implementation for convenience.
     */
    public static class MyNodeList implements NodeList {
	private Vector<Node> nodes;

	public MyNodeList() {
	    nodes = new Vector<Node>();
	}

	public int getLength() {
	    return nodes.size();
	}

	public Node item(int index) {
	    return nodes.elementAt(index);
	}

	public void add(Node n) {
	    nodes.add(n);
	}

	public void add(NodeList n) {
	    for (int i = 0; i < n.getLength(); i++) {
		nodes.add(n.item(i));
	    }
	}
    };

    /**
     * Expand any import or include statements in-place.
     */
    public static void doImports(Document doc, URL baseURL) throws Exception {
	Element docElement = doc.getDocumentElement();
	NodeList schemaNodes = docElement.getElementsByTagNameNS(
		"http://www.w3.org/2001/XMLSchema", "schema");
	if ("schema".equals(docElement.getLocalName())
		&& "http://www.w3.org/2001/XMLSchema".equals(docElement
			.getNamespaceURI())) {
	    schemaNodes = new MyNodeList();
	    ((MyNodeList) schemaNodes).add(docElement);
	}
	// System.err.println("Found " + schemaNodes.getLength() +
	// " schema nodes on import pass of "+baseURL);
	boolean DEEP = true;
	for (int i = 0; i < schemaNodes.getLength(); i++) {
	    Element schemaElement = (Element) schemaNodes.item(i);
	    NodeList importNodes = schemaElement.getElementsByTagNameNS(
		    "http://www.w3.org/2001/XMLSchema", "import");
	    if (importNodes.getLength() == 0) {
		continue;
	    }
	    // System.err.println("Found " + importNodes.getLength() +
	    // " import nodes in "+baseURL);

	    Element importElement = (Element) importNodes.item(0); // assuming
								   // one import
								   // max per
								   // schema
								   // element
	    String schemaLocation = importElement
		    .getAttribute("schemaLocation");
	    if (schemaLocation == null || schemaLocation.length() == 0) {
		continue; // blank is actually allowed, but is not informative
			  // for us
	    }
	    URL importURL = new URL(baseURL, schemaLocation);
	    Document importDoc = docBuilder.parse(importURL.openStream());
	    // recurse as imports may have imports, etc.
	    doImports(importDoc, importURL);
	    // maybe we should have checked for circular references somehow?

	    Element newSchemaElement = importDoc.getDocumentElement();
	    if (!"schema".equals(newSchemaElement.getLocalName())
		    || !"http://www.w3.org/2001/XMLSchema"
			    .equals(newSchemaElement.getNamespaceURI())) {
		throw new Exception(
			"Don't know how to import an XML Schema file "
				+ "without a root 'schema' tag in the XML schema namespace..."
				+ " (instead found '"
				+ newSchemaElement.getLocalName()
				+ "' in namespace "
				+ newSchemaElement.getNamespaceURI() + ")");
	    }

	    // todo: should copy namespace from import statement to replaced
	    // subtree
	    Node dupe = doc.importNode(newSchemaElement, DEEP);
	    schemaElement.getParentNode().replaceChild(dupe, schemaElement);
	}

	// refresh, in case an import had an include, etc.
	schemaNodes = docElement.getElementsByTagNameNS(
		"http://www.w3.org/2001/XMLSchema", "schema");
	if ("schema".equals(docElement.getLocalName())
		&& "http://www.w3.org/2001/XMLSchema".equals(docElement
			.getNamespaceURI())) {
	    schemaNodes = new MyNodeList();
	    ((MyNodeList) schemaNodes).add(docElement);
	}
	// System.err.println("Found " + schemaNodes.getLength() +
	// " schema nodes on include pass of "+baseURL);
	for (int i = 0; i < schemaNodes.getLength(); i++) {
	    NodeList inclNodes = ((Element) schemaNodes.item(i))
		    .getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema",
			    "include");
	    // System.err.println("Found " + inclNodes.getLength() +
	    // " include nodes on pass of "+baseURL);
	    if (inclNodes.getLength() == 0) {
		continue;
	    }

	    for (int j = 0; j < inclNodes.getLength(); j++) {
		Element inclNode = (Element) inclNodes.item(j);
		String schemaLocation = inclNode.getAttribute("schemaLocation");
		if (schemaLocation == null || schemaLocation.length() == 0) {
		    continue; // blank may allowed, dunno, but is not
			      // informative for us anyway
		}
		URL inclURL = new URL(baseURL, schemaLocation);
		Document inclDoc = docBuilder.parse(inclURL.openStream());
		// recurse as imports may have imports, etc.
		doImports(inclDoc, inclURL);
		// maybe we should have checked for circular references somehow?

		// remove the "include" statement, replace it with the schema
		// bits referenced
		Node inclParentNode = inclNode.getParentNode();
		inclParentNode.removeChild(inclNode);
		Element inclRoot = inclDoc.getDocumentElement();
		NodeList newData = inclRoot.getElementsByTagNameNS(
			"http://www.w3.org/2001/XMLSchema", "schema");
		if ("schema".equals(inclRoot.getLocalName())
			&& "http://www.w3.org/2001/XMLSchema".equals(inclRoot
				.getNamespaceURI())) {
		    newData = new MyNodeList();
		    ((MyNodeList) newData).add(inclRoot);
		}
		for (int k = 0; k < newData.getLength(); k++) { // should be
								// only one, but
								// we'll do this
								// anyway...
		    NodeList schemaParts = ((Element) newData.item(k))
			    .getChildNodes();
		    for (int l = 0; l < schemaParts.getLength(); l++) {
			Node dupe = doc.importNode(schemaParts.item(l), DEEP);
			inclParentNode.appendChild(dupe);
		    }
		}
	    }
	}
    }
}
