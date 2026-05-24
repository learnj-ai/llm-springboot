## ADDED Requirements

### Requirement: Database tool creation
The system SHALL provide database query tools annotated with @Tool that allow the LLM to retrieve customer and ticket information from PostgreSQL.

#### Scenario: Retrieve customer information by ID
- **WHEN** the LLM invokes `getCustomerInfo` tool with a valid customer ID
- **THEN** the system returns customer name, email, and subscription plan

#### Scenario: Search support tickets by status
- **WHEN** the LLM invokes `searchTickets` tool with status "open", "closed", or "pending"
- **THEN** the system returns up to 10 matching tickets with ID, subject, and creation date

#### Scenario: Handle non-existent customer
- **WHEN** the LLM invokes `getCustomerInfo` with an invalid customer ID
- **THEN** the system returns "Customer not found" without throwing an exception

### Requirement: External API tool creation
The system SHALL provide REST API tools that allow the LLM to fetch real-time data from third-party services.

#### Scenario: Fetch current weather for a city
- **WHEN** the LLM invokes `getCurrentWeather` tool with a city name
- **THEN** the system calls OpenWeatherMap API and returns weather description and temperature in Celsius

#### Scenario: Handle API failure gracefully
- **WHEN** the external API is unavailable or returns an error
- **THEN** the system returns a user-friendly error message without exposing API details

### Requirement: MCP server configuration
The system SHALL configure the ChatLanguageModel with registered tools following Model Context Protocol standards.

#### Scenario: Register multiple tools with LLM
- **WHEN** the application starts
- **THEN** the ChatLanguageModel bean is configured with all @Tool-annotated beans (CustomerDataTool, WeatherTool)

#### Scenario: Tool metadata is discoverable
- **WHEN** the LLM needs to select a tool
- **THEN** each tool's description and parameter annotations are available for selection

### Requirement: Tool orchestration
The system SHALL orchestrate multi-turn conversations where the LLM can request tool execution, receive results, and generate final responses.

#### Scenario: Single tool execution flow
- **WHEN** user asks "What's the weather in Boston?"
- **THEN** the LLM requests WeatherTool execution, receives the result, and generates a natural language response

#### Scenario: Multi-tool execution flow
- **WHEN** user asks a question requiring multiple tools
- **THEN** the system executes each tool request sequentially and combines results in the final response

#### Scenario: No tool needed
- **WHEN** user asks a question that doesn't require external data
- **THEN** the LLM generates a direct response without invoking any tools

### Requirement: REST API for chat with tools
The system SHALL expose a `/api/v1/assistant/chat` endpoint that accepts user messages and returns AI responses generated with tool support.

#### Scenario: Successful chat request
- **WHEN** POST request to `/api/v1/assistant/chat` with valid message
- **THEN** returns HTTP 200 with AI response (potentially tool-augmented)

#### Scenario: Chat request triggers tool usage
- **WHEN** user message requires database lookup (e.g., "What's customer 12345's email?")
- **THEN** the response includes data retrieved via CustomerDataTool
