# Fito Gen

**Fito Gen** is a commercial-style desktop application for plant nurseries focused on:
- plant inventory,
- plant batch tracking,
- contrahent management,
- phytosanitary document workflows,
- numbering,
- shared reference data,
- backup,
- auditability,
- local CSV import/export,
- future application and server updates.

The project is developed as a layered production-oriented desktop application, not a demo.

## Tech stack

- Java 21
- JavaFX
- SQLite
- Maven

## Current modules

- Dashboard
- Plants
- Plant Batches
- Contrahents
- Documents
- EPPO Admin
- Settings
- Updates
- Help

## Architecture

The project keeps the layered structure:
- `model`
- `repository`
- `service`
- `ui/controller`
- `ui/router`
- `resources/view`
- `resources/styles`

Important separation:
- `model` contains CRUD/UI entities such as `Plant`, `PlantBatch`, `Document`, `DocumentItem`, `Contrahent`, `AppUser`, `AuditLogEntry`
- `domain` contains numbering rules such as `NumberingConfig`, `NumberingType`, `NumberingSectionType`

## Current module direction

### Documents

Current direction:
- documents have status support,
- document list printing opens preview first,
- print and PDF actions belong to preview,
- document types are managed in `Settings`,
- active documents must not use cancelled plant batches.

### Plant Batches

Current direction:
- plant batches have status-aware workflows,
- batch numbering uses dedicated logic,
- cancellation is guarded against active document usage.

### Plants

Current direction:
- CRUD and search are part of the module,
- `visibilityStatus` is part of the model,
- passport requirement and informational EPPO code are supported,
- plant settings remain managed through `Settings`.

### EPPO Admin

Current direction:
- EPPO remains the reference-data layer,
- one EPPO code can be related to multiple species and multiple countries/zones,
- shared countries are not managed in EPPO Admin,
- the full shared country dictionary stays in `Settings`.

## Current Settings scope

`Settings` currently covers:
- plant settings,
- numbering,
- document types,
- shared country dictionary,
- users,
- issuer profile,
- backup,
- CSV import/export preview for Plants, Contrahents and Documents,
- read-only Audit Log.

Recent UX direction in `Settings`:
- document types have local filtering and summary,
- shared country dictionary has local filtering and summary for custom entries,
- issuer profile shows completeness and dictionary consistency feedback,
- Audit Log stays read-only but supports filtering and clearer summaries,
- CSV preview gives readiness-oriented validation messaging.

`Help` should remain outside `Settings`.

## Current Updates scope

`Updates` is reserved for:
- future application update,
- future `Server Update` for:
  - Plants,
  - EPPO,
  - shared country dictionary.

Current direction of the screen:
- local readiness and dry-run style preview,
- no local CSV import/export,
- no mixing of Settings CSV workflows into Updates,
- readiness preview for Plants, EPPO and shared country dictionary.

## Current CSV direction

Local CSV workflows are handled in **Settings -> Import / Export CSV**.

Implemented direction:
- Plants: import preview + export
- Contrahents: import preview + export
- Documents: import preview + export

Planned later:
- broader validation
- richer dry-run support before final import actions

## Shared country dictionary

The shared country dictionary is one common source used by:
- Contrahents,
- EPPO,
- issuer profile data in `Settings`.

Direction:
- full dictionary management belongs to `Settings`,
- base catalog + custom user entries,
- custom entries should remain auditable,
- do not duplicate the full dictionary in EPPO Admin.

## Audit Log

Audit Log backend exists and the current UI in `Settings` is read-only.
The intended long-term coverage includes:
- Plants
- Contrahents
- PlantBatches
- Documents
- AppSettings
- DocumentType
- AppUser
- shared country dictionary
- EPPO and its assignments

## Stability notes for further work

Pay extra attention to recurring regression risks:
- CSV escaping with double quotes and newline characters in Java strings,
- fragile FXML properties that may break JavaFX loader coercion,
- avoid large controller rewrites when only a local change is needed,
- do not mix `Updates` with local CSV workflows,
- do not move `Help` back into `Settings`.
