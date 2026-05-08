# Mox EquipLog

An Android application to help you manage and track maintenance logs for your equipments. Keep your equipments in top condition by logging every operation, from cleaning the chain to complex repairs.

## ✨ Features

*   **Equipment Management**: Add, edit, archive, and reorder your list of personal equipments.
*   **Operation Types**: Create and manage a custom list of maintenance operations.
*   **Maintenance Logs**: Log every maintenance activity, linking an equipment with an operation, date, and notes.
*   **Predictive Maintenance**: Smart forecasting of next maintenance events based on historical usage trends or manual fallback values.
*   **Cost Analysis**: Track maintenance costs and identify efficiency drops with automatic warnings based on customizable growth thresholds.
*   **Google Calendar Integration**: Sync planned maintenance activities directly to your Google Calendar for better planning.
*   **Reporting & Analytics**: Comprehensive section with charts and statistics:
    *   **Usage Trends**: Visualize usage values over time by equipment or operation.
    *   **Frequency Analysis**: Identify which parts of your equipment require more frequent care.
    *   **Cost Evolution**: Detailed breakdown of maintenance expenses and their distribution.
*   **Advanced Sorting & Filtering**: Easily search through your logs and save custom filters for quick access.
*   **Full Customization**:
    *   **Organized Settings**: Streamlined setup divided into **General** (Analytics & Units), **Appearance** (Themes & Media), and **System** (Backups).
    *   **Colors & Themes**: Assign unique colors to sections and toggle between Material 3 and custom color palettes for reports.
    *   **Images & Icons**: Personalize each item with predefined icons or images from your gallery.
*   **Data Management**:
    *   **Backup & Restore**: Secure your data with local database backups (including all user preferences).
    *   **Data Export**: Export your logs and reports in CSV or ZIP formats.
    *   **Persistence**: Robust local storage using **Room** database for both application data and user settings.

## 🛠 Tech Stack & Libraries

This project is built with 100% Kotlin and follows modern Android development practices.

*   **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) for building the entire UI declaratively.
*   **Architecture**: MVVM (Model-View-ViewModel) with Clean Architecture principles.
*   **Database & Preferences**: [Room](https://developer.android.com/training/data-storage/room) for robust and persistent storage of all application data and user settings (no external preference files).
*   **Dependency Injection**: [Koin](https://insert-koin.io/) for managing dependencies in a pragmatic way.
*   **Asynchronous Programming**: Kotlin [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) and [Flow](https://developer.android.com/kotlin/flow) for managing background tasks and data streams.
*   **Image Loading**: [Coil](https://coil-kt.github.io/coil/) for loading images efficiently.
*   **Testing**:
    *   **Unit Tests**: [JUnit](https://junit.org/junit5/), [Robolectric](http://robolectric.org/), [Turbine](https://github.com/cashapp/turbine) and [MockK](https://mockk.io/) for testing ViewModels, DAOs, and Flows.
    *   **UI Tests**: [Compose Test Rule](https://developer.android.com/jetpack/compose/testing) for integration and UI testing.

## 🛣️ Future Roadmap

*   **PDF Export Expansion**: Enhanced PDF reports with embedded charts and maintenance history summaries.
*   **Cloud Sync Integration**: Optional cloud backup to keep data synchronized across multiple devices.

## 🚀 Setup & Build

1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Let Gradle sync the dependencies.
4.  To build the project, run the following command in the terminal:
    ```bash
    ./gradlew assembleDebug
    ```

---
*This README and the code was generated with assistance from Gemini in Android Studio.*
