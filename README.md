# Mox EquipLog

![Android CI](https://github.com/moxmose/Mox_EquipLog/actions/workflows/android.yml/badge.svg)

An Android application to help you manage and track maintenance logs for your equipments. Keep your equipments in top condition by logging every operation, from cleaning the chain to complex repairs.

## ✨ Features

*   **Organizational Sections**: Group your equipment and activities into logical environments (e.g., Garage, Garden, Health, Bikes). Filter your entire experience by section to keep your dashboard focused and clean.
*   **Predictive Maintenance**: Smart forecasting of next maintenance events based on:
    *   **Usage Rates**: Set at the Equipment level (learned automatically from history or using manual fallbacks).
    *   **Intervals**: Set at the Operation level (distance or time-based).
    *   **Forecasted vs Planned**: The UI distinguishes between **Forecasted** interventions (AI-predicted based on trends) and **Planned** interventions (manually scheduled by the user).
*   **Guided Onboarding**: A multi-step introduction to the app's principles, workflow, and predictive logic to get users started quickly.
*   **Demo Scenarios**: Pre-populated data for various use cases (Cars, Garden, Health, Bikes) to immediately explore reports, trends, and forecasting capabilities.
*   **Equipment & Operation Management**: Fully customizable tracking with unique measurement units (km, hours, liters, etc.) and personalized media.
*   **Maintenance Logs**: Detailed recording of activities including costs, notes, and the ability to reset equipment counters.
*   **Cost Analysis**: Monitor expenses and identify efficiency drops with automatic warnings when maintenance costs exceed customizable growth thresholds.
*   **Google Calendar Integration**: Sync your maintenance schedule directly to your Google account for external reminders.
*   **Reporting & Analytics**: Comprehensive visualization suite:
    *   **Usage Trends**: Values over time by equipment or operation.
    *   **Frequency Analysis**: Maintenance distribution across your assets.
    *   **Cost Evolution**: Breakdown of spending and efficiency metrics.
*   **Full Customization**:
    *   **Appearance**: Custom themes, background blur/saturation adjustments, and Material 3 support.
    *   **Images & Icons**: Extensive library management for your equipment and operation photos.
*   **Data Management**: Robust local backups, CSV exports, and total data portability via ZIP imports/exports.

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

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---
*This README and the code was generated with assistance from Gemini in Android Studio.*
