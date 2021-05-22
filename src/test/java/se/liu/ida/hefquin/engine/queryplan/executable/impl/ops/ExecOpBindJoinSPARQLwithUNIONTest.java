package se.liu.ida.hefquin.engine.queryplan.executable.impl.ops;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.graph.Graph;
import org.apache.jena.sparql.core.Var;
import org.junit.Test;

import se.liu.ida.hefquin.engine.federation.SPARQLEndpoint;
import se.liu.ida.hefquin.engine.query.TriplePattern;
import se.liu.ida.hefquin.engine.queryplan.ExpectedVariables;
import se.liu.ida.hefquin.engine.queryplan.logical.impl.LogicalOpTPAdd;
import se.liu.ida.hefquin.engine.queryplan.physical.impl.PhysicalOpBindJoinWithUNION;

public class ExecOpBindJoinSPARQLwithUNIONTest extends TestsForTPAddAlgorithms<SPARQLEndpoint> {
	
	@Test
	public void tpWithJoinOnObject() {
		_tpWithJoinOnObject();
	}

	@Test
	public void tpWithJoinOnSubjectAndObject() {
		_tpWithJoinOnSubjectAndObject();
	}

	@Test
	public void tpWithoutJoinVariable() {
		_tpWithoutJoinVariable();
	}

	@Test
	public void tpWithEmptyInput() {
		_tpWithEmptyInput();
	}

	@Test
	public void tpWithEmptySolutionMappingAsInput() {
		_tpWithEmptySolutionMappingAsInput();
	}

	@Test
	public void tpWithEmptyResponses() {
		_tpWithEmptyResponses();
	}
	
	@Test
	public void tpWithSpuriousDuplicates() {
		_tpWithSpuriousDuplicates();
	}

	@Override
	protected SPARQLEndpoint createFedMemberForTest( final Graph dataForMember) {
		return new SPARQLEndpointForTest(dataForMember);
	}

	@Override
	protected UnaryExecutableOp createExecOpForTest( final TriplePattern tp, final SPARQLEndpoint fm) {
		final LogicalOpTPAdd tpadd = new LogicalOpTPAdd(fm, tp);
		final PhysicalOpBindJoinWithUNION bindjoin = new PhysicalOpBindJoinWithUNION(tpadd); 
		return bindjoin.createExecOp(bindjoin.getExpectedVariables(new ExpectedVariables() {
			@Override
			public Set<Var> getPossibleVariables() {
				return new HashSet<Var>();
			}
			
			@Override
			public Set<Var> getCertainVariables() {
				return new HashSet<Var>();
			}
		}));
	}

}
