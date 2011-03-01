package ca.wilkinsonlab.daggoo;

import java.util.HashMap;
import java.util.Map;

public class ResultMap {

    private String result;

    private Map<String, String> inputs;

    public ResultMap() {
	this("", null);
    }

    public ResultMap(String result, Map<String, String> inputs) {
	setResult(result);
	setInputs(inputs);
    }

    public String getResult() {
	return result;
    }

    public void setResult(String result) {
	if (result != null)
	    this.result = result.trim();
    }

    public Map<String, String> getInputs() {
	return inputs;
    }

    public void setInputs(Map<String, String> inputs) {
	if (inputs == null)
	    this.inputs = new HashMap<String, String>();
	else
	    this.inputs = inputs;
    }

}
