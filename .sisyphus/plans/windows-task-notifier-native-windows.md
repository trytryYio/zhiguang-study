# Windows Task Notifier: Native Windows Notifications

## TL;DR

> **Quick Summary**: Replace the `windows-task-notifier` skill's Server酱/WeChat delivery path with native Windows toast notifications using BurntToast, and make notification failure surface as an explicit skill error.
>
> **Deliverables**:
> - Server酱-specific sending flow removed from `windows-task-notifier`
> - BurntToast-based Windows notification path added
> - Skill docs/config/examples updated for local Windows use only
> - Automated verification added after implementation
>
> **Estimated Effort**: Short
> **Parallel Execution**: YES - 2 waves
> **Critical Path**: Task 1 → Task 3 → Task 6 → Final Verification

---

## Context

### Original Request
User wants `windows-task-notifier` changed so it no longer sends completion notices through Server酱/WeChat and instead only sends Windows system notifications.

### Interview Summary
**Key Discussions**:
- Notification strategy: full replacement, no dual-mode fallback
- Windows notification implementation: BurntToast accepted as the default approach
- Test strategy: tests after implementation
- Failure handling: if notification delivery fails, the skill must explicitly error

**Research Findings**:
- BurntToast is the most practical option for Windows PowerShell 5.1 local automation
- SnoreToast and direct WinRT are viable backups but add more operational complexity
- Exact file inventory of the skill should be verified first inside the skill directory before edits begin

### Metis Review
**Identified Gaps** (addressed conservatively due consultation timeout):
- File inventory ambiguity: resolved by making repository/skill discovery the first task
- Scope creep risk: locked to one skill only; no unrelated notification frameworks or global orchestrator changes
- Acceptance gap: added explicit success/failure-path verification around BurntToast availability and runtime send behavior

---

## Work Objectives

### Core Objective
Refactor the local `windows-task-notifier` skill so it emits native Windows toast notifications instead of Server酱/WeChat notifications, while preserving the existing task-complete trigger behavior.

### Concrete Deliverables
- Updated skill implementation under `C:/Users/yuan/.config/opencode/skills/windows-task-notifier/`
- Updated skill documentation describing BurntToast setup and usage
- Updated config or script surface with no Server酱-specific required fields
- Automated verification covering success path and explicit failure path

### Definition of Done
- [ ] Triggering the skill on Windows produces a native toast notification via BurntToast
- [ ] Server酱-specific runtime send path is removed or unreachable
- [ ] Missing BurntToast / send failure causes an explicit, user-visible error
- [ ] Documentation matches the new local Windows-only setup

### Must Have
- BurntToast-based Windows notification delivery
- Full removal of Server酱/WeChat delivery behavior from this skill
- Explicit error behavior on notification failure
- Tests/verification added after implementation

### Must NOT Have (Guardrails)
- No dual-mode notifier abstraction unless strictly required by existing skill architecture
- No lingering required Server酱 tokens, endpoints, or WeChat-facing docs in the skill
- No silent swallow of notification failures
- No unrelated edits outside `windows-task-notifier` except minimal shared references required to keep it working

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** - ALL verification is agent-executed.

### Test Decision
- **Infrastructure exists**: Unknown at planning time; verify in Task 1
- **Automated tests**: Tests-after
- **Framework**: Use the skill's existing test harness if present; otherwise add the smallest viable script-based verification path appropriate to the skill's language/runtime

### QA Policy
Every task includes agent-executed QA scenarios. Evidence saved to `.sisyphus/evidence/task-{N}-{scenario-slug}.{ext}`.

- **PowerShell / local script**: Use Bash or PowerShell commands to invoke the skill or helper script and capture stdout/stderr/exit codes
- **Config/documentation**: Use file reads plus command execution to prove setup instructions are accurate
- **Failure-path checks**: Intentionally break BurntToast availability or inject invalid runtime state and assert explicit errors

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately - discovery + isolated preparation):
├── Task 1: Map current skill files, triggers, config, and send path [quick]
├── Task 2: Design BurntToast notification contract and runtime prerequisites [quick]
└── Task 3: Prepare test/verification strategy aligned to actual skill runtime [quick]

Wave 2 (After Wave 1 - implementation + docs, MAX PARALLEL):
├── Task 4: Replace Server酱 send implementation with BurntToast delivery [unspecified-high]
├── Task 5: Remove/update config surface and docs for Windows-only usage [writing]
└── Task 6: Add automated verification and explicit failure-path checks [unspecified-high]

