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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.util.ComponentFactory;
import org.protege.editor.core.ui.util.Icons;
import org.protege.editor.owl.ui.view.cls.AbstractOWLClassViewComponent;
import org.sadiframework.editor.AbstractDefinitionFieldGenerator;
import org.sadiframework.editor.DefinitionField;
import org.sadiframework.editor.DefinitionFieldGeneratorImpl;
import org.sadiframework.generator.java.JavaGeneratorWorker;
import org.sadiframework.generator.perl.ServiceGeneratorPerlWorker;
import org.sadiframework.preferences.PreferenceManager;
import org.sadiframework.properties.SADIProperties;
import org.sadiframework.service.ServiceDefinition;
import org.sadiframework.swing.AbstractButton;
import org.sadiframework.swing.DropJTextField;
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

public class SadiGeneratorView extends AbstractOWLClassViewComponent {

    private static final long serialVersionUID = 2499960057967596902L;
    private PreferenceManager manager = PreferenceManager.newInstance();
    private String selectedClassAsString = "";
    // our main scroll pane; add/remove components to this
    private JPanel mainPane;
    private PropertyChangeListener pListener;
    // keeps a reference to our textfields so that we can save them if necessary
    private HashMap<String, JComponent> fields = new HashMap<String,JComponent>();

    private DefinitionField[] definitionFields;
    
    private final ResourceBundle bundle = ResourceBundle.getBundle("org.sadiframework.utils.i18n.EditorResourceBundle");
    
    private JButton generateBtn, generatePerlBtn;
    
    private LoggingWindowFrame console;
    
    // java specific vars
    private JButton javaLocalDeployBtn, javaPackageWarBtn, javaCwdBtn;
    private JavaGeneratorWorker javaServiceWorker;
    
    // perl specific vars
    private ServiceGeneratorPerlWorker perlServiceWorker;
    private JButton perlDefinitionBtn;
    
    @Override
    public void initialiseClassView() throws Exception {
        // init sets up all of the windows
        init();
    }
    
