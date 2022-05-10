package se.liu.ida.hefquin.engine.wrappers.graphqlwrapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static java.util.Map.entry;
import static org.junit.Assert.assertEquals;

import org.apache.jena.atlas.json.JsonNull;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.datatypes.xsd.impl.XSDBaseNumericType;
import org.apache.jena.datatypes.xsd.impl.XSDBaseStringType;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.junit.Test;

import se.liu.ida.hefquin.engine.query.BGP;
import se.liu.ida.hefquin.engine.query.TriplePattern;
import se.liu.ida.hefquin.engine.query.impl.BGPImpl;
import se.liu.ida.hefquin.engine.query.impl.TriplePatternImpl;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.data.GraphQLEntrypoint;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.data.GraphQLViewProperty;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.data.impl.GraphQLEntrypointImpl;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.data.impl.GraphQLEntrypointType;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.data.impl.GraphQLFieldType;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.data.impl.GraphQLViewPropertyImpl;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.impl.RdfViewConfigurationImpl;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.impl.SPARQL2GraphQLTranslatorImpl;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.query.GraphQLQuery;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.query.impl.GraphQLQueryImpl;

public class SPARQL2GraphQLTranslatorTest {
    
    // Author properties
    protected static final GraphQLViewProperty a1 = new GraphQLViewPropertyImpl("id","ID!",GraphQLFieldType.SCALAR);
    protected static final GraphQLViewProperty a2 = new GraphQLViewPropertyImpl("name","String!",GraphQLFieldType.SCALAR);
    protected static final GraphQLViewProperty a3 = new GraphQLViewPropertyImpl("age","Int!",GraphQLFieldType.SCALAR);
    protected static final GraphQLViewProperty a4 = new GraphQLViewPropertyImpl("books","Book",GraphQLFieldType.OBJECT);

    // Book properties
    protected static final GraphQLViewProperty b1 = new GraphQLViewPropertyImpl("id","ID!",GraphQLFieldType.SCALAR);
    protected static final GraphQLViewProperty b2 = new GraphQLViewPropertyImpl("title","String!",GraphQLFieldType.SCALAR);
    protected static final GraphQLViewProperty b3 = new GraphQLViewPropertyImpl("nr_pages","Int!",GraphQLFieldType.SCALAR);
    protected static final GraphQLViewProperty b4 = new GraphQLViewPropertyImpl("genre","String",GraphQLFieldType.SCALAR);
    protected static final GraphQLViewProperty b5 = new GraphQLViewPropertyImpl("authors","Author",GraphQLFieldType.OBJECT);

    // Parameter definitions for entrypoints
    protected static final TreeMap<String,String> paramDefs1 = new TreeMap<>(Map.ofEntries(
        entry("id", "ID!")
    ));

    protected static final TreeMap<String,String> paramDefs2 = new TreeMap<>(Map.ofEntries(
        entry("name","String"),
        entry("age","Int")
    ));

    protected static final TreeMap<String,String> paramDefs3 = new TreeMap<>(Map.ofEntries(
        entry("title","String"),
        entry("nr_pages","Int"),
        entry("genre","Genre")
    ));

    // query entrypoints
    protected static final GraphQLEntrypoint e1 = new GraphQLEntrypointImpl("author", paramDefs1, "Author");
    protected static final GraphQLEntrypoint e2 = new GraphQLEntrypointImpl("authors", paramDefs2, "Author");
    protected static final GraphQLEntrypoint e3 = new GraphQLEntrypointImpl("allAuthors", new TreeMap<>(), "Author");
    protected static final GraphQLEntrypoint e4 = new GraphQLEntrypointImpl("book", paramDefs1, "Book");
    protected static final GraphQLEntrypoint e5 = new GraphQLEntrypointImpl("books", paramDefs3, "Book");
    protected static final GraphQLEntrypoint e6 = new GraphQLEntrypointImpl("allBooks", new TreeMap<>(), "Book");

