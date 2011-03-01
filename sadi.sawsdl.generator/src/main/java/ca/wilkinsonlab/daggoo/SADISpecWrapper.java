package ca.wilkinsonlab.daggoo;

import java.net.URL;
import java.util.*;

/**
 * Base class for implementing classes that parse some existing service specification 
 * enhanced to wrap the operations with SADI Semantic information, such as CGIs and WSDL services. 
 */
public class SADISpecWrapper{

    private Map<String,String> serviceNames;
    protected String currentService;
    private Map<String,String> serviceTypes;
    private Map<String,String> serviceDescs;
    private Map<String,String> providerURIs;
    private Map<String,String> centralEndpoints;
    private URL specURL; 
    private String contactEmail;

    private Map<String,Map<String,String>> serviceInputs;
    private Map<String,Map<String,String>> serviceInputFormats;
    private Map<String,Map<String,String>> serviceOutputs;
    private Map<String,Map<String,String>> serviceOutputFormats;
    private Map<String,Map<String,String>> fixedParams;
    private Map<String,Map<String,String>> sadiServiceName2SadiParamRenameMap;

    public SADISpecWrapper(){
	serviceNames = new HashMap<String,String>();
	currentService = "";
	serviceTypes = new HashMap<String,String>();
	serviceDescs = new HashMap<String,String>();
	providerURIs = new HashMap<String,String>();
	centralEndpoints = new HashMap<String,String>();

	serviceInputs = new HashMap<String,Map<String,String>>();
	serviceOutputs = new HashMap<String,Map<String,String>>();
	fixedParams = new HashMap<String,Map<String,String>>();
	serviceInputFormats = new HashMap<String,Map<String,String>>();
	serviceOutputFormats = new HashMap<String,Map<String,String>>();	
	sadiServiceName2SadiParamRenameMap = new HashMap<String,Map<String,String>>();
    }

    public Map<String,String> getSadiParams2ServiceParams(){
	return sadiServiceName2SadiParamRenameMap.get(currentService);
    }

    /**
     * Primarily for use by systems that allow renaming of params, since SAWSDL doesn't directly
     */
    public void setSadiParams2ServiceParams(Map<String,String> sadiName2serviceName){
	sadiServiceName2SadiParamRenameMap.put(currentService, sadiName2serviceName);
    }

    public String[] getServiceNames(){
	return providerURIs.keySet().toArray(new String[providerURIs.size()]);
    }

    public void setCurrentService(String serviceToReport) throws IllegalArgumentException{
	currentService = serviceToReport;
    }

    /**
     * location of the WSDL, CGI form, etc., only one per class instance
     */
    public void setSpecURL(URL serviceSpecURL){
	specURL = serviceSpecURL;
    }

    public URL getSpecURL(){
	return specURL;
    }

    public String getServiceName(){
	return currentService;
    }

    public void setServiceType(String type){
	serviceTypes.put(currentService, type);
    }

    public String getServiceType(){
	return serviceTypes.get(currentService);
    }

    public void setServiceDesc(String desc){
	serviceDescs.put(currentService, desc);
    }

    public String getServiceDesc(){
	return serviceDescs.get(currentService);
    }

    public void setProviderURI(String uri){
	providerURIs.put(currentService, uri);
    }

    public String getProviderURI(){
	return providerURIs.get(currentService);
    }

    public void setCentralEndpoint(String ep){
	centralEndpoints.put(currentService, ep);
    }

    public String getCentralEndpoint(){
	return centralEndpoints.get(currentService);
    }

    public void setContactEmail(String email){
	contactEmail = email;
    }

    public String getContactEmail(){
	return contactEmail;
    }

    /**
     * @param specs Map<cgi_param_name,mobyservlet_param_spec>, where mobyservlet_param_spec has the form paramName:ObjectClass as per the mobyService annotation mechanism
     */
    public void setPrimaryInputs(Map<String,String> specs){
	serviceInputs.put(currentService, specs);
    }

    public Map<String,String> getPrimaryInputs(){
	return serviceInputs.get(currentService);
    }


    public void setPrimaryInputFormats(Map<String,String> specs){
	serviceInputFormats.put(currentService, specs);
    }

    /**
     * Note that the string values for the formats are specialized for the 
     * various LegacyService child classes, as they will all have their own naming scheme for legacy
     * formats.
     */
    public Map<String,String> getPrimaryInputFormats(){
	return serviceInputFormats.get(currentService);
    }

    public void setPrimaryOutputs(Map<String,String> specs){
	serviceOutputs.put(currentService, specs);
    }

    public Map<String,String> getPrimaryOutputs(){
	return serviceOutputs.get(currentService);
    }

    public void setPrimaryOutputFormats(Map<String,String> specs){
	serviceOutputFormats.put(currentService, specs);
    }

    public Map<String,String> getPrimaryOutputFormats(){
	return serviceOutputFormats.get(currentService);
    }

    public void setFixedParams(Map<String,String> params){
	fixedParams.put(currentService, params);
    }

    public Map<String,String> getFixedParams(){
	return fixedParams.get(currentService);
    }
}
