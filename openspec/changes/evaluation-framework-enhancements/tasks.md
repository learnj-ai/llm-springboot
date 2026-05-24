## 1. Custom Evaluators - Citation Quality

- [x] 1.1 Create CitationQualityEvaluator class extending BaseEvaluator
- [x] 1.2 Implement citation pattern detection ([1], [2] format)
- [x] 1.3 Add configurable minimum citation count threshold
- [x] 1.4 Register CitationQualityEvaluator as Spring bean
- [x] 1.5 Add configuration properties to application.yml
- [x] 1.6 Write unit tests for CitationQualityEvaluator

## 2. Custom Evaluators - Source Diversity

- [ ] 2.1 Create SourceDiversityEvaluator class extending BaseEvaluator
- [ ] 2.2 Implement distinct source counting logic from task outputs
- [ ] 2.3 Add configurable minimum source count threshold
- [ ] 2.4 Register SourceDiversityEvaluator as Spring bean
- [ ] 2.5 Add configuration properties to application.yml
- [ ] 2.6 Write unit tests for SourceDiversityEvaluator

## 3. Custom Evaluators - Answer Completeness

- [ ] 3.1 Create AnswerCompletenessEvaluator class extending BaseEvaluator
- [ ] 3.2 Implement multi-part query detection (keywords: "and", "features and pricing")
- [ ] 3.3 Implement aspect coverage checking in response
- [ ] 3.4 Calculate completeness score based on coverage percentage
- [ ] 3.5 Register AnswerCompletenessEvaluator as Spring bean
- [ ] 3.6 Add configuration properties to application.yml
- [ ] 3.7 Write unit tests for AnswerCompletenessEvaluator

## 4. Custom Evaluators - Semantic Coherence

- [ ] 4.1 Create SemanticCoherenceEvaluator class extending BaseEvaluator
- [ ] 4.2 Implement sentence structure analysis for logical flow
- [ ] 4.3 Implement contradiction detection using negation patterns
- [ ] 4.4 Calculate coherence score based on structure and consistency
- [ ] 4.5 Register SemanticCoherenceEvaluator as Spring bean
- [ ] 4.6 Add configuration properties to application.yml
- [ ] 4.7 Write unit tests for SemanticCoherenceEvaluator

## 5. Custom Evaluators - Bias Detection

- [ ] 5.1 Create BiasDetectionEvaluator class extending BaseEvaluator
- [ ] 5.2 Create loaded language word lists (positive/negative bias indicators)
- [ ] 5.3 Implement one-sided perspective detection using viewpoint keywords
- [ ] 5.4 Calculate bias score based on language and balance
- [ ] 5.5 Register BiasDetectionEvaluator as Spring bean
- [ ] 5.6 Add configuration properties to application.yml
- [ ] 5.7 Write unit tests for BiasDetectionEvaluator

## 6. Custom Evaluators - Integration

- [ ] 6.1 Update DokimosEvaluationService to include all 5 custom evaluators
- [ ] 6.2 Update buildEvaluatorList() method with new evaluator filtering
- [ ] 6.3 Add custom evaluator names to validateEvaluatorNames() in EvaluationController
- [ ] 6.4 Update getAllEvaluators() helper method
- [ ] 6.5 Verify all custom evaluators work with runExperiment() method

## 7. Caching Infrastructure

- [ ] 7.1 Create EvaluationCacheConfig configuration class
- [ ] 7.2 Configure RedisCacheManager with appropriate serializers
- [ ] 7.3 Add spring-boot-starter-data-redis dependency (verify existing)
- [ ] 7.4 Create cache configuration properties in application.yml
- [ ] 7.5 Add Redis connection configuration (host, port, password)

## 8. Caching Service Layer

