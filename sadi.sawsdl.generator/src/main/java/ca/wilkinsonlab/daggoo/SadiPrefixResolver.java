package ca.wilkinsonlab.daggoo;

import org.apache.xml.utils.PrefixResolver;
import org.w3c.dom.*;
import java.util.Vector;

/**
 * The main purpose of this class is to provide default mapping from 
 * prefices we will use in the XPath statements built in the various 
 * client classes to URI's of XML namespaces.  For example, if one wanted to
 * construct a XPath statement looking for queryResponse elements from
 * the SADI namespace, there is no direct mechanism to specify this in
 * the XPath.  One can instead compile an XPath statement such as "//moby:queryResponse"
 * and use this PrefixResolver, that defines the "moby" prefix as pointing to the 
 * SADI XML Namespace.  Otherwise the "//moby:queryResponse" will not match the document
 * unless the document happens to use "moby:" as that Namespace's prefix.
 *
 * Using this class is the only way to compile XPaths that are independent
 * of the namespace declarations in the document (since those are the default ones
 * used by XPath evaluators).
 *
 * NOTE: the methods of this class rely on the DOM being parsed with a namespace-aware
 * parser!
 */

public class SadiPrefixResolver implements PrefixResolver{
    public static final String SADI_XML_NAMESPACE = "http://sadiframework.org/sadi";
    public static final String SADI_XML_NAMESPACE_INVALID = "http://www.biomoby.org/moby-s";
    public static final String SADI_XML_PREFIX = "sadi";
    public static final String SADI_TRANSPORT_NAMESPACE = "http://sadiframework.org/";
    public static final String SADI_TRANSPORT_PREFIX = "mobyt";
    public static final String XSI_NAMESPACE1999 = "http://www.w3.org/1999/XMLSchema-instance";
    public static final String XSI_NAMESPACE2001 = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String XSI_PREFIX = "xsi";
    public static final String XSI1999_PREFIX = "xsi1999";
    public static final String XSI2001_PREFIX = "xsi2001";
    public static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
    public static final String XSD_PREFIX = "xsd";
    public static final String SOAP_ENC_NAMESPACE ="http://schemas.xmlsoap.org/soap/encoding/";
    public static final String SOAP_ENC_PREFIX ="soap-enc";
    public static final String SOAP_NAMESPACE ="http://schemas.xmlsoap.org/wsdl/soap/";
    public static final String SOAP_PREFIX ="soap";
    public static final String WS_ADDRESSING_PREFIX = "wsa";
    public static final String WS_ADDRESSING_NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/03/addressing";
    public static final String WSRP_PREFIX = "wsrp";
    public static final String WSRP_NAMESPACE = "http://docs.oasis-open.org/wsrf/rp-2";
    public static final String XHTML_PREFIX = "xhtml";
    public static final String XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml";
    public static final String XLINK_PREFIX = "xlink";
    public static final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";
    public static final String WSDL_PREFIX = "wsdl";
    public static final String WSDL_NAMESPACE = "http://schemas.xmlsoap.org/wsdl/";
    public static final String WSDL20_PREFIX = "wsdl20";
    public static final String WSDL20_NAMESPACE = "http://www.w3.org/2006/01/wsdl";
    public static final String HTTP_PREFIX = "http";
    public static final String HTTP_NAMESPACE = "http://schemas.xmlsoap.org/wsdl/http/";
    public static final String SAWSDL_PREFIX = "sawsdl";
    public static final String SAWSDL_NAMESPACE = "http://www.w3.org/ns/sawsdl";
    public static final String LSID_PREFIX = "lsid";
    public static final String LSID_NAMESPACE = "http://lsid.omg.org/predicates#";
    public static final String DUBLIN_CORE_PREFIX = "dc";
    public static final String DUBLIN_CORE_NAMESPACE = "http://purl.org/dc/elements/1.1/";

    /**
     * We don't really implement this as it can be extremely complicated.
     *
     * @return null, so don't expect to resolve relative URI's in your XPaths
     */
    public String getBaseIdentifier(){
	return null;
    }

