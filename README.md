# Tanks QA Automation Framework

A Java-based QA Automation framework built to test Unity's official **Tanks!** sample game, demonstrating that game testing is fundamentally the same discipline as web/mobile test automation.

## Overview

This project treats a Unity game as any other application under test. Using **AltTester** as the automation driver, the framework follows the **Page Object Model (POM)** pattern — the same approach used in Selenium/Appium frameworks — applied to game scenes and UI elements.

> "Game testing is just QA. The application happens to be a game."

## Tech Stack

| Layer | Technology |
|---|---|
| Game Engine | Unity 6 LTS |
| Game Under Test | Unity Tanks! (official sample) |
| Automation Driver | AltTester SDK 2.2.0 |
| Test Framework | Java + JUnit 5 |
| Build Tool | Maven 3.9 |
| Version Control | Git + GitHub |
| CI/CD | GitHub Actions (coming soon) |

## Framework Structure

```
src/test/java/com/tanksqa/
├── base/
│   └── BaseTest.java          # Driver setup and teardown
├── pages/                     # Page Object Model
│   ├── MainMenuPage.java      # Main menu interactions
│   ├── GameplayPage.java      # In-game actions and assertions
│   └── GameOverPage.java      # End screen assertions
├── tests/
│   ├── NavigationTests.java   # Menu and scene navigation tests
│   └── GameplayTests.java     # Core gameplay scenario tests
└── utils/
    └── WaitUtils.java         # Custom wait helpers
```

## The POM Parallel

| Web Automation | Game Automation (AltTester) |
|---|---|
| Browser | Instrumented game build |
| DevTools / Locators | AltTester Desktop inspector |
| `driver.findElement(By.id)` | `altDriver.findObject(By.NAME)` |
| `.click()` | `.click()` |
| `.getText()` | `.getText()` |
| NUnit / TestNG | JUnit 5 |
| Jenkins / GitHub Actions | GameCI / GitHub Actions |

## Prerequisites

- Java 11+
- Maven 3.6+
- Unity 6 LTS
- AltTester Desktop (Community)
- Tanks! project instrumented with AltTester Unity SDK

## Getting Started

### 1. Clone the repo
```bash
git clone https://github.com/klincarovt/unity-tanks-qa-automation.git
cd tanks-qa-framework
```

### 2. Install dependencies
```bash
mvn install
```

### 3. Instrument the Unity game
- Open the Tanks! project in Unity
- Go to AltTester menu → Instrument build
- Run the instrumented build

### 4. Start AltTester Desktop
- Open AltTester Desktop
- Start the server

### 5. Run the tests
```bash
mvn test
```

## Test Scenarios

- Verify main menu loads on game launch
- Verify clicking Play starts the game
- Verify gameplay UI is visible during a round
- Verify round text exists during gameplay
- Verify game over screen appears at end of game

## Key Concepts Demonstrated

- **Page Object Model** applied to game scenes
- **AltTester Driver** as the game equivalent of Selenium WebDriver
- **Game object inspection** equivalent to browser DevTools
- **Separation of concerns** — game project and test framework are independent repos

## Related Repository

Unity Tanks! game project (instrumented):
`https://github.com/klincarovt/unity-tanks-qa-automation`

## Presentation

Built as part of a QA Automation research presentation exploring game testing automation as an emerging discipline in software quality engineering.
