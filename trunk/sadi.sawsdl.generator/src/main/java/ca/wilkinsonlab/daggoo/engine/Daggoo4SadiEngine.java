package ca.wilkinsonlab.daggoo.engine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ca.wilkinsonlab.daggoo.LiftingMap;
import ca.wilkinsonlab.daggoo.LiftingSchemaMapping;
import ca.wilkinsonlab.daggoo.LoweringSchemaMapping;
import ca.wilkinsonlab.daggoo.ResultMap;
import ca.wilkinsonlab.daggoo.servlets.SoapServlet;
import ca.wilkinsonlab.daggoo.sparql.SparqlQueryEngine;
import ca.wilkinsonlab.daggoo.sparql.SparqlResult;
import ca.wilkinsonlab.daggoo.utils.IOUtils;
import ca.wilkinsonlab.daggoo.utils.WSDLConfig;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * 
 * @author Eddie Kawas
 * 
 */
public class Daggoo4SadiEngine {

    private static Logger logger = Logger.getLogger(Daggoo4SadiEngine.class
	    .getName());

    protected WSDLConfig wsdlConfig;
    
    private String mappingPrefix = "";

    private String serviceSpecParameterString;

    private XPath xPath = null;

    private ArrayList<LoweringSchemaMapping> loweringMappings = new ArrayList<LoweringSchemaMapping>();

    private ArrayList<LiftingSchemaMapping> liftingMappings = new ArrayList<LiftingSchemaMapping>();

    /**
     * Default constructor
     * 
     * @throws Exception
     */
    public Daggoo4SadiEngine(URL wsdlURL, String serviceName) throws Exception {
	this(wsdlURL, serviceName, "");
    }
    
    public Daggoo4SadiEngine(URL wsdlURL, String serviceName, String mapPrefix) throws Exception {
	wsdlConfig = new WSDLConfig(wsdlURL);
	wsdlConfig.setCurrentService(serviceName);
	this.mappingPrefix = mapPrefix;
	init();
    }
    
    

    private void init() throws Exception {
	Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.NullLogChute");
	// Explicitly set these so Java Web services will work in Java 1.5
	if (System.getProperty("javax.xml.stream.XMLInputFactory") == null) {
	    System.setProperty("javax.xml.stream.XMLInputFactory",
		    "com.ctc.wstx.stax.WstxInputFactory");
	}

	if (System.getProperty("javax.xml.stream.XMLOutputFactory") == null) {
	    System.setProperty("javax.xml.stream.XMLOutputFactory",
		    "com.ctc.wstx.stax.WstxOutputFactory");
	}

	try {
	    if (xPath == null) {
		xPath = (new org.apache.xpath.jaxp.XPathFactoryImpl())
			.newXPath();
	    }
	} catch (Exception e) {
	    logger.log(Level.SEVERE,
		    "Could not create an XPath: " + e.getMessage(), e);
	}

	// init our SERVICE_SPEC_PARAMETER string
	QName serv = wsdlConfig.getServiceQName();
	QName port = wsdlConfig.getPortQName();
	QName op = wsdlConfig.getOperationInputQName();
	String style = wsdlConfig.getOperationStyle();
	String use = wsdlConfig.getOperationEncoding();
	String opName = wsdlConfig.getOperationName();
	serviceSpecParameterString = String.format(
		"%s %s %s %s %s %s %s %s %s %s", serv.getNamespaceURI(),
		serv.getLocalPart(), port.getNamespaceURI(),
		port.getLocalPart(), op.getNamespaceURI(), op.getLocalPart(),
		wsdlConfig.getSoapAction(), opName, style, use);
	initInputMappings();
	initOutputMappings();
    }

