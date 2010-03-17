/**
 * 
 */
package org.sadiframework.generator.perl;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import org.jdesktop.swingworker.SwingWorker;
import org.sadiframework.editor.views.SADIPreferencePanel;
import org.sadiframework.editor.views.SadiServiceGeneratorView;
import org.sadiframework.preferences.PreferenceManager;

/**
 * @author Eddie
 *
 */
public class ServiceGeneratorPerlWorker extends SwingWorker {

    private PreferenceManager manager = PreferenceManager.newInstance();
    
    public ServiceGeneratorPerlWorker() {
        // listen for changes to the DO_SERVICE_GENERATION preference
        manager.addPropertyChangeListener(SadiServiceGeneratorView.DO_SERVICE_GENERATION, new PropertyChangeListener() { 
            public void propertyChange(PropertyChangeEvent evt) {
                // cancel our task
                if (!manager.getBooleanPreference(SadiServiceGeneratorView.DO_SERVICE_GENERATION, false)) {
                    if (!isCancelled() || !isDone()) {
                        cancel(true);
                    }
                }
            }
        });
    }
    
    /* (non-Javadoc)
     * @see org.jdesktop.swingworker.SwingWorker#doInBackground()
     */
    protected Object doInBackground() throws Exception {
        String perl = manager.getPreference(SADIPreferencePanel.PERL_PATH, "");
        String libs = manager.getPreference(SADIPreferencePanel.PERL_5LIB_DIR, "");
        String scriptDir = manager.getPreference(
                SADIPreferencePanel.PERL_SADI_SCRIPTS_DIR, "");
        boolean isAsync = manager.getBooleanPreference(SadiServiceGeneratorView.PERL_GENERATOR_SERVICE_ASYNC, true);
        String name = manager.getPreference(SadiServiceGeneratorView.PERL_GENERATOR_SERVICE_NAME, "");

        // TODO make sure that the service name is not blank!
        Generator gen = new Generator(perl, libs, scriptDir);
        String str = "";
        try {
            str = gen.generateService(name, isAsync);
        } catch (IOException ioe) {
            manager.saveBooleanPreference(SadiServiceGeneratorView.DO_SERVICE_GENERATION, false);
        } catch (InterruptedException ie) {
            manager.saveBooleanPreference(SadiServiceGeneratorView.DO_SERVICE_GENERATION, false);
        }
        return str;
    }
    protected void done() {
        super.done();
        manager.saveBooleanPreference(SadiServiceGeneratorView.DO_SERVICE_GENERATION, false);
    }

}
