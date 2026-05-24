package com.techcorp.assistant.module05.controller;

import com.techcorp.assistant.module05.security.*;
import com.techcorp.assistant.module05.service.SimpleRAGService;
import com.techcorp.assistant.module05.service.SimpleRAGService.RAGResponse;
import com.techcorp.assistant.module05.service.SimpleRAGService.RetrievedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/secure")
public class SecureRAGController {

    private static final Logger log = LoggerFactory.getLogger(SecureRAGController.class);

    private final PromptInjectionGuard promptInjectionGuard;
    private final PIIMaskingService piiMaskingService;
    private final OutputValidator outputValidator;
    private final DocumentAccessControl documentAccessControl;
    private final SecurityAuditService securityAuditService;
    private final SimpleRAGService ragService;

    public SecureRAGController(
            PromptInjectionGuard promptInjectionGuard,
            PIIMaskingService piiMaskingService,
            OutputValidator outputValidator,
            DocumentAccessControl documentAccessControl,
            SecurityAuditService securityAuditService,
            SimpleRAGService ragService) {
        this.promptInjectionGuard = promptInjectionGuard;
        this.piiMaskingService = piiMaskingService;
        this.outputValidator = outputValidator;
        this.documentAccessControl = documentAccessControl;
        this.securityAuditService = securityAuditService;
        this.ragService = ragService;
    }

    @PostMapping("/query")
    public ResponseEntity<SecureResponse> query(@RequestBody SecureRequest request) {
        String userId = request.userId() != null ? request.userId() : "anonymous";
        List<String> userRoles = request.userRoles() != null ? request.userRoles() : List.of();
        String department = request.department();

        log.info("Secure query from user: {}", userId);

        // Layer 1: Prompt injection detection
        PromptInjectionGuard.ValidationResult validationResult = promptInjectionGuard.validate(request.query());
        if (validationResult.isRejected()) {
            securityAuditService.logSecurityEvent(new SecurityAuditService.SecurityEvent(
                    "PROMPT_INJECTION",
                    SecurityAuditService.Severity.HIGH,
                    userId,
                    validationResult.reason()
            ));

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new SecureResponse(
                            "Request rejected for security reasons.",
                            false,
                            List.of(validationResult.reason())
                    ));
        }

        // Layer 2: Sanitize input
        String sanitizedQuery = promptInjectionGuard.sanitizeInput(request.query());

        // Layer 3: Mask PII in input
        String maskedQuery = piiMaskingService.maskPII(sanitizedQuery);

        securityAuditService.logSecurityEvent(new SecurityAuditService.SecurityEvent(
                "QUERY_PROCESSING",
                SecurityAuditService.Severity.LOW,
                userId,
                "Query processed through security layers"
        ));

        // Layer 4: Execute RAG with access control
        RAGResponse ragResponse = ragService.query(maskedQuery, userId, userRoles, department);

        // Filter documents by permissions
        List<RetrievedDocument> accessibleDocs = documentAccessControl.filterByPermissions(
                ragResponse.sourceDocuments(),
                userRoles,
                department
        );

        // Layer 5: Validate output
        OutputValidator.ValidationCriteria validation = outputValidator.validateOutput(ragResponse.response());

        if (!validation.safe()) {
            securityAuditService.logSecurityEvent(new SecurityAuditService.SecurityEvent(
                    "UNSAFE_OUTPUT",
                    SecurityAuditService.Severity.MEDIUM,
                    userId,
                    "Output failed safety validation: " + validation.violations()
            ));

            return ResponseEntity.ok(new SecureResponse(
                    "I apologize, but I cannot provide that information due to content safety policies.",
                    false,
                    validation.violations()
            ));
        }

        // Layer 6: Check for hallucinations
        List<String> sourceContents = accessibleDocs.stream()
                .map(RetrievedDocument::content)
                .collect(Collectors.toList());

        boolean hasHallucination = outputValidator.containsHallucination(ragResponse.response(), sourceContents);
        if (hasHallucination) {
            securityAuditService.logSecurityEvent(new SecurityAuditService.SecurityEvent(
                    "HALLUCINATION_DETECTED",
                    SecurityAuditService.Severity.MEDIUM,
                    userId,
                    "Response contains hallucinated information"
            ));

            return ResponseEntity.ok(new SecureResponse(
                    "I don't have enough reliable information to answer that question accurately.",
                    false,
                    List.of("Potential hallucination detected")
            ));
        }

        // Layer 7: Mask PII in output
        String finalResponse = piiMaskingService.maskPII(ragResponse.response());

        securityAuditService.logSecurityEvent(new SecurityAuditService.SecurityEvent(
                "QUERY_SUCCESS",
                SecurityAuditService.Severity.LOW,
                userId,
                "Query completed successfully"
        ));

        return ResponseEntity.ok(new SecureResponse(finalResponse, true, List.of()));
    }

    public record SecureRequest(
            String query,
            String userId,
            List<String> userRoles,
            String department
    ) {}

    public record SecureResponse(
            String response,
            boolean safe,
            List<String> securityIssues
    ) {}
}