- [ ] 8.1 Create CachingEvaluatorProxy class implementing Evaluator interface
- [ ] 8.2 Implement cache key generation strategy (evaluator + input + output hash)
- [ ] 8.3 Add @Cacheable annotation with appropriate cache name and key
- [ ] 8.4 Implement selective caching logic (LLM-as-judge only by default)
- [ ] 8.5 Add configurable evaluator list for caching enablement
- [ ] 8.6 Implement graceful degradation when Redis unavailable

## 9. Cache Management API

- [ ] 9.1 Add POST /api/v1/eval/cache/clear endpoint for clearing all cache
- [ ] 9.2 Add DELETE /api/v1/eval/cache/{evaluatorName} endpoint for selective invalidation
- [ ] 9.3 Add GET /api/v1/eval/cache/stats endpoint for cache statistics
- [ ] 9.4 Implement cache warming endpoint POST /api/v1/eval/cache/warm
- [ ] 9.5 Add error handling and validation for cache endpoints

## 10. Cache Metrics

- [ ] 10.1 Add Micrometer counter for cache hits (evaluation.cache.hits)
- [ ] 10.2 Add Micrometer counter for cache misses (evaluation.cache.misses)
- [ ] 10.3 Add gauge for cache size (evaluation.cache.entries)
- [ ] 10.4 Add gauge for estimated cost savings (evaluation.cache.cost.savings.usd)
- [ ] 10.5 Implement cost calculation logic based on token estimates
- [ ] 10.6 Expose metrics via /actuator/prometheus endpoint

## 11. Caching Tests

- [ ] 11.1 Write unit tests for CachingEvaluatorProxy
- [ ] 11.2 Write integration tests for Redis cache behavior
- [ ] 11.3 Test cache hit/miss scenarios
- [ ] 11.4 Test cache TTL expiration
- [ ] 11.5 Test selective caching by evaluator type
- [ ] 11.6 Test graceful degradation when Redis unavailable

## 12. GitHub Actions Pipeline Template

- [ ] 12.1 Create cicd/github-actions/ directory structure
- [ ] 12.2 Create evaluation-pr.yml workflow for pull request checks
- [ ] 12.3 Configure PR workflow with lenient thresholds (70% pass rate)
- [ ] 12.4 Add artifact upload for evaluation results (JSON, HTML)
- [ ] 12.5 Create evaluation-main.yml workflow for main branch
- [ ] 12.6 Configure main workflow with stricter thresholds (80% pass rate)
- [ ] 12.7 Add secrets documentation for OPENAI_API_KEY
- [ ] 12.8 Create README.md with setup instructions

## 13. GitLab CI Pipeline Template

- [ ] 13.1 Create cicd/gitlab-ci/ directory structure
- [ ] 13.2 Create .gitlab-ci.yml with evaluation stage
- [ ] 13.3 Configure quality gates with configurable thresholds
- [ ] 13.4 Add JUnit XML report publishing
- [ ] 13.5 Add artifact publishing for evaluation results
- [ ] 13.6 Configure CI/CD variables for OPENAI_API_KEY
- [ ] 13.7 Create README.md with setup instructions

## 14. Jenkins Pipeline Template

- [ ] 14.1 Create cicd/jenkins/ directory structure
- [ ] 14.2 Create Jenkinsfile with declarative pipeline
- [ ] 14.3 Configure evaluation stage with credentials binding
- [ ] 14.4 Add build artifact archiving
- [ ] 14.5 Configure email notifications on failure
- [ ] 14.6 Add parallel execution configuration
- [ ] 14.7 Create README.md with setup instructions

## 15. CI/CD Pipeline Features

- [ ] 15.1 Implement threshold enforcement logic in pipelines
- [ ] 15.2 Add environment-specific threshold configuration (dev/staging/prod)
- [ ] 15.3 Add selective evaluation (smoke test vs full evaluation)
- [ ] 15.4 Configure parallel evaluator execution
- [ ] 15.5 Add result visualization (inline PR comments, badges)
- [ ] 15.6 Document artifact storage options (S3, Artifactory)

