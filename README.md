# Finora for Android

Finora is a modern, native Android personal finance and expense management application built entirely with Kotlin and Jetpack Compose. Designed as a clean, privacy-first mobile banking dashboard, Finora empowers users to track daily expenses and income, manage monthly budgets, analyze financial trends through interactive visualizations, and maintain total ownership of their financial data.

---

## Key Features

### Transaction Management
* **Income & Expense Tracking**: Record financial transactions with title, amount, category, custom notes, and date.
* **Search & Filter**: Search transactions instantaneously by keyword across titles and notes. Filter by type (Expense / Income) or category.
* **Date Grouping & Gestures**: Transactions are grouped chronologically with daily expense totals. Includes swipe-to-delete with an instant undo action via Snackbar.

### Interactive Analytics Dashboard
* **Financial Summary Header**: Real-time view of current net balance, total income, total expenses, and percentage change compared to the previous period.
* **Income vs. Expense Bar Chart**: Interactive dual-bar comparison chart with custom period aggregation and tap-to-inspect tooltips.
* **Account Balance Trend Line**: Smooth line chart tracking cumulative account balance over time with grid lines and interactive point inspection.
* **Category Distribution Donut Chart**: Animated donut chart visualizing expense breakdown by category, featuring interactive segment selection and a category detail bottom sheet.
* **Performance Metrics**: Real-time calculation of savings rate (%), average monthly expenses, highest single income, and highest single expense.
* **Top 5 Largest Expenses**: Dedicated list highlighting the largest expense entries in the selected time range.
* **Flexible Time Filters**: Preset ranges (*This Week*, *This Month*, *Last 3 Months*, *This Year*, *All Time*) and a *Custom Date Range* selector with date pickers.

### Category & Budget Management
* **Custom Categories**: Create custom expense and income categories with customizable names, 20+ vector icons, and color palettes.
* **Category Control**: Toggle category visibility without deleting historical transaction records. Safe deletion automatically reassigns transactions to *Sonstiges* (Others).
* **Monthly Budgeting**: Set monthly spending limits with progress indicators, daily allowance calculations, and spending rate forecasts.

### Preferences & Data Privacy
* **100% Offline & Private**: All data is stored locally on-device using an encrypted Room SQLite database. No account creation or internet connection required.
* **Data Backup & Import**: Full JSON data export and import capability, compatible with exports from the original iOS Finora application.
* **Multi-Currency Support**: Support for 12 currencies including EUR (€), USD ($), GBP (£), CHF (Fr), JPY (¥), CAD ($), AUD ($), CNY (¥), INR (₹), PLN (zł), TRY (₺), and MDL (L).
* **Localization**: Full user interface support for German and English.
* **Widget & System Integration**: Jetpack Glance Home Screen Widget displaying current monthly spending and quick-action shortcuts for 1-tap entry creation.
* **Material You Theming**: Supports Material 3 dynamic coloring on Android 12+ (API 31+) with explicit Light, Dark, and System appearance toggles.

---

## Tech Stack

| Domain | Technology / Library | Description |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.0.21 | 100% Native Kotlin implementation |
| **UI Framework** | Jetpack Compose | Declarative UI with Compose BOM `2024.12.01` |
| **Design System** | Material Design 3 | Material 3 components & Material Icons Extended |
| **Architecture** | MVVM + Repository | Clean Architecture with Coroutines & StateFlow |
| **Navigation** | Navigation Compose | Type-safe screen navigation (`2.8.5`) |
| **Database** | Room Database | Local SQLite persistence via Room `2.6.1` & KSP |
| **Preferences** | DataStore Preferences | Jetpack DataStore (`1.1.1`) for user settings |
| **Serialization** | Kotlinx Serialization | Type-safe JSON export/import (`1.7.3`) |
| **Widgets** | Jetpack Glance | Declarative Home Screen AppWidget (`1.1.1`) |
| **Build System** | Gradle Kotlin DSL | AGP `8.13.2`, Compile SDK `35`, Min SDK `26` |

---

## Project Structure

