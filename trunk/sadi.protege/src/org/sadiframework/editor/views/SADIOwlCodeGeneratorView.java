package org.sadiframework.editor.views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
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

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.util.Icons;
import org.protege.editor.owl.ui.view.cls.AbstractOWLClassViewComponent;
import org.sadiframework.generator.perl.DatatypeGeneratorPerlWorker;
import org.sadiframework.preferences.PreferenceManager;
import org.sadiframework.properties.SADIProperties;
import org.sadiframework.swing.AbstractButton;
import org.sadiframework.swing.JTextFieldWithHistory;
import org.sadiframework.swing.LoggingWindowFrame;
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

public class SADIOwlCodeGeneratorView extends AbstractOWLClassViewComponent {

    private static final long serialVersionUID = 1163429806964693423L;
    private final ResourceBundle bundle = ResourceBundle.getBundle("org.sadiframework.utils.i18n.EditorResourceBundle");
    private JButton generateOWLBtn, openOntologyBtn;
    private JLabel owlLabel;
    private String NO_OWL_INDIVIDUAL = String.format("<html><b>%s</b></html>", bundle.getString("testing_input_data_panel_no_owl_individual"));
    private DatatypeGeneratorPerlWorker perlDatatypeWorker;
    
    private PreferenceManager manager = PreferenceManager.newInstance();
    private String selectedClassAsString = "";
    // our main scroll pane; add/remove components to this
    private PropertyChangeListener pListener;
    private LoggingWindowFrame console;
    
    
    @Override
    public void initialiseClassView() throws Exception {
        if (pListener == null) {
            pListener = new GeneratorPropertyListener();
            manager.addPropertyChangeListener(pListener);
        }
        JPanel mainPane = new JPanel(new GridBagLayout(), true);
        mainPane.setAlignmentX(LEFT_ALIGNMENT);
        mainPane.setAlignmentY(TOP_ALIGNMENT);
        UIUtils.addComponent(mainPane, getGenerateOwlGeneratorPanel(), 0, 0, 1, 1, UIUtils.NWEST, UIUtils.HORI, 1.0, 1.0);
        setLayout(new BorderLayout(6, 6));
        setAlignmentY(TOP_ALIGNMENT);
        setAlignmentX(LEFT_ALIGNMENT);
        add(new JScrollPane(mainPane), BorderLayout.CENTER);
    }

    @Override
    protected OWLClass updateView(OWLClass selectedClass) {
        if (selectedClass != null) {
            // save the selected class for later use
            manager.savePreference(SADIProperties.GEN_SELECTED_OWL_CLASS, selectedClass.getIRI().toString());
            render(getSelectedOWLClass());
        }
        return null;
    }
    
