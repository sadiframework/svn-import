package ca.wilkinsonlab.daggoo.sparql;

import java.util.ArrayList;
import java.util.List;

public class SparqlResult {

    private String binding;
    private List<String> values;
    
    public SparqlResult() {
	this("",null);
    }
    
    public SparqlResult(String b,  List<String> list) {
	binding=b;
	values = list == null ? new ArrayList<String>() : list;
    }
    
    public void addValue(String value) {
	if (values == null)
	    values =new ArrayList<String>();
	values.add(value);
    }
    
    public void addValues(List<String> values) {
	for (String value : values)
	    addValue(value);
    }
    
    public List<String> getValues() {
	return values == null ? new ArrayList<String>() : values;
    }
    public String getBinding() {
	return binding;
    }
    public void setBinding(String binding) {
	this.binding = binding;
    }
    public void setValues(List<String> values) {
	this.values = values;
    }
    @Override
    public String toString() {
        return getValues().toString();
    }
    
}
