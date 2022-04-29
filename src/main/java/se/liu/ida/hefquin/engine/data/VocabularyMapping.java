package se.liu.ida.hefquin.engine.data;

import java.util.Set;

import se.liu.ida.hefquin.engine.query.SPARQLGraphPattern;
import se.liu.ida.hefquin.engine.query.TriplePattern;

public interface VocabularyMapping
{
	/**
	 * Applies this vocabulary mapping to the given triple pattern and
	 * returns the resulting combination of triple patterns, which may be
	 * a union of triple patterns (captured as a {@link SPARQLUnionPattern})
	 * or just a single triple pattern (captured as a {@link TriplePattern}).
	 * If this mapping is not relevant for the given triple pattern (i.e.,
	 * applying the mapping to the given triple pattern does not have any
	 * effect), then the result of this function is simply the given triple
	 * pattern itself.
	 */
	SPARQLGraphPattern translateTriplePattern( TriplePattern tp );
	
	/**
	 * Applies this vocabulary mapping to the given solution mapping expressed
	 * in the local vocabulary and returns a resulting set of solution mappings
	 * expressed in the global vocabulary, if boolean fromLocal is true. Else the
	 * vocabulary mapping is used to from the global vocabulary to the local
	 * vocabulary.
	 * If this mapping is not relevant for the given solution mapping (i.e.,
	 * applying the mapping to the given solution mapping does not have any
	 * effect), then the result of this function is a set containing only the
	 * given solution mapping.
	 */
	Set<SolutionMapping> translateSolutionMapping( SolutionMapping sm , boolean fromLocal);

}
