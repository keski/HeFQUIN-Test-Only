package se.liu.ida.hefquin.engine.federation.access.impl.req;

import se.liu.ida.hefquin.base.query.TriplePattern;
import se.liu.ida.hefquin.base.query.impl.QueryPatternUtils;
import se.liu.ida.hefquin.base.queryplan.ExpectedVariables;
import se.liu.ida.hefquin.engine.federation.access.TriplePatternRequest;

import java.util.Objects;

public class TriplePatternRequestImpl implements TriplePatternRequest
{
	protected final TriplePattern tp;

	public TriplePatternRequestImpl( final TriplePattern tp ) {
		assert tp != null;
		this.tp = tp;
	}

	@Override
	public boolean equals( final Object o ) {
		return o instanceof TriplePatternRequest
				&& ((TriplePatternRequest) o).getQueryPattern().equals(tp);
	}

	@Override
	public int hashCode(){
		return tp.hashCode() ^ Objects.hash(super.getClass().getName() );
	}

	@Override
	public TriplePattern getQueryPattern() {
		return tp;
	}

	@Override
	public ExpectedVariables getExpectedVariables() {
		return QueryPatternUtils.getExpectedVariablesInPattern(tp);
	}

	@Override
	public String toString(){
		return tp.toString();
	}

}
