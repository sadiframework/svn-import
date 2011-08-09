package ca.wilkinsonlab.sadi.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.SADIException;
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
	public Collection<Service> findServicesByPredicate(final String predicate)
	{
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.findServicesByPredicate(predicate);
			}	
		});
	}
	
	public Collection<Service> findServicesByInputClass(final String inputClassUri)
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
	public Collection<Service> findServicesByInputInstance(final Resource subject)
	{
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.findServicesByInputInstance(subject);
			}	
		});
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.registry.Registry#findServices(com.hp.hpl.jena.rdf.model.Resource, java.lang.String)
     */
	public Collection<Service> findServices(final Resource subject, final String predicate)
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
	public Collection<ServiceInputPair> discoverServices(final Model model)
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
	public Collection<String> findPredicatesBySubject(final Resource subject)
	{
		return (Collection<String>) accumulate(new Accumulator<String>() {
			public Collection<String> get(Registry registry) throws Exception {
				return registry.findPredicatesBySubject(subject);
			}	
		});
	}
	
	public Collection<Service> getAllServices() {
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.getAllServices();
			}	
		});
	}

	public ServiceStatus getServiceStatus(String serviceURI) throws SADIException {
		for (Registry registry: registries) {
			try {
				Service service = registry.getService(serviceURI);
				if (service != null)
					return registry.getServiceStatus(serviceURI);
			} catch (Exception e) {
				log.error(String.format("error contacting registry %s", registry), e);
			}
		}
		return null;
	}

	@Override
	public Collection<Service> findServicesByInputClass(final OntClass clazz) throws SADIException
	{
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.findServicesByInputClass(clazz);
			}
		});
	}

	@Override
	public Collection<Service> findServicesByInputClass(final OntClass clazz, final boolean withReasoning) throws SADIException
	{
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.findServicesByInputClass(clazz, withReasoning);
			}
		});
	}

	@Override
	public Collection<Service> findServicesByConnectedClass(final OntClass clazz) throws SADIException
	{
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.findServicesByConnectedClass(clazz);
			}
		});
	}

	@Override
	public Collection<Service> findServicesByConnectedClass(final OntClass clazz, final boolean withReasoning) throws SADIException
	{
		return accumulate(new Accumulator<Service>() {
			public Collection<? extends Service> get(Registry registry) throws Exception {
				return registry.findServicesByConnectedClass(clazz, withReasoning);
			}
		});
	}
	
	private <T> Collection<T> accumulate(Accumulator<T> accum)
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

	private interface Accumulator<T>
	{	
		public Collection<? extends T> get(Registry registry) throws Exception;
	}
}
