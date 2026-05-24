## ADDED Requirements

### Requirement: Prompt injection detection
The system SHALL detect and reject user inputs containing prompt injection patterns before they reach the LLM.

#### Scenario: Detect ignore instructions pattern
- **WHEN** user input contains "ignore previous instructions" or "ignore all prompts"
- **THEN** validation returns rejected with reason "Potential prompt injection detected"

#### Scenario: Detect system override pattern
- **WHEN** user input contains "system: override" or "[INST]...[/INST]"
- **THEN** validation returns rejected with reason matching the detected pattern

#### Scenario: Detect role manipulation
- **WHEN** user input contains "you are now" attempting role manipulation
- **THEN** validation returns rejected

#### Scenario: Detect excessive special characters
- **WHEN** special characters exceed 30% of input length
- **THEN** validation returns rejected with reason "Excessive special characters detected"

#### Scenario: Approve benign input
- **WHEN** user input contains no injection patterns
- **THEN** validation returns approved

### Requirement: Input sanitization
The system SHALL sanitize user inputs by removing potentially malicious content while preserving legitimate text.

#### Scenario: Remove HTML/XML tags
- **WHEN** input contains `<script>` or other HTML tags
- **THEN** sanitized output removes all content within angle brackets

#### Scenario: Normalize whitespace
- **WHEN** input contains multiple consecutive spaces or tabs
- **THEN** sanitized output replaces them with single spaces

### Requirement: PII masking
The system SHALL detect and mask personally identifiable information (email, phone, SSN, credit card) in both inputs and outputs.

#### Scenario: Mask email addresses
- **WHEN** text contains email addresses (e.g., "user@example.com")
- **THEN** masked text replaces them with "[EMAIL_REDACTED]"

#### Scenario: Mask phone numbers
- **WHEN** text contains phone numbers in format XXX-XXX-XXXX or similar
- **THEN** masked text replaces them with "[PHONE_REDACTED]"

#### Scenario: Mask social security numbers
- **WHEN** text contains SSN in format XXX-XX-XXXX
- **THEN** masked text replaces them with "[SSN_REDACTED]"

#### Scenario: Mask credit card numbers
- **WHEN** text contains 16-digit card numbers
- **THEN** masked text replaces them with "[CARD_REDACTED]"

### Requirement: PII detection reporting
The system SHALL detect PII and report all matches with type and position without masking.

#### Scenario: Detect multiple PII types
- **WHEN** text contains email and phone number
- **THEN** detection result includes PIIMatch objects for both with type, value, and position

#### Scenario: No PII detected
- **WHEN** text contains no PII patterns
- **THEN** detection result returns containsPII=false with empty matches list

### Requirement: LLM-based output validation
The system SHALL use a fast LLM (gpt-3.5-turbo) as a judge to validate AI outputs for safety, toxicity, and factuality.

#### Scenario: Validate safe output
- **WHEN** AI response contains appropriate, professional content
- **THEN** validation returns safe=true with high confidence

#### Scenario: Detect toxic content
- **WHEN** AI response contains harmful or toxic language
- **THEN** validation returns safe=false with violations listed

#### Scenario: Detect confidential information disclosure
- **WHEN** AI response attempts to disclose confidential data
- **THEN** validation returns safe=false with "disclosure of confidential information" violation

### Requirement: Hallucination detection
The system SHALL detect when AI outputs contain information not present in source documents.

#### Scenario: Detect hallucinated information
- **WHEN** AI response includes claims not found in source documents
- **THEN** containsHallucination returns true

#### Scenario: Verify grounded response
- **WHEN** AI response only uses information from source documents
- **THEN** containsHallucination returns false

### Requirement: Document access control
The system SHALL filter retrieved documents based on user roles and department before passing them to the LLM.

#### Scenario: Filter by required role
- **WHEN** document metadata specifies required_role="admin" and user lacks that role
- **THEN** document is excluded from results

#### Scenario: Filter by department
- **WHEN** document metadata specifies department="engineering" and user is in "sales"
- **THEN** document is excluded from results

#### Scenario: Allow access when requirements met
- **WHEN** user has required role and matching department
- **THEN** document is included in results

### Requirement: Security audit logging
The system SHALL log all security events to application logs and Redis with severity-based alerting.

#### Scenario: Log security event
- **WHEN** a security event occurs (prompt injection, unsafe output, etc.)
- **THEN** event is logged with type, severity, user ID, timestamp, and details

#### Scenario: Store in Redis for monitoring
- **WHEN** security event is logged
- **THEN** AuditEvent is pushed to Redis list "security-events"

#### Scenario: Alert on high severity
- **WHEN** security event has severity HIGH or CRITICAL
- **THEN** alert is sent (logged as ERROR) in addition to normal logging

#### Scenario: Retrieve recent events
- **WHEN** monitoring system queries recent events
- **THEN** system returns last N events from Redis

### Requirement: Secure RAG endpoint
The system SHALL expose `/api/v1/secure/query` that applies all security layers (injection defense, PII masking, output validation, access control) in sequence.

#### Scenario: Reject prompt injection
- **WHEN** POST to `/api/v1/secure/query` with injection attempt
- **THEN** returns HTTP 400, logs security event with HIGH severity

#### Scenario: Block unsafe output
- **WHEN** RAG generates response that fails validation
- **THEN** returns generic safety message, logs MEDIUM severity event

#### Scenario: Successful secure query
- **WHEN** valid query passes all checks
- **THEN** returns HTTP 200 with PII-masked response
