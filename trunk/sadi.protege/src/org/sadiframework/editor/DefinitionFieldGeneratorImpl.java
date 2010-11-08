package org.sadiframework.editor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Eddie Kawas
 * 
 */
public class DefinitionFieldGeneratorImpl extends AbstractDefinitionFieldGenerator {

    private final static String XML_FILENAME = "sadi-signature-fields.xml";

    private final ResourceBundle bundle = ResourceBundle
            .getBundle("org.sadiframework.utils.i18n.EditorResourceBundle");

    @Override
    public DefinitionField[] getDefinitionFields() {
        DefinitionField[] fields = new DefinitionField[map.size()];
        // slot these guys into empty spots in the fields array
        ArrayList<DefinitionField> badIndex = new ArrayList<DefinitionField>();
        // if index of current item is -1 or out of bounds, add it to the end of the array ...
        // if multiple indices are -1 or out of bounds, then sort based on label and add to the array
        for (DefinitionField d : map.values()) {
            if (d.getIndex() == -1 || d.getIndex() > fields.length || fields[d.getIndex()-1] != null) {
                badIndex.add(d);
            } else {
                fields[d.getIndex()-1] = d;
            }
        }
        // sort based on label
        Collections.sort(badIndex, new Comparator<DefinitionField>() {
            public int compare(DefinitionField o1, DefinitionField o2) {
                return o1.getLabel().compareTo(o2.getLabel());
            }
        });
        for (DefinitionField d : badIndex) {
            for (int x = 0; x < fields.length; x++) {
                if (fields[x] == null){
                    fields[x] = d;
                    break;
                }
            }
        }
        
        return fields;
    }

    // map of definition ID to DefinitionField object
    private HashMap<String, DefinitionField> map;

    /**
     * Default Constructor
     */
    public DefinitionFieldGeneratorImpl() {
        map = new HashMap<String, DefinitionField>();
        load_xml();
    }

    @Override
    public DefinitionField getDefinitionByID(String id) {
        if (map.containsKey(id))
            return map.get(id);
        return null;
    }

    private void load_xml() {
        InputStream in=null;
        try {
            in = getClass().getResourceAsStream(
                    String.format("/resources/%s", XML_FILENAME));
            if (in == null) {
                return;
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(in);
            doc.getDocumentElement().normalize();

            NodeList nodeLst = doc.getElementsByTagName("DefinitionField");

            for (int s = 0; s < nodeLst.getLength(); s++) {
                Node node = nodeLst.item(s);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) node;
                    String label = e.getAttribute("label");
                    String type = e.getAttribute("type");
                    String id = e.getAttribute("id");
                    String order = e.getAttribute("index"); // extract int
                    String req = e.getAttribute("required").toLowerCase(); // extract boolean
                    String helpText = e.getAttribute("helpText"); // check bundle for text
                    String key = e.getAttribute("key");
                    
                    // now process the attributes
                    int index = -1;
                    boolean required = true;
                    try {
                        index = Integer.parseInt(order);
                    } catch (NumberFormatException nfe) {}
                    required = Boolean.parseBoolean(req);
                    // if the following items refer to a key in our bundle then load them
                    try {
                        label = bundle.getString(label);
                    } catch (MissingResourceException mre) {
                        // need a label
                        continue;
                    }
                    try {
                        helpText = bundle.getString(helpText);
                    } catch (MissingResourceException mre) {
                        helpText = "";
                    }
                    //System.out.println(String.format("label(%s), type(%s), id(%s), index(%s), required(%s), helpText(%s), key(%s)", label, type, id, index, required, helpText, key ));
                    // create our definition
                    DefinitionField def = new DefinitionField(key, label, type);
                    def.setRequired(required);
                    def.setHelpText(helpText);
                    def.setId(id);
                    def.setIndex(index);
                    // put in the map
                    map.put(key, def);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                }
        }
    }
}