Wave FINAL (After ALL tasks — 4 parallel reviews, then user okay):
├── Task F1: Plan compliance audit (oracle)
├── Task F2: Code quality review (unspecified-high)
├── Task F3: Real manual QA (unspecified-high)
└── Task F4: Scope fidelity check (deep)
-> Present results -> Get explicit user okay

Critical Path: Task 1 → Task 4 → Task 6 → F1-F4 → user okay
Parallel Speedup: ~40% faster than sequential
Max Concurrent: 3
```

### Dependency Matrix

- **1**: - - 4,5,6,1
- **2**: - - 4,5,2
- **3**: 1 - 6,2
- **4**: 1,2 - 5,6,2
- **5**: 1,2,4 - F1-F4,2
- **6**: 1,3,4 - F1-F4,2

### Agent Dispatch Summary

- **1**: **3** - T1 → `quick`, T2 → `quick`, T3 → `quick`
- **2**: **3** - T4 → `unspecified-high`, T5 → `writing`, T6 → `unspecified-high`
- **FINAL**: **4** - F1 → `oracle`, F2 → `unspecified-high`, F3 → `unspecified-high`, F4 → `deep`

---

## TODOs

- [ ] 1. Inventory the current `windows-task-notifier` skill

  **What to do**:
  - Enumerate the exact files under `C:/Users/yuan/.config/opencode/skills/windows-task-notifier/` and identify entry points, helper scripts, docs, and config surfaces
  - Locate all Server酱/WeChat references, trigger hooks, required environment/config keys, and the current completion-event flow
  - Record the actual runtime used by the skill (PowerShell, script wrapper, other)

  **Must NOT do**:
  - Do not edit behavior yet
  - Do not broaden discovery into unrelated skills

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: focused local discovery and mapping task
  - **Skills**: `[]`
  - **Skills Evaluated but Omitted**:
    - `windows-task-notifier`: this is the target skill, not a planning skill to load

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 2, 3)
  - **Blocks**: 4, 5, 6
  - **Blocked By**: None

  **References**:
  - `C:/Users/yuan/.config/opencode/skills/windows-task-notifier/` - target skill root; executor must map actual files before edits
  - `C:/Users/yuan/.config/opencode/skills/windows-task-notifier/SKILL.md` - skill contract, usage, and any documented runtime/config assumptions
  - `.sisyphus/drafts/windows-task-notifier.md` - confirmed user decisions and research summary captured during planning

  **Acceptance Criteria**:
  - [ ] Exact file inventory for the skill is captured with role labels
  - [ ] Current send path and trigger path are documented with concrete file references
  - [ ] All Server酱-specific config keys/endpoints are identified

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Discovery produces a complete skill map
    Tool: Bash / PowerShell
    Preconditions: Skill directory exists at C:/Users/yuan/.config/opencode/skills/windows-task-notifier/
    Steps:
      1. List all files under the skill root recursively.
      2. Capture the inventory grouped into entry points, scripts, docs, tests, and config.
      3. Search within the skill root for strings like "Server酱", "serverchan", "sctapi", and "wechat".
    Expected Result: A concrete inventory plus located legacy references is saved.
    Failure Indicators: Missing inventory sections, no located send-path file, or unclear runtime path.
    Evidence: .sisyphus/evidence/task-1-skill-inventory.txt

  Scenario: Discovery confirms trigger source
    Tool: Bash / PowerShell
    Preconditions: Same as above
    Steps:
      1. Inspect the skill entry documentation or entry script.
      2. Trace where the completion event enters the notifier.
      3. Record the command/function invoked during task completion.
    Expected Result: The exact trigger hook is identified and saved.
    Evidence: .sisyphus/evidence/task-1-trigger-trace.txt
  ```

  **Evidence to Capture:**
  - [ ] `task-1-skill-inventory.txt`
  - [ ] `task-1-trigger-trace.txt`

  **Commit**: NO

