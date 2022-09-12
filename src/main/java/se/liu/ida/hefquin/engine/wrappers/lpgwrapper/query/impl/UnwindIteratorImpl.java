package se.liu.ida.hefquin.engine.wrappers.lpgwrapper.query.impl;

import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.query.CypherVar;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.query.UnwindIterator;
import se.liu.ida.hefquin.engine.wrappers.lpgwrapper.query.WhereCondition;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UnwindIteratorImpl implements UnwindIterator {

    protected final CypherVar innerVar;
    //TODO: this below should be a CypherExpression
    protected final String listExpression;
    protected final List<WhereCondition> filters;
    protected final List<String> returnExpressions;
    protected final CypherVar alias;

    public UnwindIteratorImpl(final CypherVar innerVar, final String listExpression,
                              final List<WhereCondition> filters, final List<String> returnExpressions,
                              final CypherVar alias) {
        assert innerVar != null;
        assert listExpression != null;
        assert alias != null;

        this.innerVar = innerVar;
        this.listExpression = listExpression;
        this.filters = filters;
        this.returnExpressions = returnExpressions;
        this.alias = alias;
    }

    @Override
    public CypherVar getInnerVar() {
        return innerVar;
    }

    @Override
    public String getListExpression() {
        return listExpression;
    }

    @Override
    public List<WhereCondition> getFilters() {
        return filters;
    }

    @Override
    public List<String> getReturnExpressions() {
        return returnExpressions;
    }

    @Override
    public CypherVar getAlias() {
        return alias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnwindIteratorImpl that = (UnwindIteratorImpl) o;
        return innerVar.equals(that.innerVar) && listExpression.equals(that.listExpression)
                && Objects.equals(filters, that.filters)
                && Objects.equals(returnExpressions, that.returnExpressions)
                && alias.equals(that.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(innerVar, listExpression, filters, returnExpressions, alias);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("UNWIND [")
                .append(innerVar)
                .append(" IN ")
                .append(listExpression);
        if (filters != null && !filters.isEmpty()) {
            builder.append(" WHERE ");
            builder.append(filters.stream().map(Objects::toString).collect(Collectors.joining(" AND ")));
        }
        builder.append(" | [")
                .append(String.join(", ", returnExpressions))
                .append("]] AS ")
                .append(alias);
        return builder.toString();
    }

}
