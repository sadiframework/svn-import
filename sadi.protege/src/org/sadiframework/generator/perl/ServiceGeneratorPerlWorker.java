/**
 * 
 */
package org.sadiframework.generator.perl;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import org.jdesktop.swingworker.SwingWorker;
import org.sadiframework.editor.views.SADIPreferencePanel;
import org.sadiframework.preferences.PreferenceManager;
import org.sadiframework.properties.SADIProperties;
import org.sadiframework.service.ServiceDefinition;

/**
 * @author Eddie
 * 
 */
public class ServiceGeneratorPerlWorker extends SwingWorker<Boolean, Object> {

    private PreferenceManager manager = PreferenceManager.newInstance();
    private ServiceDefinition serviceDefinition;
    private File outputFile;
    	
    public ServiceGeneratorPerlWorker(ServiceDefinition serviceDefinition, File outputFile) {
    	
    	setServiceDefinition(serviceDefinition);
    	setOutputFile(outputFile);
    	
        // listen for changes to the DO_SERVICE_GENERATION preference
        manager.addPropertyChangeListener(SADIProperties.DO_PERL_SERVICE_GENERATION,
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        // cancel our task
                        if (!manager.getBooleanPreference(
                                SADIProperties.DO_PERL_SERVICE_GENERATION, false)) {
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
    // PRE: all applicable fields needed are correct (i.e. name, definition file exists, etc)
    // POST: unless cancelled, this will 
    protected Boolean doInBackground() throws Exception {
        String perl = manager.getPreference(SADIPreferencePanel.PERL_PATH, "");
        String libs = manager.getPreference(SADIPreferencePanel.PERL_5LIB_DIR, "");
        String scriptDir = manager.getPreference(SADIPreferencePanel.PERL_SADI_SCRIPTS_DIR, "");
//        boolean isAsync = manager
//                .getBooleanPreference(SADIProperties.GENERATOR_SERVICE_ASYNC, true);
//        boolean doBoth = manager.getBooleanPreference(SADIProperties.GENERATOR_DO_BOTH_GENERATION, true);
//        boolean useForce = manager.getBooleanPreference(SADIProperties.PERL_SADI_USE_FORCE, false);
//        
//        String name = manager.getPreference(SADIProperties.GENERATOR_SERVICE_NAME, "");
//        String pSadiHomedir = manager.getPreference(SADIProperties.PERL_SADI_HOME_DIRECTORY, "");
        Generator gen = new Generator(perl, libs, scriptDir);
//        String str = "";
        try {
//            str = gen.generateService(name, pSadiHomedir, isAsync, doBoth, useForce);
          gen.generateService(getServiceDefinition(), getOutputFile());
        } catch (IOException ioe) {
            manager.saveBooleanPreference(SADIProperties.DO_PERL_SERVICE_GENERATION, false);
            return false;
        }
//        } catch (InterruptedException ie) {
//            manager.saveBooleanPreference(SADIProperties.DO_PERL_SERVICE_GENERATION, false);
//        }
//        return str;
       return true;
    }

    protected void done() {
        super.done();
        manager.saveBooleanPreference(SADIProperties.DO_PERL_SERVICE_GENERATION, false);
    }
    
    public void setServiceDefinition(ServiceDefinition serviceDefinition) {
    	this.serviceDefinition = serviceDefinition;
    }

    public ServiceDefinition getServiceDefinition() {
    	return serviceDefinition;
    }
    
    public void setOutputFile(File outputFile) {
    	this.outputFile = outputFile;
    }

    public File getOutputFile() {
    	return outputFile;
    }
    
}
