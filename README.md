# GameRate - Video Game Rating Platform

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="GameRate Logo" width="120"/>
</p>

<p align="center">
  <strong>A comprehensive Android application for discovering, rating, and discussing video games</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#screenshots">Screenshots</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#installation">Installation</a> •
  <a href="#configuration">Configuration</a> •
  <a href="#documentation">Documentation</a>
</p>

---

## Overview

**GameRate** is a full-featured video game rating platform built with modern Android technologies. It enables:
- **Players** to discover games, write reviews, build personal collections, and connect with other gamers
- **Editors/Publishers** to publish games, monitor performance with interactive analytics dashboards

The application integrates with **Steam API** for game data import and uses **Firebase** as a complete backend solution.

---

## Features

### For Players

| Feature | Description |
|---------|-------------|
| **Game Discovery** | Browse and search games by title, genre, or editor |
| **Advanced Sorting** | Sort by highest rated, most reviewed, or trending |
| **Reviews** | Write, edit, and delete reviews with 1-5 star ratings |
| **Review Reliability** | See reliability scores based on reviewer account age and activity |
| **Bias Detection** | Warning labels for potentially biased reviews |
| **Report Reviews** | Report spam, offensive, or fake reviews |
| **Personal Collections** | Mark games as Liked, Played, or add to Wishlist |
| **Badges & Achievements** | Earn badges for milestones (first review, 5 reviews, 10 reviews) |
| **Public Profiles** | View other users' profiles, reviews, collections, and badges |
| **Follow Editors** | Follow game editors and see their published games |
| **Friends System** | Send/accept friend requests, manage friend list |
| **Notifications Center** | View friend requests and other notifications |
| **Real-time Chat** | Message friends with instant delivery |
| **Trending Games** | Discover popular games (daily/weekly/monthly) |
| **Dark/Light Mode** | Toggle theme with sync across devices |

### For Editors

| Feature | Description |
|---------|-------------|
| **Game Management** | Add, edit, delete games with image upload |
| **Steam Import** | Import game data directly from Steam API |
| **Game Edit History** | Track all create/update/delete actions on games |
| **Statistics Dashboard** | View total games, reviews, average rating |
| **Interactive Charts** | Rating distribution (Pie), Evolution (Line), Reviews/Day (Bar) |
| **Real-time Updates** | Statistics refresh automatically on new reviews |

### Platform Features

| Feature | Description |
|---------|-------------|
| **Authentication** | Email/Password and Google Sign-In |
| **Dark/Light Mode** | Theme preference synced across devices |
| **Multi-language** | English and French (i18n ready) |
| **Offline Support** | Firestore caching for offline access |
| **Real-time Sync** | Instant updates across devices |

---

## Screenshots

### Authentication
| Login | Signup |
|-------|--------|
| Email/password + Google Sign-In | Role selection (Player/Editor) |

### Games
| Game List | Game Details | Add Review |
|-----------|--------------|------------|
| Searchable with ratings | Full info + reviews | Star rating + comment |

### Profile & Social
| My Profile | Public Profile | Chat |
|------------|----------------|------|
| Collections + stats | Reviews + wishlist | Real-time messaging |

### Editor Dashboard
| Overview | Pie Chart | Line Chart |
|----------|-----------|------------|
| Summary cards | Rating distribution | Rating evolution |

---

## Architecture

The application follows **MVVM (Model-View-ViewModel)** architecture with **Repository pattern**:

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Jetpack Compose Screens                   │  │
│  │   LoginScreen • GameListScreen • ProfileScreen • ...   │  │
│  └───────────────────────────────────────────────────────┘  │
│                            │                                 │
│                            ▼                                 │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                     ViewModels                         │  │
│  │   AuthVM • GameVM • UserVM • StatsVM • ChatVM • ...    │  │
│  │              StateFlow + collectAsState                │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                        Data Layer                            │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                    Repositories                        │  │
│  │   GameRepo • UserRepo • StatsRepo • ChatRepo • Steam   │  │
│  └───────────────────────────────────────────────────────┘  │
│                            │                                 │
│              ┌─────────────┴─────────────┐                  │
│              ▼                           ▼                  │
│  ┌─────────────────────┐     ┌─────────────────────┐       │
│  │      Firebase       │     │     Steam API       │       │
│  │ Auth•Firestore•Store│     │   Retrofit+OkHttp   │       │
│  └─────────────────────┘     └─────────────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

