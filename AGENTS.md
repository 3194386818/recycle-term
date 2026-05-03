# AGENTS.md — recycle-term

Monorepo: 终端回收管理系统. `recycle-term-frontend/` (Vue 3 + TS) + `recycle-term-backend/` (Spring Boot 3.4.5).

## Build / Dev / Lint / Test

### Frontend (`recycle-term-frontend/`)

```bash
npm install              # install deps
npm run dev              # dev server :5173, proxies /api → :8081
npm run build            # vue-tsc type-check + vite build
npm run preview          # preview production build
```

- No linter, formatter, or test runner configured. DO NOT run `npm run lint` or `npm test`.
- `vue-tsc -b --noEmit` for type-check alone; `vite build` on WSL may fail on native bindings (use `vue-tsc` separately).

### Backend (`recycle-term-backend/`)

```bash
mvn clean package -DskipTests    # compile + JAR
mvn spring-boot:run              # start :8081
mvn test                         # no tests exist yet
```

- Java 21, use system `mvn` (no Maven wrapper).
- Place new tests in `src/test/java/com/xiaohei/recycle/`.

### Database

- MySQL 8, db `recycle_term`, utf8mb4.
- `ddl-auto: update` auto-creates/alters tables from JPA entities.
- Schema reference: `src/main/resources/db/schema.sql` (may be stale, trust JPA entities).

### Deploy (local → server)

```bash
# Local build
cd recycle-term-backend && mvn clean package -DskipTests
cd recycle-term-frontend && npm install && npm run build
# Upload via paramiko SFTP (JAR → /opt/recycle-term/, dist → /var/www/recycle-term/)
# Server: install -m 755 JAR && systemctl restart recycle-term
```

- Server: 43.130.238.145, SSH as `xiaohei`, no sudo (use `su -` for root).
- Maven 3.9.9 at `/opt/apache-maven-3.9.9/bin/mvn`, Java 21 at `/usr/lib/jvm/java-21-openjdk`.
- Nginx reverse proxy: `/etc/nginx/conf.d/recycle-term.conf`, SSL at `/etc/nginx/ssl/`.

## Project Structure

```
recycle-term/
├── recycle-term-frontend/
│   └── src/
│       ├── api/index.ts          # Public API calls
│       ├── api/admin.ts          # Admin API calls (JWT interceptor)
│       ├── types/index.ts        # All TypeScript interfaces
│       ├── views/                # Vue pages
│       │   └── admin/            # Admin pages (JWT guarded)
│       ├── router/index.ts       # Routes + admin auth guard
│       ├── main.ts               # Element Plus + icon global registration
│       └── App.vue               # Header + sidebar layout
├── recycle-term-backend/
│   └── src/main/java/com/xiaohei/recycle/
│       ├── controller/           # @RestController under /api
│       ├── service/              # @Service business logic
│       ├── repository/           # JpaRepository + JpaSpecificationExecutor
│       ├── entity/               # @Entity JPA classes
│       ├── dto/                  # Request/response DTOs
│       └── config/               # CORS, JWT interceptor, WebMvcConfig
└── data/                         # Sample Excel files
```

## Code Style — Frontend

### Vue Components
- `<script setup lang="ts">` always. Composition API only.
- SFC order: template → script → style. Use `<style scoped>`.
- Imports: `import { ref, onMounted } from 'vue'`, `import type { ... } from '../types'`, API functions from `../api` or `../api/admin`.
- Element Plus icons registered globally in `main.ts`; use directly as `<Search />`.
- API responses: `const { data: res } = await getTasks()` (wrapper is `ApiResult<T>`).
- Naming: PascalCase files, camelCase functions/refs, kebab-case CSS.
- Debounce search: `setTimeout`/`clearTimeout` pattern (see `TaskList.vue`).
- Mobile: detect via `window.innerWidth <= 768`, use `class-name="hide-mobile"` for hidden columns.
- Error handling: `try { await api() } catch (e: any) { ElMessage.error(e.response?.data?.message || 'xxx') }`.
- **DON'T**: use `window.location.hash` for navigation — use `router.push()` instead.

### Status System

