package se.liu.ida.hefquin.engine.queryplan.executable.impl.ops;

import se.liu.ida.hefquin.engine.data.SolutionMapping;
import se.liu.ida.hefquin.engine.federation.FederationMember;
import se.liu.ida.hefquin.engine.federation.access.TriplePatternRequest;
import se.liu.ida.hefquin.engine.federation.access.impl.req.TriplePatternRequestImpl;
import se.liu.ida.hefquin.engine.query.TriplePattern;
import se.liu.ida.hefquin.engine.query.impl.QueryPatternUtils;

public abstract class ExecOpGenericIndexNestedLoopsJoinWithTPFRequests<MemberType extends FederationMember>
           extends ExecOpGenericIndexNestedLoopsJoinWithRequestOps<TriplePattern,MemberType>
{
	public ExecOpGenericIndexNestedLoopsJoinWithTPFRequests( final TriplePattern query, final MemberType fm ) {
		super( query, fm );
	}

	@Override
	protected NullaryExecutableOp createExecutableRequestOperator( final SolutionMapping inputSolMap ) {
		final TriplePatternRequest req = createRequest(inputSolMap);
		return createRequestOperator(req);
	}

	protected TriplePatternRequest createRequest( final SolutionMapping inputSolMap ) {
		final TriplePattern tp = QueryPatternUtils.applySolMapToTriplePattern(inputSolMap, query);
		return new TriplePatternRequestImpl(tp);
	}

	protected abstract NullaryExecutableOp createRequestOperator( final TriplePatternRequest req );

}