### Project Structure

```
com.isep.kotlinproject/
├── api/                    # External API interfaces
│   ├── SteamApiService.kt
│   └── SteamModels.kt
├── model/                  # Data classes
│   ├── User.kt
│   ├── Game.kt
│   ├── Review.kt
│   ├── GameStats.kt
│   ├── Badge.kt
│   ├── ReviewReport.kt
│   ├── ReviewReliability.kt
│   ├── BiasIndicator.kt
│   ├── GameHistory.kt
│   ├── Chat.kt
│   ├── Message.kt
│   ├── FriendRequest.kt
│   └── Trending.kt
├── repository/             # Data access layer
│   ├── GameRepository.kt
│   ├── UserRepository.kt
│   ├── StatsRepository.kt
│   ├── ChatRepository.kt
│   └── SteamRepository.kt
├── viewmodel/              # Business logic
│   ├── AuthViewModel.kt
│   ├── GameViewModel.kt
│   ├── UserViewModel.kt
│   ├── UsersViewModel.kt
│   ├── StatsViewModel.kt
│   └── ChatViewModel.kt
├── ui/                     # Presentation layer
│   ├── auth/               # Login, Signup
│   ├── game/               # GameList, GameDetail, AddEditGame
│   ├── profile/            # MyProfile, ProfileScreen
│   ├── users/              # UsersListScreen, PublicUserProfile
│   ├── editor/             # EditorDashboard
│   ├── social/             # Friends, Chat, ChatsList
│   ├── trending/           # TrendingScreen
│   ├── main/               # MainScreen with BottomNav
│   ├── onboarding/         # Onboarding flow
│   ├── components/         # Reusable UI components
│   └── theme/              # Material Design 3 theme
├── util/                   # Utilities
│   └── LocaleManager.kt
└── MainActivity.kt
```

---

## Tech Stack

| Category | Technology | Version |
|----------|------------|---------|
| **Language** | Kotlin | 2.0.0 |
| **UI Framework** | Jetpack Compose | 2024.04.01 |
| **Navigation** | Navigation Compose | 2.8.9 |
| **State Management** | StateFlow | - |
| **Image Loading** | Coil | 2.6.0 |
| **Charts** | MPAndroidChart | 3.1.0 |
| **Networking** | Retrofit + OkHttp | 2.11.0 |
| **Backend** | Firebase | 33.7.0 |
| **Auth** | Firebase Auth + Google | 21.3.0 |
| **Database** | Cloud Firestore | - |
| **Storage** | Firebase Storage | - |

---

## Installation

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Firebase CLI: `npm install -g firebase-tools`
- A Firebase project

### 1. Clone the Repository

```bash
git clone https://github.com/Clashix/Kotlin-finalProject-ISEP.git
cd KotlinProject/Kotlin-finalProject-ISEP
```

### 2. Open in Android Studio

1. Open Android Studio
2. Select "Open an existing project"
3. Navigate to `Kotlin-finalProject-ISEP` folder
4. Wait for Gradle sync to complete

### 3. Configure Firebase

