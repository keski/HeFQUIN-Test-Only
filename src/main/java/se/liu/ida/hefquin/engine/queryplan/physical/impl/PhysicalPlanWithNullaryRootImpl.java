package se.liu.ida.hefquin.engine.queryplan.physical.impl;

import java.util.NoSuchElementException;

import se.liu.ida.hefquin.engine.queryplan.ExpectedVariables;
import se.liu.ida.hefquin.engine.queryplan.PhysicalPlan;
import se.liu.ida.hefquin.engine.queryplan.physical.NullaryPhysicalOp;
import se.liu.ida.hefquin.engine.queryplan.physical.PhysicalPlanWithNullaryRoot;
import se.liu.ida.hefquin.engine.queryplan.utils.PhysicalPlanFactory;

public class PhysicalPlanWithNullaryRootImpl implements PhysicalPlanWithNullaryRoot
{
	private final NullaryPhysicalOp rootOp;

	/**
	 * Instead of creating such a plan directly using
	 * this constructor, use {@link PhysicalPlanFactory}.
	 */
	protected PhysicalPlanWithNullaryRootImpl( final NullaryPhysicalOp rootOp ) {
		assert rootOp != null;
		this.rootOp = rootOp;
	}

	@Override
	public NullaryPhysicalOp getRootOperator() {
		return rootOp;
	}

	@Override
	public ExpectedVariables getExpectedVariables() {
		return rootOp.getExpectedVariables();
	}

	@Override
	public int numberOfSubPlans() { return 0; }

	@Override
	public PhysicalPlan getSubPlan( final int i ) throws NoSuchElementException {
		throw new NoSuchElementException( "this physical plan does not have any sub-plan" );
	}

}
