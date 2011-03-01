/**
 * 
 */
package ca.wilkinsonlab.daggoo;

/**
 * @author Eddie
 * 
 */
public class LiftingMap {

    private String id;

    private String type;

    private String value;

    /**
     * XPATH: LiftingMap with this type implies that the value for this object is an xpath expression
     */
    public final static String XPATH = "xpath";

    /**
     * REGEX: LiftingMap with this type implies that the value for this object is a regular expression
     */
    public final static String REGEX = "regex";

    /**
     * STRING: LiftingMap with this type implies that the value for this object is contained in the lowering schema with this ID
     */
    public final static String STRING = "string";

    /**
     * Default constructor
     */
    public LiftingMap() {
	this("", STRING, "");
    }

    /**
     * 
     * @param id
     *            the id of the mapping
     * @param type
     *            the type of the mapping (one of <code>XPATH</code>,
     *            <code>REGEX</code>, <code>STRING</code>)
     * @param value
     *            how to apply the mapping. For <code>XPATH</code> and
     *            <code>REGEX</code> this is the expression.
     */
    public LiftingMap(String id, String type, String value) {
	setId(id);
	setType(type);
	setValue(value);
    }

    public String getId() {
	return id;
    }

    public void setId(String id) {
	if (id != null)
	    this.id = id.trim();
    }

    public String getType() {
	return type;
    }

    /**
     * 
     * @param type
     */
    public void setType(String type) {
	if (type != null)
	    this.type = type.trim();
    }

    public String getValue() {
	return value;
    }

    public void setValue(String value) {
	if (value != null)
	    this.value = value.trim();
    }
    @Override
    public String toString() {
        return "id(" + id + "), type(" + type + "), value(" + value + ")";
    }

}
