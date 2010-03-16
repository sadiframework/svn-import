package org.sadiframework.editor;

import java.util.ResourceBundle;

/**
 * 
 * @author Eddie Kawas
 *
 */
public class PerlDefinitionFieldGenerator extends
		AbstractDefinitionFieldGenerator {

	private final ResourceBundle bundle = ResourceBundle
			.getBundle("org.sadiframework.utils.i18n.EditorResourceBundle");

	@Override
	public DefinitionField[] getDefinitionFields() {
		return new DefinitionField[] {
				new DefinitionField("ServiceName", bundle
						.getString("definition_name"), TEXT_FIELD ),
				new DefinitionField("Authority", bundle
						.getString("definition_authority"), TEXT_FIELD),
				new DefinitionField("ServiceType", bundle
						.getString("definition_service_type"), DROP_TEXT_FIELD),
				new DefinitionField("InputClass", bundle
						.getString("definition_input_class"), DROP_TEXT_FIELD),
				new DefinitionField("OutputClass", bundle
						.getString("definition_output_class"), DROP_TEXT_FIELD),
				new DefinitionField("Description", bundle
						.getString("definition_description"), TEXT_FIELD),
				new DefinitionField("UniqueIdentifier", bundle
						.getString("definition_unique_id"), TEXT_FIELD),
				new DefinitionField("Authoritative", bundle
						.getString("definition_authoritative"), BOOLEAN_FIELD),
				new DefinitionField("Provider", bundle
						.getString("definition_provider"), TEXT_FIELD),
				new DefinitionField("ServiceURI", bundle
						.getString("definition_service_uri"), TEXT_FIELD),
				new DefinitionField("URL", bundle
						.getString("definition_endpoint"), TEXT_FIELD),
				new DefinitionField("SignatureURL", bundle
						.getString("definition_signature_url"), TEXT_FIELD), };
	}

}
