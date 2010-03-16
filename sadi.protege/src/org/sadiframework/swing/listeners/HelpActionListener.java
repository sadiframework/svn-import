/**
 * 
 */
package org.sadiframework.swing.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

/**
 * @author Eddie Kawas
 *
 */
public class HelpActionListener implements ActionListener{

    private String msg = "";
    private final ResourceBundle bundle = ResourceBundle.getBundle("org.sadiframework.utils.i18n.EditorResourceBundle");
    public HelpActionListener(String msg) {
        if (msg != null)
            this.msg = msg;
    }
    public void actionPerformed(ActionEvent e) {
        JComponent source = null;
        if (e.getSource() instanceof JComponent) {
            source = (JComponent)e.getSource();
                
        }
        JOptionPane.showMessageDialog(source, msg, bundle.getString("help"), JOptionPane.INFORMATION_MESSAGE);
    }

}
