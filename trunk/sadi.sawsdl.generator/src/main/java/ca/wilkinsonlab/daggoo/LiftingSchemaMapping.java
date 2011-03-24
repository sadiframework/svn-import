package ca.wilkinsonlab.daggoo;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import ca.wilkinsonlab.sadi.rdfpath.RDFPath;

public class LiftingSchemaMapping extends SchemaMapping {

    List<LiftingMap> liftingMap;
    Map<String, RDFPath> rdfPaths = new HashMap<String, RDFPath>();
    
    public LiftingSchemaMapping() {
	super();
	rdfPaths = new HashMap<String, RDFPath>();
    }
    
    public List<LiftingMap> getLiftingMap() {
        return liftingMap;
    }
    public void setLiftingMap(List<LiftingMap> liftingMap) {
        this.liftingMap = liftingMap;
    }
    
    public void addLiftingMapping(LiftingMap mapping) {
	if (this.liftingMap == null)
	    setLiftingMap(new ArrayList<LiftingMap>());
        liftingMap.add(mapping);
    }
    
    @Override
    public void setTemplate(String template) {
        super.setTemplate(template);
        processTemplate();
    }

    private void processTemplate() {
	rdfPaths = new HashMap<String, RDFPath>();
	PropertiesConfiguration p = new PropertiesConfiguration();
	try {
	    p.load(new ByteArrayInputStream(getTemplate().getBytes()));
	} catch (ConfigurationException e) {
	    e.printStackTrace();
	    return;
	}
	Iterator<?> it = p.getKeys();
	while (it.hasNext()) {
	    String key = it.next() + "";
	    String[] parts = new String[]{};
	    RDFPath path;
	    if (p.getProperty(key) instanceof String) {
		String s = (String)p.getProperty(key); 
		parts = s.split(",");
		path = new RDFPath(parts);
	    } else if (p.getProperty(key) instanceof List){
		List s = (List)p.getProperty(key);
		parts = new String[s.size()];
		for (int i = 0; i < parts.length; i++) {
		    parts[i] = s.get(i) +"";
		}
		path = new RDFPath(parts);
	    } else {
		continue;
	    }
	    path.reuseExistingNodes = false;
	    rdfPaths.put(key.toString(), path);
	}
    }

    public Map<String, RDFPath> getRdfPaths() {
        return rdfPaths;
    }
    
}
