/**
 * 
 */
package org.sadiframework.editor;

/**
 * @author Eddie
 * 
 */
public class DefinitionField {

	private String key = "";
	private String label = "";
	private String type = "";
	private String action = "";

	/**
	 * @param key
	 * @param label
	 * @param type
	 * @param action
	 */
	public DefinitionField(String key, String label, String type) {
		super();
		this.key = key;
		this.label = label;
		this.type = type;
	}



	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @param key
	 *            the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @param label
	 *            the label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * @param action the action to set
	 */
	public void setAction(String action) {
		this.action = action;
	}

}
