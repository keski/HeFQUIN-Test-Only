package se.liu.ida.hefquin.engine.queryproc.impl.optimizer.rewriting.rules;

import se.liu.ida.hefquin.engine.queryplan.PhysicalOperator;
import se.liu.ida.hefquin.engine.queryplan.PhysicalPlan;
import se.liu.ida.hefquin.engine.queryplan.logical.NaryLogicalOp;
import se.liu.ida.hefquin.engine.queryplan.physical.impl.PhysicalPlanWithNaryRootImpl;
import se.liu.ida.hefquin.engine.queryplan.utils.PhysicalPlanFactory;
import se.liu.ida.hefquin.engine.queryproc.impl.optimizer.rewriting.RuleApplication;

import java.util.ArrayList;
import java.util.List;

public class RuleDivideMultiwayJoinToJoinOfSubPlans extends AbstractRewritingRuleImpl{

    public RuleDivideMultiwayJoinToJoinOfSubPlans( final double priority ) {
        super(priority);
    }

    @Override
    protected boolean canBeAppliedTo( final PhysicalPlan plan ) {
        final PhysicalOperator op = plan.getRootOperator();
        return IdentifyLogicalOp.matchMultiwayJoin(op);
    }

    @Override
    protected RuleApplication createRuleApplication( final PhysicalPlan[] pathToTargetPlan ) {
        return new AbstractRuleApplicationImpl(pathToTargetPlan, this) {
            @Override
            protected PhysicalPlan rewritePlan( final PhysicalPlan plan ) {
                final List<PhysicalPlan> subPlans = new ArrayList<>();
                for ( int i = 0; i < plan.numberOfSubPlans(); i++) {
                    subPlans.add( plan.getSubPlan(i) );
                }

                final PhysicalPlan subPlan1 = plan.getSubPlan(0);
                subPlans.remove(subPlan1);
                final PhysicalPlan subPlan2 = PhysicalPlanFactory.createPlan((NaryLogicalOp) plan.getRootOperator(), subPlans);
                return PhysicalPlanFactory.createPlanWithJoin( subPlan1, subPlan2);
            }
        };
    }

}
