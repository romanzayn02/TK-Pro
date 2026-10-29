# Settings Button Alignment & Layout Refinement

Redesign the Settings screen button layouts to fix vertical misalignment in the Google Account sync section and the Personal Profile photo section, creating a polished, balanced, and consistent interface.

### User Review & Critical Decisions

> [!IMPORTANT]
> The user confirmed the following layout decisions during Phase 1:
> - **Scope of Fix**: Align both the top Google sync row and the profile photo action buttons.
> - **Top Account Row Layout**: Replace the cramped, multi-line pill with a dedicated Google Account status banner card featuring a clear email badge, switch account affordance, and a balanced row of equal-height action buttons (**Auto Restore** and **403 Fix**).
> - **Profile Photo Buttons**: Match the heights, typography, and vertical baselines of **Upload Photo** and **Use Google Photo** so they align side by side.
> - **Bottom Dock Clearance**: Leave the existing bottom padding as is per user preference.

---

### 1. Overview & Core Concept

- **What It Does**: Resolves awkward multi-line text wrapping and mismatched button heights in `SettingsScreen.kt`. It structures the Google Account state into a clean account card with dedicated action buttons, and standardizes the profile photo button row with uniform button heights.
- **Target Audience**: Store owners and ledger managers navigating settings who need clean, readable controls for cloud sync, error diagnosis, and profile customization.
- **Key Value**: Delivers professional visual polish, eliminates text clipping and wrapping quirks, and ensures all interactive button touch targets adhere to Material 3 density and alignment standards.

---

### 2. User Experience & Visual Design

#### Visual Layout & Hierarchy

```
┌────────────────────────────────────────────────────────┐
│ 👤 Google Account & Sync Status                        │
│ ┌────────────────────────────────────────────────────┐ │
│ │ [Photo/Avatar] cocchampruman@gmail.com  [Switch ↷] │ │
│ └────────────────────────────────────────────────────┘ │
│                                                        │
│ ┌─────────────────────────┐  ┌───────────────────────┐ │
│ │ ☁️ Auto Restore          │  │ ❓ 403 Fix             │ │
│ └─────────────────────────┘  └───────────────────────┘ │
└────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────┐
│ 📷 Profile Photo (Custom Device Image)                 │
│ [Avatar]   Uses your Google photo by default...        │
│                                                        │
│            ┌────────────────────┐ ┌──────────────────┐ │
│            │ 📷 Upload Photo    │ │ 🔄 Google Photo  │ │
│            └────────────────────┘ └──────────────────┘ │
└────────────────────────────────────────────────────────┘
```

- **Top Google Sync Card**:
  - **Account Status Row**: Full-width container with the Google profile avatar, truncated single-line email, and a subtle "Switch" / "লগআউট/পরিবর্তন" button.
  - **Action Button Row**: Equal-weight, equal-height (44dp) pill buttons for **Auto Restore** (with cloud download icon) and **403 Fix** (with help icon), maintaining horizontal harmony and equal heights.
- **Profile Photo Section**:
  - Equalize button height (`42.dp`) and vertical centering for both **Upload Photo** and **Use Google Photo** (shortened label to prevent multi-line wrapping, e.g. "Google Photo" / "Google ছবি").

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Separate Account Status from Action Triggers**
  - *Chosen Approach*: Display the signed-in Google account email in its own horizontal status row above the action buttons, rather than trying to cram an email address, an avatar, "Auto Restore", and "403 Fix" into a single 3-button row.
  - *Why*: Email addresses vary widely in length (e.g. `cocchampruman@gmail.com` is 23 characters). Packing it into a one-third width chip forces 3 lines of awkward word-wrapping that ruins vertical alignment.
  - *Alternatives Considered*: Truncating the email with ellipsis in the small chip (still resulted in mismatched text heights compared to short labels).

- **Decision 2: Equal-Height Row Constraints (`IntrinsicSize.Max` / Fixed Min Height)**
  - *Chosen Approach*: Standardize interactive pill chip buttons with `Modifier.height(44.dp)` and `maxLines = 1` text with subtle ellipsis if ever constrained.
  - *Why*: Guarantees crisp pixel alignment across all screen widths and font scale settings.

---

### 4. Technical Architecture & Component Structure

```
SettingsScreen (LazyColumn)
  │
  ├── item("google_docs_live_sync_card")
  │     ├── Header & Auto-Sync Toggle Switch
  │     ├── Docs & Sheets Accordion Items
  │     └── [NEW] Dedicated Google Account Card & Action Row
  │           ├── Account Info Row (Avatar + Email + Switch Account)
  │           └── Equal-Height Action Row ([Auto Restore] & [403 Fix])
  │
  └── item("personal_profile_card")
        └── Profile Photo Section
              ├── Avatar Preview + Badge
              └── Equal-Height Action Row ([Upload Photo] & [Google Photo])
```

#### Files to Modify
- `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`:
  - Refactor lines 891–993 to render the dedicated Google account banner and equal-height action buttons.
  - Refactor lines 1556–1589 to enforce equal height and single-line labels on photo action buttons.
