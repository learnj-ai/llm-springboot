package com.techcorp.assistant.module05.security;

import com.techcorp.assistant.module05.service.SimpleRAGService.DocumentMetadata;
import com.techcorp.assistant.module05.service.SimpleRAGService.RetrievedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentAccessControlTest {

    private DocumentAccessControl accessControl;

    @BeforeEach
    void setUp() {
        accessControl = new DocumentAccessControl();
    }

    @Test
    void testFilterByRequiredRole() {
        List<RetrievedDocument> documents = List.of(
                new RetrievedDocument("doc1", "Public content", 0.9, new DocumentMetadata(null, null)),
                new RetrievedDocument("doc2", "Admin content", 0.8, new DocumentMetadata(null, "admin")),
                new RetrievedDocument("doc3", "User content", 0.7, new DocumentMetadata(null, "user"))
        );

        // User without admin role
        List<RetrievedDocument> filtered = accessControl.filterByPermissions(
                documents,
                List.of("user"),
                null
        );

        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().noneMatch(doc -> doc.id().equals("doc2")));
    }

    @Test
    void testFilterByDepartment() {
        List<RetrievedDocument> documents = List.of(
                new RetrievedDocument("doc1", "Engineering doc", 0.9, new DocumentMetadata("engineering", null)),
                new RetrievedDocument("doc2", "Sales doc", 0.8, new DocumentMetadata("sales", null)),
                new RetrievedDocument("doc3", "General doc", 0.7, new DocumentMetadata(null, null))
        );

        // User in engineering department
        List<RetrievedDocument> filtered = accessControl.filterByPermissions(
                documents,
                List.of(),
                "engineering"
        );

        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().anyMatch(doc -> doc.id().equals("doc1")));
        assertTrue(filtered.stream().anyMatch(doc -> doc.id().equals("doc3")));
        assertFalse(filtered.stream().anyMatch(doc -> doc.id().equals("doc2")));
    }

    @Test
    void testAllowAccessWhenRequirementsMet() {
        RetrievedDocument document = new RetrievedDocument(
                "doc1",
                "Restricted content",
                0.9,
                new DocumentMetadata("engineering", "admin")
        );

        // User with admin role in engineering department
        boolean hasAccess = accessControl.hasAccess(
                document,
                List.of("admin", "user"),
                "engineering"
        );

        assertTrue(hasAccess);
    }

    @Test
    void testDenyAccessMissingRole() {
        RetrievedDocument document = new RetrievedDocument(
                "doc1",
                "Admin only",
                0.9,
                new DocumentMetadata(null, "admin")
        );

        boolean hasAccess = accessControl.hasAccess(
                document,
                List.of("user"),
                null
        );

        assertFalse(hasAccess);
    }

    @Test
    void testDenyAccessWrongDepartment() {
        RetrievedDocument document = new RetrievedDocument(
                "doc1",
                "Engineering only",
                0.9,
                new DocumentMetadata("engineering", null)
        );

        boolean hasAccess = accessControl.hasAccess(
                document,
                List.of(),
                "sales"
        );

        assertFalse(hasAccess);
    }

    @Test
    void testAllowAccessNoRestrictions() {
        RetrievedDocument document = new RetrievedDocument(
                "doc1",
                "Public content",
                0.9,
                new DocumentMetadata(null, null)
        );

        boolean hasAccess = accessControl.hasAccess(
                document,
                List.of(),
                null
        );

        assertTrue(hasAccess);
    }

    @Test
    void testEnrichWithACL() {
        RetrievedDocument original = new RetrievedDocument(
                "doc1",
                "Content",
                0.9,
                new DocumentMetadata(null, null)
        );

        RetrievedDocument enriched = accessControl.enrichWithACL(
                original,
                "admin",
                "engineering"
        );

        assertEquals("doc1", enriched.id());
        assertEquals("Content", enriched.content());
        assertEquals(0.9, enriched.score());
        assertEquals("engineering", enriched.metadata().department());
        assertEquals("admin", enriched.metadata().requiredRole());
    }
}
