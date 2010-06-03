package org.sadiframework.generator.java;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.maven.cli.CLIManager;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.properties.internal.EnvironmentUtils;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.sadiframework.exceptions.SADIServiceException;

/**
 * A maven utility that class that calls an embedded maven to perform predefined goals
 * @author Eddie Kawas
 *
 */
public class MavenExecutor {

    // TODO - TEST WITHOUT SETTINGS.xml
    public static final String GENERATE_SERVICE = "ca.wilkinsonlab.sadi:sadi-generator:generate-service";

    public static final String PACKAGE_SERVICE = "package";
    public static final String COMPILE_SERVICE = "compile";
    public static final String RUN_SERVICE = "jetty:run";
    public static final String STOP_SERVICE = "jetty:stop";
    public static final String CLEAN = "clean";

    /**
     * 
     * @param directory the service main directory
     * @param serviceName the name of the service to generate
     * @param serviceClass the JAVA class that will contain the impl of the service
     * @param inputClass the OWL input class for our SADI service
     * @param outputClass the OWL output class for our SADI service
     * @param isAsync true if the service to generate is Asynchronous, false otherwise
     * @param extraOptions any other maven command line options
     * @return true if generation succeeded, false otherwise
     */
    public static boolean GenerateService(String directory, String serviceName,
            String serviceClass, String inputClass, String outputClass, boolean isAsync,
            String[] extraOptions) throws SADIServiceException {
        if (serviceName == null || serviceClass == null || inputClass == null
                || outputClass == null) {
            throw new SADIServiceException("One of {Service Name, inputClass, outputClass} was missing.\nPlease ensure that you have specified those fields.");
        }
        if (serviceName.trim().equals("") || serviceClass.trim().equals("")
                || inputClass.trim().equals("") || outputClass.trim().equals("")) {
            throw new SADIServiceException("One of {Service Name, inputClass, outputClass} is an empty string.\nPlease ensure that you have specified those fields.");
        }
        Properties systemProperties = new Properties();
        Properties userProperties = new Properties();

        ArrayList<String> params = new ArrayList<String>(Arrays.asList(extraOptions));

        params.add(String.format("-DserviceName=%s", serviceName.trim()));
        params.add(String.format("-DserviceClass=%s", serviceClass.trim()));
        params.add(String.format("-DinputClass=%s", inputClass.trim()));
        params.add(String.format("-DoutputClass=%s", outputClass.trim()));

        CLIManager cliManager = new CLIManager();
        CommandLine cli = null;
        try {
            cli = cliManager.parse(params.toArray(new String[] {}));
        } catch (ParseException e) {
            return false;
        }
        if (cli == null)
            return false;
        populateProperties(cli, systemProperties, userProperties);

        try {
            directory = directory.trim();
            MavenEmbedder m = new MavenEmbedder();
            MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory(
                    new File(directory)).setGoals(Arrays.asList(new String[] { GENERATE_SERVICE }));

            request.setPom(new File(directory, "pom.xml"));
            request.setSystemProperties(systemProperties);
            request.setUserProperties(userProperties);
            setVerbatimLevels(cli, request);
            MavenExecutionResult result = m.execute(request);
            if (result == null || result.getBuildSummary(result.getProject()) instanceof BuildFailure) {
                return false;
            }
            return true;
        } catch (ComponentLookupException e) {
            e.printStackTrace();
            return false;
        } catch (PlexusContainerException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 
     * @param directory the service main directory
     * @param extraOptions any other maven command line options
     * @return true if we were able to deploy the service, false otherwise
     */
    public static boolean LocalDeployService(String directory, String[] extraOptions) {
        Properties systemProperties = new Properties();
        Properties userProperties = new Properties();

        ArrayList<String> params = new ArrayList<String>(Arrays.asList(extraOptions));
        CLIManager cliManager = new CLIManager();
        CommandLine cli = null;
        try {
            cli = cliManager.parse(params.toArray(new String[] {}));
        } catch (ParseException e) {
            return false;
        }
        if (cli == null)
            return false;
        populateProperties(cli, systemProperties, userProperties);
        try {
            directory = directory.trim();
            MavenEmbedder m = new MavenEmbedder();
            MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory(
                    new File(directory)).setGoals(
                    Arrays.asList(new String[] { RUN_SERVICE }));

            request.setPom(new File(directory, "pom.xml"));
            request.setThreadCount("1");
            request.setSystemProperties(systemProperties);
            request.setUserProperties(userProperties);
            setVerbatimLevels(cli, request);
            MavenExecutionResult result = m.execute(request);
            if (result == null || result.getBuildSummary(result.getProject()) instanceof BuildFailure) {
                return false;
            }
            return true;
        } catch (ComponentLookupException e) {
            e.printStackTrace();
            return false;
        } catch (PlexusContainerException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 
     * @param directory the service main directory
     * @param extraOptions any other maven command line options
     * @return true if we were able to un-deploy the service, false otherwise
     */
    public static boolean LocalUndeployService(String directory, String[] extraOptions) {
        Properties systemProperties = new Properties();
        Properties userProperties = new Properties();

        ArrayList<String> params = new ArrayList<String>(Arrays.asList(extraOptions));
        
        CLIManager cliManager = new CLIManager();
        CommandLine cli = null;
        try {
            cli = cliManager.parse(params.toArray(new String[] {}));
        } catch (ParseException e) {
            return false;
        }
        if (cli == null)
            return false;
        populateProperties(cli, systemProperties, userProperties);

        try {
            directory = directory.trim();
            MavenEmbedder m = new MavenEmbedder();
            MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory(
                    new File(directory)).setGoals(
                    Arrays.asList(new String[] {STOP_SERVICE }));

            request.setPom(new File(directory, "pom.xml"));
            request.setSystemProperties(systemProperties);
            setVerbatimLevels(cli, request);
            MavenExecutionResult result = m.execute(request);
            if (result == null || result.getBuildSummary(result.getProject()) instanceof BuildFailure) {
                return false;
            }
            return true;
        } catch (ComponentLookupException e) {
            e.printStackTrace();
            return false;
        } catch (PlexusContainerException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 
     * @param directory the service main directory
     * @param extraOptions any other maven command line options
     * @return true if we could compile the project, false otherwise
     */
    public static boolean CompileService(String directory, String[] extraOptions) {
        Properties systemProperties = new Properties();
        Properties userProperties = new Properties();

        ArrayList<String> params = new ArrayList<String>(Arrays.asList(extraOptions));
        CLIManager cliManager = new CLIManager();
        CommandLine cli = null;
        try {
            cli = cliManager.parse(params.toArray(new String[] {}));
        } catch (ParseException e) {
            return false;
        }
        if (cli == null)
            return false;
        populateProperties(cli, systemProperties, userProperties);
        try {
            directory = directory.trim();
            MavenEmbedder m = new MavenEmbedder( );
            MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory(
                    new File(directory)).setGoals(
                    Arrays.asList(new String[] { "clean", COMPILE_SERVICE }));

            request.setPom(new File(directory, "pom.xml"));
            request.setSystemProperties(systemProperties);
            setVerbatimLevels(cli, request);
            MavenExecutionResult result = m.execute(request);
            if (result == null || result.getBuildSummary(result.getProject()) instanceof BuildFailure) {
                return false;
            }
            return true;
        } catch (ComponentLookupException e) {
            e.printStackTrace();
            return false;
        } catch (PlexusContainerException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 
     * @param directory the service main directory
     * @param extraOptions any other maven command line options
     * @return true if we successfully created the WAR for the SADI service project, false otherwise
     */
    public static boolean PackageService(String directory, String[] extraOptions) {
        boolean success = true;
        Properties systemProperties = new Properties();
        Properties userProperties = new Properties();

        ArrayList<String> params = new ArrayList<String>(Arrays.asList(extraOptions));
        CLIManager cliManager = new CLIManager();
        CommandLine cli = null;
        try {
            cli = cliManager.parse(params.toArray(new String[] {}));
        } catch (ParseException e) {
            return false;
        }
        if (cli == null)
            return false;
        populateProperties(cli, systemProperties, userProperties);
        try {
            directory = directory.trim();
            MavenEmbedder m = new MavenEmbedder();
            MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory(
                    new File(directory)).setGoals(
                    Arrays.asList(new String[] { "clean", PACKAGE_SERVICE }));

            request.setPom(new File(directory, "pom.xml"));
            request.setSystemProperties(systemProperties);
            request.setUserProperties(userProperties);
            setVerbatimLevels(cli, request);
            MavenExecutionResult result = m.execute(request);
            if (result == null || result.getBuildSummary(result.getProject()) instanceof BuildFailure) {
                success = false;
            }
        } catch (ComponentLookupException e) {
            e.printStackTrace();
            success = false;
        } catch (PlexusContainerException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    private static void setVerbatimLevels(CommandLine cli, MavenExecutionRequest request) {
        boolean debug = cli.hasOption(CLIManager.DEBUG);
        boolean quiet = !debug && cli.hasOption(CLIManager.QUIET);
        if (debug) {
            request.setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_DEBUG);
        } else if (quiet) {
            request.setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_ERROR);
        } else {
            request.setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_INFO);
        }
    }

    private static void populateProperties(CommandLine commandLine, Properties systemProperties,
            Properties userProperties) {
        EnvironmentUtils.addEnvVars(systemProperties);
        if (commandLine.hasOption(CLIManager.SET_SYSTEM_PROPERTY)) {
            String[] defStrs = commandLine.getOptionValues(CLIManager.SET_SYSTEM_PROPERTY);

            if (defStrs != null) {
                for (int i = 0; i < defStrs.length; ++i) {
                    setCliProperty(defStrs[i], userProperties);
                }
            }
        }

        systemProperties.putAll(System.getProperties());
    }

    private static void setCliProperty(String property, Properties properties) {
        String name;

        String value;

        int i = property.indexOf("=");

        if (i <= 0) {
            name = property.trim();

            value = "true";
        } else {
            name = property.substring(0, i).trim();

            value = property.substring(i + 1);
        }

        properties.setProperty(name, value);
        System.setProperty(name, value);
    }
}
