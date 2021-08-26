package se.liu.ida.hefquin.engine.queryproc.impl.optimizer.rewriting.rules;

import se.liu.ida.hefquin.engine.queryplan.PhysicalOperator;
import se.liu.ida.hefquin.engine.queryplan.PhysicalPlan;
import se.liu.ida.hefquin.engine.queryplan.logical.impl.LogicalOpTPAdd;
import se.liu.ida.hefquin.engine.queryplan.physical.PhysicalOperatorForLogicalOperator;
import se.liu.ida.hefquin.engine.queryplan.utils.PhysicalPlanFactory;
import se.liu.ida.hefquin.engine.queryproc.impl.optimizer.rewriting.RuleApplication;

public class RuleChangeOrderOfTwoTPAddBJVALUES extends AbstractRewritingRuleImpl{

    public RuleChangeOrderOfTwoTPAddBJVALUES( final double priority ) {
        super(priority);
    }

    @Override
    protected boolean canBeAppliedTo( final PhysicalPlan plan ) {
        final PhysicalOperator rootOp = plan.getRootOperator();
        final PhysicalOperator subRootOp = plan.getSubPlan(0).getRootOperator();
        return IdentifyPhysicalOpUsedForTPAdd.isBindJoinVALUES(rootOp) && IdentifyPhysicalOpUsedForTPAdd.isBindJoinVALUES(subRootOp);
    }

    @Override
    protected RuleApplication createRuleApplication( final PhysicalPlan[] pathToTargetPlan ) {
        return new AbstractRuleApplicationImpl(pathToTargetPlan, this) {
            @Override
            protected PhysicalPlan rewritePlan( final PhysicalPlan plan ) {
                final PhysicalOperatorForLogicalOperator rootOp = (PhysicalOperatorForLogicalOperator) plan.getRootOperator();
                final LogicalOpTPAdd rootLop = (LogicalOpTPAdd) rootOp.getLogicalOperator();

                final PhysicalOperatorForLogicalOperator subRootOp = (PhysicalOperatorForLogicalOperator) plan.getSubPlan(0).getRootOperator();
                final LogicalOpTPAdd subRootLop = (LogicalOpTPAdd) subRootOp.getLogicalOperator();

                final PhysicalPlan newSubPlan = PhysicalPlanFactory.createPlanWithBindJoinVALUES( rootLop , plan.getSubPlan(0).getSubPlan(0) );
                return PhysicalPlanFactory.createPlanWithBindJoinVALUES( subRootLop, newSubPlan );
            }
        };
    }

}
