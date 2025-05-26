package com;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AdvanceSQLBuilder {
    private String fromClause;
    private final List<String> selectColumns = new ArrayList<>();
    private final List<String> joins = new ArrayList<>();
    private final List<String> whereClauses = new ArrayList<>();
    private final List<String> conditionGroups = new ArrayList<>();
    private final List<Object> parameters = new ArrayList<>();
    private String orderBy;
    private Integer limit;
    private Integer offset;

    // For union queries
    private final List<String> unionQueries = new ArrayList<>();
    private final List<Object> unionParams = new ArrayList<>();
    private boolean useUnionAll = false;

    public AdvanceSQLBuilder(String fromClause) {
        this.fromClause = fromClause;
    }

    public AdvanceSQLBuilder select(String columnOrExpr, String alias) {
        if (alias != null && !alias.isEmpty()) {
            selectColumns.add(columnOrExpr + " AS " + alias);
        } else {
            selectColumns.add(columnOrExpr);
        }
        return this;
    }

    public AdvanceSQLBuilder selectCase(String alias, String... conditionsAndResults) {
        StringBuilder caseExpr = new StringBuilder("CASE ");
        for (int i = 0; i < conditionsAndResults.length - 1; i += 2) {
            caseExpr.append("WHEN ").append(conditionsAndResults[i])
                    .append(" THEN '").append(conditionsAndResults[i + 1]).append("' ");
        }
        if (conditionsAndResults.length % 2 == 1) {
            caseExpr.append("ELSE '").append(conditionsAndResults[conditionsAndResults.length - 1]).append("' ");
        }
        caseExpr.append("END");
        selectColumns.add(caseExpr + (alias != null ? " AS " + alias : ""));
        return this;
    }

    public AdvanceSQLBuilder selectNestedCase(String alias, String outerCondition, String innerCondition, String innerResult, String elseResult) {
        String caseExpr = String.format(
                "CASE WHEN %s THEN (CASE WHEN %s THEN '%s' ELSE '%s' END) ELSE '%s' END",
                outerCondition, innerCondition, innerResult, elseResult, elseResult
        );
        selectColumns.add(caseExpr + (alias != null ? " AS " + alias : ""));
        return this;
    }

    public AdvanceSQLBuilder join(String joinClause) {
        joins.add(joinClause);
        return this;
    }

    public AdvanceSQLBuilder where(String condition, Object... params) {
        if (condition != null && !condition.trim().isEmpty()) {
            whereClauses.add(condition);
            if (params != null) {
                parameters.addAll(Arrays.asList(params));
            }
        }
        return this;
    }

    public AdvanceSQLBuilder orderBY(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public AdvanceSQLBuilder limit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public AdvanceSQLBuilder offset(Integer offset) {
        this.offset = offset;
        return this;
    }

    public AdvanceSQLBuilder limitBasedOnCondition(Boolean condition, int limitTrue, int limitFalse) {
        this.limit = condition ? limitTrue : limitFalse;
        return this;
    }

    /**
     * Add another query to union with this query.
     * This builder will become a union query builder if this method is called.
     */
    public AdvanceSQLBuilder union(AdvanceSQLBuilder other) {
        if (this.unionQueries.isEmpty()) {
            this.unionQueries.add(this.buildSimple());
            this.unionParams.addAll(this.getParameters());
        }
        this.unionQueries.add(other.buildSimple());
        this.unionParams.addAll(other.getParameters());
        this.useUnionAll = false;
        return this;
    }
    public AdvanceSQLBuilder andWhere(String condition, Object... params) {
        if (condition != null && !condition.trim().isEmpty()) {
            conditionGroups.add("(" + condition + ")");
            if (params != null) {
                parameters.addAll(Arrays.asList(params));
            }
        }
        return this;
    }

    public AdvanceSQLBuilder orWhereGroup(String... conditions) {
        List<String> group = new ArrayList<>();
        for (String condition : conditions) {
            if (condition != null && !condition.trim().isEmpty()) {
                group.add(condition);
            }
        }
        if (!group.isEmpty()) {
            conditionGroups.add("(" + String.join(" OR ", group) + ")");
        }
        return this;
    }
    public AdvanceSQLBuilder unionAll(AdvanceSQLBuilder other) {
        if (this.unionQueries.isEmpty()) {
            this.unionQueries.add(this.buildSimple());
            this.unionParams.addAll(this.getParameters());
        }
        this.unionQueries.add(other.buildSimple());
        this.unionParams.addAll(other.getParameters());
        this.useUnionAll = true;
        return this;
    }

    private String buildSimple() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(selectColumns.isEmpty() ? "*" : String.join(", ", selectColumns));
        sql.append(" FROM ").append(fromClause);
        for (String join : joins) {
            sql.append(" ").append(join);
        }
        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
        }

        if (orderBy != null) {
            sql.append(" ORDER BY ").append(orderBy);
        }
        return sql.toString();
    }




    public String build() {
        // If unionQueries are present, build union SQL
        if (!unionQueries.isEmpty()) {
            String unionType = useUnionAll ? " UNION ALL " : " UNION ";
            return unionQueries.stream().collect(Collectors.joining(unionType));
        }

        // Normal single query build with Oracle pagination
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(selectColumns.isEmpty() ? "*" : String.join(", ", selectColumns));
        sql.append(" FROM ").append(fromClause);
        for (String join : joins) {
            sql.append(" ").append(join);
        }
        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
        }
        if (!conditionGroups.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditionGroups));
        }
        if (orderBy != null) {
            sql.append(" ORDER BY ").append(orderBy);
        }

        // Oracle pagination using OFFSET/FETCH
        if (offset != null && limit != null) {
            sql.append(" OFFSET ").append(offset).append(" ROWS FETCH NEXT ").append(limit).append(" ROWS ONLY");
        } else if (limit != null) {
            sql.append(" FETCH FIRST ").append(limit).append(" ROWS ONLY");
        } else if (offset != null) {
            // Oracle requires FETCH if OFFSET is specified, so default FETCH ALL ROWS
            sql.append(" OFFSET ").append(offset).append(" ROWS");
        }

        return sql.toString();
    }

    public List<Object> getParameters() {
        if (!unionQueries.isEmpty()) {
            return unionParams;
        }
        return parameters;
    }
}
