/**
 * 
 */
package org.sadiframework.editor;

/**
 * This abstract class ties together the available fields for definition files.
 * 
 * @author Eddie Kawas
 * 
 */
public abstract class AbstractDefinitionFieldGenerator {

    final public static String TEXT_FIELD = "text";
    final public static String BOOLEAN_FIELD = "bool";
    final public static String DROP_TEXT_FIELD = "droptext";

    /**
     * 
     * @return an array of DefinitionField objects in their sorted order
     */
    public abstract DefinitionField[] getDefinitionFields();

    /**
     * 
     * @param id
     *            the id of the DefinitionField you wish to obtain
     * @return the DefinitionField with the given id or <code>null</code> if it
     *         doesnt exist
     */
    public abstract DefinitionField getDefinitionByID(String id);
}