- [ ] 2. Define the BurntToast delivery contract

  **What to do**:
  - Confirm how the skill should invoke BurntToast (module import, notification title/body formatting, exit/error behavior)
  - Decide the minimum required runtime prerequisite checks (module availability, PowerShell version, Windows environment)
  - Specify how task metadata maps into the notification payload

  **Must NOT do**:
  - Do not add optional channels, fallback services, or cross-platform abstraction
  - Do not downgrade failure semantics to warning-only

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: narrow design decision with low edit surface
  - **Skills**: `[]`
  - **Skills Evaluated but Omitted**:
    - `writing`: documentation is not the primary output here

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 3)
  - **Blocks**: 4, 5
  - **Blocked By**: None

  **References**:
  - `.sisyphus/drafts/windows-task-notifier.md` - chosen implementation and failure-policy decisions
  - `https://github.com/Windos/BurntToast` - module usage and compatibility guidance
  - `https://www.powershellgallery.com/packages/BurntToast` - installation expectations for CurrentUser scope

  **Acceptance Criteria**:
  - [ ] Notification contract defines title/body/source fields
  - [ ] Runtime prerequisite checks are explicitly documented
  - [ ] Failure conditions and corresponding error behavior are defined

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: BurntToast contract is executable
    Tool: Bash / PowerShell
    Preconditions: BurntToast installed in CurrentUser scope or documented as missing
    Steps:
      1. Import BurntToast in PowerShell.
      2. Run the proposed minimum notification command with fixed text: "Task complete" and "windows-task-notifier smoke test".
      3. Capture exit code and command output.
    Expected Result: Command shape is valid and runtime expectations are known.
    Failure Indicators: Import failure without explicit handling plan, unclear payload fields.
    Evidence: .sisyphus/evidence/task-2-burnttoast-contract.txt

  Scenario: Missing prerequisite path is defined
    Tool: Bash / PowerShell
    Preconditions: A shell session without importing BurntToast first
    Steps:
      1. Attempt the selected command path without the required module/runtime condition.
      2. Confirm the expected explicit error text is specified.
    Expected Result: Failure mode is concrete and documented.
    Evidence: .sisyphus/evidence/task-2-prereq-failure.txt
  ```

  **Evidence to Capture:**
  - [ ] `task-2-burnttoast-contract.txt`
  - [ ] `task-2-prereq-failure.txt`

  **Commit**: NO

- [ ] 3. Define post-implementation verification for the real runtime

  **What to do**:
  - Detect whether the skill already has tests or a runnable smoke harness
  - Decide the smallest maintainable automated verification approach for this skill after implementation
  - Specify both success-path and fail-hard-path checks based on the actual runtime discovered in Task 1

  **Must NOT do**:
  - Do not overbuild a test framework unrelated to the skill
  - Do not leave verification as manual-only

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: focused validation planning on a small surface area
  - **Skills**: `[]`
  - **Skills Evaluated but Omitted**:
    - `review-work`: this is premature before implementation exists

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2)
  - **Blocks**: 6
  - **Blocked By**: 1

  **References**:
  - `C:/Users/yuan/.config/opencode/skills/windows-task-notifier/` - inspect actual existing test/runtime assets
  - `.sisyphus/drafts/windows-task-notifier.md` - chosen tests-after strategy and fail-hard requirement

  **Acceptance Criteria**:
  - [ ] Verification method is matched to the actual skill runtime
  - [ ] Success-path and failure-path automated checks are enumerated
  - [ ] Evidence file list for post-implementation checks is defined

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Existing verification surface is identified
    Tool: Bash / PowerShell
    Preconditions: Skill directory inventory completed
    Steps:
      1. Search the skill root for test files, scripts, or smoke-run commands.
      2. Record which commands can be reused after implementation.
      3. Record what new lightweight verification must be added if nothing exists.
    Expected Result: A concrete post-implementation verification approach is chosen.
    Evidence: .sisyphus/evidence/task-3-verification-plan.txt

  Scenario: Failure-path verification is defined
    Tool: Bash / PowerShell
    Preconditions: Task 1 and 2 outputs available
    Steps:
      1. Choose one realistic failure trigger (e.g. missing BurntToast module).
      2. Define the command and expected non-zero/error text.
    Expected Result: The future failure-path check is explicit and reproducible.
    Evidence: .sisyphus/evidence/task-3-failure-verification.txt
  ```

  **Evidence to Capture:**
  - [ ] `task-3-verification-plan.txt`
  - [ ] `task-3-failure-verification.txt`

  **Commit**: NO

