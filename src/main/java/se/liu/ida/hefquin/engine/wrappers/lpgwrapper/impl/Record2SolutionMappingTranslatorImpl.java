package se.liu.ida.hefquin.engine.wrappers.lpgwrapper.impl;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import se.liu.ida.hefquin.engine.data.SolutionMapping;
import se.liu.ida.hefquin.engine.data.impl.SolutionMappingImpl;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.LPG2RDFConfiguration;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.Record2SolutionMappingTranslator;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.data.RecordEntry;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.data.TableRecord;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.data.impl.LPGNode;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.data.impl.LPGNodeValue;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.data.impl.MapValue;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.query.CypherExpression;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.query.CypherMatchQuery;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.query.CypherQuery;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.query.CypherUnionQuery;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.query.impl.expression.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Record2SolutionMappingTranslatorImpl implements Record2SolutionMappingTranslator {

    private CypherExpression currentMarker =  null;
    private List<AliasedExpression> returnExpressions = null;

    @Override
    public SolutionMapping translateRecord(final TableRecord record, final LPG2RDFConfiguration conf,
                                           final CypherQuery query, final Map<CypherVar, Var> varMap) {
        final BindingBuilder builder = Binding.builder();
        final int n = record.size();
        if (returnExpressions == null)
            returnExpressions = getRelevantReturnExpressions(0, query);

        for (int i = 0; i < n; i++) {
            final RecordEntry current = record.getEntry(i);
            final AliasedExpression aExp = returnExpressions.get(i);
            final CypherVar alias = aExp.getAlias();
            final Var var = varMap.get(alias);
            final CypherExpression expression = aExp.getExpression();
            if (aExp instanceof MarkerExpression) {
                final CypherExpression dataMarker = new LiteralExpression(current.getValue().toString());
                if (currentMarker == null) {
                    currentMarker = expression;
                }
                if (! currentMarker.equals(dataMarker)) {
                    currentMarker = dataMarker;
                    returnExpressions = getRelevantReturnExpressions(extractIndex(dataMarker), query);
                }
                continue;
            }
            if (expression instanceof CountLargerThanZeroExpression) {
                if (current.getValue().toString().equals("true")) continue;
                else return new SolutionMappingImpl(BindingFactory.binding());
            }
            if (expression instanceof CypherVar) {
                builder.add(var, conf.mapNode(((LPGNodeValue) current.getValue()).getNode()));
            } else if (expression instanceof TripleMapExpression) {
                final Map<String, Object> map = ((MapValue) current.getValue()).getMap();
                builder.add(var, NodeFactory.createTripleNode(
                        conf.mapNode((LPGNode) map.get("s")),
                        conf.mapEdgeLabel(map.get("e").toString()),
                        conf.mapNode((LPGNode) map.get("t"))));
            } else if (expression instanceof TypeExpression) {
                builder.add(var, conf.mapEdgeLabel(current.getValue().toString()));
            } else if (expression instanceof GetItemExpression) {
                final int index = ((GetItemExpression) expression).getIndex();
                if (index == 0) {
                    builder.add(var, conf.mapProperty(current.getValue().toString()));
                } else if (index == 1) {
                    builder.add(var, NodeFactory.createLiteral(current.getValue().toString()));
                } else {
                    throw new IllegalArgumentException("Invalid Return Statement");
                }
            } else if (expression instanceof LabelsExpression) {
                builder.add(var, conf.mapNodeLabel(current.getValue().toString()));
            } else if (expression instanceof PropertyAccessExpression) {
                builder.add(var, NodeFactory.createLiteral(current.getValue().toString()));
            } else if (expression instanceof LiteralExpression) {
                builder.add(var, conf.getLabel());
            } else {
                throw new IllegalArgumentException("Invalid Return Statement");
            }
        }

        return new SolutionMappingImpl(builder.build());
    }

    private int extractIndex(CypherExpression dataMarker) {
        final String literal = dataMarker.toString();
        return Integer.parseInt(literal.substring(2, literal.length()-1));
    }

    private List<AliasedExpression> getRelevantReturnExpressions(final int index, final CypherQuery query) {
        if (query instanceof CypherMatchQuery)
            return ((CypherMatchQuery) query).getReturnExprs();
        return ((CypherUnionQuery) query).getSubqueries().get(index).getReturnExprs();
    }

    @Override
    public List<SolutionMapping> translateRecords(final List<TableRecord> records,
                                                  final LPG2RDFConfiguration conf, final CypherQuery query,
                                                  final Map<CypherVar, Var> varMap) {
        final List<SolutionMapping> results = new ArrayList<>();
        for (final TableRecord record : records) {
            results.add(translateRecord(record, conf, query, varMap));
        }
        return results;
    }

}
