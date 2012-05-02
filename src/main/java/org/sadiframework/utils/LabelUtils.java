package org.sadiframework.utils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.DataRange;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;
import com.hp.hpl.jena.util.iterator.MapFilter;
import com.hp.hpl.jena.util.iterator.MapFilterIterator;
import com.hp.hpl.jena.util.iterator.NullIterator;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

public class LabelUtils
{
	private static final Logger log = Logger.getLogger(LabelUtils.class);
	
	/**
	 * Returns an iterator over the labels for the specified subject
	 * in any language.
	 * @param subject the subject
	 * @return an iterator over the labels for the subject
	 */
	public static ExtendedIterator<Label> getLabels(Resource subject)
	{
		return getLabelIterator(subject, null);
	}
	
	/**
	 * Returns an iterator over the labels for the specified subject 
	 * matching the specified language.
	 * @param subject the subject
	 * @param language the language
	 * @return an iterator over the labels for the subject and language
	 */
	public static ExtendedIterator<String> getLabels(Resource subject, String language)
	{
		return new MapFilterIterator<Label, String>(labelToStringFilter, getLabelIterator(subject, language));
	}
	
	/**
	 * Returns a label for the specified subject.
	 * If there is more than one such label, return one at random,
	 * preferentially one whose language matches the current locale.
	 * If there are no labels defined, return a default label 
	 * (see {@link #getDefaultLabel(Resource)} for details).
	 * @param subject the subject
	 * @return a label for the specified subject
	 */
	public static String getLabel(Resource subject)
	{
		String localeLang = Locale.getDefault().getLanguage();
		ExtendedIterator<Label> labels = getLabels(subject);
		String match = getLabelBestMatchingLanguage(labels, localeLang);
		labels.close();
		if (match != null)
			return match;
		else
			return getDefaultLabel(subject);
	}
	
	/**
	 * Returns a label for the specified subject matching the
	 * specified language.
	 * If there are no labels defined, return null.
	 * @param subject the subject
	 * @param language the language
	 * @return a label for the subject and language
	 */
	public static String getLabel(Resource subject, String language)
	{
		String match = null;
		ExtendedIterator<Label> labels = getLabelIterator(subject, language);
		if (labels.hasNext()) {
			match = labels.next().getText();
		}
		labels.close();
		return match;
	}
	
