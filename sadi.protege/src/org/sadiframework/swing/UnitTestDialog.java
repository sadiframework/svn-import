/**
 * 
 */
package org.sadiframework.swing;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.util.Icons;
import org.sadiframework.preferences.PreferenceManager;
import org.sadiframework.service.ServiceUnitTest;

/**
 * @author Eddie
 * 
 */
public class UnitTestDialog extends JDialog {

    private static final long serialVersionUID = -8371516957650916161L;

    private static String UNIT_TEST_DIRECTORY = "unit-test-directory";
    private static String UNIT_TEST_XML_DIRECTORY = "unit-test-xml-directory";
    
    private final ResourceBundle bundle = ResourceBundle
            .getBundle("org.sadiframework.utils.i18n.EditorResourceBundle");
    private PreferenceManager manager = PreferenceManager.newInstance();
    
    private JButton input, output;
    private JTextField inputFile, outputFile, servicename;

    public UnitTestDialog() {
        // set the layout, title and always on top
        setLayout(new GridBagLayout());
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        setTitle(bundle.getString("testing_service_unit_test_title"));
        // handle closing properly
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                setVisible(false);
                dispose();
            }
        });
        // create the service name label, textfield
        JLabel serviceLabel = new JLabel(bundle.getString("definition_name"), JLabel.LEADING);
        servicename = new JTextField(25);
        serviceLabel.setLabelFor(servicename);
        
        // create the input label, textfield and button
        JLabel inputLabel = new JLabel(bundle.getString("testing_service_unit_test_input"), JLabel.LEADING);
        inputFile = new JTextField(25);
        inputFile.setEditable(false);
        inputLabel.setLabelFor(inputFile);
        // no label for button (has an icon set below)
        input = new AbstractButton("", true, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // this is the action that is invoked when the open file button
                // is clicked on
                String title = bundle.getString("open");
                String directory = manager.getPreference(UNIT_TEST_XML_DIRECTORY, System.getProperty("user.dir"));
                String description = bundle.getString("xml_file_types");
                JFileChooser chooser = UIUtils.getOpenFileChooser(title, directory, UIUtils
                        .createFileFilter(description, new String[] { ".xml", ".owl", ".rdf" }));
                if (chooser.showOpenDialog((Component) e.getSource()) != JFileChooser.APPROVE_OPTION) {
                    // TODO close the window
                    return;
                }
                File file = chooser.getSelectedFile();
                if (file == null || !file.exists()) {
                    // TODO warn the user
                    return;
                }
                inputFile.setText(file.getAbsolutePath());
            }
        });
        input.setIcon(Icons.getIcon("project.open.gif"));

        // create the output label, textfield and button
        JLabel outputLabel = new JLabel(bundle.getString("testing_service_unit_test_output"), JLabel.LEADING);
        outputFile = new JTextField(25);
        outputFile.setEditable(false);
        outputLabel.setLabelFor(outputFile);
        // no label for button (has an icon set below)
        output = new AbstractButton("", true, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // this is the action that is invoked when the open file button
                // is clicked on
                String title = bundle.getString("open");
                String directory = manager.getPreference(UNIT_TEST_XML_DIRECTORY, System.getProperty("user.dir"));
                String description = bundle.getString("xml_file_types");
                JFileChooser chooser = UIUtils.getOpenFileChooser(title, directory, UIUtils
                        .createFileFilter(description, new String[] { ".xml", ".owl", ".rdf" }));
                if (chooser.showOpenDialog((Component) e.getSource()) != JFileChooser.APPROVE_OPTION) {
                    // TODO close the window
                    return;
                }
                File file = chooser.getSelectedFile();
                if (file == null || !file.exists()) {
                    // TODO warn the user
                    return;
                }
                outputFile.setText(file.getAbsolutePath());
            }
        });
        output.setIcon(Icons.getIcon("project.open.gif"));
        
        // create the save/cancel buttons
        JButton save = new AbstractButton(bundle.getString("save_file"), true, new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                if (servicename == null || servicename.getText().trim().equals("")) {
                    // no service name
                            JOptionPane.showMessageDialog(getParent(), bundle
                                    .getString("testing_service_unit_test_invalid_name"), bundle
                                    .getString("error"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                ServiceUnitTest test = new ServiceUnitTest();
                test.setInputFilePath(inputFile.getText());
                test.setOutputFilePath(outputFile.getText());
                
                File directory = null;
                // get a file chooser
                JFileChooser chooser = UIUtils.getOpenDirectoryChooser(
                        bundle.getString("open"), UNIT_TEST_DIRECTORY);

                int returnVal = chooser.showOpenDialog((JComponent) e
                        .getSource());
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    directory = chooser.getSelectedFile();
                    if (!directory.canWrite()) {
                        JOptionPane.showMessageDialog(getParent(), bundle
                                .getString("editor_cannot_write"),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                // save the directory in preferences
                manager.savePreference(UNIT_TEST_DIRECTORY, directory.getAbsolutePath());
                
                // save the file:
                File outfile = null;
                try {
                    outfile = new File(directory, servicename.getText());
                } catch (NullPointerException npe) {
                    ErrorLogPanel.showErrorDialog(npe);
                }
                // if it exists, prompt to overwrite
                if (outfile == null)
                    return;
                if (outfile.exists()) {
                    int confirm = JOptionPane.showConfirmDialog(getParent(),
                            String
                                    .format(bundle.getString("definition_file_exists"), servicename.getText()));
                    if (confirm == JOptionPane.CANCEL_OPTION || confirm == JOptionPane.NO_OPTION) {
                        // tell them to rename the service or choose a
                        // different directory
                        JOptionPane.showMessageDialog(getParent(), bundle
                                .getString("definition_rename"));
                        return;
                    }
                }
                try {
                    BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
                    out.write(test.toString());
                    out.close();
                } catch (IOException ioe) {
                    ErrorLogPanel.showErrorDialog(ioe);
                    return;
                }
                setVisible(false);
                dispose();
            }
        });
        JButton cancel = new AbstractButton(bundle.getString("cancel"), true, new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                if (inputFile != null)
                    inputFile.setText("");
                if (outputFile != null)
                    outputFile.setText("");
                setVisible(false);
                dispose();
            }
        });
        // create the save, cancel button panel
        JPanel buttonPanel = UIUtils.createButtonPanel(new JButton[]{save, cancel});
        
        // add to the layout
        UIUtils.addComponent(this, serviceLabel,0, 0, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(this, servicename, 1, 0, 2, 1, UIUtils.WEST, UIUtils.HORI, 1.0, 0.0);
        UIUtils.addComponent(this, inputLabel,  0, 1, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(this, inputFile,   1, 1, 2, 1, UIUtils.WEST, UIUtils.HORI, 1.0, 0.0);
        UIUtils.addComponent(this, input,       3, 1, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(this, outputLabel, 0, 2, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(this, outputFile,  1, 2, 2, 1, UIUtils.WEST, UIUtils.HORI, 1.0, 0.0);
        UIUtils.addComponent(this, output,      3, 2, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(this, buttonPanel, 0, 3, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        // pack it all together
        pack();
    }

}
