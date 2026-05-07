# Add ProtectedRoute for Frontend Authentication

## TL;DR

> **Quick Summary**: Add ProtectedRoute component to intercept unauthenticated access to protected routes, integrating with existing AuthContext and backend Spring Security.
>
> **Deliverables**:
> - ProtectedRoute component
> - Updated App.tsx with protected routes
> - Integration with existing AuthContext
>
> **Estimated Effort**: Quick
> **Parallel Execution**: NO - sequential
> **Critical Path**: ProtectedRoute → App.tsx update

---

## Context

### Original Request
User wants to implement frontend authentication interception that works with existing backend Spring Security and AuthContext.

### Current State
- ✅ Backend: Spring Security protects API endpoints (returns 401)
- ✅ Frontend: AuthContext manages authentication state
- ✅ Frontend: request.ts handles 401 responses
- ❌ Frontend: No route-level protection (users can access /profile directly)

### Integration Points
- `AuthContext` - provides `user`, `tokens`, `isLoading`
- `useAuth` hook - access authentication state
- `request.ts` - handles API 401 errors
- React Router - route configuration

---

## Work Objectives

### Core Objective
Add ProtectedRoute component that integrates with existing AuthContext to intercept unauthenticated access to protected routes.

### Concrete Deliverables
- `src/components/common/ProtectedRoute.tsx` - Route guard component
- Updated `src/App.tsx` - wrap protected routes with ProtectedRoute
- Integration with existing AuthContext

### Definition of Done
- [ ] ProtectedRoute component created
- [ ] App.tsx updated with protected routes
- [ ] Unauthenticated users redirected to /login
- [ ] Loading state handled during auth check
- [ ] Works with existing AuthContext

### Must Have
- Use existing AuthContext (no new auth logic)
- Redirect to /login when not authenticated
- Handle loading state
- Preserve redirect URL for post-login redirect

### Must NOT Have (Guardrails)
- Do not duplicate existing AuthContext logic
- Do not break existing authentication flow
- Do not add new authentication methods

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** - ALL verification is agent-executed. No exceptions.

### Test Decision
- **Infrastructure exists**: YES
- **Automated tests**: NO (tests-after)
- **Framework**: None
- **If TDD**: N/A

### QA Policy
Every task MUST include agent-executed QA scenarios. Evidence saved to `.sisyphus/evidence/task-{N}-{scenario-slug}.{ext}`.

- **Frontend/UI**: Use Playwright - Test protected route access, redirect behavior
- **API/Backend**: Use Bash (curl) - Verify backend still protects API

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately):
├── Task 1: Create ProtectedRoute component [quick]
├── Task 2: Update App.tsx with protected routes [quick]