	/**
	 * Returns a default label for the specified subject. 
	 * If the subject has a URI, the local name of that URI will be used. 
	 * If the subject is an anonymous property restriction, a description 
	 * of the restriction will be used. 
	 * If none of these are true, subject.toString() will be used.
	 * @param subject the subject
	 * @return a default label for the specified subject
	 */
	public static String getDefaultLabel(Resource subject)
	{
		if (subject.isURIResource()) {
			try {
				URI uri = new URI(subject.getURI());
				String fragment = uri.getFragment();
				if (fragment != null)
					return fragment;
				
				String path = StringUtils.defaultString(uri.getPath(), uri.toASCIIString());
				try {
					path = URLDecoder.decode(path, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// I don't think this should happen...
					log.error(String.format("error URL-decoding %s", path), e);
				}
				if (StringUtils.contains(path, "/"))
					return StringUtils.substringAfterLast(path, "/");
				else if (StringUtils.contains(path, ":"))
					return StringUtils.substringAfterLast(path, ":");
				else
					return path;
			} catch (URISyntaxException e) {
				// I don't think this should happen...
				log.error(String.format("failed to parse URI %s", subject.getURI()), e);
			}
		}
		
		// if we get here, there was no URI or we couldn't parse it...
		if (subject.canAs(OntClass.class))
			return LabelUtils.getClassString(subject.as(OntClass.class));
		
		return subject.toString();
	}
	
	/**
	 * Returns a description for the specified subject.
	 * If there is more than one such label, return one at random,
	 * preferentially one whose language matches the current locale.
	 * If there are no descriptions defined, return null.
	 * @param subject the subject
	 * @return a description for the specified subject
	 */
	public static String getDescription(Resource subject)
	{
		String localeLang = Locale.getDefault().getLanguage();
		ExtendedIterator<Label> labels = getDescriptionIterator(subject, null);
		String match = getLabelBestMatchingLanguage(labels, localeLang);
		labels.close();
		return match;
	}
	
	/**
	 * Returns a description for the specified subject matching the
	 * specified language.
	 * If there are no descriptions defined, return null.
	 * @param subject the subject
	 * @param language the language
	 * @return a description for the subject and language
	 */
	public static String getDescription(Resource subject, String language)
	{
		String match = null;
		ExtendedIterator<Label> labels = getDescriptionIterator(subject, language);
		if (labels.hasNext()) {
			match = labels.next().getText();
		}
		labels.close();
		return match;
	}
	
	/**
	 * Returns a label for the specified RDF node.
	 * @param node the RDF node
	 * @return a label for the RDF node
	 */
	public static String toString(RDFNode node)
	{
		if (node.isResource())
			return getLabel(node.asResource());
		else
			return node.toString();
	}

	/**
	 * Returns a description of the specified OWL restriction
	 * @param r the OWL restriction
	 * @return a description of the OWL restriction
	 */
	public static String getRestrictionString(Restriction r)
	{
		StringBuilder buf = new StringBuilder();
		try {
			buf.append(getLabel(r.getOnProperty()));
		} catch (ConversionException e) {
			/* if the property is undefined in the ontology model, we won't
			 * get an OntProperty; in this case, we should be able to get a
			 * Property by finding the value of OWL.onProperty...
			 *  this happens if the property is undefined in the ontology model...
			 */
			if (r.hasProperty(OWL.onProperty))
				buf.append(toString(r.getProperty(OWL.onProperty).getObject()));
			else
				buf.append("unknown property");
		}
		if (r.isMinCardinalityRestriction()) {
			buf.append(" min ");
			buf.append(r.asMinCardinalityRestriction().getMinCardinality());
		} else if (r.isMaxCardinalityRestriction()) {
			buf.append(" max ");
			buf.append(r.asMaxCardinalityRestriction().getMaxCardinality());
		} else if (r.isCardinalityRestriction()) {
			buf.append(" exactly ");
			buf.append(r.asCardinalityRestriction().getCardinality());
		}
		if (r.isAllValuesFromRestriction()) {
			buf.append(" only ");
			buf.append(getLabel(r.asAllValuesFromRestriction().getAllValuesFrom()));
		} else if (r.isSomeValuesFromRestriction()) {
			buf.append(" some ");
			buf.append(getLabel(r.asSomeValuesFromRestriction().getSomeValuesFrom()));
		} else if (r.isHasValueRestriction()) {
			buf.append(" value ");
			buf.append(toString(r.asHasValueRestriction().getHasValue()));
		}
		return buf.toString();
	}
	
	public static String getClassString(OntClass c)
	{
		String s = "unknown class";
		if (c.isRestriction())
			s = LabelUtils.getRestrictionString(c.asRestriction());
		else if (c.isUnionClass())
			s = joinOperands(" or ", c.asUnionClass().getOperands());
		else if (c.isIntersectionClass())
			s = joinOperands(" and ", c.asIntersectionClass().getOperands());
		else if (c.isComplementClass())
			s = String.format("not %s", getLabel(c.asComplementClass().getOperand()));
		else if (c.isDataRange())
			s = String.format("one of %s", joinOperands(", ", c.asDataRange().getOneOf()));
		return String.format("(%s)", s);
	}
	
	public static String getDataRangeString(DataRange range)
	{
		return String.format("one of %s", joinOperands(", ", range.getOneOf()));
	}
	
	static String joinOperands(String conjunction, RDFList operands)
    {
    	/* sort the operands so that our labels are predictable...
    	 */
    	List<String> labels = new MapFilterIterator<RDFNode, String>(rdfNodeToStringFilter, operands.iterator()).toList();
    	Collections.sort(labels, labelComparator);
		return StringUtils.join(labels, conjunction);
	}
    private static final Comparator<String> labelComparator = new LabelComparator();
    private static final class LabelComparator implements Comparator<String>
    {
		@Override
		public int compare(String s1, String s2)
		{
			int n1=s1.length(), n2=s2.length();
            for (int i1=0, i2=0; i1<n1 && i2<n2; i1++, i2++) {
                char c1 = s1.charAt(i1);
                char c2 = s2.charAt(i2);
                if (c1 != c2) {
                	if (Character.isLetterOrDigit(c1)) {
                		if (!Character.isLetterOrDigit(c2)) {
                			return -1; // c1 is alphanumeric, c2 is not
                		}
                	} else {
                		if (Character.isLetterOrDigit(c2)) {
                			return 1; // c2 is alphanumeric, c1 is not
                		}
                	}
                    return c1 - c2;
                }
            }
            return n1 - n2;
		}
    }

	/**
     * Answer true if the desired lang tag matches the target lang tag.
     * {@link com.hp.hpl.jena.ontology.impl.OntResourceImpl#langTagMatch(String, String)}
     * is protected so we have to re-implement here...
     */
    static boolean langTagMatch( String desired, String target ) {
        return (desired == null) ||
               (desired.equalsIgnoreCase( target )) ||
               (target.length() > desired.length() && desired.equalsIgnoreCase( target.substring( desired.length() ) ));
    }
	
	static String getLabelBestMatchingLanguage(ExtendedIterator<Label> labels, String lang)
	{
		/* TODO currently, if we're looking for "en_us", "en_gb" won't be
		 * treated any better than "de" and maybe it should be...
		 */
		String match = null;
		while (labels.hasNext()) {
			Label label = labels.next();
			if (langTagMatch(lang, label.getLanguage())) {
				match = label.getText();
				break;
			} else if (match == null){
				match = label.getText();
			}
		}
		return match;
	}
    
	static ExtendedIterator<Label> getLabelIterator(Resource subject, String lang)
	{
		if (subject.getModel() == null) {
			return new NullIterator<Label>();
		} else if (subject instanceof OntResource) {
			return new MapFilterIterator<RDFNode, Label>(literalToLabelFilter, 
					((OntResource)subject).listLabels(lang));
		} else {
			return new MapFilterIterator<Statement, Label>(new StatementToLabelFilter(lang), 
					subject.listProperties(RDFS.label));
		}
	}
	
	static ExtendedIterator<Label> getDescriptionIterator(Resource subject, String lang)
	{
		if (subject.getModel() == null) {
			return new NullIterator<Label>();
		} else if (subject instanceof OntResource) {
			return new MapFilterIterator<RDFNode, Label>(literalToLabelFilter, ((OntResource)subject).listComments(lang));
		} else {
			return new MapFilterIterator<Statement, Label>(new StatementToLabelFilter(lang), 
					subject.listProperties(RDFS.comment).andThen(
					subject.listProperties(DC.description)));
		}
	}

	private static final class StatementToLabelFilter implements MapFilter<Statement, Label>
	{
		private LangTagFilter languageFilter;
		
		public StatementToLabelFilter(String lang)
		{
			if (lang != null)
				languageFilter = new LangTagFilter(lang);
		}
		
		@Override
		public Label accept(Statement statement)
		{
			if (!statement.getPredicate().equals(RDFS.label))
				return null;
			else if (languageFilter != null && !languageFilter.accept(statement))
				return null;
			else
				return new Label(statement.getString(), statement.getLanguage());
		}
	}
	
	/**
	 * {@link com.hp.hpl.jena.ontology.impl.OntResourceImpl$LangTagFilter} is protected 
	 * so we have to re-implement here...
	 */
	private static class LangTagFilter extends Filter<Statement>
    {
        protected String m_lang;
        public LangTagFilter( String lang ) { m_lang = lang; }
        @Override
        public boolean accept( Statement x ) {
            RDFNode o = x.getObject();
            return o.isLiteral() && langTagMatch( m_lang, ((Literal) o).getLanguage() );
        }
    }
	
	private static LiteralToLabelFilter literalToLabelFilter = new LiteralToLabelFilter();
	private static final class LiteralToLabelFilter implements MapFilter<RDFNode, Label>
	{
		@Override
		public Label accept(RDFNode literal)
		{
			if (!literal.isLiteral())
				return null;
			else
				return new Label(literal.asLiteral().getString(), literal.asLiteral().getLanguage());
		}
	}
	
	private static LabelToStringFilter labelToStringFilter = new LabelToStringFilter();
	private static final class LabelToStringFilter implements MapFilter<Label, String>
	{
		@Override
		public String accept(Label label)
		{
			return label.getText();
		}
	}
	
	private static RDFNodeToStringFilter rdfNodeToStringFilter = new RDFNodeToStringFilter();
	private static final class RDFNodeToStringFilter implements MapFilter<RDFNode, String>
	{
		@Override
		public String accept(RDFNode node)
		{
			return LabelUtils.toString(node);
		}
	}
	
	public static class Label
	{
		public String text;
		public String lang;
		
		public Label(String text)
		{ 
			this(text, null);
		}
		
		public Label(String text, String lang)
		{
			this.text = text; this.lang = lang;
		}
		
		public String getText() {
			return text;
		}

		public String getLanguage() {
			return lang;
		}
	}
}
