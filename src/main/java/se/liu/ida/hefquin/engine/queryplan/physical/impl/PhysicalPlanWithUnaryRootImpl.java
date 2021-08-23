package se.liu.ida.hefquin.engine.queryplan.physical.impl;

import java.util.NoSuchElementException;

import se.liu.ida.hefquin.engine.queryplan.ExpectedVariables;
import se.liu.ida.hefquin.engine.queryplan.PhysicalPlan;
import se.liu.ida.hefquin.engine.queryplan.physical.PhysicalPlanWithUnaryRoot;
import se.liu.ida.hefquin.engine.queryplan.physical.UnaryPhysicalOp;
import se.liu.ida.hefquin.engine.queryplan.utils.PhysicalPlanFactory;

public class PhysicalPlanWithUnaryRootImpl implements PhysicalPlanWithUnaryRoot
{
	private final UnaryPhysicalOp rootOp;
	private final PhysicalPlan subPlan;

	/**
	 * Instead of creating such a plan directly using
	 * this constructor, use {@link PhysicalPlanFactory}.
	 */
	protected PhysicalPlanWithUnaryRootImpl( final UnaryPhysicalOp rootOp,
	                                         final PhysicalPlan subPlan ) {
		assert rootOp != null;
		assert subPlan != null;

		this.rootOp = rootOp;
		this.subPlan = subPlan;
	}

	@Override
	public UnaryPhysicalOp getRootOperator() {
		return rootOp;
	}

	@Override
	public ExpectedVariables getExpectedVariables() {
		return rootOp.getExpectedVariables( getSubPlan().getExpectedVariables() );
	}

	@Override
	public PhysicalPlan getSubPlan() {
		return subPlan;
	}

	@Override
	public int numberOfSubPlans() { return 1; }

	@Override
	public PhysicalPlan getSubPlan( final int i ) throws NoSuchElementException {
		if ( i == 0 )
			return subPlan;
		else
			throw new NoSuchElementException( "this physical plan does not have a " + i + "-th sub-plan" );
	}

}
