package se.liu.ida.hefquin.engine.wrappers.lpgwrapper.utils;

import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.query.*;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.query.impl.CypherUnionQueryImpl;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.query.impl.expression.*;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.query.impl.match.EdgeMatchClause;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.query.impl.match.NodeMatchClause;

import java.util.*;
import java.util.stream.Collectors;

public class CypherQueryCombinator {
    public static CypherQuery combine(final CypherQuery q1, final CypherQuery q2, final CypherVarGenerator gen) {
        if (q1 instanceof CypherMatchQuery && q2 instanceof CypherMatchQuery) {
            return combineMatchMatch((CypherMatchQuery) q1, (CypherMatchQuery) q2, gen);
        } else if (q1 instanceof  CypherUnionQuery && q2 instanceof CypherMatchQuery) {
            return combineUnionMatch((CypherUnionQuery) q1, (CypherMatchQuery) q2, gen);
        } else if (q1 instanceof CypherMatchQuery && q2 instanceof CypherUnionQuery) {
            return combineUnionMatch((CypherUnionQuery) q2, (CypherMatchQuery) q1, gen);
        } else if (q1 instanceof CypherUnionQuery && q2 instanceof CypherUnionQuery) {
            return combineUnionUnion((CypherUnionQuery) q1, (CypherUnionQuery) q2, gen);
        } else {
            return null;
        }
    }

    private static CypherMatchQuery combineMatchMatch(final CypherMatchQuery q1, final CypherMatchQuery q2,
                                                      final CypherVarGenerator gen) {
        if ( hasInvalidJoins(q1, q2) ) {
            return null;
        }

        final CypherQueryBuilder builder = new CypherQueryBuilder();

        final List<MatchClause> matches1 = q1.getMatches();
        final List<MatchClause> matches2 = q2.getMatches();
        final List<NodeMatchClause> nodes1 = matches1.stream().filter(x -> x instanceof NodeMatchClause)
                .map(x -> (NodeMatchClause) x).collect(Collectors.toList());
        final List<NodeMatchClause> nodes2 = matches2.stream().filter(x -> x instanceof NodeMatchClause)
                .map(x -> (NodeMatchClause) x).collect(Collectors.toList());
        final List<EdgeMatchClause> edges1 = matches1.stream().filter(x -> x instanceof EdgeMatchClause)
                .map(x -> (EdgeMatchClause) x).collect(Collectors.toList());
        final List<EdgeMatchClause> edges2 = matches2.stream().filter(x -> x instanceof EdgeMatchClause)
                .map(x -> (EdgeMatchClause) x).collect(Collectors.toList());
        for (final EdgeMatchClause m : edges1){
            builder.add(m);
        }
        for (final EdgeMatchClause m : edges2) {
            if (! edges1.contains(m)) {
                builder.add(m);
            }
        }
        for (final NodeMatchClause m : nodes1) {
            if (edges1.stream().noneMatch(x->x.isRedundantWith(m)) &&
                edges2.stream().noneMatch(x -> x.isRedundantWith(m))) {
                builder.add(m);
            }
        }
        for (final NodeMatchClause m : nodes2) {
            if ((!nodes1.contains(m)) && edges1.stream().noneMatch(x->x.isRedundantWith(m)) &&
                    edges2.stream().noneMatch(x -> x.isRedundantWith(m))) {
                builder.add(m);
            }
        }
        for (final BooleanCypherExpression c : q1.getConditions())
            builder.add(c);
        for (final BooleanCypherExpression c : q2.getConditions())
            builder.add(c);

        final Set<CypherVar> uvars1 = q1.getUvars();
        final Set<CypherVar> uvars2 = q2.getUvars();
        final Map<CypherVar, List<UnwindIterator>> iteratorJoin = new HashMap<>();
        for (final UnwindIterator i : q1.getIterators()) {
            if (q1.getReturnExprs().stream().noneMatch(x -> q2.getAliases().contains(x.getAlias())
                    && x.getVars().contains(i.getAlias()))) {
                builder.add(i);
            } else {
                iteratorJoin.put(q1.getReturnExprs().stream().filter(x -> x.getVars().contains(i.getAlias()))
                        .findFirst().get().getAlias(), List.of(i));
            }
        }
        for (final UnwindIterator i : q2.getIterators()) {
            if (q2.getReturnExprs().stream().noneMatch(x -> q1.getAliases().contains(x.getAlias())
                    && x.getVars().contains(i.getAlias()))) {
                builder.add(i);
            } else {
                iteratorJoin.get(q1.getReturnExprs().stream().filter(x -> x.getVars().contains(i.getAlias()))
                        .findFirst().get().getAlias()).add(i);
            }
        }
        final Map<CypherVar, CypherVar> unwindVarsMap = new HashMap<>();
        for (final Map.Entry<CypherVar,List<UnwindIterator>> e : iteratorJoin.entrySet()) {
            final UnwindIterator u1 = e.getValue().get(0);
            final UnwindIterator u2 = e.getValue().get(1);
            final List<BooleanCypherExpression> filters = new ArrayList<>(u1.getFilters());
            filters.addAll(u2.getFilters());
            final List<CypherExpression> retExprs = new ArrayList<>(u1.getReturnExpressions());
            for (final CypherExpression ex : u2.getReturnExpressions()) {
                if (! retExprs.contains(ex))
                    retExprs.add(ex);
            }
            final CypherVar anonVar = gen.getAnonVar();
            unwindVarsMap.put(e.getKey(), anonVar);
            builder.add(new UnwindIteratorImpl(u1.getInnerVar(),
                    combineLists(u1.getListExpression(), u2.getListExpression()),
                    filters, retExprs, anonVar));
        }
        for (final AliasedExpression r : q1.getReturnExprs()) {
            if (iteratorJoin.containsKey(r.getAlias())){
                if (r.getExpression() instanceof GetItemExpression) {
                    final GetItemExpression ex = (GetItemExpression) r.getExpression();
                    builder.add(new AliasedExpression(new GetItemExpression(
                        unwindVarsMap.get(r.getAlias()), ex.getIndex()), r.getAlias()));
                } else {
                    throw new IllegalArgumentException("Joins involving UNWIND variables need to return GetItem expressions");
                }
            } else {
                builder.add(r);
            }
        }
        for (final AliasedExpression r : q2.getReturnExprs()) {
            if (q1.getReturnExprs().contains(r)) continue;
            if (iteratorJoin.containsKey(r.getAlias())) continue;
            if (q1.getAliases().contains(r.getAlias())) {
                final AliasedExpression r1 = q1.getReturnExprs().stream()
                        .filter(x -> x.getAlias().equals(r.getAlias())).findFirst().get();
                if (!uvars2.containsAll(r.getVars()) && !uvars1.containsAll(r1.getVars())) {
                    builder.add(new EqualityExpression(r.getExpression(), r1.getExpression()));
                }
            } else {
                builder.add(r);
            }
        }
        return builder.build();
    }

