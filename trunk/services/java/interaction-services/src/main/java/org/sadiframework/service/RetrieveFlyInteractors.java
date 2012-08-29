package org.sadiframework.service;

import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.InputClass;
import org.sadiframework.service.annotations.Name;
import org.sadiframework.service.annotations.OutputClass;
import org.sadiframework.service.annotations.URI;
import org.sadiframework.vocab.LSRN;

@URI("http://sadiframework.org/interaction-services/RetrieveFlyInteractors")
@Name("RetrieveFlyInteractors")
@ContactEmail("info@sadiframework.org")
@InputClass("http://purl.oclc.org/SADI/LSRN/FLYBASE_Record")
@OutputClass("http://sadiframework.org/interaction-services/RetrieveFlyInteractors.owl#OutputClass")
public class RetrieveFlyInteractors extends InteractionService {

	private static final long serialVersionUID = 1L;

	@Override
	protected String getResourcePathForInteractionFile() {
		return "/flybase-interactions.txt";
	}

	@Override
	protected String getLSRNNamespace() {
		return LSRN.Namespace.FLYBASE;
	}

}