    private void init() {
        
        if (pListener == null) {
            pListener = new GeneratorPropertyListener();
            manager.addPropertyChangeListener(pListener);
        }
        
        // add flavour specific code here        
        if (mainPane == null) {
            mainPane = new JPanel();
            mainPane.setLayout(new GridBagLayout());
            mainPane.setAlignmentX(LEFT_ALIGNMENT);
            mainPane.setAlignmentY(TOP_ALIGNMENT);
            // a panel to hold the panels that dont change regardless of flavour
            JPanel panel = new JPanel(new GridBagLayout(), true);
            // add the definition portion of the panel
            UIUtils.addComponent(panel, getSadiServiceSignaturePanel(), 0, 0, 1, 1, UIUtils.NWEST, UIUtils.HORI, 1.0, 1.0);
            // add our panel to the main pane
            UIUtils.addComponent(mainPane, panel, 0, 0, 1, 1, UIUtils.NWEST, UIUtils.HORI, 1.0, 1.0);
        } else {
            // remove the flavour specific panel
            mainPane.remove(1);
            mainPane.validate();
        }
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Perl", getPerlSpecificPanel());
        tabbedPane.addTab("Java", getJavaSpecificPanel());
        UIUtils.addComponent(mainPane, tabbedPane, 0, 1, 1, 1, UIUtils.NWEST, UIUtils.HORI, 0.0, 100.0);
               
        // remove all from our parent
        removeAll();
        validate();
        // set the layout for our view
        setLayout(new BorderLayout(6, 6));
        setAlignmentY(TOP_ALIGNMENT);
        setAlignmentX(LEFT_ALIGNMENT);
        // add our new content to the panel
        add(new JScrollPane(mainPane), BorderLayout.CENTER);
        // is this necessary?
        updateUI();
        validate();
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
                || (manager.getBooleanPreference(SADIProperties.GENERATOR_OWL_BY_FILE, true))) {
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
    
    /*
     * (non-Javadoc)
     * 
     * @seeorg.protege.editor.owl.ui.view.AbstractOWLSelectionViewComponent#
     * isOWLClassView()
     */
    protected boolean isOWLClassView() {
        return true;
    }

    public void setSelectedClassAsString(String selectedClassAsString) {
        this.selectedClassAsString = selectedClassAsString;
    }

    public String getSelectedClassAsString() {
        return selectedClassAsString;
    }
    
   
    
    // panel declarations here:
    private JPanel getSadiServiceSignaturePanel() {
        definitionFields = new DefinitionFieldGeneratorImpl().getDefinitionFields();
        int numPairs = definitionFields.length;
        fields = new HashMap<String,JComponent>();

        // Create and populate the panel.
        JPanel p = new JPanel(new GridBagLayout());
        for (int i = 0; i < numPairs; i++) {
            JLabel l = new JLabel(
                    definitionFields[i].isRequired() ? 
                            String.format("<html><b>%s:</b></html>", definitionFields[i].getLabel()) : 
                            String.format("<html><i>%s:</i></html>", definitionFields[i].getLabel()), 
                    JLabel.TRAILING);
            UIUtils.addComponent(p, l, 0, i, 1, 1, UIUtils.NWEST, UIUtils.NONE, 0.0, 0.0);
            if (definitionFields[i].getType().equals(AbstractDefinitionFieldGenerator.TEXT_FIELD)) {
                JTextField textField = new JTextField(25);
                l.setLabelFor(textField);
                UIUtils
                        .addComponent(p, textField, 1, i, 2, 1, UIUtils.NWEST, UIUtils.HORI, 1.0,
                                0.0);
                fields.put(definitionFields[i].getId() ,textField);
            } else if (definitionFields[i].getType().equals(
                    AbstractDefinitionFieldGenerator.DROP_TEXT_FIELD)) {
                DropJTextField textField = new DropJTextField(25, getOWLModelManager());
                l.setLabelFor(textField);
                UIUtils
                        .addComponent(p, textField, 1, i, 2, 1, UIUtils.NWEST, UIUtils.HORI, 1.0,
                                0.0);
                fields.put(definitionFields[i].getId() ,textField);
            } else if (definitionFields[i].getType().equals(
                    AbstractDefinitionFieldGenerator.BOOLEAN_FIELD)) {
                JCheckBox textField = new JCheckBox();
                l.setLabelFor(textField);
                textField.setSelected(false);
                UIUtils
                        .addComponent(p, textField, 1, i, 1, 1, UIUtils.NWEST, UIUtils.NONE, 0.0,
                                0.0);
                fields.put(definitionFields[i].getId() ,textField);
            }
            if (!definitionFields[i].getHelpText().equals("")) {
                // TODO add help dialog
                final int index = i;
                JButton help = new AbstractButton("<html><b>?</b></html>", true, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JOptionPane.showMessageDialog(SadiGeneratorView.this,String.format("%s",definitionFields[index].getHelpText()),definitionFields[index].getLabel(),JOptionPane.QUESTION_MESSAGE);
                    }
                });  
                UIUtils
                    .addComponent(p, help, 3, i, 1, 1, UIUtils.NWEST, UIUtils.NONE, 0.0, 0.0);                
            }

        }

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(ComponentFactory.createTitledBorder(bundle
                .getString("sadi_definition_editor_title")));
        panel.add(p, BorderLayout.CENTER);

