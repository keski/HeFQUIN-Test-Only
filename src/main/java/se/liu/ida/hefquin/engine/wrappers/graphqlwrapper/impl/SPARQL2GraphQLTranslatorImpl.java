package se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.jena.atlas.json.JsonNull;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.impl.LiteralLabel;

import se.liu.ida.hefquin.engine.federation.GraphQLEndpoint;
import se.liu.ida.hefquin.engine.query.BGP;
import se.liu.ida.hefquin.engine.query.TriplePattern;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.GraphQL2RDFConfiguration;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.SPARQL2GraphQLTranslator;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.data.GraphQLArgument;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.data.GraphQLEntrypoint;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.data.impl.GraphQLArgumentImpl;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.data.impl.GraphQLEntrypointPath;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.data.impl.GraphQLEntrypointType;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.data.impl.GraphQLFieldType;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.data.impl.GraphQLIDPath;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.query.GraphQLQuery;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.query.impl.GraphQLQueryImpl;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.utils.GraphCycleDetector;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.utils.SGPNode;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.utils.SPARQL2GraphQLHelper;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.utils.StarPattern;
import se.liu.ida.hefquin.engine.wrappers.graphqlwrapper.utils.URI2GraphQLHelper;

public class SPARQL2GraphQLTranslatorImpl implements SPARQL2GraphQLTranslator {

    @Override
    public GraphQLQuery translateBGP(final BGP bgp, final GraphQL2RDFConfiguration config,
            final GraphQLEndpoint endpoint) {

        // Initialize necessary data structures
        // - collection of subject-based star patterns of the given
        //   BGP, indexed by their respective subject nodes
        final Map<Node, StarPattern> indexedStarPatterns = decomposeIntoStarPatterns(bgp);
        // - mapping from triple patterns to star patterns such that
        //   each triple pattern whose object is the subject of a star
        //   pattern maps to that star pattern
        final Map<TriplePattern, StarPattern> connectors = createConnectors(indexedStarPatterns);
        // - subset of the star patterns, contains only the ones from
        //   which we need to create entry points for the GraphQL query.
        final Set<GraphQLTypedStarPattern> rootStarPatterns = determineRootStarPatterns( indexedStarPatterns.values(), connectors, config );

        final SPARQL2GraphQLHelper helper = new SPARQL2GraphQLHelper(config, endpoint, indexedStarPatterns, connectors);

        // Check whether it was possible to create suitable root star patterns.
        // If not, we need to return a GraphQL query that fetches everything
        // from the GraphQL endpoint.
        if ( rootStarPatterns == null ) {
            return materializeAll(helper);
        }

        return generateQueryData(helper, rootStarPatterns);
    }

    /**
     * Genereates a GraphQL query that fetches everyhing from @param endpoint
     */
    protected GraphQLQuery materializeAll( final SPARQL2GraphQLHelper helper ) {

        int entrypointCounter = 0;
        final Set<String> finishedFieldPaths = new HashSet<>();
        final Set<String> objectTypeNames = helper.getEndpoint().getGraphQLObjectTypes();
        for(final String objectTypeName : objectTypeNames){
            // Get the full list entrypoint
            final GraphQLEntrypoint e = helper.getEndpoint().getEntrypoint(objectTypeName,GraphQLEntrypointType.FULL);
            final String currentPath = new GraphQLEntrypointPath(e, entrypointCounter).toString();
            finishedFieldPaths.add(currentPath + new GraphQLIDPath(objectTypeName));

            final Set<String> allObjectURI = URI2GraphQLHelper.getPropertyURIs(objectTypeName,GraphQLFieldType.OBJECT, helper.getConfig(), helper.getEndpoint() );
            final Set<String> allScalarURI = URI2GraphQLHelper.getPropertyURIs(objectTypeName,GraphQLFieldType.SCALAR, helper.getConfig(), helper.getEndpoint() );

            // Add all fields that nests another object
            for(final String uri : allObjectURI){
                finishedFieldPaths.add( helper.addEmptyObjectField(currentPath,objectTypeName,uri) );
            }
            // Add all fields that represent scalar values
            for(final String uri : allScalarURI){
                finishedFieldPaths.add( helper.addScalarField(currentPath,uri) );
            }

            ++entrypointCounter;
        }

        return new GraphQLQueryImpl(finishedFieldPaths,new HashSet<>());
    }

