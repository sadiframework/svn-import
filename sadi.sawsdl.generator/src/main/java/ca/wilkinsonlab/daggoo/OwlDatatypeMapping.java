package ca.wilkinsonlab.daggoo;

import java.util.ArrayList;

public class OwlDatatypeMapping {

    private String soapId, prefix, valuesFrom, owlProperty;
    
    private ArrayList<String> extraProperties;
    private ArrayList<String> extraClasses;
    
    private boolean array = false;

    public OwlDatatypeMapping() {
	extraClasses = new ArrayList<String>();
	extraProperties = new ArrayList<String>();
    }

    public String getSoapId() {
	return soapId;
    }

    public void setSoapId(String soapId) {
	this.soapId = soapId;
    }

    public String getPrefix() {
	return prefix;
    }

    /**
     * This method helps you add prefixes to your input data. For instance, KEGG
     * pathways require <code>path:</code> to be prefixed to identifiers. SADI
     * input classes will not pass this prefix in their input. So set it here!
     * 
     * @param prefix
     */
    public void setPrefix(String prefix) {
	this.prefix = prefix;
    }

    public String getValuesFrom() {
	return valuesFrom;
    }

    public void setValuesFrom(String valuesFrom) {
	this.valuesFrom = valuesFrom;
    }

    public String getOwlProperty() {
	return owlProperty;
    }

    public void setOwlProperty(String owlProperty) {
	this.owlProperty = owlProperty;
    }
    
    @Override
    public String toString() {
	StringBuilder s = new StringBuilder(String.format("%s{\n\t<%s valuesFrom %s>", soapId, owlProperty, valuesFrom));
	for (int x = 0; x< extraClasses.size(); x++) {
	    s.append(String.format("\n\t\t<%s valuesFrom %s>", extraProperties.get(x), extraClasses.get(x)));
	}
	s.append("\n}");
	return s.toString();
    }

    public void setArray(boolean array) {
	this.array = array;
    }

    public boolean isArray() {
	return array;
    }
    
    public void addExtra(String owlProperty, String owlClass) {
	if (owlProperty != null && owlClass != null) {
	    extraClasses.add(owlClass);
	    extraProperties.add(owlProperty);
	}
    }
    
    public ArrayList<String[]> getExtras() {
	ArrayList<String[]> list = new ArrayList<String[]>();
	for (int x = 0; x < extraClasses.size(); x++) {
	    String[] s = new String[2];
	    s[0] = extraProperties.get(x);
	    s[1] = extraClasses.get(x);
	    list.add(s);
	}
	return list;
    }
    public int getExtraCount() {
	return extraClasses.size();
    }

}
