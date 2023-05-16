package se.liu.ida.hefquin.engine.data.mappings.impl;

import java.util.HashSet;
import java.util.Set;

import se.liu.ida.hefquin.engine.data.SolutionMapping;
import se.liu.ida.hefquin.engine.data.VocabularyMapping;
import se.liu.ida.hefquin.engine.data.mappings.EntityMapping;
import se.liu.ida.hefquin.engine.data.mappings.SchemaMapping;
import se.liu.ida.hefquin.engine.query.SPARQLGraphPattern;
import se.liu.ida.hefquin.engine.query.TriplePattern;
import se.liu.ida.hefquin.engine.query.impl.SPARQLUnionPatternImpl;

/**
 * Temporary class. Will be removed once the rest of the internal API has been
 * changed to use {@link SchemaMapping} and {@link EntityMapping}.
 */
public class VocabularyMappingWrappingImpl implements VocabularyMapping
{
	protected final EntityMapping em;
	protected final SchemaMapping sm;

	public VocabularyMappingWrappingImpl( final EntityMapping em, final SchemaMapping sm ) {
		assert em != null;
		assert sm != null;

		this.em = em;
		this.sm = sm;
	}

	@Override
	public SPARQLGraphPattern translateTriplePattern( final TriplePattern tp ) {
		final Set<TriplePattern> emResult = em.applyToTriplePattern(tp);

		if ( emResult.size() == 1 ) {
			return sm.applyToTriplePattern( emResult.iterator().next() );
		}

		final SPARQLUnionPatternImpl u = new SPARQLUnionPatternImpl();
		for ( final TriplePattern tp2 : emResult ) {
			u.addSubPattern( sm.applyToTriplePattern(tp2) );
		}
		return u;
	}

	@Override
	public Set<SolutionMapping> translateSolutionMapping( final SolutionMapping solmap ) {
		final Set<SolutionMapping> emResult = em.applyToSolutionMapping(solmap);

		if ( emResult.size() == 1 ) {
			return sm.applyInverseToSolutionMapping( emResult.iterator().next() );
		}

		final Set<SolutionMapping> result = new HashSet<>();
		for ( final SolutionMapping solmap2 : emResult ) {
			result.addAll( sm.applyInverseToSolutionMapping(solmap2) );
		}
		return result;
	}

	@Override
	public Set<SolutionMapping> translateSolutionMappingFromGlobal( final SolutionMapping solmap ) {
		final Set<SolutionMapping> emResult = em.applyToSolutionMapping(solmap);

		if ( emResult.size() == 1 ) {
			return sm.applyToSolutionMapping( emResult.iterator().next() );
		}

		final Set<SolutionMapping> result = new HashSet<>();
		for ( final SolutionMapping solmap2 : emResult ) {
			result.addAll( sm.applyToSolutionMapping(solmap2) );
		}
		return result;
	}

	@Override
	public boolean isEquivalenceOnly() {
		throw new UnsupportedOperationException();
	}

}
