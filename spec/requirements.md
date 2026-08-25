# Entities

The only entities in this application are:
- Application, including name and other information.
- Operating System, can be added and removed. Each application could be available for one or more OS.
- Tag, optionally assigned to each app. One app can have multiple tags.

The schema of the database can be found at `resources/db.sql` of the Java project with more detail.
By default, some major operating systems are added.

# Functionalities

User should be able to:
- Add, remove, and edit tags, operating systems, and applications.
- See the list of applications, and filter them based on tag, operating system, or both.
- Search for applications. All the fields of application entity should be used for search; Name, OS, tag, description, source, and website.
- See details of each application.
- Export a copy of the application database with this format: `appdooni-<timestamp>.db`.
- Import a database to be used in application. The content of the imported database should be added to the current database, and not replaced by the previous database.
- The database and configuration of the application should be stored in standard directories of each operating system or the convention that operating system has. For example:
  - Linux: `~/.local/share/appdooni/appdooni.db`
  - Windows: `%LOCALAPPDATA%\appdooni\appdooni.db`
  - macOS: `~/Library/Application Support/appdooni/appdooni.db`
