package ca.wilkinsonlab.daggoo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ca.wilkinsonlab.daggoo.utils.Base64;
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
	Properties p = new Properties();
	try {
	    p.load(new ByteArrayInputStream(getTemplate().getBytes()));
	} catch (IOException e) {
	    // TODO log this error
	}
	for (Object key : p.keySet()) {
	    String s = p.getProperty(key.toString());
	    String[] parts = s.split(",");
	    // each part is base64 encoded ... except for the last element
	    for (int i = 0; i < parts.length-1; i++) {
		parts[i] = new String(Base64.base64ToByteArray(parts[i]));
	    }
	    RDFPath path = new RDFPath(parts);
	    path.reuseExistingNodes = false;
	    rdfPaths.put(key.toString(), path);
	}
    }

    public Map<String, RDFPath> getRdfPaths() {
        return rdfPaths;
    }
    
}
