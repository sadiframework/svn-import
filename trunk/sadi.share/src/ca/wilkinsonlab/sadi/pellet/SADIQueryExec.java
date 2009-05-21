package ca.wilkinsonlab.sadi.pellet;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.datatypes.Datatype;
import org.mindswap.pellet.query.QueryExec;
import org.mindswap.pellet.query.QueryPattern;
import org.mindswap.pellet.query.QueryResultBinding;
import org.mindswap.pellet.query.QueryResults;
import org.mindswap.pellet.query.impl.DistVarsQueryExec;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;

public class SADIQueryExec extends DistVarsQueryExec implements QueryExec {
    
	public static Log log = LogFactory.getLog( SADIQueryExec.class );
    
	public SADIQueryExec() {
	}

	@Override
	protected void exec( int index, QueryResultBinding binding, QueryResults results ) {

		if(patterns.size() <= index) {
		    results.add(binding);
			return; 
		} 
		
		QueryPattern pattern0 = (QueryPattern) patterns.get(index);
		
        if( log.isTraceEnabled() )
            log.trace( "Check pattern " + pattern0 + " " + binding);
		
		if( !pattern0.isGround() ) {		    
		    QueryPattern pattern = pattern0.apply( binding );
		    
		    ATermAppl subj = pattern.getSubject();
		    ATermAppl pred = pattern.getPredicate();
		    ATermAppl obj = pattern.getObject();
		    
		    if( ATermUtils.isVar(subj) && ATermUtils.isVar(obj) ) {
		    	throw new RuntimeException(
		    			"Query engine tried to resolve a triple pattern with an unbound subject and an unbound object: " 
		    			+ pattern.toString() + ". Either the triple patterns have been ordered incorrectly, or the query" 
		    			+ " is unresolvable.");
		    }
		    
		    if( ATermUtils.isVar(subj)) {
		    	// CASE: ?var <URI> <URI|literal>
				Collection<ATermAppl> sValues = null;
				if( pattern.isTypePattern() ) { 
                    sValues = kb.getInstances( obj );
				}
				else {
					sValues = kb.getIndividualsWithProperty( pred, obj );
				}
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
				for(ATermAppl o : oValues) {
					QueryResultBinding newBinding = (QueryResultBinding) binding.clone();
					newBinding.setValue( obj, o );
					exec( index+1, newBinding, results );                        
				}
		    }
		    else {
		    	// CASE: <URI> <URI> <URI|literal>
		    	if(isTripleSatisfied(subj, pred, obj)) {
					exec( index+1, binding, results );
		    	}
		    	return;
		    }
		    
		}
		else {
			if(isTripleSatisfied(pattern0))
				exec( index+1, binding, results );
			return;
		}
	}

	private boolean isTripleSatisfied(QueryPattern pattern) 
	{
		return isTripleSatisfied(pattern.getSubject(), pattern.getPredicate(), pattern.getObject());
	}

	/** 
	 * Copied this method from DistVarsQueryExec, because it was private. -- BV
	 */ 
	private boolean isTripleSatisfied(ATermAppl s, ATermAppl p, ATermAppl o) {
        if( log.isTraceEnabled() )
            log.trace( "Check triple " + s + " " + (p  == null ? "rdf:type" : p.getName())+ " " + o);
	    
		if(ATermUtils.isVar( s ) || ATermUtils.isVar( o ))
			throw new RuntimeException("No value assigned to variables when checking triple in query!");
						
		boolean tripleSatisfied = (p == null) 
			? kb.isType(s, o)		
		    : kb.hasPropertyValue(s, p, o);
				
		return tripleSatisfied;
	}		
}



