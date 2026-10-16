# VLStream Cloud v1.2.7

Built from the current mainline at `d4a39cb1`, with the production Flyway history
continued from v1.2.6.

## Highlights

- Add a unified protocol-aware device square with clearer source-availability
  status and safer handling of offline devices.
- Expand tenant-scoped GPU compute with company management, shared task queues,
  provider-backed inventory and guarded marketplace endpoints.
- Persist and validate training datasets asynchronously; improve model artifact
  handling and add an optional isolated CPU worker for Hi3519DV500 OM conversion.
- Harden local-account and tenant identity flows, and enforce tenant-scoped event
  media access and deletion.
- Improve workflow guidance and localization across the console.

## Compatibility and database upgrades

- Deploy WVP separately using [VLStream WVP Lite v1.0.8](https://github.com/OortCloudGroup/VLStream-Cloud-Lite/releases/tag/v1.0.8),
  image `ghcr.io/oortcloudgroup/vlstream-cloud-lite:1.0.8`. WVP remains the
  device center and owns ZLMediaKit; the VLS Compose project does not start a
  second WVP or ZLMediaKit stack.
- Backend and frontend images:
  `ghcr.io/oortcloudgroup/vlstream-backend:1.2.7` and
  `ghcr.io/oortcloudgroup/vlstream-frontend:1.2.7`.
- Production upgrades continue the public v1.2.6 Flyway history and apply
  `V1_2_0_029` through `V1_2_0_048`. `prod` selects
  `db/migration/release`; `dev` and `local` retain the separate mainline
  history. Never change migration locations for a database that already has
  Flyway history.
- Fresh installations import the sanitized VLS bootstrap schema and the
  reviewed WVP v1.0.8 schema; Flyway then applies the release migrations. The
  deployment archive includes the optional OM-conversion Compose overlay, but
  not vendor SVP/ATC software or its licenses.

Cloud-compute behavior depends on tenant configuration, provider access, network
reachability and the target GPU runtime. The optional OM worker is isolated and
disabled unless explicitly enabled; successful conversion still requires
device-side validation. Release checks do not establish model quality or
compatibility for every GPU or device.
