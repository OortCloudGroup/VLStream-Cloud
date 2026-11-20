# VLStream Cloud v1.2.12

Based on `origin/main` at `f5102657`. This release continues the public v1.2.11 Flyway history.

## Highlights

- Run smart-annotation rounds on selected compute nodes through the durable shared queue, preserve validation splits during retries, and automatically confirm only predictions that meet the task's full validation rules.
- Expand Model Hub workflows with featured local resources, retry-safe publication metadata, verified model deployment, and support for eligible artifacts that do not originate from formal training.
- Bind compute catalogs and workers to authorized platform applications and execution environments; improve paid-run recovery, stock handling, explicit consent, and responsive GPU selection.
- Scale dataset review and annotation with server-side pagination and more reliable import, training, and publication workflows.
- Improve safety-event review, video-square layouts and thumbnails, device-model refresh, RTSP location editing, and platform-realm session isolation.
- Strengthen SM2 login-password encryption by upgrading `sm-crypto` to a CSPRNG-backed release.

## Compatibility and database upgrades

- Deploy the latest final VLStream WVP Lite release, v1.0.12, separately. Its public image is `ghcr.io/oortcloudgroup/vlstream-cloud-lite:1.0.12`; WVP remains the authoritative device directory and owns ZLMediaKit. See the [WVP v1.0.12 release](https://github.com/OortCloudGroup/VLStream-Cloud-Lite/releases/tag/v1.0.12).
- WVP v1.0.12 scopes federated access to grants verified by VLStream and restricts raw-address ONVIF and federated device operations. Its release introduces no Flyway migration; the schema lineage remains through `V1_2_16`.
- Backend and frontend images: `ghcr.io/oortcloudgroup/vlstream-backend:1.2.12` and `ghcr.io/oortcloudgroup/vlstream-frontend:1.2.12`.
- Upgrades from v1.2.11 apply release migrations `V1_2_0_074` through `V1_2_0_076` in order. V074 links smart-annotation rounds to shared compute execution; V075 adds Model Hub publication state and permits independently verified artifacts; V076 records the identity and metadata snapshot of each publication request.
- Fresh installations use the sanitized VLS and reviewed WVP bootstrap schemas; Flyway applies subsequent migrations. Production uses the release migration history, while development keeps its separate mainline history. The package supports bundled or externally managed MySQL and Redis.

Public GitHub Actions artifacts omit private compute-billing configuration and supplier credentials. Paid supplier integrations require approved private runtime configuration; see `docs/COMPUTE_BILLING.md` in the release archive. Provider operation, automatic power control, model quality, and device behavior also depend on tenant permissions, operator configuration, external services, network reachability, and target hardware.
