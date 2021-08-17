package se.liu.ida.hefquin.engine.queryproc.impl.optimizer.cardinality;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.junit.Test;

import se.liu.ida.hefquin.engine.EngineTestBase;
import se.liu.ida.hefquin.engine.federation.FederationMember;
import se.liu.ida.hefquin.engine.federation.TPFServer;
import se.liu.ida.hefquin.engine.federation.access.CardinalityResponse;
import se.liu.ida.hefquin.engine.federation.access.DataRetrievalRequest;
import se.liu.ida.hefquin.engine.federation.access.FederationAccessException;
import se.liu.ida.hefquin.engine.federation.access.FederationAccessManager;
import se.liu.ida.hefquin.engine.federation.access.TPFRequest;
import se.liu.ida.hefquin.engine.federation.access.TriplePatternRequest;
import se.liu.ida.hefquin.engine.federation.access.impl.req.TriplePatternRequestImpl;
import se.liu.ida.hefquin.engine.query.TriplePattern;
import se.liu.ida.hefquin.engine.query.impl.TriplePatternImpl;
import se.liu.ida.hefquin.engine.queryplan.PhysicalPlan;
import se.liu.ida.hefquin.engine.queryplan.logical.impl.LogicalOpJoin;
import se.liu.ida.hefquin.engine.queryplan.logical.impl.LogicalOpRequest;
import se.liu.ida.hefquin.engine.queryplan.logical.impl.LogicalOpTPAdd;
import se.liu.ida.hefquin.engine.queryplan.physical.BinaryPhysicalOp;
import se.liu.ida.hefquin.engine.queryplan.physical.impl.PhysicalOpBindJoin;
import se.liu.ida.hefquin.engine.queryplan.physical.impl.PhysicalOpRequest;
import se.liu.ida.hefquin.engine.queryplan.physical.impl.PhysicalOpSymmetricHashJoin;
import se.liu.ida.hefquin.engine.queryplan.physical.impl.PhysicalPlanWithBinaryRootImpl;
import se.liu.ida.hefquin.engine.queryplan.physical.impl.PhysicalPlanWithNullaryRootImpl;
import se.liu.ida.hefquin.engine.queryplan.physical.impl.PhysicalPlanWithUnaryRootImpl;
import se.liu.ida.hefquin.engine.queryproc.impl.optimizer.CardinalityEstimation;

public class CardinalityEstimationImplTest extends EngineTestBase
{
	protected static boolean PRINT_TIME = false; protected static final long SLEEP_MILLIES = 0L;
	//protected static boolean PRINT_TIME = true;  protected static final long SLEEP_MILLIES = 100L;

	@Test
	public void oneRequestOp() throws InterruptedException, ExecutionException {
		final FederationAccessManager fedAccessMgr = new MyFederationAccessManager();
		final CardinalityEstimation cardEstimator = new CardinalityEstimationImpl(fedAccessMgr);

		final CompletableFuture<Integer> f = initEstimateForRequestPlan(42, cardEstimator);

		final int result = f.get().intValue();
		assertEquals(42, result);
	}

	@Test
	public void twoRequestOpsInParallel() throws InterruptedException, ExecutionException {
		final FederationAccessManager fedAccessMgr = new MyFederationAccessManager(SLEEP_MILLIES);
		final CardinalityEstimation cardEstimator = new CardinalityEstimationImpl(fedAccessMgr);

		final PhysicalPlan plan1 = createRequestPlan(42);
		final PhysicalPlan plan2 = createRequestPlan(13);

		final long startTime = new Date().getTime();

		final CompletableFuture<Integer> f1 = cardEstimator.initiateCardinalityEstimation(plan1);
		final CompletableFuture<Integer> f2 = cardEstimator.initiateCardinalityEstimation(plan2);

		final int result1 = f1.get().intValue();
		final int result2 = f2.get().intValue();

		final long endTime = new Date().getTime();
		if ( PRINT_TIME ) System.out.println( "twoRequestOpsInParallel \t milliseconds passed: " + (endTime - startTime) );

		assertEquals(42, result1);
		assertEquals(13, result2);
	}

	@Test
	public void twoRequestOpsInSequence() throws InterruptedException, ExecutionException {
		final FederationAccessManager fedAccessMgr = new MyFederationAccessManager(SLEEP_MILLIES);
		final CardinalityEstimation cardEstimator = new CardinalityEstimationImpl(fedAccessMgr);

		final PhysicalPlan plan1 = createRequestPlan(42);
		final PhysicalPlan plan2 = createRequestPlan(13);

		final long startTime = new Date().getTime();

		final CompletableFuture<Integer> f1 = cardEstimator.initiateCardinalityEstimation(plan1);
		final int result1 = f1.get().intValue();

		final CompletableFuture<Integer> f2 = cardEstimator.initiateCardinalityEstimation(plan2);
		final int result2 = f2.get().intValue();

		final long endTime = new Date().getTime();
		if ( PRINT_TIME ) System.out.println( "twoRequestOpsInSequence \t milliseconds passed: " + (endTime - startTime) );

		assertEquals(42, result1);
		assertEquals(13, result2);
	}

