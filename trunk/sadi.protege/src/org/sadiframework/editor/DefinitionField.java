/**
 * 
 */
package org.sadiframework.editor;

import javax.swing.Icon;

/**
 * @author Eddie
 * 
 */
public class DefinitionField {

	private String key = ""; // key
	private String label = ""; // label
	private String type = ""; // type TEXT | CHECKBOX | DROP_TEXT
	private String action = "";
	private String id = "";
	private int index = -1;
	private boolean required = true; // is this a required field
	private String helpText = ""; // the help text to display
	private Icon icon = null; // an icon for this field (optional)


	/**
	 * @param key
	 * @param label
	 * @param type
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

    /**
     * @return the id
     */
	public String getId() {
        return this.id;
    }

    /**
     * @param id the id to set
     */
	public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the required
     */
	public boolean isRequired() {
        return this.required;
    }

    /**
     * @param required the required to set
     */
	public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * @return the helpText
     */
	public String getHelpText() {
        return this.helpText;
    }

    /**
     * @param helpText the helpText to set
     */
	public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    /**
     * @return the icon
     */
	public Icon getIcon() {
        return this.icon;
    }

    /**
     * @param icon the icon to set
     */
	public void setIcon(Icon icon) {
        this.icon = icon;
    }



    /**
     * @return the index
     */
    public int getIndex() {
        return this.index;
    }



    /**
     * @param index the index to set
     */
    public void setIndex(int index) {
        this.index = index;
    }
}
