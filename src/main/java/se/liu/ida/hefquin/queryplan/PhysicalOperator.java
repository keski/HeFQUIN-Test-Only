package se.liu.ida.hefquin.queryplan;

import java.util.NoSuchElementException;

public interface PhysicalOperator
{
	/**
	 * Creates and returns the executable operator to be used for
	 * this physical operator. The implementation of this method
	 * has to create a new {@link ExecutableOperator} object each
	 * time it is called.
	 */
	ExecutableOperator createExecOp();

	/**
	 * Returns the number of children that this operator has.
	 */
	int numberOfChildren();

	/**
	 * Returns the i-th child of this operator, where i starts at
	 * index 0 (zero).
	 *
	 * If the operator has fewer children (or no children at all),
	 * then a {@link NoSuchElementException} will be thrown.
	 */
	PhysicalOperator getChild( int i ) throws NoSuchElementException;
}
