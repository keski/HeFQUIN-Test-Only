package se.liu.ida.hefquin.engine.queryproc.impl.optimizer.randomized;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import se.liu.ida.hefquin.engine.queryplan.LogicalPlan;
import se.liu.ida.hefquin.engine.queryplan.PhysicalPlan;
import se.liu.ida.hefquin.engine.queryproc.QueryOptimizationException;
import se.liu.ida.hefquin.engine.queryproc.QueryOptimizer;
import se.liu.ida.hefquin.engine.queryproc.impl.optimizer.CostModel;
import se.liu.ida.hefquin.engine.queryproc.impl.optimizer.QueryOptimizationContext;
import se.liu.ida.hefquin.engine.queryproc.impl.optimizer.rewriting.RewritingRule;
import se.liu.ida.hefquin.engine.queryproc.impl.optimizer.rewriting.RuleApplication;
import se.liu.ida.hefquin.engine.queryproc.impl.optimizer.rewriting.RuleInstances;
import se.liu.ida.hefquin.engine.queryproc.impl.optimizer.utils.CostEstimationUtils;
import se.liu.ida.hefquin.engine.utils.Pair;

public class IterativeImprovementBasedQueryOptimizer implements QueryOptimizer {
	
	protected final QueryOptimizationContext context;
	protected final StoppingConditionForIterativeImprovement condition;
	protected final Random rng  = new Random();
	
	public IterativeImprovementBasedQueryOptimizer (final QueryOptimizationContext ctxt, final StoppingConditionForIterativeImprovement x) {
		assert ctxt != null;
		assert x != null;
		context = ctxt;
		condition = x;
	}
	
	
	@Override
	public PhysicalPlan optimize( final LogicalPlan initialPlan ) throws QueryOptimizationException {
		return optimize( context.getLogicalToPhysicalPlanConverter().convert(initialPlan,false) );
	}

	public PhysicalPlan optimize( final PhysicalPlan initialPlan ) throws QueryOptimizationException {
		// The best plan we have found so far. As we have only found one plan, it is the best one so far.
		PhysicalPlan bestPlan = initialPlan;
		
		CostModel costModel = context.getCostModel(); //Created as a value and stored rather than just using the getCostModel, because the same model is used later too.

		Double[] initialCost = CostEstimationUtils.getEstimates(costModel, initialPlan);
		
		// generation = number of times the outer loop has run. Has to be declared here since it will increment each outer loop.
		int generation = 0;
		
		// bestCost = best possible cost out of everything that the algorithm has found. Has to be declared here so that we have something to compare to in all the iterations of the outer loop without losing track of it between loops.
		Double bestCost = initialCost[0];
		
		while(!condition.readyToStop(generation)) { // Currently only handles generation number as a stopping condition!
			
			// The randomized plan generator is to be used here. As a temporary measure, the initial plan is used.
			PhysicalPlan currentPlan = initialPlan; // This variable will hold the plan which is currently being worked on.
			Double currentCost = initialCost[0]; // The cost of the current plan. For it to be a local minimum, none of its neighbours can have a lower cost
			boolean improvementFound = false;
			
			do {
				final List<PhysicalPlan> neighbours = getNeighbours(currentPlan);
				final Double[] neighbourCosts = CostEstimationUtils.getEstimates(costModel, neighbours);
				final List<Pair<PhysicalPlan,Double>> betterPlans = new ArrayList<>();
				improvementFound = false;
				
				// Using a for-loop in order to have the index for which neighbouring plan to pick.
				for (int x = 0; x < neighbourCosts.length; x++) {
					if(neighbourCosts[x] < currentCost) {
						improvementFound = true;
						final Pair<PhysicalPlan,Double> betterPlan = new Pair<PhysicalPlan, Double>(neighbours.get(x), neighbourCosts[x]);
						betterPlans.add(betterPlan); // I haven't figured out how to do this properly for Java yet, but it will be solved by the PhysicalPlanWithCost anyway.
					}
				}
				
				if(improvementFound) { // if we have found at least one possible improvement, we want to make one of them into our new current plan.
					final Pair<PhysicalPlan,Double> newPlan = betterPlans.get(rng.nextInt(betterPlans.size())); // Get a random object.
					currentPlan = newPlan.object1;
					currentCost = newPlan.object2;
				}
			}
			while(improvementFound);
			
			if(currentCost < bestCost) {
				bestCost = currentCost;
				bestPlan = currentPlan;
			}
			
			generation++;
		}
		
		return bestPlan;
	}
	
	protected List<PhysicalPlan> getNeighbours(final PhysicalPlan initialPlan) {
		List<PhysicalPlan> resultList = new ArrayList<PhysicalPlan>(); // Create an empty list. Just List<PhysicalPlan> didn't work so I had to look this up.
		
		RuleInstances rInst = new RuleInstances();
		RewritingRule[] rules =  (RewritingRule[]) rInst.ruleInstances.toArray(); // The IDE wants me to have a cast here.

		Set<RuleApplication> ruleApplications = Collections.emptySet();
		
		for ( final RewritingRule rr : rules) {
			ruleApplications.addAll(rr.determineAllPossibleApplications(initialPlan));
		}
		
		for ( final RuleApplication ra : ruleApplications ) {
			resultList.add( ra.getResultingPlan() );
		}
		
		return resultList;
	}
}
