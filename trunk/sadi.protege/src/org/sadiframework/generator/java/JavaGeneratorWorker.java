package org.sadiframework.generator.java;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.jdesktop.swingworker.SwingWorker;
import org.sadiframework.exceptions.SADIServiceException;
import org.sadiframework.preferences.PreferenceManager;
import org.sadiframework.properties.SADIProperties;
import org.sadiframework.service.ServiceDefinition;
import org.sadiframework.utils.ZipUtilities;

public class JavaGeneratorWorker extends SwingWorker<String, Object> {
    
    /**
     * Flag for worker to perform local deploy of service
     */
    public static final String DEPLOY = "worker-deploy-service";
    /**
     * Flag for worker to perform local undeploy of service
     */
    public static final String UNDEPLOY = "worker-undeploy-service";
    /**
     * Flag for worker to create a WAR for our service
     */
    public static final String PACKAGE = "worker-package-service";
    /**
     * Flag for worker to generate a service project
     */
    public static final String GENERATE = "worker-generate-service";

    private PreferenceManager manager = PreferenceManager.newInstance();
    private final static String SADI_SKELETON_FILENAME = "sadi-service-skeleton-0.0.3.zip";
    private final static String SADI_SKELETON_PROJECT_FOLDER = "sadi.service.skeleton";

    private String outputDirectory;
    private String rootFolderName;
    private ServiceDefinition definition;
    private String servicePackage = "";
    private String extraMavenArgs = "";
    private String action = "";

    public JavaGeneratorWorker() {
        this("");
    }

    public JavaGeneratorWorker(String directory) {
        this("", "");
    }

