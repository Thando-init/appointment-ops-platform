# CI/CD process

## Why CI/CD matters

CI/CD turns the checks we run manually into a repeatable process. The goal is not to make the project look corporate; it is to learn how teams prevent broken code from being merged or deployed.

## Pipeline file

The workflow is:

```text
.github/workflows/ci.yml
```

It runs on pushes and pull requests.

## Pipeline stages

### Stage 1: unit and static checks

The pipeline:

1. Checks out the repository.
2. Installs Java 21.
3. Runs `mvn clean test`.
4. Checks frontend JavaScript syntax with Node.
5. Validates n8n JSON files.

### Stage 2: acceptance tests

The pipeline:

1. Starts a PostgreSQL 16 service container.
2. Starts the Spring Boot API.
3. Waits for `/api/v1/health`.
4. Runs `tests/acceptance/test_api.py`.
5. Prints the API log if the job fails.

The acceptance test creates a booking and checks that a duplicate attempt returns `409 Conflict`.

## Local equivalents

Run Java and static checks:

```powershell
mvn -f services/booking-service/pom.xml clean test
node --check apps/booking-form/app.js
node --check apps/booking-form/tilt-effects.js
```

Run acceptance tests locally:

```powershell
python tests/acceptance/test_api.py
```

Or use the Makefile in Git Bash:

```bash
make test
make api-test
```

## Branch and pull-request process

For each feature:

1. Create a branch, for example `feature/booking-dashboard`.
2. Make one small change at a time.
3. Run local tests.
4. Commit with an imperative message.
5. Push the branch.
6. Open a pull request.
7. Wait for CI to pass.
8. Review the changed files and test output.
9. Merge only after the checks are green.

## Deployment process to add later

A production pipeline should add these gates after CI:

```text
Unit tests
    ↓
Acceptance tests
    ↓
Build immutable application artifact
    ↓
Security/dependency scan
    ↓
Deploy to staging
    ↓
Smoke test staging
    ↓
Manual approval for production
    ↓
Deploy production
    ↓
Health check and rollback plan
```

The current repository does not deploy automatically. That is intentional: local correctness and understanding should come before adding cloud credentials or a production target.

## Secrets

Never put these in GitHub workflow YAML:

- PostgreSQL passwords
- n8n API keys
- WhatsApp tokens
- Payment provider secrets
- Calendar credentials

Use GitHub Actions Secrets or the target platform's secret manager.

## CI troubleshooting

| Failure | First check |
|---|---|
| Maven compilation failure | Run `mvn clean test` locally from the service folder |
| API never becomes healthy | Inspect `/tmp/booking-api.log` in the CI output |
| Migration failure | Check the Flyway path and SQL filename |
| Acceptance test conflict | Ensure CI starts with a fresh database |
| JavaScript syntax failure | Run `node --check` on the changed file |
| Invalid workflow JSON | Run `python3 -m json.tool` |