    // Variables nodes
    protected static final Node var1 = NodeFactory.createVariable("author");
    protected static final Node var2 = NodeFactory.createVariable("book");
    protected static final Node var3 = NodeFactory.createVariable("name");
    protected static final Node var4 = NodeFactory.createVariable("title");
    protected static final Node var5 = NodeFactory.createVariable("id");
    protected static final Node var6 = NodeFactory.createVariable("s");
    protected static final Node var7 = NodeFactory.createVariable("p");
    protected static final Node var8 = NodeFactory.createVariable("o");

    // URI nodes
    protected static final Node uri1 = NodeFactory.createURI("p:id_of_Author");
    protected static final Node uri2 = NodeFactory.createURI("p:name_of_Author");
    protected static final Node uri3 = NodeFactory.createURI("p:age_of_Author");
    protected static final Node uri4 = NodeFactory.createURI("p:books_of_Author");
    protected static final Node uri5 = NodeFactory.createURI("p:id_of_Book");
    protected static final Node uri6 = NodeFactory.createURI("p:title_of_Book");
    protected static final Node uri7 = NodeFactory.createURI("p:nr_pages_of_Book");
    protected static final Node uri8 = NodeFactory.createURI("p:genre_of_Book");
    protected static final Node uri9 = NodeFactory.createURI("p:authors_of_Book");
    protected static final Node uri10 = NodeFactory.createURI("rdf:type");
    protected static final Node uri11 = NodeFactory.createURI("c:Author");
    protected static final Node uri12 = NodeFactory.createURI("c:Book");

    // Literal nodes
    protected static final Node lit1 = NodeFactory.createLiteral("auth3", XSDBaseStringType.XSDstring);
    protected static final Node lit2 = NodeFactory.createLiteral("book5", XSDBaseStringType.XSDstring);
    protected static final Node lit3 = NodeFactory.createLiteral("39", XSDBaseNumericType.XSDunsignedInt);
    protected static final Node lit4 = NodeFactory.createLiteral("William Shakespeare", XSDBaseStringType.XSDstring);
    protected static final Node lit5 = NodeFactory.createLiteral("MYSTERY", XSDBaseStringType.XSDstring);

    // Translator and config
    protected static final SPARQL2GraphQLTranslator translator = new SPARQL2GraphQLTranslatorImpl();
    protected static final RdfViewConfiguration config = initializeRDFViewConfig();


    /**
     * Initializes the RDFViewConfiguration
     */
    protected static final RdfViewConfiguration initializeRDFViewConfig(){
        Map<String,GraphQLViewProperty> authorProperties = new HashMap<>();
        Map<String,GraphQLViewProperty> bookProperties = new HashMap<>();
        Map<String,Map<String,GraphQLViewProperty>> classToProperty = new HashMap<>();
        Map<GraphQLEntrypointType,GraphQLEntrypoint> authorEntrypoints = new HashMap<>();
        Map<GraphQLEntrypointType,GraphQLEntrypoint> bookEntrypoints = new HashMap<>();
        Map<String,Map<GraphQLEntrypointType,GraphQLEntrypoint>> classToEntrypoint = new HashMap<>();
        authorProperties.put("id",a1);
        authorProperties.put("name",a2);
        authorProperties.put("age",a3);
        authorProperties.put("books",a4);
        bookProperties.put("id",b1);
        bookProperties.put("title",b2);
        bookProperties.put("nr_pages",b3);
        bookProperties.put("genre",b4);
        bookProperties.put("authors",b5);

        classToProperty.put("Author",authorProperties);
        classToProperty.put("Book",bookProperties);

        authorEntrypoints.put(GraphQLEntrypointType.SINGLE,e1);
        authorEntrypoints.put(GraphQLEntrypointType.FILTERED,e2);
        authorEntrypoints.put(GraphQLEntrypointType.FULL,e3);

        bookEntrypoints.put(GraphQLEntrypointType.SINGLE,e4);
        bookEntrypoints.put(GraphQLEntrypointType.FILTERED,e5);
        bookEntrypoints.put(GraphQLEntrypointType.FULL,e6);

        classToEntrypoint.put("Author", authorEntrypoints);
        classToEntrypoint.put("Book", bookEntrypoints);
        return new RdfViewConfigurationImpl(classToProperty, classToEntrypoint);
    }