## 16. Prometheus Metrics for Evaluation

- [ ] 16.1 Add @Timed annotation to runExperiment() method
- [ ] 16.2 Create gauge for latest pass rate (evaluation.pass.rate)
- [ ] 16.3 Create counter for total evaluations run (evaluation.total.count)
- [ ] 16.4 Create counter for LLM API calls (evaluation.llm.api.calls)
- [ ] 16.5 Create histogram for evaluator execution time (evaluation.evaluator.duration)
- [ ] 16.6 Create gauge for estimated cost per evaluation (evaluation.cost.usd)

## 17. Grafana Dashboard - Overview

- [ ] 17.1 Create dashboards/overview.json file
- [ ] 17.2 Add current pass rate panel (stat panel with threshold colors)
- [ ] 17.3 Add 7-day pass rate trend graph (time series)
- [ ] 17.4 Add evaluator summary table (pass rates per evaluator)
- [ ] 17.5 Add recent regressions alert panel
- [ ] 17.6 Configure 30-second auto-refresh
- [ ] 17.7 Add Prometheus datasource configuration

## 18. Grafana Dashboard - Evaluator Details

- [ ] 18.1 Create dashboards/evaluator-details.json file
- [ ] 18.2 Add per-evaluator score time series panels
- [ ] 18.3 Add score distribution histograms
- [ ] 18.4 Add min/max/avg score stat panels
- [ ] 18.5 Add failed examples table (drill-down)
- [ ] 18.6 Add evaluator comparison heatmap
- [ ] 18.7 Configure 1-minute auto-refresh

## 19. Grafana Dashboard - Operations

- [ ] 19.1 Create dashboards/operations.json file
- [ ] 19.2 Add cache hit rate gauge panel
- [ ] 19.3 Add cost savings from cache panel
- [ ] 19.4 Add cache size trend graph
- [ ] 19.5 Add execution duration histogram
- [ ] 19.6 Add LLM API call count panel
- [ ] 19.7 Add estimated cost tracking panel
- [ ] 19.8 Configure 30-second auto-refresh

## 20. Grafana Dashboard Configuration

- [ ] 20.1 Add environment selector variable (dev/staging/prod)
- [ ] 20.2 Configure environment-specific threshold annotations
- [ ] 20.3 Add cross-environment comparison row
- [ ] 20.4 Configure alerting rules for pass rate drops
- [ ] 20.5 Configure alerting rules for evaluator degradation
- [ ] 20.6 Document alert notification channel setup

## 21. Documentation - Custom Evaluators

- [ ] 21.1 Create docs/custom-evaluators/OVERVIEW.md
- [ ] 21.2 Document CitationQualityEvaluator usage and configuration
- [ ] 21.3 Document SourceDiversityEvaluator usage and configuration
- [ ] 21.4 Document AnswerCompletenessEvaluator usage and configuration
- [ ] 21.5 Document SemanticCoherenceEvaluator usage and configuration
- [ ] 21.6 Document BiasDetectionEvaluator usage and configuration
- [ ] 21.7 Add configuration reference with all properties and defaults

## 22. Documentation - Caching

- [ ] 22.1 Create docs/caching/SETUP.md with Redis installation guide
- [ ] 22.2 Create docs/caching/CONFIGURATION.md with property reference
- [ ] 22.3 Create docs/caching/PERFORMANCE.md with tuning recommendations
- [ ] 22.4 Document cache key strategy and TTL selection
- [ ] 22.5 Document cache invalidation API endpoints
- [ ] 22.6 Add troubleshooting section for Redis connection issues

## 23. Documentation - CI/CD Integration

- [ ] 23.1 Create docs/cicd/GITHUB-ACTIONS.md setup guide
- [ ] 23.2 Create docs/cicd/GITLAB-CI.md setup guide
- [ ] 23.3 Create docs/cicd/JENKINS.md setup guide
- [ ] 23.4 Document threshold configuration strategies
- [ ] 23.5 Document secrets management for OpenAI API key
- [ ] 23.6 Add troubleshooting section for pipeline failures

