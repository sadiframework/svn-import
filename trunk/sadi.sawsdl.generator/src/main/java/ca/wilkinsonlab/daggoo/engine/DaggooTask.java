package ca.wilkinsonlab.daggoo.engine;

import java.io.File;

import ca.wilkinsonlab.daggoo.SAWSDLService;
import ca.wilkinsonlab.sadi.tasks.Task;

import com.hp.hpl.jena.rdf.model.Model;

public class DaggooTask extends Task {

    private SAWSDLService s;
    private String mappingPrefix;
    private Model outputModel;
    private String input = "";
    
    public DaggooTask(SAWSDLService service, String prefix, String input) {
	this.s = service;
	this.mappingPrefix = prefix;
	this.input = input;
    }
    
    
    public void run() {
	// execute the service request
	Daggoo4SadiEngine engine = null;
	try {
	    engine = new Daggoo4SadiEngine(new File(s.getWsdlLocation()).toURI().toURL(), s.getName(), mappingPrefix == null ? "" : mappingPrefix);
	} catch (Exception e) {
	    e.printStackTrace();
	    setError(e);
	    return;
	}
	
	try {
	    setOutputModel(engine.processRequest(input));
	} catch (Exception e) {
	    e.printStackTrace();
	    setError(e);
	    return;
	}
	success();
    }

    public Model getOutputModel() {
        return outputModel;
    }

    @Override
    public void dispose() {
	super.dispose();
	if (outputModel != null)
	    outputModel.close();

    }

    public void setOutputModel(Model outputModel) {
        this.outputModel = outputModel;
    }
    
    public String getServiceName() {
	return s.getName() == null ? "" : s.getName();
    }

}
