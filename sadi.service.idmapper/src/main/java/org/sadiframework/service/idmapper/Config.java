package org.sadiframework.service.idmapper;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;

public class Config
{
	protected static final Log log = LogFactory.getLog(Config.class);
	public static final String CONFIG_FILENAME = "idmapper.properties";
	public static final String CONFIG_URI_PREFIX_SUBSET = "uri.prefix";
	public static final String CONFIG_NAMESPACE_SUBSET = "uniprot.namespace";
	public static final String CONFIG_RELATIONSHIP_SUBSET = "relationship";
	protected static Config theInstance = null;
	protected Configuration config;
	protected PrefixMapping prefixMapping;
	protected BiMap<String, String> uniprotToLSRN;
	protected Map<LSRNNamespacePair,String> lsrnRelationships;

	protected Config() {
		try {
			config = new PropertiesConfiguration(CONFIG_FILENAME);
		} catch (ConfigurationException e) {
			log.warn(String.format("error reading %s: %s", CONFIG_FILENAME, e));
			config = new PropertiesConfiguration();
		}
		initPrefixMapping();
		initNamespaceMap();
		initRelationshipMap();
	}

	static synchronized public Config getInstance() {
		if (theInstance == null)
			theInstance = new Config();
		return theInstance;
	}

	protected void initPrefixMapping() {
		prefixMapping = new PrefixMappingImpl();
		Configuration prefixConfig = config.subset(CONFIG_URI_PREFIX_SUBSET);
		for (Iterator<?> i = prefixConfig.getKeys(); i.hasNext(); ) {
			String key = (String)i.next();
			String uri = prefixConfig.getString(key);
			prefixMapping.setNsPrefix(key, uri);
		}
	}

	protected void initNamespaceMap() {
		uniprotToLSRN = Maps.synchronizedBiMap(HashBiMap.<String, String>create());
		Configuration namespaceConfig = config.subset(CONFIG_NAMESPACE_SUBSET);
		for (Iterator<?> i = namespaceConfig.getKeys(); i.hasNext(); ) {
			String uniprotNamespace = (String)i.next();
			String lsrnNamespace = namespaceConfig.getString(uniprotNamespace);
			uniprotToLSRN.put(uniprotNamespace, lsrnNamespace);
		}
	}

	protected void initRelationshipMap() {
		lsrnRelationships = Collections.synchronizedMap(new LinkedHashMap<LSRNNamespacePair,String>());
		Configuration relationshipConfig = config.subset(CONFIG_RELATIONSHIP_SUBSET);
		for (Iterator<?> i = relationshipConfig.getKeys(); i.hasNext(); ) {
			String key = (String)i.next();
			String namespaces[] = key.split("\\.", 2);
			if (namespaces.length == 2) {
				LSRNNamespacePair namespacePair = new LSRNNamespacePair(namespaces[0], namespaces[1]);
				String relationshipURI = relationshipConfig.getString(key);
				relationshipURI = prefixMapping.expandPrefix(relationshipURI);
				lsrnRelationships.put(namespacePair, relationshipURI);
			}
		}
	}

	public String getRelationshipURI(String sourceLSRNNamespace, String targetLSRNNamespace) {
		return lsrnRelationships.get(new LSRNNamespacePair(sourceLSRNNamespace, targetLSRNNamespace));
	}

	public Set<LSRNNamespacePair> getServiceNamespacePairs() {
		return lsrnRelationships.keySet();
	}

	public String getLSRNNamespace(String uniprotNamespace) {
		return uniprotToLSRN.get(uniprotNamespace);
	}

	public String getUniprotNamespace(String lsrnNamespace) {
		return uniprotToLSRN.inverse().get(lsrnNamespace);
	}

	public static final class LSRNNamespacePair {
		protected String sourceNamespace;
		protected String targetNamespace;
		public LSRNNamespacePair(String sourceNamespace, String targetNamespace) {
			this.sourceNamespace = sourceNamespace;
			this.targetNamespace = targetNamespace;
		}
		public String getSourceNamespace() {
			return sourceNamespace;
		}
		public String getTargetNamespace() {
			return targetNamespace;
		}
		@Override
		public boolean equals(Object other) {
			if (other instanceof LSRNNamespacePair) {
				LSRNNamespacePair otherPair = (LSRNNamespacePair)other;
				return (this.getSourceNamespace().equals(otherPair.getSourceNamespace()) &&
					this.getTargetNamespace().equals(otherPair.getTargetNamespace()));
			}
			return false;
		}
		@Override
		public int hashCode() {
			return (getSourceNamespace() + getTargetNamespace()).hashCode();
		}
	}

}
