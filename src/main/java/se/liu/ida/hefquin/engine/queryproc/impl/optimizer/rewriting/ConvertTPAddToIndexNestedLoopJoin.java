package se.liu.ida.hefquin.engine.queryproc.impl.optimizer.rewriting;

import se.liu.ida.hefquin.engine.queryplan.logical.UnaryLogicalOp;
import se.liu.ida.hefquin.engine.queryplan.logical.impl.LogicalOpTPAdd;
import se.liu.ida.hefquin.engine.queryplan.physical.UnaryPhysicalOp;
import se.liu.ida.hefquin.engine.queryplan.physical.impl.PhysicalOpIndexNestedLoopsJoin;

public abstract class ConvertTPAddToIndexNestedLoopJoin extends ConvertTPAddToUnaryPhysicalOp{

    @Override
    public UnaryPhysicalOp definePhysicalOperator( final LogicalOpTPAdd lop ){
        return new PhysicalOpIndexNestedLoopsJoin(lop);
    }

}
