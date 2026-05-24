package com.techcorp.assistant.module03.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CustomerDataTool.
 */
class CustomerDataToolTest {

    private CustomerDataTool customerDataTool;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Create in-memory H2 database for testing.
        // `generateUniqueName(true)` gives every test a fresh DB; without it,
        // EmbeddedDatabaseBuilder reuses the default "testdb" name across tests
        // in the same JVM and the second test fails with
        // "Table CUSTOMERS already exists".
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .addScript("classpath:test-schema.sql")
                .addScript("classpath:test-data.sql")
                .build();

        jdbcTemplate = new JdbcTemplate(dataSource);
        customerDataTool = new CustomerDataTool(jdbcTemplate);
    }

    @Test
    void testGetCustomerInfo_ValidCustomer() {
        // When
        String result = customerDataTool.getCustomerInfo("12345");

        // Then
        assertThat(result).contains("Alice Johnson");
        assertThat(result).contains("alice.johnson@example.com");
        assertThat(result).contains("premium");
    }

    @Test
    void testGetCustomerInfo_NonExistentCustomer() {
        // When
        String result = customerDataTool.getCustomerInfo("99999");

        // Then
        assertThat(result).contains("Customer not found");
    }

    @Test
    void testSearchTickets_OpenStatus() {
        // When
        String result = customerDataTool.searchTickets("open");

        // Then
        assertThat(result).contains("open ticket");
        assertThat(result).doesNotContain("closed");
    }

    @Test
    void testSearchTickets_InvalidStatus() {
        // When
        String result = customerDataTool.searchTickets("invalid");

        // Then
        assertThat(result).contains("Invalid status");
    }

    @Test
    void testSearchTickets_CaseInsensitive() {
        // When
        String result = customerDataTool.searchTickets("PENDING");

        // Then
        assertThat(result).contains("pending ticket");
    }
}