    private void initInputMappings() throws MalformedURLException, IOException,
	    Exception {
	// process the loweringSchemaMappings
	Map<String, String> loweringMap = wsdlConfig.getPrimaryInputFormats();
	for (String key : loweringMap.keySet()) {
	    // key is the parameter name
	    // value is the url to our lowering schema for constructing our
	    String mapUrl = loweringMap.get(key);
	    if (!mappingPrefix.isEmpty()) {
		try {
		    new URL(mapUrl);
		} catch (MalformedURLException m) {
		    mapUrl = mappingPrefix + "/" + mapUrl;    
		}
	    }
	    URLConnection conn = new URL(mapUrl).openConnection();
	    conn.setUseCaches(false);
	    String lm = "";
	    if (conn instanceof HttpURLConnection) {
		lm = IOUtils.readFromConnection((HttpURLConnection)conn);
	    } else {
		lm = IOUtils.readFromConnection(conn);
	    }
	    // input
	    // get the sparql
	    String sparql = IOUtils.getSPARQLFromXML(lm);
	    // get the lowering schema
	    String lowering = IOUtils.getLoweringOrLiftingTemplateFromXML(lm);
	    LoweringSchemaMapping lsm = new LoweringSchemaMapping();
	    lsm.setSparqlQuery(sparql);
	    lsm.setTemplate(lowering);
	    lsm.setName(key);
	    loweringMappings.add(lsm);
	}
    }

    private void initOutputMappings() throws MalformedURLException,
	    IOException, Exception {
	Map<String, String> liftingMap = wsdlConfig.getPrimaryOutputFormats();
	for (String key : liftingMap.keySet()) {
	    // get our lifting schema
	    String mapUrl = liftingMap.get(key);
	    if (!mappingPrefix.isEmpty()) {
		try {
		    new URL(mapUrl);
		} catch (MalformedURLException m) {
		    mapUrl = mappingPrefix + "/" + mapUrl;    
		}
	    }
	    URLConnection conn = new URL(mapUrl).openConnection();
	    conn.setUseCaches(false);
	    String lm = "";
	    if (conn instanceof HttpURLConnection) {
		lm = IOUtils.readFromConnection((HttpURLConnection)conn);
	    } else {
		lm = IOUtils.readFromConnection(conn);
	    }
	    String lifting = IOUtils
		    .getLoweringOrLiftingTemplateFromXML(new ByteArrayInputStream(
			    lm.getBytes()));
	    // get the mappings
	    List<LiftingMap> mappings = IOUtils.getLiftingMappings(lm);
	    LiftingSchemaMapping lsm = new LiftingSchemaMapping();
	    lsm.setTemplate(lifting);
	    lsm.setLiftingMap(mappings);
	    lsm.setName(key);
	    liftingMappings.add(lsm);
	}
    }

