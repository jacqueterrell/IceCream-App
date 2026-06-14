# Ice Cream Now - AI Development Rules

## Architecture
- Use MVVM architecture.
- UI layer should only communicate with ViewModels.
- Business logic belongs in repositories or use cases.
- Avoid business logic inside Compose UI.

---

## State Management
- Use StateFlow for new Compose screens.
- Keep UI state immutable using data classes.
- Avoid mutable state inside composables unless localized UI state.

---

## Firebase Rules
- Firebase Anonymous Auth is required for customer sessions.
- Customer identity must persist between app launches.
- Firestore writes should avoid duplication.
- Notification tokens must be refreshed safely.

---

## Vendor/Customer Relationship Rules
- Customers do NOT browse random vendors.
- Customers connect to vendors:
    1. through direct invite
    2. QR code
    3. manual vendor lookup
- Vendor/customer relationship is intended to remain persistent.

---

## Notifications
- Push notifications should be vendor-specific.
- Avoid duplicate notifications.
- Handle notification taps safely when app is backgrounded or cold started.

---

## Compose Rules
- Keep composables stateless when possible.
- Hoist state to ViewModels.
- Avoid heavy logic during recomposition.
- Use remember only for local UI concerns.

---

## Refactoring Rules
- Avoid rewriting unrelated files.
- Keep changes scoped and incremental.
- Preserve existing architecture patterns.

---

## QA Expectations
Before completing a feature:
- consider offline mode
- consider app restarts
- consider rapid user interaction
- consider notification timing
- consider Firebase synchronization edge cases
- consider low network conditions

---

## Documentation
- Explain architectural changes clearly.
- Generate implementation summaries when large changes occur.