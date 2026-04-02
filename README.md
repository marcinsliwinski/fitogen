# Fito Gen

**Fito Gen** is a desktop application for plant nurseries focused on **plant inventory**, **batch tracking**, **phytosanitary documents**, and **reference-data administration**.

Built as a commercial-style Java desktop project, it combines a layered architecture with a practical workflow for real operational use.

## What it does

- manages **plants**, **plant batches**, and **contrahents**
- supports **phytosanitary documents** with preview and PDF generation
- provides configurable **numbering** for documents and batches
- includes **settings**, **backup**, **EPPO administration**, **updates readiness**, and **operational help**
- keeps an **Audit Log** for key administrative and business actions

## Main modules

- **Dashboard** — overview entry point
- **Plants** — CRUD, search, visibility status, EPPO info
- **Plant Batches** — CRUD, status flow, soft cancellation, numbering
- **Contrahents** — CRUD with shared country dictionary
- **Documents** — CRUD, status handling, preview-first print flow, PDF export
- **EPPO Admin** — EPPO codes, zones, species links, zone links
- **Settings** — issuer profile, users, document types, numbering, dictionaries, backup, audit log
- **Updates** — operational readiness for future application/data updates and safe import entry
- **Help** — quick-start and day-to-day operational guidance

## Tech stack

- **Java 21**
- **JavaFX**
- **SQLite**
- **Maven**
- **OpenPDF**
- **SLF4J**

## Architecture

The project uses a layered structure and keeps UI concerns separate from business logic:

- `model`
- `domain`
- `repository`
- `service`
- `ui/controller`
- `ui/router`
- `resources/view`
- `resources/styles`

This keeps the codebase maintainable and makes it easier to evolve complex workflows without collapsing everything into controllers.

## Product direction

Fito Gen is being developed as a desktop tool for nurseries that need a structured workflow around:

- traceable plant batches
- phytosanitary document generation
- shared country and EPPO reference data
- business-safe administration and backup
- gradual expansion toward imports, updates, validation, and production hardening

## Current highlights

- layered CRUD flow across the main modules
- configurable numbering services
- document preview and PDF generation
- shared country directory used across multiple areas
- EPPO reference model with codes, species, and zones
- Audit Log wired through core services and settings
- operationalized **Updates** and **Help** modules instead of static placeholders

## Running locally

### Requirements

- Java 21
- Maven 3.9+

### Start the app

```bash
mvn clean javafx:run