    // render the class and recursively all of its subclasses
    private void render(OWLClass owlClass) {
        // if nothing selected OR we are generating OWL from File, ignore this
        if (owlClass == null
                || (manager.getBooleanPreference(SADIProperties.GENERATOR_OWL_BY_FILE, true) && manager
                        .getBooleanPreference(SADIProperties.DATATYPE_GEN_USE_PERL, true))
                || (manager.getBooleanPreference(SADIProperties.GENERATOR_OWL_BY_FILE, true) && !manager
                        .getBooleanPreference(SADIProperties.DATATYPE_GEN_USE_PERL, true))) {
            return;
        }
        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

            IRI ontologyIRI = IRI.create("http://sadiframework.org/generator.owl");

            // Now create the ontology - we use the ontology IRI (not the
            // physical URI)
            OWLOntology ontology = manager.createOntology(ontologyIRI);

            for (OWLOntology ont : getOWLModelManager().getOntologies()) {
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
            setSelectedClassAsString((stream.toString()));

        } catch (OWLOntologyCreationException e) {
            ErrorLogPanel.showErrorDialog(e);
        } catch (OWLOntologyStorageException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    @Override
    public void disposeView() {
        // some cleanup here ...
        removePropertyChangeListener(pListener);
    }
    
    public void setSelectedClassAsString(String selectedClassAsString) {
        this.selectedClassAsString = selectedClassAsString;
    }

    public String getSelectedClassAsString() {
        return selectedClassAsString;
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

    private JPanel getGenerateOwlGeneratorPanel() {
        JPanel datatypePanel = new JPanel(new GridBagLayout(), true);
        datatypePanel.setAlignmentX(LEFT_ALIGNMENT);
        datatypePanel.setAlignmentY(TOP_ALIGNMENT);
        datatypePanel.setBorder(BorderFactory.createTitledBorder(bundle
                .getString("sadi_generator_perl_datatype_title")));
        JRadioButton byFile = new JRadioButton(bundle
                .getString("sadi_generator_perl_datatype_by_file"));
        JRadioButton byClass = new JRadioButton(bundle
                .getString("sadi_generator_perl_datatype_by_class"));
        JTextFieldWithHistory ontology = new JTextFieldWithHistory(25,SADIProperties.GENERATOR_OWL_ONT_FILENAME);
        ontology.setEditable(false);
        ButtonGroup dGroup = new ButtonGroup();
        dGroup.add(byFile);
        dGroup.add(byClass);
        owlLabel = new JLabel(NO_OWL_INDIVIDUAL, JLabel.LEADING);
        
        byFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manager.saveBooleanPreference(SADIProperties.GENERATOR_OWL_BY_FILE, true);
                openOntologyBtn.setEnabled(true);
                updateView(getSelectedOWLClass());
            }
        });
        byClass.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manager.saveBooleanPreference(SADIProperties.GENERATOR_OWL_BY_FILE, false);
                openOntologyBtn.setEnabled(false);
                updateView(getSelectedOWLClass());
            }
        });
        openOntologyBtn = new AbstractButton("", true, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // this is the action that is invoked when the open file button
                // is clicked on
                String title = bundle.getString("open");
                String description = bundle.getString("xml_file_types");
                JFileChooser chooser = UIUtils.getOpenFileChooser(
                        title, 
                        SADIProperties.GENERATOR_OWL_ONT_FILENAME, 
                        UIUtils.createFileFilter(description,new String[] { ".xml", ".owl", ".rdf" })
                );
                if (chooser.showOpenDialog((Component) e.getSource()) != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                File file = chooser.getSelectedFile();
                if (file == null || !file.exists())
                    return;
                manager.savePreference(SADIProperties.GENERATOR_OWL_ONT_FILENAME, file.getAbsolutePath());
            }
        });
        openOntologyBtn.setIcon(Icons.getIcon("project.open.gif"));

        // set the selected radio button
        byFile.setSelected(manager.getBooleanPreference(SADIProperties.GENERATOR_OWL_BY_FILE, true));
        byClass.setSelected(!manager.getBooleanPreference(SADIProperties.GENERATOR_OWL_BY_FILE, true));
        openOntologyBtn.setEnabled(manager.getBooleanPreference(SADIProperties.GENERATOR_OWL_BY_FILE, true));

        generateOWLBtn = new AbstractButton(bundle.getString("generate"), true,
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        String ontFilename = "";
                        if (manager.getBooleanPreference(SADIProperties.GENERATOR_OWL_BY_FILE, true)) {
                            // generate code for file ontology
                            ontFilename = manager.getPreference(SADIProperties.GENERATOR_OWL_ONT_FILENAME, "");
                            // check that filename is not empty
                            if (ontFilename == null || ontFilename.trim().equals("")) {
                                String msg = bundle.getString("sadi_generator_perl_datatype_empty");
                                String title = bundle.getString("error");
                                JOptionPane.showMessageDialog(getTopLevelAncestor(), msg, title,
                                        JOptionPane.ERROR_MESSAGE);
                                manager.saveBooleanPreference(SADIProperties.GENERATOR_GENERATE_OWL, false);
                                return;
                            }
                            try {
                                ontFilename = new File(ontFilename).toURI().toURL().toString();
                            } catch (MalformedURLException e1) {
                                ErrorLogPanel.showErrorDialog(e1);
                                manager.saveBooleanPreference(SADIProperties.GENERATOR_GENERATE_OWL, false);
                                return;
                            }
                        } else {
                            // generate owl for specific node
                            // save the ontology to a temp file ...
                            if (selectedClassAsString == null
                                    || selectedClassAsString.trim().equals("")) {
                                String msg = bundle.getString("sadi_generator_perl_datatype_empty");
                                String title = bundle.getString("error");
                                JOptionPane.showMessageDialog(getTopLevelAncestor(), msg, title,
                                        JOptionPane.ERROR_MESSAGE);
                                manager.saveBooleanPreference(SADIProperties.GENERATOR_GENERATE_OWL, false);
                                return;
                            }
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
                                manager.saveBooleanPreference(SADIProperties.GENERATOR_GENERATE_OWL, false);
                                return;
                            }
                        }
                        manager.savePreference(SADIProperties.GENERATOR_OWL_TMP_FILE_LOCATION, ontFilename);
                        manager.saveBooleanPreference(SADIProperties.GENERATOR_GENERATE_OWL, true);
                    }
                });
        
        JRadioButton javaRB = new JRadioButton("Java");
        JRadioButton perlRB = new JRadioButton("Perl");
        javaRB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manager.saveBooleanPreference(SADIProperties.DATATYPE_GEN_USE_PERL, false);
            }
        });
        perlRB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manager.saveBooleanPreference(SADIProperties.DATATYPE_GEN_USE_PERL, true);
            }
        });

        // create a button group for java or perl
        ButtonGroup group = new ButtonGroup();
        group.add(javaRB);
        group.add(perlRB);
        if (manager.getBooleanPreference(SADIProperties.DATATYPE_GEN_USE_PERL, true)) {
            perlRB.setSelected(true);
        } else {
            javaRB.setSelected(true);
        }
        
        JPanel rbPanel = new JPanel(new FlowLayout(FlowLayout.LEADING), true);
        rbPanel.add(javaRB);
        rbPanel.add(perlRB);
        
        // add the components to the datatype panel
        UIUtils.addComponent(datatypePanel, byFile, 0, 0, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0,
                0.0);
        UIUtils.addComponent(datatypePanel, ontology, 1, 0, 2, 1, UIUtils.WEST, UIUtils.BOTH, 1.0,
                0.0);
        UIUtils.addComponent(datatypePanel, openOntologyBtn, 3, 0, 1, 1, UIUtils.WEST, UIUtils.NONE,
                0.0, 0.0);
        UIUtils.addComponent(datatypePanel, byClass, 0, 1, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0,
                0.0);
        UIUtils.addComponent(datatypePanel, owlLabel, 1, 1, 2, 1, UIUtils.WEST, UIUtils.BOTH, 1.0,
                0.0);
        UIUtils.addComponent(datatypePanel, rbPanel, 0, 2, 2, 1, UIUtils.NWEST, UIUtils.NONE, 1.0, 1.0);
        UIUtils.addComponent(datatypePanel, UIUtils.createButtonPanel(new JButton[] {
                generateOWLBtn, }), 0, 3, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0,
                0.0);
        // add glue to make our panel look proper
        //UIUtils.addComponent(datatypePanel, Box.createGlue(), 0, 3, 2, 1, UIUtils.NWEST, UIUtils.REMAINDER, 0.0, 1.0);
        return datatypePanel;
    }
    
    // inner class for listening to changes in our properties
    private class GeneratorPropertyListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            String key = evt.getPropertyName();
            if (key.equals(SADIProperties.DATATYPE_GEN_USE_PERL)) {
                // dont do anything
            } else if (key.equals(SADIProperties.GEN_SELECTED_OWL_CLASS)) {
                // here we listen for changes to the selected owl individual
                if (!manager.getBooleanPreference(SADIProperties.GENERATOR_OWL_BY_FILE, true)) {
                    String name = manager.getPreference(SADIProperties.GEN_SELECTED_OWL_CLASS, null);
                    if (name == null)
                        return;
                    owlLabel.setText(String.format("<html><b><i>%s</i></b></html>", name));
                    owlLabel.getParent().validate();
                } else {
                    owlLabel.setText(NO_OWL_INDIVIDUAL);
                    owlLabel.getParent().validate();
                }                
            } else if (key.equals(SADIProperties.GENERATOR_GENERATE_OWL)) {
                // fire appropriate generator property
                // actual logic is done in the Perl/Java property branch
                if ((Boolean)evt.getNewValue()) {
                    if (manager.getBooleanPreference(SADIProperties.DATATYPE_GEN_USE_PERL, true)) {
                        manager.saveBooleanPreference(SADIProperties.DO_PERL_DATATYPE_GENERATION, true);
                    } else {
                        // java code generator
                        manager.saveBooleanPreference(SADIProperties.DO_JAVA_DATATYPE_GENERATION, true);
                    }
                } else {
                    // cancel generate owl
                    if (manager.getBooleanPreference(SADIProperties.DATATYPE_GEN_USE_PERL, true)) {
                        // perl code generator
                        manager.saveBooleanPreference(SADIProperties.DO_PERL_DATATYPE_GENERATION, false);
                    } else {
                        // java code generator
                        manager.saveBooleanPreference(SADIProperties.DO_JAVA_DATATYPE_GENERATION, false);
                    }
                }
            } else if (key.equals(SADIProperties.DO_PERL_DATATYPE_GENERATION)) {
                if ((Boolean)evt.getNewValue()) {
                    // perl code generator start generator
                    generateOWLBtn.setEnabled(false);
                    perlDatatypeWorker = 
                        new DatatypeGeneratorPerlWorker(manager.getPreference(SADIProperties.GENERATOR_OWL_TMP_FILE_LOCATION, ""));
                    console = new LoggingWindowFrame(bundle.getString("result"), SADIProperties.DO_PERL_DATATYPE_GENERATION);
                    perlDatatypeWorker.execute();
                    console.setLocationRelativeTo(SADIOwlCodeGeneratorView.this.getParent());
                    console.setVisible(true);
                } else {
                    // cancel
                    perlDatatypeWorker = null;
                    generateOWLBtn.setEnabled(true);
                }
            } else if (key.equals(SADIProperties.DO_JAVA_DATATYPE_GENERATION)) {
                if ((Boolean)evt.getNewValue()) {
                    // disable the buttons
                    generateOWLBtn.setEnabled(false);
                    // owl 2 code logic here:
                    String s = bundle.getString("sadi_generator_java_no_owl_gen");
                    String title = bundle.getString("sadi_generator_java_no_owl_gen_title");
                    UIUtils.showMsgDialog(title, s, getTopLevelAncestor());
                    // fire property changed to cancel this request
                    manager.saveBooleanPreference(key, false);
                    // end owl 2 code logic
                } else {
                    
                    // re-enable the buttons
                    generateOWLBtn.setEnabled(true);
                }
            }

        }
        
    }
}
