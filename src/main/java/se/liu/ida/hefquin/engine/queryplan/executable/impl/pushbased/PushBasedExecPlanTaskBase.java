package se.liu.ida.hefquin.engine.queryplan.executable.impl.pushbased;

import se.liu.ida.hefquin.engine.data.SolutionMapping;
import se.liu.ida.hefquin.engine.queryplan.executable.ExecOpExecutionException;
import se.liu.ida.hefquin.engine.queryplan.executable.IntermediateResultBlock;
import se.liu.ida.hefquin.engine.queryplan.executable.IntermediateResultBlockBuilder;
import se.liu.ida.hefquin.engine.queryplan.executable.IntermediateResultElementSink;
import se.liu.ida.hefquin.engine.queryplan.executable.impl.ExecPlanTask;
import se.liu.ida.hefquin.engine.queryplan.executable.impl.ExecPlanTaskBase;
import se.liu.ida.hefquin.engine.queryplan.executable.impl.ExecPlanTaskInputException;
import se.liu.ida.hefquin.engine.queryplan.executable.impl.ExecPlanTaskInterruptionException;
import se.liu.ida.hefquin.engine.queryplan.executable.impl.GenericIntermediateResultBlockBuilderImpl;
import se.liu.ida.hefquin.engine.queryproc.ExecutionContext;

/**
 * Push-based implementation of {@link ExecPlanTask}.
 * This implementation makes the following assumption:
 * - There is only one thread that consumes the output of this
 *   task (by calling {@link #getNextIntermediateResultBlock()}).
 */
public abstract class PushBasedExecPlanTaskBase extends ExecPlanTaskBase
{
	protected static final int DEFAULT_OUTPUT_BLOCK_SIZE = 50;

	protected final int outputBlockSize;

	private final IntermediateResultBlockBuilder blockBuilder = new GenericIntermediateResultBlockBuilderImpl();


	protected PushBasedExecPlanTaskBase( final ExecutionContext execCxt, final int preferredMinimumBlockSize ) {
		super(execCxt, preferredMinimumBlockSize);

		if ( preferredMinimumBlockSize == 1 )
			outputBlockSize = DEFAULT_OUTPUT_BLOCK_SIZE;
		else
			outputBlockSize = preferredMinimumBlockSize;
	}

	@Override
	public final void run() {
		setStatus(Status.RUNNING);

		final IntermediateResultElementSink sink = new MyIntermediateResultElementSink();

		boolean failed       = false;
		boolean interrupted  = false;
		try {
			produceOutput(sink);
		}
		catch ( final ExecOpExecutionException | ExecPlanTaskInputException e ) {
			setCauseOfFailure(e);
			failed = true;
		}
		catch ( final ExecPlanTaskInterruptionException  e ) {
			setCauseOfFailure(e);
			interrupted = true;
		}

		synchronized (availableResultBlocks) {
			if ( failed ) {
				setStatus(Status.FAILED);
				availableResultBlocks.notifyAll();
			}
			else if ( interrupted ) {
				setStatus(Status.INTERRUPTED);
				availableResultBlocks.notifyAll();
			}
			else {
				// everything went well; let's see whether we still have some
				// output solution mappings for a final output result block
				if ( blockBuilder.sizeOfCurrentBlock() > 0 ) {
					// yes we have; let's create the output result block and
					// notify the potentially waiting consuming thread of it
					availableResultBlocks.add( blockBuilder.finishCurrentBlock() );
					setStatus(Status.COMPLETED_NOT_CONSUMED);
				}
				else {
					// no more output solution mappings; set the completion
					// status depending on whether there still are output
					// result blocks available to be consumed
					if ( availableResultBlocks.isEmpty() )
						setStatus(Status.COMPLETED_AND_CONSUMED);
					else
						setStatus(Status.COMPLETED_NOT_CONSUMED);
				}
				availableResultBlocks.notify();
			}
		}
	}

	protected abstract void produceOutput( final IntermediateResultElementSink sink )
			throws ExecOpExecutionException, ExecPlanTaskInputException, ExecPlanTaskInterruptionException;


	@Override
	public final IntermediateResultBlock getNextIntermediateResultBlock()
			throws ExecPlanTaskInterruptionException, ExecPlanTaskInputException {

		synchronized (availableResultBlocks) {

			IntermediateResultBlock nextBlock = null;
			while ( nextBlock == null && getStatus() != Status.COMPLETED_AND_CONSUMED ) {
				// before trying to get the next block, make sure that
				// we have not already reached some problematic status
				final Status currentStatus = getStatus();
				if ( currentStatus == Status.FAILED ) {
					throw new ExecPlanTaskInputException("Execution of this task has failed with an exception (operator: " + getExecOp().toString() + ").", getCauseOfFailure() );
				}
				else if ( currentStatus == Status.INTERRUPTED ) {
					throw new ExecPlanTaskInputException("Execution of this task has been interrupted (operator: " + getExecOp().toString() + ").");
				}
				else if ( currentStatus == Status.WAITING_TO_BE_STARTED ) {
					// this status is not actually a problem; the code in the
					// remainder of this while loop will simply start waiting
					// for the first result block to become available (which
					// should happen after the execution of this task will
					// get started eventually)
					//System.err.println("Execution of this task has not started yet (operator: " + getExecOp().toString() + ").");
				}

				// try to get the next block
				nextBlock = availableResultBlocks.poll();

				// Did we reach the end of all result blocks to be expected?
				if ( currentStatus == Status.COMPLETED_NOT_CONSUMED && availableResultBlocks.isEmpty() ) {
					setStatus(Status.COMPLETED_AND_CONSUMED); // yes, we did
				}

				if ( nextBlock == null && getStatus() != Status.COMPLETED_AND_CONSUMED ) {
					// if no next block was available and the producing
					// thread is still active, wait for the notification
					// that a new block has become available
					try {
						availableResultBlocks.wait();
					} catch ( final InterruptedException e ) {
						throw new ExecPlanTaskInterruptionException(e);
					}
				}
			}

			return nextBlock;
		}
	}


	protected class MyIntermediateResultElementSink implements IntermediateResultElementSink {
		@Override
		public void send( final SolutionMapping element ) {
			blockBuilder.add(element);
			// If we have collected enough solution mappings, produce the next
			// output result block with these solution mappings and inform the
			// consuming thread in case it is already waiting for the next block
			if ( blockBuilder.sizeOfCurrentBlock() >= outputBlockSize ) {
				final IntermediateResultBlock nextBlock = blockBuilder.finishCurrentBlock();
				synchronized (availableResultBlocks) {
					availableResultBlocks.add(nextBlock);
					availableResultBlocks.notify();
				}
			}
		}
	}

}
