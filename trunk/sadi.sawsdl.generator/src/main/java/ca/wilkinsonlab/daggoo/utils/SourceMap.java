package ca.wilkinsonlab.daggoo.utils;

import javax.xml.namespace.QName;

import java.io.*;
import java.util.*;

/**
 * Converts a Map of Strings into a Source for the Java SOAP libraries.
 * The Map should be of the form (SOAP input param name -> XML Schema Instance data).
 * This class assumes that all of the elements being put into the map are of the same 
 * namespace as the message name given in the constructor.  Nesting of elements is 
 * achieved using a colon, e.g. given "a:b" and "a:c" keys, a two level map is created 
 * with one entry at the first level (a), and two at the second level (b, c).  MEMBER_REF_PREFIX
 * as the entry value denotes the nesting of maps.
 */
public class SourceMap extends javax.xml.transform.stream.StreamSource implements Map<String,String[]>{
    private static final String SINGLE_VAL_PREFIX = "_seahawkSingleValuePrefix09";
    private static final String MEMBER_REF_PREFIX = "_thisIsARefNotanActualvalue";
    private Map<String,String[]> members;
    private Map<String,SourceMap> submaps;
    private QName messageName;
    private String use;

    /**
     * @param msgName the name of the operation message (WSDL 1.1) or operation element (WSDL 2.0) that is being encapsulated
     * @param encoding "literal" or "encoded", the WSDL data encoding style, or "raw", so the wrapping tag is already assumed to be present
     */
    public SourceMap(QName msgName, String encoding){
	members = new LinkedHashMap<String,String[]>();
	submaps = new LinkedHashMap<String,SourceMap>();
	messageName = msgName;
	use = encoding;
    }

    /**
     * Just before we give up the data, set the stream to be the contents of the 
     * Map set to date.
     */
    public InputStream getInputStream(){
	setInputStream(new ByteArrayInputStream(toString().getBytes()));
	return super.getInputStream();
    }

    public String toString(){
	
//	if (use.equalsIgnoreCase("literal")) {
//	    return 
//	       serializeMapContents();
//	}
	return //"<?xml version='1.0'?>\n"+
	       "<"+messageName.getLocalPart()+" xmlns=\""+messageName.getNamespaceURI()+"\">"+
	       serializeMapContents()+
               "</"+messageName.getLocalPart()+">";
    }

    private String serializeMapContents(){
	StringBuffer xmlContents = new StringBuffer();

	// Serialize map members 
	for(Map.Entry<String,String[]> member: members.entrySet()){
	    SourceMap submembers = getSubMap(member.getKey());
	    if(submembers != null){
		// nested elements
		xmlContents.append(createXML(member.getKey(), submembers.serializeMapContents()));
	    }
	    else{
		// process the whole bunch of values at once
		xmlContents.append(createXML(member.getKey(), member.getValue()));
	    }
	}

	return xmlContents.toString();
    }

    private String createXML(String name, String[] values){
	String valuesXML = "";
	int x = 0;
	if(use.equals("literal") || use.equals("raw")){
	    for(String value: values){
		if (x++ == 0) {
		    valuesXML = createXML(name, value);
		} else {
		    valuesXML += createXML(name, value);
		}
	    }
	}
	// encoded: todo get data type dynamically
	else{
	    if(values.length == 1 && values[0].indexOf(SINGLE_VAL_PREFIX) == 0){
		return createXML(name, values[0]);
	    }
	    else{ //an array
		valuesXML = "<"+name+" xsi:type=\"SOAP-ENC:Array\" SOAP-ENC:arrayType=\"xsd:string[" +
		    values.length+"]\" xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance\" " +
		    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " + 
		    "xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\">";
		for(String value: values){
		    valuesXML += "<item xsi:type=\"xsd:string\">"+value+"</item>";
		}
		valuesXML += "</"+name+">";
	    }
	}
	return valuesXML;
    }

    private String createXML(String name, String value){
	if(value.indexOf(SINGLE_VAL_PREFIX) == 0){
	    value = value.substring(SINGLE_VAL_PREFIX.length());
	}
	if(use.equals("raw")){
	    return value;
	}
	else if(use.equals("literal")){
	    return "<"+name +">"+value+"</"+name+">";
	}
	else{
	    return "<"+name+" xsi:type=\"xsd:string\" " +
		"xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance\" "+
		"xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">" +
		value+"</"+name+">"; //encoded: todo get data type dynamically
	}
    }

