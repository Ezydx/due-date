# DueDate ⌛ (v1.1.0)

A high-performance, human-readable deadline and time tracker desktop application built with **Java 24+** and fully compatible with **Apache NetBeans 25** on Windows 11.

---

## 🎯 What's New in v1.1.0

- 🔕 **Turn Off Overdues & Mute Reminders**: Easily mute reminders for overdue deadlines while keeping them in your list.
- 📁 **Untrack / Ignore System (`tracked.conf`)**: Gitignore-style file untracking without deleting `.dd` files from disk.
- 🎨 **Java 2D Vector Icons**: Pixel-perfect vector icons for Refresh, Settings, Calendar, Import, and Actions that never fail to render on Windows 11.
- ⏱️ **Clamped Notification Timing**: Smart duration clamping (e.g. "due in 1 hour" or "overdue by 15m") replacing odd polling intervals like "1 hour and 2 minutes".
- 🏷️ **Creation Metadata (`[created]`) & Permanent Indexing**: Intrinsic creation order `#` is permanently attached to each item and flips naturally when sorted ascending/descending.
- 💡 **Hover Details Card**: Rich multi-line tooltip card on row hover displaying complete deadline details without messy horizontal scrollbars.
- 🛡️ **Selection Guidance**: Helpful dialogs if actions like Delete, Edit, or Mute are triggered without selecting a row first.
- 📦 **Standard Maven Project Layout**: Open directly in Apache NetBeans 25 with 1-click Run (<kbd>F6</kbd>) and Test (<kbd>Alt</kbd>+<kbd>F6</kbd>).

---

## 📋 File Format: `.dd` Specification

Every deadline object is stored as an individual `.dd` file using `|?|` as the delimiter at the end of each field:
```text
[title] "Team Milestone Sync" |?|
[since] 2026/11/01 - 09:00:am |?|
[deadline] 2026/11/15 - 05:30:pm |?|
[created] 2026/11/01 - 08:30:am |?|
[person] "Guest" |?|
[description] "Review deliverables and test production fat-jar" |?|
```
* **Order Independent**: The parser recognizes tags `[title]`, `[since]`, `[deadline]`, `[created]`, `[person]`, and `[description]` in any order.
* **Instant DateTime Format**: `yyyy/MM/dd - hh:mm:pm/am` (e.g. `2026/11/08 - 02:45:am`).
* **Person Tag**: Tagged user/person, defaults to `"Guest"`.
* **Safe Escaping**: Any delimiter `|?|` inside descriptions is automatically escaped and restored.

---

## 🔕 Tracking & Muting: `tracked.conf` (`.tracked`)

Stored in `%APPDATA%\DueDates\tracked.conf`:
```text
[mute]
file1.dd, file4.dd
file9-11.dd
// Comments starting with // or # are ignored
// Separators are commas and newlines

[untrack]
file2.dd
# note 1
file7elaven.dd  ,dodo.dd
```
- **Muted Files**: Loaded and visible in the table and calendar (with subtle dimmed style and `[Muted 🔕]` badge), but sound chimes and persistent alerts are silenced.
- **Untracked Files**: Ignored by the app completely and skipped from disk loading, keeping files safe on disk.
- **Re-tracking**: When importing an untracked file, DueDate asks if you'd like to re-track it.

---

## 📅 Interactive Calendar View
A dedicated monthly calendar popup featuring colored indicator dots on each day:
* 🟣 **Purple Dot**: `Since` (Start date) — always purple.
* ⌛ **Deadline Dots**: Color-coded by proximity:
  * ⚫ **Black**: Already ended (overdue).
  * 🔴 **Red**: Deadline is today.
  * 🟠 **Orange**: Deadline is this week (within 7 days).
  * 🟢 **Green**: Deadline is longer than a week and up to 1 month.
  * 🔵 **Blue**: Deadline is longer than one month.
* Clicking on any day displays all starting and ending deadlines for that date in the sidebar.

---

## 🚀 Building & Running

### Requirements
* Java 24 or newer (JDK 24 / JDK 25)

### Build
Run `build.bat` or compile with Maven:
```cmd
build.bat
```
or
```cmd
mvn clean package
```

### Run
Launch with 1-click:
```cmd
run.bat
```
or
```cmd
java -jar DueDate.jar
```

To run silently in background (System Tray mode):
```cmd
java -jar DueDate.jar --background
```

### Open in Apache NetBeans 25
1. **File** ➔ **Open Project...** (<kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>O</kbd>)
2. Select `d:\gravity\DueDate` (NetBeans detects the Maven project).
3. Press <kbd>F6</kbd> to Run, or <kbd>Alt</kbd> + <kbd>F6</kbd> to run all 47 unit tests!