```
InpensoAndroid/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/app/finora/
│           │   ├── MainActivity.kt          # Main activity entry point & shortcut handling
│           │   ├── data/
│           │   │   ├── Analytics.kt         # Analytics engine & date range calculations
│           │   │   ├── Backup.kt            # JSON import/export & iOS format parser
│           │   │   ├── Database.kt          # Room Database & DAO interfaces
│           │   │   ├── Models.kt            # Entity definitions & built-in constants
│           │   │   ├── Repository.kt        # Data repository abstraction layer
│           │   │   └── SettingsStore.kt     # Jetpack DataStore preferences manager
│           │   ├── ui/
│           │   │   ├── AppNav.kt            # Main Scaffold navigation host
│           │   │   ├── Format.kt           # Currency & date formatting utilities
│           │   │   ├── MainViewModel.kt     # ViewModel state management & Flow exposure
│           │   │   ├── components/          # Reusable UI components & interactive charts
│           │   │   │   ├── Charts.kt        # Canvas bar, line, and donut charts
│           │   │   │   ├── Common.kt        # Section cards, bubbles, transaction rows
│           │   │   │   └── IconMap.kt       # Vector icon map & color palette
│           │   │   ├── screens/             # Main application screens
│           │   │   │   ├── AnalyticsScreen.kt          # Interactive Analytics Dashboard
│           │   │   │   ├── CategoryManagementScreen.kt # Category editor & manager
│           │   │   │   ├── EditExpenseScreen.kt        # Add/Edit entry form screen
│           │   │   │   ├── HomeScreen.kt               # Overview & recent activity
│           │   │   │   ├── SettingsScreen.kt           # User preferences & currency selection
│           │   │   │   └── TransactionsScreen.kt       # Filterable transaction list
│           │   │   └── theme/               # Material 3 theme configuration
│           │   └── widget/
│           │       └── FinoraWidget.kt      # Glance Home Screen AppWidget & Receiver
│           └── res/                         # XML layouts, values, shortcuts, & drawables
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

---

## Installation & Setup

### Prerequisites
* **Android Studio**: Ladybug (2024.2.1) or newer recommended.
* **JDK**: Java 17 JDK configured in Android Studio.
* **Android SDK**: API Level 35 (Android 15) SDK installed.
* **Device / Emulator**: Android device running API 26 (Android 8.0) or higher.

### Building from Source

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/VintusS/Inpenso.git
   cd Inpenso
   ```

2. **Open in Android Studio**:
   * Launch Android Studio and select **Open**.
   * Navigate to the cloned directory and wait for Gradle synchronization to complete.

3. **Build the Debug APK**:
   ```bash
   ./gradlew :app:assembleDebug
   ```

4. **Run on Connected Device**:
   * Enable **USB Debugging** on your Android test device.
   * Click **Run 'app'** in Android Studio or use ADB:
   ```bash
   ./gradlew :app:installDebug
   ```

---

## Configuration

Finora is designed as an offline-first application and does not require external API keys or environment secrets.

* **Database File**: Stored locally in application sandboxed storage as `finora.db`.
* **Preferences**: Stored via Jetpack DataStore as `settings.preferences_pb`.
* **Build Targets**: Configured in `app/build.gradle.kts`:
  * `compileSdk = 35`
  * `minSdk = 26`
  * `targetSdk = 35`

---

## Usage Guide

1. **Adding a Transaction**:
   * Tap the **+** Floating Action Button on the Home or Transactions screen.
   * Select transaction type (*Expense* or *Income*), enter the amount, select a category, and optionally add a title or note. Tap **Save**.
2. **Analyzing Finances**:
   * Navigate to the **Analytics** tab.
   * Filter by time range (*This Month*, *Last 3 Months*, *Custom Range*, etc.).
   * Tap on chart bars or balance points to inspect detailed period values.
   * Tap on any category in the Donut Chart or Legend to view all transactions for that category.
3. **Managing Categories**:
   * Go to **Mehr** (Settings) → **Kategorien verwalten**.
   * Create custom categories with custom icons and colors, or toggle the visibility of unused built-in categories.
4. **Data Backup & Restore**:
   * Export complete transaction history and settings as a formatted JSON file via the Settings screen.
   * Restore backups at any time with automatic deduplication.

---

## Development & Workflow

### Useful Gradle Commands

* **Compile Kotlin Sources**:
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```
* **Build Debug APK**:
  ```bash
  ./gradlew :app:assembleDebug
  ```
* **Clean Build Cache**:
  ```bash
  ./gradlew clean
  ```

---

## Roadmap

- [ ] Automated processing for recurring monthly/yearly subscription expenses.
- [ ] Export transactions to CSV and PDF format.
- [ ] Biometric authentication lock (Fingerprint / Face Unlock).
- [ ] Opt-in encrypted cloud synchronization across devices.

---

## License & Acknowledgments

* **License**: Open-source released under the **MIT License**.
* **Original Design Attribution**: Native Android port of the original iOS [Inpenso](https://github.com/VintusS/Inpenso) project by Dragomir Mindrescu.
