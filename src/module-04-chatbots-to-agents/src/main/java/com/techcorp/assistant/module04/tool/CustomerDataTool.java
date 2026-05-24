package com.techcorp.assistant.module04.tool;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Database tool providing customer and support ticket information.
 * Uses @Tool annotation to make methods available to the LLM.
 */
@Component
public class CustomerDataTool {
    private static final Logger log = LoggerFactory.getLogger(CustomerDataTool.class);
    private final JdbcTemplate jdbcTemplate;

    public CustomerDataTool(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Retrieves customer information by ID.
     *
     * @param customerId The customer ID to look up
     * @return Customer information including name, email, and subscription plan
     */
    @Tool("Retrieves customer information by customer ID including name, email, and subscription plan")
    public String getCustomerInfo(@P("The customer ID to retrieve information for") String customerId) {
        log.debug("Tool invoked: getCustomerInfo({})", customerId);

        try {
            String sql = """
                SELECT customer_id, name, email, subscription_plan, created_at
                FROM customers
                WHERE customer_id = ?
                """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, customerId);

            if (results.isEmpty()) {
                return "Customer not found: No customer exists with ID " + customerId;
            }

            Map<String, Object> customer = results.get(0);
            return String.format("""
                Customer Information:
                - ID: %s
                - Name: %s
                - Email: %s
                - Subscription Plan: %s
                - Member Since: %s
                """,
                customer.get("customer_id"),
                customer.get("name"),
                customer.get("email"),
                customer.get("subscription_plan"),
                customer.get("created_at")
            );
        } catch (Exception e) {
            log.error("Error retrieving customer info for ID: {}", customerId, e);
            return "Error retrieving customer information. Please try again later.";
        }
    }

    /**
     * Searches support tickets by status.
     *
     * @param status The ticket status to filter by (open, pending, or closed)
     * @return List of matching support tickets
     */
    @Tool("Searches support tickets by status. Valid statuses are: open, pending, closed")
    public String searchTickets(@P("The ticket status to search for (open, pending, or closed)") String status) {
        log.debug("Tool invoked: searchTickets({})", status);

        try {
            // Validate status parameter
            String normalizedStatus = status.toLowerCase().trim();
            if (!List.of("open", "pending", "closed").contains(normalizedStatus)) {
                return "Invalid status. Please use one of: open, pending, closed";
            }

            String sql = """
                SELECT t.ticket_id, t.customer_id, c.name as customer_name,
                       t.subject, t.status, t.created_at
                FROM support_tickets t
                JOIN customers c ON t.customer_id = c.customer_id
                WHERE t.status = ?
                ORDER BY t.created_at DESC
                LIMIT 10
                """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, normalizedStatus);

            if (results.isEmpty()) {
                return "No tickets found with status: " + normalizedStatus;
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("Found %d %s ticket(s):\n\n", results.size(), normalizedStatus));

            for (Map<String, Object> ticket : results) {
                response.append(String.format("""
                    Ticket #%s
                    - Customer: %s (ID: %s)
                    - Subject: %s
                    - Status: %s
                    - Created: %s

                    """,
                    ticket.get("ticket_id"),
                    ticket.get("customer_name"),
                    ticket.get("customer_id"),
                    ticket.get("subject"),
                    ticket.get("status"),
                    ticket.get("created_at")
                ));
            }

            return response.toString();
        } catch (Exception e) {
            log.error("Error searching tickets with status: {}", status, e);
            return "Error searching tickets. Please try again later.";
        }
    }
}
