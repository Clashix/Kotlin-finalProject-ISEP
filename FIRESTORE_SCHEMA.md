# Firestore Database Schema

## Collections Overview

```
firestore-root/
|
+-- users/{userId}
|   +-- Document fields: uid, displayName, email, photoURL, role, locale, 
|   |                    createdAt, friends[], likedGames[], playedGames[], wishlist[]
|   |
|   +-- (subcollection) notifications/{notificationId}
|       +-- Document fields: type, fromUserId, fromUserName, message, timestamp, read
|
+-- games/{gameId}
|   +-- Document fields: id, editorId, editorName, title, description, genre, 
|   |                    releaseDate, imageUrl, steamAppId, developer,
|   |                    averageRating, ratingCount, totalRatingSum,
|   |                    createdAt, updatedAt
|   |
|   +-- (subcollection) reviews/{reviewId}
|       +-- Document fields: id, gameId, userId, userName, userPhotoURL,
|                            rating, comment, timestamp, updatedAt
|
+-- game_stats/{gameId}
|   +-- Document fields: gameId, averageRating, totalReviews, ratingDistribution{},
|   |                    dailyStats[], lastUpdated
|   |
|   +-- (subcollection) daily_stats/{date}
|       +-- Document fields: date, reviewCount, averageRating, ratingDistribution{}
|
+-- chats/{chatId}
|   +-- Document fields: participants[], participantNames{}, lastMessage, 
|   |                    lastMessageTimestamp, createdAt
|   |
|   +-- (subcollection) messages/{messageId}
|       +-- Document fields: senderId, senderName, content, timestamp, read
|
+-- friend_requests/{requestId}
|   +-- Document fields: fromUserId, fromUserName, fromUserPhotoURL,
|                        toUserId, status, timestamp
|
+-- trending/{period}
    +-- Document fields: period, games[], lastUpdated
```

---

## Detailed Collection Schemas

### 1. Users Collection (`/users/{userId}`)

```typescript
interface User {
  uid: string;                    // Firebase Auth UID
  displayName: string;            // User display name
  email: string;                  // User email
  photoURL: string;               // Profile photo URL (Firebase Storage or external)
  role: "player" | "editor";      // User role
  locale: "fr" | "en";            // User preferred language
  createdAt: Timestamp;           // Account creation date
  friends: string[];              // Array of friend user IDs
  likedGames: string[];           // Array of liked game IDs
  playedGames: string[];          // Array of played game IDs
  wishlist: string[];             // Array of wishlist game IDs
}
```

**Indexes Required:**
- `role` (ASC) - For querying users by role
- `displayName` (ASC) - For searching users
- `createdAt` (DESC) - For sorting new users

---

### 2. Games Collection (`/games/{gameId}`)

```typescript
interface Game {
  id: string;                     // Document ID
  editorId: string;               // Editor's user ID who created this game
  editorName: string;             // Editor's display name
  title: string;                  // Game title
  description: string;            // Game description
  genre: string;                  // Game genre
  releaseDate: string;            // Release date (YYYY-MM-DD format)
  imageUrl: string;               // Cover image URL
  steamAppId: string | null;      // Steam App ID (optional)
  developer: string;              // Developer/Studio name
  averageRating: number;          // Calculated average rating (0-5)
  ratingCount: number;            // Total number of reviews
  totalRatingSum: number;         // Sum of all ratings (for average calculation)
  createdAt: Timestamp;           // Game creation date
  updatedAt: Timestamp;           // Last update date
}
```

**Indexes Required:**
- `editorId` (ASC) + `createdAt` (DESC) - For editor's games
- `genre` (ASC) + `averageRating` (DESC) - For filtering by genre
- `averageRating` (DESC) + `ratingCount` (DESC) - For trending/popular
- `title` (ASC) - For alphabetical listing

---

### 3. Reviews Subcollection (`/games/{gameId}/reviews/{reviewId}`)

```typescript
interface Review {
  id: string;                     // Document ID
  gameId: string;                 // Parent game ID
  userId: string;                 // Reviewer's user ID
  userName: string;               // Reviewer's display name
  userPhotoURL: string;           // Reviewer's photo URL
  rating: number;                 // Rating 1-5
  comment: string;                // Review text
  timestamp: Timestamp;           // Creation timestamp
  updatedAt: Timestamp;           // Last update timestamp
}
```

**Indexes Required:**
- `userId` (ASC) - For finding user's review
- `timestamp` (DESC) - For chronological listing
- `rating` (ASC) - For filtering by rating

---

### 4. Game Stats Collection (`/game_stats/{gameId}`)

> **Note:** This collection is managed ONLY by Cloud Functions.