## 24. Documentation - Grafana Dashboards

- [ ] 24.1 Create docs/dashboards/SETUP.md with Grafana installation
- [ ] 24.2 Create docs/dashboards/METRICS.md with Prometheus metrics reference
- [ ] 24.3 Create docs/dashboards/ALERTING.md with alert configuration guide
- [ ] 24.4 Document dashboard import procedure
- [ ] 24.5 Document datasource configuration
- [ ] 24.6 Add troubleshooting section for dashboard issues

## 25. Lab Exercises

- [ ] 25.1 Create docs/labs/LAB-6.4-CUSTOM-EVALUATOR-LIBRARY.md
- [ ] 25.2 Create docs/labs/LAB-6.5-EVALUATION-CACHING.md
- [ ] 25.3 Create docs/labs/LAB-6.6-CICD-INTEGRATION.md
- [ ] 25.4 Create docs/labs/LAB-6.7-EVALUATION-DASHBOARDS.md
- [ ] 25.5 Add exercises and learning objectives to each lab
- [ ] 25.6 Add troubleshooting sections to each lab

## 26. Update Module 06 Documentation

- [ ] 26.1 Update MODULE-06-WORKSHOP.md with new capabilities overview
- [ ] 26.2 Update README.md with custom evaluators and caching features
- [ ] 26.3 Update TROUBLESHOOTING.md with caching and dashboard issues
- [ ] 26.4 Add production deployment patterns section
- [ ] 26.5 Update API reference with cache management endpoints
- [ ] 26.6 Update configuration reference with new properties

## 27. Testing - Custom Evaluators

- [ ] 27.1 Write unit tests for all 5 custom evaluators (30+ tests total)
- [ ] 27.2 Write integration test for custom evaluator registration
- [ ] 27.3 Test evaluator filtering with custom evaluator names
- [ ] 27.4 Test configuration property binding for all evaluators
- [ ] 27.5 Verify custom evaluators work with runExperiment()

## 28. Testing - Caching

- [ ] 28.1 Write unit tests for CachingEvaluatorProxy
- [ ] 28.2 Write integration tests for Redis cache behavior
- [ ] 28.3 Test cache key generation correctness
- [ ] 28.4 Test selective caching by evaluator type
- [ ] 28.5 Test cache TTL expiration
- [ ] 28.6 Test cache invalidation endpoints
- [ ] 28.7 Test graceful degradation without Redis

## 29. Testing - CI/CD Templates

- [ ] 29.1 Test GitHub Actions workflow in sample repository
- [ ] 29.2 Test GitLab CI pipeline in sample repository
- [ ] 29.3 Test Jenkins pipeline in test environment
- [ ] 29.4 Verify quality gate enforcement works
- [ ] 29.5 Verify artifact publishing works
- [ ] 29.6 Test threshold configuration override

## 30. Testing - Grafana Dashboards

- [ ] 30.1 Import all 3 dashboards into test Grafana instance
- [ ] 30.2 Verify all panels display correctly
- [ ] 30.3 Test environment selector variable
- [ ] 30.4 Verify Prometheus queries return expected data
- [ ] 30.5 Test alerting rules trigger correctly
- [ ] 30.6 Test PDF export functionality

## 31. Final Integration

- [ ] 31.1 Run full evaluation experiment with all 10 evaluators (5 built-in + 5 custom)
- [ ] 31.2 Verify caching reduces execution time on second run
- [ ] 31.3 Verify all metrics appear in /actuator/prometheus
- [ ] 31.4 Verify cache statistics API returns correct data
- [ ] 31.5 Run Maven build to ensure no compilation errors
- [ ] 31.6 Run all tests to ensure integration works end-to-end