    public JavaGeneratorWorker(String directory, String rootFolderName) {
        if (directory == null)
            directory = "";
        if (directory.trim().equals("")) {
            directory = System.getProperty("user.dir");
        }
        this.outputDirectory = directory;
        if (rootFolderName == null)
            rootFolderName = "";
        if (rootFolderName.trim().equals("")) {
            rootFolderName = SADI_SKELETON_PROJECT_FOLDER;
        }
        this.rootFolderName = rootFolderName;
        
        // listen for changes to the DO_JAVA_* preferences
        manager.addPropertyChangeListener(new PropertyChangeListener() { 
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(SADIProperties.DO_JAVA_SERVICE_GENERATION)) {
                    // cancel our task
                    if (!manager.getBooleanPreference(SADIProperties.DO_JAVA_SERVICE_GENERATION, false)) {
                        if (!isCancelled() || !isDone()) {
                            cancel(true);
                        }
                    }
                } else if (evt.getPropertyName().equals(SADIProperties.DO_JAVA_GENERATOR_CREATE_PACKAGE)) {
                    // cancel our task
                    if (!manager.getBooleanPreference(SADIProperties.DO_JAVA_GENERATOR_CREATE_PACKAGE, false)) {
                        if (!isCancelled() || !isDone()) {
                            cancel(true);
                        }
                    }
                } else if (evt.getPropertyName().equals(SADIProperties.DO_JAVA_GENERATOR_DEPLOY)) {
                    // cancel our task
                    if (!manager.getBooleanPreference(SADIProperties.DO_JAVA_GENERATOR_DEPLOY, false)) {
                        if (!isCancelled() || !isDone()) {
                            System.out.println("\r\n");
                            cancel(true);
                        }
                    }
                } else if (evt.getPropertyName().equals(SADIProperties.DO_JAVA_GENERATOR_UNDEPLOY)) {
                    // cancel our task
                    if (!manager.getBooleanPreference(SADIProperties.DO_JAVA_GENERATOR_UNDEPLOY, false)) {
                        if (!isCancelled() || !isDone()) {
                            cancel(true);
                        }
                    }
                }  
            }
        });
    }

    @Override
    protected String doInBackground() throws Exception {
        if (getAction().equals(PACKAGE)) {
            String result = mvnPackageService();
            //manager.saveBooleanPreference(SADIProperties.DO_JAVA_GENERATOR_CREATE_PACKAGE, false);
            return result;
        } else if (getAction().equals(DEPLOY)) {
            String result = mvnDeployService();
            //manager.saveBooleanPreference(SADIProperties.DO_JAVA_GENERATOR_DEPLOY, false);
            return result;
        } else if (getAction().equals(UNDEPLOY)) {
            String result = mvnUndeployService();
            //manager.saveBooleanPreference(SADIProperties.DO_JAVA_GENERATOR_UNDEPLOY, false);
            return result;
        } else if (getAction().equals(GENERATE)) {
            String result = mvnGenerateService();
            //manager.saveBooleanPreference(SADIProperties.DO_JAVA_SERVICE_GENERATION, false);
            return result;
        } else {
            return null;
        }
    }
    
    private String mvnDeployService() {
        File serviceDir = new File(this.outputDirectory, this.rootFolderName);
        // now call maven to deploy the service to localhost
        boolean success = MavenExecutor.LocalDeployService(
                serviceDir.getAbsolutePath(), 
                getExtraMavenArgs().split(" "));
        // our message
        if (success) {
            return String.format("Successfully deployed service, %s.", definition.getName());
        } else {
            return String.format("Service deployment failed. Please refer to console for any messages.");
        }
    }
    private String mvnUndeployService() {
        File serviceDir = new File(this.outputDirectory, this.rootFolderName);
        boolean success = MavenExecutor.LocalUndeployService(
                serviceDir.getAbsolutePath(), 
                getExtraMavenArgs().split(" "));
        // our message
        if (success) {
            return String.format("Successfully stopped service, %s.", definition.getName());
        } else {
            return String.format("Service un-deployment failed. Please refer to console for any messages.");
        }
    }
    
    private String mvnPackageService() {
        // now call maven to package the service into a WAR file
        boolean success = MavenExecutor.PackageService(
                String.format("%s/%s", outputDirectory, definition.getName()), 
                getExtraMavenArgs().split(" "));
        // our message
        if (success) {
            return String.format("Successfully created package for, %s, in %s.", definition.getName(), this.outputDirectory);
        } else {
            return String.format("Service generation failed. Please refer to console for any messages.");
        }
    }

    private String mvnGenerateService() {
        InputStream in = getClass().getResourceAsStream(
                String.format("/resources/%s", SADI_SKELETON_FILENAME));
        // unzip to tmp directory, rename directory, move to correct directory
        File temp = new File(this.outputDirectory, String.format("sadi_java_skeleton-%s-tmp-dir", Long.toString(System.nanoTime())));
        File serviceDir = new File(this.outputDirectory, this.rootFolderName);
        if (!serviceDir.exists()) {
            // create the directory and unzip skeleton project there
            temp.mkdirs();
            temp.deleteOnExit();
            if (temp.exists()) {
                // unzip into temp directory
                try {
                    ZipUtilities.unzip(in, temp.getAbsolutePath());
                } catch (IOException ioe) {
                    return String.format("Error inflating SADI skeleton project\n%s", ioe.getMessage());
                }
                // now move the temp directory to the outDir
                File file = new File(temp, SADI_SKELETON_PROJECT_FOLDER);
                boolean success = file.renameTo(serviceDir); 
                if (!success) { 
                    return "Could not create skeleton project for Java SADI service (x01)";
                }
            }
            temp.delete();
        }
        // now call maven to generate the actual skeleton
        boolean success = false;
        
        try {
        success = MavenExecutor.GenerateService(
                serviceDir.getAbsolutePath(), 
                definition.getName(),
                String.format("%s%s", getServicePackage(), definition.getName()),
                definition,
                getExtraMavenArgs().split(" "));
        } catch (SADIServiceException sse) {
            String s = String.format(sse.getMessage());
            System.err.println(s);
            return s;
        }
        // our message
        if (success) {
            String s = String.format("Successfully created skeleton project, %s, in %s.", this.rootFolderName, this.outputDirectory);
            System.out.println(s);
            return s;
        } else {
            String s = String.format("Service generation failed. Please refer to console for any messages.");
            System.out.println(s);
            return s; 
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jdesktop.swingworker.SwingWorker#done()
     */
    protected void done() {
        super.done();
        System.out.println(String.format("Done!"));
        if (getAction().equals(PACKAGE)) {
            manager.saveBooleanPreference(SADIProperties.DO_JAVA_GENERATOR_CREATE_PACKAGE, false);
            return;
        } else if (getAction().equals(DEPLOY)) {
            manager.saveBooleanPreference(SADIProperties.DO_JAVA_GENERATOR_DEPLOY, false);
            return;
        } else if (getAction().equals(UNDEPLOY)) {
            manager.saveBooleanPreference(SADIProperties.DO_JAVA_GENERATOR_UNDEPLOY, false);
            return;
        } else if (getAction().equals(GENERATE)) {
            manager.saveBooleanPreference(SADIProperties.DO_JAVA_SERVICE_GENERATION, false);
            return;
        }
        System.out.println(String.format("Done: NULL"));
        return;
        
    }
    
    public void setDefinition(ServiceDefinition definition) {
        if (definition != null)
            this.definition = definition;
    }
    
    public ServiceDefinition getDefinition() {
        return this.definition;
    }

    /**
     * @param servicePackage the servicePackage to set
     */
    public void setServicePackage(String servicePackage) {
        if (servicePackage != null)
            this.servicePackage = servicePackage.trim().equals("") ? String.format("com.example.sadi.MyService.") : String.format("%s.", servicePackage.trim());
    }

    /**
     * @return the servicePackage
     */
    public String getServicePackage() {
        return servicePackage;
    }

    /**
     * @param extraMavenArgs the extraMavenArgs to set
     */
    public void setExtraMavenArgs(String extraMavenArgs) {
        if (extraMavenArgs != null)
            this.extraMavenArgs = extraMavenArgs.trim();
    }

    /**
     * @return the extraMavenArgs
     */
    public String getExtraMavenArgs() {
        return extraMavenArgs;
    }

    /**
     * @return the action
     */
    public String getAction() {
        return this.action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(String action) {
        this.action = action;
    }
}
