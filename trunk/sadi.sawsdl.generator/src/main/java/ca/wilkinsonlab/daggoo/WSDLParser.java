package ca.wilkinsonlab.daggoo;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.ws.Service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.wilkinsonlab.daggoo.WrappingServlet.MyNodeList;
import ca.wilkinsonlab.daggoo.utils.WSDLConfig;



public class WSDLParser {
        
    private ArrayList<SoapDatatypeMapping> inputSoapDatatypes = new ArrayList<SoapDatatypeMapping>();
    private ArrayList<SoapDatatypeMapping> outputSoapDatatypes = new ArrayList<SoapDatatypeMapping>();

    private static final long serialVersionUID = -6258791849258643444L;

    private static final String ARRAY_TYPE_SENTINEL = "ar_TyPe";

    private static final String BASIC_TYPE_SENTINEL = "baSIc_TyPe";

    private static final String BASIC_NAME_SENTINEL = "baSIc_naMe";

    public static final String DOC_PARAM_SENTINEL = "_is_doc_style";

    private static final String DEFERRED_NAMESPACE_URI = "http://my.deferred.sentinel.for.schema.references/";

    private static DocumentBuilder docBuilder;

    private static Logger logger = Logger
	    .getLogger(WSDLParser.class.getName());

    private Document wsdlDoc;
    private Document sawsdlDoc;
    
    private URL url;
    
    public WSDLParser(URL url) {
	this.url = url;
    }
    
