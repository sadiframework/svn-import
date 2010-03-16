package org.sadiframework.swing;

import java.awt.event.ActionListener;

import javax.swing.JButton;

/**
 * Helper class for generating JButtons
 * 
 * @author Eddie Kawas
 * 
 */
public class AbstractButton extends JButton {

	private static final long serialVersionUID = -8946369137418103834L;

	/**
	 * Default constructor: empty label and enabled
	 */
	public AbstractButton() {
		this("", true);
	}

	/**
	 * Create an enabled JButton with a specified label
	 * 
	 * @param label
	 *            the JButton label
	 */
	public AbstractButton(String label) {
		this(label, true);
	}

	/**
	 * 
	 * @param label
	 *            the JButton label
	 * @param isEnabled
	 *            whether or not the button is enabled
	 */
	public AbstractButton(String label, boolean isEnabled) {
		this(label, isEnabled, null);
	}

	/**
	 * 
	 * @param label
	 *            the JButton label
	 * @param isEnabled
	 *            whether or not the button is enabled
	 * @param al
	 *            an action listener (or null if you dont have one)
	 */
	public AbstractButton(String label, boolean isEnabled, ActionListener al) {
		super(label);
		setEnabled(isEnabled);
		if (al != null) {
			addActionListener(al);
		}
	}

}
