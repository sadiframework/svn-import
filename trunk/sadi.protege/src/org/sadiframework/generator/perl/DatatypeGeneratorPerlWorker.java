/**
 * 
 */
package org.sadiframework.generator.perl;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import org.jdesktop.swingworker.SwingWorker;
import org.sadiframework.editor.views.SADIPreferencePanel;
import org.sadiframework.preferences.PreferenceManager;
import org.sadiframework.properties.SADIProperties;

/**
 * @author Eddie
 * 
 */
public class DatatypeGeneratorPerlWorker extends SwingWorker<String, Object> {

    private String filename;
    private PreferenceManager manager = PreferenceManager.newInstance();

    public DatatypeGeneratorPerlWorker(String filename) {

        this.filename = filename == null ? "" : filename;
        // listen for changes to the DO_SERVICE_GENERATION preference
        manager.addPropertyChangeListener(SADIProperties.DO_PERL_DATATYPE_GENERATION,
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        // cancel our task
                        if (!manager.getBooleanPreference(
                                SADIProperties.DO_PERL_DATATYPE_GENERATION, false)) {
                            if (!isCancelled() || !isDone()) {
                                cancel(true);
                            }
                        }
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jdesktop.swingworker.SwingWorker#doInBackground()
     */
    @Override
    protected String doInBackground() {

        String perl = manager.getPreference(SADIPreferencePanel.PERL_PATH, "");
        String libs = manager.getPreference(SADIPreferencePanel.PERL_5LIB_DIR, "");
        String scriptDir = manager.getPreference(SADIPreferencePanel.PERL_SADI_SCRIPTS_DIR, "");
        
        Generator gen = new Generator(perl, libs, scriptDir);
        String str = "";
        try {
            str = gen.generateDatatypes(filename);
        } catch (IOException ioe) {
            manager.saveBooleanPreference(SADIProperties.DO_PERL_DATATYPE_GENERATION, false);
        } catch (InterruptedException ie) {
            manager.saveBooleanPreference(SADIProperties.DO_PERL_DATATYPE_GENERATION, false);
        }
        return str;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jdesktop.swingworker.SwingWorker#done()
     */
    protected void done() {
        super.done();
        manager.saveBooleanPreference(SADIProperties.DO_PERL_DATATYPE_GENERATION, false);
    }

}
