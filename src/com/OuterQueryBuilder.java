package com;

import java.util.ArrayList;
import java.util.List;

public class OuterQueryBuilder {

    private AdvanceSQLBuilder innerQuery;
    private final List<String> selectColumns = new ArrayList<>();
    private final String outerAlias = "inner_table";
    private String orderBy;
    private Integer limit;
    private Integer offset;
    private final List<Object> parameters = new ArrayList<>();

    public OuterQueryBuilder(AdvanceSQLBuilder innerQuery) {
        this.innerQuery = innerQuery;
    }

    public OuterQueryBuilder selectFromInner(String columnOrExpr, String alias) {
        selectColumns.add(outerAlias + "." + columnOrExpr + (alias != null ? " AS " + alias : ""));
        return this;
    }

    public OuterQueryBuilder selectStatic(String columnOrExpr, String alias) {
        selectColumns.add(columnOrExpr + (alias != null ? " AS " + alias : ""));
        return this;
    }

    public OuterQueryBuilder orderBY(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public OuterQueryBuilder limit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public OuterQueryBuilder offset(Integer offset) {
        this.offset = offset;
        return this;
    }

    public OuterQueryBuilder limitBasedOnCondition(Boolean condition, int limitTrue, int limitFalse) {
        this.limit = condition ? limitTrue : limitFalse;
        return this;
    }

    public String build() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(selectColumns.isEmpty() ? "*" : String.join(", ", selectColumns));
        sql.append(" FROM (").append(innerQuery.build()).append(") ").append(outerAlias);

        if (orderBy != null) {
            sql.append(" ORDER BY ").append(orderBy);
        }

        // Oracle pagination
        if (offset != null && limit != null) {
            sql.append(" OFFSET ").append(offset).append(" ROWS FETCH NEXT ").append(limit).append(" ROWS ONLY");
        } else if (limit != null) {
            sql.append(" FETCH FIRST ").append(limit).append(" ROWS ONLY");
        } else if (offset != null) {
            sql.append(" OFFSET ").append(offset).append(" ROWS");
        }

        // Parameters order: inner first, then outer (no parameters added for pagination in Oracle)
        parameters.addAll(innerQuery.getParameters());

        return sql.toString();
    }

    public List<Object> getParameters() {
        return parameters;
    }
}
