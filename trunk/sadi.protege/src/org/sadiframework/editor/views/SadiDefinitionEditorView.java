package org.sadiframework.editor.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.text.JTextComponent;


import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.util.ComponentFactory;
import org.protege.editor.owl.ui.view.AbstractOWLSelectionViewComponent;
import org.sadiframework.editor.AbstractDefinitionFieldGenerator;
import org.sadiframework.editor.DefinitionField;
import org.sadiframework.editor.PerlDefinitionFieldGenerator;
import org.sadiframework.preferences.PreferenceManager;
import org.sadiframework.service.ServiceDefinition;
import org.sadiframework.swing.AbstractButton;
import org.sadiframework.swing.DropJTextField;
import org.sadiframework.swing.UIUtils;
import org.sadiframework.swing.listeners.ResetActionListener;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;

public class SadiDefinitionEditorView extends AbstractOWLSelectionViewComponent {

    private PreferenceManager manager = PreferenceManager.newInstance();
    /**
     * SADI Definition Editor Last known user directory
     */
    final public static String DEFINITION_DIRECTORY = "definition-directory";

    /**
     * SADI Definition Editor Last known user filename
     */
    final public static String DEFINITION_FILENAME = "definition-filename";

    /**
     * SADI Definition Editor Last known user filename checksum
     */
    final public static String DEFINITION_FILENAME_CHKSUM = "definition-filename-checksum";

    private static final long serialVersionUID = 6011258260093115589L;
    private final ResourceBundle bundle = ResourceBundle
            .getBundle("org.sadiframework.utils.i18n.EditorResourceBundle");
    // keeps a reference to our textfields so that we can save them if necessary
    private ArrayList<JComponent> fields = new ArrayList<JComponent>();

    private JButton open, reset, save;

    private DefinitionField[] definitionFields;

    // remove any listeners and perform tidyup (none required in this case)
    public void disposeView() {

    }

    private JComponent getDefinitionViewComponent() {
        return this;
    }

    @Override
    public void initialiseView() throws Exception {
        setLayout(new BorderLayout(6, 6));
        add(new JScrollPane(getDefinitionEditorPanel()), BorderLayout.CENTER);
    }
    
