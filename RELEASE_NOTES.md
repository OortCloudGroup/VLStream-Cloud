# VLStream Cloud v1.2.0

This release is built from the current `origin/main` baseline and extends the
one-command deployment with the WVP protocol backend and ZLMediaKit.

## Highlights

- Added independent WVP and ZLMediaKit containers for GB28181, ONVIF, and RTSP workflows.
- Added same-origin frontend routing through `/bus/wvp-server/`; browser requests no longer depend on a separately configured WVP address.
- Added VLStream token federation between the WVP backend and the primary backend.
- Added an independent `ry-wvp` database in the shared MySQL instance and Redis database index `10`.
- Added Flyway-managed WVP database initialization and future upgrades.
- Removed device records, media-node records, local network addresses, and third-party keys from the public WVP baseline.
- Kept Linux ISUP and Dahua native SDK listeners disabled by default until their vendor runtimes are installed and verified.

## Deployment

- Default images are `ghcr.io/oortcloudgroup/vlstream-backend:1.2.0`, `ghcr.io/oortcloudgroup/vlstream-frontend:1.2.0`, and `ghcr.io/oortcloudgroup/vlstream-wvp-backend:1.2.0`.
- Extract the release archive, copy `.env.example` to `.env`, update every `change-me` value, and run `docker compose up -d`.
- For device access from another computer, set `SIP_PUBLIC_IP` and `ZLMEDIAKIT_PUBLIC_HOST` to the Docker host's reachable address and open the documented SIP/media ports.

## Database

- New VLStream installations import `sql/init/10-oortcloud-workflowforms-vls.sql`.
- Compose creates the separate WVP database; the WVP container then initializes and upgrades it with Flyway.
- Existing installations retain their named volumes and receive immutable Flyway migrations when the corresponding backend starts.
- Back up both databases before upgrading. Do not manually execute Flyway migration copies from `sql/upgrade`.
