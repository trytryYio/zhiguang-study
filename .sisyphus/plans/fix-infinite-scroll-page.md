# 修复无限滚动 page 参数不递增问题

## TL;DR

> **Quick Summary**: 修复 `useInfiniteScroll` hook 中 `loadMore` 函数的依赖项问题，移除 `page` 依赖，确保 page 参数正确递增
>
> **Deliverables**:
> - 修改 `zhiguang_new/src/hooks/userInfiniteScroll.ts`
> - 移除 `page` 从 `loadMore` 的依赖项中
>
> **Estimated Effort**: Quick
> **Parallel Execution**: NO - single file change
> **Critical Path**: Single task

---

## Context

### Original Request
用户反馈无限滚动不起作用，每次请求的 page 参数都是 1，说明 `useInfiniteScroll` Hook 中的 page 状态没有正确更新。

### Interview Summary
**Key Discussions**:
- 前端项目在 `D:\Idea\IdeaProjectTemp\zhiguang-study\zhiguang_new`
- 后端在当前文件夹
- 使用 graphify 搜索文件

**Research Findings**:
- `HomePage.tsx` 已经在使用 `useInfiniteScroll` hook
- `useInfiniteScroll.ts` 中 `loadMore` 的依赖项包含了 `page`
- 这导致每次 `page` 变化时 `loadMore` 函数被重新创建，但内部使用的是闭包中的旧 `page` 值

### Metis Review
**Identified Gaps** (addressed):
- 无需额外审查，问题已明确

---

## Work Objectives

### Core Objective
修复 `useInfiniteScroll` hook 中 `loadMore` 函数的依赖项问题，确保 page 参数正确递增。

### Concrete Deliverables
- 修改 `zhiguang_new/src/hooks/userInfiniteScroll.ts` 第 51 行
- 移除 `page` 从 `loadMore` 的依赖项中

### Definition of Done
- [ ] `loadMore` 的依赖项不包含 `page`
- [ ] 添加注释说明为什么 `page` 不在依赖项中

### Must Have
- 移除 `page` 从依赖项中
- 添加注释说明原因

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
└── Task 1: 修复 useInfiniteScroll.ts 依赖项 [quick]

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

- [ ] 1. 修复 useInfiniteScroll.ts 依赖项

  **What to do**:
  - 读取 `zhiguang_new/src/hooks/userInfiniteScroll.ts`
  - 找到第 51 行的依赖项数组
  - 移除 `page` 从依赖项中
  - 添加注释说明为什么 `page` 不在依赖项中

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
  - `zhiguang_new/src/hooks/userInfiniteScroll.ts:51` - 需要修改的依赖项数组

  **API/Type References** (contracts to implement against):
  - React `useCallback` 依赖项规则

  **Test References** (testing patterns to follow):
  - 无

  **External References** (libraries and frameworks):
  - React 官方文档：https://react.dev/reference/react/useCallback

  **WHY Each Reference Matters** (explain the relevance):
  - `userInfiniteScroll.ts:51` - 这是需要修改的具体位置
  - React 官方文档 - 解释为什么 `page` 不应该在依赖项中

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
  Scenario: 验证依赖项修改正确
    Tool: Bash (grep)
    Preconditions: 文件已修改
    Steps:
      1. 读取 zhiguang_new/src/hooks/userInfiniteScroll.ts 第 51 行
      2. 检查依赖项数组不包含 'page'
      3. 检查依赖项数组包含 'hasMore', 'loading', 'fetchFn', 'initialSize'
    Expected Result: 依赖项数组为 [hasMore, loading, fetchFn, initialSize]
    Failure Indicators: 依赖项数组包含 'page'
    Evidence: .sisyphus/evidence/task-1-dependencies.txt

  Scenario: 验证注释已添加
    Tool: Bash (grep)
    Preconditions: 文件已修改
    Steps:
      1. 搜索文件中包含 "page 不在依赖项中" 的注释
      2. 确认注释存在
    Expected Result: 注释存在
    Failure Indicators: 注释不存在
    Evidence: .sisyphus/evidence/task-1-comment.txt
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
> **Never mark F1-F4 as checked before getting user's okay.** Rejection or user feedback -> fix -> re-run -> present again -> wait for okay.

- [ ] F1. **验证修复效果** — `unspecified-high`
  启动前端应用，滚动到页面底部，检查 console.log 输出，确认 page 参数正确递增（1, 2, 3...）。
  Output: `Page 递增 [PASS/FAIL] | Console 输出 [N/N] | VERDICT`

---

## Commit Strategy

- **1**: `fix(hooks): remove page from useInfiniteScroll dependencies` - zhiguang_new/src/hooks/userInfiniteScroll.ts

---

## Success Criteria

### Verification Commands
```bash
# 检查依赖项
grep -A 1 "}, \[page," zhiguang_new/src/hooks/userInfiniteScroll.ts
# 应该没有输出（说明 page 已被移除）

# 检查注释
grep "page 不在依赖项中" zhiguang_new/src/hooks/userInfiniteScroll.ts
# 应该有输出（说明注释已添加）
```

### Final Checklist
- [ ] `page` 已从依赖项中移除
- [ ] 注释已添加说明原因
- [ ] page 参数在滚动时正确递增
