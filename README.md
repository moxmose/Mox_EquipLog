# Mox EquipLog

An Android application to help you manage and track maintenance logs for your equipments. Keep your equipments in top condition by logging every operation, from cleaning the chain to complex repairs.

## ✨ Features

*   **Equipment Management**: Add, edit, dismiss, and reorder your list of personal equipments.
*   **Operation Types**: Create and manage a custom list of maintenance operations.
*   **Maintenance Logs**: Log every maintenance activity, linking a equipment with an operation, date, and notes.
*   **Advanced Sorting & Filtering**: Easily search through your logs and sort them by date, equipment, operation, kilometers, or notes.
*   **Data Persistence**: Your data is saved locally using a Room database.
*   **User Settings**: Simple user preference management using DataStore.

## 🛠 Tech Stack & Libraries

This project is built with 100% Kotlin and follows modern Android development practices.

*   **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) for building the entire UI declaratively.
*   **Architecture**: MVVM (Model-View-ViewModel).
*   **Database**: [Room](https://developer.android.com/training/data-storage/room) for robust and persistent local data storage.
*   **Preferences**: [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) for storing simple key-value data like user settings.
*   **Dependency Injection**: [Koin](https://insert-koin.io/) for managing dependencies in a pragmatic way.
*   **Asynchronous Programming**: Kotlin [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) and [Flow](https://developer.android.com/kotlin/flow) for managing background tasks and data streams.
*   **Image Loading**: [Coil](https://coil-kt.github.io/coil/) for loading images efficiently.
*   **Testing**:
    *   **Unit Tests**: [JUnit](https://junit.org/junit5/), [Robolectric](http://robolectric.org/), and [Turbine](https://github.com/cashapp/turbine) for testing ViewModels, DAOs, and Flows.

## 🚀 Setup & Build

1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Let Gradle sync the dependencies.
4.  To build the project, run the following command in the terminal:
    ```bash
    ./gradlew assembleDebug
    ```

---
*This README and the code was generated with assistance from Gemini in Android Studio, an automated system to do some things right and others wrong .*
