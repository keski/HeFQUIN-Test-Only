package se.liu.ida.hefquin.engine.data.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Test;

import se.liu.ida.hefquin.engine.data.VocabularyMapping;
import se.liu.ida.hefquin.engine.query.BGP;
import se.liu.ida.hefquin.engine.query.SPARQLGraphPattern;
import se.liu.ida.hefquin.engine.query.SPARQLGroupPattern;
import se.liu.ida.hefquin.engine.query.TriplePattern;
import se.liu.ida.hefquin.engine.query.impl.BGPImpl;
import se.liu.ida.hefquin.engine.query.impl.SPARQLUnionPatternImpl;
import se.liu.ida.hefquin.engine.query.SPARQLUnionPattern;
import se.liu.ida.hefquin.engine.query.impl.TriplePatternImpl;
import se.liu.ida.hefquin.engine.utils.Pair;

public class VocabularyMappingTest
{
	@Test
	public void VocabularyMappingConstructorTest() {
		final Pair<Set<Triple>,Set<Triple>> testData = CreateTestTriples();

		final VocabularyMappingImpl vm = new VocabularyMappingImpl(testData.object1);

		final Set<Triple> queryResults = new HashSet<>();
		final Iterator<Triple> i = vm.getVocabularyMappingAsGraph().find();
		while(i.hasNext()) {
			queryResults.add(i.next());
		}
		
		assertEquals(queryResults, testData.object1);
	}
	
	@Test
	public void TranslateTriplePatternTest() {
		
		final Pair<Set<Triple>,Set<Triple>> testData = CreateTestTriples();

		final VocabularyMapping vm = new VocabularyMappingImpl(testData.object1);
		
		Node s = NodeFactory.createURI("s1");
		Node p = RDF.type.asNode();
		Node o = NodeFactory.createURI("o1");
		TriplePattern testTp = new TriplePatternImpl(s, p, o);
		SPARQLGraphPattern translation = vm.translateTriplePattern(testTp);
		
		Set<Triple> translationTriples = new HashSet<>();
		assertTrue(translation instanceof SPARQLUnionPatternImpl);
		for (final SPARQLGraphPattern i : ((SPARQLUnionPatternImpl) translation).getSubPatterns()) {
			assertTrue(i instanceof SPARQLGroupPattern);
			for (final SPARQLGraphPattern j : ((SPARQLGroupPattern) i).getSubPatterns()) {
				assertTrue(j instanceof SPARQLUnionPatternImpl);
				for (final SPARQLGraphPattern k : ((SPARQLUnionPatternImpl) j).getSubPatterns()) {
					assertTrue(k instanceof TriplePattern);
					translationTriples.add(((TriplePattern) k).asJenaTriple());
				}
			}
		}
		
		assertEquals(testData.object2, translationTriples);
		
		//Test with variable
		o = NodeFactory.createVariable("o");
		testTp = new TriplePatternImpl(s, p, o);
		translation = vm.translateTriplePattern(testTp);
		
		translationTriples = new HashSet<>();
		assertTrue(translation instanceof SPARQLUnionPattern);
		for (final SPARQLGraphPattern i : ((SPARQLUnionPattern) translation).getSubPatterns()) {
			assertTrue(i instanceof SPARQLUnionPattern);
			for (final SPARQLGraphPattern j : ((SPARQLUnionPattern) i).getSubPatterns()) {
				assertTrue(j instanceof TriplePattern);
				translationTriples.add(((TriplePattern) j).asJenaTriple());
			}
		}
		
		final Set<Triple> expectedResults = new HashSet<>();
		s = NodeFactory.createURI("s2");
		p = NodeFactory.createURI("Subtype");
		expectedResults.add(new Triple(s, p, o));
		
		s = NodeFactory.createURI("s3");
		expectedResults.add(new Triple(s, p, o));
		
		s = o;
		p = NodeFactory.createURI("Not type");
		o = NodeFactory.createURI("s2");
		expectedResults.add(new Triple(s, p, o));
		
		o = NodeFactory.createURI("s3");
		expectedResults.add(new Triple(s, p, o));		
		
		assertEquals(expectedResults, translationTriples);
		
	}
	
