package se.liu.ida.hefquin.engine.queryplan.executable.impl.ops;

import java.util.Iterator;

import se.liu.ida.hefquin.engine.data.SolutionMapping;
import se.liu.ida.hefquin.engine.data.utils.JoiningIterableForSolMaps;
import se.liu.ida.hefquin.engine.data.utils.UnionIterableForSolMaps;
import se.liu.ida.hefquin.engine.federation.TPFServer;
import se.liu.ida.hefquin.engine.federation.access.TriplePatternRequest;
import se.liu.ida.hefquin.engine.federation.access.impl.req.TriplePatternRequestImpl;
import se.liu.ida.hefquin.engine.query.BGP;
import se.liu.ida.hefquin.engine.query.SPARQLGraphPattern;
import se.liu.ida.hefquin.engine.query.SPARQLGroupPattern;
import se.liu.ida.hefquin.engine.query.SPARQLUnionPattern;
import se.liu.ida.hefquin.engine.query.TriplePattern;
import se.liu.ida.hefquin.engine.queryplan.executable.ExecOpExecutionException;
import se.liu.ida.hefquin.engine.queryplan.executable.IntermediateResultElementSink;
import se.liu.ida.hefquin.engine.queryplan.executable.impl.MaterializingIntermediateResultElementSinkWithTranslation;
import se.liu.ida.hefquin.engine.queryproc.ExecutionContext;

/**
 * Implementation of an operator to request a (complete) TPF from a TPF server.
 * This implementation handles pagination of the TPF; that is, it requests all
 * the pages, one after another.
 */
public class ExecOpRequestTPFatTPFServerWithTranslation extends ExecOpGenericRequest<TriplePatternRequest, TPFServer>
{
	public ExecOpRequestTPFatTPFServerWithTranslation( final TriplePatternRequest req, final TPFServer fm ) {
		super( req, fm );

		assert fm.getVocabularyMapping() != null;
	}

	
	@Override
	protected void _execute( final IntermediateResultElementSink sink, final ExecutionContext execCxt)
			throws ExecOpExecutionException {
		
		final SPARQLGraphPattern reqTranslation = fm.getVocabularyMapping().translateTriplePattern(req.getQueryPattern());
		for ( final SolutionMapping sm : handlePattern(reqTranslation, execCxt) ) {
			sink.send(sm);
		}	
	}
	
	protected Iterable<SolutionMapping> handlePattern(final SPARQLGraphPattern p, final ExecutionContext execCxt) throws ExecOpExecutionException{
		if (p instanceof TriplePattern) {
			return handleTriplePattern((TriplePattern) p, execCxt);
		} else if (p instanceof SPARQLUnionPattern) {
			return handleUnionPattern(((SPARQLUnionPattern) p), execCxt);
		} else if (p instanceof SPARQLGroupPattern) {
			return handleGroupPattern(((SPARQLGroupPattern) p), execCxt);
		} else if (p instanceof BGP) {
			return handleBGP(((BGP) p), execCxt);
		} else {
			throw new ExecOpExecutionException(p.toString(), this);
		}
	}
	
	protected Iterable<SolutionMapping> handleTriplePattern(final TriplePattern tp, final ExecutionContext execCxt) throws ExecOpExecutionException{
		
		final TriplePatternRequest newReq = new TriplePatternRequestImpl(tp);
		final ExecOpRequestTPFatTPFServer op = new ExecOpRequestTPFatTPFServer(newReq, fm);
		final MaterializingIntermediateResultElementSinkWithTranslation sink = new MaterializingIntermediateResultElementSinkWithTranslation(fm.getVocabularyMapping());
		
		op.execute(sink, execCxt);
	
		return sink.getMaterializedIntermediateResult();
	}
	
	protected Iterable<SolutionMapping> handleUnionPattern(final SPARQLUnionPattern up, final ExecutionContext execCxt) throws ExecOpExecutionException {
		final Iterator<SPARQLGraphPattern> i = up.getSubPatterns().iterator();
		Iterable<SolutionMapping> unionTranslation = handlePattern(i.next(), execCxt);
		while(i.hasNext()){
			unionTranslation = new UnionIterableForSolMaps(unionTranslation, handlePattern(i.next(), execCxt));
		}
		return unionTranslation;
	}
	
	protected Iterable<SolutionMapping> handleGroupPattern(final SPARQLGroupPattern gp, final ExecutionContext execCxt) throws ExecOpExecutionException {
		final Iterator<SPARQLGraphPattern> i = gp.getSubPatterns().iterator();
		Iterable<SolutionMapping> groupTranslation = handlePattern(i.next(), execCxt);
		while(i.hasNext()){
			groupTranslation = new JoiningIterableForSolMaps(groupTranslation, handlePattern(i.next(), execCxt));
		}
		return groupTranslation;
	}
	
	protected Iterable<SolutionMapping> handleBGP(final BGP bgp, final ExecutionContext execCxt) throws ExecOpExecutionException {
		Iterable<SolutionMapping> bgpTranslation = null;
		for(final TriplePattern i : bgp.getTriplePatterns()) {
			if (bgpTranslation == null) {
				bgpTranslation = handleTriplePattern(i, execCxt);
			} else {
				bgpTranslation = new JoiningIterableForSolMaps(bgpTranslation, handleTriplePattern(i, execCxt));
			}
		}
		return bgpTranslation;
	}

}
