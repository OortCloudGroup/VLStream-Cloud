# Agent Guide (apaas-workflowforms)
Operating rules for coding agents in this repository.

## 交互要求
- Thinking思考过程用中文表述
- Reply回答也要用中文回复

## Repo snapshot
- Build system: Maven multi-module (`pom.xml` at repo root)
- Java baseline: JDK 8 (`java.version=1.8`)
- Spring Boot: `2.7.11`
- Major frameworks: MyBatis-Plus, Sa-Token, Flowable
- Default timezone assumptions: `Asia/Shanghai`
- Main modules:
  - `ruoyi-admin` (main boot app)
  - `ruoyi-system` (business services)
  - `ruoyi-framework`, `ruoyi-common` (shared foundation)
  - `ruoyi-flowable`, `ruoyi-job`, `ruoyi-oss`, `ruoyi-sms`, `ruoyi-generator`, `ruoyi-demo`, `ruoyi-extend/*`

## Environment and setup
- Require Maven 3.6+ (no `mvnw` wrapper in repo).
- Root `pom.xml` configures internal repos:
  - `http://192.168.88.218:8403/repository/maven-public/`
- If dependency resolution fails, you may need intranet/VPN or a mirror in `settings.xml`.

## Profiles and filtered config
- Root Maven profiles: `dev` (default), `local`, `prod`.
- `ruoyi-admin/src/main/resources/application.yml` uses filtered placeholders:
  - `spring.profiles.active: @profiles.active@`
  - `logging.level.com.ruoyi: "@logging.level@"`
- Switching profile requires rerunning Maven so filtered resources are regenerated.

## Command reference
### Build
- Full build: `mvn -ntp clean package`
- Full build (skip tests): `mvn -ntp -DskipTests clean package`
- Build main app module + deps: `mvn -ntp -pl ruoyi-admin -am package`
- Build with explicit profile: `mvn -ntp -P{dev|local|prod} -DskipTests clean package`

### Run
- Run main app from source: `mvn -ntp -pl ruoyi-admin spring-boot:run`
- Run with profile: `mvn -ntp -Pprod -pl ruoyi-admin spring-boot:run`
- Run packaged jar: `java -jar ruoyi-admin/target/apaas-workflowforms.jar`
- Override runtime profile at boot: `java -jar ruoyi-admin/target/apaas-workflowforms.jar --spring.profiles.active=dev`
- Other runnable modules (when needed):
  - `mvn -ntp -pl ruoyi-extend/ruoyi-monitor-admin spring-boot:run`
  - `mvn -ntp -pl ruoyi-extend/ruoyi-xxl-job-admin spring-boot:run`

### Tests (JUnit 5 + Surefire groups)
- Run all tests (default profile is `dev`): `mvn -ntp test`
- Run tests with profile tag filtering: `mvn -ntp -Plocal test` or `mvn -ntp -Pprod test`
- Run tests in one module: `mvn -ntp -pl ruoyi-admin test`
- Run a single test class: `mvn -ntp -pl ruoyi-admin -Dtest=DemoUnitTest test`
- Run a single test method: `mvn -ntp -pl ruoyi-admin -Dtest=DemoUnitTest#testTest test`
- Run a tagged single test method (example):
  - `mvn -ntp -pl ruoyi-admin -Pdev -Dtest=TagUnitTest#testTagDev test`
  - `mvn -ntp -pl ruoyi-admin -Plocal -Dtest=TagUnitTest#testTagLocal test`
- Override group directly (advanced): `mvn -ntp -Dprofiles.active=local -Dgroups=local test`

### Test filtering caveat (important)
- Root Surefire config uses:
  - `groups=${profiles.active}`
  - `excludedGroups=exclude`
- Practical behavior:
  - Only tests tagged with current profile group are selected (`@Tag("dev"|"local"|"prod")`).
  - Tests tagged `@Tag("exclude")` are always skipped.
  - Untagged tests can appear as "0 tests run" under this config.

### Lint/static checks
- No Checkstyle/Spotless/PMD config detected.
- Use compilation/tests as quality gates:
  - `mvn -ntp -DskipTests verify`
  - `mvn -ntp test`

## Docker (optional)
- Compose file: `script/docker/docker-compose.yml`
  - Up: `docker compose -f script/docker/docker-compose.yml up -d`
  - Down: `docker compose -f script/docker/docker-compose.yml down`
- Extra DB stack: `script/docker/database.yml`
- `dockerfile` references internal base images (`192.168.88.150/...`), so intranet may be required.

