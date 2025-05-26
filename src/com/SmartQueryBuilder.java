package com;

public class SmartQueryBuilder {
    public static void main(String[] args) {
        String sql = buildUnionStatement(true);
        System.out.println("Oracle SQL with UNION:\n" + sql);
    }

    public static String buildUnionStatement(boolean fetchTop100) {
                    AdvanceSQLBuilder builder = new AdvanceSQLBuilder("EMPLOYEE e")
                    .select("e.id", "EmployeeId")
                    .select("e.name", "Name")
                    .selectCase("Status",
                            "e.status = 'A'", "Active",
                            "e.status = 'I'", "Inactive",
                            "Unknown")
                    .selectNestedCase("SeniorityLevel",
                            "e.experience > 5", "e.role = 'Manager'", "Senior Manager", "Experienced")
                    .andWhere("e.department = ?", "IT")
                    .orWhereGroup("e.city = 'New York'", "e.city = 'Chicago'")
                    .orderBY("e.name")
                    .limit(10);


            AdvanceSQLBuilder query2 = new AdvanceSQLBuilder("EMPLOYEE e")
                .select("e.id", "id")
                .select("e.name", "name")
                .where("e.status = ?", "inactive")
                .orderBY("e.name DESC");

        // Union all example
        query1.unionAll(query2);

        OuterQueryBuilder outer = new OuterQueryBuilder(query1)
                .selectFromInner("id", "RollNo")
                .selectFromInner("name", "FirstName")
                .selectStatic("'Sidhant Gupta'", "TeacherName")
                .selectStatic("CASE WHEN inner_table.id IS NULL THEN 'NO ID' ELSE 'IS ID' END", "Status")
                .orderBY("RollNo ASC")
                .limitBasedOnCondition(fetchTop100, 100, 2000);

        return outer.build();
    }
}
