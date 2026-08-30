<p align="center">
  <img src="appdooni.png" width="128" alt="AppDooni icon">
</p>

<h1 align="center">AppDooni</h1>

<p align="center">Desktop catalog for applications across operating systems.</p>

<p align="center">
  <a href="https://github.com/ferigeek/appdooni"><img alt="license" src="https://img.shields.io/badge/license-Apache--2.0-blue"></a>
  <a href="https://openjfx.io"><img alt="javafx" src="https://img.shields.io/badge/JavaFX-21-3a75c4"></a>
  <a href="https://www.sqlite.org"><img alt="sqlite" src="https://img.shields.io/badge/SQLite-3-lightgrey"></a>
</p>

AppDooni is a JavaFX desktop application for cataloging applications with their operating systems and tags. Data is stored locally in a portable SQLite `.db` file that is also the import/export format.

## Features

- **Applications** — add, edit, view details, search across name, OS, tag, description, source and website.
- **Operating systems** — add, rename, remove. Removing an OS deletes apps exclusive to it, others keep remaining OSs. Every app requires at least one OS.
- **Tags** — add, rename, remove (removes association only), assign multiple tags per app, search tags.
- **Filtering** — filter by OS tab (`All` plus one per OS), by one or more tags with `AND`/`OR` toggle, combined with free-text search. `Clear` button resets tag selection.
- **Import / Export** — export a copy as `appdooni-<timestamp>.db`, import merges into current DB with conflict resolution (`Skip`, `Overwrite`, `Keep both`).
- **Logs** — visible in the bottom pane and written to a file beside the database.

## Tech Stack

Java 25 · Maven · JavaFX 21 / FXML (Scene Builder) · ControlsFX · Ikonli (FontAwesome 5, Material 2) · SQLite JDBC · SLF4J + Logback · JUnit 5

## Requirements

- JDK 25
- Maven 3.9+ (or use `./mvnw` wrapper)

## Running

```bash
./mvnw clean javafx:run
# or
mvn clean javafx:run
```

Run tests:

```bash
./mvnw test
```

Generate Javadoc (sole code documentation):

```bash
./mvnw javadoc:javadoc
# output: target/reports/apidocs/
```

Package runtime image:

```bash
./mvnw clean javafx:jlink
# output: target/app/
```

## Database & Logs

Stored in the OS-standard application directory:

| OS | Location |
|---|---|
| Linux | `~/.local/share/appdooni/appdooni.db` |
| Windows | `%LOCALAPPDATA%\appdooni\appdooni.db` |
| macOS | `~/Library/Application Support/appdooni/appdooni.db` |

Logs are written to `appdooni.log` in the same directory and mirrored to the **Logs** pane. See `src/main/resources/db.sql` for schema and default OS seed data (`Linux`, `Windows`, `macOS`, `BSD`, `iOS`, `Android`).

## Project Structure

```
src/main/java/com/github/ferigeek/appdooni/
  model/        domain objects (Application, OperatingSystem, Tag)
  repository/   SQLite/JDBC persistence (DatabaseManager)
  service/      business logic and validation
  controller/   JavaFX/FXML interaction (MainController)
  App.java      JavaFX entry point, sets window icon
  Launcher.java non-JavaFX launcher
src/main/resources/
  com/github/ferigeek/appdooni/appdooni.fxml
  com/github/ferigeek/appdooni/appdooni.png   app icon
  db.sql
  logback.xml
spec/
  requirements.md  functional spec
  ui.md            layout spec
  ERD.puml         database ERD
```

See `AGENTS.md` for contributor rules.

## Icon

Icon source is `appdooni.svg` (vector) rendered to `appdooni.png` (800×880) bundled as the window and About dialog icon via `App` and `MainController`.

## License

Apache License 2.0 — see [LICENSE](LICENSE).
