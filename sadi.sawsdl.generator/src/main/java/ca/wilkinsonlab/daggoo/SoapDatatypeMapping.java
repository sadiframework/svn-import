package ca.wilkinsonlab.daggoo;

public class SoapDatatypeMapping {

    private String memberName = "";
    private boolean optional = false;
    private String datatype = "";
    private String prefix = "";
    private boolean array = false;
    
    public SoapDatatypeMapping() {
	
    }
    
    public String getMemberName() {
        return memberName;
    }
    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }
    public boolean isOptional() {
        return optional;
    }
    public void setOptional(boolean optional) {
        this.optional = optional;
    }
    public String getDatatype() {
        return datatype;
    }
    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }
    public String getPrefix() {
        return prefix;
    }
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    public boolean isArray() {
        return array;
    }
    public void setArray(boolean array) {
        this.array = array;
    }
    @Override
    public String toString() {
        return String.format("%s%s(%s%s) %s", datatype, isArray() ? "[]" : "", prefix, memberName, isOptional() ? "" : "required");
    }
}
