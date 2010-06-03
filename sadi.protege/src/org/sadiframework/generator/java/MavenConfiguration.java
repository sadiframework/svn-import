package org.sadiframework.generator.java;

import java.io.File;

public class MavenConfiguration {

    private String globalSettingsFile;
    private String userSettingsFile;
    
    
    
    public static final String userHome = System.getProperty( "user.home" );
    
    public static final File userMavenConfigurationHome = new File( userHome, ".m2" );
    
    public static final File DEFAULT_USER_SETTINGS_FILE = new File( userMavenConfigurationHome, "settings.xml" );
    
    public static final File DEFAULT_GLOBAL_SETTINGS_FILE = new File( System.getProperty( "maven.home", System.getProperty( "user.dir", "" ) ), "conf/settings.xml" ); 
    
    public MavenConfiguration() {
        globalSettingsFile = DEFAULT_GLOBAL_SETTINGS_FILE.getAbsolutePath();
        userSettingsFile = DEFAULT_USER_SETTINGS_FILE.getAbsolutePath();
    }

    /**
     * @return the globalSettingsFile
     */
    public String getGlobalSettingsFile() {
        return this.globalSettingsFile;
    }

    /**
     * @param globalSettingsFile the globalSettingsFile to set
     */
    public void setGlobalSettingsFile(String globalSettingsFile) {
        this.globalSettingsFile = globalSettingsFile;
    }

    /**
     * @return the userSettingsFile
     */
    public String getUserSettingsFile() {
        return this.userSettingsFile;
    }

    /**
     * @param userSettingsFile the userSettingsFile to set
     */
    public void setUserSettingsFile(String userSettingsFile) {
        this.userSettingsFile = userSettingsFile;
    }
    
    
}