See [Configuration](#configuration) section below.

### 4. Run the App

```bash
./gradlew assembleDebug
```

Or click the Run button in Android Studio.

---

## Configuration

### Firebase Setup

#### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" and follow the wizard
3. Enable Google Analytics (optional)

#### 2. Add Android App

1. Click "Add app" and select Android
2. Package name: `com.isep.kotlinproject`
3. Download `google-services.json`
4. Place it in `app/` directory

#### 3. Add SHA-1 Fingerprint (for Google Sign-In)

```bash
./gradlew signingReport
```

Copy the SHA-1 and add it to Firebase Console > Project Settings > Your apps

#### 4. Enable Authentication

1. Go to Authentication > Sign-in method
2. Enable "Email/Password"
3. Enable "Google" (add support email)

#### 5. Create Firestore Database

1. Go to Firestore Database
2. Click "Create database"
3. Start in test mode initially

#### 6. Enable Storage

1. Go to Storage
2. Click "Get started"

### Deploy Firebase Services

```bash
# Login to Firebase
firebase login

# Deploy security rules
firebase deploy --only firestore:rules

# Deploy indexes
firebase deploy --only firestore:indexes

# Deploy storage rules
firebase deploy --only storage
```

---

## Firestore Schema

| Collection | Description |
|------------|-------------|
| `users` | User profiles with social data and theme preference |
| `users/{uid}/badges` | Earned badges (subcollection) |
| `games` | Game catalog with metadata |
| `games/{id}/reviews` | Game reviews (subcollection) |
| `game_stats` | Computed statistics |
| `game_stats/{id}/daily_stats` | Daily statistics |
| `game_history` | Editor action history (create/update/delete) |
| `review_reports` | Reported reviews for moderation |
| `chats` | Chat conversations |
| `chats/{id}/messages` | Chat messages |
| `friend_requests` | Friend request records |
| `notifications` | User notifications |
| `trending` | Trending games rankings |

---

## Security Rules

The app implements comprehensive Firestore security rules:

| Rule | Description |
|------|-------------|
| **Users** | Read: all authenticated; Write: owner only |
| **Games** | Read: all authenticated; Write: editor owner only |
| **Reviews** | Read: all authenticated; Write: review author only |
| **Game Stats** | Read: all authenticated; Write: Cloud Functions only |
| **Chats** | Read/Write: participants only |
| **Friend Requests** | Read: sender or recipient; Write: sender only |

See `firestore.rules` for complete implementation.

---

## Building the APK

### Debug Build

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build

1. Create a keystore:
```bash
keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias release
```

2. Configure signing in `app/build.gradle.kts`

3. Build:
```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

---

## Testing

### Run Unit Tests

```bash
./gradlew testDebugUnitTest
```

### Run Instrumented Tests

```bash
./gradlew connectedDebugAndroidTest
```

### Testing Checklist

- [ ] User registration (Email + Google)
- [ ] Login/Logout
- [ ] Game CRUD (Editor)
- [ ] Review CRUD (Player)
- [ ] Review reporting
- [ ] Like/Played/Wishlist toggles
- [ ] Friend requests (send/accept/reject)
- [ ] Notifications center
- [ ] Real-time chat
- [ ] Statistics dashboard with charts
- [ ] Game edit history (Editor)
- [ ] Advanced game sorting
- [ ] Trending games
- [ ] Follow/Unfollow editors
- [ ] Badges display on profile
- [ ] Dark/Light mode toggle
- [ ] Language switching
- [ ] Profile editing
- [ ] Users directory and search
- [ ] Public profile viewing

---

## Localization

### Supported Languages

- English (default)
- French

### Adding a New Language

1. Create `res/values-{lang}/strings.xml`
2. Copy all strings from `res/values/strings.xml`
3. Translate each string
4. Update `LocaleManager.SUPPORTED_LOCALES`

---

## Performance Considerations

| Optimization | Implementation |
|--------------|----------------|
| **Pagination** | Cursor-based Firestore pagination |
| **Denormalization** | Game titles stored in reviews |
| **Client-side Compute** | Statistics and trending computed locally |
| **Indexing** | Composite indexes for queries |
| **Caching** | Firestore SDK local cache |
| **Debouncing** | 300ms delay on search input |

---

## Troubleshooting

### Google Sign-In Fails

**Cause**: Missing SHA-1 fingerprint

**Solution**:
```bash
./gradlew signingReport
```
Add SHA-1 to Firebase Console > Project Settings > Your apps

### Firestore Permission Denied

**Cause**: Security rules not deployed

**Solution**:
```bash
firebase deploy --only firestore:rules
```

### Query Requires Index

**Cause**: Missing composite index

**Solution**: Click the link in the error message or deploy indexes:
```bash
firebase deploy --only firestore:indexes
```

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

---

## Authors

- **Romain HERRENKNECHT** - *Developer* - [romain.herrenknecht@eleve.isep.fr](mailto:romain.herrenknecht@eleve.isep.fr)
- **Charles PEPINEAU** - *Developer* - [charles.pepineau@eleve.isep.fr](mailto:charles.pepineau@eleve.isep.fr)
- **Francois BERI** - *Developer* - [francois.beri@eleve.isep.fr](mailto:francois.beri@eleve.isep.fr)

---

## Acknowledgments

- [Firebase Documentation](https://firebase.google.com/docs)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
- [Coil](https://coil-kt.github.io/coil/)
- [Steam Web API](https://partner.steamgames.com/doc/webapi)

---

## License

This project was developed for educational purposes as part of the Android Development course at **ISEP - Institut Superieur d'Electronique de Paris**.

---

<p align="center">
  Made with Kotlin and Firebase
</p>
