package org.sadiframework.editor.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;


import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.util.ComponentFactory;
import org.protege.editor.core.ui.util.Icons;
import org.protege.editor.owl.ui.view.cls.AbstractOWLClassViewComponent;
import org.sadiframework.generator.perl.DatatypeGeneratorPerlWorker;
import org.sadiframework.generator.perl.Generator;
import org.sadiframework.generator.perl.ServiceGeneratorPerlWorker;
import org.sadiframework.preferences.PreferenceManager;
import org.sadiframework.swing.AbstractButton;
import org.sadiframework.swing.JTextFieldWithHistory;
import org.sadiframework.swing.SpringUtilities;
import org.sadiframework.swing.UIUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class SadiServiceGeneratorView extends AbstractOWLClassViewComponent {

    private static final long serialVersionUID = 6011258260093115589L;

    // generator privates
    private final ResourceBundle bundle = ResourceBundle
            .getBundle("org.sadiframework.utils.i18n.EditorResourceBundle");
    private PreferenceManager manager = PreferenceManager.newInstance();
    private String selectedClassAsString = "";

    // generator preference keys
    public static final String DO_SERVICE_GENERATION = "service-generator-processing";
    public static final String DO_DATATYPE_GENERATION = "datatype-generator-processing";
    
    // PERL vars
    //preference keys
    public static final String PERL_GENERATOR_SERVICE_NAME = "perl-generator-service-name";
    public static final String PERL_GENERATOR_SERVICE_ASYNC = "perl-generator-service-async";
    public static final String PERL_GENERATOR_OWL_ONT_FILENAME = "perl-generator-owl-ont-filename";
    public static final String PERL_GENERATOR_OWL_BY_FILE = "perl-generator-owl-by-file";
    public static final String PERL_GENERATOR_OWL_CLASS = "perl-generator-owl-class";

    // what to show when no owl individual is selected
    private String NO_OWL_INDIVIDUAL = String.format("<html><b>%s</b></html>", bundle
            .getString("testing_input_data_panel_no_owl_individual"));
    private JButton perlGenerateBtn, perlCancelGenBtn, perlGenOWLBtn, perlCancelGenOWLBtn,
            perlOpenOnt;
    private JLabel owlLabel;
    private ServiceGeneratorPerlWorker perlServiceWorker;
    private DatatypeGeneratorPerlWorker perlDatatypeWorker;

    // JAVA privates

    @Override
    public void initialiseClassView() throws Exception {
        setLayout(new BorderLayout(6, 6));

        // Lay out the panel.
        JPanel p = new JPanel(new SpringLayout());

        p.add(getPerlPanel());
        p.add(getJavaPanel());

        SpringUtilities.makeCompactGrid(p, 2, 1, // rows, cols
                6, 6, // initX, initY
                6, 6); // xPad, yPad
        p.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(p, BorderLayout.CENTER);
        add(new JScrollPane(panel));

    }

    @Override
    protected OWLClass updateView(OWLClass selectedClass) {
        if (selectedClass != null) {
            manager.savePreference(PERL_GENERATOR_OWL_CLASS, selectedClass.getIRI().toString());
            render(getSelectedOWLClass());
        }
        return null;
    }

    // render the class and recursively all of its subclasses
    private void render(OWLClass owlClass) {
        if (owlClass == null || manager.getBooleanPreference(PERL_GENERATOR_OWL_BY_FILE, true))
            return;
        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

            IRI ontologyIRI = IRI.create("http://sadiframework.org/generator.owl");

            // Now create the ontology - we use the ontology IRI (not the
            // physical URI)
            OWLOntology ontology = manager.createOntology(ontologyIRI);

            for (OWLOntology ont : getOWLModelManager().getActiveOntologies()) {
                if (ont != null) {
                    for (OWLAxiom axiom : owlClass.getReferencingAxioms(ont)) {
                        if (axiom != null) {
                            AddAxiom addAxiom = new AddAxiom(ontology, axiom);
                            manager.applyChange(addAxiom);
                        }
                    }
                }
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            manager.saveOntology(ontology, new RDFXMLOntologyFormat(), stream);
            selectedClassAsString = (stream.toString());

        } catch (OWLOntologyCreationException e) {
            ErrorLogPanel.showErrorDialog(e);
        } catch (OWLOntologyStorageException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    @Override
    public void disposeView() {

    }

    private JPanel getPerlPanel() {
        JPanel mainPanel = new JPanel(true);
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(ComponentFactory.createTitledBorder(bundle
                .getString("sadi_generator_perl_title")));

        // perl panel will have:

        // Generate Service sub panel
        // -> generate sync/async service
        JPanel servicePanel = new JPanel(new GridBagLayout(), true);
        servicePanel.setAlignmentX(LEFT_ALIGNMENT);
        servicePanel.setBorder(BorderFactory.createTitledBorder(bundle
                .getString("sadi_generator_perl_service_title")));
        JLabel label = new JLabel(bundle.getString("sadi_generator_perl_service_name"));
        JTextFieldWithHistory service = new JTextFieldWithHistory(25, PERL_GENERATOR_SERVICE_NAME);
        label.setLabelFor(service);
                
        perlGenerateBtn = new AbstractButton(bundle.getString("generate"), true,
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        manager.saveBooleanPreference(DO_SERVICE_GENERATION, true);
                        perlServiceWorker = new ServiceGeneratorPerlWorker();
                        perlServiceWorker.execute();
                    }
                });

        perlCancelGenBtn = new AbstractButton(bundle.getString("cancel"), false,
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        manager.saveBooleanPreference(DO_SERVICE_GENERATION, false);
                    }
                });

        // create radio buttons to indicate flavour of service to generate
        JRadioButton sync = new JRadioButton(bundle.getString("sync"));
        JRadioButton async = new JRadioButton(bundle.getString("async"));
        sync.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manager.saveBooleanPreference(PERL_GENERATOR_SERVICE_ASYNC, false);
            }
        });
        async.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manager.saveBooleanPreference(PERL_GENERATOR_SERVICE_ASYNC, true);
            }
        });

        // create a button group
        ButtonGroup group = new ButtonGroup();
        group.add(sync);
        group.add(async);
        if (manager.getBooleanPreference(PERL_GENERATOR_SERVICE_ASYNC, false)) {
            async.setSelected(true);
        } else {
            sync.setSelected(true);
        }
        
     // what to do when the property DO_SERVICE_GENERATION property changes
        manager.addPropertyChangeListener(DO_SERVICE_GENERATION, new PropertyChangeListener() {
            
            public void propertyChange(PropertyChangeEvent evt) {
                // start our generation
                if (manager.getBooleanPreference(DO_SERVICE_GENERATION, false)) {
                    perlGenerateBtn.setEnabled(false);
                    perlCancelGenBtn.setEnabled(true);
                } else {
                    // task is cancelled or we are finished
                    perlGenerateBtn.setEnabled(true);
                    perlCancelGenBtn.setEnabled(false);
                    if (perlServiceWorker != null && perlServiceWorker.isDone() && !perlServiceWorker.isCancelled()) {
                        String s = "";
                        try {
                            if (perlServiceWorker.get() instanceof String) {
                                s = (String) perlServiceWorker.get();
                                // TODO - show this string to the user
                                System.out.println(s);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                    perlServiceWorker = null;
                }
            }
        });
        
        // add the components
        UIUtils.addComponent(servicePanel, label, 0, 0, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(servicePanel, service, 1, 0, 2, 1, UIUtils.WEST, UIUtils.BOTH, 1.0,
                0.0);
        UIUtils.addComponent(servicePanel, sync, 0, 1, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(servicePanel, async, 0, 2, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(servicePanel, UIUtils.createButtonPanel(new JButton[] {
                perlGenerateBtn, perlCancelGenBtn }), 0, 3, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0,
                0.0);

        // Generate datatype sub panel
        // -> generate by choosing owl class OR specify URL to ontology
        JPanel datatypePanel = new JPanel(new GridBagLayout(), true);
        datatypePanel.setAlignmentX(LEFT_ALIGNMENT);
        datatypePanel.setBorder(BorderFactory.createTitledBorder(bundle
                .getString("sadi_generator_perl_datatype_title")));
        JRadioButton byFile = new JRadioButton(bundle
                .getString("sadi_generator_perl_datatype_by_file"));
        JRadioButton byClass = new JRadioButton(bundle
                .getString("sadi_generator_perl_datatype_by_class"));
        JTextFieldWithHistory ontology = new JTextFieldWithHistory(25,
                PERL_GENERATOR_OWL_ONT_FILENAME);
        ButtonGroup dGroup = new ButtonGroup();
        dGroup.add(byFile);
        dGroup.add(byClass);
        owlLabel = new JLabel(NO_OWL_INDIVIDUAL, JLabel.LEADING);
        manager.addPropertyChangeListener(PERL_GENERATOR_OWL_CLASS, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                // here we listen for changes to the selected owl individual
                if (!manager.getBooleanPreference(PERL_GENERATOR_OWL_BY_FILE, true)) {
                    String name = manager.getPreference(PERL_GENERATOR_OWL_CLASS, null);
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
        byFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manager.saveBooleanPreference(PERL_GENERATOR_OWL_BY_FILE, true);
                perlOpenOnt.setEnabled(true);
                updateView(getSelectedOWLClass());
            }
        });
        byClass.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manager.saveBooleanPreference(PERL_GENERATOR_OWL_BY_FILE, false);
                perlOpenOnt.setEnabled(false);
                updateView(getSelectedOWLClass());
            }
        });
        perlOpenOnt = new AbstractButton("", true, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // this is the action that is invoked when the open file button
                // is clicked on
                String title = bundle.getString("open");
                String directory = new File(manager.getPreference(PERL_GENERATOR_OWL_ONT_FILENAME,
                        "__FOO__")).getParent();
                String description = bundle.getString("xml_file_types");
                JFileChooser chooser = UIUtils.getOpenFileChooser(title, directory == null ? System
                        .getProperty("user.dir") : directory, UIUtils.createFileFilter(description,
                        new String[] { ".xml", ".owl", ".rdf" }));
                if (chooser.showOpenDialog((Component) e.getSource()) != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                File file = chooser.getSelectedFile();
                if (file == null || !file.exists())
                    return;
                manager.savePreference(PERL_GENERATOR_OWL_ONT_FILENAME, file.getAbsolutePath());
            }
        });
        perlOpenOnt.setIcon(Icons.getIcon("project.open.gif"));

        // set the selected radio button
        byFile.setSelected(manager.getBooleanPreference(PERL_GENERATOR_OWL_BY_FILE, true));
        byClass.setSelected(!manager.getBooleanPreference(PERL_GENERATOR_OWL_BY_FILE, true));
        perlOpenOnt.setEnabled(manager.getBooleanPreference(PERL_GENERATOR_OWL_BY_FILE, true));

        perlGenOWLBtn = new AbstractButton(bundle.getString("generate"), true,
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        String ontFilename = "";
                        if (manager.getBooleanPreference(PERL_GENERATOR_OWL_BY_FILE, true)) {
                            // generate code for file ontology
                            ontFilename = manager
                                    .getPreference(PERL_GENERATOR_OWL_ONT_FILENAME, "");
                            try {
                                ontFilename = new File(ontFilename).toURI().toURL().toString();
                            } catch (MalformedURLException e1) {
                                ErrorLogPanel.showErrorDialog(e1);
                                manager.saveBooleanPreference(DO_DATATYPE_GENERATION, false);
                                return;
                            }
                        } else {
                            // generate owl for specific node
                            // save the ontology to a temp file ...
                            try {
                                // Create temp file.
                                File temp = File.createTempFile("SADI_PERL_ONT_TMP-", ".owl");
                                ontFilename = temp.toURI().toURL().toString();
                                // Delete temp file when program exits.
                                temp.deleteOnExit();
                                // Write to temp file
                                BufferedWriter out = new BufferedWriter(new FileWriter(temp));
                                out.write(selectedClassAsString);
                                out.close();
                            } catch (IOException ioe) {
                                ErrorLogPanel.showErrorDialog(ioe);
                                manager.saveBooleanPreference(DO_DATATYPE_GENERATION, false);
                                return;
                            }
                        }
                        manager.saveBooleanPreference(DO_DATATYPE_GENERATION, true);
                        perlDatatypeWorker = new DatatypeGeneratorPerlWorker(ontFilename);
                        perlDatatypeWorker.execute();
                    }
                });

        perlCancelGenOWLBtn = new AbstractButton(bundle.getString("cancel"), false,
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        manager.saveBooleanPreference(DO_DATATYPE_GENERATION, false);
                    }
                });
        
        manager.addPropertyChangeListener(DO_DATATYPE_GENERATION, new PropertyChangeListener() {
            
            public void propertyChange(PropertyChangeEvent evt) {                
             // start our generation
                if (manager.getBooleanPreference(DO_DATATYPE_GENERATION, false)) {
                    perlGenOWLBtn.setEnabled(false);
                    perlCancelGenOWLBtn.setEnabled(true);
                } else {
                    // task is cancelled or we are finished
                    perlGenOWLBtn.setEnabled(true);
                    perlCancelGenOWLBtn.setEnabled(false);
                    if (perlDatatypeWorker != null && perlDatatypeWorker.isDone() && !perlDatatypeWorker.isCancelled()) {
                        String s = "";
                        try {
                            if (perlDatatypeWorker.get() instanceof String) {
                                s = (String) perlDatatypeWorker.get();
                                // TODO - show this string to the user
                                System.out.println(s);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                    perlDatatypeWorker = null;
                }
            }
        });
        
        // add the components to the datatype panel
        UIUtils.addComponent(datatypePanel, byFile, 0, 0, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0,
                0.0);
        UIUtils.addComponent(datatypePanel, ontology, 1, 0, 2, 1, UIUtils.WEST, UIUtils.BOTH, 1.0,
                0.0);
        UIUtils.addComponent(datatypePanel, perlOpenOnt, 3, 0, 1, 1, UIUtils.WEST, UIUtils.NONE,
                0.0, 0.0);
        UIUtils.addComponent(datatypePanel, byClass, 0, 1, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0,
                0.0);
        UIUtils.addComponent(datatypePanel, owlLabel, 1, 1, 2, 1, UIUtils.WEST, UIUtils.BOTH, 1.0,
                0.0);
        UIUtils.addComponent(datatypePanel, UIUtils.createButtonPanel(new JButton[] {
                perlGenOWLBtn, perlCancelGenOWLBtn }), 0, 2, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0,
                0.0);
        // set the preferred sizes
        datatypePanel.setMaximumSize(datatypePanel.getPreferredSize());
        datatypePanel.setMaximumSize(datatypePanel.getPreferredSize());

        // add to mainPanel
        UIUtils.addComponent(mainPanel, servicePanel, 0, 0, 2, 1, UIUtils.NWEST, UIUtils.HORI, 5.0,
                0.0);
        UIUtils.addComponent(mainPanel, datatypePanel, 0, 1, 2, 1, UIUtils.NWEST, UIUtils.HORI,
                5.0, 0.0);
        UIUtils.addComponent(mainPanel, Box.createVerticalGlue(), 0, 2, 2, 1, UIUtils.NWEST,
                UIUtils.BOTH, 5.0, 0.0);
        // set the preferred size
        mainPanel.setMaximumSize(mainPanel.getPreferredSize());
        return mainPanel;

    }

    private JPanel getJavaPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(ComponentFactory.createTitledBorder(bundle
                .getString("sadi_generator_java_title")));
        return panel;

    }
}
