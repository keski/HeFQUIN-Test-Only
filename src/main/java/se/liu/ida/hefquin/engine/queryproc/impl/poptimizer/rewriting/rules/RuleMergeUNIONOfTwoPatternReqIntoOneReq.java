package se.liu.ida.hefquin.engine.queryproc.impl.poptimizer.rewriting.rules;

import se.liu.ida.hefquin.engine.federation.FederationMember;
import se.liu.ida.hefquin.engine.federation.access.impl.req.SPARQLRequestImpl;
import se.liu.ida.hefquin.engine.query.SPARQLGraphPattern;
import se.liu.ida.hefquin.engine.queryplan.logical.impl.LogicalOpRequest;
import se.liu.ida.hefquin.engine.queryplan.physical.PhysicalOperator;
import se.liu.ida.hefquin.engine.queryplan.physical.PhysicalOperatorForLogicalOperator;
import se.liu.ida.hefquin.engine.queryplan.physical.PhysicalPlan;
import se.liu.ida.hefquin.engine.queryplan.utils.LogicalOpUtils;
import se.liu.ida.hefquin.engine.queryplan.utils.PhysicalPlanFactory;
import se.liu.ida.hefquin.engine.queryproc.impl.poptimizer.rewriting.RuleApplication;

public class RuleMergeUNIONOfTwoPatternReqIntoOneReq extends AbstractRewritingRuleImpl{

    public RuleMergeUNIONOfTwoPatternReqIntoOneReq( final double priority ) {
        super(priority);
    }

    @Override
    protected boolean canBeAppliedTo( final PhysicalPlan plan ) {
        final PhysicalOperator rootOp = plan.getRootOperator();
        if ( IdentifyLogicalOp.isUnion(rootOp) ) {
            final PhysicalOperator subPlanOp1 = plan.getSubPlan(0).getRootOperator();
            final PhysicalOperator subPlanOp2 = plan.getSubPlan(1).getRootOperator();

            return IdentifyTypeOfRequestUsedForReq.twoGraphPatternReqWithSameSPARQLEndpoint( subPlanOp1, subPlanOp2);
        }
        return false;
    }

    @Override
    protected RuleApplication createRuleApplication( final PhysicalPlan[] pathToTargetPlan ) {
        return new AbstractRuleApplicationImpl(pathToTargetPlan, this) {
            @Override
            protected PhysicalPlan rewritePlan( final PhysicalPlan plan ) {
                final PhysicalPlan subPlan1 = plan.getSubPlan(0);
                final PhysicalPlan subPlan2 = plan.getSubPlan(1);

                final LogicalOpRequest<?, ?> subPlanLop1 = (LogicalOpRequest<?, ?>) ((PhysicalOperatorForLogicalOperator) subPlan1.getRootOperator()).getLogicalOperator();
                final LogicalOpRequest<?, ?> subPlanLop2 = (LogicalOpRequest<?, ?>) ((PhysicalOperatorForLogicalOperator) subPlan2.getRootOperator()).getLogicalOperator();

                final SPARQLGraphPattern newGraphPattern = LogicalOpUtils.createNewGraphPatternWithUnion( subPlanLop1, subPlanLop2 );
                final SPARQLRequestImpl newReq = new SPARQLRequestImpl( newGraphPattern );

                final FederationMember fm = subPlanLop1.getFederationMember();

                return PhysicalPlanFactory.createPlanWithRequest( new LogicalOpRequest<>(fm, newReq) );
            }
        };
    }

}
