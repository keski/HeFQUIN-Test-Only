package se.liu.ida.hefquin.engine.queryplan.executable.impl.ops;

import java.util.Iterator;

import se.liu.ida.hefquin.engine.data.SolutionMapping;
import se.liu.ida.hefquin.engine.queryplan.executable.ExecOpExecutionException;
import se.liu.ida.hefquin.engine.queryplan.executable.IntermediateResultBlock;
import se.liu.ida.hefquin.engine.queryplan.executable.IntermediateResultElementSink;
import se.liu.ida.hefquin.engine.queryproc.ExecutionContext;

public abstract class UnaryExecutableOpBaseWithIterator extends UnaryExecutableOpBase
{
	public UnaryExecutableOpBaseWithIterator( final boolean collectExceptions ) {
		super(collectExceptions);
	}

	@Override
	public int preferredInputBlockSize() {
		return 1;
	}

	@Override
	protected void _process( final IntermediateResultBlock input,
	                         final IntermediateResultElementSink sink,
	                         final ExecutionContext execCxt )
			 throws ExecOpExecutionException {
		final Iterator<SolutionMapping> it = createInputToOutputIterator( input.getSolutionMappings() );
		_process(it, sink, execCxt);
	}

	@Override
	protected void _concludeExecution( final IntermediateResultElementSink sink,
	                                   final ExecutionContext execCxt ) {
		// nothing to be done here
	}

	protected abstract Iterator<SolutionMapping> createInputToOutputIterator( Iterable<SolutionMapping> input ) throws ExecOpExecutionException;

	protected void _process( final Iterator<SolutionMapping> output,
	                         final IntermediateResultElementSink sink,
	                         final ExecutionContext execCxt ) {
		while ( output.hasNext() ) {
			sink.send( output.next() );
		}
	}

}
