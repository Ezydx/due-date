# DueDate ⌛

A high-performance, human-readable deadline and time tracker desktop application built with **Java 24+**.

---

## 🎯 Key Features

### 1. Human-Readable `.dd` File Format
Every deadline object is stored as an individual `.dd` file using `|?|` as the delimiter at the end of each field:
```text
[title] "Team Milestone Sync" |?|
[since] 2026/11/01 - 09:00:am |?|
[deadline] 2026/11/15 - 05:30:pm |?|
[person] "Guest" |?|
[description] "Review Q4 deliverables and test production fat-jar" |?|
```
* **Order Independent**: The parser recognizes tags `[title]`, `[since]`, `[deadline]`, `[person]`, and `[description]` in any order.
* **Instant DateTime Format**: `yyyy/MM/dd - hh:mm:pm/am` (e.g. `2026/11/08 - 02:45:am`).
* **Person Tag**: Tagged user/person, defaults to `"Guest"`.
* **Safe Escaping**: Any delimiter `|?|` inside descriptions is automatically escaped and restored.

### 2. Title Collision Disambiguation
If multiple deadline files share the exact same title, the UI displays the filename upon both titles for clear disambiguation:
```
deadline44 (meating1)
deadline44 (subscription)
```
Unique titles are displayed cleanly without extra parentheses.

### 3. Date & Time Inputs & Relative Resolver
* **Start Date (`[since]`)**:
  * Checkbox **"Set to Now"**: Automatically sets the start time to the current instant and disables manual input fields.
  * Date & Time pickers: Year, Month, Day, Hour (1-12), Minute (00-59), AM/PM. Unset hours/minutes default to `00:00`.
* **Deadline (`[deadline]`)**:
  * **Direct Mode**: Select the target date and time directly.
  * **Relative Mode**: Select a base (`Now`, `Since`, or `Customize`) and add offsets:
    * `+ Years`
    * `+ Months`
    * `+ Days`
    * `+ Hours`
    * `+ Minutes`
  * Live resolver updates and previews the calculated target date and time in real-time.

### 4. Interactive Calendar View 📅
A dedicated monthly calendar popup featuring colored indicator dots on each day:
* 🟣 **Purple Dot**: `Since` (Start date) — always purple.
* ⌛ **Deadline Dots**: Color-coded by proximity:
  * ⚫ **Black**: Already ended (overdue).
  * 🔴 **Red**: Deadline is today.
  * 🟠 **Orange**: Deadline is this week (within 7 days).
  * 🟢 **Green**: Deadline is longer than a week and up to 1 month.
  * 🔵 **Blue**: Deadline is longer than one month.
* Clicking on any day displays all starting and ending deadlines for that date in the sidebar.

### 5. Universal Search & Filter Chips
* **Universal Search Box**: Instant real-time filtering across titles, descriptions, person tags, filenames, and date strings.
* **Filter Chips**: One-click quick filters: `All`, `Today`, `This Week`, `Ended / Overdue`, `Future`.
* **Customizable Columns**: Configure visible columns in Settings:
  * `#` (Index)
  * `Title` (with collision disambiguation)
  * `Since (Start)`
  * `Deadline`
  * `Status / Remaining` (colored proximity badge)
  * `Person`
  * `Description` (preview)
  * `File Name`
* Click any table header to toggle sort order (**Ascending** / **Descending**).

### 6. Windows Background Daemon & Audio Bell Chime 🔔
* **Windows Startup Integration**: Option to automatically start DueDate on Windows boot (`HKCU\Software\Microsoft\Windows\CurrentVersion\Run`).
* **Background System Tray**:
  * Minimizes to system tray when the window is closed.
  * Double-click tray icon to restore.
  * Right-click tray menu: Open, New Deadline, Check Deadlines Now, Exit.
* **Synthesized Audio Bell Chime**: Plays a crisp, melodic dual-tone chime (587Hz + 880Hz sine wave decay) using Java Sound API without requiring external audio files.
* **Configurable Notifications**:
  * Startup summary alert (overdue, today, this week).
  * Persistent reminders: recurring alert every configured interval (e.g. every 60 minutes) for deadlines due today or overdue.

### 7. Storage & Configuration Location
All files are persistently stored in:
```
%APPDATA%\DueDates\
├── config.properties          # Settings and preferences
└── deadlines\                 # User .dd deadline files
    ├── team_sync.dd
    ├── meating1.dd
    └── subscription.dd
```

---

## 🚀 Building & Running

### Requirements
* Java 24 or newer (JDK 24 / JDK 25)

### Build
Run `build.bat` or compile with `javac`:
```cmd
build.bat
```
This compiles the sources, runs the automated test suite (32 unit tests), and packages the executable `DueDate.jar`.

### Run
Launch using `run.bat` or `java`:
```cmd
run.bat
```
or
```cmd
java -jar DueDate.jar
```

To run silently in the background (System Tray mode):
```cmd
java -jar DueDate.jar --background
```
