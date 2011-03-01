package ca.wilkinsonlab.daggoo.utils;
import org.w3c.dom.*;

import ca.wilkinsonlab.daggoo.CachedNamespaceContextImpl;
import ca.wilkinsonlab.daggoo.SADISpecWrapper;
import ca.wilkinsonlab.daggoo.SadiPrefixResolver;
import ca.wilkinsonlab.daggoo.utils.SADITags;

import javax.xml.xpath.*;
import javax.xml.parsers.*;
import javax.xml.XMLConstants;

import java.io.InputStream;
import java.math.*;
import java.net.URL;
import java.util.*;

/**
 * This class presents HTML forms as if they were ACD file descriptions of a command.
 * This allows us to wrap Web pages as Moby services using a similar mechanism to that
 * which wraps ACD-described command line tools such as EMBOSS programs.
 */
public class XHTMLForm extends SADISpecWrapper{

    private static XPath xPath;
    private static DocumentBuilder docBuilder;
    private Document xhtmlDoc;
    private boolean strict = false; //are all moby markup fields required in the HTML?
    
    private Map<String,List<String>> formFiles;
    private Map<String,String> formEncType;
    private Map<String,String> formAction;
    private Map<String,String> formMethod;  // GET or POST
    private Map<String,Map<String,String>> formSubmitOptions;  // the submit buttons for each form
    private Map<String,Map<String,String>> formImageOptions;  // the image buttons fort each form (a type of submit)

    // used when strict = false to refer to services indirectly by the their form name/order
    private Map<String,String> alias2CanonicalServiceName;  

    public final static String METHOD_POST = "POST";
    public final static String METHOD_GET = "GET";
    public final static String SUBMIT_DATATYPE = "submit";
    public final static String IMAGE_DATATYPE = "image";

    public final static String NULL_NAME = "null";
    public final static String MULTIPART = "multipart/form-data";
    public final static String URLENCODED = "application/x-www-form-urlencoded";
    public final static String RADIO_SENTINEL = "noRealRadioOrAnyFormFieldShouldHaveThisValuePlease!";
    public final static String RADIO_DEFAULT_SENTINEL = "2noRealRadioOrAnyFormFieldShouldHaveThisValuePlease!";
    public final static String HIDDEN_SENTINEL = "noRealHiddenOrAnyFormFieldShouldHaveThisValuePlease!";
    public final static String IMAGE_ANONYMOUS_NAME = "noRealImageButtonShouldHaveThisNamePlease!";
    public final static String SUBMIT_ANONYMOUS_NAME = "noRealSubmitButtonShouldHaveThisNamePlease!";
    public final static String SUBMIT_DEFAULT_VALUE = "Submit Query";//Used by Direfox & IE7

    private final String MOBY_PREFIX_PLACEHOLDER = "%MOBYPREFIX%";
    private final String ANON_SERVICE_PREFIX = "noServiceShouldHaveThisNamePlease";

    private final String META_CONTACT_XPATH = "/xhtml:html/xhtml:head/xhtml:meta[@name = \""+MOBY_PREFIX_PLACEHOLDER+":contact\"]";
    private final String META_SERVICE_XPATH = "/xhtml:html/xhtml:head/xhtml:meta[@name = \""+MOBY_PREFIX_PLACEHOLDER+":service\"]";
    private final String SERVICE_SCHEME_ATTR = "scheme";
    private final String SERVICE_SPEC_ATTR = "content";

    private final String SERVICE_NAME_PLACEHOLDER = "%SERVICENAME%";
    private final String FORM_XPATH = "/xhtml:html/xhtml:body//xhtml:form[starts-with(@class, \""+MOBY_PREFIX_PLACEHOLDER+":"+
	                                                                       SERVICE_NAME_PLACEHOLDER + ":\")]";
    private final String FORM_PARAM_XPATH = ".//xhtml:input | .//xhtml:textarea | .//xhtml:select | .//button";
    
