# Fito Gen

**Fito Gen** is a desktop application for plant nurseries focused on:
- plant inventory,
- plant batch tracking,
- contrahent management,
- phytosanitary document workflows,
- numbering,
- reference-data administration,
- backup and technical settings.

The project is developed as a layered commercial-style desktop application, not a demo.

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
- `model` contains CRUD/UI entities such as `Plant`, `PlantBatch`, `Document`, `DocumentItem`, `Contrahent`, `AppUser`
- `domain` contains numbering rules such as `NumberingConfig`, `NumberingType`, `NumberingSectionType`

## Current Settings scope

`Settings` currently covers:
- plant settings,
- numbering,
- document types,
- shared country dictionary,
- users,
- CSV import/export preview for Plants and Contrahents,
- read-only Audit Log,
- backup,
- issuer profile.

## Current Updates scope

`Updates` is reserved for:
- future application update,
- future `Server Update` for:
  - Plants,
  - EPPO,
  - shared country dictionary.

`Updates` is **not** the place for local CSV import/export.

## Current CSV direction

Local CSV workflows are handled in **Settings**.

Implemented direction:
- Plants: import preview + export
- Contrahents: import preview + export

Planned later:
- Documents CSV
- broader validation and dry-run support

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

## Product notes

- document list printing should open preview first,
- print/PDF actions belong to preview,
- active documents should not use cancelled plant batches,
- shared country dictionary is one source for Contrahents, EPPO, and issuer data,
- Help should stay outside `Settings`.

## Stability notes for further work

Pay extra attention to recurring regression risks:
- CSV escaping with double quotes and newline characters in Java strings,
- fragile FXML properties that may break JavaFX loader coercion,
- avoid large controller rewrites when only a local change is needed.