- [ ] 4. Replace the Server酱 send path with BurntToast delivery

  **What to do**:
  - Edit the actual notifier implementation to remove the Server酱/WeChat dispatch logic and wire the completion event to BurntToast instead
  - Implement the selected prerequisite checks and explicit error behavior when BurntToast is unavailable or send fails
  - Preserve the existing trigger timing and user-facing purpose of the skill: notify when work is done

  **Must NOT do**:
  - Do not change when the skill triggers unless discovery shows the current trigger is broken
  - Do not leave dead Server酱 code paths callable
  - Do not silently ignore PowerShell/module failures

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: multi-file refactor with runtime behavior change and failure semantics
  - **Skills**: `[]`
  - **Skills Evaluated but Omitted**:
    - `writing`: implementation is primary here, not docs

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 5, 6 after dependencies clear)
  - **Blocks**: 5, 6
  - **Blocked By**: 1, 2

  **References**:
  - Task 1 inventory output - identifies the actual entry script(s) and notifier implementation file(s)
  - Task 2 contract output - selected BurntToast command shape, payload mapping, and error policy
  - `https://github.com/Windos/BurntToast/blob/main/README.md` - concrete module usage patterns

  **Acceptance Criteria**:
  - [ ] Completion event now routes only to BurntToast notification logic
  - [ ] Server酱 runtime send logic is removed or made unreachable from the skill
  - [ ] Missing BurntToast or send failure returns an explicit error
  - [ ] Notification title/body still communicate task completion clearly

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Success path sends a Windows toast
    Tool: Bash / PowerShell
    Preconditions: BurntToast installed; implementation updated; sample completion trigger available
    Steps:
      1. Invoke the notifier through the real skill entry point with fixed completion data.
      2. Capture stdout/stderr and exit code.
      3. Verify the command succeeds and evidence of the toast invocation is saved.
    Expected Result: Exit code 0 and a native Windows toast is emitted via BurntToast.
    Failure Indicators: Legacy Server酱 path still called, missing explicit runtime error, or command exits non-zero unexpectedly.
    Evidence: .sisyphus/evidence/task-4-success-toast.txt

  Scenario: Missing BurntToast fails hard
    Tool: Bash / PowerShell
    Preconditions: BurntToast unavailable or intentionally hidden from the session
    Steps:
      1. Invoke the notifier through the same real entry point.
      2. Capture stderr and exit code.
      3. Assert the skill returns a non-zero exit and clear error text explaining the missing prerequisite or send failure.
    Expected Result: Explicit error surfaced; no silent success.
    Evidence: .sisyphus/evidence/task-4-missing-burnttoast.txt
  ```

  **Evidence to Capture:**
  - [ ] `task-4-success-toast.txt`
  - [ ] `task-4-missing-burnttoast.txt`

  **Commit**: NO

- [ ] 5. Update config surface and documentation for Windows-only usage

  **What to do**:
  - Remove or rewrite Server酱-specific setup instructions, examples, and required config from the skill docs
  - Document BurntToast installation, prerequisites, and failure behavior
  - Ensure any sample commands or examples use Windows-native notifications only

  **Must NOT do**:
  - Do not leave contradictory docs referencing WeChat/Server酱 as the normal path
  - Do not document unsupported fallback flows

  **Recommended Agent Profile**:
  - **Category**: `writing`
    - Reason: doc and config-facing cleanup with precision requirements
  - **Skills**: `[]`
  - **Skills Evaluated but Omitted**:
    - `github-actions-docs`: not relevant to this local skill

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 4, 6)
  - **Blocks**: F1-F4
  - **Blocked By**: 1, 2, 4

  **References**:
  - `C:/Users/yuan/.config/opencode/skills/windows-task-notifier/SKILL.md` - primary skill doc to update
  - Task 1 inventory output - identifies any supplemental README/examples/config files
  - Task 2 contract output - source of truth for documented prerequisites and behavior

  **Acceptance Criteria**:
  - [ ] No primary docs require Server酱 keys or WeChat workflow
  - [ ] BurntToast setup instructions are complete and Windows-specific
  - [ ] Failure behavior is documented consistently with implementation

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Docs support a clean setup path
    Tool: Read + Bash / PowerShell
    Preconditions: Documentation updated
    Steps:
      1. Read the skill documentation and extract the setup steps.
      2. Execute the documented installation/import command(s) in order.
      3. Run the documented smoke example.
    Expected Result: The documented path works without undocumented steps.
    Failure Indicators: Missing prerequisite, stale Server酱 config, or broken sample command.
    Evidence: .sisyphus/evidence/task-5-doc-smoke.txt

  Scenario: Legacy references are removed
    Tool: Bash / PowerShell
    Preconditions: Documentation and config updated
    Steps:
      1. Search the skill directory for "Server酱", "wechat", "sctapi", and any old token names.
      2. Save the search output.
    Expected Result: No live user-facing docs/config require the old path.
    Evidence: .sisyphus/evidence/task-5-legacy-search.txt
  ```

  **Evidence to Capture:**
  - [ ] `task-5-doc-smoke.txt`
  - [ ] `task-5-legacy-search.txt`

  **Commit**: NO

