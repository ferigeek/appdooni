# AGENTS.md

## Introduction

This project is a JavaFX desktop application for cataloging applications across operating systems. It uses Scene Builder/FXML for the UI and SQLite for local persistence. The SQLite `.db` file is also the import/export format.

Keep the project simple, readable, modular, and maintainable. Javadoc is the project's only code documentation; do not create separate documentation for APIs, architecture, or behavior.

## Project Structure

Use `tree` command to see the project.

Keep responsibilities separated:

* `model`: domain objects
* `repository`: SQLite/JDBC persistence
* `service`: application/business logic
* `controller`: JavaFX/FXML interaction

## Tools and Packages

* **Java / Maven** — language and build system
* **JavaFX / FXML** — UI
* **Scene Builder** — FXML design
* **ControlsFX** — additional JavaFX controls
* **Ikonli** — icons
* **SQLite JDBC** — persistence
* **SLF4J + Logback** — logging

Do not add, remove, modify, or upgrade dependencies. Dependency changes are done by the developer.

## Rules

### Code

* Keep code simple, direct, and readable.
* Prefer clear names and small, focused classes/methods.
* Avoid unnecessary abstractions, patterns, helpers, or clever code.
* Keep UI, business logic, and persistence separated.
* Do not introduce unrelated refactoring or changes.

### Documentation and comments

* Use complete, accurate Javadoc for important non-obvious behavior.
* Use comments sparingly. Only comment code that is complex, unusual, non-obvious, or worth explicitly noting.
* Prefer clearer code over explanatory comments.

### Database

* Keep SQLite access inside the repository/persistence layer.
* Preserve the portability of the `.db` file.
* Do not introduce unnecessary database complexity.

### Logging

* Log meaningful events and failures, not routine operations everywhere.
* Never log secrets or unnecessary sensitive data.
* Application code should use SLF4J, not Logback APIs directly.

### Git

Each commit must contain **one clear and specific topic**.

Commit messages must have:

```text
type(optional scope): summary

Description explaining:
- what changed
- how behavior is different
- what problem was solved
- anything important to remember or noticeable if exists
```

* Do not mix unrelated changes in one commit.
* Do not commit changes unless explicitly told every time.

### Dependencies and Build

* Do not add or remove or modify any dependency or anything related to dependencies.
* Do not modify `pom.xml`, unless you've explicitly been told.