	@Test
	public void sameRequestOpTwiceInParallel() throws InterruptedException, ExecutionException {
		final FederationAccessManager fedAccessMgr = new MyFederationAccessManager(SLEEP_MILLIES);
		final CardinalityEstimation cardEstimator = new CardinalityEstimationImpl(fedAccessMgr);

		final PhysicalPlan plan = createRequestPlan(42);

		final long startTime = new Date().getTime();

		final CompletableFuture<Integer> f1 = cardEstimator.initiateCardinalityEstimation(plan);
		final CompletableFuture<Integer> f2 = cardEstimator.initiateCardinalityEstimation(plan);

		final int result1 = f1.get().intValue();
		final int result2 = f2.get().intValue();

		final long endTime = new Date().getTime();
		if ( PRINT_TIME ) System.out.println( "sameRequestOpTwiceInParallel \t milliseconds passed: " + (endTime - startTime) );

		assertEquals(42, result1);
		assertEquals(42, result2);
	}

	@Test
	public void sameRequestOpTwiceInSequence() throws InterruptedException, ExecutionException {
		final FederationAccessManager fedAccessMgr = new MyFederationAccessManager(SLEEP_MILLIES);
		final CardinalityEstimation cardEstimator = new CardinalityEstimationImpl(fedAccessMgr);

		final PhysicalPlan plan = createRequestPlan(42);

		final long startTime = new Date().getTime();

		final CompletableFuture<Integer> f1 = cardEstimator.initiateCardinalityEstimation(plan);
		final int result1 = f1.get().intValue();

		final CompletableFuture<Integer> f2 = cardEstimator.initiateCardinalityEstimation(plan);
		final int result2 = f2.get().intValue();

		final long endTime = new Date().getTime();
		if ( PRINT_TIME ) System.out.println( "sameRequestOpTwiceInSequence \t milliseconds passed: " + (endTime - startTime) );

		assertEquals(42, result1);
		assertEquals(42, result2);
	}

	@Test
	public void joinOfTwoRequestOps() throws InterruptedException, ExecutionException {
		final FederationAccessManager fedAccessMgr = new MyFederationAccessManager(SLEEP_MILLIES);
		final CardinalityEstimation cardEstimator = new CardinalityEstimationImpl(fedAccessMgr);

		final PhysicalPlan plan = createJoinPlan(42, 13);

		final long startTime = new Date().getTime();

		final CompletableFuture<Integer> f = cardEstimator.initiateCardinalityEstimation(plan);
		final int result = f.get().intValue();

		final long endTime = new Date().getTime();
		if ( PRINT_TIME ) System.out.println( "joinOfTwoRequestOps \t milliseconds passed: " + (endTime - startTime) );

		assertEquals(13, result);
	}

	@Test
	public void joinOfSameRequestOp() throws InterruptedException, ExecutionException {
		final FederationAccessManager fedAccessMgr = new MyFederationAccessManager(SLEEP_MILLIES);
		final CardinalityEstimation cardEstimator = new CardinalityEstimationImpl(fedAccessMgr);

		final PhysicalPlan plan = createJoinPlan(42, 42);

		final long startTime = new Date().getTime();

		final CompletableFuture<Integer> f = cardEstimator.initiateCardinalityEstimation(plan);
		final int result = f.get().intValue();

		final long endTime = new Date().getTime();
		if ( PRINT_TIME ) System.out.println( "joinOfSameRequestOp \t milliseconds passed: " + (endTime - startTime) );

		assertEquals(42, result);
	}

	@Test
	public void oneTPAdd() throws InterruptedException, ExecutionException {
		final FederationAccessManager fedAccessMgr = new MyFederationAccessManager(SLEEP_MILLIES);
		final CardinalityEstimation cardEstimator = new CardinalityEstimationImpl(fedAccessMgr);

		final PhysicalPlan plan = createTPAddPlan(42, 13);

		final long startTime = new Date().getTime();

		final CompletableFuture<Integer> f = cardEstimator.initiateCardinalityEstimation(plan);
		final int result = f.get().intValue();

		final long endTime = new Date().getTime();
		if ( PRINT_TIME ) System.out.println( "oneTPAdd \t milliseconds passed: " + (endTime - startTime) );

		assertEquals(13, result);
	}

