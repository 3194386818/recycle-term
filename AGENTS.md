# AGENTS.md — recycle-term

Monorepo for a terminal recycling management system (终端回收管理系统).
Two sub-projects: `recycle-term-frontend/` (Vue 3 SPA) and `recycle-term-backend/` (Spring Boot REST API).

## Build / Dev / Lint / Test Commands

### Frontend (recycle-term-frontend/)

```bash
cd recycle-term-frontend
npm install              # install dependencies
npm run dev              # dev server on :5173 (proxies /api → :8081)
npm run build            # type-check with vue-tsc then vite build
npm run preview          # preview production build
```

- There is **no linter, formatter, or test runner** configured in the frontend.
- To add linting, install `eslint` + `@vue/eslint-config-typescript` or add a `vitest` config.
- Until then, do NOT assume `npm run lint` or `npm test` exist.

### Backend (recycle-term-backend/)

```bash
cd recycle-term-backend
mvn clean install        # compile + package
mvn spring-boot:run      # start on :8081
mvn test                 # run all tests (none exist yet)
```

- Java 21, Spring Boot 3.4.5, Maven wrapper not included — use system `mvn`.
- There are **no test classes** currently. When adding tests, place them in `src/test/java/com/xiaohei/recycle/`.

### Database

- MySQL 8, database `recycle_term`, charset `utf8mb4`.
- JPA `ddl-auto: update` creates/alters tables automatically.
- Schema reference: `recycle-term-backend/src/main/resources/db/schema.sql`.

## Project Structure

```
recycle-term/
├── recycle-term-frontend/
│   ├── src/
│   │   ├── api/index.ts          # Axios wrapper, all backend calls
│   │   ├── types/index.ts        # Shared TypeScript interfaces
│   │   ├── views/                # Vue page components
│   │   ├── router/index.ts       # Vue Router routes
│   │   ├── main.ts               # App entry, Element Plus setup
│   │   └── App.vue               # Root layout
│   └── vite.config.ts
├── recycle-term-backend/
│   ├── src/main/java/com/xiaohei/recycle/
│   │   ├── controller/           # REST controllers (all under /api)
│   │   ├── service/              # Business logic
│   │   ├── repository/           # Spring Data JPA repositories
│   │   ├── entity/               # JPA entities
│   │   ├── dto/                  # Request/response DTOs
│   │   └── config/               # CORS, etc.
│   └── src/main/resources/
│       ├── application.yml
│       └── db/schema.sql
└── data/                         # Sample Excel data files
```

## Code Style — Frontend (Vue 3 + TypeScript)

### Vue Components
- Use `<script setup lang="ts">` — always.
- Single-file components (`.vue`): template → script → style order.
- Scoped styles: always use `<style scoped>`.
- Use Composition API exclusively (no Options API).

### Imports
- Vue ecosystem: `import { ref, onMounted } from 'vue'`
- Element Plus: import from `element-plus` (e.g., `ElMessage`, `ElMessageBox`).
- Element Plus icons: imported globally in `main.ts`; use directly as `<Search />`.
- Types: use `import type { ... }` for type-only imports from `../types`.
- API calls: import functions from `../api`.

### TypeScript
- Interfaces for all data shapes live in `src/types/index.ts`.
- Use explicit typing on `ref<T>()` when type cannot be inferred.
- API responses are always `ApiResult<T>` — destructure as `const { data: res } = await someApi()`.
- Use `Partial<T>` for update payloads.

### Naming
- Component files: PascalCase (`TaskList.vue`, `ScanPage.vue`).
- Functions: camelCase (`fetchTasks`, `toggleComplete`).
- Reactive refs: camelCase (`const tasks = ref<RecycleTask[]>([])`).
- CSS classes: kebab-case (`stat-card`, `toolbar-btns`).

### Patterns
- Debounce search inputs with `setTimeout`/`clearTimeout` pattern (see `TaskList.vue`).
- Loading state: use `v-loading` directive with a `ref<boolean>`.
- Error handling: rely on Axios interceptor or try/catch; show errors with `ElMessage.error()`.
- Navigation: use `useRouter()` and `$router.push()`.

### UI Framework
- Element Plus is the component library — use its components (`el-table`, `el-card`, `el-button`, etc.).
- Chinese is the UI language; all user-facing strings are in Chinese.

## Code Style — Backend (Spring Boot + Java)

### Architecture
- Layered: Controller → Service → Repository → Entity.
- Controllers: `@RestController` with `@RequestMapping("/api/...")`.
- Services: `@Service` with `@RequiredArgsConstructor` for DI.
- Repositories: extend `JpaRepository` or `JpaSpecificationExecutor`.

### Lombok
- Use `@Data` on entities and DTOs.
- Use `@RequiredArgsConstructor` for constructor injection (never `@Autowired`).
- Use `@AllArgsConstructor` / `@NoArgsConstructor` where needed.

### JPA Entities
- `@Entity` + `@Table(name = "snake_case")`.
- `@Id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- Column names: `@Column(name = "snake_case")`.
- Timestamps: `@CreationTimestamp` / `@UpdateTimestamp` from Hibernate.

### Response Wrapper
- All endpoints return `Result<T>` (code, message, data).
- Success: `Result.ok(data)` or `Result.ok("message", data)`.
- Error: `Result.error("message")` or `Result.error(code, "message")`.

### Naming
- Packages: lowercase (`com.xiaohei.recycle.controller`).
- Classes: PascalCase (`RecycleTaskService`, `TerminalRecord`).
- Methods: camelCase (`getById`, `search`, `deleteById`).
- DB tables/columns: snake_case.

### Error Handling
- Return `Result.error(...)` with Chinese messages for client-facing errors.
- Use `RuntimeException` for not-found cases (e.g., `orElseThrow(() -> new RuntimeException("任务不存在"))`).
- Controller-level try/catch for validation (see `ImportController`).

### Validation
- Jakarta Validation (`spring-boot-starter-validation`).
- Use `@RequestBody` for JSON, `@RequestParam` for query params, `@PathVariable` for URL segments.

## API Conventions

- Base path: `/api`
- REST resources: `/api/tasks`, `/api/records`, `/api/import`
- Pagination: `page` (0-indexed) + `size` params → returns `Page<T>`.
- Sorting: server-side default `Sort.by(DESC, "id")`.
- Frontend proxy: Vite proxies `/api` to `http://127.0.0.1:8081`.

## Important Notes

- The backend `application.yml` contains a real DB password — do not commit credential changes.
- CORS is wide open (`*`) — do not tighten without asking.
- `ddl-auto: update` is active — schema changes happen via JPA, not manual SQL.
- No `.cursorrules`, `.cursor/rules/`, or `.github/copilot-instructions.md` exist.
