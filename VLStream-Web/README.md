# VLStream-Web

> VLStream Cloud — AI-driven open-source Video IoT platform frontend

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)
[![Vue](https://img.shields.io/badge/Vue-3.3-42b883.svg)](https://vuejs.org/)
[![Vite](https://img.shields.io/badge/Vite-4.4-646cff.svg)](https://vitejs.dev/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.3-409eff.svg)](https://element-plus.org/)

VLStream-Web is a high-performance, AI-driven open-source web video platform that transforms traditional video surveillance into intelligent visual management. It combines low-latency streaming, powerful AI analysis, and flexible scalability to provide a robust foundation for building smart, efficient, and secure video IoT systems.

This repository contains the **frontend project** of the platform (located in [`VLStream-ui/`](./VLStream-ui)), built with Vue 3 + Vite, and integrates with a backend gateway and unified identity authentication (SSO) based on the [SpringBlade](https://bladex.cn/) microservice framework.

---

## Table of Contents

- [Core Features](#core-features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Functional Modules](#functional-modules)
- [Quick Start](#quick-start)
- [Environment Configuration](#environment-configuration)
- [Build & Deployment](#build--deployment)
- [Architecture & Authentication](#architecture--authentication)
- [License](#license)

---

## Core Features

- 🎥 **Low-Latency Streaming** — Supports multi-protocol ingestion (RTSP / HLS / WebRTC) with a built-in video square, multi-pane layouts, PTZ control, and video playback.
- 🤖 **AI Analysis** — Algorithm warehouse management, algorithm training / annotation / model management, analysis requests and results, event management, and scene governance.
- 🗺️ **GIS Integration** — Leaflet-based map visualization for an at-a-glance spatial view of devices and cameras.
- 🧩 **AI Compute Scheduling** — Container instance management for elastic scheduling of inference resources.
- 🔐 **Enterprise-Grade Permissions** — Integrates with a unified user platform (SSO) and ships a complete RBAC system covering users, roles, menus, departments, posts, tenants, data scopes, and API scopes.
- 🌐 **Out of the Box** — Element Plus + Avue form/table engine, CRUD mixins, and a large library of reusable business components.

## Tech Stack

| Category | Technology |
| --- | --- |
| Framework | [Vue 3](https://vuejs.org/) (Composition API) |
| Build Tool | [Vite 4](https://vitejs.dev/) |
| UI Library | [Element Plus](https://element-plus.org/) + [@smallwei/avue](https://avuejs.com/) |
| State Management | [Pinia](https://pinia.vuejs.org/) |
| Routing | [Vue Router 4](https://router.vuejs.org/) |
| HTTP | [axios](https://axios-http.com/) (multi-instance with interceptors) |
| Video Playback | [hls.js](https://github.com/video-dev/hls.js/), OPlayer |
| Map | [Leaflet](https://leafletjs.com/) |
| Encryption | [sm-crypto](https://github.com/JuneAndGreen/sm-crypto) (Chinese national cryptography SM2/SM4) |
| Others | markdown-it, nprogress, vue-i18n |

## Project Structure

```
VLStream-Web/
├── LICENSE                         # MIT License
├── README.md                       # This document
└── VLStream-ui/                    # Frontend project root
    ├── index.html                  # Application entry HTML
    ├── package.json                # Dependencies and scripts
    ├── vite.config.js              # Vite config (base path, dev proxy)
    ├── nginx.conf.example          # Production Nginx config example
    ├── build-prod.bat              # Windows one-click build script
    ├── scripts/                    # Deployment / refactoring helpers
    ├── public/                     # Static assets (OPlayer, etc.)
    └── src/
        ├── main.js                 # Application entry
        ├── App.vue                 # Root component
        ├── router/                 # Routing + global guards (incl. SSO auth)
        ├── layout/                 # Global layout shell
        ├── api/                    # Backend API wrappers (per module)
        ├── views/                  # Pages (grouped by domain, see below)
        ├── components/             # Globally reusable components
        ├── utils/                  # Utilities (request, auth, token sync, etc.)
        ├── mixins/                 # Common mixins (CRUD, etc.)
        └── assets/                 # Images, styles, and other static assets
```

`src/views/` domain breakdown:

| Directory | Domain |
| --- | --- |
| `VideoAggregation/` | Video aggregation: video square, device management, video playback, monitoring alarms, tag management, camera settings, analysis results |
| `DecisionAI/` | Decision AI: event management, scene governance, analysis requests, design drawings, scene editing, algorithm configuration, task configuration |
| `AlgorithmWarehouse/` | Algorithm warehouse: algorithm management, algorithm training platform |
| `AlgorithmTraining/` | Algorithm training: annotation, models, training jobs, SSH connections |
| `AIComputingScheduling/` | AI compute scheduling: container instances |
| `System/` | System management: users, roles, menus, departments, posts, tenants, data scopes, API scopes |

## Functional Modules

- **Workspace** — Platform overview dashboard
- **Video Square / Device Management** — Multi-pane live video, device onboarding and organization
- **Video Playback / Monitoring Alarms** — Historical recording retrieval and playback, real-time alarm handling
- **Event Management / Scene Governance** — AI event reception, scenario-based governance orchestration
- **Intelligent Analysis (Requests / Results)** — Submit analysis tasks and view results
- **Algorithm Management / Training Platform** — Algorithm publishing and training task scheduling
- **Annotation / Models / Training** — Dataset annotation, model management, training jobs, SSH remote connections
- **Container Instances** — Compute container scheduling and monitoring
- **System Management** — Complete RBAC permissions and tenant system
- **User Profile / Open Service Center**

## Quick Start

### Prerequisites

- [Node.js](https://nodejs.org/) **16+** (18 LTS recommended)
- npm (bundled with Node), or pnpm / yarn
- Backend services: the SpringBlade gateway listens on `http://127.0.0.1:8080` by default (overridable via environment variables)

### Install & Run Locally

```bash
# 1. Enter the frontend project directory
cd VLStream-ui

# 2. Install dependencies
npm install

# 3. Start the dev server (port 3000 by default, listens on all interfaces)
npm run dev
```

Once started, visit: <http://localhost:3000/>

> In dev mode, all backend requests (prefixed with `/system`, `/blade-auth`, `/vls`, `/api`, `/sso`, etc.) are proxied to the backend via Vite, targeting `http://127.0.0.1:8080` by default.

### Other Scripts

```bash
npm run build     # Build for production, output in dist/
npm run preview   # Preview the production build locally
npm run lint      # ESLint check with auto-fix
```

On Windows, you can also run the one-click build script:

```bash
# From the VLStream-ui directory
build-prod.bat
```

## Environment Configuration

The project is configured via Vite environment variables. Create a `.env.local` file in the `VLStream-ui/` directory (not committed to Git) to override values as needed:

| Variable | Description | Default |
| --- | --- | --- |
| `VITE_DEV_PROXY_TARGET` | Dev backend proxy target | `http://127.0.0.1:8080` |
| `VITE_SSO_PROXY_TARGET` | Dev SSO proxy target | `http://oort.oortcloudsmart.com:21410/bus/apaas-sso` |
| `VITE_API_BASE_URL` | Production API base URL | `http://oort.oortcloudsmart.com:21410/bus/vls-server` |
| `VITE_BLADE_CLIENT_AUTH_HEADER` | OAuth Client Basic auth header | `Basic c2FiZXI6c2FiZXJfc2VjcmV0` |

Example `.env.local`:

```ini
# Proxy dev requests to a local backend
VITE_DEV_PROXY_TARGET=http://127.0.0.1:8080
```

## Build & Deployment

### 1. Build the Production Bundle

```bash
cd VLStream-ui
npm run build
```

Output directory: `VLStream-ui/dist/`

> ⚠️ Note: The production build uses a fixed `base` path of `/bus/vls-ui/` (see `vite.config.js`). Ensure your deployment path matches it.

### 2. Deploy with Nginx

Deploy all files from `dist/` to an Nginx static directory (e.g., `/usr/share/nginx/html/`), and configure the site following [`VLStream-ui/nginx.conf.example`](./VLStream-ui/nginx.conf.example):

```nginx
server {
    listen 21410;
    server_name your-domain.com;

    # Frontend static assets
    location /bus/vls-ui/ {
        alias /usr/share/nginx/html/;
        try_files $uri $uri/ /bus/vls-ui/index.html;
        # Enable the following cross-origin isolation headers if you need
        # multi-threaded video processing:
        # add_header Cross-Origin-Embedder-Policy "require-corp";
        # add_header Cross-Origin-Opener-Policy "same-origin";
    }

    # Proxy backend gateway API (vls-server)
    location /bus/vls-server/ {
        proxy_pass http://your-backend-gateway:18080/;
        # ... proxy_set_header / CORS headers, see example config
    }

    # Proxy unified user platform API (apaas-sso)
    location /bus/apaas-sso/ {
        proxy_pass http://your-backend-gateway:18080/;
        # ...
    }
}
```

Deployment notes:

- The static site must be served under the `/bus/vls-ui/` root path, with History-mode `try_files` fallback to `index.html`.
- The backend gateway and SSO gateway are exposed via Nginx reverse proxy to avoid frontend CORS issues.
- To enable `SharedArrayBuffer` (multi-threaded video decoding), configure the `COEP` / `COOP` response headers.

## Architecture & Authentication

### Backend Architecture

This is a frontend-only project; all business capabilities rely on backend services:

- **Backend Framework**: SpringBlade microservices (`blade-auth` for auth, `blade-system` for system, `blade-job` for scheduling, `vls-*` for business services, etc.).
- **Unified Authentication (SSO)**: Integrates with the `apaas-sso` unified user platform, supporting external platform session pass-through, URL-token redirects, and multi-level local token cache fallback.
- **Gateway**: In production, all services are exposed through Nginx reverse proxy under `/bus/vls-server/`.

### Authentication Mechanism

`src/utils/request.js` defines three axios instances (business requests, image uploads, auth requests) that uniformly handle:

- **Header Injection**: Automatically injects `Authorization`, `blade-auth`, and `accesstoken` for SpringBlade JWT compatibility.
- **OAuth Client Auth**: Uses a Basic Client auth header when obtaining a token at login.
- **Multi-Level Token Lookup**: URL params → sessionStorage → localStorage.
- **Unified Error Handling**: Recognizes SpringBlade `success: false` business errors and surfaces localized messages based on HTTP status codes.

`src/utils/` also contains a full suite of cross-system token sync utilities (`tokenSync`, `crossSystemTokenSync`, `cookieTokenSync`, `storageEventSync`, etc.) that keep the login state in sync with the external unified user platform.

## License

This project is open-sourced under the [MIT License](./LICENSE).

Copyright (c) 2026 VLStream