    /**
     * Decomposes the given BGP into its subject-based star patterns and returns
     * the resulting star patterns indexed by their respective subject nodes.
     */
    protected Map<Node, StarPattern> decomposeIntoStarPatterns(final BGP bgp){
        final Map<Node, StarPattern> result = new HashMap<>();
        for ( final TriplePattern tp : bgp.getTriplePatterns() ){
            final Node subject = tp.asJenaTriple().getSubject();

            StarPattern starPattern = result.get(subject);
            if ( starPattern == null ) {
                starPattern = new GraphQLTypedStarPattern();
                result.put(subject, starPattern);
            }

            starPattern.addTriplePattern(tp);
        }

        return result;
    }

    /**
     * Creates a connector map using @param indexedStarPatterns
     */
    protected Map<TriplePattern,StarPattern> createConnectors( final Map<Node, StarPattern> indexedStarPatterns ) {
        final Map<TriplePattern,StarPattern> connectors = new HashMap<>();
        final Map<Node,SGPNode> sgpNodes = new HashMap<>();

        for ( final StarPattern sp : indexedStarPatterns.values() ) {
            final Node subject = sp.getSubject();
            for ( final TriplePattern tp : sp.getTriplePatterns() ) {
                final Node object = tp.asJenaTriple().getObject();
                final StarPattern spForObject = indexedStarPatterns.get(object); // may be null!

                if ( spForObject != null && ! object.equals(subject) ) {
                    SGPNode subjectSgpNode = sgpNodes.get(subject);
                    SGPNode objectSgpNode = sgpNodes.get(object);

                    if ( subjectSgpNode == null ) {
                        subjectSgpNode = new SGPNode();
                        sgpNodes.put(subject, subjectSgpNode);
                    }

                    if ( objectSgpNode == null ) {
                        objectSgpNode = new SGPNode();
                        sgpNodes.put(object, objectSgpNode);
                    }

                    subjectSgpNode.addAdjacentNode(tp, objectSgpNode);
                    connectors.put(tp, spForObject);
                }

            }
        }

        // Remove all potential cyclic connector bindings
        final Set<TriplePattern> connectorsToBeRemoved = GraphCycleDetector.determineCyclicConnectors(sgpNodes.values());
        connectors.keySet().removeAll(connectorsToBeRemoved);
        return connectors;
    } 

    /**
     * Returns the star patterns from the given collection that will be used
     * for creating entry points in the GraphQL query to be generated.
     *
     * To this end, the given collection of star patterns is first filtered
     * to ignore every star pattern that has an incoming connector.
     * Thereafter, for each of the remaining star patterns (i.e., the ones that
     * do not have an incoming connector), the corresponding GraphQL object type
     * is determined and associated with the star pattern before adding it to
     * the output set.
     * Finally, the output set is returned only if it was possible to determine
     * a corresponding GraphQL object type for all of the star patterns therein.
     * If there is at least one such star pattern for which it was not possible,
     * then this function returns <code>null</code>.
     */
    protected Set<GraphQLTypedStarPattern> determineRootStarPatterns( final Collection<StarPattern> sps,
                                                                      final Map<TriplePattern, StarPattern> connectors,
                                                                      final GraphQL2RDFConfiguration config ) {
        final Set<GraphQLTypedStarPattern> result = new HashSet<>();
        for ( final StarPattern sp : sps ) {
            final boolean hasConnectors = connectors.containsValue(sp);
            if ( ! hasConnectors ) { // ignore star patterns that have incoming connectors
                final String type = SPARQL2GraphQLHelper.determineSgpType(sp, config);
                // If the GraphQL object type for the current star pattern cannot
                // be determined, then we can immediately return null.
                if ( type == null ) {
                    return null;
                }

                final GraphQLTypedStarPattern gtsp = (GraphQLTypedStarPattern) sp;
                gtsp.setGraphQLObjectType(type);
                result.add(gtsp);
            }
        }
        return result;
    }

