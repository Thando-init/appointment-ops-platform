# ADR-001: Keep booking rules in Spring Boot and use PostgreSQL

- **Status:** Accepted
- **Date:** 2026-09-16

## Context

The project needs to demonstrate frontend development, Java backend development, database persistence, and automation with n8n. The booking workflow must remain reliable when external providers fail.

## Decision

Keep scheduling and booking persistence in the Spring Boot service. Use PostgreSQL as the local and intended deployment database. Use n8n for external side effects after a booking is persisted.

## Reasons

- The backend can validate all clients consistently.
- PostgreSQL provides a realistic multi-user database.
- n8n is useful for orchestration but should not be the only durable store.
- Separate boundaries make failures easier to reason about.

## Consequences

The project has more setup than a browser-only demo. In return, it demonstrates real integration skills: HTTP contracts, SQL migrations, repositories, tests, environment configuration, and workflow automation.

The next improvement is a transactional outbox so a persisted booking and its automation event cannot drift apart.