    @Test
    public void test1(){
        /**
         * ?author p:id_of_Author    "auth3" .
         * ?author p:name_of_Author  ?name .
         * ?author p:books_of_Author ?book .
         * ?book   p:title_of_Book   ?title
         */
        Set<TriplePattern> tps = new HashSet<>();
        tps.add(new TriplePatternImpl(var1, uri1, lit1));
        tps.add(new TriplePatternImpl(var1, uri2, var3));
        tps.add(new TriplePatternImpl(var1, uri4, var2));
        tps.add(new TriplePatternImpl(var2, uri6, var4));
        final BGP bgp = new BGPImpl(tps);
        final GraphQLQuery translatedQuery = translator.translateBGP(bgp, config);

        // Expected result
        TreeSet<String> fieldPaths = new TreeSet<>();
        fieldPaths.add("ep_single0:author(id:$var0)/id_Author:id");
        fieldPaths.add("ep_single0:author(id:$var0)/node_books_of_Author:books/id_Book:id");
        fieldPaths.add("ep_single0:author(id:$var0)/node_books_of_Author:books/scalar_title_of_Book:title");
        fieldPaths.add("ep_single0:author(id:$var0)/scalar_id_of_Author:id");
        fieldPaths.add("ep_single0:author(id:$var0)/scalar_name_of_Author:name");
        JsonObject parameterValues = new JsonObject();
        parameterValues.put("var0", "auth3");
        TreeMap<String,String> parameterDefinitions = new TreeMap<>();
        parameterDefinitions.put("var0","ID!");
        final GraphQLQuery expectedQuery = new GraphQLQueryImpl(fieldPaths, parameterValues, parameterDefinitions);

        assertEquals(expectedQuery.toString(), translatedQuery.toString());
        assertEquals(parameterValues, translatedQuery.getParameterValues());
    }

    @Test
    public void test2(){
        /**
         * ?s ?p ?o .
         */
        Set<TriplePattern> tps = new HashSet<>();
        tps.add(new TriplePatternImpl(var6, var7, var8));
        final BGP bgp = new BGPImpl(tps);
        final GraphQLQuery translatedQuery = translator.translateBGP(bgp, config);

        // Expected Result
        TreeSet<String> fieldPaths = new TreeSet<>();
        fieldPaths.add("ep_full0:allBooks/id_Book:id");
        fieldPaths.add("ep_full0:allBooks/node_authors_of_Book:authors/id_Author:id");
        fieldPaths.add("ep_full0:allBooks/scalar_genre_of_Book:genre");
        fieldPaths.add("ep_full0:allBooks/scalar_id_of_Book:id");
        fieldPaths.add("ep_full0:allBooks/scalar_nr_pages_of_Book:nr_pages");
        fieldPaths.add("ep_full0:allBooks/scalar_title_of_Book:title");
        fieldPaths.add("ep_full1:allAuthors/id_Author:id");
        fieldPaths.add("ep_full1:allAuthors/node_books_of_Author:books/id_Book:id");
        fieldPaths.add("ep_full1:allAuthors/scalar_age_of_Author:age");
        fieldPaths.add("ep_full1:allAuthors/scalar_id_of_Author:id");
        fieldPaths.add("ep_full1:allAuthors/scalar_name_of_Author:name");
        final JsonObject parameterValues = new JsonObject();
        final TreeMap<String,String> parameterDefinitions = new TreeMap<>();
        final GraphQLQuery expectedQuery = new GraphQLQueryImpl(fieldPaths, parameterValues, parameterDefinitions);

        assertEquals(expectedQuery.toString(), translatedQuery.toString());
        assertEquals(parameterValues, translatedQuery.getParameterValues());
    }

