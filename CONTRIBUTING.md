# CONTRIBUTING.md — B&B Video Editor

## Getting Started

1. Clone the repository
2. Open in Android Studio (Hedgehog or later recommended)
3. Let Gradle sync — all dependencies are in `gradle/libs.versions.toml`
4. Run on an emulator or physical device with API 26+
5. Read `ARCHITECTURE.md` and `DECISIONS.md` before making changes

---

## Code Style

### Kotlin
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Max line length: **120 characters**
- Use `data class` for value objects; `sealed class` for exhaustive states
- Prefer `val` over `var`; use `var` only when mutation is essential
- Use named parameters for function calls with 3+ arguments
- Always use `operator fun invoke()` on single-method use case classes

### Naming Conventions
| Type | Convention | Example |
|---|---|---|
| Class | PascalCase | `MediaImportViewModel` |
| Function | camelCase | `loadMediaItems()` |
| Constant | SCREAMING_SNAKE_CASE | `DATABASE_NAME` |
| Resource ID | snake_case | `btn_new_project` |
| Resource file | snake_case | `fragment_home.xml` |

---

## Architecture Rules

Follow the rules in `AGENTS.md` exactly.

### Layer Checklist Before Submitting
- [ ] Domain layer: no Android SDK imports (except engine interfaces)
- [ ] Data layer: no business logic; only maps data and delegates
- [ ] Feature: one Fragment, one ViewModel, one UiState sealed class
- [ ] ViewModel: no Android context references
- [ ] Fragment: no business logic; only UI rendering

---

## Making Changes

### Feature Development Workflow

1. **Understand the scope** — read `ROADMAP.md` to confirm the feature belongs in the current phase
2. **Design first** — outline what classes/files you'll create and how they connect
3. **Write domain first** — start with models and interfaces before any Android code
4. **Data layer next** — implement repository and data source
5. **UI last** — ViewModel and Fragment are wired last, after the logic is complete
6. **Test** — add unit tests for use cases and ViewModels

### New Feature Checklist
- [ ] Domain model or interface defined
- [ ] Repository interface updated if needed
- [ ] Use case created in `core/domain/usecase/`
- [ ] Repository implementation updated
- [ ] DI bindings added if new interfaces introduced
- [ ] UiState sealed class created for the feature
- [ ] ViewModel extends `BaseViewModel`
- [ ] Fragment extends `BaseFragment<VB>`
- [ ] Navigation action added to `nav_graph.xml`
- [ ] Strings in `strings.xml`
- [ ] No hardcoded dimensions
- [ ] `DECISIONS.md` updated if an architectural decision was made

---

## Dependency Management

All library versions are managed in `gradle/libs.versions.toml`.

To add a new dependency:
1. Add the version to `[versions]`
2. Add the library to `[libraries]`
3. Add the plugin to `[plugins]` if applicable
4. Reference via `libs.xxx` alias in `build.gradle.kts`
5. Document the decision in `DECISIONS.md`

**Do not add dependencies without discussing with the team.**

---

## Testing

### Running Tests
```bash
./gradlew test           # Unit tests
./gradlew connectedTest  # Instrumented tests (requires device/emulator)
./gradlew assembleDebug  # Build verification
```

### Test Standards
- Use `kotlinx-coroutines-test` with `UnconfinedTestDispatcher`
- Inject `CoroutineDispatchers` with test dispatchers in all ViewModel tests
- Use Fake implementations (not Mockito mocks) for repository tests where possible
- Test files mirror the main source structure under `test/`

---

## Git Workflow

### Branching
- `main`: Stable, builds always pass
- `phase-X`: Active phase development branch
- `feature/X`: Individual feature branches off `phase-X`

### Commit Messages
Format: `[scope]: description`

Examples:
- `[mediaimport]: add filter chip selection`
- `[room]: add asset table migration`
- `[timeline]: implement clip trim use case`
- `[docs]: update ROADMAP.md with Phase 2 plan`

### Pull Request Rules
- PRs require a build pass
- No PR merges broken tests
- Include a description of what changed and why
- Reference the relevant ROADMAP phase

---

## Code Review Checklist

**For Reviewers**:
- [ ] Architecture rules followed (see AGENTS.md)
- [ ] No business logic in Fragments or Activities
- [ ] No Android SDK in domain layer (except permitted engine interface types)
- [ ] AppResult used for all failable operations
- [ ] Strings externalized
- [ ] No TODO without a GitHub issue
- [ ] `_binding` nulled in `onDestroyView()` for all Fragments
