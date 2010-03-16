/**
 * 
 */
package org.sadiframework.swing.listeners;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.text.JTextComponent;

import org.protege.editor.core.ui.util.JOptionPaneEx;

/**
 * This class represents the reset action. On reset, a dialogue comes up to
 * confirm a reset. If user insists on a reset, then all fields passed to the
 * constructor are reset.
 * 
 * @author Eddie Kawas
 * 
 */
public class ResetActionListener implements ActionListener {

    private final ResourceBundle bundle = ResourceBundle.getBundle("org.sadiframework.utils.i18n.EditorResourceBundle");
    private JComponent[] fields;
    /**
     * 
     * @param fields
     */
    public ResetActionListener(JComponent[] fields) {
       this.fields = fields;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        JComponent source = null;
        Component parent = null;
        if (e.getSource() instanceof JComponent) {
            source = (JComponent)e.getSource();
            if (source.getParent() != null)
                parent = source.getParent();
        }
        int retVal = JOptionPaneEx.showConfirmDialog(parent, bundle
                .getString("definition_reset_title"), new JLabel(bundle
                .getString("definition_reset")), JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_OPTION, source);
        if (retVal == JOptionPane.YES_OPTION)
            for (int i = 0; i < fields.length; i++) {
                JComponent jc = fields[i];
                if (jc instanceof JTextComponent) {
                    ((JTextComponent) jc).setText("");
                } else if (jc instanceof JToggleButton) {
                    ((JToggleButton) jc).setSelected(false);
                }
            }

    }

}
