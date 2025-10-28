# Changelog

## [0.4.0] - Dev

### Added

- Support for file upload fields, including disk storage and byte array handling, with annotation-based configuration.
- Support for property filtering for list views, supporting custom field selection via configuration.
- Support for annotation-based configuration for file upload, primary key and password fields.
- Support for dark mode in the admin interface, with a toggle switch.

### Changed

- Replaced Exposed's entity classes with table objects for querying and data manipulation.
- Improved header ordering for list views to prioritise primary key fields.

### Fixed

- Resolved issue with serialisation of date/time fields in MongoDB entities.

## [0.3.2] - 2025-08-27

### Fixed

- Corrected session validation logic to ensure proper authentication checks.
- Fixed `MongoDao` to support entities with immutable (`val`) IDs, ensuring correct persistence and retrieval.
- Fixed `MongoDao`'s `find` method to properly return the document from the `MongoAdminUser` collection.
- Fixed `MongoDao`'s `createTable` method to use the actual entity class name for the collection, instead of the
  reflection class name.
- Fixed creation logic for MongoDB and JPA entities.
- Improved password hashing logic: Password fields are now dynamically detected and securely hashed before saving,
  supporting multiple common password field names.
- Ensured that LocalDateTime fields are correctly detected and mapped to the appropriate HTML input type (
  datetime-local) in form generation logic for MongoDB entities.

## [0.3.1] - 2025-08-10

> Note: Versions prior to 0.3.1 had dependency resolution issues and are not installable. Please use 0.3.1 or newer.

### Changed

- Removed MongoDB BOM from build and dependency management.
- Declared explicit versions for all MongoDB-related dependencies to ensure correct POM generation and avoid leaking
  platform constraints.
- Improved Gradle publishing configuration for better Maven Central compatibility.
- Fixed potential dependency resolution issues for consumers.

## [0.3.0] - 2025-08-10

### Added

- Support for custom favicon via configuration.
- Default favicon included in static resources.
- Abstracted template rendering via a `TemplateRenderer` interface, allowing support for multiple template engines (
  e.g., Mustache, FreeMarker, Thymeleaf, etc.) instead of being limited to Mustache.

### Changed

- Added `<!DOCTYPE html>` to HTML output to ensure standards mode and avoid quirks mode in browsers.
- Updated all view rendering logic to use the configured `TemplateRenderer`, making the library engine-agnostic and
  extensible.
- Switched from CDN to locally compiled Tailwind CSS.
- Configured Tailwind CLI to purge unused styles and minify output.
- Updated template files to reference local tailwind.min.css.

## [0.2.0] - 2025-08-03

### Added

- Logout functionality to the admin interface.

## [0.1.1] - 2025-07-31

### Fixed

- Fixed bug in delete endpoint that caused incorrect entity deletion.

## [0.1.0] - 2025-07-28

### Added

- Initial release of Ktor Panel.
- Admin interface generation for Ktor servers.
- Support for Exposed, Hibernate, and MongoDB backends.
- Basic authentication and configuration features.