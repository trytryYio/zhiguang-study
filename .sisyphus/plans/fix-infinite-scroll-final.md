# 修复无限滚动 page 参数不递增问题（最终修复）

## TL;DR

> **Quick Summary**: 修复 HomePage.tsx 中 fetchFeed 函数的响应处理，确保正确解析后端返回的分页信息
>
> **Deliverables**:
> - 修改 `zhiguang_new/src/pages/HomePage.tsx`
> - 正确处理后端返回的分页数据
>
> **Estimated Effort**: Quick
> **Parallel Execution**: NO - single file change
> **Critical Path**: Single task

---

## Context

### Original Request
用户反馈无限滚动不起作用，每次请求的 page 参数都是 1，说明 `useInfiniteScroll` Hook 中的 page 状态没有正确更新。

### Research Findings
- `useInfiniteScroll.ts` 已经修复：移除了 `page` 从依赖项中
- `HomePage.tsx` 中的 `fetchFeed` 函数没有正确处理后端返回的分页信息
- 后端返回的数据结构为 `{items: Array, page: number, size: number, hasMore: boolean}`
- 当前代码错误地将 `resp.hasNext` 作为 `hasMore` 的判断依据

### Metis Review
**Identified Gaps** (addressed):
- 无需额外审查，问题已明确

---

## Work Objectives

### Core Objective
修复 HomePage.tsx 中 fetchFeed 函数的响应处理，确保正确解析后端返回的分页信息。

### Concrete Deliverables
- 修改 `zhiguang_new/src/pages/HomePage.tsx` 第 28 行
- 使用 `resp.hasMore` 而不是 `resp.hasNext` 来判断是否还有更多数据

### Definition of Done
- [ ] `fetchFeed` 函数正确使用 `resp.hasMore` 判断是否有更多数据
- [ ] 移除对不存在的 `hasNext` 字段的引用

### Must Have
- 使用 `resp.hasMore` 而不是 `resp.hasNext`
- 移除对 `hasNext` 字段的引用

### Must NOT Have (Guardrails)
- 不修改其他代码
- 不修改 UI 结构
- 不修改样式

---

## Verification Strategy (MANDATORY)

> **ZERO HUMAN INTERVENTION** - ALL verification is agent-executed. No exceptions.

### Test Decision
- **Infrastructure exists**: NO
- **Automated tests**: NO
- **Framework**: None
- **If TDD**: N/A

### QA Policy
Every task MUST include agent-executed QA scenarios (see TODO template below).
Evidence saved to `.sisyphus/evidence/task-{N}-{scenario-slug}.{ext}`.

- **Frontend/UI**: Use Playwright (playwright skill) - Navigate, interact, assert DOM, screenshot
- **TUI/CLI**: Use interactive_bash (tmux) - Run command, send keystrokes, validate output
- **API/Backend**: Use Bash (curl) - Send requests, assert status + response fields
- **Library/Module**: Use Bash (bun/node REPL) - Import, call functions, compare output

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately - single task):
└── Task 1: 修复 HomePage.tsx 分页处理 [quick]

Wave FINAL (After ALL tasks — verification):
└── Task F1: 验证修复效果 [unspecified-high]

