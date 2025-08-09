# Changelog

## [Unreleased] - dev

### Added

- Support for custom favicon via configuration.
- Default favicon included in static resources.
- Abstracted template rendering via a `TemplateRenderer` interface, allowing support for multiple template engines (
  e.g., Mustache, FreeMarker, Thymeleaf, etc.) instead of being limited to Mustache.

### Changed

- Added `<!DOCTYPE html>` to HTML output to ensure standards mode and avoid quirks mode in browsers.
- Updated all view rendering logic to use the configured `TemplateRenderer`, making the library engine-agnostic and
  extensible.

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