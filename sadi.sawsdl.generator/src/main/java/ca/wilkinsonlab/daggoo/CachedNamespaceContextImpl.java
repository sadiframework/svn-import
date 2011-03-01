package ca.wilkinsonlab.daggoo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CachedNamespaceContextImpl implements NamespaceContext {
    private static final String DEFAULT_NS = "DEFAULT";
    private Map<String, String> prefix2Uri = new HashMap<String, String>();
    private Map<String, String> uri2Prefix = new HashMap<String, String>();

    /**
     * This constructor parses the document and stores all namespaces it can
     * find. If toplevelOnly is true, only namespaces in the root are used.
     * 
     * @param document
     *            source document
     * @param toplevelOnly
     *            restriction of the search to enhance performance
     */
    public CachedNamespaceContextImpl(Document document, boolean toplevelOnly) {
        if (document != null)
            examineNode(document.getFirstChild(), toplevelOnly);
        // As a bonus, always include the "fn" prefix, which allows access to XPath functions
	prefix2Uri.put("http://www.w3.org/2005/xpath-functions", "fn");
	uri2Prefix.put("fn", "http://www.w3.org/2005/xpath-functions");
	// Populate with default mappings (Mainly for XPath usage)
	prefix2Uri.put(SadiPrefixResolver.SADI_XML_NAMESPACE, SadiPrefixResolver.SADI_XML_PREFIX);
	prefix2Uri.put(SadiPrefixResolver.SADI_XML_NAMESPACE_INVALID, SadiPrefixResolver.SADI_XML_PREFIX);
	prefix2Uri.put(SadiPrefixResolver.SADI_TRANSPORT_NAMESPACE, SadiPrefixResolver.SADI_TRANSPORT_PREFIX);
	prefix2Uri.put(SadiPrefixResolver.XSI_NAMESPACE1999, SadiPrefixResolver.XSI1999_PREFIX);
	prefix2Uri.put(SadiPrefixResolver.XSI_NAMESPACE2001, SadiPrefixResolver.XSI2001_PREFIX);
	prefix2Uri.put(SadiPrefixResolver.SOAP_ENC_NAMESPACE, SadiPrefixResolver.SOAP_ENC_PREFIX);
	prefix2Uri.put(SadiPrefixResolver.WS_ADDRESSING_NAMESPACE, SadiPrefixResolver.WS_ADDRESSING_PREFIX); 
	prefix2Uri.put(SadiPrefixResolver.WSRP_NAMESPACE, SadiPrefixResolver.WSRP_PREFIX);	
	prefix2Uri.put(SadiPrefixResolver.XHTML_NAMESPACE, SadiPrefixResolver.XHTML_PREFIX);
	prefix2Uri.put(SadiPrefixResolver.XLINK_NAMESPACE, SadiPrefixResolver.XLINK_PREFIX);
	prefix2Uri.put(SadiPrefixResolver.WSDL_NAMESPACE, SadiPrefixResolver.WSDL_PREFIX);
	prefix2Uri.put(SadiPrefixResolver.HTTP_NAMESPACE, SadiPrefixResolver.HTTP_PREFIX);
	prefix2Uri.put(SadiPrefixResolver.SAWSDL_NAMESPACE, SadiPrefixResolver.SAWSDL_PREFIX);
	prefix2Uri.put(SadiPrefixResolver.LSID_NAMESPACE, SadiPrefixResolver.LSID_PREFIX);
	prefix2Uri.put(SadiPrefixResolver.XSD_NAMESPACE, SadiPrefixResolver.XSD_PREFIX);
	prefix2Uri.put(SadiPrefixResolver.DUBLIN_CORE_NAMESPACE, SadiPrefixResolver.DUBLIN_CORE_PREFIX);

	// Reverse map prefix -> nsURI
	uri2Prefix.put(SadiPrefixResolver.XSI_PREFIX, SadiPrefixResolver.XSI_NAMESPACE2001);
	for(Map.Entry<String, String> entry: prefix2Uri.entrySet()){
	    uri2Prefix.put(entry.getValue(), entry.getKey());
	}
    }

    /**
     * A single node is read, the namespace attributes are extracted and stored.
     * 
     * @param node
     *            to examine
     * @param attributesOnly,
     *            if true no recursion happens
     */
    private void examineNode(Node node, boolean attributesOnly) {
        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            storeAttribute((Attr) attribute);
        }

        if (!attributesOnly) {
            NodeList chields = node.getChildNodes();
            for (int i = 0; i < chields.getLength(); i++) {
                Node chield = chields.item(i);
                if (chield.getNodeType() == Node.ELEMENT_NODE)
                    examineNode(chield, false);
            }
        }        
    }

    /**
     * This method looks at an attribute and stores it, if it is a namespace
     * attribute.
     * 
     * @param attribute
     *            to examine
     */
    private void storeAttribute(Attr attribute) {
        // examine the attributes in namespace xmlns
        if (attribute.getNamespaceURI() != null
                && attribute.getNamespaceURI().equals(
                        XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            // Default namespace xmlns="uri goes here"
            if (attribute.getNodeName().equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                putInCache(DEFAULT_NS, attribute.getNodeValue());
            } else {
                // The defined prefixes are stored here
                putInCache(attribute.getLocalName(), attribute.getNodeValue());
            }
        }

    }

    private void putInCache(String prefix, String uri) {
        prefix2Uri.put(prefix, uri);
        uri2Prefix.put(uri, prefix);
    }

    /**
     * This method is called by XPath. It returns the default namespace, if the
     * prefix is null or "".
     * 
     * @param prefix
     *            to search for
     * @return uri
     */
    public String getNamespaceURI(String prefix) {
        if (prefix == null || prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
            return prefix2Uri.get(DEFAULT_NS);
        } else {
            return prefix2Uri.get(prefix);
        }
    }

    /**
     * This method is not needed in this context, but can be implemented in a
     * similar way.
     */
    public String getPrefix(String namespaceURI) {
        return uri2Prefix.get(namespaceURI);
    }

    @SuppressWarnings("rawtypes")
    public Iterator getPrefixes(String namespaceURI) {
        // Not implemented
        return null;
    }
}
