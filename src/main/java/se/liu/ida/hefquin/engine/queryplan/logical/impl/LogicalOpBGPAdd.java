package se.liu.ida.hefquin.engine.queryplan.logical.impl;

import java.util.Set;

import org.apache.jena.sparql.core.Var;

import se.liu.ida.hefquin.engine.federation.FederationMember;
import se.liu.ida.hefquin.engine.query.BGP;
import se.liu.ida.hefquin.engine.query.impl.QueryPatternUtils;
import se.liu.ida.hefquin.engine.queryplan.ExpectedVariables;
import se.liu.ida.hefquin.engine.queryplan.logical.LogicalPlanVisitor;
import se.liu.ida.hefquin.engine.queryplan.logical.UnaryLogicalOp;
import se.liu.ida.hefquin.engine.queryplan.utils.ExpectedVariablesUtils;

public class LogicalOpBGPAdd extends LogicalOperatorBase implements UnaryLogicalOp
{
	protected final FederationMember fm;
	protected final BGP bgp;

	public LogicalOpBGPAdd( final FederationMember fm, final BGP bgp ) {
		assert fm != null;
		assert bgp != null;
		assert fm.getInterface().supportsBGPRequests();

		this.fm = fm;
		this.bgp = bgp;
	}

	public FederationMember getFederationMember() {
		return fm;
	} 

	public BGP getBGP() {
		return bgp;
	}

	@Override
	public ExpectedVariables getExpectedVariables( final ExpectedVariables... inputVars ) {
		assert inputVars.length == 1;

		final ExpectedVariables expVarsPattern = QueryPatternUtils.getExpectedVariablesInPattern(bgp);
		final ExpectedVariables expVarsInput = inputVars[0];

		final Set<Var> certainVars = ExpectedVariablesUtils.unionOfCertainVariables(expVarsPattern, expVarsInput);
		final Set<Var> possibleVars = ExpectedVariablesUtils.unionOfPossibleVariables(expVarsPattern, expVarsInput);
		possibleVars.removeAll(certainVars);

		return new ExpectedVariables() {
			@Override public Set<Var> getCertainVariables() { return certainVars; }
			@Override public Set<Var> getPossibleVariables() { return possibleVars; }
		};
	}

	@Override
	public boolean equals( final Object o ) {
		if ( o == this )
			return true;

		if ( ! (o instanceof LogicalOpBGPAdd) )
			return false;

		final LogicalOpBGPAdd oo = (LogicalOpBGPAdd) o;
		return oo.fm.equals(fm) && oo.bgp.equals(bgp); 
	}

	@Override
	public int hashCode(){
		return fm.hashCode() ^ bgp.hashCode();
	}

	@Override
	public void visit( final LogicalPlanVisitor visitor ) {
		visitor.visit(this);
	}

	@Override
	public String toString(){
		final int codeOfBGP = bgp.toString().hashCode();
		final int codeOfFm = fm.getInterface().toString().hashCode();

		return "> bgpAdd" +
				"[" + codeOfBGP + ", "+ codeOfFm + "]"+
				" ( "
				+ bgp.toString()
				+ ", "
				+ fm.getInterface().toString()
				+ " )";
	}
	
	public String toPrintString(String identStr) {
		final int codeOfBGP = bgp.toString().hashCode();
		final int codeOfFm = fm.getInterface().toString().hashCode();
		
		return identStr + "> bgpAdd" +
				"[" + codeOfBGP + ", "+ codeOfFm + "]"+
				" ( "
				+ bgp.toString()
				+ ", "
				+ fm.getInterface().toString()
				+ " )";
	}

}