	@Test
	public void TranslateSubjectTest() {
		final Set<Triple> testTriples = new HashSet<>();
		//Equality
		Node s = NodeFactory.createURI("s1");
		Node p = OWL.sameAs.asNode();
		Node o  = NodeFactory.createURI("s2");
		testTriples.add(new Triple(s, p, o));
				
		//Multiple mappings for same subject
		o = NodeFactory.createURI("s3");
		testTriples.add(new Triple(s, p, o));
		
		final VocabularyMapping vm = new VocabularyMappingImpl(testTriples);
		
		p = NodeFactory.createURI("p");
		o = NodeFactory.createURI("o");
		TriplePattern testTp = new TriplePatternImpl(s, p, o);
		SPARQLGraphPattern translation = vm.translateTriplePattern(testTp);
		List<SPARQLGraphPattern> translationSubPatterns = new ArrayList<>();
		assertTrue(translation instanceof SPARQLUnionPattern);
		for(SPARQLGraphPattern i : ((SPARQLUnionPattern) translation).getSubPatterns()) {
			translationSubPatterns.add(i);
		}
		
		List<SPARQLGraphPattern> expectedResults = new ArrayList<>();
		s = NodeFactory.createURI("s2");
		expectedResults.add(new TriplePatternImpl(s, p, o));
		s = NodeFactory.createURI("s3");
		expectedResults.add(new TriplePatternImpl(s, p, o));
		
		assertTrue(translationSubPatterns.containsAll(expectedResults));
		assertTrue(translationSubPatterns.size() == expectedResults.size());
		
		//Testing with variable
		s = NodeFactory.createVariable("s");
		p = NodeFactory.createURI("p");
		o = NodeFactory.createURI("o");
		testTp = new TriplePatternImpl(s, p, o);
		translation = vm.translateTriplePattern(testTp);
		assertTrue(testTp.equals(translation));
		
	}
	
	@Test
	public void TranslatePredicateTest() {
		final Set<Triple> testTriples = new HashSet<>();
		//Equality
		Node s = NodeFactory.createURI("p1");
		Node p = OWL.equivalentProperty.asNode();
		Node o = NodeFactory.createURI("p2");
		testTriples.add(new Triple(s, p, o));
				
		//Predicate inverse
		p = OWL.inverseOf.asNode();
		o = NodeFactory.createURI("Not p1");
		testTriples.add(new Triple(s, p, o));
			
		//Predicate subProperty
		o = NodeFactory.createURI("Subtype");
		p = RDFS.subPropertyOf.asNode();
		testTriples.add(new Triple(o, p, s));
		
		//Predicate Intersection
		p = OWL.intersectionOf.asNode();
		Node blank = NodeFactory.createBlankNode();
		testTriples.add(new Triple(s, p, blank));	
		
		p = RDF.first.asNode();
		o = NodeFactory.createURI("p3");
		testTriples.add(new Triple(blank, p, o));	
		
		p = RDF.rest.asNode();
		o = NodeFactory.createBlankNode();
		testTriples.add(new Triple(blank, p, o));
		
		blank = o;
		p = RDF.first.asNode();
		o = NodeFactory.createURI("p4");
		testTriples.add(new Triple(blank, p, o));	
		
		p = RDF.rest.asNode();
		o = RDF.nil.asNode();
		testTriples.add(new Triple(blank, p, o));
		
		//Predicate Union
		p = OWL.unionOf.asNode();
		blank = NodeFactory.createBlankNode();
		testTriples.add(new Triple(s, p, blank));	
		
		p = RDF.first.asNode();
		o = NodeFactory.createURI("p5");
		testTriples.add(new Triple(blank, p, o));	
		
		p = RDF.rest.asNode();
		o = NodeFactory.createBlankNode();
		testTriples.add(new Triple(blank, p, o));
		
		blank = o;
		p = RDF.first.asNode();
		o = NodeFactory.createURI("p6");
		testTriples.add(new Triple(blank, p, o));	
		
		p = RDF.rest.asNode();
		o = RDF.nil.asNode();
		testTriples.add(new Triple(blank, p, o));
		

		final VocabularyMapping vm = new VocabularyMappingImpl(testTriples);
		
		s = NodeFactory.createURI("s");
		p = NodeFactory.createURI("p1");
		o = NodeFactory.createURI("o");
		TriplePattern testTp = new TriplePatternImpl(s, p, o);
		SPARQLGraphPattern translation = vm.translateTriplePattern(testTp); 
		List<SPARQLGraphPattern> translationSubPatterns = new ArrayList<>();
		assertTrue(translation instanceof SPARQLUnionPattern);
		for(SPARQLGraphPattern i : ((SPARQLUnionPattern) translation).getSubPatterns()) {
			if(i instanceof SPARQLUnionPattern) {
				assertTrue(((SPARQLUnionPattern) i).getNumberOfSubPatterns() == 2);
				for(SPARQLGraphPattern j : ((SPARQLUnionPattern) i).getSubPatterns()) {
					translationSubPatterns.add(j);
				}
			} else {
				translationSubPatterns.add(i);
			}
		}
	
		final List<SPARQLGraphPattern> expectedResults = new ArrayList<>();
		p = NodeFactory.createURI("p2");
		expectedResults.add(new TriplePatternImpl(s, p, o));
		p = NodeFactory.createURI("Subtype");
		expectedResults.add(new TriplePatternImpl(s, p, o));
		
		//Union subpatterns
		p = NodeFactory.createURI("p5");
		expectedResults.add(new TriplePatternImpl(s, p, o));
		p = NodeFactory.createURI("p6");
		expectedResults.add(new TriplePatternImpl(s, p, o));
		
		final BGPImpl intersection = new BGPImpl();
		p = NodeFactory.createURI("p3");
		intersection.addTriplePattern(new TriplePatternImpl(s, p, o));
		p = NodeFactory.createURI("p4");
		intersection.addTriplePattern(new TriplePatternImpl(s, p, o));
		expectedResults.add(intersection);
		
		p = NodeFactory.createURI("Not p1");
		expectedResults.add(new TriplePatternImpl(o, p, s));
		
		assertTrue(translationSubPatterns.containsAll(expectedResults));
		assertTrue(translationSubPatterns.size() == expectedResults.size());
		
		//Testing with variable
		s = NodeFactory.createURI("s");
		p = NodeFactory.createVariable("p");
		o = NodeFactory.createURI("o");
		testTp = new TriplePatternImpl(s, p, o);
		translation = vm.translateTriplePattern(testTp);
		assertTrue(testTp.equals(translation));
	}
	
