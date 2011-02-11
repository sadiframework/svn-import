package org.sadiframework.editor.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

import org.jdesktop.swingworker.SwingWorker;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.util.ComponentFactory;
import org.protege.editor.core.ui.util.Icons;
import org.protege.editor.owl.ui.view.individual.AbstractOWLIndividualViewComponent;
import org.sadiframework.exceptions.SADIServiceException;
import org.sadiframework.preferences.PreferenceManager;
import org.sadiframework.service.Execute;
import org.sadiframework.swing.AbstractButton;
import org.sadiframework.swing.JTextFieldWithHistory;
import org.sadiframework.swing.SpringUtilities;
import org.sadiframework.swing.UIUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
//import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class SadiSimpleClientView extends AbstractOWLIndividualViewComponent {

    private static final long serialVersionUID = 6011258260093115589L;

    private PreferenceManager manager = PreferenceManager.newInstance();

    private final ResourceBundle bundle = ResourceBundle
            .getBundle("org.sadiframework.utils.i18n.EditorResourceBundle");

    @SuppressWarnings("rawtypes")
    SwingWorker worker;
    //
    // Some preference keys for this view
    //
    public static final String IS_INPUT_FROM_INDIVIDUAL = "input-from-individual";
    public static final String TESTING_OWL_INDIVIDUAL = "testing-owl-individual";
    public static final String TESTING_OWL_INDIVIDUAL_URI = "testing-owl-individual-uri";
    public static final String TESTING_CURRENT_SAVE_DIR = "testing-current-directory";
    public static final String TESTING_CURRENT_FILE = "testing-current-file";
    public static final String TESTING_SERVICE_ENDPOINT = "testing-service-endpoint";

    // what to show when no owl individual is selected
    private String NO_OWL_INDIVIDUAL = String.format("<html><b>%s</b></html>", bundle
            .getString("testing_input_data_panel_no_owl_individual"));

    // service output area
    private JTextArea resultPane;

    // holder for our service input
    private String serviceInputXML = "";

    // 
    // swing components
    //
    private JButton saveButton, callButton, cancelButton, openXML;
    private JLabel owlLabel;

    // whether or not we allow use owl indiv radio button to be selected by
    // default
    private boolean defaultUseOwlIndivual = false;

    @Override
    public void initialiseIndividualsView() throws Exception {
        setLayout(new BorderLayout(6, 6));

        // set some default params
        manager.saveBooleanPreference(IS_INPUT_FROM_INDIVIDUAL, defaultUseOwlIndivual);
        manager.savePreference(TESTING_OWL_INDIVIDUAL, "");
        manager.savePreference(TESTING_OWL_INDIVIDUAL_URI, "");

        // Lay out the panel.
        JPanel p = new JPanel(new SpringLayout());

        p.add(getServiceInvocationPanel());
        p.add(getInputDataPanel());
        p.add(getServiceResultPanel());

        SpringUtilities.makeCompactGrid(p, 3, 1, // rows, cols
                6, 6, // initX, initY
                6, 6); // xPad, yPad
        p.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(p, BorderLayout.CENTER);
        add(new JScrollPane(panel));
    }

    @Override
    public OWLNamedIndividual updateView(OWLNamedIndividual individual) {
        if (individual != null) {
            manager.savePreference(TESTING_OWL_INDIVIDUAL, individual.getIRI().toString());
            manager.savePreference(TESTING_OWL_INDIVIDUAL_URI, individual.getIRI().toString());
            //render(individual);
        }
        return null;
    }

    // render the class and recursively all of its subclasses
    private void render(OWLNamedIndividual individual, String inputClass, boolean log) {
        if (individual == null || !manager.getBooleanPreference(IS_INPUT_FROM_INDIVIDUAL, true))
            return;
        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

            IRI ontologyIRI = IRI.create("http://sadiframework.org/testing.owl");

            // Now create the ontology - we use the ontology IRI (not the
            // physical URI)
            OWLOntology ontology = manager.createOntology(ontologyIRI);

            for (OWLOntology ont : getOWLModelManager().getActiveOntologies()) {
                if (ont != null) {
                    for (OWLAxiom axiom : individual.getReferencingAxioms(ont)) {
                        if (axiom != null) {
                            AddAxiom addAxiom = new AddAxiom(ontology, axiom);
                            manager.applyChange(addAxiom);
                        }
                    } 
                }
            }
            // add type information to the individual ...
            OWLDataFactory factory = manager.getOWLDataFactory();
            //Set<OWLClassExpression> types = individual.getTypes(ontology);
            if (inputClass != null) {
                //if (types.isEmpty()) {
                if (log) {
                    resultPane.setText(String.format("%s\n%s", resultPane.getText(), "adding rdf:type statement to individual"));
                }
                manager.applyChange(
                        new AddAxiom(
                                ontology,
                                factory.getOWLClassAssertionAxiom(factory.getOWLClass(IRI.create(inputClass)), individual)
                        )
                );
            }
//                } else {
//                    if (log)
//                        resultPane.setText(String.format("%s\n%s", resultPane.getText(), "rdf:type for individual exists; not adding " + inputClass));
//                }
           
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            manager.saveOntology(ontology, new RDFXMLOntologyFormat(), stream);
            serviceInputXML = (stream.toString());

        } catch (OWLOntologyCreationException e) {
            ErrorLogPanel.showErrorDialog(e);
        } catch (OWLOntologyStorageException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    // remove any listeners and perform tidyup (none required in this case)
    public void disposeView() {

    }

    // /////////////////////////////////////////////
    // returns the service input subpanel
    // /////////////////////////////////////////////
    private Component getInputDataPanel() {
        // panel for first radio button
        JPanel panel = new JPanel(new GridBagLayout(), true);
        JTextFieldWithHistory inputXML = new JTextFieldWithHistory(25, TESTING_CURRENT_FILE);
        inputXML.setEditable(false);

        JRadioButton inputFromXML = new JRadioButton(bundle
                .getString("testing_input_data_panel_from_xml"));
        // no label for button (has an icon set below)
        openXML = new AbstractButton("", true, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // this is the action that is invoked when the open file button
                // is clicked on
                String title = bundle.getString("testing_input_data_panel_from_xml");
                String directory = manager.getPreference(TESTING_CURRENT_SAVE_DIR, System
                        .getProperty("user.dir"));
                String description = bundle.getString("xml_file_types");
                JFileChooser chooser = UIUtils.getOpenFileChooser(title, directory, UIUtils
                        .createFileFilter(description, new String[] { ".xml", ".owl", ".rdf" }));
                if (chooser.showOpenDialog((Component) e.getSource()) != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                File file = chooser.getSelectedFile();
                if (file == null || !file.exists())
                    return;
                manager.savePreference(TESTING_CURRENT_SAVE_DIR, file.getParent());
                manager.savePreference(TESTING_CURRENT_FILE, file.getAbsolutePath());
            }
        });
        openXML.setIcon(Icons.getIcon("project.open.gif"));

        // second radio button
        JRadioButton inputFromIndividual = new JRadioButton(bundle
                .getString("testing_input_data_panel_from_individual"));
        owlLabel = new JLabel(NO_OWL_INDIVIDUAL, JLabel.LEADING);
        manager.addPropertyChangeListener(TESTING_OWL_INDIVIDUAL, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                // here we listen for changes to the selected owl individual
                if (manager.getBooleanPreference(IS_INPUT_FROM_INDIVIDUAL, true)) {
                    String name = manager.getPreference(TESTING_OWL_INDIVIDUAL, null);
                    if (name == null)
                        return;
                    owlLabel.setText(String.format("<html><b><i>%s</i></b></html>", name));
                    owlLabel.getParent().validate();
                } else {
                    owlLabel.setText(NO_OWL_INDIVIDUAL);
                    owlLabel.getParent().validate();
                }
            }
        });

        // save button
        saveButton = new AbstractButton(bundle.getString("testing_input_data_panel_save"), manager
                .getBooleanPreference(IS_INPUT_FROM_INDIVIDUAL, true), new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // render the current individual
                String endpoint = manager.getPreference(TESTING_SERVICE_ENDPOINT, "");
                if (!endpoint.equals("")) {
                    render(getSelectedOWLIndividual(), getInputClassFromSignature(endpoint), false);
                } else {
                    render(getSelectedOWLIndividual(), null, false);
                }
                if (serviceInputXML != null && !serviceInputXML.trim().equals("")) {
                    String title = bundle.getString("testing_input_data_panel_save");
                    String directory = manager.getPreference(TESTING_CURRENT_SAVE_DIR, System
                            .getProperty("user.dir"));

                    JFileChooser chooser = UIUtils.getSaveFileChooser(title, directory, null);
                    if (chooser.showSaveDialog((Component) e.getSource()) != JFileChooser.APPROVE_OPTION) {
                        return;
                    }
                    File file = chooser.getSelectedFile();
                    if (file == null)
                        return;
                    manager.savePreference(TESTING_CURRENT_SAVE_DIR, file.getParent());
                    if (file.exists()) {
                        String msg = bundle.getString("testing_input_data_panel_save_file_exists");
                        title = bundle.getString("testing_input_data_panel_save_file_exists_title");
                        if (JOptionPane.showConfirmDialog((Component) e.getSource(), msg, title,
                                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                            return;
                    }
                    try {
                        BufferedWriter out = new BufferedWriter(new FileWriter(file));
                        out.write(serviceInputXML);
                        out.close();
                    } catch (IOException ioe) {
                        ErrorLogPanel.showErrorDialog(ioe);
                    }
                } else {
                    JOptionPane.showMessageDialog(
                            SadiSimpleClientView.this, 
                            bundle.getString("testing_input_data_panel_from_individual_error"),
                            bundle.getString("error"), 
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        });

//        JButton createTestButton = new AbstractButton(bundle
//                .getString("testing_input_data_panel_create_unit_test"), true,
//                new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        UnitTestDialog dialog = new UnitTestDialog();
//                        dialog.setLocationRelativeTo(getParent());
//                        dialog.setVisible(true);
//                    }
//                });
        
        callButton = new AbstractButton(bundle.getString("testing_service_invocation_panel_call"),
                true, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (manager.getPreference(TESTING_SERVICE_ENDPOINT, "").equals("")) {
                            // tell the user to enter an endpoint
                            String msg = bundle.getString("testing_service_invocation_panel_no_endpoint_msg");
                            String title = bundle.getString("error");
                            JOptionPane.showMessageDialog(getTopLevelAncestor(), msg, title,
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        if (!manager.getBooleanPreference(IS_INPUT_FROM_INDIVIDUAL, true)) {
                            if (manager.getPreference(TESTING_CURRENT_FILE, null) == null) {
                                // tell the user to select a file
                                String msg = bundle.getString("testing_service_invocation_panel_no_file_msg");
                                String title = bundle.getString("error");
                                JOptionPane.showMessageDialog(getTopLevelAncestor(), msg, title,
                                        JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                            File file = new File(manager.getPreference(TESTING_CURRENT_FILE, null));
                            try {
                                StringBuilder sb = new StringBuilder();
                                BufferedReader br = new BufferedReader(new FileReader(file));
                                String newline = System.getProperty("line.separator");
                                String line = null;
                                while ((line = br.readLine()) != null) {
                                    sb.append(line + newline);
                                }
                                serviceInputXML = sb.toString();
                            } catch (IOException ioe) {
                                ErrorLogPanel.showErrorDialog(ioe);
                            }
                        } 
//                        else {
//                            if (serviceInputXML == null || serviceInputXML.trim().equals("")) {
//                                // tell the user to select an individual
//                                String msg = bundle.getString("testing_service_invocation_panel_no_ind_msg");
//                                String title = bundle.getString("error");
//                                JOptionPane.showMessageDialog(getTopLevelAncestor(), msg, title,
//                                        JOptionPane.ERROR_MESSAGE);
//                                return;
//                            }
//                        }
                        createWorker();
                        worker.execute();
                    }
                });

        cancelButton = new AbstractButton(bundle.getString("cancel"), false, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                worker.cancel(true);
                cancelButton.setEnabled(false);
                callButton.setEnabled(true);
                resultPane.setText(bundle.getString("testing_service_invocation_panel_cancel_msg"));
                redraw();
            }
        });

        // place radio button in a group
        ButtonGroup group = new ButtonGroup();
        group.add(inputFromXML);
        group.add(inputFromIndividual);

        // add event handlers for each event
        inputFromXML.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                manager.saveBooleanPreference(IS_INPUT_FROM_INDIVIDUAL, false);
                saveButton.setEnabled(false);
                openXML.setEnabled(true);
                updateView(getSelectedOWLIndividual());
            }
        });
        inputFromIndividual.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                manager.saveBooleanPreference(IS_INPUT_FROM_INDIVIDUAL, true);
                serviceInputXML = "";
                updateView(getSelectedOWLIndividual());
                saveButton.setEnabled(true);
                openXML.setEnabled(false);

            }
        });

        // set the default for the group
        if (manager.getBooleanPreference(IS_INPUT_FROM_INDIVIDUAL, defaultUseOwlIndivual))
            inputFromIndividual.setSelected(true);
        else
            inputFromXML.setSelected(true);

        // add our components to the panel
        UIUtils
                .addComponent(panel, inputFromXML, 0, 0, 1, 1, UIUtils.NWEST, UIUtils.NONE, 0.0,
                        0.0);
        UIUtils.addComponent(panel, inputXML, 1, 0, 2, 1, UIUtils.NWEST, UIUtils.HORI, 1.0, 0.0);
        UIUtils.addComponent(panel, openXML, 3, 0, 1, 1, UIUtils.NWEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(panel, inputFromIndividual, 0, 1, 1, 1, UIUtils.NWEST, UIUtils.NONE,
                0.0, 0.0);
        UIUtils.addComponent(panel, owlLabel, 1, 1, 2, 1, UIUtils.NWEST, UIUtils.HORI, 1.0, 0.0);
//        UIUtils.addComponent(panel, UIUtils.createButtonPanel(new JButton[] { saveButton,
//                createTestButton }), 0, 2, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(panel, UIUtils.createButtonPanel(new JButton[] { callButton, cancelButton, saveButton,}), 0, 2, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(panel, Box.createGlue(), 0, 2, 2, 1, UIUtils.WEST, UIUtils.BOTH, 1.0,
                1.0);

        panel.setBorder(ComponentFactory.createTitledBorder(bundle
                .getString("testing_input_data_panel_title")));
        panel.setMaximumSize(panel.getPreferredSize());
        return panel;
    }

    // /////////////////////////////////////////////
    // returns the service results subpanel
    // /////////////////////////////////////////////
    private Component getServiceResultPanel() {
        JPanel panel = new JPanel(new SpringLayout());
        resultPane = UIUtils.createArea("", false);
        panel.add(new JScrollPane(resultPane));
        // Lay out the panel.
        SpringUtilities.makeCompactGrid(panel, 1, 1, // rows, cols
                6, 6, // initX, initY
                6, 6); // xPad, yPad
        panel.setBorder(ComponentFactory.createTitledBorder(bundle
                .getString("testing_service_result_panel_title")));
        return new JScrollPane(panel);
    }

    // /////////////////////////////////////////////
    // returns the service invocation subpanel
    // /////////////////////////////////////////////
    private Component getServiceInvocationPanel() {

        JPanel panel = new JPanel(new GridBagLayout(), true);

        JLabel l = new JLabel(bundle.getString("testing_service_invocation_panel_endpoint") + ":");
        JTextFieldWithHistory serviceEndpoint = new JTextFieldWithHistory(25,
                TESTING_SERVICE_ENDPOINT);
        l.setLabelFor(serviceEndpoint);
//        callButton = new AbstractButton(bundle.getString("testing_service_invocation_panel_call"),
//                true, new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        if (manager.getPreference(TESTING_SERVICE_ENDPOINT, "").equals("")) {
//                            // tell the user to enter an endpoint
//                            String msg = bundle.getString("testing_service_invocation_panel_no_endpoint_msg");
//                            String title = bundle.getString("error");
//                            JOptionPane.showMessageDialog(getTopLevelAncestor(), msg, title,
//                                    JOptionPane.ERROR_MESSAGE);
//                            return;
//                        }
//                        if (!manager.getBooleanPreference(IS_INPUT_FROM_INDIVIDUAL, true)) {
//                            if (manager.getPreference(TESTING_CURRENT_FILE, null) == null) {
//                                // tell the user to select a file
//                                String msg = bundle.getString("testing_service_invocation_panel_no_file_msg");
//                                String title = bundle.getString("error");
//                                JOptionPane.showMessageDialog(getTopLevelAncestor(), msg, title,
//                                        JOptionPane.ERROR_MESSAGE);
//                                return;
//                            }
//                            File file = new File(manager.getPreference(TESTING_CURRENT_FILE, null));
//                            try {
//                                StringBuilder sb = new StringBuilder();
//                                BufferedReader br = new BufferedReader(new FileReader(file));
//                                String newline = System.getProperty("line.separator");
//                                String line = null;
//                                while ((line = br.readLine()) != null) {
//                                    sb.append(line + newline);
//                                }
//                                serviceInputXML = sb.toString();
//                            } catch (IOException ioe) {
//                                ErrorLogPanel.showErrorDialog(ioe);
//                            }
//                        } else {
//                            if (serviceInputXML == null || serviceInputXML.trim().equals("")) {
//                                // tell the user to select an individual
//                                String msg = bundle.getString("testing_service_invocation_panel_no_ind_msg");
//                                String title = bundle.getString("error");
//                                JOptionPane.showMessageDialog(getTopLevelAncestor(), msg, title,
//                                        JOptionPane.ERROR_MESSAGE);
//                                return;
//                            }
//                        }
//                        createWorker();
//                        worker.execute();
//                    }
//                });
//
//        cancelButton = new AbstractButton(bundle.getString("cancel"), false, new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                worker.cancel(true);
//                cancelButton.setEnabled(false);
//                callButton.setEnabled(true);
//                resultPane.setText(bundle.getString("testing_service_invocation_panel_cancel_msg"));
//                redraw();
//            }
//        });
//
//        JPanel bp = UIUtils.createButtonPanel(new JButton[]{callButton, cancelButton });
        UIUtils.addComponent(panel, l,               0, 0, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(panel, serviceEndpoint, 1, 0, 2, 1, UIUtils.WEST, UIUtils.BOTH, 1.0,0.0);
        //UIUtils.addComponent(panel, bp,              0, 1, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);

        panel.setBorder(ComponentFactory.createTitledBorder(bundle
                .getString("testing_service_invocation_panel_title")));
        panel.setMaximumSize(panel.getPreferredSize());
        return panel;
    }

    @SuppressWarnings("rawtypes")
    private void createWorker() {
        worker = new SwingWorker() {
            public void setup() {
                resultPane
                        .setText(bundle.getString("testing_service_invocation_panel_running_msg"));
                redraw();
                callButton.setEnabled(false);
                cancelButton.setEnabled(true);
            }

            @Override
            protected void done() {
                try {
                    resultPane.setText(get().toString().trim());
                    resultPane.setCaretPosition(0);
                } catch (Exception ignore) {
                }
                redraw();
                callButton.setEnabled(true);
                cancelButton.setEnabled(false);
            }

            @Override
            protected Object doInBackground() throws Exception {
                setup();
                String text = "";
                try {
                    String endpoint = manager.getPreference(TESTING_SERVICE_ENDPOINT, "");
                    if (manager.getBooleanPreference(IS_INPUT_FROM_INDIVIDUAL, defaultUseOwlIndivual)) {
                        resultPane.setText(String.format("%s\n%s", resultPane.getText(), "Resolving input class from service signature"));
                        String inputClass = getInputClassFromSignature(endpoint);
                        if (inputClass != null) {
                            resultPane.setText(String.format("%s\n%s", resultPane.getText(), "found potential rdf:type for individual of " + inputClass));
                        }
                        render(getSelectedOWLIndividual(), inputClass, true);
                    }
                    
                    String xml = serviceInputXML;
                    text = Execute.executeCgiService(endpoint, xml);
                    if (worker.isCancelled()) {
                        text = bundle.getString("testing_service_invocation_panel_cancel_msg");
                    }
                } catch (SADIServiceException se) {
                    ErrorLogPanel.showErrorDialog(se);
                    text = se.getMessage();
                }
                return text;
            }
        };
    }
    
    private String getInputClassFromSignature(String endpoint) {
        OWLOntologyManager ontManager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = null;
        try {
            ontology = ontManager.loadOntologyFromOntologyDocument(new URL(endpoint).openStream());
        } catch (OWLOntologyCreationException e) {
            
        } catch (MalformedURLException e) {
            
        } catch (IOException e) {
            
        }
        if (ontology != null)
            for (OWLIndividual individual : ontology.getIndividualsInSignature()) {
                Set<OWLObjectPropertyAssertionAxiom> inputParameters = ontology.getObjectPropertyAssertionAxioms(individual);
                for (OWLObjectPropertyAssertionAxiom in : inputParameters) {
                    if (in.getProperty().toString().equals("<http://www.mygrid.org.uk/mygrid-moby-service#inputParameter>")) {
                        if (in.getObject() != null) {
                            OWLIndividual i = in.getObject();
                            for (OWLObjectPropertyAssertionAxiom prop : ontology.getObjectPropertyAssertionAxioms(i)) {
                                return prop.getObject().toStringID();
                            }
                        }
                    }
                }
            }
        resultPane.setText(String.format("%s\n%s", resultPane.getText(), "failed to retrieve a type for the individual ..."));
        return null;
    }

    private void redraw() {
        validate();
        updateUI();
    }
}
