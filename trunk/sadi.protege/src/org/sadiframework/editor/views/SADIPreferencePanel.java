/**
 * 
 */
package org.sadiframework.editor.views;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;


import org.protege.editor.core.ui.util.LinkLabel;
import org.protege.editor.core.ui.util.NativeBrowserLauncher;
import org.sadiframework.preferences.PreferenceManager;
import org.sadiframework.swing.JTextFieldWithHistory;
import org.sadiframework.swing.UIUtils;

/**
 * @author Eddie Kawas
 * 
 */
public class SADIPreferencePanel extends org.protege.editor.core.ui.preferences.PreferencesPanel {

    private static final long serialVersionUID = -4934465657064212123L;
    private PreferenceManager manager = PreferenceManager.newInstance();
    private final ResourceBundle bundle = ResourceBundle
            .getBundle("org.sadiframework.utils.i18n.EditorResourceBundle");

    // Perl SADI variables
    public final static String PERL_PATH = "perl-exe-path";
    public final static String PERL_5LIB_DIR = "perl-5lib-dir";
    public final static String PERL_SADI_SCRIPTS_DIR = "sadises-scripts-dir";

    private String TMP_PERL_PATH = "perl-exe-path-tmp";
    private String TMP_PERL_5LIB_DIR = "perl-5lib-dir-tmp";
    private String TMP_PERL_SADI_SCRIPTS_DIR = "sadises-scripts-dir-tmp";

    private String[] PERL_SADI_PREFERENCE_KEYS = { PERL_PATH, PERL_5LIB_DIR, PERL_SADI_SCRIPTS_DIR };
    private String[] TMP_PERL_SADI_PREFERENCE_KEYS = { TMP_PERL_PATH, TMP_PERL_5LIB_DIR, TMP_PERL_SADI_SCRIPTS_DIR };
    
    private String[] PERL_SADI_LABELS = { "Perl Path: ", "Perl LIB Directories: ", "Perl SADI Scripts Directory: " };
    private String[] PERL_SADI_HELP = { 
                bundle.getString("preference_help_perl_exe"),
                bundle.getString("preference_help_perl_5lib"),
                bundle.getString("preference_help_perl_sadi_scripts")
            };
    
    // Java variables
    
    private String[] JAVA_SADI_PREFERENCE_KEYS = { };
    private String[] TMP_JAVA_SADI_PREFERENCE_KEYS = { };
    
    private String[] JAVA_SADI_LABELS = { };
    private String[] JAVA_SADI_HELP = { };
    

    @Override
    public void applyChanges() {
        // update real text field preference values with the ones that were
        // entered!
        for (int i = 0; i < TMP_PERL_SADI_PREFERENCE_KEYS.length; i++) {
            manager.savePreference(PERL_SADI_PREFERENCE_KEYS[i], manager.getPreference(
                    TMP_PERL_SADI_PREFERENCE_KEYS[i], ""));
        }
        for (int i = 0; i < TMP_JAVA_SADI_PREFERENCE_KEYS.length; i++) {
            manager.savePreference(JAVA_SADI_PREFERENCE_KEYS[i], manager.getPreference(
                    TMP_JAVA_SADI_PREFERENCE_KEYS[i], ""));
        }
    }

    public void initialise() throws Exception {
        add(getPreferencesPanel());

    }

    private Component getPreferencesPanel() {
        // Create and populate the perl panel. (reset the values to saved
        // values)
        JPanel preferencesPanel = new JPanel(new GridBagLayout());
        preferencesPanel.setBorder(BorderFactory.createTitledBorder("Perl SADI"));
        for (int i = 0; i < TMP_PERL_SADI_PREFERENCE_KEYS.length; i++) {
            // reset preferences
            manager.savePreference(TMP_PERL_SADI_PREFERENCE_KEYS[i], manager.getPreference(
                    PERL_SADI_PREFERENCE_KEYS[i], ""));
            // create the interface
            JLabel label = new JLabel(PERL_SADI_LABELS[i]);
            JTextFieldWithHistory field = new JTextFieldWithHistory(25,
                    TMP_PERL_SADI_PREFERENCE_KEYS[i]);
            JButton hBtn = UIUtils.createHelpButton(PERL_SADI_HELP[i]);

            UIUtils
                    .addComponent(preferencesPanel, label, 0, i, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0,
                            0.0);
            UIUtils
                    .addComponent(preferencesPanel, field, 1, i, 3, 1, UIUtils.WEST, UIUtils.HORI, 0.0,
                            0.0);
            UIUtils.addComponent(preferencesPanel, hBtn, 4, i, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        }
        LinkLabel l = new LinkLabel("SADI Homepage", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                NativeBrowserLauncher.openURL("http://sadiframework.org");
            }
        });
        l.setHorizontalAlignment(JLabel.LEADING);

        // create the java panel
        JPanel javaPanel = new JPanel(new GridBagLayout());
        javaPanel.setBorder(BorderFactory.createTitledBorder("jSADI"));
        
        for (int i = 0; i < TMP_JAVA_SADI_PREFERENCE_KEYS.length; i++) {
            // reset preferences
            manager.savePreference(JAVA_SADI_PREFERENCE_KEYS[i], manager.getPreference(
                    JAVA_SADI_PREFERENCE_KEYS[i], ""));
            // create the interface
            JLabel label = new JLabel(JAVA_SADI_LABELS[i]);
            JTextFieldWithHistory field = new JTextFieldWithHistory(25,
                    TMP_JAVA_SADI_PREFERENCE_KEYS[i]);
            JButton hBtn = UIUtils.createHelpButton(JAVA_SADI_HELP[i]);

            UIUtils
                    .addComponent(javaPanel, label, 0, i, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0,
                            0.0);
            UIUtils
                    .addComponent(javaPanel, field, 1, i, 3, 1, UIUtils.WEST, UIUtils.HORI, 0.0,
                            0.0);
            UIUtils.addComponent(javaPanel, hBtn, 4, i, 1, 1, UIUtils.WEST, UIUtils.NONE, 0.0, 0.0);
        }
        

        // create the main panel
        JPanel panel = new JPanel(new GridBagLayout());
        UIUtils.addComponent(panel, preferencesPanel, 0, 0, 1, 1, UIUtils.NWEST, UIUtils.HORI, 0.0, 0.0);
        //UIUtils.addComponent(panel, javaPanel, 0, 1, 1, 1, UIUtils.NWEST, UIUtils.HORI, 0.0, 0.0);
        UIUtils.addComponent(panel, l, 0, 2, 1, 1, UIUtils.SWEST, UIUtils.NONE, 0.0, 0.0);

        return panel;
    }

    public void dispose() throws Exception {

    }
}