    static {
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	dbf.setNamespaceAware(true);
	try {
	    docBuilder = dbf.newDocumentBuilder();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void addServiceAttributesToSAWSDL(
	    String baseURL, String serviceName, String serviceAuthority, 
	    String serviceType, String contactEmail, String description) {
	
	// find our modelReference attributes and liftingSchemaMapping and loweringSchemaMapping ... update the url
	NodeList list = sawsdlDoc.getElementsByTagName("*");
	int updateCount = 0;
	for (int x = 0; x < list.getLength(); x++) {
	    if (list.item(x) instanceof Element) {
		Element e = (Element) list.item(x);
		if (e.hasAttributeNS(SadiPrefixResolver.SAWSDL_NAMESPACE, WSDLConfig.SAWSDL_MODEL_ATTR)) {
		    if (e.hasAttributeNS(SadiPrefixResolver.SAWSDL_NAMESPACE, WSDLConfig.SAWSDL_INMAP_ATTR)) {
			e.removeAttributeNS(SadiPrefixResolver.SAWSDL_NAMESPACE, WSDLConfig.SAWSDL_INMAP_ATTR);
			e.removeAttributeNS(SadiPrefixResolver.SAWSDL_NAMESPACE, WSDLConfig.SAWSDL_MODEL_ATTR);
			// input model reference
			e.setAttributeNS(SadiPrefixResolver.SAWSDL_NAMESPACE, SadiPrefixResolver.SAWSDL_PREFIX + ":" + WSDLConfig.SAWSDL_MODEL_ATTR, baseURL+"owl#inputClass");
			e.setAttributeNS(SadiPrefixResolver.SAWSDL_NAMESPACE, SadiPrefixResolver.SAWSDL_PREFIX + ":" + WSDLConfig.SAWSDL_INMAP_ATTR, baseURL+"lowering");
			updateCount++;
		    } else if (e.hasAttributeNS(SadiPrefixResolver.SAWSDL_NAMESPACE, WSDLConfig.SAWSDL_OUTMAP_ATTR)) {
			e.removeAttributeNS(SadiPrefixResolver.SAWSDL_NAMESPACE, WSDLConfig.SAWSDL_OUTMAP_ATTR);
			e.removeAttributeNS(SadiPrefixResolver.SAWSDL_NAMESPACE, WSDLConfig.SAWSDL_MODEL_ATTR);
			// output model reference
			e.setAttributeNS(SadiPrefixResolver.SAWSDL_NAMESPACE, SadiPrefixResolver.SAWSDL_PREFIX + ":" + WSDLConfig.SAWSDL_MODEL_ATTR, baseURL+"owl#outputClass");
			e.setAttributeNS(SadiPrefixResolver.SAWSDL_NAMESPACE, SadiPrefixResolver.SAWSDL_PREFIX + ":" + WSDLConfig.SAWSDL_OUTMAP_ATTR, baseURL+"lifting");
			updateCount++;
		    }
		    if (updateCount >= 2)
			break;
		}
	    }
	}
	
	// create a new element
	Element e = sawsdlDoc.createElementNS(SadiPrefixResolver.SAWSDL_NAMESPACE, "attrExtensions");
	e.setAttributeNS(
		SadiPrefixResolver.SAWSDL_NAMESPACE,
		SadiPrefixResolver.SAWSDL_PREFIX + ":" + WSDLConfig.SAWSDL_MODEL_ATTR,
		baseURL != null ? (baseURL.endsWith("/") ? baseURL.substring(0, baseURL.length() - 1) : baseURL) : serviceName); 
//	e.setAttributeNS(SadiPrefixResolver.SADI_XML_NAMESPACE, SadiPrefixResolver.SADI_XML_PREFIX + ":" + WSDLConfig.SERVICE_NAME_ATTR, serviceName);
//	e.setAttributeNS(SadiPrefixResolver.SADI_XML_NAMESPACE, SadiPrefixResolver.SADI_XML_PREFIX + ":" + WSDLConfig.SERVICE_AUTH_ATTR,serviceAuthority);
//	e.setAttributeNS(SadiPrefixResolver.SADI_XML_NAMESPACE, SadiPrefixResolver.SADI_XML_PREFIX + ":" + WSDLConfig.SERVICE_CONTACT_ATTR, contactEmail);
//	e.setAttributeNS(SadiPrefixResolver.SADI_XML_NAMESPACE, SadiPrefixResolver.SADI_XML_PREFIX + ":" + WSDLConfig.SERVICE_DESC_ATTR, description);
//	e.setAttributeNS(SadiPrefixResolver.SADI_XML_NAMESPACE, SadiPrefixResolver.SADI_XML_PREFIX + ":" + WSDLConfig.REGISTRY_ATTR, SAWSDLService.REGISTRY_ENDPOINT);
	
	list = sawsdlDoc.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/","portType");
	// list should technically only have 1 item in it ...
	if (list.getLength() > 0) {
	    if (list.item(0).getFirstChild() != null)
		list.item(0).getFirstChild().appendChild(e);
	}	
    }
    
    
    public void processWSDL(String name) {

	// reset our input/outputs
	inputSoapDatatypes = new ArrayList<SoapDatatypeMapping>();
	outputSoapDatatypes = new ArrayList<SoapDatatypeMapping>();
	
	try {
	    
	    // Use JAX-WS to get the service and port list
	    // Verify if the service info was actually parsed properly by trying
	    // to use it with JAX-WS
	    if (wsdlDoc == null) {
		wsdlDoc = docBuilder.parse(url.openStream());
		doImports(wsdlDoc, url);
	    }
	    // new sawsdldoc each time we process WSDL ...
	    sawsdlDoc = docBuilder.newDocument();
	    
	    
	    // Before we do anything else, let's inplace edit the DOM for any
	    // import or include statements
	    // suck in the xmlns declarations etc, for the definitions Element ...
	    sawsdlDoc.appendChild(sawsdlDoc.adoptNode(wsdlDoc.getDocumentElement().cloneNode(false)));
	    // append the sadi namespace and the sawsdl namespaces to the document element ...
	    sawsdlDoc.getDocumentElement().setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,"xmlns:"+SadiPrefixResolver.SAWSDL_PREFIX,SadiPrefixResolver.SAWSDL_NAMESPACE);
	    sawsdlDoc.getDocumentElement().setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,"xmlns:"+SadiPrefixResolver.SADI_XML_PREFIX,SadiPrefixResolver.SADI_XML_NAMESPACE);
	    
	    NodeList serviceElements = wsdlDoc.getDocumentElement()
		    .getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/",
			    "service");

	    if (serviceElements == null || serviceElements.getLength() == 0) {
		// TODO throw exception "Could not find any service elements in the WSDL namespace ("+ "http://schemas.xmlsoap.org/wsdl/) in the given WSDL file ("+ url + ")");
		return;
	    }

	    int anonCount = 0; // used to generate unique names for anonymously
			       // declared parts of the WSDL def
	    for (int i = 0; i < serviceElements.getLength(); i++) {
		Element serviceElement = (Element) serviceElements.item(i);
		
		String serviceName = serviceElement.getAttribute("name");
		if (serviceName == null || serviceName.trim().length() == 0) {
		 // TODO throw exception "A service element in the WSDL file ("+ url+ ") did not have a 'name' attribute.");
		    return;
		}
		
		// append the service node, and its children, to the sawsdl
		sawsdlDoc.getDocumentElement().appendChild(sawsdlDoc.adoptNode(serviceElement.cloneNode(true)));

		String serviceNamespaceURI = null;
		for (Node element = serviceElement; element != null
			&& element instanceof Element; element = serviceElement
			.getParentNode()) {

		    String tns = ((Element) element)
			    .getAttribute("targetNamespace");
		    if (tns != null && tns.trim().length() > 0) {
			serviceNamespaceURI = tns;
			break;
		    }
		}
		if (serviceNamespaceURI == null) {
		 // TODO throw exception "A target namespace declaration (targetNamespace attribute) "+ "at or above the service element in the WSDL file ("+ url + ") could not be found.");
		    return;
		}

		QName serviceQName = new QName(serviceNamespaceURI, serviceName);

		Service service = null;
		try {
		    service = Service.create(url, serviceQName);
		} catch (Exception e) {
		    // TODO throw exception e.getClass().getName() + " while using JAX-WS to create a handle for "+ "the service "+ serviceQName+ ", either the WSDL or the expected service name is wrong");
		    return;
		}
		//out.println("<h2>Service " + serviceName + " (namespace "+ serviceNamespaceURI + ")</h2>");

		Iterator<QName> portQNames = service.getPorts();
		if (!portQNames.hasNext()) {
		    continue;
		}
		do {
		    QName portQName = portQNames.next();
		    //out.print("<h3>Port " + portQName.getLocalPart()+ " (namespace " + portQName.getNamespaceURI()+ ")</h3>");

		    // <wsdl:port binding="impl:OntologyQuerySoapBinding"
		    // name="OntologyQuery">

		    NodeList portElements = wsdlDoc.getDocumentElement()
			    .getElementsByTagNameNS(
				    "http://schemas.xmlsoap.org/wsdl/", "port");
		    for (int j = 0; j < portElements.getLength(); j++) {
			Element portElement = (Element) portElements.item(j);
			if (!portQName.getLocalPart().equals(
				portElement.getAttribute("name"))) {
			    continue;
			}
			String binding = portElement.getAttribute("binding");
			if (binding == null || binding.trim().length() == 0) {
			    // TODO throw exception ("Error: Could not find binding attribute for port "+ portQName.getLocalPart());
			    return;
			}
			if (binding.indexOf(":") != -1) {
			    binding = binding
				    .substring(binding.indexOf(":") + 1); // lop
									  // off
									  // prefix
			}
			NodeList bindingElements = wsdlDoc.getDocumentElement()
				.getElementsByTagNameNS(
					"http://schemas.xmlsoap.org/wsdl/",
					"binding");

			for (int k = 0; k < bindingElements.getLength(); k++) {
			    Element bindingElement = (Element) bindingElements.item(k);
			    Element sawsdlBindingElement = (Element) bindingElement.cloneNode(false);
			    if (!binding.equals(bindingElement
				    .getAttribute("name"))) {
				continue;
			    }
			    String type = bindingElement.getAttribute("type");
			    if (type.indexOf(":") != -1) {
				type = type.substring(type.indexOf(":") + 1); // lop
									      // off
									      // prefix
			    }

			    NodeList soapBindings = bindingElement
				    .getElementsByTagNameNS(
					    "http://schemas.xmlsoap.org/wsdl/soap/",
					    "binding");

			    if (soapBindings.getLength() == 0) { // ignore GET,
								 // POST, etc.
								 // bindings for
								 // now
				continue;
			    }

			    Map<QName, String> op2Action = new LinkedHashMap<QName, String>();
			    Map<QName, QName> op2InMsg = new LinkedHashMap<QName, QName>();
			    Map<QName, QName> op2OutMsg = new LinkedHashMap<QName, QName>();
			    Map<QName, String> msg2Use = new LinkedHashMap<QName, String>();
			    Map<QName, Map<String, QName>> msg2Parts = new LinkedHashMap<QName, Map<String, QName>>();
			    @SuppressWarnings("unused")
			    Map<String, String> part2Type = new LinkedHashMap<String, String>();

			    // There is a soap binding!
			    String style = null;
			    for (int m = 0; m < soapBindings.getLength(); m++) {
				Element soapBinding = (Element) soapBindings.item(m);
				sawsdlBindingElement.appendChild(soapBinding.cloneNode(true));
				if (soapBinding.getAttribute("style") != null) {
				    style = soapBinding.getAttribute("style");
				    break;
				}
			    }
			    if (style == null) {
				// TODO throw exception ("Error: Could not find style of soap binding for " + binding);
				return;
			    }
			    

			    // read through to get the encoding and soap action
			    NodeList ops = bindingElement
				    .getElementsByTagNameNS(
					    "http://schemas.xmlsoap.org/wsdl/",
					    "operation");
			    for (int m = 0; m < ops.getLength(); m++) {
				Element op = (Element) ops.item(m);
				QName opName = getQName(op.getAttribute("name"), op);
				// this is where we filter for our operation name ...
				if (!opName.getLocalPart().equals(name)) {
				    continue;
				}
				sawsdlBindingElement.appendChild(op.cloneNode(true));
				sawsdlDoc.getDocumentElement().appendChild(sawsdlDoc.importNode(sawsdlBindingElement, true));
				// done with binding element
				sawsdlBindingElement = null;
				NodeList soapOps = op
					.getElementsByTagNameNS(
						"http://schemas.xmlsoap.org/wsdl/soap/",
						"operation");
				String soapAction = null;
				for (int n = 0; n < soapOps.getLength(); n++) {
				    Element soapOp = (Element) soapOps.item(n);
				    soapAction = soapOp
					    .getAttribute("soapAction");
				    if (soapAction != null
					    && soapAction.trim().length() > 0) {
					break;
				    }
				}
				if (soapAction == null) {
				 // TODO throw exception ("Error: Could not find a soapAction attribute for operation "+ opName.getLocalPart() + "(NS "+ opName.getNamespaceURI()))
				    return;
				}
				op2Action.put(opName, soapAction);

				NodeList inputs = op.getElementsByTagNameNS(
					"http://schemas.xmlsoap.org/wsdl/",
					"input");
				String inputMsgName = null;
				@SuppressWarnings("unused")
				QName inputMsgQName = null;
				for (int n = 0; n < inputs.getLength(); n++) {
				    Element input = (Element) inputs.item(n);
				    inputMsgName = input.getAttribute("name");

				    NodeList soapInputs = input
					    .getElementsByTagNameNS(
						    "http://schemas.xmlsoap.org/wsdl/soap/",
						    "body");
				    if (soapInputs == null
					    || soapInputs.getLength() == 0) {
					// TODO throw exception ("Error: Could not find a SOAP body definition for operation " + opName.getLocalPart() + "(NS " + opName.getNamespaceURI() + ")\n");
					return;
				    }
				    Element bodyDef = (Element) soapInputs
					    .item(0);
				    String use = bodyDef.getAttribute("use");
				    if (use == null || use.trim().length() == 0) {
					// TODO throw exception ("Error: Could not find a SOAP body definition " + "'use' attribute for input of operation " + opName.getLocalPart() + "(NS " + opName.getNamespaceURI() + ")\n");
					return;
				    }
				    if (inputMsgName == null
					    || inputMsgName.trim().length() == 0) {
					msg2Use.put(opName, use); // applies to
								  // all inputs
				    } else {
					msg2Use.put(
						getQName(inputMsgName, input),
						use);
				    }
				    // for doc/lit, rpc/enc will come from
				    // portType def
				    // if(use.equals("literal")){
				    // op2InMsg.put(opName,
				    // getQName(inputMsgName, input));
				    // }
				}

				NodeList outputs = op.getElementsByTagNameNS(
					"http://schemas.xmlsoap.org/wsdl/",
					"output");
				String outputMsgName = null;
				QName outputMsgQName = null;
				for (int n = 0; n < outputs.getLength(); n++) {
				    Element output = (Element) outputs.item(n);
				    outputMsgName = output.getAttribute("name");
				    if (outputMsgName == null
					    || outputMsgName.trim().length() == 0) {
					outputMsgName = opName.getLocalPart()
						+ "Response";
				    }
				    outputMsgQName = getQName(outputMsgName,
					    output);
				    NodeList soapOutputs = output
					    .getElementsByTagNameNS(
						    "http://schemas.xmlsoap.org/wsdl/soap/",
						    "body");
				    if (soapOutputs == null
					    || soapOutputs.getLength() == 0) {
					// TODO throw exception ("Error: Could not find a SOAP body definition for operation " + opName.getLocalPart() + "(NS " + opName.getNamespaceURI() + ")\n");
					return;
				    }
				    Element bodyDef = (Element) soapOutputs
					    .item(0);
				    String use = bodyDef.getAttribute("use");
				    if (use == null || use.trim().length() == 0) {
					// TODO throw exception ("Error: Could not find a SOAP body definition " + "'use' attribute for output of operation " + opName.getLocalPart() + "(NS " + opName.getNamespaceURI() + ")\n");
					return;
				    }
				    msg2Use.put(outputMsgQName, use);
				}
				if (outputMsgName != null
					&& outputMsgName.trim().length() != 0) {
				    op2OutMsg.put(opName, outputMsgQName); // for
									   // doc/lit,
									   // rpc/enc
									   // will
									   // come
									   // from
									   // portType
									   // def
				}
			    } // for wsdl:operations
			      // Before we can print the service info, we need
			      // to collect datatype information

			    Map<QName, QName> msg2MsgDef = new LinkedHashMap<QName, QName>();
			    NodeList portTypeBindings = wsdlDoc
				    .getElementsByTagNameNS(
					    "http://schemas.xmlsoap.org/wsdl/",
					    "portType");
			    // System.err.println("There are " +
			    // portTypeBindings.getLength() +
			    // " port type bindings");
			    for (int m = 0; m < portTypeBindings.getLength(); m++) {
				Element portTypeBinding = (Element) portTypeBindings.item(m);
				Element sawsdlportTypeBinding = (Element) portTypeBinding.cloneNode(false);
				
				if (type.equals(portTypeBinding
					.getAttribute("name"))) {
				    // out.print("<h4>SOAP Binding " + binding +
				    // "/ portType " +
				    // type + " / style " + style+"</h4>");
				    NodeList operations = portTypeBinding
					    .getElementsByTagNameNS(
						    "http://schemas.xmlsoap.org/wsdl/",
						    "operation");
				    // System.err.println("There are " +
				    // operations.getLength() +
				    // " operation bindings");
				    for (int n = 0; n < operations.getLength(); n++) {
					Element operation = (Element) operations.item(n);
					
					QName opName = getQName(operation.getAttribute("name"),operation);
					
					if (!opName.getLocalPart().equals(name)) {
					    continue;
					}
					sawsdlportTypeBinding.appendChild(operation.cloneNode(true));
					sawsdlDoc.getDocumentElement().appendChild(sawsdlDoc.importNode(sawsdlportTypeBinding, true));
					// TODO before adding it to our document, get the message elements based on the name using input message attribute ...

					NodeList inputs = operation
						.getElementsByTagNameNS(
							"http://schemas.xmlsoap.org/wsdl/",
							"input");
					// System.err.println("There are " +
					// inputs.getLength() +
					// " input bindings");
					if (inputs == null
						|| inputs.getLength() == 0) {
					 // TODO throw exception ("Error: Could not find a WSDL input definition for operation " + opName.getLocalPart() + "(NS " + opName.getNamespaceURI() + ")\n");
					    return;
					}
					Element input = (Element) inputs
						.item(0);
					String inputMessage = input
						.getAttribute("message");
					if (inputMessage == null
						|| inputMessage.trim().length() == 0) {
					    // TODO throw exception ("Error: Could not find a WSDL portType input message type for operation " + opName.getLocalPart() + "(NS "+ opName.getNamespaceURI()  + ")\n");
					    return;
					}
					String inputName = input
						.getAttribute("name");
					String use = msg2Use.get(getQName(
						inputMessage, input));
					if (use == null) {
					    // lit/enc may be defined for whole
					    // op rather than individual
					    // messages
					    use = msg2Use.get(opName);
					}
					if (use != null
						&& use.equals("literal")) {
					    msg2MsgDef.put(
						    getQName(inputName, input),
						    getQName(inputMessage,
							    input));
					}
					// System.err.println("Recorded op " +
					// opName + " mapping to " +
					// getQName(inputMessage, input));
					op2InMsg.put(opName,
						getQName(inputMessage, input)); // rpc/enc

					NodeList outputs = operation
						.getElementsByTagNameNS(
							"http://schemas.xmlsoap.org/wsdl/",
							"output");
					if (outputs == null
						|| outputs.getLength() == 0) {
					    // TODO throw exception ("Error: Could not find a WSDL output definition for operation " + opName.getLocalPart() + "(NS " + opName.getNamespaceURI() + ")\n");
					    return;
					}
					Element output = (Element) outputs
						.item(0);
					String outputMessage = output
						.getAttribute("message");
					if (outputMessage == null
						|| outputMessage.trim()
							.length() == 0) {
					    // TODO throw exception ("Error: Could not find a WSDL portType output message type for operation " + opName.getLocalPart()+ "(NS " + opName.getNamespaceURI() + ")\n");
					    return;
					}
					String outputName = output
						.getAttribute("name");
					if (outputName == null
						|| outputName.trim().length() == 0) {
					    op2OutMsg.put(
						    opName,
						    getQName(outputMessage,
							    output)); // rpc/enc
								      // doesn't
								      // have a
								      // name
					} else {
					    msg2MsgDef
						    .put(getQName(outputName,
							    output),
							    getQName(
								    outputMessage,
								    output)); // for
									      // doc/lit
					}
				    }
				}
			    } // for wsdl:portType

			    // Check the messages to actually populate the forms
			    NodeList messages = wsdlDoc.getElementsByTagNameNS(
				    "http://schemas.xmlsoap.org/wsdl/",
				    "message");
			    for (int m = 0; m < messages.getLength(); m++) {
				Element message = (Element) messages.item(m);
				
				String messageName = message.getAttribute("name");
				// TODO if messageName == name of sawsdl operation message name -> save it!
				if (messageName == null
					|| messageName.trim().length() == 0) {
				    // TODO throw exception ("Error: Could not find message name attribute (message #" + m + " in the WSDL");
				    return;
				}
				QName messageQName = getQName(messageName,message);
				for (QName q : op2InMsg.keySet()) {
				    if (op2InMsg.get(q).getNamespaceURI().equals(messageQName.getNamespaceURI()) && op2InMsg.get(q).getLocalPart().equals(messageQName.getLocalPart())) {
					// input message
					Element e = (Element)sawsdlDoc.importNode(message, true);
					// look for each 'part' and add attributes for modelReference and loweringSchemaMapping
					NodeList parts = e.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/","part");
					for (int n = 0; n < parts.getLength(); n++) {
					    Element part = (Element) parts.item(n);
					    part.setAttributeNS(
							SadiPrefixResolver.SAWSDL_NAMESPACE,
							"modelReference", "owl#inputClass");
					    part.setAttributeNS(
							SadiPrefixResolver.SAWSDL_NAMESPACE,
							"loweringSchemaMapping", "lowering");
					}
					sawsdlDoc.getDocumentElement().appendChild(e);
				    }
				    if (op2OutMsg.get(q).getNamespaceURI().equals(messageQName.getNamespaceURI()) && op2OutMsg.get(q).getLocalPart().equals(messageQName.getLocalPart())) {
					// output message
					Element e = (Element)sawsdlDoc.importNode(message, true);
					// look for each 'part' and add attributes for modelReference and loweringSchemaMapping
					NodeList parts = e.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/","part");
					for (int n = 0; n < parts.getLength(); n++) {
					    Element part = (Element) parts.item(n);
					    part.setAttributeNS(
							SadiPrefixResolver.SAWSDL_NAMESPACE,
							"modelReference", "owl#outputClass");
					    part.setAttributeNS(
							SadiPrefixResolver.SAWSDL_NAMESPACE,
							"liftingSchemaMapping", "lifting");
					}
					sawsdlDoc.getDocumentElement().appendChild(e);
				    }
				}

				NodeList parts = message
					.getElementsByTagNameNS(
						"http://schemas.xmlsoap.org/wsdl/",
						"part");
				Map<String, QName> partsMap = new LinkedHashMap<String, QName>();
				for (int n = 0; n < parts.getLength(); n++) {
				    Element part = (Element) parts.item(n);
				    String partName = part.getAttribute("name");
				    // type is encoded, literal if element
				    String datatype = part.getAttribute("type");
				    String element = part
					    .getAttribute("element");
				    if (datatype != null
					    && datatype.trim().length() != 0) {
					partsMap.put(partName,
						getQName(datatype, part)); // rpc/encoded
				    } else if (element != null
					    && element.trim().length() != 0) {
					// it's an element we need to chase down
					// for the types later
					partsMap.put(partName,
						getQName(element, part));
				    } else {
					// TODO throw exception ("Error: Could not find either an 'element' for " + "'type' attribute for message part " + partName + " of message " + messageName);
					return;
				    }
				}
				msg2Parts.put(messageQName, partsMap);
			    }

			    NodeList types = wsdlDoc
				    .getElementsByTagNameNS(
					    "http://schemas.xmlsoap.org/wsdl/",
					    "types");
			    Map<QName, Map<String, QName>> element2Members = new LinkedHashMap<QName, Map<String, QName>>();
			    Map<QName, Map<String, QName>> type2Members = new LinkedHashMap<QName, Map<String, QName>>();
			    			    
			    for (int m = 0; m < types.getLength(); m++) {
				Element typeTag = (Element) types.item(m);
				
				// save all types in the sawsdl ...
				sawsdlDoc.getDocumentElement().appendChild(sawsdlDoc.importNode(typeTag, true));
				
				MyNodeList typeDefs = new MyNodeList();
				// Get the list of data types under each schema
				// tag
				NodeList typeChildren = typeTag.getChildNodes();
				for (int n = 0; n < typeChildren.getLength(); n++) {
				    Node typeChild = typeChildren.item(n);
				    if (typeChild instanceof Element
					    && "schema".equals(typeChild
						    .getLocalName())) {
					typeDefs.add(getTypes((Element) typeChild));
				    }
				}

				for (int n = 0; n < typeDefs.getLength(); n++) {
				    Element element = (Element) typeDefs
					    .item(n);

				    String elementName = element
					    .getAttribute("name");
				    if (elementName == null
					    || elementName.trim().length() == 0) {
					// TODO throw exception ("Error: Could not find the name attribute for a schema element (#" + n + " of type declaration block #" + m +")" );
					return;
				    }

				    Map<String, QName> memberMap = new LinkedHashMap<String, QName>();

				    // It could either be a basic type (handled
				    // right here), or a complex one with
				    // subfields
				    String elementType = element
					    .getAttribute("type");
				    if (elementType != null
					    && elementType.trim().length() != 0) {
					QName eT = getQName(elementType,
						element);
					if (eT.getNamespaceURI() == null
						|| eT.getNamespaceURI()
							.equals("http://www.w3.org/2001/XMLSchema")) {
					    memberMap.put(BASIC_TYPE_SENTINEL,
						    eT);
					    memberMap
						    .put(BASIC_NAME_SENTINEL,
							    new QName(null,
								    elementName));
					} else {
					    // pointer to complex type elsewhere
					    // in the schema tag
					    memberMap.put(
						    BASIC_TYPE_SENTINEL,
						    getQName(getRef(eT),
							    element));
					    memberMap
						    .put(BASIC_NAME_SENTINEL,
							    new QName(null,
								    elementName));
					}
				    }

				    NodeList subelements = element
					    .getElementsByTagNameNS(
						    "http://www.w3.org/2001/XMLSchema",
						    "element");
				    // If there are no subelements, it could be
				    // a simple type such as an enumeration,
				    // the other base condition besides the
				    // basic type handled above
				    if (subelements.getLength() == 0) {
					NodeList restrictions = element
						.getElementsByTagNameNS(
							"http://www.w3.org/2001/XMLSchema",
							"restriction");
					if (restrictions.getLength() == 0) {
					    restrictions = element
						    .getElementsByTagNameNS(
							    "http://www.w3.org/2001/XMLSchema",
							    "extension");
					    if (restrictions.getLength() != 0) {
						String baseAttr = restrictions
							.getLength() == 0 ? "xs:string"
							: ((Element) restrictions
								.item(0))
								.getAttribute("base");
						@SuppressWarnings("unused")
						QName baseType = getQName(
							baseAttr, element);
						QName anonQName = getQName(
							baseAttr + "SubType"
								+ anonCount++,
							element);
						memberMap.put(
							BASIC_TYPE_SENTINEL,
							anonQName);
						memberMap.put(
							BASIC_NAME_SENTINEL,
							new QName(null,
								elementName));
					    }
					    // else, some time you just get a
					    // blank type...
					} else {
					    // TODO handle extension properly
					    // we have a restriction ... get it
					    
					    // what is the base of the restriction?					    
					    NodeList attrElements = element.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema","attribute");
					    for (int p = 0; attrElements != null && p < attrElements.getLength(); p++) {
						Element attrElement = (Element) attrElements.item(p);
						String wsdlArrayType = attrElement.getAttributeNS("http://schemas.xmlsoap.org/wsdl/","arrayType");
						if (wsdlArrayType != null && wsdlArrayType.trim().length() > 0) {
						    memberMap.put(
							    ARRAY_TYPE_SENTINEL,
							    getQName(wsdlArrayType,attrElement));
						}
					    }
					}
				    }
				    for (int p = 0; p < subelements.getLength(); p++) {
					Element subelement = (Element) subelements
						.item(p);
					String subelementRef = subelement
						.getAttribute("ref");
					String subelementType = subelement
						.getAttribute("type");
					if (subelementRef != null
						&& subelementRef.trim()
							.length() != 0) {
					    // dereference
					    QName subq = getQName(
						    subelementRef, subelement);
					    subelementType = getRef(subq);
					}
					String subelementName = subelement
						.getAttribute("name");
					if (subelementName == null
						|| subelementName.trim()
							.length() == 0) {
					    if (subelementRef == null
						    || subelementRef.trim()
							    .length() == 0) {
						// TODO throw exception ("Error: Could not find the name attribute for a schema subelement (#" + p + " of schema element " + elementName );
						return;
					    } else {
						subelementName = subelementRef;
					    }
					}
					if (subelementType == null
						|| subelementType.trim()
							.length() == 0) {
					    // Last ditch, see if the type is
					    // declared directly and anonymously
					    // in the subelement declaration. If
					    // so, generate a new datatype
					    // that'll be added to the parsing
					    // list being processed in this
					    // loop...
					    NodeList exts = getTypes(subelement);
					    if (exts.getLength() == 1) {
						// Assume the first complexType
						// tag is the top level one
						QName anonQName = getQName(
							"anonymousType"
								+ anonCount++,
							element);
						subelementType = getRef(anonQName);
						Element anonTypeDef = (Element) exts
							.item(0)
							.cloneNode(true); // true
									  // =
									  // deep
									  // copy
						anonTypeDef
							.setAttribute(
								"name",
								anonQName
									.getLocalPart());
						// Now insert the anonymous type
						// into the parse tree and types
						// list
						// so it is resolved correctly
						// with QName and all
						element.getParentNode()
							.appendChild(
								anonTypeDef);
						typeDefs.add(anonTypeDef);
						// System.err.println("Created data type "
						// + anonQName +
						// " for subelement " +
						// subelementName +
						// " of element " +
						// elementName);
					    } else {
						// TODO throw exception ("Error: Could not find the type attribute for a schema subelement (#" + p + " of schema element " + elementName + "), found " + exts.getLength() + " candidates");
						return;
					    }
					}

					// Check for array types, doc/lit
					String maxOccurs = subelement
						.getAttribute("maxOccurs");
					if ("unbounded".equals(maxOccurs)) {
					    subelementType = subelementType
						    + "[]"; // [] notes the
							    // array type
					}
					String minOccurs = subelement
						.getAttribute("minOccurs");
					if ("0".equals(minOccurs)) {
					    // QName does not check for XML
					    // spec's NCName compatability, so
					    // '~' is safe
					    subelementType = subelementType
						    + "~";
					    // System.err.println("Made "+subelementName+" optional");
					}
					memberMap.put(
						subelementName,
						getQName(subelementType,
							subelement));
				    }
				    // Check for array types, rpc/encoded
				    // e.g. <xsd:attribute
				    // ref="soapenc:arrayType"
				    // wsdl:arrayType="typens:Subtype[]"/>
				    NodeList attrElements = element
					    .getElementsByTagNameNS(
						    "http://www.w3.org/2001/XMLSchema",
						    "attribute");
				    for (int p = 0; attrElements != null
					    && p < attrElements.getLength(); p++) {
					Element attrElement = (Element) attrElements
						.item(p);
					String wsdlArrayType = attrElement
						.getAttributeNS(
							"http://schemas.xmlsoap.org/wsdl/",
							"arrayType");
					if (wsdlArrayType != null
						&& wsdlArrayType.trim()
							.length() > 0) {
					    memberMap.put(
						    ARRAY_TYPE_SENTINEL,
						    getQName(wsdlArrayType,
							    attrElement));
					}
				    }

				    if (element.getLocalName()
					    .equals("element")) {
					element2Members.put(
						getQName(elementName, element),
						memberMap);
				    } else { // probably complexType def
					type2Members.put(
						getQName(elementName, element),
						memberMap);
				    }
				} // end for <schema><element|complexType>...
			    } // for wsdl:types

			    // Now we have enough info to print forms for the
			    // operations
			    // System.err.println("About to print services");
			    //inputs
			    for (QName opQName : op2InMsg.keySet()) {
				// System.err.println("printing service for " +
				// opQName);
				QName messageQName = op2InMsg.get(opQName);
				if (messageQName == null) {
				    System.err
					    .println("Got null value for message of "
						    + opQName);
				    continue;
				}
				// set the encapsulating element to the
				// operation name if rpc, or element name if doc				
				Map<String, QName> partsMap = msg2Parts
					.get(messageQName);
				if (partsMap == null) {
				    System.err
					    .println("Got null parts map for message "
						    + messageQName);
				    continue;
				}
				boolean isRpcStyle = "rpc".equals(style);

				String use = msg2Use.get(messageQName);
				if (use == null) {
				    use = msg2Use.get(opQName);
				}

				for (Map.Entry<String, QName> part : partsMap
					.entrySet()) {
				    // doc style, should be only one part
				    if (!isRpcStyle) { // assume document style
					QName dataType = part.getValue();
					
					Map<String, QName> subpartsMap = element2Members
						.get(dataType);
					if (subpartsMap == null) {
					    if (dataType
						    .getNamespaceURI()
						    .equals("http://schemas.xmlsoap.org/soap/encoding/")) {
						inputSoapDatatypes.add(generateSoapX(part.getKey(),
							part.getValue(),
							element2Members,
							type2Members, ""));
					    } else {
						// TODO log error ("Error: cannot find definition for data type " + dataType + "\nValid types are:");
//						for (QName key : element2Members
//							.keySet()) {
//						    out.print(" " + key);
//						}
					    }
					    continue;
					}
					if (subpartsMap
						.containsKey(BASIC_TYPE_SENTINEL)) {
					    QName t = subpartsMap
						    .get(BASIC_TYPE_SENTINEL);
					    while (DEFERRED_NAMESPACE_URI
						    .equals(t.getNamespaceURI())) {
						String[] p = t.getLocalPart()
							.split("_deferred_");
						t = new QName(decode(p[0]),
							p[1]);
					    }
					    if (t.getNamespaceURI()
						    .equals("http://www.w3.org/2001/XMLSchema")) {
						throw new Exception(
							"Got bare XSD type as contents of WSDL message");
					    }
					    subpartsMap = type2Members.get(t);
					}
					for (Map.Entry<String, QName> subpart : subpartsMap
						.entrySet()) {
					    inputSoapDatatypes.add(generateSoapX(
						    subpart.getKey(),
						    subpart.getValue(),
						    type2Members, type2Members,
						    ""));
					}
				    } else { // rpc style
					inputSoapDatatypes.add(generateSoapX(part.getKey(),
						part.getValue(),
						element2Members, type2Members,
						""));
				    }
				}
			    } // for input ops
			    
			    for (QName opQName : op2OutMsg.keySet()) {
				// System.err.println("printing service for " +
				// opQName);
				QName messageQName = op2OutMsg.get(opQName);
				if (messageQName == null) {
				    System.err
					    .println("Got null value for message of "
						    + opQName);
				    continue;
				}
				// set the encapsulating element to the
				// operation name if rpc, or element name if doc				
				Map<String, QName> partsMap = msg2Parts
					.get(messageQName);
				if (partsMap == null) {
				    System.err
					    .println("Got null parts map for message "
						    + messageQName);
				    continue;
				}
				boolean isRpcStyle = "rpc".equals(style);

				String use = msg2Use.get(messageQName);
				if (use == null) {
				    use = msg2Use.get(opQName);
				}

				for (Map.Entry<String, QName> part : partsMap
					.entrySet()) {
				    // doc style, should be only one part
				    if (!isRpcStyle) { // assume document style
					QName dataType = part.getValue();
					
					Map<String, QName> subpartsMap = element2Members
						.get(dataType);
					if (subpartsMap == null) {
					    if (dataType
						    .getNamespaceURI()
						    .equals("http://schemas.xmlsoap.org/soap/encoding/")) {
						outputSoapDatatypes.add(generateSoapX(part.getKey(),
							part.getValue(),
							element2Members,
							type2Members, ""));
					    } else {
						// TODO log error ("Error: cannot find definition for data type " + dataType + "\nValid types are:");
//						for (QName key : element2Members
//							.keySet()) {
//						    out.print(" " + key);
//						}
					    }
					    continue;
					}
					if (subpartsMap
						.containsKey(BASIC_TYPE_SENTINEL)) {
					    QName t = subpartsMap
						    .get(BASIC_TYPE_SENTINEL);
					    while (DEFERRED_NAMESPACE_URI
						    .equals(t.getNamespaceURI())) {
						String[] p = t.getLocalPart()
							.split("_deferred_");
						t = new QName(decode(p[0]),
							p[1]);
					    }
					    if (t.getNamespaceURI()
						    .equals("http://www.w3.org/2001/XMLSchema")) {
						throw new Exception(
							"Got bare XSD type as contents of WSDL message");
					    }
					    subpartsMap = type2Members.get(t);
					}
					for (Map.Entry<String, QName> subpart : subpartsMap
						.entrySet()) {
					    outputSoapDatatypes.add(generateSoapX(
						    subpart.getKey(),
						    subpart.getValue(),
						    type2Members, type2Members,
						    ""));
					}
				    } else { // rpc style
					outputSoapDatatypes.add(generateSoapX(part.getKey(),
						part.getValue(),
						element2Members, type2Members,
						""));
				    }
				}
			    } // for ops
			    
			} // for wsdl:binding
		    } // for wsdl port
		} while (portQNames.hasNext());
		// for jax-ws ports
	    } // for services
	    
	} catch (java.io.IOException ioe) {
	    logger.log(Level.SEVERE,
		    "While printing HTML form to servlet output stream", ioe);
	    ioe.printStackTrace();
	    return;
	} catch (org.xml.sax.SAXException saxe) {
	    logger.log(Level.SEVERE, "While parsing WSDL URL " + url, saxe);
	    saxe.printStackTrace();
	    return;
	} catch (Exception e) {
	    logger.log(Level.SEVERE, "While compiling WSDL URL " + url, e);
	    e.printStackTrace();
	    return;
	}
    }

    // Find all declarations immediately below the given element if they are of
    // the form element, simpleType or complexType
    private MyNodeList getTypes(Element parent) {
	MyNodeList nl = new MyNodeList();
	NodeList candidates = parent.getElementsByTagNameNS(
		"http://www.w3.org/2001/XMLSchema", "complexType");
	for (int i = 0; i < candidates.getLength(); i++) {
	    // Immediate descendent
	    Node candidate = candidates.item(i);
	    if (candidate.getParentNode() == parent) {
		nl.add(candidate);
	    }
	}
	candidates = parent.getElementsByTagNameNS(
		"http://www.w3.org/2001/XMLSchema", "simpleType");
	for (int i = 0; i < candidates.getLength(); i++) {
	    // Immediate descendent
	    Node candidate = candidates.item(i);
	    if (candidate.getParentNode() == parent) {
		nl.add(candidate);
	    }
	}
	candidates = parent.getElementsByTagNameNS(
		"http://www.w3.org/2001/XMLSchema", "element");
	for (int i = 0; i < candidates.getLength(); i++) {
	    // Immediate descendent
	    Node candidate = candidates.item(i);
	    if (candidate.getParentNode() == parent) {
		nl.add(candidate);
	    }
	}
	return nl;
    }

    // Use to reference another qname, for dereferencing later
    private String getRef(QName qname) {
	return "deferred:" + encode(qname.getNamespaceURI()) + "_deferred_"
		+ qname.getLocalPart();
    }

    private QName getQName(String typeName, Node contextNode) throws Exception {
	if (typeName.indexOf(":") != -1) {
	    String p[] = typeName.split(":");
	    if (p.length != 2) {
		throw new Exception("Error: type attribute value '" + typeName
			+ "' does not have the expected 'ns:value' form.");
	    }
	    String tns = null;
	    if (p[0].equals("deferred")) {
		tns = DEFERRED_NAMESPACE_URI;
	    } else {
		tns = contextNode.lookupNamespaceURI(p[0]);
	    }
	    if (p[0].equals("tns")) { // sometimes the tns prefix is not
				      // declared, but used anyway
		return getTargetNSQName(p[1], contextNode);
	    } else {
		return new QName(tns, p[1]);
	    }
	} else {
	    return getTargetNSQName(typeName, contextNode);
	}
    }

    private QName getTargetNSQName(String typeName, Node contextNode) {
	// See if targetNamespace is defined in an ancestor element, otherwise
	// the NS will be null
	String targetNSURI = null;
	for (Element ancestor = (Element) contextNode; ancestor != null; ancestor = (Element) ancestor
		.getParentNode()) {
	    String tns = ancestor.getAttribute("targetNamespace");
	    if (tns != null && tns.trim().length() != 0) {
		targetNSURI = tns;
		break;
	    }
	}
	return new QName(targetNSURI, typeName);
    }

    /**
     * Make the url safe for use as a datatype, i.e. get rid of ':'
     */
    private String encode(String q) {
	return q.replaceAll(":", "cOL_On");
    }

    private String decode(String q) {
	return q.replaceAll("cOL_On", ":");
    }

    @SuppressWarnings("rawtypes")
    private SoapDatatypeMapping generateSoapX(String memberName, 
	    QName dataType, Map<QName, Map<String, QName>> msg2Parts,
	    Map<QName, Map<String, QName>> type2Parts, String prefix) {
	
	SoapDatatypeMapping soapDatatypeMapping = new SoapDatatypeMapping();

	String datatype = dataType.getLocalPart();

	// check if optional
	boolean isOptional = false;
	if (datatype.endsWith("~")) {
	    isOptional = true;
	    datatype = datatype.substring(0, datatype.length() - 1);
	    dataType = new QName(dataType.getNamespaceURI(), datatype);
	}

	// check if it's a reference
	while (DEFERRED_NAMESPACE_URI.equals(dataType.getNamespaceURI())) {
	    String[] p = dataType.getLocalPart().split("_deferred_");
	    dataType = new QName(decode(p[0]), p[1]);
	}

	datatype = dataType.getLocalPart();

	// check if its an array
	boolean isArray = false;
	if (datatype.endsWith("[]") 
		|| (type2Parts.containsKey(dataType) && ((Map)type2Parts.get(dataType)).containsKey(ARRAY_TYPE_SENTINEL))//
		) {
	    isArray = true;
	    if (datatype.endsWith("[]"))//
		datatype = datatype.substring(0, datatype.length() - 2);
	    dataType = new QName(dataType.getNamespaceURI(), datatype);
	}

	if ("http://www.w3.org/2001/XMLSchema".equals(dataType
		.getNamespaceURI())
		|| "http://schemas.xmlsoap.org/soap/encoding/".equals(dataType
			.getNamespaceURI())) {
	    if (!isArray) {
		soapDatatypeMapping.setArray(false);
		soapDatatypeMapping.setOptional(isOptional);
		soapDatatypeMapping.setPrefix(prefix);
		soapDatatypeMapping.setMemberName(memberName);
		if (memberName.equals(ARRAY_TYPE_SENTINEL)) {
		    memberName = ""; // composite is an array, ignore the
				     // sentinel name
		}
		if (datatype.equals("string")) {
		    soapDatatypeMapping.setDatatype(datatype);
		    
		} else if (datatype.equals("int")) {
		    soapDatatypeMapping.setDatatype(datatype);
		} else if (datatype.equals("double")) {
		    soapDatatypeMapping.setDatatype(datatype);
		} else if (datatype.equals("float")) {
		    soapDatatypeMapping.setDatatype(datatype);
		} else if (datatype.equals("boolean")) {
		    soapDatatypeMapping.setDatatype(datatype);
		} else {
		    soapDatatypeMapping.setDatatype(datatype);
		}
		// todo: deal with string enum
	    } else { // isArray
		soapDatatypeMapping.setArray(true);
		if (memberName.equals(ARRAY_TYPE_SENTINEL)) {
		    memberName = ""; // composite is an array, ignore the
				     // sentinel name
		    if (prefix.endsWith(":")) { // get rid of blank prefix in
						// this instance
			prefix = prefix.substring(0, prefix.length() - 1);
		    }
		}
		soapDatatypeMapping.setOptional(isOptional);
		soapDatatypeMapping.setPrefix(prefix);
		soapDatatypeMapping.setMemberName(memberName);
		if (datatype.equals("string")) {
		    soapDatatypeMapping.setDatatype(datatype);
		} else if (datatype.equals("int")) {
		    soapDatatypeMapping.setDatatype(datatype);
		} else if (datatype.equals("double")) {
		    soapDatatypeMapping.setDatatype(datatype);
		} else if (datatype.equals("float")) {
		    soapDatatypeMapping.setDatatype(datatype);
		} else if (datatype.equals("boolean")) {
		    soapDatatypeMapping.setDatatype(datatype);
		} else {
		    soapDatatypeMapping.setDatatype(datatype);
		}
	    }
	} else { // doc/lit or complex rpc type
	    Map<String, QName> subparts = msg2Parts.get(dataType);
	    if (subparts == null) {
		subparts = type2Parts.get(dataType);
		if (subparts == null) {
		    StringBuilder sb = new StringBuilder();
		    sb.append("Error: cannot find definition for data type " + dataType); // +"\nValid types are:");
		     for(QName key: msg2Parts.keySet()){
			 sb.append(" "+key);
		     }
		     logger.warning(sb.toString());
		    return null;
		}
		// eddie added whole block ....
		if (subparts.containsKey(BASIC_TYPE_SENTINEL)) {
		    QName typeName = subparts.get(BASIC_TYPE_SENTINEL);
		    if (isArray) {
			typeName = new QName(typeName.getNamespaceURI(),
				typeName.getLocalPart() + "[]");
		    }
		    if (isOptional) {
			typeName = new QName(typeName.getNamespaceURI(),
				typeName.getLocalPart() + "~");
		    }
		    return generateSoapX(memberName, typeName, type2Parts,type2Parts, prefix);
		} else if (subparts.containsKey(ARRAY_TYPE_SENTINEL)) {
		    if (memberName.equals(ARRAY_TYPE_SENTINEL)) {
			memberName = ""; // composite is an array, ignore the
			// sentinel name
			if (prefix.endsWith(":")) { // get rid of blank prefix in
			    // this instance
			    prefix = prefix.substring(0, prefix.length() - 1);
			}
		    }
		    QName typeName = subparts.get(ARRAY_TYPE_SENTINEL);
		    if (isArray) {
			typeName = new QName(typeName.getNamespaceURI(),
				typeName.getLocalPart());// + "[]");
		    }
		    if (isOptional) {
			typeName = new QName(typeName.getNamespaceURI(),
				typeName.getLocalPart() + "~");
		    }
		    return generateSoapX(memberName, typeName, type2Parts,type2Parts, prefix);
		    
		    
		    
//		    soapX.setArray(true);
//		    soapX.setOptional(isOptional);
//		    soapX.setPrefix(prefix);
//		    soapX.setMemberName(memberName);
//		    
//		    if (datatype.equals("string")) {
//			soapX.setDatatype(datatype);
//		    } else if (datatype.equals("int")) {
//			soapX.setDatatype(datatype);
//		    } else if (datatype.equals("double")) {
//			soapX.setDatatype(datatype);
//		    } else if (datatype.equals("float")) {
//			soapX.setDatatype(datatype);
//		    } else if (datatype.equals("boolean")) {
//			soapX.setDatatype(datatype);
//		    } else {
//			soapX.setDatatype(datatype);
//		    }
		}
	    } else if (subparts.containsKey(BASIC_TYPE_SENTINEL)) {
		QName typeName = subparts.get(BASIC_TYPE_SENTINEL);
		if (isArray) {
		    typeName = new QName(typeName.getNamespaceURI(),
			    typeName.getLocalPart() + "[]");
		}
		if (isOptional) {
		    typeName = new QName(typeName.getNamespaceURI(),
			    typeName.getLocalPart() + "~");
		}
		return generateSoapX(memberName, typeName, type2Parts,type2Parts, prefix);
	    } else {
		// currently we don't handle arrays of composites in the form...
		// FIXME
//		out.print("<table border=\"1\"><tr bgcolor=\"#DDDDDD\"><td>Composite "
//			+ memberName
//			+ " ("
//			+ (isOptional ? "optional, " : "")
//			+ datatype + "):</td></tr><tr><td>");
//		for (Map.Entry<String, QName> subpart : subparts.entrySet()) {
//		    writeDataType(subpart.getKey(), subpart.getValue(),type2Parts, type2Parts, prefix + memberName + ":");
//		}
//		out.print("</td></tr></table>\n");
	    }
	}
	return soapDatatypeMapping;

    }

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
    
    public ArrayList<String> getOperationNames() throws Exception {
	if (wsdlDoc == null) {
	    wsdlDoc = docBuilder.parse(url.openStream());
	    doImports(wsdlDoc, url);
	}
	NodeList ops = wsdlDoc.getDocumentElement().getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/","operation");
	ArrayList<String> services = new ArrayList<String>();
	// 
	for (int m = 0; m < ops.getLength(); m++) {
		try {
		    Element op = (Element) ops.item(m);
		    String opname = op.getAttribute("name");
		    if (opname != null) {
			services.add(opname);
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}
	return new ArrayList<String>(new HashSet<String>(services));
    }

    public ArrayList<SoapDatatypeMapping> getInputSoapDatatypes() {
        return inputSoapDatatypes;
    }
    
    public Map<String, Boolean> getInputSoap2IsArrayMap() {
	HashMap<String, Boolean> map = new HashMap<String, Boolean>();
	for (SoapDatatypeMapping x : inputSoapDatatypes) {
	    if (x != null)
		map.put(x.getMemberName(), x.isArray());
	}
	return map;
    }
    
    public Map<String, Boolean> getOutputSoap2IsArrayMap() {
	HashMap<String, Boolean> map = new HashMap<String, Boolean>();
	for (SoapDatatypeMapping x : outputSoapDatatypes) {
	    if (x != null)
		map.put(x.getMemberName(), x.isArray());
	}
	return map;
    }
    
    public ArrayList<String> getInputSoapDatatypeParameterNames() {
	ArrayList<String> names = new ArrayList<String>();
	for (SoapDatatypeMapping x : inputSoapDatatypes) {
	    if (x != null)
		names.add(x.getMemberName());
	}
	return names;
    }
    
    public boolean[] getInputSoapDatatypeRequirements() {
	boolean[] bools = new boolean[inputSoapDatatypes.size()];
	for (int x = 0; x < inputSoapDatatypes.size(); x++) {
	    if (inputSoapDatatypes.get(x) != null)
		bools[x] = !inputSoapDatatypes.get(x).isOptional();
	}
	return bools;
    }
    
    public boolean[] getOutputSoapDatatypeRequirements() {
	boolean[] bools = new boolean[outputSoapDatatypes.size()];
	for (int x = 0; x < outputSoapDatatypes.size(); x++) {
	    if (outputSoapDatatypes.get(x) != null)
		bools[x] = !outputSoapDatatypes.get(x).isOptional();
	}
	return bools;
    }
    
    public ArrayList<SoapDatatypeMapping> getOutputSoapDatatypes() {
        return outputSoapDatatypes;
    }

    public ArrayList<String> getOutputSoapDatatypeParameterNames() {
	ArrayList<String> names = new ArrayList<String>();
	for (SoapDatatypeMapping x : outputSoapDatatypes) {
	    if (x != null)
		names.add(x.getMemberName());
	}
	return names;
    }

    public Document getSawsdlDoc() {
        return sawsdlDoc;
    }

    public void setSawsdlDoc(Document sawsdlDoc) {
        this.sawsdlDoc = sawsdlDoc;
    }
}
