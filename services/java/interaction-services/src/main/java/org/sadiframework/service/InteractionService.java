package org.sadiframework.service;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.apache.log4j.Logger;
import org.sadiframework.service.simple.SimpleAsynchronousServiceServlet;
import org.sadiframework.utils.LSRNUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

abstract public class InteractionService extends SimpleAsynchronousServiceServlet
{
	private static final Logger log = Logger.getLogger(InteractionService.class);
	private static final long serialVersionUID = 1L;

	abstract protected String getResourcePathForInteractionFile();
	abstract protected String getLSRNNamespace();

	@Override
	public void processInput(Resource input, Resource output)
	{
		Model outputModel = output.getModel();
		String inputGene = LSRNUtils.getID(input, LSRNUtils.getIdentifierClass(getLSRNNamespace()));
		if (inputGene == null) {
			log.warn(String.format("skipping input %s, could not determine %s gene identifier", input, getLSRNNamespace()));
			return;
		}
		Set<String> visitedInteractors = new HashSet<String>();
		Scanner s = null;
        try {
        	s = new Scanner(InteractionService.class.getResourceAsStream(getResourcePathForInteractionFile()));
            while (s.hasNext()) {
                String gene1 = s.next();
            	String gene2 = s.next();
                if (inputGene.equals(gene1) && !visitedInteractors.contains(gene2)) {
                	Resource interactor = getLSRNNode(outputModel, getLSRNNamespace(), gene2);
                	output.addProperty(Vocab.interacts_with, interactor);
                	visitedInteractors.add(gene2);
                }
                else if (inputGene.equals(gene2) && !visitedInteractors.contains(gene1)){
                	Resource interactor = getLSRNNode(outputModel, getLSRNNamespace(), gene1);
                	output.addProperty(Vocab.interacts_with, interactor);
                }
            }
        }
        finally {
            if (s != null) {
                s.close();
            }
        }
	}

	/**
	 * Create an LSRN record in the given model or return the existing
	 * record if it already exists.  The purpose of this method is to
	 * avoid creating spurious blank node by creating the same LSRN record
	 * multiple times.
	 *
	 * @param model the model
	 * @param id the identifier
	 * @return the Resource which is the root node of the LSRN record in the model
	 */
	protected Resource getLSRNNode(Model model, String lsrnNamespace, String id)
	{
		Resource node = model.getResource(LSRNUtils.getURI(lsrnNamespace, id));
		if (!model.contains(node, RDF.type)) {
			node = LSRNUtils.createInstance(model, LSRNUtils.getClass(lsrnNamespace), id);
		}
		return node;
	}

	private static final class Vocab
	{
		private static Model m_model = ModelFactory.createDefaultModel();
		public static final Property interacts_with = m_model.createProperty("http://dev.biordf.net/~iwood/interaction.owl#interacts_with");
	}


}
