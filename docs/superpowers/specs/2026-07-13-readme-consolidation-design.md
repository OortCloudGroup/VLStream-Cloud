# README Consolidation Design

## Objective

Consolidate the useful project-level information from
`VLStream-Cloud-Backend-Server/vls-stream/README.md` into the repository-root
`README.md`, while keeping the root document as the primary source and preserving
its English language and VLStream Cloud positioning.

## Scope

- Update only the repository-root `README.md`.
- Delete only `VLStream-Cloud-Backend-Server/vls-stream/README.md` after its
  relevant information has been incorporated.
- Preserve all other module- and component-specific README files.
- Do not change application code, configuration, dependencies, or build output.

## Content Strategy

The merged README will retain the root document's project introduction, video
stream management focus, core business capabilities, API examples, and contact
information. It will incorporate only durable, project-level information from the
backend README:

- the RuoYi-based multi-module backend architecture;
- Flowable workflow support;
- Sa-Token authentication and authorization;
- user, role, permission, and workflow management capabilities;
- Maven profiles, backend configuration locations, startup commands, and Docker
  deployment guidance.

Method-level `SysRoleService` documentation, stale change-log entries, generic
coding guidance, and unverified statements will not be migrated.

## Accuracy Rules

Repository source and configuration take precedence over either existing README.
The consolidated document will use the verified Java 8 baseline, Spring Boot
2.7.11, Flowable 6.8.0, actual Maven modules, `ruoyi-admin` entry point, and real
configuration paths. Commands will be written relative to the repository root or
will explicitly show the required directory change.

## Proposed Structure

1. Project Introduction
2. Technology Stack
3. Repository and Backend Module Structure
4. Core Function Modules
5. Quick Start
6. API Interfaces and Response Format
7. Docker and Environment Notes
8. Contact Information

## Verification

- Confirm the root README is valid Markdown and has no references to the deleted
  backend README.
- Confirm all documented local paths exist.
- Confirm the documented Java and framework versions match `vls-stream/pom.xml`.
- Confirm only the targeted backend README is removed and all other README files
  remain.
- Review the final Git diff for accidental changes. A Java compilation is not
  required because the change is documentation-only.