	@Test
	public void TranslateObjectTest() {
		final Set<Triple> testTriples = new HashSet<>();
		//Equality
		Node s = NodeFactory.createURI("o1");
		Node p = OWL.equivalentClass.asNode();
		Node o = NodeFactory.createURI("o2");
		testTriples.add(new Triple(s, p, o));
		
		//Equality
		p = OWL.sameAs.asNode();
		o = NodeFactory.createURI("o3");
		testTriples.add(new Triple(s, p, o));	
		
		//Object subClass
		o = NodeFactory.createURI("Subclass");
		p = RDFS.subClassOf.asNode();
		testTriples.add(new Triple(o, p, s));
		
		
		//Object Intersection
		p = OWL.intersectionOf.asNode();
		Node blank = NodeFactory.createBlankNode();
		testTriples.add(new Triple(s, p, blank));	
		
		p = RDF.first.asNode();
		o = NodeFactory.createURI("o4");
		testTriples.add(new Triple(blank, p, o));	
		
		p = RDF.rest.asNode();
		o = NodeFactory.createBlankNode();
		testTriples.add(new Triple(blank, p, o));
		
		blank = o;
		p = RDF.first.asNode();
		o = NodeFactory.createURI("o5");
		testTriples.add(new Triple(blank, p, o));	
		
		p = RDF.rest.asNode();
		o = RDF.nil.asNode();
		testTriples.add(new Triple(blank, p, o));
		
		//Object Union
		p = OWL.unionOf.asNode();
		blank = NodeFactory.createBlankNode();
		testTriples.add(new Triple(s, p, blank));	
		
		p = RDF.first.asNode();
		o = NodeFactory.createURI("o6");
		testTriples.add(new Triple(blank, p, o));	
		
		p = RDF.rest.asNode();
		o = NodeFactory.createBlankNode();
		testTriples.add(new Triple(blank, p, o));
		
		blank = o;
		p = RDF.first.asNode();
		o = NodeFactory.createURI("o7");
		testTriples.add(new Triple(blank, p, o));	
		
		p = RDF.rest.asNode();
		o = RDF.nil.asNode();
		testTriples.add(new Triple(blank, p, o));

		final VocabularyMapping vm = new VocabularyMappingImpl(testTriples);
		
		s = NodeFactory.createURI("s");
		p = RDF.type.asNode();
		o = NodeFactory.createURI("o1");
		TriplePattern testTp = new TriplePatternImpl(s, p, o);
		SPARQLGraphPattern translation = vm.translateTriplePattern(testTp); 
		List<SPARQLGraphPattern> translationSubPatterns = new ArrayList<>();
		assertTrue(translation instanceof SPARQLUnionPattern);
		for(SPARQLGraphPattern i : ((SPARQLUnionPattern) translation).getSubPatterns()) {
			if(i instanceof SPARQLUnionPattern) {
				assertTrue(((SPARQLUnionPattern) i).getNumberOfSubPatterns() == 2);
				for(SPARQLGraphPattern j : ((SPARQLUnionPattern) i).getSubPatterns()) {
					translationSubPatterns.add(j);
				}
			} else {
				translationSubPatterns.add(i);
			}
		}
			
		List<SPARQLGraphPattern> expectedResults = new ArrayList<>();
		o = NodeFactory.createURI("o2");
		expectedResults.add(new TriplePatternImpl(s, p, o));
		o = NodeFactory.createURI("o3");
		expectedResults.add(new TriplePatternImpl(s, p, o));
		o = NodeFactory.createURI("Subclass");
		expectedResults.add(new TriplePatternImpl(s, p, o));
		
		//Union subpatterns
		o = NodeFactory.createURI("o6");
		expectedResults.add(new TriplePatternImpl(s, p, o));
		o = NodeFactory.createURI("o7");
		expectedResults.add(new TriplePatternImpl(s, p, o));
		
		final BGPImpl intersection = new BGPImpl();
		o = NodeFactory.createURI("o4");
		intersection.addTriplePattern(new TriplePatternImpl(s, p, o));
		o = NodeFactory.createURI("o5");
		intersection.addTriplePattern(new TriplePatternImpl(s, p, o));
		expectedResults.add(intersection);
		
		assertTrue(translationSubPatterns.containsAll(expectedResults));
		assertTrue(translationSubPatterns.size() == expectedResults.size());
		
		//Testing with variable
		s = NodeFactory.createURI("s");
		p = NodeFactory.createURI("p");
		p = NodeFactory.createVariable("o");
		testTp = new TriplePatternImpl(s, p, o);
		translation = vm.translateTriplePattern(testTp);
		assertTrue(testTp.equals(translation));
	}
	
