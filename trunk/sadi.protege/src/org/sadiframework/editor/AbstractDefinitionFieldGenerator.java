/**
 * 
 */
package org.sadiframework.editor;

/**
 * This abstract class ties together the available fields for definition files.
 * @author Eddie Kawas
 *
 */
public abstract class AbstractDefinitionFieldGenerator {

	final public static String TEXT_FIELD = "text";
	final public static String BOOLEAN_FIELD = "bool";
	final public static String DROP_TEXT_FIELD = "droptext";
	
	public abstract DefinitionField[] getDefinitionFields();
}
