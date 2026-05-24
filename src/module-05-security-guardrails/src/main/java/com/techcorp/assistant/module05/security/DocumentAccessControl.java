package com.techcorp.assistant.module05.security;

import com.techcorp.assistant.module05.service.SimpleRAGService.RetrievedDocument;
import com.techcorp.assistant.module05.service.SimpleRAGService.DocumentMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentAccessControl {

    private static final Logger log = LoggerFactory.getLogger(DocumentAccessControl.class);

    public List<RetrievedDocument> filterByPermissions(
            List<RetrievedDocument> documents,
            List<String> userRoles,
            String userDepartment) {

        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        List<RetrievedDocument> filtered = documents.stream()
                .filter(doc -> hasAccess(doc, userRoles, userDepartment))
                .collect(Collectors.toList());

        int filteredCount = documents.size() - filtered.size();
        if (filteredCount > 0) {
            log.info("Filtered {} documents based on access control", filteredCount);
        }

        return filtered;
    }

    public boolean hasAccess(RetrievedDocument document, List<String> userRoles, String userDepartment) {
        DocumentMetadata metadata = document.metadata();

        if (metadata == null) {
            // No restrictions - allow access
            return true;
        }

        // Check required role
        String requiredRole = metadata.requiredRole();
        if (requiredRole != null && !requiredRole.isBlank()) {
            if (userRoles == null || !userRoles.contains(requiredRole)) {
                log.debug("Access denied to document {} - missing required role: {}",
                        document.id(), requiredRole);
                return false;
            }
        }

        // Check department
        String docDepartment = metadata.department();
        if (docDepartment != null && !docDepartment.isBlank()) {
            if (userDepartment == null || !docDepartment.equals(userDepartment)) {
                log.debug("Access denied to document {} - department mismatch: required={}, user={}",
                        document.id(), docDepartment, userDepartment);
                return false;
            }
        }

        return true;
    }

    public RetrievedDocument enrichWithACL(RetrievedDocument document, String requiredRole, String department) {
        DocumentMetadata enrichedMetadata = new DocumentMetadata(department, requiredRole);
        return new RetrievedDocument(
                document.id(),
                document.content(),
                document.score(),
                enrichedMetadata
        );
    }
}
