package se.liu.ida.hefquin.engine.queryproc.impl.optimizer.evolutionaryAlgorithm.rewritingRule.impl;

import se.liu.ida.hefquin.engine.queryplan.PhysicalPlan;
import se.liu.ida.hefquin.engine.queryproc.impl.optimizer.evolutionaryAlgorithm.rewritingRule.ConvertTPAddToSymmetricHashJoin;
import se.liu.ida.hefquin.engine.queryproc.impl.optimizer.evolutionaryAlgorithm.rewritingRule.IdentifyPhysicalOperatorOfTPAdd;

public class RuleConvertTPAddBJToSymmetricHashJoin extends ConvertTPAddToSymmetricHashJoin {

    @Override
    public Boolean canBeAppliedTo( final PhysicalPlan pp ) {
        return new IdentifyPhysicalOperatorOfTPAdd(pp).matchTPAddBindJoin();
    }

}
