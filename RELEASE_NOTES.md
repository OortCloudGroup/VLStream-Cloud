# VLStream Cloud v1.2.13

Based on `origin/main` at `a7d32aa9`. This release continues the public v1.2.12 Flyway history.

## Highlights

- Add conversational video-square scenes with unified map, topology, and BIM views, per-tile preview error isolation, and scene-aware assistant support.
- Add vision-assisted smart annotation, LLM annotation-quality review, and scoped event LLM review with tenant-aware record visibility.
- Display authorized media and event locations in tenant event management, and stabilize tenant navigation and session recovery across platform realms.
- Coordinate shared compute execution with explicit lease release for both regular and paid runs, keep paid instances running during model transfer, and clean up shared training workspaces.
- Add tenant-aware public upload links, share preset algorithm catalogs across tenants, and seed built-in algorithm covers while keeping model records scoped to trusted tenants.
- Improve camera playback and recording controls, reduce preview pressure, and refresh device-model state more reliably.
- Sync default member menu grants once per tenant at startup and after menu changes, excluding the system management module.
- Enable the CPU OM conversion queue by default, gate IPC remote tunnel sessions on device online state, add a guarded database backup utility, and add a native Windows deployment path without Docker.

## Compatibility and database upgrades

- Deploy the latest final VLStream WVP Lite release, v1.0.12, separately. Its public image is `ghcr.io/oortcloudgroup/vlstream-cloud-lite:1.0.12`; WVP remains the authoritative device directory and owns ZLMediaKit. See the [WVP v1.0.12 release](https://github.com/OortCloudGroup/VLStream-Cloud-Lite/releases/tag/v1.0.12).
- WVP v1.0.12 scopes federated access to grants verified by VLStream and restricts raw-address ONVIF and federated device operations. Its release introduces no Flyway migration; the schema lineage remains through `V1_2_16`.
- Backend and frontend images: `ghcr.io/oortcloudgroup/vlstream-backend:1.2.13` and `ghcr.io/oortcloudgroup/vlstream-frontend:1.2.13`.
- Upgrades from v1.2.12 apply release migrations `V1_2_0_077` through `V1_2_0_087` in order: V077 adds the smart-annotation vision API contract; V078 and V079 release execution leases and pin accepted paid runs to their original coordinator and node; V080 tracks shared training workspace cleanup; V081 adds tenant-aware public upload links; V082 records smart-annotation LLM quality reviews; V083 records scoped event LLM reviews; V084 persists video-square scene definitions; V085 records which default member menu grants were already synchronized per tenant; V086 seeds built-in algorithm cover assets; V087 registers the shared preset algorithm catalog.
- Fresh installations use the sanitized VLS and reviewed WVP bootstrap schemas; Flyway applies subsequent migrations. Production uses the release migration history, while development keeps its separate mainline history. The package supports bundled or externally managed MySQL and Redis.

Public GitHub Actions artifacts omit private compute-billing configuration and supplier credentials. Paid supplier integrations require approved private runtime configuration; see `docs/COMPUTE_BILLING.md` in the release archive. Provider operation, automatic power control, model quality, and device behavior also depend on tenant permissions, operator configuration, external services, network reachability, and target hardware.
