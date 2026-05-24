package com.techcorp.assistant.module04.agent;

import com.techcorp.assistant.module04.tool.CustomerDataTool;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Component;

/**
 * Specialized agent for customer support queries.
 *
 * Handles questions about customers, accounts, and support tickets.
 */
@Component
public class CustomerSupportAgent implements SpecializedAgent {

    private final ChatModel chatModel;
    private final CustomerDataTool customerDataTool;

    public CustomerSupportAgent(
            ChatModel chatModel,
            CustomerDataTool customerDataTool) {
        this.chatModel = chatModel;
        this.customerDataTool = customerDataTool;
    }

    @Override
    public String process(String request) {
        String prompt = String.format("""
                You are a customer support specialist. Answer questions about:
                - Customer accounts and information
                - Support tickets and their status
                - Account issues and troubleshooting

                Available tools:
                - Look up customer information by ID
                - Search support tickets by status

                User question: %s

                Provide a helpful and professional response.
                """, request);

        return chatModel.chat(prompt);
    }

    @Override
    public String getName() {
        return "CustomerSupportAgent";
    }

    @Override
    public String getDescription() {
        return "Handles customer account queries, support tickets, and account-related issues";
    }
}
