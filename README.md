# Fab Pack Tracker
Android app for tracking asset packs on Fab, with an AI helper
that polishes listing titles and descriptions.

_This project is not affiliated with Fab or Epic Games_

## Why
I sell asset packs on Fab and wanted a simple phone tracker; Fab has no public seller API, so data is entered manually

## Features
- CRUD interface for your asset packs
- Improving a pack's title and description using AI
- Show AI hints about missing information in your pack
- Estimated revenue per pack

## Tech stack and architecture
Kotlin, Jetpack Compose, ViewModel + StateFlow, Room, Retrofit + kotlinx.serialization, Groq API.

UI (Compose) -> ViewModel -> Repository / DAO -> Retrofit / Room

## Getting started

### Requirements
- Android Studio Quail 3
- Android SDK 37 (the project uses compileSdk 37, minSdk 26)
- A device or emulator with Android 8.0 (API 26) or newer

### Run the app
1. Clone the repository and open it in Android Studio:
   ```
   git clone https://github.com/hovrah888/FAB-Pack-Tracker
   ```
2. Let Gradle sync finish.
3. Select a device and press Run.

The app works without any keys: you can add, edit and delete packs.

### Enable the AI helper (optional)
The "Improve with AI" button uses the [Groq](https://console.groq.com) API.

1. Create an API key in the Groq console.
2. Add this line to local.properties in the project root
   (Android Studio creates this file automatically; it is listed in .gitignore):
   ```
   groq_api_key=YOUR_KEY_HERE
   ```
3. Sync the project and run again.

Without a key the button shows an error message; the rest of the app is unaffected.
Never commit your key.

## Tests
Unit tests run on the JVM, no emulator needed:

./gradlew testDebugUnitTest  # macOS / Linux
gradlew.bat testDebugUnitTest  # Windows

The HTML report is written to app/build/reports/tests/testDebugUnitTest/index.html.
In Android Studio you can also right-click the test folder and choose Run Tests.

### What is covered
- FormState: parsing and validation of price and sales count (comma as decimal separator, whitespace, empty and invalid input)
- GroqRepository: parsing the model response, error handling (network, HTTP, malformed JSON, empty answer), request contents. The API is replaced by a fake.
- AddEditPackViewModel: state transitions on load and save, validation on save, AI suggestion flow (loading, success, failure, double request)
- PackListViewModel: list from the database, deletion

The tests use a hand-written fake DAO and fake API, kotlinx-coroutines-test
and a rule that replaces the main dispatcher.

### What is not covered
- The real Room DAO and migrations (instrumented tests)
- Compose UI tests

## Design decisions
- **Constructor injection, no DI framework.** Dependencies are wired by hand in
  `MainActivity`. This keeps the classes easy to test with fakes; the cost is
  verbose wiring, which is the main reason to adopt Hilt next.
- **MVVM with a single state object per concern.** Screen state is exposed as
  `StateFlow`; one-off events (e.g. "saved") go through a `Channel` so they are
  not replayed after rotation.
- **Estimated revenue is computed, not stored** (`salesCount * price`), so it
  cannot drift out of sync with the fields it comes from.
- **AI output is a suggestion, not an edit.** A language model can invent
  facts, so the result is shown in a dialog and applied only on confirmation.
  The prompt forbids adding information that is not in the input.
- **AI hints about missing information are not persisted.** They go stale as
  soon as the text is edited.
- **No scraping of Fab pages.** Fab sits behind a bot challenge; I chose not to
  work around it and kept manual entry.

## Known limitations
- Fab has no public seller API, so all data is entered by hand.
- The Groq key is embedded in the app and can be extracted from the APK, so the
  app is not suitable for publishing without a proxy server.
- Revenue is an estimate (sales × price); it ignores discounts and platform fees.
- If an AI request is dismissed while still running, the late answer can reopen
  the suggestion dialog (the UI does not currently allow this).
- No database migrations: a schema change requires clearing app data.
<img width="606" height="1282" alt="image" src="https://github.com/user-attachments/assets/189d97d6-826a-4084-9eeb-233e9ff22115" />
<img width="658" height="1302" alt="image" src="https://github.com/user-attachments/assets/9b87c4e6-fbfc-4fef-983b-5517cd2dd740" />
<img width="622" height="1274" alt="image" src="https://github.com/user-attachments/assets/208b0fb4-ceab-4151-9417-9764c252e46c" />
<img width="654" height="1290" alt="image" src="https://github.com/user-attachments/assets/b3dd3c21-f66b-430f-9d08-c27c580f2ea5" />
<img width="634" height="1288" alt="image" src="https://github.com/user-attachments/assets/81b3f98d-7cf1-4fbb-b5e6-16fe4b4122e2" />
