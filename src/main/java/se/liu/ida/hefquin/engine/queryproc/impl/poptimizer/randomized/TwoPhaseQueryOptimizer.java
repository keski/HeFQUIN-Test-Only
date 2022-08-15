package se.liu.ida.hefquin.engine.queryproc.impl.poptimizer.randomized;

import se.liu.ida.hefquin.engine.queryplan.logical.LogicalPlan;
import se.liu.ida.hefquin.engine.queryplan.logical.LogicalPlanUtils;
import se.liu.ida.hefquin.engine.queryplan.physical.PhysicalPlan;
import se.liu.ida.hefquin.engine.queryproc.PhysicalOptimizationException;
import se.liu.ida.hefquin.engine.queryproc.PhysicalOptimizationStats;
import se.liu.ida.hefquin.engine.queryproc.impl.poptimizer.PhysicalOptimizationStatsImpl;
import se.liu.ida.hefquin.engine.queryproc.impl.poptimizer.QueryOptimizationContext;
import se.liu.ida.hefquin.engine.queryproc.impl.poptimizer.rewriting.RuleInstances;
import se.liu.ida.hefquin.engine.utils.Pair;

public class TwoPhaseQueryOptimizer extends RandomizedQueryOptimizerBase {
	protected final IterativeImprovementBasedQueryOptimizer optimizer1;
	protected final SimulatedAnnealing optimizer2;

	public TwoPhaseQueryOptimizer( final StoppingConditionForIterativeImprovement condition1,
								final EquilibriumConditionForSimulatedAnnealing condition2,
	                           final QueryOptimizationContext context,
	                           final RuleInstances rewritingRules) {
		super(context, rewritingRules);
		optimizer1 = new IterativeImprovementBasedQueryOptimizer(condition1, context, rewritingRules);
		optimizer2 = new SimulatedAnnealing(condition2, context, rewritingRules);
	}

	@Override
	public boolean assumesLogicalMultiwayJoins() {
		return optimizer1.assumesLogicalMultiwayJoins();
	}

	@Override
	public Pair<PhysicalPlan, PhysicalOptimizationStats> optimize( final LogicalPlan initialPlan )
			throws PhysicalOptimizationException {
		final PhysicalPlan halfwayPlan = optimizer1.optimize(initialPlan).object1;
		final PhysicalPlan finalPlan = optimizer2.optimize(halfwayPlan,LogicalPlanUtils.countSubplans(initialPlan),0.1).object1;

		final PhysicalOptimizationStats myStats = new PhysicalOptimizationStatsImpl();

		return new Pair<>( finalPlan, myStats );
	}

}
