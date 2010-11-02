package org.sadiframework.properties;

public class SADIProperties {

    // generator preference keys
    //public static final String SERVICE_GEN_USE_PERL = "service-generator-use-perl"; // boolean
    public static final String DATATYPE_GEN_USE_PERL = "datatype-generator-use-perl"; // boolean
    public static final String GEN_SELECTED_OWL_CLASS = "generator-selected-owl-class"; // String
    public static final String GENERATOR_SERVICE_ASYNC = "generator-service-is-async"; // boolean
    public static final String GENERATOR_GENERATE_SERVICE = "generator-service-generate-service"; // boolean
    public static final String GENERATOR_GENERATE_OWL = "generator-service-generate-owl-code"; // boolean
    public static final String GENERATOR_SERVICE_NAME = "generator-service-name"; // string
    public static final String GENERATOR_OWL_ONT_FILENAME = "generator-service-name"; // string
    public static final String GENERATOR_OWL_BY_FILE = "generator-owl-by-file"; // boolean
    public static final String GENERATOR_OWL_TMP_FILE_LOCATION = "generator-owl-tmp-file-location"; // boolean
    public static final String GENERATOR_DO_BOTH_GENERATION = "generator-gen-datatypes-and-service"; // boolean
    // PERL preference keys
    
    public static final String DO_PERL_SERVICE_GENERATION = "perl-service-generator-processing"; // boolean
    public static final String DO_PERL_DATATYPE_GENERATION = "perl-datatype-generator-processing"; // boolean
    public static final String PERL_SADI_HOME_DIRECTORY = "perl-sadi-home-directory"; // string
    public static final String PERL_SADI_DEFINITION_DIRECTORY = "perl-sadi-definition-directory"; // string

    // JAVA preference keys
    /**
     * The JAVA package for the service we will generate
     */
    public static final String JAVA_GENERATOR_SERVICE_PACKAGE = "java-generator-service-package"; // string
    /**
     * The JAVA service working directory
     */
    public static final String JAVA_SERVICE_SKELETONS_WORKING_DIR = "java-service-skeletons-working-dir"; // string
    /**
     * Extra maven arguments to pass to our embedded maven engine
     */
    public static final String JAVA_SERVICE_EXTRA_MAVEN_ARGS = "java-service-extra-maven-arguments"; // string
    /**
     * Boolean: are we currently generating a service in JAVA
     */
    public static final String DO_JAVA_SERVICE_GENERATION = "java-service-generator-processing"; // boolean
    /**
     * Boolean: are we currently generating datatypes from OWL ontologies in JAVA
     */
    public static final String DO_JAVA_DATATYPE_GENERATION = "java-datatype-generator-processing"; // boolean
    /**
     * Boolean: are we currently deploying our service to localhost
     */
    public static final String DO_JAVA_GENERATOR_DEPLOY = "java-generator-deploy-local-service"; // boolean
    /**
     * Boolean: are we currently un-deploying our service to localhost
     */
    public static final String DO_JAVA_GENERATOR_UNDEPLOY = "java-generator-un-deploy-local-service"; // boolean
    /**
     * Boolean: are we currently packaging our service as a WAR file
     */
    public static final String DO_JAVA_GENERATOR_CREATE_PACKAGE = "java-generator-create-war-file"; // boolean
    /**
     * String: the name of the project (place to generate 1 or more services) for the java builder
     */
    public static final String JAVA_SERVICE_SKELETONS_PROJECT_NAME = "java-generator-project-name";
}