    public Model processRequest(String sadiInput) {
	// serviceInputs map will contain an invocation for each key
	Map<String, Map<String, SparqlResult>> serviceInputs = null;
	ArrayList<Map<String, String>> soap_input_list = new ArrayList<Map<String, String>>();

	// for each LoweringSchemaMapping
	for (LoweringSchemaMapping l : loweringMappings) {
	    // apply sparql query to our data
	    serviceInputs = SparqlQueryEngine.executeSPARQL(sadiInput,
		    l.getSparqlQuery());
	    for (String key : serviceInputs.keySet()) {
		Map<String, String> soap_inputs = new HashMap<String, String>();
		Map<String, SparqlResult> m = serviceInputs.get(key);
		// map the sparql results to inputs for our SOAP service
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.NullLogChute");
		VelocityContext context = new VelocityContext();
		StringWriter writer = new StringWriter();
		// remove the entry from m and replace it with the parameter
		// name / value
		for (Iterator<String> iterator = m.keySet().iterator(); iterator.hasNext();) {
		    String k = iterator.next();
		    context.put(k, m.get(k).getValues());
		    // remove the current key using the iterator (to avoid
		    // concurrent modification exceptions)
		    iterator.remove();
		}
		// add our inputNodeURI to the context in case the template
		// needs it
		context.put(SparqlQueryEngine.INPUT_NODE_URI_KEY, key);
		try {
		    // apply velocity tempalte
		    ve.evaluate(context, writer, "", l.getTemplate());
		    // save data in our map (to be passed later on to SOAP
		    // service
		    soap_inputs.put(l.getName(), writer.toString());
		} catch (ParseErrorException e) {
		    e.printStackTrace();
		} catch (MethodInvocationException e) {
		    e.printStackTrace();
		} catch (ResourceNotFoundException e) {
		    e.printStackTrace();
		}
		soap_inputs.put(SparqlQueryEngine.INPUT_NODE_URI_KEY, key);
		soap_input_list.add(soap_inputs);
	    } // done mapping sparql results to service inputs
	} // done iterating over loweringSchemaMappings
	  // call SOAP service
	  // store result in a list
	ArrayList<ResultMap> resultList = new ArrayList<ResultMap>();

	// foreach set of inputs
	for (Map<String, String> inputs : soap_input_list) {
	    // merge 'inputs' and 'map'
	    HashMap<String, String> soapInvocationMap = new HashMap<String, String>();
	    soapInvocationMap.put(SoapServlet.SERVICE_SPEC_PARAM, serviceSpecParameterString);
	    soapInvocationMap.putAll(inputs);
	    soapInvocationMap.remove(SparqlQueryEngine.INPUT_NODE_URI_KEY);
	    // call our service
	    SoapServlet soap = new SoapServlet();
	    String serviceResult = soap.callService(soapInvocationMap, wsdlConfig.getSpecURL());
	    // save our result
	    resultList.add(new ResultMap(serviceResult, inputs));
	    logger.warning(serviceResult);
	}

	// create an empty model
	Model model = ModelFactory.createDefaultModel();
	for (ResultMap result : resultList) {
	    for (LiftingSchemaMapping lsm : liftingMappings) {
		// use velocity to fill in our RDF template
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.NullLogChute");
		VelocityContext context = new VelocityContext();
		StringWriter writer = new StringWriter();
		// remove the entry from m and replace it with the parameter
		// name / value
		for (LiftingMap l : lsm.getLiftingMap()) {
		    if (l.getType().equals(LiftingMap.STRING)) {
			// TODO check that values actually exist
			context.put(l.getId(), result.getInputs().get(l.getId()));
		    } else if (l.getType().equals(LiftingMap.XPATH)) {
			String resultXPath = l.getValue();
			NodeList resultNodes = null;
			XPathExpression xPathExp;
			try {
			    xPathExp = xPath.compile(resultXPath);
			    resultNodes = (NodeList) xPathExp.evaluate(
				    new InputSource(new ByteArrayInputStream(
					    result.getResult().getBytes())),
				    XPathConstants.NODESET);
			} catch (Exception e) {
			    e.printStackTrace();
			    context.put(l.getId(), new String[] {});
			    continue;
			}
			String[] nodes = new String[resultNodes.getLength()];
			for (int i = 0; i < resultNodes.getLength(); i++) {
			    nodes[i] = resultNodes.item(i).getTextContent();
			}
			context.put(l.getId(), nodes);
			// apply XPATH to our result
		    } else if (l.getType().equals(LiftingMap.REGEX)) {
			// TODO
		    }
		}
		try {
		    ve.evaluate(context, writer, "", lsm.getTemplate());
		} catch (ParseErrorException e) {
		    e.printStackTrace();
		} catch (MethodInvocationException e) {
		    e.printStackTrace();
		} catch (ResourceNotFoundException e) {
		    e.printStackTrace();
		}
		// should we merge models here?
		try {
		    model.read(new ByteArrayInputStream(writer.toString().getBytes()), null);
		} catch (Exception e) {
		    model.read(new ByteArrayInputStream(writer.toString().getBytes()), null, "N-TRIPLE");
		}
	    }
	}
	// done
	return model; //.write(sadiOutput, "RDF/XML-ABBREV");

    }

    public WSDLConfig getWsdlConfig() {
        return wsdlConfig;
    }

    public void setWsdlConfig(WSDLConfig wsdlConfig) {
        this.wsdlConfig = wsdlConfig;
    }

    public String getMappingPrefix() {
        return mappingPrefix;
    }

    public void setMappingPrefix(String mappingPrefix) {
        this.mappingPrefix = mappingPrefix;
    }
}
