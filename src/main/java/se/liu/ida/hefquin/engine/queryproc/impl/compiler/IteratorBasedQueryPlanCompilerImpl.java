package se.liu.ida.hefquin.engine.queryproc.impl.compiler;

import se.liu.ida.hefquin.engine.queryplan.executable.BinaryExecutableOp;
import se.liu.ida.hefquin.engine.queryplan.executable.ExecutablePlan;
import se.liu.ida.hefquin.engine.queryplan.executable.IntermediateResultBlockBuilder;
import se.liu.ida.hefquin.engine.queryplan.executable.NullaryExecutableOp;
import se.liu.ida.hefquin.engine.queryplan.executable.UnaryExecutableOp;
import se.liu.ida.hefquin.engine.queryplan.executable.impl.GenericIntermediateResultBlockBuilderImpl;
import se.liu.ida.hefquin.engine.queryplan.executable.impl.iterbased.IteratorBasedExecutablePlanImpl;
import se.liu.ida.hefquin.engine.queryplan.executable.impl.iterbased.ResultBlockIterOverResultElementIter;
import se.liu.ida.hefquin.engine.queryplan.executable.impl.iterbased.ResultBlockIterator;
import se.liu.ida.hefquin.engine.queryplan.executable.impl.iterbased.ResultElementIterWithBinaryExecOp;
import se.liu.ida.hefquin.engine.queryplan.executable.impl.iterbased.ResultElementIterWithNullaryExecOp;
import se.liu.ida.hefquin.engine.queryplan.executable.impl.iterbased.ResultElementIterWithUnaryExecOp;
import se.liu.ida.hefquin.engine.queryplan.executable.impl.iterbased.ResultElementIterator;
import se.liu.ida.hefquin.engine.queryplan.physical.PhysicalPlan;
import se.liu.ida.hefquin.engine.queryproc.ExecutionContext;
import se.liu.ida.hefquin.engine.queryproc.QueryCompilationException;
import se.liu.ida.hefquin.engine.queryproc.QueryProcContext;

public class IteratorBasedQueryPlanCompilerImpl extends QueryPlanCompilerBase
{
	public IteratorBasedQueryPlanCompilerImpl( final QueryProcContext ctxt ) {
		super(ctxt);
	}

	@Override
	public ExecutablePlan compile( final PhysicalPlan qep )
			throws QueryCompilationException
	{
		final ExecutionContext execCxt = createExecContext();
		final ResultElementIterator it = compile( qep, execCxt );
		return new IteratorBasedExecutablePlanImpl(it);
	}

	protected ResultElementIterator compile( final PhysicalPlan qep,
	                                         final ExecutionContext execCxt )
	{
		if ( qep.numberOfSubPlans() == 0 )
		{
			final NullaryExecutableOp execOp = (NullaryExecutableOp) qep.getRootOperator().createExecOp(true);
			return new ResultElementIterWithNullaryExecOp(execOp, execCxt);
		}
		else if ( qep.numberOfSubPlans() == 1 )
		{
			final PhysicalPlan subPlan = qep.getSubPlan(0);

			final UnaryExecutableOp execOp = (UnaryExecutableOp) qep.getRootOperator().createExecOp( true, subPlan.getExpectedVariables() );

			final ResultElementIterator elmtIterSubPlan = compile(subPlan, execCxt);
			final ResultBlockIterator blockIterSubPlan = createBlockIterator( elmtIterSubPlan, execOp.preferredInputBlockSize() );
			return new ResultElementIterWithUnaryExecOp(execOp, blockIterSubPlan, execCxt);
		}
		else if ( qep.numberOfSubPlans() == 2 )
		{
			final PhysicalPlan subPlan1 = qep.getSubPlan(0);
			final PhysicalPlan subPlan2 = qep.getSubPlan(1);

			final BinaryExecutableOp execOp = (BinaryExecutableOp) qep.getRootOperator().createExecOp(
					true,
					subPlan1.getExpectedVariables(),
					subPlan2.getExpectedVariables() );

			final ResultElementIterator elmtIterSubPlan1 = compile(subPlan1, execCxt);
			final ResultBlockIterator blockIterSubPlan1 = createBlockIterator( elmtIterSubPlan1, execOp.preferredInputBlockSizeFromChild1() );

			final ResultElementIterator elmtIterSubPlan2 = compile(subPlan2, execCxt);
			final ResultBlockIterator blockIterSubPlan2 = createBlockIterator( elmtIterSubPlan2, execOp.preferredInputBlockSizeFromChild2() );

			return new ResultElementIterWithBinaryExecOp(execOp, blockIterSubPlan1, blockIterSubPlan2, execCxt);
		}
		else
		{
			throw new IllegalArgumentException();
		}
	}

	protected ResultBlockIterator createBlockIterator( final ResultElementIterator elmtIter, final int preferredBlockSize ) {
		final IntermediateResultBlockBuilder blockBuilder = new GenericIntermediateResultBlockBuilderImpl();
		return new ResultBlockIterOverResultElementIter( elmtIter, blockBuilder, preferredBlockSize );
	}

}