    private static ListCypherExpression combineLists(final ListCypherExpression l1,
                                                     final ListCypherExpression l2) {
        return null;
    }

    private static boolean hasInvalidJoins(final CypherMatchQuery q1, final CypherMatchQuery q2) {
        final List<AliasedExpression> ret1 = q1.getReturnExprs();
        final List<AliasedExpression> ret2 = q2.getReturnExprs();
        final List<CypherVar> aliases1 = q1.getAliases();
        final List<CypherVar> aliases2 = q2.getAliases();
        final Set<CypherVar> uvars1 = q1.getUvars();
        final Set<CypherVar> uvars2 = q1.getUvars();

        for (final AliasedExpression e : ret1) {
            if (aliases2.contains(e.getAlias()) && uvars1.contains(e.getAlias()))
                return true;
        }

        for (final AliasedExpression e : ret2){
            if (aliases1.contains(e.getAlias()) && uvars2.contains(e.getAlias()))
                return true;
        }
        return false;
    }

    private static CypherUnionQuery combineUnionMatch(final CypherUnionQuery q1, final CypherMatchQuery q2,
                                                      final CypherVarGenerator gen) {
        final List<CypherMatchQuery> subqueries = new ArrayList<>();
        for (final CypherMatchQuery q : q1.getSubqueries()) {
            final CypherMatchQuery combination = combineMatchMatch(q, q2, gen);
            if (combination == null){
                return null;
            }
            subqueries.add(combination);
        }
        return new CypherUnionQueryImpl(subqueries);
    }

    private static CypherUnionQuery combineUnionUnion(final CypherUnionQuery q1, final CypherUnionQuery q2,
                                                      final CypherVarGenerator gen) {
        final List<CypherMatchQuery> subqueries = new ArrayList<>();
        for (final CypherMatchQuery q : q1.getSubqueries()) {
            final CypherUnionQuery combination = combineUnionMatch(q2, q, gen);
            if (combination == null) {
                return null;
            }
            subqueries.addAll(combination.getSubqueries());
        }
        return new CypherUnionQueryImpl(subqueries);
    }

}
