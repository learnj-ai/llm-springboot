## Context

Module 06 (Enterprise Production) currently includes tasks 18.1-18.7 for implementing a custom evaluation framework from scratch. This involves:
- Manual calculation of accuracy using embedding cosine similarity
- Custom relevance scoring between responses and context
- LLM-as-judge implementation for faithfulness
- Custom aggregation of metrics across test cases
- Manual JSON parsing of evaluation datasets

Dokimos is a production-ready Java evaluation framework specifically designed for LLM applications. It's framework-agnostic with explicit support for Spring AI, LangChain4J, and other Java AI frameworks. The workshop uses Spring Boot with Langchain4J for OpenAI integration, making Dokimos a natural fit.

Current constraints:
- Workshop targets Java 25, Spring Boot 4.0.5, Langchain4J 1.11.0
- Students progress sequentially through modules
- Educational focus requires clear, understandable patterns
- Module 06 must demonstrate production-ready evaluation practices

## Goals / Non-Goals

**Goals:**
- Replace custom evaluation implementation (tasks 18.1-18.7) with Dokimos integration
- Provide students with industry-standard evaluation patterns
- Reduce code complexity while maintaining educational value
- Demonstrate experiment orchestration and result tracking
- Enable extensibility through custom evaluators
- Maintain REST API compatibility for evaluation endpoint