	@Test
	public void twoTPAdd() throws InterruptedException, ExecutionException {
		final FederationAccessManager fedAccessMgr = new MyFederationAccessManager(SLEEP_MILLIES);
		final CardinalityEstimation cardEstimator = new CardinalityEstimationImpl(fedAccessMgr);

		final PhysicalPlan subplan = createTPAddPlan(42, 13);
		final PhysicalPlan plan = createTPAddPlan(subplan, 22);

		final long startTime = new Date().getTime();

		final CompletableFuture<Integer> f = cardEstimator.initiateCardinalityEstimation(plan);
		final int result = f.get().intValue();

		final long endTime = new Date().getTime();
		if ( PRINT_TIME ) System.out.println( "twoTPAdd \t milliseconds passed: " + (endTime - startTime) );

		assertEquals(13, result);
	}


	protected CompletableFuture<Integer> initEstimateForRequestPlan(
			final int card,
			final CardinalityEstimation cardEstimator )
	{
		final PhysicalPlan plan = createRequestPlan(card);
		return cardEstimator.initiateCardinalityEstimation(plan);
	}

	protected PhysicalPlan createRequestPlan( final int card ) {
		return createRequestPlan( Integer.valueOf(card) );
	}

	protected PhysicalPlan createRequestPlan( final Integer card ) {
		final TriplePattern tp = createTriplePattern(card);
		return createRequestPlan(tp);
	}

	protected TriplePattern createTriplePattern( final Integer card ) {
		return new TriplePatternImpl(
				Var.alloc("s"),
				NodeFactory.createBlankNode(),
				NodeFactory.createLiteralByValue(card, XSDDatatype.XSDint) );
	}

	protected PhysicalPlan createRequestPlan( final TriplePattern tp ) {
		final FederationMember fm = new TPFServerForTest();
		final TriplePatternRequest req = new TriplePatternRequestImpl(tp);

		final LogicalOpRequest<?,?>  lopReq = new LogicalOpRequest<>(fm, req);
		final PhysicalOpRequest<?,?> popReq = new PhysicalOpRequest<>(lopReq);

		return new PhysicalPlanWithNullaryRootImpl(popReq);
	}

	protected PhysicalPlan createJoinPlan( final int card1, final int card2 ) {
		final PhysicalPlan subplan1 = createRequestPlan(card1);
		final PhysicalPlan subplan2 = createRequestPlan(card2);
		return createJoinPlan(subplan1, subplan2);
	}

	protected PhysicalPlan createJoinPlan( final PhysicalPlan subplan1,
	                                       final PhysicalPlan subplan2 ) {
		final BinaryPhysicalOp pop = new PhysicalOpSymmetricHashJoin( new LogicalOpJoin() );
		return new PhysicalPlanWithBinaryRootImpl(pop, subplan1, subplan2);
	}

	protected PhysicalPlan createTPAddPlan( final int card1, final int card2 ) {
		return createTPAddPlan( createRequestPlan(card1), card2 );
	}

	protected PhysicalPlan createTPAddPlan( final PhysicalPlan subplan,
	                                        final int card2 ) {
		final TriplePattern tp = createTriplePattern(card2);
		return createTPAddPlan(subplan, tp);
	}

	protected PhysicalPlan createTPAddPlan( final PhysicalPlan subplan,
	                                        final TriplePattern tp ) {
		final FederationMember fm = new TPFServerForTest();
		final LogicalOpTPAdd tpAdd = new LogicalOpTPAdd(fm, tp);
		return new PhysicalPlanWithUnaryRootImpl( new PhysicalOpBindJoin(tpAdd),
		                                          subplan );
	}


	protected static class MyFederationAccessManager extends FederationAccessManagerForTest
	{
		protected final long sleepMillis;

		public MyFederationAccessManager( final long sleepMillis ) {
			this.sleepMillis = sleepMillis;
		}

		public MyFederationAccessManager() {
			this(0L);
		}

		@Override
		public CompletableFuture<CardinalityResponse> issueCardinalityRequest(
				final TPFRequest req,
				final TPFServer fm ) throws FederationAccessException
		{
			final Object o = req.getQueryPattern().asJenaTriple().getObject().getLiteralValue();
			final int c = ((Integer) o).intValue();

			final CardinalityResponse resp = new CardinalityResponse() {
				@Override public Date getRetrievalEndTime() { return null; }
				@Override public Date getRequestStartTime() { return null; }
				@Override public DataRetrievalRequest getRequest() { return req; }
				@Override public FederationMember getFederationMember() { return fm; }
				@Override public int getCardinality() { return c; }
			};

			if ( sleepMillis > 0L ) {
				try {
					Thread.sleep(sleepMillis);
				} catch ( final InterruptedException e ) {
					throw new FederationAccessException(e, req, fm);
				}
			}

			return CompletableFuture.completedFuture(resp);
		}
	}

}