    /**
     * Effectively deletes the composite, leaving you with a blank base object
     */
    public void clear(){
	members.clear();
    }

    public Collection<String[]> values(){
	return members.values();
    }

    /**
     * To check for the presence of a field with a given name
     */
    public boolean containsKey(Object fieldName){
	return members.containsKey(fieldName);
    }

    /**
     * To check for the presence of a value in one of the members (e.g Integer, Float, String, Calendar)
     */
    public boolean containsValue(Object value){
	return members.containsValue(value);
    }

    /**
     * Retrieves each field name/value pair for the members of the composite object
     */
    public Set<Map.Entry<String,String[]>> entrySet(){
	return members.entrySet();
    }

    /**
     * Returns true if and only if both objects have the same fields with the same values, and the same object ID
     */
    public boolean equals(Object o){
	return members.equals(o);
    }

    /**
     * Retrieves a member of the composite with a given field name.
     */
    public String[] get(Object fieldName){
	return members.get(fieldName);
    }

    public int hashCode(){
	return members.hashCode();
    }

    /**
     * Is this a blank, uninstantiated object?
     */
    public boolean isEmpty(){
	return members.isEmpty();
    }

    /**
     * Retrieves a list of the field names in this object
     */
    public Set<String> keySet(){
	return members.keySet();
    }

    /**
     * To be used if a sequence is expected for a member.  fieldName can have the 
     * form a or a:b for nested tags to be represented.
     */
    public String[] put(String fieldName, String[] values){
	if(fieldName.indexOf(":") != -1){
	    // Nested member, so delegate
	    SourceMap subMap = getSubMap(fieldName);
	    if(subMap == null){
		subMap = createSubMap(fieldName);
	    }
	    String subfield = fieldName.substring(fieldName.indexOf(":") + 1);
	    return subMap.put(subfield, values);
	}
	else{
	    return members.put(fieldName, values);
	}
    }

    public String[] put(String fieldName, String value){
	if(fieldName.indexOf(":") != -1){
	    // Nested member, so delegate
	    SourceMap subMap = getSubMap(fieldName);
	    if(subMap == null){
		subMap = createSubMap(fieldName);
	    }
	    String subfield = fieldName.substring(fieldName.indexOf(":") + 1);	
	    return subMap.put(subfield, value);
	}
	else{
	    return members.put(fieldName, new String[]{SINGLE_VAL_PREFIX+value});
	}
    }

    private SourceMap createSubMap(String fieldName){
	if(fieldName.indexOf(":") != -1){  // chop off any nested names
	    fieldName = fieldName.substring(0, fieldName.indexOf(":"));
	}
	members.put(fieldName, new String[]{MEMBER_REF_PREFIX+submaps.size()});
	SourceMap newMap = new SourceMap(new QName(""), use);
	submaps.put(""+submaps.size(), newMap);
	return newMap;
    }

    // Essential the data structure is Map<field,string(ref)> -> Map<string(ref),SourceMap>
    // to allow any value to be either a literal string or pointer to nested elements
    private SourceMap getSubMap(String fieldName){
	if(fieldName.indexOf(":") != -1){  // chop off any nested names
	    fieldName = fieldName.substring(0, fieldName.indexOf(":"));
	}
	String vals[] = members.get(fieldName);
	if(vals == null || vals.length != 1){
	    return null;  //must be an array of more than one literal value
	}
	String submapRef = vals[0];
	if(submapRef.indexOf(MEMBER_REF_PREFIX) != 0){
	    return null;
	}
	submapRef = submapRef.substring(MEMBER_REF_PREFIX.length());
	return submaps.get(submapRef);
    }

    /**
     * Sets a number of object fields at once.
     */
    public void putAll(Map<? extends String,? extends String[]> map){
	members.putAll(map);
    }

    /**
     * Removes the field with the given name, if present
     *
     * @return the data field removed
     */
    public String[] remove(Object fieldName){
	return members.remove(fieldName);
    }

    /**
     * Reports the number of data members in the composite object
     */
    public int size(){
	return members.size();
    }
  
}
