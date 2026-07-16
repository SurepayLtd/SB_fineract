package org.apache.fineract.selfservice.testing.support;

import org.junit.jupiter.api.Test;
import java.sql.*;

public class DbProbeTest extends SelfServiceIntegrationTestBase {

    @Test
    public void testProbe() throws Exception {
        System.out.println("=== DB PROBE START ===");
        try (Connection conn = getJdbcConnection()) {
            System.out.println("CONNECTED TO DEFAULT DB! URL: " + conn.getMetaData().getURL());
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, name FROM m_office")) {
                System.out.println("Offices:");
                while (rs.next()) {
                    System.out.println(" - ID: " + rs.getInt("id") + ", Name: " + rs.getString("name"));
                }
            }
        } catch (Exception e) {
            System.err.println("Default DB Error: " + e.getMessage());
            e.printStackTrace();
        }

        // Also let's try connecting to fineract_tenants DB
        String tenantsUrl = System.getProperty("fineract.it.tenantsUrl", "jdbc:mariadb://localhost:3318/fineract_tenants");
        String user = System.getProperty("fineract.it.dbUsername", "root");
        String password = System.getProperty("fineract.it.dbPassword", "mysql");
        try (Connection conn = DriverManager.getConnection(tenantsUrl, user, password)) {
            System.out.println("CONNECTED TO TENANTS DB! URL: " + conn.getMetaData().getURL());
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, name, identifier FROM tenants")) {
                System.out.println("Tenants:");
                while (rs.next()) {
                    System.out.println(" - ID: " + rs.getInt("id") + ", Name: " + rs.getString("name") + ", Identifier: " + rs.getString("identifier"));
                }
            }
        } catch (Exception e) {
            System.err.println("Tenants DB Error: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("=== DB PROBE END ===");
    }
}
