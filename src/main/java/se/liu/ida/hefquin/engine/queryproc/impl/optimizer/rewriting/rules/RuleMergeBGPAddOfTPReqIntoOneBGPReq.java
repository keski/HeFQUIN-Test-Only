package se.liu.ida.hefquin.engine.queryproc.impl.optimizer.rewriting.rules;

import se.liu.ida.hefquin.engine.federation.FederationMember;
import se.liu.ida.hefquin.engine.queryplan.LogicalOperator;
import se.liu.ida.hefquin.engine.queryplan.PhysicalOperator;
import se.liu.ida.hefquin.engine.queryplan.PhysicalPlan;
import se.liu.ida.hefquin.engine.queryplan.logical.impl.LogicalOpBGPAdd;
import se.liu.ida.hefquin.engine.queryplan.physical.PhysicalOperatorForLogicalOperator;

public class RuleMergeBGPAddOfTPReqIntoOneBGPReq extends GenericRuleMergeBGPAddOfReqIntoOneBGPReq{

    public RuleMergeBGPAddOfTPReqIntoOneBGPReq( final double priority ) {
        super(priority);
    }

    @Override
    protected boolean canBeAppliedTo( final PhysicalPlan plan ) {
        final PhysicalOperator rootOp = plan.getRootOperator();
        final LogicalOperator rootLop = ((PhysicalOperatorForLogicalOperator) rootOp).getLogicalOperator();

        if( rootLop instanceof LogicalOpBGPAdd ) {
            final FederationMember fm = ((LogicalOpBGPAdd)rootLop).getFederationMember();

            final PhysicalOperator subRootOp = plan.getSubPlan(0).getRootOperator();
            return IdentifyTypeOfRequestUsedForReq.isTriplePatternRequestWithFm( subRootOp, fm );
        }
        return false;
    }

}
