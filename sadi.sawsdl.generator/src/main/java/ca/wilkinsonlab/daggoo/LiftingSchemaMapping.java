package ca.wilkinsonlab.daggoo;

import java.util.ArrayList;
import java.util.List;

public class LiftingSchemaMapping extends SchemaMapping {

    List<LiftingMap> liftingMap;
    public LiftingSchemaMapping() {
	super();
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
    
}