    private JPanel getDefinitionEditorPanel() {
        // TODO load field defs based on flavour of service (Perl | JAVA)
        definitionFields = new PerlDefinitionFieldGenerator().getDefinitionFields();

        int numPairs = definitionFields.length;
        fields = new ArrayList<JComponent>();

        // Create and populate the panel.
        JPanel p = new JPanel(new GridBagLayout());
        for (int i = 0; i < numPairs; i++) {
            JLabel l = new JLabel(definitionFields[i].getLabel() + ":", JLabel.TRAILING);
            UIUtils.addComponent(p, l, 0, i, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
            if (definitionFields[i].getType().equals(AbstractDefinitionFieldGenerator.TEXT_FIELD)) {
                JTextField textField = new JTextField(25);
                l.setLabelFor(textField);
                UIUtils
                        .addComponent(p, textField, 1, i, 2, 1, UIUtils.WEST, UIUtils.HORI, 1.0,
                                0.0);
                fields.add(textField);
            } else if (definitionFields[i].getType().equals(
                    AbstractDefinitionFieldGenerator.DROP_TEXT_FIELD)) {
                DropJTextField textField = new DropJTextField(25, getOWLModelManager());
                l.setLabelFor(textField);
                UIUtils
                        .addComponent(p, textField, 1, i, 2, 1, UIUtils.WEST, UIUtils.HORI, 1.0,
                                0.0);
                fields.add(textField);
            } else if (definitionFields[i].getType().equals(
                    AbstractDefinitionFieldGenerator.BOOLEAN_FIELD)) {
                JCheckBox textField = new JCheckBox();
                l.setLabelFor(textField);
                textField.setSelected(false);
                UIUtils
                        .addComponent(p, textField, 1, i, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0,
                                0.0);
                fields.add(textField);
            }

        }
        // add glue to make our panel look proper
        UIUtils.addComponent(p, Box.createGlue(), 0, numPairs, 2, 1, UIUtils.WEST,
                UIUtils.REMAINDER, 1.0, 1.0);

        p.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(ComponentFactory.createTitledBorder(bundle
                .getString("sadi_definition_editor_title")));
        panel.add(getSadiButtonPanel(), BorderLayout.PAGE_START);
        panel.add(p, BorderLayout.CENTER);

        return panel;
    }

    private JComponent getSadiButtonPanel() {
        JToolBar p = ComponentFactory.createViewToolBar();
        p.setLayout(new FlowLayout(FlowLayout.LEFT));
        // JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        open = new AbstractButton(bundle.getString("open_file"), true, new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                File file = null;
                JFileChooser chooser = UIUtils.getOpenFileChooser(bundle.getString("open"),
                        DEFINITION_DIRECTORY, null);
                if (!manager.getPreference(DEFINITION_DIRECTORY, "").equals(""))
                    chooser.setCurrentDirectory(new File(manager.getPreference(
                            DEFINITION_DIRECTORY, "")));
                int returnVal = chooser.showOpenDialog((JComponent) e.getSource());
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    file = chooser.getSelectedFile();
                }
                if (file == null)
                    return;

                // set the current directory in the preferences
                manager.savePreference(DEFINITION_DIRECTORY, file.getParent());
                Properties properties = new Properties();
                try {
                    properties.load(new FileInputStream(file));
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    JOptionPane.showMessageDialog(getDefinitionViewComponent(), bundle
                            .getString("definition_read_error"), bundle
                            .getString("definition_read_error_title"), JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                    JOptionPane.showMessageDialog(getDefinitionViewComponent(), bundle
                            .getString("definition_read_error"), bundle
                            .getString("definition_read_error_title"), JOptionPane.ERROR_MESSAGE);
                    return;
                }

                for (int i = 0; i < fields.size(); i++) {
                    JComponent jc = fields.get(i);
                    if (properties.containsKey(definitionFields[i].getKey())) {
                        if (jc instanceof JTextComponent) {
                            String value = properties.getProperty(definitionFields[i].getKey())
                                    .trim();
                            if (value.startsWith("\""))
                                value = value.substring(1);
                            if (value.endsWith("\""))
                                value = value.substring(0, value.lastIndexOf("\""));
                            ((JTextComponent) jc).setText(value);
                        } else if (jc instanceof JToggleButton) {
                            String value = properties.getProperty(definitionFields[i].getKey())
                                    .trim();
                            value = value.toLowerCase();
                            ((JToggleButton) jc).setSelected(value.matches("0|false|no") ? false
                                    : true);
                        }
                    }
                }

            }
        });
        reset = new AbstractButton(bundle.getString("reset_file"), true, new ResetActionListener(
                fields.toArray(new JComponent[] {})));
        save = new AbstractButton(bundle.getString("save_file"), true, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // declare / set the fields
                String name = ((JTextComponent) fields.get(0)).getText();
                String authority = ((JTextComponent) fields.get(1)).getText();
                String serviceType = ((JTextComponent) fields.get(2)).getText();
                String inputClass = ((JTextComponent) fields.get(3)).getText();
                String outputClass = ((JTextComponent) fields.get(4)).getText();
                String description = ((JTextComponent) fields.get(5)).getText();
                String uniqueID = ((JTextComponent) fields.get(6)).getText();
                boolean authoritative = ((JCheckBox) fields.get(7)).isSelected();
                String provider = ((JTextComponent) fields.get(8)).getText();
                String serviceURI = ((JTextComponent) fields.get(9)).getText();
                String endpoint = ((JTextComponent) fields.get(10)).getText();
                String signatureURL = ((JTextComponent) fields.get(11)).getText();

                // TODO validate the fields
                if (name.trim().equals("")) {
                    JOptionPane.showMessageDialog(getDefinitionViewComponent(), bundle
                            .getString("definition_validation_name"), bundle
                            .getString("definition_validation_title"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                ServiceDefinition def = new ServiceDefinition(name);
                def.setAuthoritative(authoritative);
                def.setAuthority(authority);
                def.setDescription(description);
                def.setEndpoint(endpoint);
                def.setInputClass(inputClass);
                def.setOutputClass(outputClass);
                def.setProvider(provider);
                def.setServiceType(serviceType);
                def.setServiceURI(serviceURI);
                def.setSignatureURL(signatureURL);
                def.setUniqueID(uniqueID);

                // get chooser for directories
                File dir = null;
                JFileChooser chooser = UIUtils.getOpenDirectoryChooser(bundle
                        .getString("definition_directory"), DEFINITION_DIRECTORY);
                int returnVal = chooser.showOpenDialog((JComponent) e.getSource());
                if (returnVal != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                dir = chooser.getSelectedFile();
                if (dir == null)
                    return;
                if (!dir.canWrite()) {
                    JOptionPane.showMessageDialog(null, bundle
                            .getString("definition_cannot_write_directory"), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
                // set the current directory in the preferences
                manager.savePreference(DEFINITION_DIRECTORY, dir.getAbsolutePath());

                // save the file:
                File outfile = null;
                try {
                    outfile = new File(dir, def.getName());
                } catch (NullPointerException npe) {
                    ErrorLogPanel.showErrorDialog(npe);
                }
                // if it exists, prompt to overwrite
                if (outfile == null)
                    return;
                if (outfile.exists()) {
                    int confirm = JOptionPane.showConfirmDialog(getDefinitionViewComponent(),
                            String
                                    .format(bundle.getString("definition_file_exists"), def
                                            .getName()));
                    if (confirm == JOptionPane.CANCEL_OPTION || confirm == JOptionPane.NO_OPTION) {
                        // tell them to rename the service or choose a
                        // different directory
                        JOptionPane.showMessageDialog(getDefinitionViewComponent(), bundle
                                .getString("definition_rename"));
                        return;
                    }
                }
                try {
                    BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
                    out.write(def.toString());
                    out.close();
                } catch (IOException ioe) {
                    ErrorLogPanel.showErrorDialog(ioe);
                    return;
                }
                // provide feedback that we saved the file and that the
                // file needs to be placed in the definitions directory
                JOptionPane.showMessageDialog(getDefinitionViewComponent(), bundle
                        .getString("definition_saved"));
            }
        });
        p.add(open);
        p.add(save);
        p.add(reset);
        p.setMaximumSize(p.getPreferredSize());
        return p;
    }

    @Override
    final protected OWLEntity updateView() {
        OWLObject cls = getSelectedOWLObject();
        if (cls != null) {
            updateRegisteredActions();
        } else {
            disableRegisteredActions();
        }
        // return null so that our view doesnt say 'Definition Editor:cls'
        return null;
    }

    /*
     * method to get the last selected thing
     */
    private OWLObject getSelectedOWLObject() {
        return getOWLWorkspace().getOWLSelectionModel().getSelectedObject();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.protege.editor.owl.ui.view.AbstractOWLSelectionViewComponent#
     * isOWLClassView()
     */
    protected boolean isOWLClassView() {
        return true;
    }
}