## Key paths
- Main app code/resources: `ruoyi-admin/src/main/java`, `ruoyi-admin/src/main/resources`
- Profile-filtered config: `ruoyi-admin/src/main/resources/application.yml`
- Profile overrides: `ruoyi-admin/src/main/resources/application-dev.yml`, `ruoyi-admin/src/main/resources/application-prod.yml`
- Global exception handler: `ruoyi-framework/src/main/java/com/ruoyi/framework/web/exception/GlobalExceptionHandler.java`
- Common response wrapper: `ruoyi-common/src/main/java/com/ruoyi/common/core/domain/R.java`
- Shared controller helpers: `ruoyi-common/src/main/java/com/ruoyi/common/core/controller/BaseController.java`
- MyBatis XML mappers: `**/src/main/resources/mapper/**/*Mapper.xml`

## Code style and implementation rules
### Formatting
- Follow `.editorconfig` exactly:
  - 4 spaces for general code
  - 2 spaces for `*.json`, `*.yml`, `*.yaml`
  - LF line endings, trim trailing whitespace
  - final newline enabled (except Markdown)
- Avoid broad formatting changes; keep diffs localized.

### Imports
- Existing code mixes explicit and wildcard imports.
- For new or edited files, prefer explicit imports unless surrounding file already uses wildcard style.
- Avoid import-only churn in untouched sections.

### Naming and file patterns
- Standard Java naming:
  - classes/interfaces: `UpperCamelCase`
  - methods/fields: `lowerCamelCase`
  - constants: `UPPER_SNAKE_CASE`
- Common suffixes/patterns:
  - `*Controller`, `I*Service`, `*ServiceImpl`, `*Mapper`
  - request/input BOs: `domain/bo/*Bo`
  - response/output VOs: `domain/vo/*Vo` and `*View`

### Layering and responsibilities
- Controllers in `ruoyi-admin/.../controller/**` should stay thin.
- Business rules belong in service layer (`ruoyi-system/**/service/impl`).
- Persistence through mapper interfaces + mapper XML.

### Dependency injection
- In `com.ruoyi` modules, common pattern is `@RequiredArgsConstructor` + `private final` fields.
- `@Resource` and `@Autowired` also exist (notably in tests and `ruoyi-extend` legacy code).
- For new service code, prefer constructor injection unless file-local convention differs.

### API and validation conventions
- Controller responses typically use `R<T>` and `TableDataInfo<T>`.
- Success/failure helpers:
  - `R.ok(...)`, `R.fail(...)`
  - `BaseController#toAjax(...)` for write operations
- Use validation groups already present in controllers:
  - `@Validated(AddGroup.class)`, `@Validated(EditGroup.class)`, `@Validated(QueryGroup.class)`

### Types and time handling
- Keep code Java 8 compatible.
- Use boxed numeric/boolean types when nullability matters for DB/DTO fields.
- Use `BigDecimal` for money/precision-sensitive math.
- Prefer `java.time.*`; keep timezone-sensitive behavior aligned with `Asia/Shanghai`.

### Transactions
- Use `@Transactional` at service methods for multi-write workflows.
- Avoid transactions in controllers.
- Keep transactional scopes small; avoid long external I/O inside open DB transactions.

### Error handling
- Business rule violations should throw `com.ruoyi.common.exception.ServiceException`.
- Let `GlobalExceptionHandler` translate exceptions to API responses.
- Flowable exceptions are explicitly mapped to user-facing failures.
- Do not swallow exceptions; log and propagate with meaningful context.

### Logging
- Prefer Lombok `@Slf4j` in `com.ruoyi` code.
- Use parameterized logging (`{}`), not string concatenation in log calls.
- Log request identifiers and key business ids; do not log secrets/tokens/full sensitive payloads.

### Database and MyBatis-Plus
- Follow existing MyBatis-Plus wrappers and `PageQuery` pagination patterns.
- Keep mapper XML namespace and mapper interface aligned.
- Never build SQL with raw string concatenation from user input.

### Tests
- Prefer focused JUnit 5 unit tests when possible.
- Use `@SpringBootTest` only when Spring context is required.
- If you need profile-gated execution, tag tests with `@Tag("dev"|"local"|"prod")`.

## Cursor and Copilot policy files
- No Cursor rules detected (`.cursor/rules/` and `.cursorrules` not present).
- No GitHub Copilot instructions detected (`.github/copilot-instructions.md` not present).
