# Oracle SQL Query Builder

A Java utility library for dynamically building complex Oracle SQL queries with support for:

* Nested queries with aliasing
* Conditional WHERE clauses with parameter binding
* UNION and UNION ALL for combining multiple queries
* Oracle 12c+ style pagination (`OFFSET ... ROWS FETCH NEXT ... ROWS ONLY`)
* Safe, ordered parameter management for prepared statements

---

## Project Structure

```
src/
 └── main/
      └── java/
           └── com/
                ├── AdvanceSQLBuilder.java    # Core builder for base SELECT queries with joins, conditions, unions, and pagination
                ├── OuterQueryBuilder.java    # Wraps inner queries, handles aliasing, static columns, and outer pagination
                └── SmartQueryBuilder.java    # Demo usage illustrating conditional limits, unions, and full query construction
README.md                               # This documentation file
```

---

## Classes Overview

### `AdvanceSQLBuilder.java`

* Constructs SQL SELECT statements with:

    * SELECT columns (with optional aliases)
    * FROM clause
    * JOINs
    * WHERE conditions and parameter binding
    * ORDER BY clauses
    * Oracle-specific pagination syntax
    * UNION and UNION ALL query combinations

### `OuterQueryBuilder.java`

* Wraps an existing `AdvanceSQLBuilder` query as a subquery aliased `inner_table`
* Select columns from the inner query with optional aliases
* Add static or computed columns (e.g., CASE statements)
* Outer-level ORDER BY, OFFSET, FETCH for pagination
* Aggregates parameters from both inner and outer queries

### `SmartQueryBuilder.java`

* Example class demonstrating typical usage patterns:

    * Building base queries with conditions
    * Combining queries via UNION ALL
    * Applying conditional limits (pagination)
    * Using outer queries with aliasing and static columns

---

## Use Cases

* Dynamically construct complex Oracle SQL queries based on application logic
* Create nested queries with clear aliasing and computed columns
* Efficiently paginate results using Oracle 12c+ syntax
* Safely manage query parameters for prepared statements
* Combine multiple queries with UNION / UNION ALL for advanced reporting

---

## Example Usage

```java
public class SmartQueryBuilder {
    public static void main(String[] args) {
        String sql = buildUnionQuery(true);
        System.out.println("Generated Oracle SQL:\n" + sql);
        System.out.println("Parameters: " + getParametersForQuery(true));
    }

    public static String buildUnionQuery(boolean fetchTop100) {
        AdvanceSQLBuilder activeEmployees = new AdvanceSQLBuilder("EMPLOYEE e")
                .select("e.id", "id")
                .select("e.name", "name")
                .andWhere("e.department = ?", "IT")
                .orWhereGroup("e.city = 'New York'", "e.city = 'Chicago'")
                .orderBY("e.name ASC");

        AdvanceSQLBuilder inactiveEmployees = new AdvanceSQLBuilder("EMPLOYEE e")
                .select("e.id", "id")
                .select("e.name", "name")
                .where("e.status = ?", "inactive")
                .orderBY("e.name DESC");

        activeEmployees.unionAll(inactiveEmployees);

        OuterQueryBuilder outerQuery = new OuterQueryBuilder(activeEmployees)
                .selectFromInner("id", "RollNo")
                .selectFromInner("name", "FirstName")
                .selectStatic("'Sidhant Gupta'", "TeacherName")
                .selectStatic("CASE WHEN inner_table.id IS NULL THEN 'NO ID' ELSE 'IS ID' END", "Status")
                .orderBY("RollNo ASC")
                .limitBasedOnCondition(fetchTop100, 100, 2000);

        return outerQuery.build();
    }

    public static List<Object> getParametersForQuery(boolean fetchTop100) {
        AdvanceSQLBuilder activeEmployees = new AdvanceSQLBuilder("EMPLOYEE e")
                .where("e.status = ?", "active")
                .where("e.department = ?", "HR");

        AdvanceSQLBuilder inactiveEmployees = new AdvanceSQLBuilder("EMPLOYEE e")
                .where("e.status = ?", "inactive");

        activeEmployees.unionAll(inactiveEmployees);

        OuterQueryBuilder outerQuery = new OuterQueryBuilder(activeEmployees)
                .limitBasedOnCondition(fetchTop100, 100, 2000);

        return outerQuery.getParameters();
    }
}
```

### Generated SQL Sample

```sql
SELECT 
  inner_table.id AS RollNo, 
  inner_table.name AS FirstName, 
  'Sidhant Gupta' AS TeacherName, 
  CASE WHEN inner_table.id IS NULL THEN 'NO ID' ELSE 'IS ID' END AS Status
FROM (
  SELECT e.id AS id, e.name AS name FROM EMPLOYEE e WHERE (e.department = ?) AND (e.city = 'New York' OR e.city = 'Chicago')
  ORDER BY e.name ASC
  UNION ALL
  SELECT e.id AS id, e.name AS name FROM EMPLOYEE e WHERE e.status = ? ORDER BY e.name DESC
) inner_table
ORDER BY RollNo ASC
OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY
```

### Parameters

```
["active", "HR", "inactive"]
```

---

## Notes

* Oracle pagination uses `OFFSET ... ROWS FETCH NEXT ... ROWS ONLY` instead of `LIMIT`.
* Parameters are stored and retrieved in order; use `PreparedStatement` with these parameters.
* Union and Union All support allows combining multiple `AdvanceSQLBuilder` queries seamlessly.
* Static columns can be constants or SQL expressions added to outer query via `selectStatic()`.

---

## Future Scope

### 1. Multi-Database Support with Factory Pattern

* **Goal:** Abstract database-specific SQL generation for Oracle, MySQL, PostgreSQL, SQL Server, etc.
* **How:** Define a `SQLBuilderFactory` interface returning DB-specific builder implementations:

    * `OracleSQLBuilder` (current builder)
    * `MySQLSQLBuilder` (with MySQL-specific pagination syntax)
    * `PostgresSQLBuilder` and so on
* This design enables runtime selection of builder based on the database type, enhancing reusability and
  maintainability.

### 2. Extended SQL Features

* Support for `GROUP BY`, `HAVING`, and aggregation functions
* Support for DML operations (`INSERT`, `UPDATE`, `DELETE`)
* Support for Common Table Expressions (CTEs)
* More flexible JOIN types (LEFT, RIGHT, FULL OUTER)
* Query hints and optimizer directives

### 3. Improved Parameter Handling

* Named parameters support (for better readability and debugging)
* Parameter type validation and conversions

### 4. Integration and Usability

* Integration with JDBC templates and ORM frameworks
* Utilities for executing built queries with automatic resource management
* Query formatting and pretty-printing for debugging

---

## How to Use

1. Add the Java classes under package `com` to your project.
2. Use `AdvanceSQLBuilder` to create base queries and combine with unions.
3. Wrap queries with `OuterQueryBuilder` for aliasing, static columns, and pagination.
4. Call `build()` to generate the SQL string, and `getParameters()` to retrieve parameters list.
5. Use these with your JDBC `PreparedStatement` to execute safely.

## 🧑‍💻 Author

**Sidhant Gupta**

Email: [guptasidhant1997@gmail.com](mailto:guptasidhant1997@gmail.com)

LinkedIn: [https://www.linkedin.com/in/gupta-sidhant/](https://www.linkedin.com/in/gupta-sidhant/)