    /**
     * Generates a GraphQL query from provided @param indexedStarPatterns,connectors,withoutConnnectors
     */
    protected GraphQLQuery generateQueryData( final SPARQL2GraphQLHelper helper,
                                              final Set<GraphQLTypedStarPattern> rootStarPatterns ) {
        final Set<String> fieldPaths = new HashSet<>();
        final Set<GraphQLArgument> queryArgs = new HashSet<>();
        
        // Counters for creating unique variable and entrypoint names
        int entrypointCounter = 0;
        int variableCounter = 0;

        // Create an entrypoint for each star pattern without an incomming connector binding
        for ( GraphQLTypedStarPattern sp : rootStarPatterns ) {
            final Map<String, LiteralLabel> sgpArguments = helper.getArguments(sp);
            final String spType = sp.getGraphQLObjectType();
            final GraphQLEntrypoint e;

            // Select entrypoint with regards to if the SGP has the required arguments
            if (SPARQL2GraphQLHelper.hasAllNecessaryArguments(sgpArguments.keySet(),
                    helper.getEndpoint().getEntrypoint(spType, GraphQLEntrypointType.SINGLE).getArgumentDefinitions().keySet())) {
                // Get single object entrypoint
                e = helper.getEndpoint().getEntrypoint(spType, GraphQLEntrypointType.SINGLE);
            } else if (SPARQL2GraphQLHelper.hasNecessaryArguments(sgpArguments.keySet(),
            		helper.getEndpoint().getEntrypoint(spType, GraphQLEntrypointType.FILTERED).getArgumentDefinitions().keySet())) {
                // Get filtered list entrypoint
                e = helper.getEndpoint().getEntrypoint(spType, GraphQLEntrypointType.FILTERED);
            } else {
                // Get full list entrypoint (No argument values)
                e = helper.getEndpoint().getEntrypoint(spType, GraphQLEntrypointType.FULL);
            }

            // Create GraphQLArguments for the current path
            final Set<GraphQLArgument> pathArguments = new LinkedHashSet<>();
            final Map<String, String> entrypointArgDefs = e.getArgumentDefinitions();

            if (entrypointArgDefs != null && !entrypointArgDefs.isEmpty()) {
                for (final String argName : new TreeSet<String>(entrypointArgDefs.keySet())) {
                    final String variableName = "var" + variableCounter;

                    final String currArgDefinition = entrypointArgDefs.get(argName);
                    final JsonValue currArgValue;

                    if (sgpArguments.containsKey(argName)) {
                        currArgValue = SPARQL2GraphQLHelper.literalToJsonValue(sgpArguments.get(argName));
                    } else {
                        currArgValue = JsonNull.instance;
                    }

                    final GraphQLArgument currArg = new GraphQLArgumentImpl(variableName, argName, currArgValue, currArgDefinition);
                    queryArgs.add(currArg);
                    pathArguments.add(currArg);
                    ++variableCounter;
                }
            }

            final String currentPath = new GraphQLEntrypointPath(e,entrypointCounter,pathArguments).toString();

            fieldPaths.addAll( helper.addSgp(sp, currentPath, spType) );
            ++entrypointCounter;
        }

        return new GraphQLQueryImpl(fieldPaths, queryArgs);
    }


    protected class GraphQLTypedStarPattern extends StarPattern {
        protected String type;
        public String getGraphQLObjectType() { return type; }
        public void setGraphQLObjectType( final String type ) { this.type = type; }
    }

}
