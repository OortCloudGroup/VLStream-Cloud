# VLStream Cloud v1.2.6

Built from `origin/main` at `dadb04a1`. This release adds persistent training
artifacts and cloud-compute workflows while retaining the public v1.2.5 database
history for production upgrades.

## Highlights

- Persist complete annotation datasets and verified model artifacts in MinIO,
  with immutable dataset references, checksum validation, retry states, and
  compatibility with existing SSH-based training data.
- Add tenant-scoped AutoDL compute nodes, encrypted credentials, background
  training lifecycle management, cancellation safeguards, and model/artifact
  return workflows.
- Improve algorithm/model import and annotation type handling, plus prevent
  real-time previews for offline devices.
- Extend IPC tunnel session handling. The backend control plane is enabled by
  default; the tunnel runtime remains an optional Compose profile.
- Keep all production secrets environment-backed and default GPU/SSH endpoints
  unset in the released runtime configuration.

## Compatibility and database upgrades

- WVP is deployed separately using
  [VLStream WVP Lite v1.0.8](https://github.com/OortCloudGroup/VLStream-Cloud-Lite/releases/tag/v1.0.8),
  image `ghcr.io/oortcloudgroup/vlstream-cloud-lite:1.0.8`. WVP remains the
  device center and owns ZLMediaKit.
- Backend and frontend images:
  `ghcr.io/oortcloudgroup/vlstream-backend:1.2.6` and
  `ghcr.io/oortcloudgroup/vlstream-frontend:1.2.6`.
- Production upgrades continue the public v1.2.5 Flyway history and apply
  `V1_2_0_024` through `V1_2_0_028`. `prod` selects
  `db/migration/release`; `dev` and `local` retain the separate mainline history.
  Never change migration locations for a database that already has Flyway
  history.
- Fresh installations import the sanitized VLS schema and the reviewed WVP
  v1.0.8 bootstrap schema. The VLS Compose project does not run WVP or
  ZLMediaKit services.

Cloud training and AutoDL behavior depends on tenant configuration, network
access, and the target GPU runtime. The included unit, integration, and release
build checks do not establish model-quality or compatibility for every GPU
type.
