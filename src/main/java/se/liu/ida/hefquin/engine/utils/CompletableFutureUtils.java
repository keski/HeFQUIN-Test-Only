package se.liu.ida.hefquin.engine.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CompletableFutureUtils
{
	/**
	 * Helper function that waits for all the given futures to complete
	 * and, then, returns their respective results. The returned array
	 * contains as many result objects as there are futures in the given
	 * list of futures, where the i-th result object in the returned array
	 * is the result of the i-th future in the given list.
	 *
	 * If any of the futures causes an exception, the function cancels all
	 * remaining futures and throws an exception that wraps the causing
	 * exception as its cause. The member {@link GetAllException#i} in
	 * this exception indicates the index of the future that caused the
	 * exception.
	 */
	public static Object[] getAll( final CompletableFuture<?>[] futures )
			throws GetAllException
	{
		final Object[] result = new Object[futures.length];

		GetAllException ex = null;

		for ( int i = 0; i < futures.length; ++i ) {
			if ( ex == null ) {
				try {
					result[i] = futures[i].get();
				}
				catch ( final InterruptedException | ExecutionException e ) {
					ex = new GetAllException(e,i);
				}
			}
			else {
				futures[i].cancel(true);
			}
		}

		if ( ex != null ) {
			throw ex;
		}

		return result;
	}


	public static class GetAllException extends Exception {
		private static final long serialVersionUID = 3973031032404035315L;

		public final int i;

		public GetAllException( final Throwable cause, final int i ) {
			super(cause);
			this.i = i;
		}
	}

}