- [ ] 6. Add tests/verification and lock in fail-hard behavior

  **What to do**:
  - Implement the tests-after strategy chosen by the user using the runtime-appropriate mechanism discovered in Task 3
  - Add or update automated checks covering both a successful BurntToast notification path and a failure path that must error
  - Ensure these checks are easy to rerun during final verification

  **Must NOT do**:
  - Do not rely on manual-only proof
  - Do not write tests that merely assert function calls without checking real error behavior

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: behavior verification plus runtime failure assertions
  - **Skills**: `[]`
  - **Skills Evaluated but Omitted**:
    - `review-work`: reserved for post-implementation review, not primary verification authoring

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 4, 5)
  - **Blocks**: F1-F4
  - **Blocked By**: 1, 3, 4

  **References**:
  - Task 3 verification plan output - chosen test harness and evidence paths
  - Task 4 implementation output - success and fail-hard runtime behavior

  **Acceptance Criteria**:
  - [ ] Automated verification exists for successful toast dispatch
  - [ ] Automated verification exists for explicit failure on missing/broken BurntToast path
  - [ ] Verification commands are documented and runnable by final reviewers

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Automated success check passes
    Tool: Bash / PowerShell
    Preconditions: Verification added and BurntToast available
    Steps:
      1. Run the selected automated verification command for the success path.
      2. Capture command output and exit code.
    Expected Result: Verification passes with exit code 0.
    Evidence: .sisyphus/evidence/task-6-success-test.txt

  Scenario: Automated failure check detects explicit error
    Tool: Bash / PowerShell
    Preconditions: Verification added; missing/broken BurntToast scenario can be induced safely
    Steps:
      1. Run the selected automated verification command for the failure path.
      2. Capture command output and exit code.
      3. Assert that the failure is explicit and the check passes because the error was correctly detected.
    Expected Result: Failure-path verification passes by observing the expected explicit error.
    Evidence: .sisyphus/evidence/task-6-failure-test.txt
  ```

  **Evidence to Capture:**
  - [ ] `task-6-success-test.txt`
  - [ ] `task-6-failure-test.txt`

  **Commit**: YES
  - Message: `refactor(windows-task-notifier): replace Server酱 with BurntToast`
  - Files: actual skill implementation, docs/config, verification assets
  - Pre-commit: run the discovered verification command(s) from Task 3

---

## Final Verification Wave

> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.

- [ ] F1. **Plan Compliance Audit** — `oracle`
  Read the plan end-to-end. For each "Must Have": verify implementation exists. For each "Must NOT Have": search the skill directory for forbidden patterns such as Server酱 endpoints, tokens, or dead config references. Check evidence files exist in `.sisyphus/evidence/`.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [ ] F2. **Code Quality Review** — `unspecified-high`
  Run the relevant linter/test/runtime checks for the skill. Review changed files for dead branches, stale config keys, hidden failure swallowing, and misleading docs. Ensure failure-path behavior is explicit.
  Output: `Build/Run [PASS/FAIL] | Tests [N pass/N fail] | Files [N clean/N issues] | VERDICT`

- [ ] F3. **Real Manual QA** — `unspecified-high`
  Execute every QA scenario in this plan on Windows-compatible runtime paths. Save command output/screenshots/logs to `.sisyphus/evidence/final-qa/`.
  Output: `Scenarios [N/N pass] | Integration [N/N] | Edge Cases [N tested] | VERDICT`

- [ ] F4. **Scope Fidelity Check** — `deep`
  Compare actual changed files/diff against the plan. Ensure only `windows-task-notifier` and minimal necessary shared references were touched. Confirm no stealth abstraction or unrelated notifier system changes were added.
  Output: `Tasks [N/N compliant] | Contamination [CLEAN/N issues] | Unaccounted [CLEAN/N files] | VERDICT`

---

## Commit Strategy

- **1**: `refactor(windows-task-notifier): replace Server酱 with BurntToast` - skill implementation, config/docs, tests - run relevant skill verification before commit

---

## Success Criteria

### Verification Commands
```bash
# Examples - replace with actual skill/runtime commands discovered in Task 1
powershell -NoProfile -Command "Import-Module BurntToast; New-BurntToastNotification -Text 'Task complete','windows-task-notifier smoke test'"  # Expected: exit 0 and toast visible in Windows notification center
```

### Final Checklist
- [ ] All "Must Have" present
- [ ] All "Must NOT Have" absent
- [ ] All tests/verification steps pass
