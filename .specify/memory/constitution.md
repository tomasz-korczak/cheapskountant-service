<!--
Sync Impact Report
- Version change: template (unversioned) → 1.0.0
- Modified principles:
  - [PRINCIPLE_1_NAME] → I. REST API Only
  - [PRINCIPLE_2_NAME] → II. Explicit API Contracts
  - [PRINCIPLE_3_NAME] → III. Tested Behavior
  - [PRINCIPLE_4_NAME] → IV. Secure by Default
  - [PRINCIPLE_5_NAME] → removed (bare minimum scope; four principles suffice)
- Added sections: Technical Constraints, Development Workflow
- Removed sections: none
- Deferred TODOs: none
-->

# Cheapskountant Service Constitution

## Core Principles

### I. REST API Only

The application MUST expose its functionality exclusively through HTTP REST endpoints returning
JSON. It MUST NOT contain frontend code, server-rendered views, or static web assets.
Operational endpoints (health checks, API documentation) are permitted.

### II. Explicit API Contracts

Every endpoint MUST have a documented contract defining method, path, request, response, and
error format. Endpoints MUST use correct HTTP methods and status codes. Errors MUST use a single
consistent machine-readable format. Breaking contract changes MUST be versioned.

### III. Tested Behavior

Every endpoint MUST be covered by an automated test verifying its success and error responses.
Business logic MUST be covered by unit tests. All tests MUST pass before a change is merged.

### IV. Secure by Default

All request input MUST be validated. Endpoints MUST require authentication unless explicitly
declared public. Secrets MUST NOT be stored in source code or written to logs.

## Technical Constraints

- Configuration MUST be supplied externally (environment variables or configuration files).
- Database schema changes MUST be delivered as Liquibase SQL changesets.
- The service MUST expose a health check endpoint.

## Development Workflow

- Every change MUST be delivered through a reviewed pull request.
- Reviewers MUST verify compliance with this constitution.

## Governance

This constitution overrides conflicting practices. Amendments require a pull request describing
the change and its rationale. Versioning follows semantic versioning: MAJOR for removed or
redefined principles, MINOR for added principles or sections, PATCH for clarifications.

**Version**: 1.0.0 | **Ratified**: 2026-09-26 | **Last Amended**: 2026-09-26
