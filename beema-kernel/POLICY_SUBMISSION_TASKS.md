# Policy Submission Implementation Task List

## Overview

This document outlines the roadmap for enhancing the Beema policy submission system. The goal is to build a complete end-to-end submission workflow that handles quote generation, binding, policy issuance, and post-submission activities across Retail, Commercial, and London Market contexts.

The current implementation provides a foundation with workflow orchestration and quote generation. The tasks below represent the next phase of development to create a production-ready submission system.

---

## Current Status

### Completed Features
- **Workflow Engine Integration:** Core workflow orchestration with state machine support
- **Quote Generation:** Basic quote calculation endpoint integrated with workflow
- **React Frontend Scaffold:** Initial submission form with product/coverage selection
- **Metadata-Driven Architecture:** Flexible schema support using JSONB for product configurations
- **Bitemporal Data Model:** Support for valid_time and transaction_time tracking

---

## Task Breakdown

### Phase 1: Data Persistence & Core Operations

#### Task 1: Persist Submissions
**Description:**
Create a PostgreSQL `submissions` table to store submission data with full bitemporal support. Include fields for customer information, product selection, coverage details, premium amounts, and workflow state tracking.

**Complexity:** Medium

**Dependencies:** None

**Acceptance Criteria:**
- [ ] Database schema created with proper indexes on workflow_id, status, and temporal columns
- [ ] JPA entity created with proper annotations for JSONB fields
- [ ] Repository layer implements save/update/query operations
- [ ] Submission data includes: customer_id, product_code, coverage_details (JSONB), premium_amount, status, valid_from, valid_to, transaction_time
- [ ] Integration tests verify CRUD operations and bitemporal queries
- [ ] Migration scripts follow Flyway/Liquibase conventions

