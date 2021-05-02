package se.liu.ida.hefquin.engine.data.jenaimpl;

import org.apache.jena.graph.Node;

public class JenaBasedTripleUtils
{
	public static TripleImpl createTriple( final Node s, final Node p, final Node o ) {
		return new TripleImpl( new org.apache.jena.graph.Triple(s,p,o) );
	}

}
