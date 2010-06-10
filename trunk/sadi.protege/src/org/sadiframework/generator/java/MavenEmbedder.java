package org.sadiframework.generator.java;

import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * A class that embeds maven
 * @author Eddie Kawas
 *
 */
public class MavenEmbedder {

    private final PlexusContainer plexus;

    private final Maven maven;

    private final MavenExecutionRequestPopulator populator;

    //private final MavenConsole console;
    final String mavenCoreRealmId = "sadi.plexus.core";
    
    /**
     * Constructs an embedder object and loads the appropriate libraries so that we can use an embedded maven
     * @throws ComponentLookupException
     * @throws PlexusContainerException
     */
    public MavenEmbedder()
            throws ComponentLookupException, PlexusContainerException {
        ClassLoader cl = this.getClass().getClassLoader();//Maven.class.getClassLoader();    
        ContainerConfiguration cc = new DefaultContainerConfiguration().setClassWorld(new ClassWorld(mavenCoreRealmId, cl)).setName("sadiMavenCore");
        PlexusContainer plexus = new DefaultPlexusContainer(cc);
        this.plexus = plexus;
        this.populator = plexus.lookup(MavenExecutionRequestPopulator.class);
        this.maven = plexus.lookup(Maven.class);
    }

    /**
     * 
     * @param request the MavenExecutionRequest to process
     * @return a MavenExecutionResult for valid requests; null otherwise
     */
    public MavenExecutionResult execute(MavenExecutionRequest request) {
        if (request == null) {
            return null;
        }
        MavenExecutionResult result;
        try {
            populator.populateDefaults(request);
            result = maven.execute(request);
        } catch (MavenExecutionRequestPopulationException ex) {
            ex.printStackTrace();
            result = new DefaultMavenExecutionResult();
            result.addException(ex);
        } catch (Exception e) {
            e.printStackTrace();
            result = new DefaultMavenExecutionResult();
            result.addException(e);
        }
        return result;
    }

    /**
     * @return the plexus
     */
    public PlexusContainer getPlexus() {
        return this.plexus;
    }
}
