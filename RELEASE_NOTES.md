# VLStream Cloud v1.2.11

Built from origin/main at c4ecc206. This release continues the public v1.2.10 Flyway history.

## Highlights

- Add tenant-scoped platform recording with retention policies and clearer recording status.
- Add per-camera cloud algorithm configuration and continuous device orchestration, with tenant-owned evidence and runtime recovery.
- Synchronize shared compute resources from the platform catalog and improve GPU readiness checks, paid-run coordination, billing recovery, and trial isolation.
- Bind OortCloud model authorization to successful login, add model and provider management, and tighten access to federated WVP operations.
- Improve safety-event exports, dataset image review pagination, map and video-square workflows, console sign-in, and English-language coverage.
- Restrict workflow page metadata access and strengthen role, menu, and tenant authorization boundaries.

## Compatibility and database upgrades

- Deploy VLStream WVP Lite v1.0.12 separately. Its public image is ghcr.io/oortcloudgroup/vlstream-cloud-lite:1.0.12. WVP remains the authoritative device directory and owns ZLMediaKit.
- WVP v1.0.12 scopes federated permissions to grants verified by VLStream, restricts raw-address ONVIF and federated device operations, and gates video-square endpoints. Device-sharing permissions still require explicit assignment to intended roles.
- Backend and frontend images: ghcr.io/oortcloudgroup/vlstream-backend:1.2.11 and ghcr.io/oortcloudgroup/vlstream-frontend:1.2.11.
- Upgrades from v1.2.10 apply release migrations V1_2_0_070 through V1_2_0_073 in version order. V070 adds platform recording sources and segments; V071 adds retention settings; V072 adds tenant-scoped continuous camera configuration.
- V073 is a guarded one-time correction for the previously confirmed local test instance. It changes state only when the exact legacy owner, claim, generation, and completed training job still match and no active paid run, training job, or node operation remains. It preserves historical orders and jobs and does not affect other instances.
- Fresh installations use the sanitized VLS and reviewed WVP bootstrap schemas; Flyway applies subsequent migrations. Production uses the release migration history, while development keeps its separate mainline history. The package supports bundled or externally managed MySQL and Redis.

Public GitHub Actions artifacts deliberately omit private compute billing configuration and supplier credentials. Paid supplier integrations require approved private runtime configuration; see docs/COMPUTE_BILLING.md in the release archive. Provider operation, automatic power control, model quality, and device behavior also depend on tenant permissions, operator configuration, external services, network reachability, and target hardware.