    @Test
    public void test3(){
        /**
         * ?author p:id_of_Author    "auth3" .
         * ?author p:books_of_Author ?book .
         * ?book   ?p                ?o .
         */
        Set<TriplePattern> tps = new HashSet<>();
        tps.add(new TriplePatternImpl(var1, uri1, lit1));
        tps.add(new TriplePatternImpl(var1, uri4, var2));
        tps.add(new TriplePatternImpl(var2, var7, var8));
        final BGP bgp = new BGPImpl(tps);
        final GraphQLQuery translatedQuery = translator.translateBGP(bgp, config);

        // Expected result
        TreeSet<String> fieldPaths = new TreeSet<>();
        fieldPaths.add("ep_single0:author(id:$var0)/id_Author:id");
        fieldPaths.add("ep_single0:author(id:$var0)/node_books_of_Author:books/id_Book:id");
        fieldPaths.add("ep_single0:author(id:$var0)/node_books_of_Author:books/node_authors_of_Book:authors/id_Author:id");
        fieldPaths.add("ep_single0:author(id:$var0)/node_books_of_Author:books/scalar_genre_of_Book:genre");
        fieldPaths.add("ep_single0:author(id:$var0)/node_books_of_Author:books/scalar_id_of_Book:id");
        fieldPaths.add("ep_single0:author(id:$var0)/node_books_of_Author:books/scalar_nr_pages_of_Book:nr_pages");
        fieldPaths.add("ep_single0:author(id:$var0)/node_books_of_Author:books/scalar_title_of_Book:title");
        fieldPaths.add("ep_single0:author(id:$var0)/scalar_id_of_Author:id");
        JsonObject parameterValues = new JsonObject();
        parameterValues.put("var0", "auth3");
        TreeMap<String,String> parameterDefinitions = new TreeMap<>();
        parameterDefinitions.put("var0","ID!");
        final GraphQLQuery expectedQuery = new GraphQLQueryImpl(fieldPaths, parameterValues, parameterDefinitions);
        
        assertEquals(expectedQuery.toString(), translatedQuery.toString());
        assertEquals(parameterValues, translatedQuery.getParameterValues());
    }

    @Test
    public void test4(){
        /**
         * ?s rdf:type c:Author .
         * ?s ?p       ?o .
         */
        Set<TriplePattern> tps = new HashSet<>();
        tps.add(new TriplePatternImpl(var6, uri10, uri11));
        tps.add(new TriplePatternImpl(var6, var7, var8));
        final BGP bgp = new BGPImpl(tps);
        final GraphQLQuery translatedQuery = translator.translateBGP(bgp, config);

        // Expected result
        TreeSet<String> fieldPaths = new TreeSet<>();
        fieldPaths.add("ep_full0:allAuthors/id_Author:id");
        fieldPaths.add("ep_full0:allAuthors/node_books_of_Author:books/id_Book:id");
        fieldPaths.add("ep_full0:allAuthors/scalar_age_of_Author:age");
        fieldPaths.add("ep_full0:allAuthors/scalar_id_of_Author:id");
        fieldPaths.add("ep_full0:allAuthors/scalar_name_of_Author:name");
        final JsonObject parameterValues = new JsonObject();
        final TreeMap<String,String> parameterDefinitions = new TreeMap<>();
        final GraphQLQuery expectedQuery = new GraphQLQueryImpl(fieldPaths, parameterValues, parameterDefinitions);
        
        assertEquals(expectedQuery.toString(), translatedQuery.toString());
        assertEquals(parameterValues, translatedQuery.getParameterValues());
    }

