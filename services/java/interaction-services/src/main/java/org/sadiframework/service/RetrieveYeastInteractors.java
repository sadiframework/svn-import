package org.sadiframework.service;

import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.InputClass;
import org.sadiframework.service.annotations.Name;
import org.sadiframework.service.annotations.OutputClass;
import org.sadiframework.vocab.LSRN;

@Name("RetrieveYeastInteractors")
@ContactEmail("info@sadiframework.org")
@InputClass("http://purl.oclc.org/SADI/LSRN/SGD_Record")
@OutputClass("http://sadiframework.org/interaction-services/RetrieveYeastInteractors.owl#OutputClass")
public class RetrieveYeastInteractors extends InteractionService {

	private static final long serialVersionUID = 1L;

	@Override
	protected String getResourcePathForInteractionFile() {
		return "/sgd-interactions.txt";
	}

	@Override
	protected String getLSRNNamespace() {
		return LSRN.Namespace.SGD;
	}

}