**Technical Notes:**
```sql
CREATE TABLE submissions (
  id UUID PRIMARY KEY,
  workflow_id VARCHAR(255) NOT NULL,
  customer_id UUID NOT NULL,
  product_code VARCHAR(50) NOT NULL,
  coverage_details JSONB NOT NULL,
  premium_amount DECIMAL(15,2),
  status VARCHAR(50) NOT NULL,
  valid_from TIMESTAMP NOT NULL,
  valid_to TIMESTAMP,
  transaction_time TIMESTAMP NOT NULL DEFAULT NOW(),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

#### Task 2: Bind Signal Integration
**Description:**
Add an "Accept Quote" button in the frontend that sends a BIND signal to the workflow engine, transitioning the submission from QUOTED to BINDING state.

**Complexity:** Simple

**Dependencies:** Task 1 (Persist Submissions)

**Acceptance Criteria:**
- [ ] REST endpoint `/api/v1/submissions/{id}/bind` created
- [ ] Endpoint validates submission is in QUOTED state before accepting bind request
- [ ] Workflow engine receives BIND signal and transitions state appropriately
- [ ] Frontend button appears only when submission status is QUOTED
- [ ] Button disabled during bind operation with loading indicator
- [ ] Success/error messages displayed to user
- [ ] Unit tests cover state validation logic
- [ ] Integration tests verify end-to-end bind flow

**Technical Notes:**
- Use optimistic locking to prevent concurrent bind attempts
- Consider idempotency for retry scenarios

---

### Phase 2: Real-Time Updates & User Experience

#### Task 3: Status Polling
**Description:**
Implement real-time status updates on the submission detail page using either polling or WebSocket connections to reflect workflow state changes.

**Complexity:** Medium

**Dependencies:** Task 1 (Persist Submissions)

**Acceptance Criteria:**
- [ ] Frontend polls submission status every 2-3 seconds while workflow is in progress
- [ ] Polling stops when terminal state reached (POLICY_ISSUED, DECLINED, EXPIRED)
- [ ] Status badge updates automatically without page refresh
- [ ] Optional: WebSocket endpoint for server-push updates (if scalability is a concern)
- [ ] Connection handling includes reconnection logic for network failures
- [ ] UI shows clear visual feedback during state transitions
- [ ] Performance tested with 100+ concurrent users

**Technical Notes:**
- Consider Server-Sent Events (SSE) as lightweight alternative to WebSocket
- Implement exponential backoff for polling to reduce server load

---

#### Task 4: Policy Feed
**Description:**
Create a submissions list view displaying all submissions for the current user with filterable status badges, search, and pagination.

**Complexity:** Medium

**Dependencies:** Task 1 (Persist Submissions)

**Acceptance Criteria:**
- [ ] REST endpoint `/api/v1/submissions` returns paginated list
- [ ] Support filtering by status (DRAFT, QUOTED, BINDING, POLICY_ISSUED, etc.)
- [ ] Support filtering by product type and date range
- [ ] Search functionality by customer name or submission ID
- [ ] Status badges color-coded (green=success, yellow=pending, red=failed)
- [ ] Each row shows: submission date, customer name, product, premium, status
- [ ] Click row navigates to submission detail page
- [ ] Pagination supports 10/25/50 items per page
- [ ] Loading states and empty states handled gracefully
- [ ] Unit tests for backend filtering/sorting logic
- [ ] E2E tests for frontend interactions

---

### Phase 3: Business Logic & Validation

#### Task 5: Validation Rules
**Description:**
Implement product-specific validation rules driven by metadata configuration. Rules should validate coverage limits, eligibility criteria, and underwriting requirements before quote generation.

**Complexity:** Complex

**Dependencies:** None (can run in parallel with Phase 1/2)

**Acceptance Criteria:**
- [ ] Validation rules stored in metadata tables or JSONB configuration
- [ ] Rules engine evaluates conditions based on submission data (e.g., age restrictions, coverage limits, occupancy types)
- [ ] Support for Retail, Commercial, and London Market specific rules
- [ ] Validation errors returned with clear field-level messages
- [ ] Frontend displays validation errors inline with form fields
- [ ] Rules are versioned with bitemporal support for audit trail
- [ ] Admin interface to configure validation rules (optional, can be manual JSONB updates initially)
- [ ] Unit tests cover edge cases (boundary values, missing fields)
- [ ] Integration tests verify rule execution across product types

**Technical Notes:**
- Consider using a rules engine like Drools or implement custom expression evaluator
- Example rule: "For HomeOwners insurance, dwelling coverage must be >= $50,000 and <= $5,000,000"

---

### Phase 4: Document Management

#### Task 6: Document Upload
**Description:**
Add file attachment support to submissions, allowing users to upload supporting documents (proof of prior insurance, property inspection reports, etc.).

**Complexity:** Medium

**Dependencies:** Task 1 (Persist Submissions)

**Acceptance Criteria:**
- [ ] REST endpoint `/api/v1/submissions/{id}/documents` for upload
- [ ] Support for PDF, JPG, PNG file types with size limit (10MB max)
- [ ] Files stored in S3/cloud storage with reference in database
- [ ] Document metadata tracked: filename, content_type, size, upload_timestamp, uploaded_by
- [ ] Frontend drag-and-drop upload component
- [ ] Document preview/download functionality
- [ ] Virus scanning integration (if required by compliance)
- [ ] Documents associated with submission via foreign key
- [ ] Soft delete support for document removal
- [ ] Unit tests for upload validation (file type, size)
- [ ] Integration tests verify end-to-end upload/retrieval

**Technical Notes:**
- Use presigned URLs for secure direct-to-S3 uploads
- Consider document retention policies for compliance

---

### Phase 5: Communication & Notifications

#### Task 7: Email Notifications
**Description:**
Send automated email notifications to customers after submission reaches QUOTED state, including quote details and next steps.

**Complexity:** Simple

**Dependencies:** Task 1 (Persist Submissions)

**Acceptance Criteria:**
- [ ] Email template created for quote confirmation (HTML + plain text)
- [ ] Template includes: customer name, product details, premium amount, coverage summary, expiration date, call-to-action button
- [ ] Email triggered automatically when workflow transitions to QUOTED state
- [ ] Integration with email service provider (SendGrid, AWS SES, or SMTP)
- [ ] Email delivery failures logged and retried (up to 3 attempts)
- [ ] Unsubscribe link included for compliance
- [ ] Template supports multi-language (if required)
- [ ] Unit tests verify email content generation
- [ ] Integration tests confirm emails sent on state transition

**Technical Notes:**
- Use Spring's async processing for non-blocking email sends
- Consider event-driven architecture with message queue for reliability

---

### Phase 6: Analytics & Reporting

#### Task 8: Analytics Tracking
**Description:**
Implement tracking for submission conversion rates, abandonment points, and workflow performance metrics to inform product and UX improvements.

**Complexity:** Complex

**Dependencies:** Task 1 (Persist Submissions), Task 4 (Policy Feed)

**Acceptance Criteria:**
- [ ] Analytics events logged for: submission started, quote viewed, quote accepted, quote expired, quote declined
- [ ] Conversion funnel report showing drop-off at each stage
- [ ] Average time-to-bind metric calculated
- [ ] Abandonment analysis by product type and coverage amount
- [ ] Dashboard showing: total submissions, conversion rate, avg premium, top products
- [ ] Data exported to analytics platform (Google Analytics, Mixpanel, or internal BI tool)
- [ ] Privacy-compliant tracking (no PII in analytics events)
- [ ] Historical trend analysis (week-over-week, month-over-month)
- [ ] Unit tests for metric calculation logic
- [ ] Integration tests verify event tracking

**Technical Notes:**
- Consider using a time-series database (InfluxDB, TimescaleDB) for metrics storage
- Implement data aggregation jobs for performance optimization

---

## Implementation Priorities

### High Priority (MVP)
1. Task 1: Persist Submissions
2. Task 2: Bind Signal Integration
3. Task 4: Policy Feed
4. Task 7: Email Notifications

### Medium Priority (Post-MVP)
5. Task 3: Status Polling
6. Task 5: Validation Rules
7. Task 6: Document Upload

### Low Priority (Future Enhancements)
8. Task 8: Analytics Tracking

---

## Cross-Cutting Concerns

### Security
- All API endpoints require authentication/authorization
- Implement rate limiting on submission endpoints
- Encrypt sensitive data at rest and in transit
- Audit logging for all submission state changes

### Testing Strategy
- Unit tests: 80%+ coverage for business logic
- Integration tests: API contract testing with Testcontainers
- E2E tests: Critical user journeys (quote to bind flow)
- Performance tests: Load testing with 1000+ concurrent submissions

### Monitoring
- Application metrics: submission count, error rates, latency
- Business metrics: conversion rates, average premium
- Alerting: Failed workflows, email delivery failures, validation errors

### Documentation
- API documentation using OpenAPI/Swagger
- Architecture decision records (ADRs) for major design choices
- Runbook for operational procedures

---

## Notes

- All tasks should support Retail, Commercial, and London Market contexts as per the Beema Unified Platform Protocol
- Use Git Worktrees for parallel development to avoid merge conflicts
- Follow bitemporal data modeling patterns for audit trail requirements
- Leverage JSONB for flexible metadata-driven configurations
- Ensure all database migrations are reversible for safe rollbacks

---

**Last Updated:** 2026-02-13
**Document Owner:** Engineering Team
**Review Cycle:** Monthly
