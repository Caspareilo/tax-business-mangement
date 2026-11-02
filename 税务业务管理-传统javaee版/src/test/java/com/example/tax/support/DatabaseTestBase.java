package com.example.tax.support;

import com.example.tax.security.SessionUser;
import com.example.tax.service.AuthService;
import org.junit.jupiter.api.BeforeEach;

import java.sql.SQLException;

public abstract class DatabaseTestBase {

    @BeforeEach
    void resetDatabase() {
        TestDatabase.reset();
    }

    protected SessionUser login(String username, String password) throws SQLException {
        return new AuthService().login(username, password, "test-trace");
    }

    protected SessionUser admin() throws SQLException {
        return login("admin", "Admin@123");
    }

    protected SessionUser clerk() throws SQLException {
        return login("clerk1", "Clerk@123");
    }

    protected SessionUser reviewer() throws SQLException {
        return login("reviewer1", "Reviewer@123");
    }

    protected SessionUser analyst() throws SQLException {
        return login("analyst1", "Analyst@123");
    }

    protected SessionUser viewer() throws SQLException {
        return login("viewer1", "Viewer@123");
    }
}
