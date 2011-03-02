/**
 * 
 */
package ca.wilkinsonlab.daggoo.listeners;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileChangeEvent;
import org.apache.commons.vfs.FileListener;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.impl.DefaultFileMonitor;
import org.apache.log4j.PropertyConfigurator;
import org.xml.sax.SAXException;

import ca.wilkinsonlab.daggoo.SAWSDLService;
import ca.wilkinsonlab.daggoo.utils.IOUtils;

/**
 * @author Eddie
 *
 */
public class ServletContextListener implements
	javax.servlet.ServletContextListener {

    /*
     * the name of our context param
     */
    private final static String INIT_PARAM_NAME = "configurable-properties";
    /**
     * KEY: The location of the configurable properties file 
     */
    public final static String PROPERTIES_LOCATION = "properties-location";
    
    /**
     * KEY: The location of our velocity templates
     */
    public final static String TEMPLATE_LOCATION = "";
    /**
     * KEY: The location of our directory containing our service files (owl, schemas, etc)
     */
    public final static String SERVICE_DIR_LOCATION = "service-dir-location";
    
    /**
     * KEY: The location of our mapping file
     */
    public final static String MAPPING_FILE_LOCATION = "mapping-file-location";
    
    /**
     * KEY: The base address of our server
     */
    public final static String SERVER_BASE_ADDRESS = "server-base-address";
    
    /**
     * KEY: The map of service names to SAWSDLServices
     */
    public final static String SAWSDL_SERVICE_MAP = "sawsdl-service-map";
    
    /**
     * KEY: The map of service names to file location
     */
    public final static String SAWSDL_SERVICE_PREFIX_MAP = "sawsdl-service-prefix-map";
    
    // map of service names to sawsdlservice
    private ConcurrentHashMap<String, SAWSDLService> services = new ConcurrentHashMap<String, SAWSDLService>();
    // map of service name to location
    //private ConcurrentHashMap<String, String> mappingPrefixes = new ConcurrentHashMap<String, String>();
    
    private static final Log log = LogFactory.getLog(ServletContextListener.class);
    private DefaultFileMonitor fm;
    
    /* (non-Javadoc)
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    public void contextDestroyed(ServletContextEvent sce) {
	ServletContext c = sce.getServletContext();
	if (c.getAttribute(PROPERTIES_LOCATION) != null) {
	    c.removeAttribute(PROPERTIES_LOCATION);
	}
	if (c.getAttribute(SERVICE_DIR_LOCATION) != null) {
	    c.removeAttribute(SERVICE_DIR_LOCATION);
	}
	if (c.getAttribute(MAPPING_FILE_LOCATION) != null) {
	    c.removeAttribute(MAPPING_FILE_LOCATION);
	}
	if (c.getAttribute(TEMPLATE_LOCATION) != null) {
	    c.removeAttribute(TEMPLATE_LOCATION);
	}
	if (c.getAttribute(SERVER_BASE_ADDRESS) != null) {
	    c.removeAttribute(SERVER_BASE_ADDRESS);
	}
	if (c.getAttribute(SAWSDL_SERVICE_MAP) != null) {
	    c.removeAttribute(SAWSDL_SERVICE_MAP);
	}
	services.clear();
	services = null;
	// stop polling
        if (fm != null)
            fm.stop();
        fm = null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    public void contextInitialized(ServletContextEvent sce) {
	final ServletContext c = sce.getServletContext();
	
	URL props = null;
	try {
	    props =  getClass().getResource("/log4j.properties");
	if(props != null) {
	    PropertyConfigurator.configure(props);
	}
	} catch (Exception e) {e.printStackTrace();}

	
	if (c != null) {
	    if (c.getInitParameter(INIT_PARAM_NAME) != null) {
		String loc = c.getInitParameter(INIT_PARAM_NAME);
		File f = null;
		try {
		    f = new File(ServletContextListener.class.getResource(loc).toURI());
		} catch (URISyntaxException e) {
		    e.printStackTrace();
		    return;
		}
		System.out.println(String.format("INIT_PARAM_LOC: %s", f.getAbsolutePath()));
		if (f.exists() && f.canRead() && f.isFile()) {
		    c.setAttribute(PROPERTIES_LOCATION, loc);
		    // TODO catch IllegalArgumentException
		    process_properties_file(c, f);
		}
	    }
	}
	
	final String serviceMappings = (String) c.getAttribute(ServletContextListener.MAPPING_FILE_LOCATION);
	final String serviceDir = (String) c.getAttribute(ServletContextListener.SERVICE_DIR_LOCATION);
	init_services(serviceMappings, serviceDir);
	try {
	    FileSystemManager fsManager = VFS.getManager();
	    // listen for changes to serviceMappings
	    FileObject fileObject = fsManager.resolveFile(serviceMappings);
	    fm = new DefaultFileMonitor(new FileListener() {

		public void fileDeleted(FileChangeEvent arg0) throws Exception {
		    // shouldn't happen
		}

		public void fileCreated(FileChangeEvent arg0) throws Exception {
		    // not necessary
		}

		public void fileChanged(FileChangeEvent arg0) throws Exception {
		    // reload our mappings ...
		    init_services(serviceMappings, serviceDir);
		    c.setAttribute(SAWSDL_SERVICE_MAP, services);
		}
	    }); 
	    fm.addFile(fileObject);
	    fm.start();
	} catch (Exception e) {
	    e.printStackTrace();
	}
	c.setAttribute(SAWSDL_SERVICE_MAP, services);
    }

    /**
     * @param context
     * @param propertiesFile
     */
    private void process_properties_file(ServletContext context, File propertiesFile) throws IllegalArgumentException{
	Properties p = new Properties();
	try {
	p.load(new FileInputStream(propertiesFile));
	if (p.containsKey("mapping-file")) {
	    if (p.getProperty("mapping-file") != null) {
		String s = p.getProperty("mapping-file");
		File f = new File (context.getRealPath(s) == null ? s : context.getRealPath(s));
		if (!f.exists()) {
		    context.setAttribute(MAPPING_FILE_LOCATION, s);
		} else {
		    context.setAttribute(MAPPING_FILE_LOCATION, f.getAbsolutePath());
		}
	    }
	}
	if (p.containsKey("templates")) {
	    if (p.getProperty("templates") != null) {
		String s = p.getProperty("templates");
		File f = new File (context.getRealPath(s) == null ? s : context.getRealPath(s));
		if (!f.exists()) {
		    context.setAttribute(TEMPLATE_LOCATION, s);
		} else {
		    context.setAttribute(TEMPLATE_LOCATION, f.getAbsolutePath());
		}
	    }
	}
	if (p.containsKey("services")) {
	    if (p.getProperty("services") != null) {
		String s = p.getProperty("services");
		File f = new File (context.getRealPath(s) == null ? s : context.getRealPath(s));
		if (!f.exists()) {
		    context.setAttribute(SERVICE_DIR_LOCATION, s);
		} else {
		    context.setAttribute(SERVICE_DIR_LOCATION, f.getAbsolutePath());
		}
	    }
	}
	if (p.containsKey("base")) {
	    if (p.getProperty("base") != null) {
		String s = p.getProperty("base");
		if (!s.endsWith("/")) {
		    s += "/";
		}
		context.setAttribute(SERVER_BASE_ADDRESS, s);
	    }
	}
	} catch (FileNotFoundException e) {
	    throw new IllegalArgumentException("Properties file was not found!", e);
	} catch (IOException e) {
	    throw new IllegalArgumentException("Problem processing Properties file!", e);
	}
	System.out.println(String.format(
		"map(%s), templates(%s), services(%s), base(%s)",
		context.getAttribute(MAPPING_FILE_LOCATION),
		context.getAttribute(TEMPLATE_LOCATION),
		context.getAttribute(SERVICE_DIR_LOCATION),
		context.getAttribute(SERVER_BASE_ADDRESS)));
    }
    
    /*
     * @param serviceMappings
     * @param serviceDir
     */
    private void init_services(String serviceMappings, String serviceDir) {
	try {
	    services = new ConcurrentHashMap<String, SAWSDLService>();
	    for (SAWSDLService s : IOUtils.getSAWSDLServices(new File(serviceMappings))) {
		if (serviceDir != null) {
		    File realPath = new File(serviceDir,
			    s.getWsdlLocation());
		    s.setWsdlLocation(realPath.getAbsolutePath());
		    // a lowering/lifing mapping prefix for later use
		    //mappingPrefixes.put(s.getName(), new File(serviceDir).toURI().toURL().toString());
		}
		services.put(s.getName(), s);
	    }
	} catch (SAXException e) {
	    // TODO throw exception?
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO throw exception?
	    e.printStackTrace();
	}
	log.info(services);
    }

}
