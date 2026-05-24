package com.techcorp.assistant.module06.dokimos;

import dev.dokimos.core.Dataset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DatasetLoader.
 */
@DisplayName("DatasetLoader Tests")
class DatasetLoaderTest {

    private DatasetLoader datasetLoader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        datasetLoader = new DatasetLoader();
    }

    @Test
    @DisplayName("Should load valid dataset successfully")
    void testLoadValidDataset() throws Exception {
        // Create temporary dataset file
        String datasetJson = """
                {
                  "name": "Test Dataset",
                  "description": "Test dataset for unit testing",
                  "examples": [
                    {
                      "inputs": {"input": "Test query"},
                      "expectedOutputs": {"output": "Test answer"},
                      "metadata": {"id": "test-001"}
                    }
                  ]
                }
                """;

        Path datasetPath = tempDir.resolve("test-dataset.json");
        Files.writeString(datasetPath, datasetJson);

        // Set dataset path via reflection
        ReflectionTestUtils.setField(datasetLoader, "datasetPath", datasetPath.toString());

        // Load dataset
        Dataset dataset = datasetLoader.loadDataset();

        // Verify dataset loaded
        assertNotNull(dataset);
        assertEquals("Test Dataset", dataset.name());
        assertEquals(1, dataset.examples().size());
    }

    @Test
    @DisplayName("Should throw exception when dataset file not found")
    void testDatasetFileNotFound() {
        // Set non-existent path
        ReflectionTestUtils.setField(datasetLoader, "datasetPath", "/nonexistent/path/dataset.json");

        // Verify exception thrown
        assertThrows(DatasetLoader.DatasetLoadException.class, () -> {
            datasetLoader.loadDataset();
        });
    }

    @Test
    @DisplayName("Should throw exception for malformed JSON")
    void testMalformedDatasetJson() throws IOException {
        // Create temporary file with invalid JSON
        Path datasetPath = tempDir.resolve("malformed.json");
        Files.writeString(datasetPath, "{invalid json}");

        // Set dataset path
        ReflectionTestUtils.setField(datasetLoader, "datasetPath", datasetPath.toString());

        // Verify exception thrown
        assertThrows(DatasetLoader.DatasetLoadException.class, () -> {
            datasetLoader.loadDataset();
        });
    }

    @Test
    @DisplayName("Should throw exception for unreadable file")
    void testUnreadableFile() throws IOException {
        // Create temporary file
        Path datasetPath = tempDir.resolve("unreadable.json");
        Files.writeString(datasetPath, "{}");

        // Make file unreadable
        datasetPath.toFile().setReadable(false);

        // Set dataset path
        ReflectionTestUtils.setField(datasetLoader, "datasetPath", datasetPath.toString());

        try {
            // Verify exception thrown
            assertThrows(DatasetLoader.DatasetLoadException.class, () -> {
                datasetLoader.loadDataset();
            });
        } finally {
            // Restore permissions for cleanup
            datasetPath.toFile().setReadable(true);
        }
    }
}
