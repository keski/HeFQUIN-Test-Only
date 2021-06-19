package se.liu.ida.hefquin.engine.data.utils;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.junit.Test;
import se.liu.ida.hefquin.engine.data.SolutionMapping;
import se.liu.ida.hefquin.engine.data.impl.SolutionMappingUtils;
import se.liu.ida.hefquin.engine.datastructures.impl.SolutionMappingsHashTableBasedOnOneVar;
import se.liu.ida.hefquin.engine.datastructures.impl.SolutionMappingsIndexBase;

import java.util.Iterator;

import static org.junit.Assert.*;

public class SolutionMappingsIterableWithSolMapFilterTest {
    @Test
    public void hashTableWithOneInputVariable() {
        final Var var1 = Var.alloc("v1");
        final Var var2 = Var.alloc("v2");
        final Var var3 = Var.alloc("v3");

        final Node x1 = NodeFactory.createURI("http://example.org/x1");
        final Node y1 = NodeFactory.createURI("http://example.org/y1");
        final Node y2 = NodeFactory.createURI("http://example.org/y2");
        final Node z1 = NodeFactory.createURI("http://example.org/z1");
        final Node z2 = NodeFactory.createURI("http://example.org/z2");
        final Node z3 = NodeFactory.createURI("http://example.org/z3");

        // create SolutionMappingsIndexBase
        final SolutionMappingsIndexBase solMHashTable = new SolutionMappingsHashTableBasedOnOneVar(var2);

        final SolutionMapping sm1 = SolutionMappingUtils.createSolutionMapping(var2, y1, var3, z1);
        solMHashTable.add(sm1);
        solMHashTable.add( SolutionMappingUtils.createSolutionMapping(
                var2, y1,
                var3, z2) );
        solMHashTable.add( SolutionMappingUtils.createSolutionMapping(
                var2, y2,
                var3, z3) );
        final Iterable<SolutionMapping> allSolMap = solMHashTable.getAllSolutionMappings();

        // iterate over the subset of solution mappings that are compatible with sm1
        final Iterable<SolutionMapping> solMapAfterFilter1 = new SolutionMappingsIterableWithSolMapFilter( allSolMap, sm1 );
        final Iterator<SolutionMapping> it1 = solMapAfterFilter1.iterator();
        assertTrue( it1.hasNext() );
        final Binding bIt1 = it1.next().asJenaBinding();
        assertEquals( 2, bIt1.size() );

        assertFalse( it1.hasNext() );

        // no solution mappings that are compatible with sm00
        final SolutionMapping sm00 = SolutionMappingUtils.createSolutionMapping(var2, y2, var3, z1);
        final Iterable<SolutionMapping> solMapAfterFilter2 = new SolutionMappingsIterableWithSolMapFilter( allSolMap, sm00 );
        final Iterator<SolutionMapping> it2 = solMapAfterFilter2.iterator();

        assertFalse( it2.hasNext() );

        // two solution mappings that are compatible with sm01
        final SolutionMapping sm01 = SolutionMappingUtils.createSolutionMapping(var2, y1, var1, x1);
        final Iterable<SolutionMapping> solMapAfterFilter3 = new SolutionMappingsIterableWithSolMapFilter( allSolMap, sm01 );
        final Iterator<SolutionMapping> it3 = solMapAfterFilter3.iterator();

        assertTrue( it3.hasNext() );
        final Binding bIt31 = it3.next().asJenaBinding();
        assertEquals( 2, bIt31.size() );

        assertTrue( it3.hasNext() );
        final Binding bIt32 = it3.next().asJenaBinding();
        assertEquals( 2, bIt32.size() );

        assertFalse( it3.hasNext() );

        // two solution mappings that are compatible with sm02
        final SolutionMapping sm02 = SolutionMappingUtils.createSolutionMapping(var1, x1);
        final Iterable<SolutionMapping> solMapAfterFilter4 = new SolutionMappingsIterableWithSolMapFilter( allSolMap, sm02 );
        final Iterator<SolutionMapping> it4 = solMapAfterFilter4.iterator();

        assertTrue( it4.hasNext() );
        final Binding bIt41 = it4.next().asJenaBinding();
        assertEquals( 2, bIt41.size() );

        assertTrue( it4.hasNext() );
        final Binding bIt42 = it4.next().asJenaBinding();
        assertEquals( 2, bIt42.size() );

        assertTrue( it4.hasNext() );
        final Binding bIt43 = it4.next().asJenaBinding();
        assertEquals( 2, bIt43.size() );

        assertFalse( it4.hasNext() );
    }
}
