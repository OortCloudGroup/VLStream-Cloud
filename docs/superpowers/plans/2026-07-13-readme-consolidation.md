# README Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce one authoritative repository-level README by enriching the existing English root document with verified backend architecture and workflow information, then remove the superseded backend README.

**Architecture:** Treat the root README as the source document and repository code/configuration as the source of truth. Curate project-level information from the backend README, correct stale paths and versions, and preserve all unrelated module READMEs.

**Tech Stack:** Markdown, Maven multi-module metadata, Spring Boot 2.7.11, Java 8, MyBatis-Plus 3.5.3.1, Sa-Token 1.34.0, Flowable 6.8.0, Git validation commands.

---

## File Map

- Modify `README.md`: authoritative English project overview, architecture, features, setup, API examples, Docker notes, and contact details.
- Delete `VLStream-Cloud-Backend-Server/vls-stream/README.md`: superseded backend-level overview.
- Preserve every other `README*` file in the repository.

### Task 1: Consolidate the Root Documentation

**Files:**
- Modify: `README.md`

- [x] **Step 1: Retain and refine the project identity**

Keep the VLStream Cloud backend-service title, project introduction, video/device/algorithm/analysis/alert capabilities, and contact information. State that the backend also includes RuoYi-based administration and Flowable workflow support.

- [x] **Step 2: Correct the technology stack from Maven metadata**

Document Java 8, Spring Boot 2.7.11, MyBatis-Plus 3.5.3.1, Sa-Token 1.34.0, Flowable 6.8.0, Redis/Redisson, Springdoc/Knife4j, and Maven. Do not preserve the stale Spring Boot 2.7.18 or FastJSON 2.0.25 claims.

- [x] **Step 3: Replace the obsolete structure tree**

Show the actual top-level `VLStream-Cloud-Backend-Server/vls-stream` and `VLStream-Web` layout. Under `vls-stream`, explain `ruoyi-admin`, `ruoyi-common`, `ruoyi-framework`, `ruoyi-system`, `ruoyi-vlstream`, `ruoyi-flowable`, `ruoyi-generator`, `ruoyi-job`, `ruoyi-oss`, `ruoyi-sms`, `ruoyi-demo`, `ruoyi-extend`, `script`, and `deploy` at project level.

- [x] **Step 4: Integrate durable APaaS capabilities**

Add concise user, role, RBAC/data-permission, and workflow management bullets. Exclude the old `SysRoleService` method inventory, example Java code, placeholder changelog, generic coding rules, and duplicated contact section.

- [x] **Step 5: Rewrite Quick Start with real paths and commands**

Use these repository-root-relative commands and paths:

```powershell
cd VLStream-Cloud-Backend-Server/vls-stream
mvn -ntp -Pdev clean package
mvn -ntp -Pdev -pl ruoyi-admin spring-boot:run
```

Point database initialization to `script/sql/mysql/mysql_ry_v0.8.X.sql`, configuration to `ruoyi-admin/src/main/resources/application.yml` plus profile files, and the packaged application to `ruoyi-admin/target/apaas-workflowforms.jar`. Explain `dev` as default and list `local`/`prod` profiles.

- [x] **Step 6: Correct API examples and response shape**

Use the verified `/vlsDeviceInfo` controller base path, including `/page`, `/{id}`, `/statistics`, and the write endpoints. Show the `R<T>` fields `code`, `msg`, and `data`; remove the unverified `message` and `timestamp` fields. Link API discovery to `/doc.html` and `/swagger-ui.html` without claiming an `/api` context path.

- [x] **Step 7: Add bounded Docker and intranet notes**

Reference `script/docker/docker-compose.yml` and note that Maven repositories or container base images may require the project network/VPN. Keep this advisory concise.

### Task 2: Remove the Superseded Backend README

**Files:**
- Delete: `VLStream-Cloud-Backend-Server/vls-stream/README.md`

- [x] **Step 1: Delete only the approved backend README**

Remove `VLStream-Cloud-Backend-Server/vls-stream/README.md`. Do not remove any frontend, component, image, or Redis data-directory README.

### Task 3: Verify the Documentation-Only Change

**Files:**
- Verify: `README.md`
- Verify absence: `VLStream-Cloud-Backend-Server/vls-stream/README.md`

- [x] **Step 1: Validate whitespace and conflict markers**

Run:

```powershell
git diff --check -- README.md VLStream-Cloud-Backend-Server/vls-stream/README.md
rg -n '<<<<<<<|=======|>>>>>>>' README.md
```

Expected: no whitespace errors and no merge-conflict markers.

- [x] **Step 2: Validate documented local paths**

Confirm these paths exist:

```text
VLStream-Cloud-Backend-Server/vls-stream/pom.xml
VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/application.yml
VLStream-Cloud-Backend-Server/vls-stream/script/sql/mysql/mysql_ry_v0.8.X.sql
VLStream-Cloud-Backend-Server/vls-stream/script/docker/docker-compose.yml
```

Expected: all four paths resolve successfully.

- [x] **Step 3: Validate versions and scope**

Compare the README technology versions with `VLStream-Cloud-Backend-Server/vls-stream/pom.xml`, list all remaining `README*` files, and inspect:

```powershell
git diff -- README.md VLStream-Cloud-Backend-Server/vls-stream/README.md
git status --short
```

Expected: the target README is deleted, unrelated README files remain, and no application source/configuration is changed by this task. Java compilation is intentionally skipped because the patch changes Markdown only.