Status integer → UI label mapping (define in a shared constant, NOT duplicated per component):

| status | label |
|--------|-------|
| 0 | 待回收 |
| 1 | 已上门 |
| 2 | 已完成(待审核) |
| 3 | 已失败(待审核) |
| 4 | 审核成功 (deprecated, use 6) |
| 5 | 审核失败 |
| 6 | 已归档 |

### Flow: 0→1→2/3→(review)→6/5→0

## Code Style — Backend

### Architecture
- Controller → Service → Repository → Entity. Never call repository from controller.
- `@RequiredArgsConstructor` for DI (no `@Autowired`). `@Data` on entities/DTOs.
- Response: `Result<T>` via `Result.ok(data)` / `Result.error("msg")`. Messages in Chinese.
- Error: `throw new RuntimeException("任务不存在")`, catch in controller for validation errors.

### JPA
- `@Entity` + `@Table(name = "snake_case")`. `@Column(name = "snake_case")`.
- `@Id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- `@CreationTimestamp` / `@UpdateTimestamp` for timestamps.
- Use `Specification` for dynamic queries. Pagination: `PageRequest.of(page, size, Sort.by(DESC, "id"))`.
- Lombok errors from LSP (getter/setter/constructor not found) are **false positives** — Maven build works.

### APIs
- Base: `/api`. Resources: `/tasks`, `/records`, `/import`, `/admin/*`.
- Pagination: `page` (0-indexed) + `size`. Status filter: `Integer status` or `Boolean pendingReview` (status 2+3).
- Admin endpoints under `/api/admin/` use JWT via `AdminInterceptor`.
- `application.yml` has real DB password — **never commit changes to credentials**.

## Known Issues / Optimization TODO

### High Priority
- **N+1 queries**: `TerminalRecordService.scan()`, `AdminService.batchCreateTasks()`, `StatsService` daily loop — batch with `saveAll()` / aggregation queries.
- **Admin delete** (`AdminService.deleteTask`) doesn't cascade-delete `terminal_record` rows.
- **Excel import** uses `XSSFWorkbook` only (`.xlsx`). Use `WorkbookFactory.create()` for `.xls` support.
- **Missing DB indexes**: `recycle_task.status`, `recycle_task.completed`, `recycle_task.completed_at`, `terminal_record.scanned_at`, unique `(task_id, serial_number)`.
- **`open-in-view: true`** anti-pattern — set to `false` and add `@Transactional` to all read services.

### Medium Priority
- **Duplicate `statusTypeMap`** in 3+ components — extract to `src/constants/index.ts`.
- **Memory leak**: `window resize` listeners not removed in `TaskList.vue`/`TaskDetail.vue` — use `onUnmounted(() => window.removeEventListener(...))`.
- **Missing error handling**: `fetchStats`, `removeRecord`, `handleDelete` lack try/catch.
- **`admin.ts:22`** uses `window.location.hash` with `createWebHistory` router — use `router.push('/admin/login')`.
- **`schema.sql`** is stale — missing `fail_reason`, `review_remark`, `reviewer_id`, status range up to 6.
- **ReviewList fetches status 2+3 separately** — add `pendingReview` param to admin API or merge requests.

### Low Priority
- No Spring Boot Actuator (`/health` endpoint).
- HTML `<html lang="en">` should be `zh-CN`.
- No favicon. Page title is "recycle-term-frontend".
- `codeable-taskRepository-searchByKeyword` and `findByPhoneNumberLikeOr...` are unused dead code.
- `OperationLog` table has no retention/cleanup mechanism.

## Important Notes

- **Token in `gitToken.txt`**: present in repo (gitignored but check history if exposed). Rotate periodically.
- **JWT secret**: hardcoded in `application.yml` and `JwtUtil.java` default — should be env var only in production.
- **Default admin**: `admin`/`admin123` in `AdminService.@PostConstruct` — change in production.
- **CORS**: wide open (`*`) — don't tighten without discussion.
- **`ddl-auto: update`** — JPA manages schema, don't run manual `ALTER TABLE` without coordination.
- No `.cursorrules`, `.cursor/rules/`, or `.github/copilot-instructions.md` exist.