Wave FINAL (After ALL tasks):
├── Task F1: Manual QA - Test protected route behavior [unspecified-high]
```

### Dependency Matrix

- **1**: - - 2, 1
- **2**: 1 - F1, 1
- **F1**: 2 - - 1

### Agent Dispatch Summary

- **1**: **2** - T1 → `quick`, T2 → `quick`
- **FINAL**: **1** - F1 → `unspecified-high`

---

## TODOs

- [ ] 1. Create ProtectedRoute component

  **What to do**:
  - Create `src/components/common/ProtectedRoute.tsx`
  - Use existing `useAuth` hook to get authentication state
  - Check if `tokens?.accessToken` exists
  - If not authenticated, redirect to `/login` with redirect URL
  - If loading, show loading spinner
  - If authenticated, render children
  - Use React Router's `Navigate` component for redirect

  **Must NOT do**:
  - Do not create new authentication logic
  - Do not duplicate AuthContext functionality
  - Do not modify existing AuthContext

  **Recommended Agent Profile**:
  > Select category + skills based on task domain. Justify each choice.
  - **Category**: `quick`
    - Reason: Simple component creation with clear requirements
  - **Skills**: []
    - No specific skills needed for this task
  - **Skills Evaluated but Omitted**:
    - None

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Task 2)
  - **Blocks**: Task 2
  - **Blocked By**: None

  **References** (CRITICAL - Be Exhaustive):

  > The executor has NO context from your interview. References are their ONLY guide.
  > Each reference must answer: "What should I look at and WHY?"

  **Pattern References** (existing code to follow):
  - `src/context/AuthContext.tsx:28-37` - AuthContextValue type definition
  - `src/context/AuthContext.tsx:48-65` - readStoredTokens function
  - `src/pages/ProfilePage.tsx:19` - useAuth hook usage example

  **API/Type References** (contracts to implement against):
  - `src/context/AuthContext.tsx:28-37` - AuthContextValue interface

  **Test References** (testing patterns to follow):
  - None

  **External References** (libraries and frameworks):
  - React Router v6: https://reactrouter.com/en/main/components/navigate

  **WHY Each Reference Matters** (explain the relevance):
  - AuthContext.tsx:28-37 - Shows the structure of AuthContextValue (user, tokens, isLoading)
  - AuthContext.tsx:48-65 - Shows how tokens are read from localStorage
  - ProfilePage.tsx:19 - Shows how to use the useAuth hook in a component

  **Acceptance Criteria**:

  > **AGENT-EXECUTABLE VERIFICATION ONLY** - No human action permitted.
  > Every criterion MUST be verifiable by running a command or using a tool.

  **QA Scenarios (MANDATORY - task is INCOMPLETE without these):**

  \`\`\`
  Scenario: Unauthenticated user redirected to login
    Tool: Playwright
    Preconditions: Frontend running on localhost:5173, no user logged in
    Steps:
      1. Clear localStorage (remove auth tokens)
      2. Navigate to http://localhost:5173/profile
      3. Wait for redirect (timeout: 5s)
      4. Check current URL
    Expected Result: Redirected to /login with redirect parameter
    Failure Indicators: Stays on /profile or redirects to wrong page
    Evidence: .sisyphus/evidence/task-1-redirect-to-login.png

  Scenario: Authenticated user can access protected route
    Tool: Playwright
    Preconditions: Frontend running on localhost:5173, user logged in
    Steps:
      1. Login with valid credentials
      2. Navigate to http://localhost:5173/profile
      3. Wait for page load (timeout: 5s)
      4. Check current URL
    Expected Result: Stays on /profile and shows profile content
    Failure Indicators: Redirected to /login or shows error
    Evidence: .sisyphus/evidence/task-1-access-protected.png
  \`\`\`

  **Evidence to Capture**:
  - [ ] Screenshot of redirect to login page
  - [ ] Screenshot of authenticated user accessing protected route

  **Commit**: NO

- [ ] 2. Update App.tsx with protected routes

  **What to do**:
  - Import ProtectedRoute component
  - Wrap protected routes with ProtectedRoute:
    - `/profile` → ProtectedRoute
    - `/profile/edit` → ProtectedRoute
    - `/create` → ProtectedRoute (if needed)
  - Keep public routes unprotected:
    - `/`
    - `/search`
    - `/login`
    - `/register`
  - Test that routes work correctly

  **Must NOT do**:
  - Do not wrap public routes with ProtectedRoute
  - Do not break existing route functionality
  - Do not change route paths

  **Recommended Agent Profile**:
  > Select category + skills based on task domain. Justify each choice.
  - **Category**: `quick`
    - Reason: Simple route configuration update
  - **Skills**: []
    - No specific skills needed for this task
  - **Skills Evaluated but Omitted**:
    - None

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 1 (after Task 1)
  - **Blocks**: F1
  - **Blocked By**: Task 1

  **References** (CRITICAL - Be Exhaustive):

  > The executor has NO context from your interview. References are their ONLY guide.
  > Each reference must answer: "What should I look at and WHY?"

  **Pattern References** (existing code to follow):
  - `src/App.tsx:1-36` - Current route configuration

  **API/Type References** (contracts to implement against):
  - None

  **Test References** (testing patterns to follow):
  - None

  **External References** (libraries and frameworks):
  - React Router v6: https://reactrouter.com/en/main/components/route

  **WHY Each Reference Matters** (explain the relevance):
  - App.tsx:1-36 - Shows the current route structure that needs to be updated

  **Acceptance Criteria**:

  > **AGENT-EXECUTABLE VERIFICATION ONLY** - No human action permitted.
  > Every criterion MUST be verifiable by running a command or using a tool.

  **QA Scenarios (MANDATORY - task is INCOMPLETE without these):**

  \`\`\`
  Scenario: Protected routes wrapped correctly
    Tool: Bash (grep)
    Preconditions: ProtectedRoute component created
    Steps:
      1. Search for ProtectedRoute usage in App.tsx
      2. Verify /profile is wrapped
      3. Verify /profile/edit is wrapped
      4. Verify public routes are not wrapped
    Expected Result: Protected routes wrapped, public routes not wrapped
    Failure Indicators: Protected routes not wrapped or public routes wrapped
    Evidence: .sisyphus/evidence/task-2-routes-wrapped.txt

  Scenario: All routes still accessible
    Tool: Playwright
    Preconditions: Frontend running on localhost:5173
    Steps:
      1. Navigate to / (public)
      2. Navigate to /search (public)
      3. Navigate to /login (public)
      4. Navigate to /register (public)
    Expected Result: All public routes accessible without redirect
    Failure Indicators: Any public route redirects to login
    Evidence: .sisyphus/evidence/task-2-public-routes.png
  \`\`\`

  **Evidence to Capture**:
  - [ ] Grep output showing ProtectedRoute usage
  - [ ] Screenshot of public routes accessible

  **Commit**: NO

---

## Final Verification Wave (MANDATORY — after ALL implementation tasks)

> 1 review agent runs. Must APPROVE. Present results to user and get explicit "okay" before completing.
>
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.**

- [ ] F1. **Manual QA** — `unspecified-high`
  Test complete authentication flow:
  1. Clear localStorage, access /profile → should redirect to /login
  2. Login, access /profile → should show profile page
  3. Access public routes → should work without login
  4. Logout, access /profile → should redirect to /login
  5. Verify backend still protects API (401 response)
  Output: `Redirect [PASS/FAIL] | Auth Access [PASS/FAIL] | Public Routes [PASS/FAIL] | Backend API [PASS/FAIL] | VERDICT`

---

## Commit Strategy

- **1**: `feat(auth): add ProtectedRoute component` - src/components/common/ProtectedRoute.tsx
- **2**: `feat(auth): protect routes with ProtectedRoute` - src/App.tsx

---

## Success Criteria

### Verification Commands
```bash
# No specific commands - manual verification via browser
```

### Final Checklist
- [ ] ProtectedRoute component created
- [ ] Protected routes wrapped with ProtectedRoute
- [ ] Unauthenticated users redirected to /login
- [ ] Authenticated users can access protected routes
- [ ] Public routes remain accessible
- [ ] Backend API still protected (401 response)
- [ ] Integration with existing AuthContext works
