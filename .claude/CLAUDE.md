# Beema Unified Platform Protocol
- **Architecture:** Metadata-driven, Bitemporal (valid_time/transaction_time), JSONB flex-schema.
- **Unified Goal:** All entities must handle Retail, Commercial, and London Market contexts.
- **Tech Stack:** Spring Boot 3, PostgreSQL, React, Temporal.io, Helm/K8s.
- **Observability:** OpenTelemetry + Jaeger (tracing), Prometheus (metrics), Grafana (dashboards).
- **Safety:** Use Git Worktrees for parallel development to avoid file conflicts.

## Key Architectural Principles
1. **Metadata-driven**: Schema changes via database, not code deployments
2. **Bitemporal**: Track both valid time (business) and transaction time (audit)
3. **Multi-tenant**: Row-Level Security with tenant isolation
4. **Event-driven**: Temporal workflows for durable orchestration
5. **Observable**: Distributed tracing and metrics for all services

## Observability Stack
- **Traces**: OpenTelemetry → Jaeger (OTLP endpoint: http://jaeger:4318)
- **Metrics**: Spring Actuator → Prometheus (scrape /actuator/prometheus)
- **Dashboards**: Grafana with auto-configured datasources
- **Correlation**: Trace IDs link traces → metrics → logs via X-Correlation-ID header

See: `platform/observability/README.md` for full guide.