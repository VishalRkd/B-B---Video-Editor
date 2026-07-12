# AGENTS.md — AI Agent Rules for B&B Video Editor

This file contains **mandatory rules** that every AI agent working on this project must read and follow before making any changes.

---

## 1. Architecture First

- **Never sacrifice architecture for speed.** If a correct implementation takes more time, take that time.
- **Think before coding.** Understand the full impact of any change before writing a single line.
- **Every file must belong** to a clear layer: domain, data, feature, UI base, or utilities.
- **Read ARCHITECTURE.md and DECISIONS.md** before making any significant change.

---

## 2. Layer Rules (Strict)

### Domain Layer (`core/domain/`)
- **Zero Android SDK imports** except in engine interfaces (MediaEngine, PreviewEngine, etc.)
- Engine interfaces may reference `android.net.Uri`, `android.graphics.Bitmap`, `android.view.Surface` since these are platform-native media types
- **No Room, Hilt, or Retrofit** references
- All business logic belongs here

### Data Layer (`core/data/`)
- Android SDK allowed
- **Room is for app-owned data ONLY**: Projects, Assets, editing history
- **Do NOT cache MediaStore data in Room.** Device media is ALWAYS fetched live from MediaStore.
- All MediaStore queries belong in `MediaStoreDataSourceImpl`

### Feature Layer (`feature/`)
- Each feature: one Fragment, one ViewModel, one UiState sealed class
- **No business logic in Fragments or Activities**
- ViewModels expose only `StateFlow<UiState>` — never mutable state directly

### UI Layer
- `BaseFragment<VB>` must be used by all fragments
- `BaseViewModel` must be used by all ViewModels
- Always null `_binding` in `onDestroyView()`

---

## 3. Patterns to Always Follow

- **AppResult<T>**: Use for all operations that can fail. Never throw raw exceptions to the UI.
- **CoroutineDispatchers**: Always inject — never use `Dispatchers.IO` directly.
- **Logger**: Always use `Logger.d/e/w()` — never `Log.d/e/w()` directly.
- **Flow**: Use `repeatOnLifecycle(STARTED)` in all Fragments for state collection.
- **Room migrations**: Every schema change must have a Room `Migration` — never use `fallbackToDestructiveMigration()` on a production build.

---

## 4. Code Quality Non-Negotiables

- No God classes. No class longer than ~300 lines without strong justification.
- No `TODO` comments without a corresponding GitHub issue.
- No hardcoded strings in Kotlin — use `strings.xml`.
- No hardcoded dimensions in Kotlin — use `dimens.xml`.
- No direct `context.startActivity()` in ViewModels.
- All new files must include the package declaration and at minimum a one-line KDoc comment.

---

## 5. Dependency Rules

- **New dependencies require approval.** Document in `DECISIONS.md` with rationale.
- All versions live in `gradle/libs.versions.toml`. No inline version strings.
- Use `ksp` not `kapt` for annotation processing.

---

## 6. Testing Rules

- Every new use case should have a unit test in `test/`.
- Use `UnconfinedTestDispatcher` with `CoroutineDispatchers` in tests.
- Fakes, not mocks, for repository tests wherever possible.

---

## 7. Git Hygiene

- One feature per commit. Atomic, focused commits.
- Commit message format: `[scope]: short description` (e.g., `[mediaimport]: add filter chip selection`)
- Never commit broken code to `main`.

---

## 8. AI Agent Workflow

For every task, strictly follow this sequence:

1. **Read** `ARCHITECTURE.md` and `DECISIONS.md`
2. **Understand** the existing code in the affected area
3. **Explain** the implementation approach before writing any code
4. **Implement** only the approved scope
5. **Verify** the project builds successfully
6. **Summarize** all changes made
7. **Stop** and wait for approval before continuing

**Never automatically continue to the next feature or phase.**