        return panel;
    }

    private void getGenerateGeneratorButtons() {
        generateBtn = new AbstractButton(bundle.getString("generate"), true,
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        manager.saveBooleanPreference(SADIProperties.DO_JAVA_SERVICE_GENERATION, true);
                    }
                });
        generatePerlBtn = new AbstractButton(bundle.getString("generate"), true,
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        manager.saveBooleanPreference(SADIProperties.DO_PERL_SERVICE_GENERATION, true);
                    }
                });
    }

    private JPanel getSyncOrAsyncRadioButtons() {        
        JRadioButton sync = new JRadioButton(bundle.getString("sync"));
        JRadioButton async = new JRadioButton(bundle.getString("async"));
        sync.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manager.saveBooleanPreference(SADIProperties.GENERATOR_SERVICE_ASYNC, false);
            }
        });
        async.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manager.saveBooleanPreference(SADIProperties.GENERATOR_SERVICE_ASYNC, true);
            }
        });

        // create a button group for synchronicity
        ButtonGroup syncGroup = new ButtonGroup();
        syncGroup.add(sync);
        syncGroup.add(async);
        if (manager.getBooleanPreference(SADIProperties.GENERATOR_SERVICE_ASYNC, false)) {
            async.setSelected(true);
        } else {
            manager.saveBooleanPreference(SADIProperties.GENERATOR_SERVICE_ASYNC, false);
            sync.setSelected(true);
        }
        
        JPanel rbPanel = new JPanel(new FlowLayout(FlowLayout.LEADING), true);
        rbPanel.add(sync);
        rbPanel.add(async);
        return rbPanel;
    }
    
    private JPanel getGenerateBothCheckbox() {
        JCheckBox doBoth = new JCheckBox(bundle.getString("generate_both"));
        // set the default state
        doBoth.setSelected(manager.getBooleanPreference(SADIProperties.GENERATOR_DO_BOTH_GENERATION, false));
        doBoth.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() instanceof JCheckBox) {
                    manager.saveBooleanPreference(SADIProperties.GENERATOR_DO_BOTH_GENERATION, ((JCheckBox)e.getSource()).isSelected());
                }
            }
        });
        JPanel rbPanel = new JPanel(new FlowLayout(FlowLayout.LEADING), true);
        rbPanel.add(doBoth);
        
        return rbPanel;
    }
    
    
    
    private JPanel getJavaSpecificPanel() {
        JPanel panel = new JPanel(true);
        panel.setLayout(new GridBagLayout());
        panel.setBorder(ComponentFactory.createTitledBorder(bundle
                .getString("sadi_generator_java_title")));

        // package field
        JLabel packagelabel = new JLabel(bundle.getString("sadi_generator_java_service_package"));
        JTextFieldWithHistory packageField = new JTextFieldWithHistory(25, SADIProperties.JAVA_GENERATOR_SERVICE_PACKAGE);
        packagelabel.setLabelFor(packageField);

        // local deploy btn
        javaLocalDeployBtn = new AbstractButton(bundle
                .getString("sadi_generator_java_local_deploy"), true, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manager.saveBooleanPreference(SADIProperties.DO_JAVA_GENERATOR_DEPLOY, true);
            }
        });
        
        // package war btn
        javaPackageWarBtn = new AbstractButton(bundle.getString("sadi_generator_java_war"), true,
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        manager.saveBooleanPreference(SADIProperties.DO_JAVA_GENERATOR_CREATE_PACKAGE, true);
                    }
                });

        // user working directory
        JLabel cwdlabel = new JLabel(bundle.getString("sadi_generator_working_dir"));
        JTextFieldWithHistory cwdField = new JTextFieldWithHistory(25,SADIProperties.JAVA_SERVICE_SKELETONS_WORKING_DIR);
        cwdField.setEditable(false);
        cwdlabel.setLabelFor(cwdField);
        
        // user project name 
        JLabel projectlabel = new JLabel(bundle.getString("sadi_generator_java_project_name"));
        JTextFieldWithHistory projectField = new JTextFieldWithHistory(25, "sadi-services", SADIProperties.JAVA_SERVICE_SKELETONS_PROJECT_NAME);
        projectField.setEditable(true);
        projectlabel.setLabelFor(projectField);
        
        // user extra maven arguments
        JLabel extralabel = new JLabel(bundle.getString("sadi_generator_extra_maven"));
        JTextFieldWithHistory extraField = new JTextFieldWithHistory(25,SADIProperties.JAVA_SERVICE_EXTRA_MAVEN_ARGS);
        extralabel.setLabelFor(extraField);
        
        javaCwdBtn = new AbstractButton("", true, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // this is the action that is invoked when the open file button
                // is clicked on
                String title = bundle.getString("open");
                JFileChooser chooser = UIUtils.getOpenDirectoryChooser(title,SADIProperties.JAVA_SERVICE_SKELETONS_WORKING_DIR);
                if (chooser.showOpenDialog((Component) e.getSource()) != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                File file = chooser.getSelectedFile();
                if (file == null || !file.exists())
                    return;
                manager.savePreference(SADIProperties.JAVA_SERVICE_SKELETONS_WORKING_DIR, file.getAbsolutePath());
            }
        });
        javaCwdBtn.setIcon(Icons.getIcon("project.open.gif"));
        
        // the generate btn
        if (generateBtn == null)
            getGenerateGeneratorButtons();
        
        // add the components
        UIUtils.addComponent(panel, cwdlabel, 0, 0, 1, 1, UIUtils.NWEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(panel, cwdField, 1, 0, 2, 1, UIUtils.NWEST, UIUtils.HORI, 1.0, 0.0);
        UIUtils.addComponent(panel, javaCwdBtn, 3, 0, 1, 1, UIUtils.NWEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(panel, projectlabel, 0, 1, 1, 1, UIUtils.NWEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(panel, projectField, 1, 1, 2, 1, UIUtils.NWEST, UIUtils.HORI, 1.0, 0.0);
        UIUtils.addComponent(panel, packagelabel, 0, 2, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(panel, packageField, 1, 2, 2, 1, UIUtils.WEST, UIUtils.HORI, 0.0, 0.0);
        UIUtils.addComponent(panel, extralabel, 0, 3, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(panel, extraField, 1, 3, 2, 1, UIUtils.WEST, UIUtils.HORI, 0.0, 0.0);
        UIUtils.addComponent(panel, getSyncOrAsyncRadioButtons(), 0, 4, 2, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(panel, getGenerateBothCheckbox(), 0, 5, 2, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(panel, UIUtils.createButtonPanel(new JButton[] { generateBtn, javaLocalDeployBtn, javaPackageWarBtn }), 0, 6, 2, 1, UIUtils.WEST, UIUtils.NONE, 1.0, 0.0);
        // remember to make the panel suck up the remainder vertical space
        return panel;
    }
    
    private JPanel getPerlSpecificPanel() {
        JPanel panel = new JPanel(new GridBagLayout(), true);
        panel.setBorder(ComponentFactory.createTitledBorder(bundle.getString("sadi_generator_perl_title")));
        
        // users Perl-SADI/ definition directory
        JLabel deflabel = new JLabel(bundle.getString("sadi_generator_perl_definition_dir"));
        JTextFieldWithHistory defField = new JTextFieldWithHistory(25,SADIProperties.PERL_SADI_HOME_DIRECTORY);
        defField.setEditable(false);
        deflabel.setLabelFor(defField);
        perlDefinitionBtn = new AbstractButton("", true, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // this is the action that is invoked when the open file button
                // is clicked on
                String title = bundle.getString("open");
                JFileChooser chooser = UIUtils.getOpenDirectoryChooser(title, SADIProperties.PERL_SADI_HOME_DIRECTORY);
                if (chooser.showOpenDialog((Component) e.getSource()) != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                File file = chooser.getSelectedFile();
                if (file == null || !file.exists())
                    return;
                manager.savePreference(SADIProperties.PERL_SADI_HOME_DIRECTORY, file.getAbsolutePath());
                
                // set the actual definitions dir by reading the sadi-services.cfg file
                // Read properties file. 
                Properties properties = new Properties(); 
                try { 
                    properties.load(new FileInputStream(String.format("%s/%s", file.getAbsolutePath(), "sadi-services.cfg"))); 
                } catch (IOException ioe) { 
                    manager.savePreference(
                            SADIProperties.PERL_SADI_DEFINITION_DIRECTORY, 
                            String.format("%s/%s", file.getAbsolutePath(), "definitions")
                    );
                    return;
                }
                if (properties.containsKey("impl.definitions")) {
                    manager.savePreference(
                            SADIProperties.PERL_SADI_DEFINITION_DIRECTORY, 
                            String.format("%s", properties.getProperty("impl.definitions")).trim()
                    );
                } else  {
                    manager.savePreference(
                            SADIProperties.PERL_SADI_DEFINITION_DIRECTORY, 
                            String.format("%s/%s", file.getAbsolutePath(), "definitions").trim()
                    );
                }
            }
        });
        
        // the generate btn
        if (generatePerlBtn == null)
            getGenerateGeneratorButtons();
        
        perlDefinitionBtn.setIcon(Icons.getIcon("project.open.gif"));
        // add the components
        UIUtils.addComponent(panel, deflabel, 0, 0, 1, 1, UIUtils.NWEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(panel, defField, 1, 0, 2, 1, UIUtils.NWEST, UIUtils.HORI, 1.0, 0.0);
        UIUtils.addComponent(panel, perlDefinitionBtn, 3, 0, 1, 1, UIUtils.NWEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(panel, getSyncOrAsyncRadioButtons(), 0, 1, 2, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        UIUtils.addComponent(panel, getGenerateBothCheckbox(), 0, 2, 2, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        // create the generate/cancel button panel
        UIUtils.addComponent(panel, UIUtils.createButtonPanel(new JButton[]{generatePerlBtn}), 0, 3, 2, 1, UIUtils.NWEST, UIUtils.NONE, 1.0, 1.0);
        return panel;
    }
    
    /*
     * returns the fields as a ServiceDefinition object
     */
    private ServiceDefinition getServiceDefinition() {
        String name = ((JTextComponent) fields.get("ServiceName")).getText();
        String description = ((JTextComponent) fields.get("Description")).getText();
        String inputClass = ((JTextComponent) fields.get("InputClass")).getText();
        String outputClass = ((JTextComponent) fields.get("OutputClass")).getText();
        String endpoint = ((JTextComponent) fields.get("URL")).getText();
        String authority = ((JTextComponent) fields.get("Authority")).getText();
        String provider = ((JTextComponent) fields.get("Provider")).getText();
        String serviceType = ((JTextComponent) fields.get("ServiceType")).getText();
        boolean authoritative = ((JCheckBox) fields.get("Authoritative")).isSelected();
        
        ServiceDefinition def = new ServiceDefinition(name.trim());
        def.setAuthoritative(authoritative);
        def.setAuthority(authority.trim());
        def.setDescription(description.trim());
        def.setEndpoint(endpoint.trim());
        def.setInputClass(inputClass.trim());
        def.setOutputClass(outputClass.trim());
        def.setProvider(provider.trim());
        def.setServiceType(serviceType.trim());
        def.setAsync(manager.getBooleanPreference(SADIProperties.GENERATOR_SERVICE_ASYNC, false));
        return def;
    }
    
    private boolean savePerlSADIDefinition(ServiceDefinition def) {

        // get chooser for directories
        File dir = null;
        try {
            dir = new File(manager.getPreference(SADIProperties.PERL_SADI_DEFINITION_DIRECTORY, null));
        } catch (NullPointerException npe) {
            // no perl sadi home directory specified
            JOptionPane.showMessageDialog(
                    this, 
                    bundle.getString("sadi_generator_perl_error_no_home_dir"),
                    bundle.getString("definition_validation_title"), 
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (!dir.canWrite()) {
            JOptionPane.showMessageDialog(null, bundle
                    .getString("definition_cannot_write_directory"), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        // save the file:
        File outfile = null;
        try {
            outfile = new File(dir, def.getName());
        } catch (NullPointerException npe) {
            ErrorLogPanel.showErrorDialog(npe);
        }
        // if it exists, prompt to overwrite
        if (outfile == null)
            return false;
        if (outfile.exists()) {
            int confirm = JOptionPane.showConfirmDialog(SadiGeneratorView.this,
                    String
                            .format(bundle.getString("definition_file_exists"), def
                                    .getName()));
            if (confirm == JOptionPane.CANCEL_OPTION || confirm == JOptionPane.NO_OPTION) {
                // tell them to rename the service or choose a
                // different directory
                JOptionPane.showMessageDialog(SadiGeneratorView.this, bundle
                        .getString("definition_rename"));
                return false;
            }
        }
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
            out.write(def.toString());
            out.close();
        } catch (IOException ioe) {
            ErrorLogPanel.showErrorDialog(ioe);
            return false;
        }
        return true;
    }
    
    // inner class for listening to changes in our properties
    private class GeneratorPropertyListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            String key = evt.getPropertyName();
            if (key.equals(SADIProperties.DO_PERL_SERVICE_GENERATION)) {
                if ((Boolean)evt.getNewValue()) {
                    // save the definition file
                    ServiceDefinition def = getServiceDefinition();
                    if (def == null) {
                        // cancel
                        manager.saveBooleanPreference(key, false);
                        return;
                    }
                    // verify the definition fields
                    if (!validateGeneratorDefinition(def)) {
                        manager.saveBooleanPreference(key, false);
                        return;
                    }
                    
                    // try to save the definition file
                    if (!savePerlSADIDefinition(def)) {
                        manager.saveBooleanPreference(key, false);
                        return;
                    }
                    // start perl sadi service generator
                    generateBtn.setEnabled(false);
                    // put the name of the service into the preference manager
                    manager.savePreference(SADIProperties.GENERATOR_SERVICE_NAME, def.getName());
                    // start perl generator
                    perlServiceWorker = new ServiceGeneratorPerlWorker();
                    perlServiceWorker.execute();
                    // set up the console
                    setupConsole(SADIProperties.DO_PERL_SERVICE_GENERATION);
                    
                } else {
                    // task is cancelled or we are finished
                    if (perlServiceWorker != null && perlServiceWorker.isDone() && !perlServiceWorker.isCancelled()) {
                        // do nothing anymore ....
                    }
                    perlServiceWorker = null;
                    generateBtn.setEnabled(true);
                }
            } else if (key.equals(SADIProperties.DO_JAVA_SERVICE_GENERATION)) {
                if ((Boolean)evt.getNewValue()) {
                    ServiceDefinition def = getServiceDefinition();
                    if (def == null) {
                        // cancel
                        manager.saveBooleanPreference(key, false);
                        return;
                    }
                    if (!validateGeneratorDefinition(def)) {
                        // cancel
                        manager.saveBooleanPreference(key, false);
                        return;
                    }
                    
                    String outdir = manager.getPreference(SADIProperties.JAVA_SERVICE_SKELETONS_WORKING_DIR, "");
                    // ensure outdir exists
                    if (!(new File(outdir).exists())) {
                        JOptionPane.showMessageDialog(
                                SadiGeneratorView.this, 
                                bundle.getString("sadi_generator_java_target_dir_error"),
                                bundle.getString("definition_validation_title"), 
                                JOptionPane.ERROR_MESSAGE);
                        manager.saveBooleanPreference(key, false);
                        return; 
                    }
                    // disable the gen button
                    generateBtn.setEnabled(false);
                    // FIXME project name should not be the service name
                    String projectName = manager.getPreference(SADIProperties.JAVA_SERVICE_SKELETONS_PROJECT_NAME, "sadi-services");
                    javaServiceWorker = new JavaGeneratorWorker(outdir, projectName);
                    // set the definition, extra maven args and package name
                    javaServiceWorker.setDefinition(def);
                    javaServiceWorker.setExtraMavenArgs(manager.getPreference(SADIProperties.JAVA_SERVICE_EXTRA_MAVEN_ARGS, ""));
                    javaServiceWorker.setServicePackage(manager.getPreference(SADIProperties.JAVA_GENERATOR_SERVICE_PACKAGE, ""));
                    javaServiceWorker.setAction(JavaGeneratorWorker.GENERATE);
                    
                    // execute the service
                    javaServiceWorker.execute();
                    // set up the console
                    setupConsole(SADIProperties.DO_JAVA_SERVICE_GENERATION);                   
                    
                    
                } else {
                    if (javaServiceWorker != null && javaServiceWorker.isDone() && !javaServiceWorker.isCancelled()) {
                        // do nothing anymore
                    }
                    javaServiceWorker = null;
                    // re-enable the buttons
                    generateBtn.setEnabled(true);
                }
            }  else if (key.equals(SADIProperties.DO_JAVA_GENERATOR_DEPLOY)) {
                if ((Boolean)evt.getNewValue()) {                    
                    String outdir = manager.getPreference(SADIProperties.JAVA_SERVICE_SKELETONS_WORKING_DIR, "");
                    String projectName = manager.getPreference(SADIProperties.JAVA_SERVICE_SKELETONS_PROJECT_NAME, "sadi-services");
                    // ensure outdir exists
                    if (!(new File(outdir).exists())) {
                        JOptionPane.showMessageDialog(
                                SadiGeneratorView.this, 
                                bundle.getString("sadi_generator_java_target_dir_error"),
                                bundle.getString("definition_validation_title"), 
                                JOptionPane.ERROR_MESSAGE);
                        manager.saveBooleanPreference(key, false);
                        return; 
                    }
                    // ensure that projectName exists!
                    if (!(new File(outdir, projectName).exists())) {
                        JOptionPane.showMessageDialog(
                                SadiGeneratorView.this, 
                                String.format(
                                        bundle.getString("sadi_generator_java_service_dir_error"), 
                                        new File(outdir, projectName).getAbsolutePath()
                                ),
                                bundle.getString("definition_validation_title"), 
                                JOptionPane.ERROR_MESSAGE);
                        manager.saveBooleanPreference(key, false);
                        return; 
                    }
                    // disable the deploy button
                    javaLocalDeployBtn.setEnabled(false);
                    javaPackageWarBtn.setEnabled(false);
                    javaServiceWorker = new JavaGeneratorWorker(outdir, projectName);
                    // set the definition, extra maven args
                    //javaServiceWorker.setDefinition(def);
                    javaServiceWorker.setExtraMavenArgs(manager.getPreference(SADIProperties.JAVA_SERVICE_EXTRA_MAVEN_ARGS, ""));
                    javaServiceWorker.setAction(JavaGeneratorWorker.DEPLOY);
                    // execute the service
                    javaServiceWorker.execute();
                    // set up the console
                    // setupConsole(SADIProperties.DO_JAVA_GENERATOR_DEPLOY, SADIProperties.DO_JAVA_GENERATOR_UNDEPLOY);
                    setupConsole(SADIProperties.DO_JAVA_GENERATOR_DEPLOY);
                    
                } else {
//                    if (javaServiceWorker != null) {
//                        javaServiceWorker.cancel(true);
//                        javaServiceWorker = null;
//                    }
                    manager.saveBooleanPreference(SADIProperties.DO_JAVA_GENERATOR_UNDEPLOY, true);
                }
            } else if (key.equals(SADIProperties.DO_JAVA_GENERATOR_UNDEPLOY)) {
                if (javaServiceWorker == null){
                    return;
                }
                if ((Boolean)evt.getNewValue()) {
                    javaLocalDeployBtn.setEnabled(true);
                    javaPackageWarBtn.setEnabled(true);
                    
                    String outdir = manager.getPreference(SADIProperties.JAVA_SERVICE_SKELETONS_WORKING_DIR, "");
                    String projectName = manager.getPreference(SADIProperties.JAVA_SERVICE_SKELETONS_PROJECT_NAME, "sadi-services");
                    javaServiceWorker = new JavaGeneratorWorker(outdir, projectName);
                    javaServiceWorker.setExtraMavenArgs(manager.getPreference(SADIProperties.JAVA_SERVICE_EXTRA_MAVEN_ARGS, ""));
                    javaServiceWorker.setAction(JavaGeneratorWorker.UNDEPLOY);
                    // execute the service
                    javaServiceWorker.execute();
                    setupConsole(SADIProperties.DO_JAVA_GENERATOR_UNDEPLOY);
                    if (javaServiceWorker!= null)
                        while (!javaServiceWorker.isDone()) {
                            try {
                                // maybe the task takes some time?
                                wait(3000);
                            } catch (InterruptedException e) {
                            }
                        }
                    javaServiceWorker = null;
                } else {
                    // do nothing
                    javaServiceWorker = null;
                }
            } else if (key.equals(SADIProperties.DO_JAVA_GENERATOR_CREATE_PACKAGE)) {
                if ((Boolean)evt.getNewValue()) {
                    ServiceDefinition def = getServiceDefinition();
                    if (def != null) {
                        // mvn package
                        String outdir = manager.getPreference(SADIProperties.JAVA_SERVICE_SKELETONS_WORKING_DIR, "");
                        String projectName = manager.getPreference(SADIProperties.JAVA_SERVICE_SKELETONS_PROJECT_NAME, "sadi-services");
                        // ensure outdir exists
                        if (!(new File(outdir).exists())) {
                            JOptionPane.showMessageDialog(
                                    SadiGeneratorView.this, 
                                    bundle.getString("sadi_generator_java_target_dir_error"),
                                    bundle.getString("definition_validation_title"), 
                                    JOptionPane.ERROR_MESSAGE);
                            manager.saveBooleanPreference(key, false);
                            return; 
                        }
                        // ensure that projectName exists!
                        if (!(new File(outdir, projectName).exists())) {
                            JOptionPane.showMessageDialog(
                                    SadiGeneratorView.this, 
                                    String.format(
                                            bundle.getString("sadi_generator_java_service_dir_error"), 
                                            new File(outdir, projectName).getAbsolutePath()
                                    ),
                                    bundle.getString("definition_validation_title"), 
                                    JOptionPane.ERROR_MESSAGE);
                            manager.saveBooleanPreference(key, false);
                            return; 
                        }
                        // disable the package button
                        javaPackageWarBtn.setEnabled(false);
                        javaServiceWorker = new JavaGeneratorWorker(outdir, projectName);
                        // set the definition, extra maven args
                        javaServiceWorker.setDefinition(def);
                        javaServiceWorker.setExtraMavenArgs(manager.getPreference(SADIProperties.JAVA_SERVICE_EXTRA_MAVEN_ARGS, ""));
                        javaServiceWorker.setAction(JavaGeneratorWorker.PACKAGE);
                        
                        // execute the service
                        javaServiceWorker.execute();
                        setupConsole(SADIProperties.DO_JAVA_GENERATOR_CREATE_PACKAGE);
                    }
                } else {
                    // re-enable the package button
                    javaPackageWarBtn.setEnabled(true);
                }
                
            }

        }
    }
    
    private void setupConsole(String key) {
        setupConsole(key, null);
    }
    /*
     * sets up a modal console that pipes system.err/out streams to it.
     * the window will listen for property change events on key and if 
     * onCloseKey is not null, when the console is closed, then onCloseKey
     * preference is saved with a true value.
     */
    private void setupConsole(String key, String onCloseKey) {
        console = new LoggingWindowFrame(bundle.getString("result"), key, onCloseKey);
        console.setLocationRelativeTo(SadiGeneratorView.this.getParent());
        console.setVisible(true);
    }

    public boolean validateGeneratorDefinition(ServiceDefinition def) {
        if (def.getName() == null || def.getName().equals("")) {
            JOptionPane.showMessageDialog(
                    SadiGeneratorView.this, 
                    bundle.getString("definition_validation_name"),
                    bundle.getString("definition_validation_title"), 
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (def.getAuthority() == null || def.getAuthority().equals("")) {
            JOptionPane.showMessageDialog(
                    SadiGeneratorView.this, 
                    bundle.getString("definition_validation_authority"),
                    bundle.getString("definition_validation_title"), 
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        
        try {
            // should throw exception because we only have a domain name ...
            new URL(def.getAuthority());
            JOptionPane.showMessageDialog(
                    SadiGeneratorView.this, 
                    bundle.getString("definition_validation_authority"),
                    bundle.getString("definition_validation_title"), 
                    JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (Exception e) {
            
        }
        
        if (def.getDescription() == null || def.getDescription().equals("")) {
            JOptionPane.showMessageDialog(
                    SadiGeneratorView.this, 
                    bundle.getString("definition_validation_description"),
                    bundle.getString("definition_validation_title"), 
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (def.getServiceType() == null || def.getServiceType().equals("")) {
            def.setServiceType("http://someontology.org/services/sometype");
        }
        
        if (def.getInputClass() == null || def.getInputClass().equals("")) {
            JOptionPane.showMessageDialog(
                    SadiGeneratorView.this, 
                    bundle.getString("definition_validation_input_class"),
                    bundle.getString("definition_validation_title"), 
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (def.getOutputClass() == null || def.getOutputClass().equals("")) {
            JOptionPane.showMessageDialog(
                    SadiGeneratorView.this, 
                    bundle.getString("definition_validation_output_class"),
                    bundle.getString("definition_validation_title"), 
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (def.getProvider() == null || def.getProvider().equals("")) {
            JOptionPane.showMessageDialog(
                    SadiGeneratorView.this, 
                    bundle.getString("definition_validation_provider"),
                    bundle.getString("definition_validation_title"), 
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (def.getEndpoint() == null || def.getEndpoint().equals("")) {
            JOptionPane.showMessageDialog(
                    SadiGeneratorView.this, 
                    bundle.getString("definition_validation_endpoint"),
                    bundle.getString("definition_validation_title"), 
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }    
}
