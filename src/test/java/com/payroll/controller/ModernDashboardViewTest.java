package com.payroll.controller;

import com.payroll.dao.ModernDashboardDao;
import com.payroll.model.ModernDashboardData;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ModernDashboardViewTest {

    @BeforeAll
    public static void initJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void testModernDashboardFxmlLoadsSuccessfully() {
        assertDoesNotThrow(() -> {
            URL fxmlUrl = getClass().getResource("/fxml/modern-dashboard-view.fxml");
            assertNotNull(fxmlUrl, "FXML resource /fxml/modern-dashboard-view.fxml must exist");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Object root = loader.load();
            assertNotNull(root, "Root node of modern dashboard must not be null");
        });
    }

    @Test
    public void testModernDashboardDaoQueries() {
        ModernDashboardDao dao = new ModernDashboardDao();
        ModernDashboardData data = dao.loadDashboardData("May", 2020);
        assertNotNull(data);
        assertEquals(121, data.getTotalEmployees(), "Total employees should match 121");
        assertNotNull(data.getSalaryPerMonth(), "Salary per month should not be null");
        assertNotNull(data.getProvidentFund(), "Provident fund should not be null");
        assertFalse(data.getGradeStats().isEmpty(), "Grade stats should be populated");
        assertEquals(9, data.getGradeStats().size(), "There should be 9 grades");
        assertFalse(data.getLeaveTrends().isEmpty(), "Leave trends should be populated");
    }
}