Critical Path: Task 1 → F1
Parallel Speedup: N/A (sequential)
Max Concurrent: 1
```

### Dependency Matrix

- **1**: - - F1, 1
- **F1**: 1 - -

> This is abbreviated for reference. YOUR generated plan must include the FULL matrix for ALL tasks.

### Agent Dispatch Summary

- **1**: **1** - T1 → `quick`
- **FINAL**: **1** - F1 → `unspecified-high`

---

## TODOs

> Implementation + Test = ONE Task. Never separate.
> EVERY task MUST have: Recommended Agent Profile + Parallelization info + QA Scenarios.
> **A task WITHOUT QA Scenarios is INCOMPLETE. No exceptions.**

- [ ] 1. 修复 HomePage.tsx 分页处理

  **What to do**:
  - 读取 `zhiguang_new/src/pages/HomePage.tsx`
  - 找到第 28 行的 `hasMore` 判断逻辑
  - 将 `resp.hasNext ?? resp.items?.length === size` 改为 `resp.hasMore`
  - 移除对 `hasNext` 字段的引用

  **Must NOT do**:
  - 不修改其他代码
  - 不修改 UI 结构
  - 不修改样式

  **Recommended Agent Profile**:
  > Select category + skills based on task domain. Justify each choice.
  - **Category**: `quick`
    - Reason: 单文件修改，简单明确
  - **Skills**: `[]`
    - 无需特殊技能

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential
  - **Blocks**: F1
  - **Blocked By**: None

  **References** (CRITICAL - Be Exhaustive):

  > The executor has NO context from your interview. References are their ONLY guide.
  > Each reference must answer: "What should I look at and WHY?"

  **Pattern References** (existing code to follow):
  - `zhiguang_new/src/pages/HomePage.tsx:28` - 需要修改的 hasMore 判断逻辑

  **API/Type References** (contracts to implement against):
  - 后端 API 返回结构：{items: Array, page: number, size: number, hasMore: boolean}

  **Test References** (testing patterns to follow):
  - 无

  **External References** (libraries and frameworks):
  - 无

  **WHY Each Reference Matters** (explain the relevance):
  - `HomePage.tsx:28` - 这是需要修改的具体位置
  - 后端 API 返回结构 - 解释为什么应该使用 `hasMore` 而不是 `hasNext`

  **Acceptance Criteria**:

  > **AGENT-EXECUTABLE VERIFICATION ONLY** - No human action permitted.
  > Every criterion MUST be verifiable by running a command or using a tool.

  **QA Scenarios (MANDATORY - task is INCOMPLETE without these):**

  > **This is NOT optional. A task without QA scenarios WILL BE REJECTED.**
  >
  > Write scenario tests that verify the ACTUAL BEHAVIOR of what you built.
  > Minimum: 1 happy path + 1 failure/edge case per task.
  > Each scenario = exact tool + exact steps + exact assertions + evidence path.
  >
  > **The executing agent MUST run these scenarios after implementation.**
  > **The orchestrator WILL verify evidence files exist before marking task complete.**

  ```
  Scenario: 验证 hasMore 判断逻辑
    Tool: Bash (grep)
    Preconditions: 文件已修改
    Steps:
      1. 读取 zhiguang_new/src/pages/HomePage.tsx 第 28 行
      2. 检查 hasMore 判断逻辑是否为 `resp.hasMore`
      3. 检查是否移除了对 hasNext 的引用
    Expected Result: hasMore 判断逻辑为 `resp.hasMore`
    Failure Indicators: hasMore 判断逻辑包含 `hasNext`
    Evidence: .sisyphus/evidence/task-1-has-more.txt
  ```

  > **Specificity requirements - every scenario MUST use:**
  > - **Selectors**: Specific CSS selectors (`.login-button`, not "the login button")
  > - **Data**: Concrete test data (`"test@example.com"`, not `"[email]"`)
  > - **Assertions**: Exact values (`text contains "Welcome back"`, not "verify it works")
  > - **Timing**: Wait conditions where relevant (`timeout: 10s`)
  > - **Negative**: At least ONE failure/error scenario per task
  >
  > **Anti-patterns (your scenario is INVALID if it looks like this):**
  > - ❌ "Verify it works correctly" - HOW? What does "correctly" mean?
  > - ❌ "Check the API returns data" - WHAT data? What fields? What values?
  > - ❌ "Test the component renders" - WHERE? What selector? What content?
  > - ❌ Any scenario without an evidence path

  **Evidence to Capture**:
  - [ ] Each evidence file named: task-{N}-{scenario-slug}.{ext}
  - [ ] Screenshots for UI, terminal output for CLI, response bodies for API

  **Commit**: NO
  - Message: N/A
  - Files: N/A
  - Pre-commit: N/A

---

## Final Verification Wave (MANDATORY — after ALL implementation tasks)


> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
>
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.**
> **Never mark F1-F4 as checked before getting user's okay. Rejection or user feedback -> fix -> re-run -> present again -> wait for okay.**

- [ ] F1. **验证修复效果** — `unspecified-high`
  启动前端应用，滚动到页面底部，检查 console.log 输出，确认 page 参数正确递增（1, 2, 3...）。
  Output: `Page 递增 [PASS/FAIL] | Console 输出 [N/N] | VERDICT`

---

## Commit Strategy

- **1**: `fix(homepage): use resp.hasMore instead of resp.hasNext` - zhiguang_new/src/pages/HomePage.tsx

---

## Success Criteria

### Verification Commands
```bash
# 检查 hasMore 判断逻辑
grep "hasMore: resp.hasMore" zhiguang_new/src/pages/HomePage.tsx
# 应该有输出（说明已使用 resp.hasMore）

# 检查是否移除了 hasNext
grep "hasNext" zhiguang_new/src/pages/HomePage.tsx
# 应该没有输出（说明已移除）
```

### Final Checklist
- [ ] 使用 `resp.hasMore` 判断是否有更多数据
- [ ] 移除了对 `hasNext` 字段的引用
- [ ] page 参数在滚动时正确递增