    static{
        //PG temporarily point to xalan while Google App Engine has bug
	// XPathFactory xPathFactory = XPathFactory.newInstance();
	XPathFactory xPathFactory = new org.apache.xpath.jaxp.XPathFactoryImpl();
	try{
	    xPath = xPathFactory.newXPath();
	    xPath.setNamespaceContext(new CachedNamespaceContextImpl(null, false));
	} catch(Exception e){
            e.printStackTrace();
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try{
            docBuilder = dbf.newDocumentBuilder();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * @param formUrl the location of the remote Web form that will be wrapped into a Moby Service
     */
    public XHTMLForm(URL formUrl) throws Exception{
	this();
	strict = true; //do not allow moby markup of HTML to be missing

	setSpecURL(formUrl);
	parse(formUrl.openStream());
    }

    /**
     * C-tor to use when you want to set a bunch of parameters manually.
     * If parse() is called on the created object, the rules surrounding 
     * required annotation fields are dropped, but manually set values will
     * be refined if they are more lax (e.g. wider int range) than the
     * parsed annotation.
     */
    public XHTMLForm(){
	formFiles = new HashMap<String,List<String>>();
	formEncType = new HashMap<String,String>();
	formAction = new HashMap<String,String>();
	formMethod = new HashMap<String,String>();
	formSubmitOptions = new HashMap<String,Map<String,String>>();
	formImageOptions = new HashMap<String,Map<String,String>>();
	alias2CanonicalServiceName = new HashMap<String,String>();
    }

    /**
     * This method has extended functionality on this class, because the service 
     * can be set using the moby service name (as per XHTML markup), the index of 
     * the form (starting at 0) in the page, the form's HTML name, or the fully qualified action URL.
     * In case any of these are not unique, the order of precedence is as listed above,
     * with the first instance of a form takling precedence over later duplicate names 
     * in the page.
     */
    public void setCurrentService(String serviceToReport) throws IllegalArgumentException{
	if(alias2CanonicalServiceName.containsKey(serviceToReport)){
	    super.setCurrentService(alias2CanonicalServiceName.get(serviceToReport));
	}
	else{
	    super.setCurrentService(serviceToReport);
	}
    }

    /**
     * We run XPaths on the XHTML document, rather than reading line-by-line
     */
    protected void parse(InputStream is) throws Exception{
	parse(docBuilder.parse(is));
    }

    /**
     * Please call setSpecURL before this call so that form URLs are properly dereferencable.
     */
    protected void parse(Document doc) throws Exception{
	xhtmlDoc = doc;

	String mobyPrefix = null;
	NamedNodeMap attrs = xhtmlDoc.getDocumentElement().getAttributes();
	for(int i = 0; i < attrs.getLength(); i++){
	    Attr attr = (Attr) attrs.item(i);
	    if(SadiPrefixResolver.SADI_XML_NAMESPACE.equals(attr.getValue()) &&
	       XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI())){
		mobyPrefix = attr.getLocalName();
		break;
	    }
	}
	if(strict && (mobyPrefix == null || mobyPrefix.trim().length() == 0)){
	    throw new Exception("The declared prefix for the Moby namespace cannot be blank in the document. "+
				"Please insert an attribute in the root html tag such as xmlns:m='"+
				SadiPrefixResolver.SADI_XML_NAMESPACE+"'");
	}

	List<String> serviceNames = parseMetaData(mobyPrefix);
	for(String serviceName: serviceNames){
	    parseForm(mobyPrefix, serviceName);
	}
		
    }

    protected String parseAuthorData(String mobyPrefix) throws Exception{
	
	String contactXPathString = META_CONTACT_XPATH.replaceAll(MOBY_PREFIX_PLACEHOLDER, mobyPrefix);

	// Find the contact info (email contact, or md5 hash of an address for privacy reasons)
	NodeList contactTags = (NodeList) xPath.evaluate(contactXPathString,
							 xhtmlDoc,
							 XPathConstants.NODESET);
	if(contactTags.getLength() == 0){
	    throw new Exception("Could not find any service author tags of the required form '" + 
				contactXPathString + "'");
	}
	if(contactTags.getLength() > 1){
	    throw new Exception("Found multiple (hence ambiguous) service author tags of the form '" + 
				contactXPathString + "'");
	}
	Node contactElement = contactTags.item(0);
	if(!(contactElement instanceof Element)){
	    throw new Exception("The XPath to retrieve the service contact info '" + 
				contactXPathString + "' did not return an element as expected (" +
				"got a " + contactElement.getClass().getName() + " instead)");
	}

	String contactInfo = ((Element) contactElement).getAttribute(SERVICE_SPEC_ATTR);
	if(contactInfo == null || contactInfo.trim().length() == 0){
	    throw new Exception("The service contact info is missing or blank in the HTML meta data headers" +
				" (the XPath used was " + 
				contactXPathString + ")");
	}
	contactInfo = contactInfo.trim();
	// Make sure it's an MD5 hash, or a real (probably qualified) SMTP e-mail format 
	if(!contactInfo.matches("[0-9a-f]{40}") &&
	   !contactInfo.matches("\\S+@\\S+\\.\\S{2,}")){
	    throw new Exception("The value of the service contact info (" + contactInfo +
				" appears to be neither a qualified email address (e.g." +
				" foo@bar.tld), nor an md5 hash of one");
	}
	return contactInfo;
    }

    protected List<String> parseMetaData(String mobyPrefix) throws Exception{
	List<String> serviceNames = new Vector<String>();
	// Automatically assign names 0, 1, 2, etc.. 
	// Moby metadata names, action URLs and specified html form names (if provided)
	// are used as aliases to refer to these numbered services if not in strict mode
	if(!strict){
	    NodeList formTags = xhtmlDoc.getElementsByTagNameNS(SadiPrefixResolver.XHTML_NAMESPACE, "form");
	    for(int i = 0; i < formTags.getLength(); i++){
		// Assign a number to the service.
		// Use the prefix to avoid possible collision between metadata-specified service name and
		// a separate form with the same name as the service.
		serviceNames.add(ANON_SERVICE_PREFIX+i);

		Element formTag = (Element) formTags.item(i);

		String formName = formTag.getAttribute("name");
		if(formName != null && formName.trim().length() != 0 && 
		   !alias2CanonicalServiceName.containsKey(formName)){
		    alias2CanonicalServiceName.put(formName,""+i);
		}

		String action = formTag.getAttribute("action");
		try{  //resolve action to full URL
		    action = (new URL(getSpecURL(), action)).toString(); 
		} catch(Exception e){
		    continue;
		}
		if(!alias2CanonicalServiceName.containsKey(action)){
		    alias2CanonicalServiceName.put(action, ""+i);
		}
	    }
	}

	NodeList serviceTags = (NodeList) xPath.evaluate(META_SERVICE_XPATH.replaceAll(MOBY_PREFIX_PLACEHOLDER, mobyPrefix), 
							 xhtmlDoc, 
							 XPathConstants.NODESET);
	if(strict && serviceTags.getLength() == 0){
	    throw new Exception("Could not find any service metadata tags of the required form '" + 
				META_SERVICE_XPATH.replaceAll(MOBY_PREFIX_PLACEHOLDER, mobyPrefix) + "'");
	}
	for(int i = 0; i < serviceTags.getLength(); i++){
	    Node serviceTag = serviceTags.item(i);
	    if(!(serviceTag instanceof Element)){
		throw new Exception("The XPath for retrieving the service metadata returned something (" +
				    serviceTag.getClass().getName() + ") other than a DOM 'Element' (XPath was '" +
				    META_SERVICE_XPATH.replaceAll(MOBY_PREFIX_PLACEHOLDER, mobyPrefix)+"')");
	    }
	    serviceNames.add(parseServiceTag((Element) serviceTag));
	}

	// There can be only one contact email for the form, so this call is not in the loop
	String author = null;
	try{
	    parseAuthorData(mobyPrefix);
	} catch(Exception e){
	    if(strict){
		throw e;
	    }
	}
	// Do not override manual setting
	if(strict || getContactEmail() == null){
	    setContactEmail(author);
	}

	return serviceNames;
    }

    /**
     * The format of the service meta tag is:
     * <pre>&lt;meta name="moby:service"
     *   scheme="http://moby.central.tld/endpoint"
     *   content="ServiceType,authority.tld,ServiceName: Text description of the service" /&gt;</pre>
     * Where if the scheme attribute is omitted, the default Moby Central endpoint is assumed.
     */
    protected String parseServiceTag(Element serviceElement) throws Exception{
	// scheme = moby central
	String scheme = serviceElement.getAttribute(SERVICE_SCHEME_ATTR);

	String spec = serviceElement.getAttribute(SERVICE_SPEC_ATTR);
	if(spec == null || spec.trim().length() == 0){
	    throw new Exception("Moby Service metadata specification attribute (" + SERVICE_SPEC_ATTR + 
				") cannot be missing or blank");
	}

	String errorPrefix = "The Moby Service metadata attribute (" + SERVICE_SPEC_ATTR + 
	    ") did not have the expected form \"ServiceType,provider.uri.tld,ServiceName:\"" +
	    "Text description of the service\"";
	int firstColonIndex = spec.indexOf(":");
	if(firstColonIndex == -1){
	    throw new Exception(errorPrefix + " (colon missing)");
	}
	String specFields[] = spec.substring(0, firstColonIndex).split(",");
	if(specFields.length != 3){
	    throw new Exception(errorPrefix + " (expected two commas preceding the colon, " +
				"but found " + (specFields.length-1) + ")");
	}
	String serviceTypeName = specFields[0];
	if(serviceTypeName == null || serviceTypeName.trim().length() == 0){
	    throw new Exception(errorPrefix + " (ServiceType was blank)");
	}
	serviceTypeName = serviceTypeName.trim();
	String serviceProviderURI = specFields[1];
	if(serviceProviderURI == null || serviceProviderURI.trim().length() < 4){  // any domain must be at least 4 chars
	    throw new Exception(errorPrefix + " (provider.uri.tld was blank or not valid)");
	}
	serviceProviderURI = serviceProviderURI.trim();
	String serviceName = specFields[2];
	if(serviceName == null || serviceName.trim().length() == 0){
	    throw new Exception(errorPrefix + " (serviceName was blank)");
	}
	serviceName = serviceName.trim();
	String serviceDesc = firstColonIndex < spec.length()-1 ? spec.substring(firstColonIndex+1).trim() : "";

	currentService = serviceName;
	setCentralEndpoint(scheme);
	setServiceType(serviceTypeName);
	setProviderURI(serviceProviderURI);
	setServiceDesc(serviceDesc);

	return serviceName;
    }

    protected void parseForm(String mobyPrefix, String serviceName) throws Exception{

	//could cache the results of the next line but I'm too lazy
	NodeList serviceForms = xhtmlDoc.getElementsByTagNameNS(SadiPrefixResolver.XHTML_NAMESPACE, "form");
	Element serviceFormElement = null;
	if(serviceName.startsWith(ANON_SERVICE_PREFIX)){
	    // The service index number is at the end of the service name
	    serviceFormElement = (Element) serviceForms.item(Integer.parseInt(
							      serviceName.substring(ANON_SERVICE_PREFIX.length())));
	}
	else{
	    String formXPathString = FORM_XPATH.replaceAll(MOBY_PREFIX_PLACEHOLDER, mobyPrefix)
		.replaceAll(SERVICE_NAME_PLACEHOLDER, serviceName);
	    NodeList namedServiceForms = (NodeList) xPath.evaluate(formXPathString,
								   xhtmlDoc, 
								   XPathConstants.NODESET);
	    if(namedServiceForms.getLength() == 0){
		throw new Exception("Could not find the form corresponding to the service '" + serviceName +
				    "' declared in the HTML document metadata.  The XPath tried was '" +
				    formXPathString + "'");
	    }
	    if(!(namedServiceForms.item(0) instanceof Element)){
		throw new Exception("When retrieving the form element corresponding to the service '" +
				    serviceName + "', the XPath result was not a DOM Element as expected (found " +
				    serviceForms.item(0).getClass().getName() + ").  The XPath was '" +
				    formXPathString + "'");
	    }
	    if(namedServiceForms.getLength() > 1){
		throw new Exception("Multiple forms correspond to the service '" + serviceName +
				    "' declared in the HTML document metadata.  I cannot continue until " +
				    "the ambiguity is resolved.  The XPath that retrieved multiple forms was '" +
				    formXPathString + "'");
	    }
	    serviceFormElement = (Element) namedServiceForms.item(0);
	    // if not strict, the numbered service has already been parsed, so reparsing would be redundant.
	    // instead, create an alias
	    if(!strict){
		for(int i = 0; i < serviceForms.getLength(); i++){
		    if(serviceFormElement.isSameNode(serviceForms.item(i))){
			alias2CanonicalServiceName.put(serviceName, ""+i);
			return;
		    }
		}
	    }
	}

	setCurrentService(serviceName);

	// Process the info in the <form...> tag itself
	parseFormTag(serviceFormElement, serviceName, mobyPrefix);

	NodeList formParams = (NodeList) xPath.evaluate(FORM_PARAM_XPATH,
							serviceFormElement, 
							XPathConstants.NODESET);
	if(formParams.getLength() == 0){
	    throw new Exception("Could not find any input fields (" + FORM_PARAM_XPATH +
				") in the for for service " + serviceName);
	}
	for(int i = 0; i < formParams.getLength(); i++){
	    if(!(formParams.item(i) instanceof Element)){
		throw new Exception("The XPath statement " + FORM_PARAM_XPATH + " retrieved a node (" +
				    formParams.item(i).getClass().getName() + ") that was not an Element");
	    }
	}
	// Then process the input fields contained in the form...
	parseFormFields(formParams, serviceName, mobyPrefix);
    }

    // Loops over the form fields then formats the resulting param specs into 
    // the format MobyServlet expects.
    protected void parseFormFields(NodeList formParams, String serviceName, String mobyPrefix) throws Exception{
	Map<String,String> fixed = new HashMap<String,String>();
	Map<String,String> submits = new HashMap<String,String>();
	Map<String,String> images = new HashMap<String,String>();
	Map<String,String[]> inputSpecs = new HashMap<String,String[]>();
	Map<String,String> inputFormats = new HashMap<String,String>();
	Map<String,String[]> secondarySpecs = new HashMap<String,String[]>();
	for(int i = 0; i < formParams.getLength(); i++){
	    Element input = (Element) formParams.item(i);
	    String mobySpec = parseMobySpec(input, mobyPrefix);
	    parseFormField(input, serviceName, mobySpec, inputSpecs, inputFormats, secondarySpecs, fixed, submits, images);
	}
	Map<String,String> inputSpecsAsStrings = new HashMap<String,String>();
	Map<String,String> secondarySpecsAsStrings = new HashMap<String,String>();
	// also check that a Moby parameter isn't defined in more than one way (i.e. name1:String:x:[a,b,c] 
	// as one hash value, then name1:Boolean:true: in another hash value) Actually, 
	// do this in the calling servlet.
	for(Map.Entry<String,String[]> spec: inputSpecs.entrySet()){
	    inputSpecsAsStrings.put(spec.getKey(), join(":", spec.getValue()));
	}
	for(Map.Entry<String,String[]> spec: secondarySpecs.entrySet()){
	    String[] value = spec.getValue();
	    if(value[3] != null && value[3].length() > 0 && !value[3].matches("\\[.*\\]")){
		value[3] = "["+value[3]+"]";
	    }
	    secondarySpecsAsStrings.put(spec.getKey(), join(":", value));
	}

	setPrimaryInputs(inputSpecsAsStrings);
	setPrimaryInputFormats(inputFormats);
	setFixedParams(fixed);
	setSubmitOptions(submits);
	setImageOptions(images);
    }

    /**
     * Turns HTML form elements into Moby service parameter specifications, either through
     * auto-configuration of parameters without Moby specs 
     * (i.e. class="moby:paramName:dataType:defaultValue:dataRange"), or with. 
     */
    protected void parseFormField(Element inputElement, String serviceName, String mobySpec,
				  Map<String,String[]> inputSpecs, 
				  Map<String,String> inputFormats, 
				  Map<String,String[]> secondarySpecs, 
				  Map<String,String> fixed,
				  Map<String,String> submits,
				  Map<String,String> images)
	throws Exception{

	String[] defaultSpec = createDefaultSecondarySpec(inputElement, serviceName);
	if(defaultSpec == null){
	    return; // e.g. reset buttons ignored
	}

	// See if a default value needs to be filled in anywhere
	boolean isRadioDefault = RADIO_DEFAULT_SENTINEL.equals(defaultSpec[3]);
	boolean isRadio = isRadioDefault || RADIO_SENTINEL.equals(defaultSpec[3]);

	// if no spec or type is hidden, interpret as a secondary parameter to auto-configure
	// radios without sopecs are a special case, as they may have real specs
	// in other input fields.
	if(!isRadio && (mobySpec == null || mobySpec.length() == 0)){
	    parseFormFieldDefault(inputElement, serviceName, secondarySpecs, fixed, submits, images);
	    return;  // next param, nothing else to config here...
	}

	// If we get this far, there is a moby spec (we're not just using HTML defaults)
	// -1 means keep trailing blank values in split()
	String[] specFields = mobySpec == null ? new String[4] : mobySpec.split(":", -1);
	for(int j = 0; j < defaultSpec.length && j < specFields.length; j++){
	    if(specFields[j] == null || specFields[j].length() == 0){
		specFields[j] = defaultSpec[j];
	    }
	}
	// Note: checking that datatype is a valid value is up to the user of 
	// this class (e.g. MobyServlet)

	// if spec says to ignore the value as a Moby parameter
	if(specFields.length == 1 && specFields[0].equals(NULL_NAME)){
	    // don't send this value, nor make it part of the moby params
	    // If it is a file type input, remove it from the form file list
	    removeFormFile(defaultSpec[0]);
	    return;
	}
	else if(specFields.length != 3 && specFields.length != 4){
	    throw new Exception("The moby parameter specification for form field \""+
				specFields[0]+"\" (" + mobySpec +
				") did not have any of the expected formats: " +
				"moby:paramName:secondaryType:defaultValue:[value_range], " +
				"moby:paramName:mobyDataType:textformat or moby:null");
	}
	// submit buttons aren't like other params, handle them separately
	else if(defaultSpec[1].equals(SUBMIT_DATATYPE) ||
		defaultSpec[1].equals(IMAGE_DATATYPE)){
	    if(specFields[3] != null && specFields[3].length() > 0 &&
	       !specFields[3].equals("["+specFields[2]+"]")){
		throw new Exception("The moby parameter specification for form submission field \""+
				    specFields[0]+"\" specifies an allowable data range of \"" +
				    specFields[3]+"\", but submission parameters are only allowed " +
				    "fixed values (in this case \"" + specFields[2] + "\"). Please " +
				    "remove the data range parameter in order to avoid a " +
				    "conflicting specification.");
	    }
	    if(specFields[1] != null && specFields[1].length() > 0 &&
	       !specFields[1].equals(SADITags.SADI_STRING)){
		throw new Exception("The moby parameter specification for form submission field \""+
				    specFields[0]+"\" specifies a data type of \"" + specFields[1] +
				    "\", but only " + SADITags.SADI_STRING + " is allowed for " +
				    "submission fields.  Please change the data type accordingly, " +
				    "or leave it blank to use the default."); 
	    }
	    // Use defaultSpec[0] because we cannot explicitly rename submits, as they aren't normal
	    // secondary parameters (i.e. specFields[0] is and should be ignored).  Should we maybe
	    // throw an error if you try to name it?  Maybe in future, if people don't think that's too pedantic...
	    if(specFields[2].length() > 0 &&
	       ((submits.containsKey(defaultSpec[0]) &&
	       !submits.get(defaultSpec[0]).equals(specFields[2])) ||
	       (images.containsKey(defaultSpec[0]) &&
	       !images.get(defaultSpec[0]).equals(specFields[2])))){
		throw new Exception("The moby parameter specification for form submission/image field \""+
				    (defaultSpec[0].equals(SUBMIT_ANONYMOUS_NAME) ||
				     defaultSpec[0].equals(IMAGE_ANONYMOUS_NAME) ? "[anonymous]" : defaultSpec[0]) +
				    "\" is ambiguous because its value is defined more than once (\"" +
				    specFields[2] + "\" and \"" + 
				    (images.containsKey(defaultSpec[0]) ? images.get(defaultSpec[0]) : 
				     submits.get(defaultSpec[0])) + "\")");
	    }
	    if(defaultSpec[1].equals(IMAGE_DATATYPE)){
		// parse the specified fixed moby param value as x,y coord
		if(!specFields[2].matches("^\\d+,\\d+$")){
		    throw new Exception("The moby parameter specification for form image field \""+
					(defaultSpec[0].equals(IMAGE_ANONYMOUS_NAME) ? "[anonymous]" : defaultSpec[0]) +
					"\" is not of expected form \"x,y\" (i.e. you must specify " +
					"fixed, non-negative mouse-click coordinates for Moby-based image form " +
					"submission such as \"moby:::0,0:\", but you provided \"" +
					mobySpec + "\")");
		}
		String[] coords = specFields[2].split(",");
		// Note: we don't check that the numbers make sense (since that would mean
		// knowing the image size whether explicitly stated or not), but the
		// regex ensures they at least aren't negative...

		// specFields[2] may be blank, which means no fixed value will be submitted
		images.put(defaultSpec[0], defaultSpec[2]);  
		// if anon, set vars "x" and "y"
		if(defaultSpec[0].equals(IMAGE_ANONYMOUS_NAME)){
		    images.put("x", coords[0]);
		    images.put("y", coords[1]);		
		}
		// else set imageParamName.x and imageParamName.y
		else{
		    images.put(defaultSpec[0]+".x", coords[0]);
		    images.put(defaultSpec[0]+".y", coords[1]);	
		}
	    }
	    // else its a submit data type
	    else{
		// put(submit field name, submit field value) 	    	    
		submits.put(specFields[0], specFields[2]);
	    }
	}
	// if spec says this is a primary param (i.e. only three args in spec)
	else if(specFields.length == 3){
	    if(inputSpecs.containsKey(defaultSpec[0]) || 
	       secondarySpecs.containsKey(defaultSpec[0])){
		throw new Exception("The input name \""+defaultSpec[0]+
				    "\" is used more than once in the form defining service \"" + 
				    serviceName + "\"");
	    }
	    // Cleave off the last array item, the text format, and
	    // put it in another hash, as this is not part of the spec
	    // MobyServlet recognizes.
	    inputFormats.put(defaultSpec[0], specFields[specFields.length-1]);
	    String[] mobyServletSpecFields = new String[specFields.length-1];
	    for(int i = 0; i < mobyServletSpecFields.length; i++){
		mobyServletSpecFields[i] = specFields[i];
	    }
	    inputSpecs.put(defaultSpec[0], mobyServletSpecFields);
	}
	// else it's a secondary, maybe with a fixed value (in which case 
	// it's set aside specially in fixedParams)
	else{
	    if(isRadio){
		// It has to be an enumerated String...
		// The only thing you can really do with a radio button
		// us change its name in Moby, or fix its value.  Any range spec is ignored.
		if(inputSpecs.containsKey(defaultSpec[0])){
		    throw new Exception("The input name \""+specFields[0]+
					"\" is already used to defined a primary " +
					" parameter, but is being specified as a " +
					"radio button name too");
		}
		
		// handle the case where a radio param is being hardcoded
		// Note: for now, there is no way to specify a default value other 
		// than the HTML default without just fixing the radio value.
		if(fixed.containsKey(defaultSpec[0])){
		    if(specFields[2] != null && specFields[2].length() > 0){
			throw new Exception("The radio parameter \"" + defaultSpec[0] +
					    "\" has been assigned more than one fixed value (" +
					    specFields[2] + " and " + fixed.get(defaultSpec[0]) + ")");
		    }
		    // else: ignore any other radio value, we're sticking with the fixed value 
		}
		// Did the user manually set a fixed value for the radio button?
		else if(specFields[2] != null && specFields[2].length() > 0 && 
			!specFields[2].equals(defaultSpec[2])){
		    // first time we're fixing the radio param value to send
		    fixed.put(defaultSpec[0], specFields[2]);
		    secondarySpecs.remove(defaultSpec[0]); //in case we've encountered the radio earlier
		}
		else if(secondarySpecs.containsKey(defaultSpec[0])){
		    String[] existingSpec = secondarySpecs.get(defaultSpec[0]);
		    // see if the radio name has already been changed in the doc.  
		    // if the new spec renames the radio too, we've got an ambiguous spec
		    if(!existingSpec[0].equals(defaultSpec[0]) &&
		       !existingSpec[0].equals(specFields[0]) &&
		       !specFields[0].equals(defaultSpec[0])){
			throw new Exception("The radio button group named in " +
					    "the HTML doc as \"" + defaultSpec[0] + 
					    "\" is ambiguously renamed by different " +
					    "Moby specs as \"" + existingSpec[0] + 
					    "\" and \"" + specFields[0]);
		    }
		    // or we're renaming for the first time
		    else if(existingSpec[0].equals(defaultSpec[0]) && !existingSpec[0].equals(specFields[0])){
			existingSpec[0] = specFields[0];
		    }
		    // otherwise it's the default name maintained

		    if(isRadioDefault){  //we've been told this item is the default value 
			existingSpec[2] = specFields[2];
		    }

		    // add the new value to the enumeration of existing param value choices
		    existingSpec[3] += "," + specFields[2];
		    return;
		}
		// else it's the first time we're encountering this radio param
		else{
		    specFields[3] = specFields[2]; // override sentinel with actual value
		    secondarySpecs.put(defaultSpec[0], specFields);
		}
		return;
	    }//end isRadio
	    else if(inputSpecs.containsKey(defaultSpec[0]) ||
		    secondarySpecs.containsKey(defaultSpec[0]) ||
		    fixed.containsKey(defaultSpec[0])){
		throw new Exception("The parameter name \"" + defaultSpec[0] +
				    "\" is used more than once in the form " +
				    "defining the service \"" + serviceName + "\"");
	    }
	    else if(secondarySpecs.containsKey(specFields[0])){
		// Ensure that if a secondary with the same name is used more than once,
		// the spec is exactly the same.
		String[] existingSpec = secondarySpecs.get(defaultSpec[0]);
		for(int i = 0; i < existingSpec.length; i++){
		    // Did the user specify a value for the spec, 
		    // and is it different from the existing one?
		    if(!existingSpec[i].equals(specFields[i]) && !specFields[i].equals(defaultSpec[i])){
			throw new Exception("The definition of " + defaultSpec[0] +
					    " redefines the moby secondary parameter " +
					    existingSpec[0] +
					    ".  If you want to use a moby secondary for " +
					    "multiple form fields, the specs must concur, or " +
					    "spec fields after the first one in the form " +
					    "must be left blank.");
		    }
		}
	    }

	    // By logic, a non-blank specFields[3] must match the form "[...]" if we got here
	    if(specFields[3] != null && specFields[3].length() > 0){
		String[] rangeValues = specFields[3].split(",");
		if(rangeValues.length == 1){  //fixed value of form moby:name:anyType:[value]
		    fixed.put(defaultSpec[0], rangeValues[0]);
		    return;
		}
	    }
		
	    // We won't check if the range data is okay, those types of logic errors
	    // must be caught by the calling class (some derivative probably of MobyServlet)
	    secondarySpecs.put(defaultSpec[0], specFields);
	}
    }

    /**
     * Autoconfigures a secondary Moby service param based on the HTML spec for a form field.
     */
    protected void parseFormFieldDefault(Element inputElement, String serviceName, 
					 Map<String,String[]> secondarySpecs,
					 Map<String,String> fixed,
					 Map<String,String> submits,
					 Map<String,String> images)
	throws Exception{

	String[] defaultSpec = createDefaultSecondarySpec(inputElement, serviceName);
	if(defaultSpec == null){
	    return;  // e.g. reset button
	}

	// The if/elses below direct the params accordingly depending on if 
	// the param is an image, a submit, a hidden, or other
	if(SUBMIT_DATATYPE.equals(defaultSpec[1])){
	    if(defaultSpec[2].equals(submits.get(defaultSpec[0]))){
		// TODO: how do we handle multiple submits with the same name but different values??
		System.err.println("Overriding submit with same name but with new different value: " + 
				   defaultSpec[0] + ", " + defaultSpec[2]);		
	    }
	    submits.put(defaultSpec[0], defaultSpec[2]);
	}
	else if(IMAGE_DATATYPE.equals(defaultSpec[1])){
	    if(defaultSpec[2].equals(images.get(defaultSpec[0]))){
		// TODO: how do we handle multiple submits with the same name but different values??
	    }
	    images.put(defaultSpec[0], defaultSpec[2]);
	}
	else if(secondarySpecs.containsKey(defaultSpec[0])){
	    // Radio buttons are a funny case where the spec is 
	    // spread over multiple input elements.
	    if(RADIO_SENTINEL.equals(defaultSpec[3]) ||
	       RADIO_DEFAULT_SENTINEL.equals(defaultSpec[3])){
		String[] existingSpec = secondarySpecs.get(defaultSpec[0]);
		// append the value to the existing radio param value enumeration
		existingSpec[3] += ","+defaultSpec[2];
		if(RADIO_DEFAULT_SENTINEL.equals(defaultSpec[3])){
		    // we've been told this item is the default value for the radio
		    existingSpec[2] = defaultSpec[2];
		}
	    }
	    else{
		throw new Exception("A non-radio parameter name (" + defaultSpec[0] + 
				    ") is repeated in the form for service '" + 
				    serviceName + "'");
	    }
	}
	else if(fixed.containsKey(defaultSpec[0])){
	    throw new Exception("A non-radio parameter name (" + defaultSpec[0] + 
				") is repeated in the form for service '" + 
				serviceName + "'");
	}
	else if(HIDDEN_SENTINEL.equals(defaultSpec[3])){
	    fixed.put(defaultSpec[0], defaultSpec[2]);
	}
	else{
	    if(RADIO_SENTINEL.equals(defaultSpec[3]) ||
	       RADIO_DEFAULT_SENTINEL.equals(defaultSpec[3])){
		defaultSpec[3] = defaultSpec[2]; // override sentinel with actual value
	    }
	    secondarySpecs.put(defaultSpec[0], defaultSpec);
	}
    }
    
    protected void parseFormTag(Element serviceFormElement, String serviceName, String mobyPrefix) throws Exception{

	// The output datatype of the service is also declared in the form tag
	List<String> outputSpecs = parseMobySpecs(serviceFormElement, mobyPrefix);
	Map<String,String> cleanOutputSpecs = new HashMap<String,String>();
	for(int i = 0; i < outputSpecs.size(); i++){
	    String spec = outputSpecs.get(i);
	    if(!outputSpecs.get(i).startsWith(serviceName+":")){
		throw new Exception("The form for service '" + serviceName +
				    "' also contains moby specs (" + spec  + 
				    ") not of the required form '"+mobyPrefix+":"+
				    serviceName+":paramName:DataType'.  " +
				    "You can only specify one service per form.");
	    }
	    if(spec.length() < serviceName.length()+4){
		throw new Exception("The form for service '" + serviceName +
				    "' contains moby specs (" + spec + 
				    ") not of the required form '"+mobyPrefix+":"+
				    serviceName+":paramName:DataType'");
	    }
	    // The outputs have no existing names in the HTML/text/etc. output, 
	    // so enforce that they be the same as the Moby param names 
	    // (i.e. the stuff before the first colon in the moby param spec)
	    String outParam = spec.substring(serviceName.length()+1);
	    if(!outParam.contains(":")){
		throw new Exception("The form for service '" + serviceName +
				    "' contains moby specs (" + spec + 
				    ") not of the required form '"+mobyPrefix+":"+
				    serviceName+":paramName:DataType' (last colon missing)");
	    }
	    cleanOutputSpecs.put(outParam.substring(0, outParam.indexOf(":")), outParam);
	}

	setPrimaryOutputs(cleanOutputSpecs);
	String encType = URLENCODED;  // This is the default XHTML value
	setFormEncodingType(encType);

	String action = serviceFormElement.getAttributeNS(SadiPrefixResolver.XHTML_NAMESPACE, "action");
	if(action == null || action.length() == 0){
	    action = serviceFormElement.getAttribute("action");
	}	
	if(action == null){
	    throw new Exception("Could not find the \"action\" attribute for the " +
				"form describing service " + serviceName); 
	}
	setFormAction(action);

	String method = serviceFormElement.getAttributeNS(SadiPrefixResolver.XHTML_NAMESPACE, "method");
	if(method == null || method.length() == 0){
	    method = serviceFormElement.getAttribute("method");
	}	
	if(method == null){
	    setFormMethod(METHOD_GET); // in accordance with XHTML spec
	}
	else if(method.trim().toUpperCase().equals(METHOD_POST.toUpperCase())){
	    setFormMethod(METHOD_POST);
	}
	else if(method.trim().toUpperCase().equals(METHOD_GET.toUpperCase())){
	    setFormMethod(METHOD_GET);
	}
	else{
	    throw new Exception("The \"method\" attribute (" + method + ") for the" +
				" form describing service " + serviceName + 
				" was neither missing (which receives a default)," +
				" nor one of the acceptable " +
				" values: \"" + METHOD_GET +"\", \"" + METHOD_POST + "\""); 
	}
    }

    protected String parseMobySpec(Element element, String mobyPrefix) throws Exception{
	List<String> specs = parseMobySpecs(element, mobyPrefix);
	if(specs == null || specs.size() == 0){
	    return null;
	}
	else if(specs.size() == 1){
	    return specs.get(0);
	}
	else{
	    throw new Exception("Expected a single Moby parameter specification, " +
				"but there were " + specs.size());
	}
    }

    protected List<String> parseMobySpecs(Element element, String mobyPrefix){
	List<String> mobySpecs = new Vector<String>();
	// parse class attribute
	String classSpecs = element.getAttributeNS(SadiPrefixResolver.XHTML_NAMESPACE, "class");
	if(classSpecs == null || classSpecs.length() == 0){
	    classSpecs = element.getAttribute("class");
	}
	if(classSpecs == null){
	    return mobySpecs;  //no html class attribute at all
	}
	for(String classSpec: classSpecs.split("\\s")){
	    String[] classParts = classSpec.split(":");
	    if(classParts.length > 1 && classParts[0].equals(mobyPrefix)){
		mobySpecs.add(classSpec.substring(mobyPrefix.length()+1));
	    }
	}

	return mobySpecs;
    }

    // spec is new String[]{name, dataType, defaultValue, range} w/ special range value to denote a checkbox or hidden
    // guarantees that dataType and name have values
    protected String[] createDefaultSecondarySpec(Element inputElement, String serviceName)
	throws Exception{

	String elementType = inputElement.getLocalName().toLowerCase();
	String fieldType = inputElement.getAttributeNS(SadiPrefixResolver.XHTML_NAMESPACE, "type");
	if(fieldType == null || fieldType.length() == 0){
	    fieldType = inputElement.getAttribute("type");
	}
	if(fieldType != null){
	    fieldType = fieldType.toLowerCase();
	}

	// check name attr for moby param label
	String nameAttr = inputElement.getAttributeNS(SadiPrefixResolver.XHTML_NAMESPACE, "name");
	if(nameAttr == null || nameAttr.trim().length() == 0){
	    nameAttr = inputElement.getAttribute("name");
	}
	if(nameAttr == null || nameAttr.trim().length() == 0){
	    // for the following, the name is optional
	    if("submit".equals(fieldType) || "reset".equals(fieldType) ||
	       "image".equals(fieldType) || "button".equals(fieldType) ||
	       "button".equals(elementType)){
		nameAttr = fieldType; // anonymous on submission
	    }
	    else{
		throw new Exception("The attribute \"name\" was missing in an input field for service " + 
				serviceName);
	    }
	}

	// check alt attr for moby param description
	String descAttr = inputElement.getAttributeNS(SadiPrefixResolver.XHTML_NAMESPACE, "alt");
	if(descAttr == null || descAttr.length() == 0){
	    descAttr = inputElement.getAttribute("alt");
	}

	// check for default value
	String valueAttr = inputElement.getAttributeNS(SadiPrefixResolver.XHTML_NAMESPACE, "value");
	if(valueAttr == null || valueAttr.length() == 0){
	    valueAttr = inputElement.getAttribute("value");
	}

	// type devination based on input type, default value
	String dataType = SADITags.SADI_STRING;
	String range = "";

	boolean isChecked = false;
	String checked = inputElement.getAttribute("checked");
	if(checked != null && checked.length() > 0 && !"0".equals(checked) && !"false".equals(checked)){
	    isChecked = true;
	}

	if("select".equals(elementType)){
	    Map<String,Boolean> options = parseSelectField(inputElement, nameAttr);
	    for(Map.Entry<String,Boolean> option: options.entrySet()){
		String optionName = option.getKey().replaceAll(",", "\\,");
		if(option.getValue() == true){
		    valueAttr = optionName; // set the default
		}
		if(range.length() == 0){
		    range = optionName;
		}
		else{
		    range += "," + optionName;
		}
	    }
	}
	else if("textarea".equals(elementType)){
	    if(valueAttr == null || valueAttr.length() == 0){
		valueAttr = inputElement.getTextContent();
	    }
	    if(valueAttr == null){
		valueAttr = "";
	    }
	}
	else if("button".equals(elementType) || "button".equals(fieldType)){
	    // do nothing, these are for javascript event launching which we obviously don't support
	    return null;
	}
	else if(fieldType == null || fieldType.length() == 0){
	    throw new Exception("The attribute \"type\" was missing for input field \"" + 
				nameAttr + "\" for service " + serviceName);
	}
	else if("reset".equals(fieldType)){
	    // do nothing
	    return null;
	}
	else if("checkbox".equals(fieldType)){
	    dataType = SADITags.SADI_BOOLEAN;
	    valueAttr = ""+isChecked;
	}
	else if("radio".equals(fieldType)){
	    range = isChecked ? RADIO_DEFAULT_SENTINEL : RADIO_SENTINEL;
	}
	else if("image".equals(fieldType)){
	    // an image input is actually a submit button of sorts
	    // images, like submits, will need to be handled specially by the caller
	    dataType = IMAGE_DATATYPE;
	    if("image".equals(nameAttr)){
		// submit button with default name assigned above doesn't add to parameters,
		// use a special sentinel to denote this
		nameAttr = IMAGE_ANONYMOUS_NAME;
	    }
	}
	else if("submit".equals(fieldType)){
	    if(valueAttr == null || valueAttr.length() == 0){
		valueAttr = SUBMIT_DEFAULT_VALUE;
	    }
	    if("submit".equals(nameAttr)){
		// submit button with default name assigned above doesn't add to parameters,
		// use a special sentinel to denote this
		nameAttr = SUBMIT_ANONYMOUS_NAME;
	    }
	    // submits with names will need to be handled specially by the caller
	    dataType = SUBMIT_DATATYPE;
	}
	else if("hidden".equals(fieldType)){
	    range = HIDDEN_SENTINEL;
	}
	else if(valueAttr == null){
	    valueAttr = "";
	}
	else{  //i.e. text, password, file
	    if("file".equals(fieldType)){
		addFormFile(nameAttr);
	    }

	    if(valueAttr != null && valueAttr.length() > 0){
		
		// Try to parse the default value multiple ways
		dataType = SADITags.SADI_INTEGER;
		
		try{new BigInteger(valueAttr);
		}catch(Exception e){
		    dataType = SADITags.SADI_FLOAT;}
		
		try{new BigDecimal(valueAttr);
		}catch(Exception e){
		    dataType = SADITags.SADI_DATETIME;}
		
		try{XHTMLForm.parseISO8601(valueAttr);
		}catch(Exception e){
		    dataType = SADITags.SADI_STRING;}
	    }
	    //else we keep the default of MobyTags.MOBYSTRING
	}
	
	return new String[]{nameAttr, dataType, valueAttr, range};
    }

    /**
     * @return a map of <option value, is default select>
     */
    public Map<String,Boolean> parseSelectField(Element selectElement, String selectName) throws Exception{
	NodeList options = selectElement.getElementsByTagNameNS(SadiPrefixResolver.XHTML_NAMESPACE, 
								"option");
	if(options == null || options.getLength() == 0){
	    options = selectElement.getElementsByTagName("option");
	}
	if(options == null || options.getLength() == 0){
	    throw new Exception("Could not find any options for select input field \"" + selectName +"\"");
	}
	
	Map<String,Boolean> optionsMap = new LinkedHashMap<String,Boolean>(); // maintains insertion-order on iteration
	for(int i = 0; i < options.getLength(); i++){
	    Element option = (Element) options.item(i);
	    String optionVal = option.getAttributeNS(SadiPrefixResolver.XHTML_NAMESPACE, "value");
	    if(optionVal == null || optionVal.length() == 0){		
		optionVal = option.getAttribute("value");
	    }
	    if(optionVal == null || optionVal.length() == 0){
		optionVal = option.getTextContent();
	    }
	    if(optionVal == null){
		optionVal = "";
	    }
	    if(optionsMap.containsKey(optionVal)){
		throw new Exception("The value \"" + optionVal + "\" is repeated in the select field \"" + 
				    selectName + "\", perhaps I'm parsing the enumeration incorrectly?  " +
				    "If the option value is actually repeated, please manually specify the parameter " +
				    "for moby using the <select class=\"moby:"+selectName+
				    ":String:defaultValue:[option1,option2...]\"> syntax");
	    }

	    String selAttr = option.getAttributeNS(SadiPrefixResolver.XHTML_NAMESPACE, "selected");
	    if(selAttr == null || selAttr.length() == 0){
		selAttr = option.getAttribute("selected");
	    }
	    if(selAttr != null && selAttr.length() > 0 && !"0".equals(selAttr) && !"false".equals(selAttr)){
		optionsMap.put(optionVal, true);
	    }
	    else{
		optionsMap.put(optionVal, false);
	    }
	}
	return optionsMap;
    }

    // Note that the map is not cloned...
    public void setSubmitOptions(Map<String,String> options){
	formSubmitOptions.put(currentService, options);
    }

    /**
     * Lists the named submit buttons, and may contain a special key SUBMIT_ANONYMOUS_NAME
     * that denotes that an unnamed submit button can be used. Note that the key sets for 
     * getSubmitOptions() and getImageOptions() are disjoint.
     */
    public Map<String,String> getSubmitOptions(){
	return formSubmitOptions.get(currentService);
    }
   
    // Note that the map is not cloned...
    public void setImageOptions(Map<String,String> options){
	formImageOptions.put(currentService, options);
    }

    /**
     * Lists the named submit buttons, and may contain a special key IMAGE_ANONYMOUS_NAME
     * that denotes that an unnamed submit button can be used.  Note that the key sets for 
     * getSubmitOptions() and getImageOptions() are disjoint.
     */
    public Map<String,String> getImageOptions(){
	return formImageOptions.get(currentService);
    }
   
    /**
     * @param encType either constant MULTIPART or URLENCODED
     */
    public void setFormEncodingType(String encType){
	formEncType.put(currentService, encType);
    }

    /**
     * @return either constant MULTIPART or URLENCODED
     */
    public String getFormEncodingType(){
	return formEncType.get(currentService);
    }

    /**
     * @param action the value of the form's action attribute
     */
    public void setFormAction(String action){
	formAction.put(currentService, action);
    }

    public String getFormAction(){
	return formAction.get(currentService);
    }

    /**
     * @param method the value of the form's method attribute (i.e. GET or POST)
     */
    public void setFormMethod(String method){
	formMethod.put(currentService, method);
    }

    public String getFormMethod(){
	return formMethod.get(currentService);
    }

    public void addFormFile(String paramName){
	if(!formFiles.containsKey(currentService)){
	    formFiles.put(currentService, new Vector<String>());
	}
	formFiles.get(currentService).add(paramName);
    }

    public void removeFormFile(String paramName){
	if(currentService != null && formFiles.containsKey(currentService)){
	    formFiles.get(currentService).remove(paramName);
	}
    }

    /**
     * @return the list of input parameters (primary and/or secondary) that should be submitted in "file" style
     */
    public List<String> getFormFiles(){
	return formFiles.get(currentService);
    }

    public static String join(String delim, String[] array){
        StringBuffer sb = join(delim, array, new StringBuffer());
        return sb.toString();
    }

    public static StringBuffer join(String delim, String[] array, StringBuffer sb){
        for(int i = 0; i < array.length; i++) {
            if(i != 0){
		sb.append(delim);
	    }
            sb.append(array[i]);
        }
        return sb;		
    }
    public static GregorianCalendar parseISO8601(String dateTime) throws IllegalArgumentException{
	// null = request for current date and time
	if(dateTime == null){
	    return new GregorianCalendar();
	}

	// YYYY-MM-DDThh:mm:ss.sTZD
	StringTokenizer st = new StringTokenizer(dateTime, "-T:.+Z", true);

	GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
	calendar.clear();
	try {
	    // Year
	    if (st.hasMoreTokens()) {
		int year = Integer.parseInt(st.nextToken());
		calendar.set(Calendar.YEAR, year);
	    } else {
		return calendar;
	    }
	    // Month
	    if (check(st, "-") && (st.hasMoreTokens())) {
		int month = Integer.parseInt(st.nextToken()) -1;
		calendar.set(Calendar.MONTH, month);
	    } else {
		return calendar;
	    }
	    // Day
	    if (check(st, "-") && (st.hasMoreTokens())) {
		int day = Integer.parseInt(st.nextToken());
		calendar.set(Calendar.DAY_OF_MONTH, day);
	    } else {
		return calendar;
	    }
	    // Hour
	    if (check(st, "T") && (st.hasMoreTokens())) {
		int hour = Integer.parseInt(st.nextToken());
		calendar.set(Calendar.HOUR_OF_DAY, hour);
	    } else {
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	    }
	    // Minutes
	    if (check(st, ":") && (st.hasMoreTokens())) {
		int minutes = Integer.parseInt(st.nextToken());
		calendar.set(Calendar.MINUTE, minutes);
	    } else {
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	    }

	    // Seconds available?
	    if (! st.hasMoreTokens()) {
		return calendar;
	    }
	    String tok = st.nextToken();
	    if (tok.equals(":")) { // seconds
		if (st.hasMoreTokens()) {
		    int secondes = Integer.parseInt(st.nextToken());
		    calendar.set(Calendar.SECOND, secondes);
		    if (! st.hasMoreTokens()) {
			return calendar;
		    }
		    // fractions of a sec
		    tok = st.nextToken();
		    if (tok.equals(".")) {
			// bug fixed, thx to Martin Bottcher
			String nt = st.nextToken();
			while(nt.length() < 3) {
			    nt += "0";
			}
			nt = nt.substring( 0, 3 ); //Cut trailing chars..
			int millisec = Integer.parseInt(nt);
			calendar.set(Calendar.MILLISECOND, millisec);
			if (! st.hasMoreTokens()) {
			    return calendar;
			}
			tok = st.nextToken();
		    } else {
			calendar.set(Calendar.MILLISECOND, 0);
		    }
		} else {
		    throw new IllegalArgumentException("No seconds specified");
		}
	    } else {
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
	    }
	    // Timezone
	    if (! tok.equals("Z")) { // UTC
		if (! (tok.equals("+") || tok.equals("-"))) {
		    throw new IllegalArgumentException("only Z, + or - allowed");
		}
		boolean plus = tok.equals("+");
		if (! st.hasMoreTokens()) {
		    throw new IllegalArgumentException("Missing hour field in timezone offset");
		}
		String tzhour = st.nextToken();
		String tzmin  = "00";
		if (check(st, ":") && (st.hasMoreTokens())) {
		    tzmin = st.nextToken();
		} else {
		    throw new IllegalArgumentException("Missing minute field in timezone offset");
		}
		if (plus) {
		    calendar.setTimeZone(TimeZone.getTimeZone("GMT+"+tzhour+":"+tzmin));
		    calendar.add(Calendar.HOUR, Integer.parseInt(tzhour));
		    calendar.add(Calendar.MINUTE, Integer.parseInt(tzmin));
		    calendar.set(Calendar.DST_OFFSET, 0);  //ISO8601 does not deal with DST
		} else {
		    calendar.setTimeZone(TimeZone.getTimeZone("GMT-"+tzhour+":"+tzmin));
		    calendar.add(Calendar.HOUR, Integer.parseInt("-"+tzhour));
		    calendar.add(Calendar.MINUTE, Integer.parseInt("-"+tzmin));
		    calendar.set(Calendar.DST_OFFSET, 0);
		}
	    }
	} catch (NumberFormatException ex) {
	    throw new IllegalArgumentException("["+ex.getMessage()+ "] is not an integer");
	}
	return calendar;
    }
    private static boolean check(StringTokenizer st, String token) throws IllegalArgumentException{
	try {
	    if (st.nextToken().equals(token)) {
		return true;
	    } else {
		throw new IllegalArgumentException("Missing ["+token+"]");
	    }
	} catch (NoSuchElementException ex) {
	    return false;
	}
    }
}
