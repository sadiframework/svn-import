package ca.wilkinsonlab.sadi.pellet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.datatypes.Datatype;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryExec;
import org.mindswap.pellet.query.QueryPattern;
import org.mindswap.pellet.query.QueryResultBinding;
import org.mindswap.pellet.query.QueryResults;
import org.mindswap.pellet.query.impl.DistVarsQueryExec;
import org.mindswap.pellet.query.impl.QueryPatternImpl;
import org.mindswap.pellet.query.impl.QueryResultBindingImpl;
import org.mindswap.pellet.utils.ATermUtils;

import ca.wilkinsonlab.sadi.vocab.W3C;

import aterm.ATermAppl;

public class SADIQueryExec extends DistVarsQueryExec implements QueryExec {
    
	public static Log log = LogFactory.getLog( SADIQueryExec.class );
    
	public SADIQueryExec() {
		super();
	}
	
	@Override
	public QueryResults exec( Query query ) {
		
		List<QueryPattern> standardPatterns = getStandardPatternList(query.getQueryPatterns());
		query.getQueryPatterns().clear();
		query.getQueryPatterns().addAll(standardPatterns);
		
		/* this is extraordinarily dumb, but the JVM isn't garbage
		 * collecting instances of DynamicKnowledgeBase that are assigned
		 * to the kb field of the static SADIQueryExec instance -- even
		 * after they're overwritten or nulled out and so have no actual 
		 * references remaining.  dumb though it is, this works and the
		 * memory leak goes away...
		 */
		return new SADIQueryExec().execSuper( query );
	}

	private QueryResults execSuper( Query query )
	{
		return super.exec( query );
	}
	
	/**
	 * <p>Return a list of query patterns where rdf:type patterns are defined in the same
	 * manner as patterns with any other predicate.</p>
	 * 
	 * <p>QueryPatternImpl has a special representation for rdf:type patterns, indicated 
	 * by p = null.  This representation disallows variables in the object position, which
	 * prevents queries such as SELECT ?type WHERE { uniprot:P12345 rdf:type ?type }.</p>   
	 */
	protected List<QueryPattern> getStandardPatternList(List<QueryPattern> patterns)
	{
		List<QueryPattern> newList = new ArrayList<QueryPattern>(); 
		for(QueryPattern pattern: patterns) {
				if(pattern.isTypePattern()) {
					ATermAppl s = pattern.getSubject();
					ATermAppl p = ATermUtils.makeTermAppl(W3C.PREDICATE_RDF_TYPE);
					ATermAppl o = pattern.getObject();
					newList.add(new QueryPatternImpl(s, p, o));
				}
				else
					newList.add(pattern);
		}
		return newList;
	}
	
	@Override
	protected void exec( int index, QueryResultBinding binding, QueryResults results )
	{
		if(patterns.size() <= index) {

			// It is possible that dist vars are not same as result vars (some
			// vars may be forced to be distinguished because of of the query
			// structure). Filter those forced vars out of the results
			if( !results.getResultVars().containsAll( binding.getVars() ) ) {
				QueryResultBinding newBinding = new QueryResultBindingImpl();
				List resultVars = results.getResultVars();
				for(int i = 0; i < resultVars.size(); i++) {
					ATermAppl var = (ATermAppl) resultVars.get(i);
					ATermAppl value = binding.getValue(var);                              
					newBinding.setValue(var, value);
				}
				binding = newBinding;
				if( results.contains( binding ))
					return;
			}

			results.add(binding);
			return; 
		} 
		
		QueryPattern pattern0 = (QueryPattern) patterns.get(index);


		if( !pattern0.isGround() ) {		    
			QueryPattern pattern = pattern0.apply( binding );

			log.trace( "resolving pattern " + pattern);

			ATermAppl subj = pattern.getSubject();
			ATermAppl pred = pattern.getPredicate();
			ATermAppl obj = pattern.getObject();

			/*
			// Pellet uses a null predicate to indicate an rdf:type pattern.  -- BV
			if(pattern.isTypePattern())
				pred = ATermUtils.makeTermAppl(RDF.Nodes.type.getURI());
			*/
			
			if( ATermUtils.isVar(subj) && ATermUtils.isVar(obj) ) {
				throw new RuntimeException(
						"Query engine tried to resolve a triple pattern with an unbound subject and an unbound object: " 
						+ pattern.toString() + ". Either the triple patterns have been ordered incorrectly, or the query" 
						+ " is unresolvable.");
			}

			if( ATermUtils.isVar(subj) ) {
				// CASE: ?var <URI> <URI|literal>
				Collection<ATermAppl> sValues = null;
				if( pred.toString().equals(W3C.PREDICATE_RDF_TYPE) && kb.isClass(pattern0.getObject())) {
					sValues = kb.getInstances( obj );
				}
				else {
					sValues = kb.getIndividualsWithProperty( pred, obj );
				}
				
				log.trace("obtained " + sValues.size() + " results for pattern " + pattern);
				for(ATermAppl s : sValues) {
					QueryResultBinding newBinding = (QueryResultBinding) binding.clone();
					newBinding.setValue( subj, s );
					exec( index+1, newBinding, results );                        
				}
			}
			else if ( ATermUtils.isVar(obj)) {
				// CASE: <URI> <URI> ?var
				Collection<ATermAppl> oValues = null;			
				if(query.getLitVars().contains( obj )) {
					Datatype datatype = query.getDatatype( obj );
					oValues = kb.getDataPropertyValues( pred, subj, datatype);
				}
				else
					oValues = kb.getObjectPropertyValues( pred, subj );

				log.trace("obtained " + oValues.size() + " results for pattern " + pattern);
				for(ATermAppl o : oValues) {
					QueryResultBinding newBinding = (QueryResultBinding) binding.clone();
					newBinding.setValue( obj, o );
					exec( index+1, newBinding, results );                        
				}
			}
			else {
				// CASE: <URI> <URI> <URI|literal>
				if(isTripleSatisfied(subj, pred, obj)) {
					log.trace("constant pattern evaluated to true: " + pattern);
					exec( index+1, binding, results );
				}
				else {
					log.trace("constant pattern evaluated to false: " + pattern);
				}
				return;
			}

		}
		else {
			log.trace( "resolving pattern " + pattern0);
			if(isTripleSatisfied(pattern0))
				exec( index+1, binding, results );
			return;
		}
	}

	private boolean isTripleSatisfied(QueryPattern pattern) 
	{
		return isTripleSatisfied(pattern.getSubject(), pattern.getPredicate(), pattern.getObject());
	}

	/* Copied this method from DistVarsQueryExec, because it was private. -- BV
	 */ 
	private boolean isTripleSatisfied(ATermAppl s, ATermAppl p, ATermAppl o)
	{
        if( log.isTraceEnabled() )
            log.trace( "Check triple " + s + " " + (p  == null ? "rdf:type" : p.toString())+ " " + o);
	    
		if(ATermUtils.isVar( s ) || ATermUtils.isVar( o ))
			throw new RuntimeException("No value assigned to variables when checking triple in query!");
						
		boolean tripleSatisfied = (p == null) 
			? kb.isType(s, o)		
		    : kb.hasPropertyValue(s, p, o);
				
		return tripleSatisfied;
	}		
}