	public Pair<Set<Triple>, Set<Triple>> CreateTestTriples(){
		final Set<Triple> testSet = new HashSet<>();
		
		//Equality
		Node s = NodeFactory.createURI("s1");
		Node p = OWL.sameAs.asNode();
		Node o = NodeFactory.createURI("s2");
		testSet.add(new Triple(s, p, o));
		
		//Multiple mappings for same subject
		o = NodeFactory.createURI("s3");
		testSet.add(new Triple(s, p, o));
		
		//Predicate inverse
		s = RDF.type.asNode();
		p = OWL.inverseOf.asNode();
		o = NodeFactory.createURI("Not type");
		testSet.add(new Triple(s, p, o));
			
		//Predicate subProperty
		o = s;
		s = NodeFactory.createURI("Subtype");
		p = RDFS.subPropertyOf.asNode();
		testSet.add(new Triple(s, p, o));
		
		//Object Intersection or union
		s = NodeFactory.createURI("o1");
		//p = OWL.unionOf.asNode();
		p = OWL.intersectionOf.asNode();
		o = NodeFactory.createBlankNode();
		testSet.add(new Triple(s, p, o));
		
		s = o;
		p = RDF.first.asNode();
		o = NodeFactory.createURI("o2");
		testSet.add(new Triple(s, p, o));
		
		p = RDF.rest.asNode();
		o = NodeFactory.createBlankNode();
		testSet.add(new Triple(s, p, o));
		
		s = o;
		p = RDF.first.asNode();
		o = NodeFactory.createURI("o3");
		testSet.add(new Triple(s, p, o));
		
		p = RDF.rest.asNode();
		o = RDF.nil.asNode();
		testSet.add(new Triple(s, p, o));	
		
		/* Wrong predicate should lead to error
		p = OWL.unionOf.asNode();
		o = NodeFactory.createURI("o4");
		testSet.add(new Triple(s, p, o));
		*/
		
		 /*Not possible mapping should lead to error
		 p = OWL.inverseOf.asNode();
		 o = NodeFactory.createURI("o5");
		 testSet.add(new Triple(s, p, o));
		 */
		
		Set<Triple> expectedResults = new HashSet<>();
		s = NodeFactory.createURI("s2");
		p = NodeFactory.createURI("Subtype");
		o = NodeFactory.createURI("o2");
		expectedResults.add(new Triple(s, p , o));
		
		o = NodeFactory.createURI("o3");
		expectedResults.add(new Triple(s, p , o));
		
		s = NodeFactory.createURI("s3");
		expectedResults.add(new Triple(s, p , o));	
		
		o = NodeFactory.createURI("o2");
		expectedResults.add(new Triple(s, p , o));
		
		s = o;
		p = NodeFactory.createURI("Not type");
		o = NodeFactory.createURI("s2");
		expectedResults.add(new Triple(s, p , o));
		
		o = NodeFactory.createURI("s3");
		expectedResults.add(new Triple(s, p , o));
		
		s = NodeFactory.createURI("o3");
		expectedResults.add(new Triple(s, p , o));
		
		o = NodeFactory.createURI("s2");
		expectedResults.add(new Triple(s, p , o));
	
		return new Pair<>(testSet, expectedResults);
	}

}
