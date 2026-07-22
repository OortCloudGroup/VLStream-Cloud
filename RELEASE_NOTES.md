# VLStream Cloud v1.1.1

This patch release is rebuilt directly from `origin/main` and fixes the issues found during a clean Docker Compose deployment of v1.1.0.

## Fixes

- Removed all legacy database views from the public MySQL initialization script. These views referenced private `apaas_*` databases and are not used by the standalone application.
- Kept backend and frontend application source aligned with `origin/main`; no historical release business patches are included.
- Added checksummed release-build copies of the SmartJavaAI 1.1.0 artifacts used by main, avoiding incompatible binaries currently resolving under the same public coordinates.
- Supplied standalone-only settings through Compose so startup does not require private XXL Job or unified messaging configuration.
- Normalized Windows-tolerated import casing only inside the ephemeral Linux frontend build context.
- Added release validation that rejects external database references and conflicting backend logging bindings.

## Deployment

- New installations still use the supplied schema at `sql/init/10-oortcloud-workflowforms-vls.sql`; only unused legacy views are omitted.
- Run `docker compose up -d` after extracting the release archive.
- Existing v1.1.0 installations should deploy v1.1.1 images and recreate the backend and frontend containers. Back up the database before applying any upgrade SQL.
