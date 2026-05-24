package com.techcorp.assistant.module04.service;

import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Decomposes complex tasks into subtasks and executes them in dependency order.
 *
 * Uses LLM to generate subtasks with dependencies, then executes them
 * respecting the dependency graph.
 */
@Service
public class TaskDecomposer {
    private static final Logger log = LoggerFactory.getLogger(TaskDecomposer.class);

    private final ChatModel chatModel;

    public TaskDecomposer(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Executes a complex task by decomposing it into subtasks.
     *
     * @param complexTask The complex task description
     * @return TaskExecutionResult with all subtask results and summary
     */
    public TaskExecutionResult executeComplexTask(String complexTask) {
        log.info("Decomposing complex task: {}", complexTask);

        // Step 1: Decompose into subtasks
        List<Subtask> subtasks = decompose(complexTask);

        if (subtasks.isEmpty()) {
            return new TaskExecutionResult(
                    complexTask,
                    List.of(),
                    "Unable to decompose task into subtasks"
            );
        }

        log.info("Decomposed into {} subtasks", subtasks.size());

        // Step 2: Execute subtasks in dependency order
        List<SubtaskResult> results = new ArrayList<>();
        Map<String, String> completedResults = new HashMap<>();

        // Build execution order
        List<Subtask> executionOrder = topologicalSort(subtasks);

        for (Subtask subtask : executionOrder) {
            log.debug("Executing subtask {}: {}", subtask.id(), subtask.description());

            // Check dependencies are met
            boolean dependenciesMet = subtask.dependencies().stream()
                    .allMatch(completedResults::containsKey);

            if (!dependenciesMet) {
                String result = "Skipped: dependencies not met";
                results.add(new SubtaskResult(subtask.id(), subtask.description(), result, false));
                continue;
            }

            // Execute subtask
            String subtaskPrompt = buildSubtaskPrompt(subtask, completedResults);
            String result = chatModel.chat(subtaskPrompt);

            completedResults.put(subtask.id(), result);
            results.add(new SubtaskResult(subtask.id(), subtask.description(), result, true));

            log.debug("Completed subtask {}", subtask.id());
        }

        // Step 3: Synthesize results
        String summary = synthesizeResults(complexTask, results);

        return new TaskExecutionResult(complexTask, results, summary);
    }

    /**
     * Decomposes a task into 3-5 subtasks using LLM.
     *
     * @param task The task to decompose
     * @return List of subtasks with dependencies
     */
    private List<Subtask> decompose(String task) {
        String decompositionPrompt = String.format("""
                Decompose this complex task into 3-5 sequential subtasks.

                Task: %s

                For each subtask, provide:
                - id: A unique identifier (e.g., "task1", "task2")
                - description: What needs to be done
                - dependencies: List of task IDs this depends on (empty if no dependencies)

                Format your response as:
                SUBTASK id=task1 deps=[]
                description: [description text]

                SUBTASK id=task2 deps=[task1]
                description: [description text]

                Keep it to 3-5 subtasks maximum.
                """, task);

        String response = chatModel.chat(decompositionPrompt);

        return parseSubtasks(response);
    }

    /**
     * Parses subtasks from LLM response.
     */
    private List<Subtask> parseSubtasks(String response) {
        List<Subtask> subtasks = new ArrayList<>();

        String[] blocks = response.split("SUBTASK");

        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;

            try {
                // Parse id
                String id = extractPattern(block, "id=(\\w+)");

                // Parse dependencies
                String depsStr = extractPattern(block, "deps=\\[([^\\]]*)\\]");
                List<String> deps = depsStr.isEmpty()
                        ? List.of()
                        : Arrays.stream(depsStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());

                // Parse description
                String description = extractPattern(block, "description:\\s*(.+?)(?=SUBTASK|$)");

                if (!id.isEmpty() && !description.isEmpty()) {
                    subtasks.add(new Subtask(id, description.trim(), deps));
                }
            } catch (Exception e) {
                log.warn("Failed to parse subtask block: {}", block, e);
            }
        }

        return subtasks;
    }

    /**
     * Extracts text matching a regex pattern.
     */
    private String extractPattern(String text, String pattern) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : "";
    }

    /**
     * Topological sort of subtasks based on dependencies.
     */
    private List<Subtask> topologicalSort(List<Subtask> subtasks) {
        List<Subtask> sorted = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (Subtask task : subtasks) {
            visit(task, subtasks, visited, sorted);
        }

        return sorted;
    }

    private void visit(Subtask task, List<Subtask> all, Set<String> visited, List<Subtask> sorted) {
        if (visited.contains(task.id())) return;

        // Visit dependencies first
        for (String depId : task.dependencies()) {
            all.stream()
                    .filter(t -> t.id().equals(depId))
                    .findFirst()
                    .ifPresent(dep -> visit(dep, all, visited, sorted));
        }

        visited.add(task.id());
        sorted.add(task);
    }

    /**
     * Builds prompt for subtask execution including dependency results.
     */
    private String buildSubtaskPrompt(Subtask subtask, Map<String, String> completedResults) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Execute this subtask:\n\n");
        prompt.append(subtask.description()).append("\n\n");

        if (!subtask.dependencies().isEmpty()) {
            prompt.append("Previous results from dependencies:\n");
            for (String depId : subtask.dependencies()) {
                String result = completedResults.get(depId);
                if (result != null) {
                    prompt.append(String.format("- %s: %s\n", depId, result));
                }
            }
        }

        prompt.append("\nProvide a concise result for this subtask.");
        return prompt.toString();
    }

    /**
     * Synthesizes subtask results into a final summary.
     */
    private String synthesizeResults(String originalTask, List<SubtaskResult> results) {
        String synthesisPrompt = String.format("""
                Original task: %s

                Subtask results:
                %s

                Provide a comprehensive summary combining all subtask results.
                """,
                originalTask,
                results.stream()
                        .map(r -> String.format("%s: %s", r.subtaskId(), r.result()))
                        .collect(Collectors.joining("\n"))
        );

        return chatModel.chat(synthesisPrompt);
    }

    /**
     * Represents a subtask with dependencies.
     */
    public record Subtask(
            String id,
            String description,
            List<String> dependencies
    ) {}

    /**
     * Result of executing a subtask.
     */
    public record SubtaskResult(
            String subtaskId,
            String description,
            String result,
            boolean success
    ) {}

    /**
     * Result of executing a complex task.
     */
    public record TaskExecutionResult(
            String originalTask,
            List<SubtaskResult> subtaskResults,
            String summary
    ) {}
}
