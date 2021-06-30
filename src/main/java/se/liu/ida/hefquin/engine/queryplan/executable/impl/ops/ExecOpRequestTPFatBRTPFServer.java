package se.liu.ida.hefquin.engine.queryplan.executable.impl.ops;

import se.liu.ida.hefquin.engine.federation.BRTPFServer;
import se.liu.ida.hefquin.engine.federation.FederationAccessException;
import se.liu.ida.hefquin.engine.federation.FederationAccessManager;
import se.liu.ida.hefquin.engine.federation.access.TPFRequest;
import se.liu.ida.hefquin.engine.federation.access.TPFResponse;
import se.liu.ida.hefquin.engine.federation.access.TriplePatternRequest;
import se.liu.ida.hefquin.engine.queryplan.executable.ExecOpExecutionException;

/**
 * Implementation of an operator to request a (complete) TPF from a brTPF server.
 * This implementation handles pagination of the TPF; that is, it requests all
 * the pages, one after another.
 */
public class ExecOpRequestTPFatBRTPFServer extends ExecOpGenericTriplePatternRequestWithTPF<BRTPFServer>
{
	public ExecOpRequestTPFatBRTPFServer( final TriplePatternRequest req, final BRTPFServer fm ) {
		super( req, fm );
	}

	@Override
	protected TPFResponse performRequest( final TPFRequest req,
	                                      final FederationAccessManager fedAccessMgr )
			throws ExecOpExecutionException
	{
		try {
			return fedAccessMgr.performRequest(req, fm);
		}
		catch ( final FederationAccessException ex ) {
			throw new ExecOpExecutionException("An exception occurred when performing the request of this request operator.", ex, this);
		}
	}
}
