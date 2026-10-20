# Contributing to Health Camp Registration and Deployment Platform

Thanks for contributing. This document defines the workflow rules for branches, commits, pull requests, and reviews. Following them keeps the history readable and reviews fast.

## 1. Branch naming

All work happens on branches created from `main` (or from the relevant support branch for hotfixes). Use kebab-case for the `<description>` and keep it short but meaningful.

| Branch pattern | Use case |
| --- | --- |
| `main` | Stable, releasable history. Pull requests merge here. |
| `feature/<description>` | New functionality, e.g. `feature/participant-registration`. |
| `bugfix/<description>` | Fixing a defect, e.g. `bugfix/registration-validation-error`. |
| `docs/<description>` | Documentation-only changes, e.g. `docs/api-endpoints`. |
| `hotfix/<description>` | Urgent production fix branched from `main`, e.g. `hotfix/login-500`. |

Do not push directly to `main`. All changes arrive through a pull request.

## 2. Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/) with one of these prefixes:

| Prefix | Meaning |
| --- | --- |
| `feat:` | A new feature |
| `fix:` | A bug fix |
| `docs:` | Documentation only |
| `test:` | Adding or fixing tests |
| `build:` | Build system, dependencies, or tooling |
| `chore:` | Routine maintenance that does not fit the above |

Format:

```
<prefix>: <short imperative summary>

<optional body explaining why and what>

<optional footer, e.g. Closes #12>
```

Examples:

```
feat: add participant registration endpoint
fix: validate required fields on registration
docs: describe planned API endpoints
test: add context-load smoke test
build: upgrade spring boot to 3.5.16
chore: update gitignore for IDE files
```

Rules:

- Keep the summary under ~72 characters, imperative mood ("add", not "added").
- One logical change per commit where possible.
- Reference the issue in the footer when relevant (`Closes #12`, `Refs #15`).

## 3. Pull requests

1. Branch from `main` (or the correct support branch for hotfixes) and rebase before opening if `main` has moved.
2. The PR must use the template in `.github/pull_request_template.md`.
3. The PR title should match the commit convention (`feat: ...`, `fix: ...`, etc.).
4. Link the related issue in the PR description.
5. Ensure the following pass before requesting review (once a CI pipeline exists):
   - `mvn clean verify`
   - No new compiler warnings introduced
6. Do not merge a PR that fails tests or CI.

## 4. Code review

- At least **one** approval is required to merge into `main`.
- Reviewers check: correctness, tests covering the change, no secrets committed, consistent style, and documentation updated where needed.
- Use GitHub's review features: comment for suggestions, "Request changes" for required fixes, "Approve" when satisfied.
- The author resolves conversations and re-pushes; reviewers re-review.
- Squash-and-merge is preferred so `main` has one commit per PR with a conventional-commit title.

## 5. Secrets and configuration

Never commit real credentials, keys, or local configuration. Use environment variables or the local-only files ignored in `.gitignore` (`application-local.yml`, `.env`, etc.). If a secret is accidentally committed, notify a maintainer immediately and rotate it.
