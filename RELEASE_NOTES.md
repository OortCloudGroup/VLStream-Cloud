# VLStream Cloud v1.2.10

Built from `origin/main` at `cd43334e`. This release continues the public
v1.2.9 Flyway history.

## Highlights

- Tighten role, menu, and membership authorization, and protect system roles
  from unsafe changes.
- Add trusted workflow ownership records. Existing workflow records are not
  automatically backfilled into the new ownership tables.
- Improve compute billing coordination, recovery, and tenant ownership checks.
  Payment and automatic power-control gates remain disabled by default.
- Improve the multi-camera video square, including drag-and-drop camera
  selection and clearer playback controls.

## Compatibility and database upgrades

- Deploy [VLStream WVP Lite v1.0.11](https://github.com/OortCloudGroup/VLStream-Cloud-Lite/releases/tag/v1.0.11)
  separately. Its public image is
  `ghcr.io/oortcloudgroup/vlstream-cloud-lite:1.0.11`. WVP remains the device
  center and owns ZLMediaKit; this package does not start a second WVP stack.
- WVP v1.0.11 provides cross-protocol source grants used by Cloud AI and
  tenant-scoped event and ONVIF controls. Device-sharing permissions still
  require explicit assignment to the intended roles.
- Backend and frontend images:
  `ghcr.io/oortcloudgroup/vlstream-backend:1.2.10` and
  `ghcr.io/oortcloudgroup/vlstream-frontend:1.2.10`.
- Production upgrades continue v1.2.9's release Flyway history and apply
  `V1_2_0_061` through `V1_2_0_069` in version order (there is no `065`).
  Migration `068` stops safely when the default member role is missing,
  disabled, or ambiguous. Migration `067` creates trusted-workflow ownership
  tables without historical backfill. Development profiles retain the
  separate mainline history.
- Fresh installations use the sanitized VLS and reviewed WVP bootstrap
  schemas; Flyway applies subsequent migrations. The package supports bundled
  MySQL and Redis or externally managed MySQL and Redis.

Compute billing and Cloud AI still depend on tenant permissions, operator
configuration, provider access, network reachability, and the target GPU
runtime. Release checks do not establish real supplier operation, payment
activation, model quality, or compatibility with every GPU or device.