**Non-Goals:**
- Replacing other Module 06 components (tracing, metrics, caching, deployment)
- Supporting evaluation for modules 01-05 (evaluation targets Module 02's RAG service)
- Building a custom Dokimos Server deployment (use local evaluation only)
- Modifying existing workshop modules 01-05

## Decisions

### Decision 1: Use Dokimos Spring AI Integration

**Choice:** Use `dokimos-spring-ai` module to bridge Spring AI's `ChatModel` with Dokimos evaluators, leveraging `SpringAiSupport.asJudge()` for LLM-as-judge patterns.

**Rationale:**
- **Compatibility**: Workshop already uses Spring Boot; Spring AI provides similar abstractions to Langchain4J
- **Reduced Boilerplate**: `SpringAiSupport.asJudge()` converts `ChatModel` to `JudgeLM` automatically
- **Type Safety**: Strong typing for evaluation criteria and results
- **Educational Value**: Students learn Spring AI patterns alongside Langchain4J

**Alternatives Considered:**
- Direct Langchain4J integration: Rejected - Dokimos' LangChain4J module is less mature than Spring AI support
- Custom JudgeLM implementation: Rejected - loses benefit of framework integration

**Implementation:**
```java
@Configuration
public class EvaluationConfig {
    @Bean
    public JudgeLM judgeLLM(ChatModel chatModel) {
        return SpringAiSupport.asJudge(chatModel);
    }
}
```

### Decision 2: Dataset Format with Dokimos Example Structure

**Choice:** Convert evaluation golden dataset to Dokimos `Dataset` format with `Example` objects containing `input`, `expected`, and `metadata` fields.

**Rationale:**
- **Framework Alignment**: Dokimos orchestration expects `Dataset` structure
- **Metadata Support**: Enables tagging examples by difficulty, category, etc.
- **Reusability**: Same dataset can run through multiple evaluators
- **Version Control**: JSON format remains git-friendly

**Format:**
```json
{
  "name": "RAG Evaluation Set",
  "examples": [
    {
      "input": "What is the capital of France?",
      "expected": "Paris",
      "metadata": {
        "category": "geography",
        "difficulty": "easy"
      }
    }
  ]
}
```

**Alternatives Considered:**
- Keep custom JSON format: Rejected - requires manual parsing and loses Dokimos integration benefits
- CSV format: Rejected - poor support for complex fields like context arrays

### Decision 3: Built-in Evaluators Over Custom Implementation

**Choice:** Use Dokimos' built-in `FaithfulnessEvaluator`, `HallucinationEvaluator`, and `ContextualRelevanceEvaluator` instead of custom implementations.

**Rationale:**
- **Battle-Tested**: Dokimos evaluators are production-proven
- **Maintained**: Bug fixes and improvements come from framework updates
- **Configurable**: Evaluators accept custom prompts and thresholds
- **Educational**: Students learn established patterns rather than reinventing wheels

**Mapping:**
| Custom Implementation | Dokimos Evaluator |
|-----------------------|-------------------|
| `calculateAccuracy()` | `ExactMatchEvaluator` or `SemanticSimilarityEvaluator` |
| `calculateFaithfulness()` | `FaithfulnessEvaluator` |
| `calculateRelevance()` | `ContextualRelevanceEvaluator` |
| N/A | `HallucinationEvaluator` (bonus) |

**Alternatives Considered:**
- Implement custom evaluators extending `BaseEvaluator`: Accepted as advanced topic, not replacement
- Mix custom and built-in: Rejected - increases complexity for beginners

### Decision 4: Experiment Orchestration with Result Aggregation

**Choice:** Use Dokimos `Experiment` API to orchestrate evaluation runs, automatically aggregating per-example results into summary statistics.

**Rationale:**
- **Orchestration**: Single API call runs all evaluators across all examples
- **Aggregation**: Framework handles averaging, min/max, standard deviation
- **Export**: Results export to JSON, CSV, or console for analysis
- **CI/CD Ready**: `dokimos-junit` enables pipeline integration

**Implementation Pattern:**
```java
Experiment experiment = Experiment.builder()
    .dataset(dataset)
    .task(ragTask)
    .evaluators(faithfulness, hallucination, relevance)
    .build();

ExperimentResult result = experiment.run();
```

**Alternatives Considered:**
- Manual loop over examples: Rejected - loses aggregation and error handling
- Parallel execution: Deferred to Dokimos internals (may support future versions)

### Decision 5: Spring AI Dependency Alongside Langchain4J

**Choice:** Add Spring AI OpenAI starter alongside existing Langchain4J dependency, using Langchain4J for RAG service and Spring AI for evaluation judge.

**Rationale:**
- **Framework Isolation**: RAG service remains Langchain4J; evaluation uses Spring AI
- **No Migration**: Existing modules 01-05 unchanged
- **Dual Learning**: Students see both frameworks' strengths
- **Practical Reality**: Production systems often mix frameworks

**Dependency Addition:**
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>dev.dokimos</groupId>
    <artifactId>dokimos-spring-ai</artifactId>
</dependency>
```

**Alternatives Considered:**
- Migrate entire workshop to Spring AI: Rejected - too disruptive, out of scope
- Build custom Langchain4J bridge: Rejected - significant effort, Spring AI bridge exists

## Risks / Trade-offs

### Risk: Dependency Conflicts Between Langchain4J and Spring AI
**Impact:** Both frameworks use OpenAI SDK; version mismatches could cause runtime errors.
**Mitigation:**
- Explicit dependency management in parent POM
- Test Module 06 startup with both frameworks active
- Document conflict resolution in troubleshooting guide
- Consider exclusion rules if conflicts arise

### Risk: Dokimos API Changes in Future Versions
**Impact:** Workshop code may break with major Dokimos updates.
**Mitigation:**
- Pin to specific Dokimos version (e.g., 0.2.0) in POM
- Document version in workshop materials
- Test against new versions before updating workshops
- Dokimos follows semantic versioning

### Risk: Learning Curve for Students Unfamiliar with Frameworks
**Impact:** Students see both Langchain4J (modules 01-05) and Spring AI (module 06 evaluation).
**Mitigation:**
- Clear documentation explaining framework choice per module
- Focus on Dokimos API, treat Spring AI as implementation detail
- Emphasize transferable concepts (datasets, evaluators, experiments)
- Provide comparison table in workshop materials

### Trade-off: Additional Dependencies vs. Code Simplicity
**Decision:** Accept 2-3 new dependencies (dokimos-core, dokimos-spring-ai, spring-ai-openai) in exchange for removing ~200 lines of custom evaluation code.
**Rationale:** Workshop benefits from production patterns outweigh dependency count. Students learn real-world tools rather than toy implementations.

### Trade-off: Educational Transparency vs. Framework Abstraction
**Decision:** Use framework abstractions (Experiment, Evaluator) even though custom code is more "transparent."
**Rationale:** Learning how to use frameworks IS the skill. Students can inspect Dokimos source code if curious. Production engineers use frameworks, not custom implementations.

## Migration Plan

### Phase 1: Dependency Integration
1. Add Dokimos and Spring AI dependencies to Module 06 POM
2. Create `EvaluationConfig` with JudgeLM bean
3. Verify no conflicts with existing Langchain4J setup
4. Test application startup

### Phase 2: Dataset Migration
1. Convert `data/eval-golden-set.json` to Dokimos `Dataset` format
2. Add metadata tags (category, difficulty) to examples
3. Create `DatasetLoader` service to load JSON into Dokimos `Dataset`

### Phase 3: Evaluator Integration
1. Replace `calculateAccuracy()` with `ExactMatchEvaluator` or `SemanticSimilarityEvaluator`
2. Replace `calculateFaithfulness()` with `FaithfulnessEvaluator`
3. Replace `calculateRelevance()` with `ContextualRelevanceEvaluator`
4. Add `HallucinationEvaluator` as bonus metric

### Phase 4: Experiment Orchestration
1. Create `Task` implementation bridging RAG service to Dokimos
2. Build `Experiment` with dataset, task, and evaluators
3. Replace custom aggregation with `ExperimentResult` API
4. Update REST endpoint to return Dokimos result format

### Phase 5: Testing & Documentation
1. Write JUnit tests using `dokimos-junit` assertions
2. Update Module 06 workshop instructions with Dokimos patterns
3. Add troubleshooting section for dependency conflicts
4. Create sample queries demonstrating evaluation API

### Rollback Strategy
- Git branch: `module-06-dokimos-integration` for isolated development
- Custom evaluation code remains in git history
- Can revert to custom implementation if Dokimos proves unsuitable
- Module 06 is independent; rollback doesn't affect modules 01-05

## Open Questions

1. **Semantic Similarity vs. Exact Match for Accuracy?**
   - Decision: Start with `ExactMatchEvaluator` for simplicity, document `SemanticSimilarityEvaluator` as advanced option
   - Semantic similarity requires embedding model configuration

2. **Should we demonstrate custom evaluators?**
   - Decision: Include one simple custom evaluator (e.g., `ResponseLengthEvaluator`) as Lab 6.1 exercise
   - Shows extensibility without overwhelming beginners

3. **Export format for evaluation results?**
   - Decision: JSON export via `ExperimentResult.toJson()` for REST API
   - Console output for local development
   - Document CSV export as optional enhancement

4. **Dokimos Server integration?**
   - Decision: Out of scope for workshop
   - Document as "production enhancement" in workshop notes
   - Local file-based results sufficient for learning

5. **Version compatibility with future Langchain4J updates?**
   - Decision: Monitor Langchain4J + Spring AI compatibility
   - Update workshop when major versions shift
   - Keep dependencies flexible in parent POM where possible