    /**
     * Convenience method that matches attributes in the SADI namespace or, for now
     * at least, with no declared namespace.
     * @return the value of the attribute, or null if the attribute does not exist
     */
    public static String getAttr(org.w3c.dom.Element e, String attrName){
	String value = e.getAttributeNS(SADI_XML_NAMESPACE, attrName);
	if(value == null || "".equals(value)){
	    value = e.getAttributeNS(SADI_XML_NAMESPACE_INVALID, attrName);
	}
	if(value == null || "".equals(value)){
	    value = e.getAttributeNS(null, attrName);
	}
	return value;
    }

    /**
     * Convenience method that matches attributes in the SADI namespace or, for now
     * at least, with no declared namespace.
     * @return the value of the attribute, or null if the attribute does not exist
     */
    public static org.w3c.dom.Element getChildElement(org.w3c.dom.Element e, String elementName){
	NodeList list = getChildElements(e, elementName);
	if(list.getLength() == 0){
	    return null;
	}
	else{
	    return (org.w3c.dom.Element) list.item(0);
	}
    }

    /**
     * Convenience method that matches child elements in the SADI namespace or, for now
     * at least, with no declared namespace.  "*" acts as a wildcard.
     * @return the list of children, or null if either the element or name is null does not exist
     */
    public static NodeList getChildElements(org.w3c.dom.Element e, String elementName){
	if(elementName == null || e == null){
	    return null;
	}
	MobyNodeList matches = new MobyNodeList();

	NodeList children = e.getChildNodes();
	for(int j = 0; children != null && j < children.getLength(); j++){
	    // Make sure it's an element, not a processing instruction, text, etc.
	    if(!(children.item(j) instanceof Element)){
		continue;
	    }
	    Element child = (Element) children.item(j);
	    // Make sure it has the right name, or wildcard
	    if(!elementName.equals("*") && !elementName.equals(child.getLocalName())){
		continue;
	    }
	    // Make sure it's in the SADI namespace, or no namespace at all
	    String uri = child.getNamespaceURI();
	    if(uri != null && uri.length() != 0 && !uri.equals(SADI_XML_NAMESPACE) &&
	       !uri.equals(SADI_XML_NAMESPACE_INVALID)){
		continue;
	    }

	    // Everything looks good
	    matches.add(child);
	}
	return matches;
    }

    public static class MobyNodeList implements NodeList{
	private Vector<Node> nodes;
	public MobyNodeList(){nodes = new Vector<Node>();}
	public int getLength(){return nodes.size();}
	public Node item(int index){return nodes.elementAt(index);}
	public void add(Node n){nodes.add(n);}
    };

    /**
     * @return For now, default SADI and XML Schema Instance (1999) prefices as defined in the class static variables
     */
    public String getNamespaceForPrefix(java.lang.String prefix){
	if(prefix == null || "".equals(prefix)){
	    return "";
	}
	else if(SADI_XML_PREFIX.equals(prefix)){
	    return SADI_XML_NAMESPACE;
	}
	else if(SADI_TRANSPORT_PREFIX.equals(prefix)){
	    return SADI_TRANSPORT_NAMESPACE;
	}
	else if(XSI_PREFIX.equals(prefix)){
	    return XSI_NAMESPACE1999;  //Crappy Perl XML Schema namespace usage is out of date, use it by default
	}
	else if(XSI1999_PREFIX.equals(prefix)){
	    return XSI_NAMESPACE1999;  
	}
	else if(XSI2001_PREFIX.equals(prefix)){
	    return XSI_NAMESPACE2001;  //Used by Axis
	}
	else if(SOAP_ENC_PREFIX.equals(prefix)){
	    return SOAP_ENC_NAMESPACE;
	}
	else if(WS_ADDRESSING_PREFIX.equals(prefix)){  //used by async services
	    return WS_ADDRESSING_NAMESPACE;
	}
	else if(WSRP_PREFIX.equals(prefix)){ //used by async services
	    return WSRP_NAMESPACE;
	}
	else{
	    return "";  // Indicates that we don't have a mapping for this prefix
	}
    }

    /**
     * Don't use this class if this method will be used.  We are not returning a 
     * valid XML namespace mapping, but a convenience method for XPath, therefore
     * we will not give context sensitive mapping.
     */
    public String getNamespaceForPrefix(java.lang.String prefix, org.w3c.dom.Node context){
	return getNamespaceForPrefix(prefix);
    }

    public boolean handlesNullPrefixes(){
	return false;
    }
}