    @Test
    public void test5(){
        /**
         * ?author p:books_of_Author  ?book .
         * ?author p:name_of_Author   "William Shakespeare" .
         * ?book   p:nr_pages_of_Book 39 .
         * ?book   p:authors_of_Book  ?author .
         */
        Set<TriplePattern> tps = new HashSet<>();
        tps.add(new TriplePatternImpl(var1, uri4, var2));
        tps.add(new TriplePatternImpl(var1, uri2, lit4));
        tps.add(new TriplePatternImpl(var2, uri7, lit3));
        tps.add(new TriplePatternImpl(var2, uri9, var1));
        final BGP bgp = new BGPImpl(tps);
        final GraphQLQuery translatedQuery = translator.translateBGP(bgp, config);

        // Expected result
        TreeSet<String> fieldPaths = new TreeSet<>();
        fieldPaths.add("ep_filtered0:authors(age:$var0,name:$var1)/id_Author:id");
        fieldPaths.add("ep_filtered0:authors(age:$var0,name:$var1)/node_books_of_Author:books/id_Book:id");
        fieldPaths.add("ep_filtered0:authors(age:$var0,name:$var1)/node_books_of_Author:books/node_authors_of_Book:authors/id_Author:id");
        fieldPaths.add("ep_filtered0:authors(age:$var0,name:$var1)/node_books_of_Author:books/scalar_nr_pages_of_Book:nr_pages");
        fieldPaths.add("ep_filtered0:authors(age:$var0,name:$var1)/scalar_name_of_Author:name");
        JsonObject parameterValues = new JsonObject();
        parameterValues.put("var0", JsonNull.instance);
        parameterValues.put("var1", "William Shakespeare");
        TreeMap<String,String> parameterDefinitions = new TreeMap<>();
        parameterDefinitions.put("var0", "Int");
        parameterDefinitions.put("var1", "String");
        final GraphQLQuery expectedQuery = new GraphQLQueryImpl(fieldPaths, parameterValues, parameterDefinitions);
        
        assertEquals(expectedQuery.toString(), translatedQuery.toString());
        assertEquals(parameterValues, translatedQuery.getParameterValues());
    }

    @Test
    public void test6(){
        /**
         * ?book p:genre_of_Book "MYSTERY" .
         * ?book p:title_of_Book ?title .
         */
        Set<TriplePattern> tps = new HashSet<>();
        tps.add(new TriplePatternImpl(var2, uri8, lit5));
        tps.add(new TriplePatternImpl(var2, uri6, var4));
        final BGP bgp = new BGPImpl(tps);
        final GraphQLQuery translatedQuery = translator.translateBGP(bgp, config);

        // Expected result
        TreeSet<String> fieldPaths = new TreeSet<>();
        fieldPaths.add("ep_filtered0:books(genre:$var0,nr_pages:$var1,title:$var2)/id_Book:id");
        fieldPaths.add("ep_filtered0:books(genre:$var0,nr_pages:$var1,title:$var2)/scalar_genre_of_Book:genre");
        fieldPaths.add("ep_filtered0:books(genre:$var0,nr_pages:$var1,title:$var2)/scalar_title_of_Book:title");
        JsonObject parameterValues = new JsonObject();
        parameterValues.put("var0", "MYSTERY");
        parameterValues.put("var1", JsonNull.instance);
        parameterValues.put("var2", JsonNull.instance);
        TreeMap<String,String> parameterDefinitions = new TreeMap<>();
        parameterDefinitions.put("var0", "Genre");
        parameterDefinitions.put("var1", "Int");
        parameterDefinitions.put("var2", "String");
        final GraphQLQuery expectedQuery = new GraphQLQueryImpl(fieldPaths, parameterValues, parameterDefinitions);
        
        assertEquals(expectedQuery.toString(), translatedQuery.toString());
        assertEquals(parameterValues, translatedQuery.getParameterValues());
    }
}
