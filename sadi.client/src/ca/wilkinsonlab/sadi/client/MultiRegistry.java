package ca.wilkinsonlab.sadi.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A class that aggregates results from several Registry objects.
 * This would be so much easier in Python...
 */
public class MultiRegistry implements Registry
{
	public static final Logger log = Logger.getLogger(MultiRegistry.class);
	
	List<Registry> registries;
	
	/**
	 * Constructs a new Registry that will aggregate results from the
	 * specified list of component registries.
	 * @param registries the component registries
	 */
	public MultiRegistry(List<Registry> registries)
	{
		this.registries = registries;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.registry.Registry#getPredicateOntology()
     */
	public OntModel getPredicateOntology()
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.registry.Registry#getService(java.lang.String)
     */
	public Service getService(String serviceURI)
	{
		for (Registry registry: registries) {
			try {
				Service service = registry.getService(serviceURI);
				if (service != null)
					return service;
			} catch (Exception e) {
				log.error(String.format("error contacting registry %s", registry), e);
			}
		}
		return null;
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.registry.Registry#findServicesByPredicate(java.lang.String)
     */
	public Collection<? extends Service> findServicesByPredicate(final String predicate)
	{
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.findServicesByPredicate(predicate);
			}	
		});
	}
	
	public Collection<? extends Service> findServicesByInputClass(final String inputClassUri)
	{
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				Collection<Service> services = new ArrayList<Service>();
				for (Service service: registry.getAllServices()) {
					OntClass inputClass = service.getInputClass();
					if (inputClass != null)
						if (inputClass.getURI().equals(inputClassUri))
							services.add(service);
				}
				return services;
			}
		});
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.registry.Registry#findServices(com.hp.hpl.jena.rdf.model.Resource)
     */
	public Collection<? extends Service> findServices(final Resource subject)
	{
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.findServices(subject);
			}	
		});
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.registry.Registry#findServices(com.hp.hpl.jena.rdf.model.Resource, java.lang.String)
     */
	public Collection<? extends Service> findServices(final Resource subject, final String predicate)
	{
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.findServices(subject, predicate);
			}	
		});
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.registry.Registry#discoverServices(com.hp.hpl.jena.rdf.model.Model)
     */
	public Collection<? extends ServiceInputPair> discoverServices(final Model model)
	{
		return accumulate(new Accumulator<ServiceInputPair>() {
			public Collection<? extends ServiceInputPair> get(Registry registry) throws Exception {
				return registry.discoverServices(model);
			}	
		});
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.registry.Registry#findPredicatesBySubject(com.hp.hpl.jena.rdf.model.Resource)
     */
	@SuppressWarnings("unchecked")
	public Collection<String> findPredicatesBySubject(final Resource subject)
	{
		return (Collection<String>) accumulate(new Accumulator<String>() {
			public Collection<String> get(Registry registry) throws Exception {
				return registry.findPredicatesBySubject(subject);
			}	
		});
	}
	
	private <T> Collection<? extends T> accumulate(Accumulator<T> accum)
	{
		Collection<T> results = new HashSet<T>();
		for (Registry registry: registries) {
			try {
				results.addAll(accum.get(registry));
			} catch (Exception e) {
				log.error(String.format("error contacting registry %s", registry), e);
			}
		}
		return results;
	}
	
	public Collection<Service> getAllServices() throws IOException {
		throw new UnsupportedOperationException();
	}

	public ServiceStatus getServiceStatus(String serviceURI) throws IOException {
		throw new UnsupportedOperationException();
	}

	private interface Accumulator<T>
	{	
		public Collection<? extends T> get(Registry registry) throws Exception;
	}


}