```typescript
interface GameStats {
  gameId: string;                           // Game ID
  averageRating: number;                    // Current average rating
  totalReviews: number;                     // Total review count
  ratingDistribution: {                     // Distribution of ratings
    "1": number;
    "2": number;
    "3": number;
    "4": number;
    "5": number;
  };
  lastUpdated: Timestamp;                   // Last computation timestamp
}
```

### 4.1 Daily Stats Subcollection (`/game_stats/{gameId}/daily_stats/{date}`)

```typescript
interface DailyStats {
  date: string;                             // Date in YYYY-MM-DD format
  reviewCount: number;                      // Reviews posted on this day
  totalRatingSum: number;                   // Sum of ratings for the day
  averageRating: number;                    // Average rating for the day
  ratingDistribution: {                     // Distribution for this day
    "1": number;
    "2": number;
    "3": number;
    "4": number;
    "5": number;
  };
}
```

---

### 5. Chats Collection (`/chats/{chatId}`)

```typescript
interface Chat {
  participants: string[];                   // Array of 2 user IDs
  participantNames: {                       // Map of userId -> displayName
    [userId: string]: string;
  };
  lastMessage: string;                      // Last message preview
  lastMessageTimestamp: Timestamp;          // Last message time
  createdAt: Timestamp;                     // Chat creation time
}
```

**Index Required:**
- `participants` (ARRAY_CONTAINS) + `lastMessageTimestamp` (DESC)

### 5.1 Messages Subcollection (`/chats/{chatId}/messages/{messageId}`)

```typescript
interface Message {
  senderId: string;                         // Sender user ID
  senderName: string;                       // Sender display name
  content: string;                          // Message content
  timestamp: Timestamp;                     // Send time
  read: boolean;                            // Read status
}
```

---

### 6. Friend Requests Collection (`/friend_requests/{requestId}`)

```typescript
interface FriendRequest {
  fromUserId: string;                       // Sender user ID
  fromUserName: string;                     // Sender display name
  fromUserPhotoURL: string;                 // Sender photo URL
  toUserId: string;                         // Recipient user ID
  status: "pending" | "accepted" | "rejected";
  timestamp: Timestamp;                     // Request timestamp
}
```

**Indexes Required:**
- `toUserId` (ASC) + `status` (ASC) + `timestamp` (DESC)
- `fromUserId` (ASC) + `status` (ASC)

---

### 7. Trending Collection (`/trending/{period}`)

```typescript
interface Trending {
  period: "daily" | "weekly" | "monthly";   // Time period
  games: TrendingGame[];                    // Ranked games
  lastUpdated: Timestamp;                   // Last computation
}

interface TrendingGame {
  gameId: string;
  title: string;
  imageUrl: string;
  averageRating: number;
  recentReviewCount: number;                // Reviews in this period
  score: number;                            // Computed trending score
  rank: number;                             // Position in ranking
}
```

---

## Composite Indexes (firestore.indexes.json)

```json
{
  "indexes": [
    {
      "collectionGroup": "games",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "editorId", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "games",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "genre", "order": "ASCENDING" },
        { "fieldPath": "averageRating", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "reviews",
      "queryScope": "COLLECTION_GROUP",
      "fields": [
        { "fieldPath": "userId", "order": "ASCENDING" },
        { "fieldPath": "timestamp", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "chats",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "participants", "arrayConfig": "CONTAINS" },
        { "fieldPath": "lastMessageTimestamp", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "friend_requests",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "toUserId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "timestamp", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "daily_stats",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "date", "order": "DESCENDING" }
      ]
    }
  ],
  "fieldOverrides": []
}
```

---

## Data Migration Notes

To migrate existing users, run this one-time script in Firebase Console or via Admin SDK:

```javascript
// Add missing fields to existing users
const usersRef = db.collection('users');
const snapshot = await usersRef.get();

const batch = db.batch();
snapshot.docs.forEach(doc => {
  const data = doc.data();
  const updates = {};
  
  if (!data.friends) updates.friends = [];
  if (!data.likedGames) updates.likedGames = [];
  if (!data.playedGames) updates.playedGames = [];
  if (!data.wishlist) updates.wishlist = [];
  if (!data.locale) updates.locale = 'en';
  if (!data.photoURL) updates.photoURL = '';
  if (!data.createdAt) updates.createdAt = admin.firestore.FieldValue.serverTimestamp();
  if (!data.uid) updates.uid = doc.id;
  if (!data.displayName && data.name) updates.displayName = data.name;
  
  if (Object.keys(updates).length > 0) {
    batch.update(doc.ref, updates);
  }
});

await batch.commit();
```
