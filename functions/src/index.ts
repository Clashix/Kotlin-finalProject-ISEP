/**
 * Firebase Cloud Functions for Game Rating Platform
 * 
 * These functions automatically compute and maintain game statistics
 * when reviews are created, updated, or deleted.
 * 
 * Collections managed:
 * - game_stats/{gameId} - Aggregated statistics per game
 * - game_stats/{gameId}/daily_stats/{date} - Daily statistics
 * - trending/{period} - Trending games rankings
 */

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

// Initialize Firebase Admin SDK
admin.initializeApp();

const db = admin.firestore();

// =====================================================
// TYPE DEFINITIONS
// =====================================================

interface Review {
  id: string;
  gameId: string;
  userId: string;
  userName: string;
  userPhotoURL?: string;
  rating: number;
  comment: string;
  timestamp: admin.firestore.Timestamp;
}

interface RatingDistribution {
  "1": number;
  "2": number;
  "3": number;
  "4": number;
  "5": number;
}

interface GameStats {
  gameId: string;
  averageRating: number;
  totalReviews: number;
  ratingDistribution: RatingDistribution;
  lastUpdated: admin.firestore.Timestamp;
}

interface DailyStats {
  date: string;
  reviewCount: number;
  totalRatingSum: number;
  averageRating: number;
  ratingDistribution: RatingDistribution;
}

interface TrendingGame {
  gameId: string;
  title: string;
  imageUrl: string;
  averageRating: number;
  recentReviewCount: number;
  score: number;
  rank: number;
}

// =====================================================
// HELPER FUNCTIONS
// =====================================================

/**
 * Get today's date in YYYY-MM-DD format
 */
function getTodayDate(): string {
  const now = new Date();
  return now.toISOString().split("T")[0];
}

/**
 * Create an empty rating distribution object
 */
function emptyRatingDistribution(): RatingDistribution {
  return { "1": 0, "2": 0, "3": 0, "4": 0, "5": 0 };
}

/**
 * Safely convert rating to distribution key
 */
function ratingKey(rating: number): keyof RatingDistribution {
  const validRating = Math.min(5, Math.max(1, Math.round(rating)));
  return validRating.toString() as keyof RatingDistribution;
}

/**
 * Calculate average rating from distribution
 */
function calculateAverageFromDistribution(
  distribution: RatingDistribution
): number {
  let totalSum = 0;
  let totalCount = 0;

  for (let i = 1; i <= 5; i++) {
    const key = i.toString() as keyof RatingDistribution;
    totalSum += i * distribution[key];
    totalCount += distribution[key];
  }

  return totalCount > 0 ? totalSum / totalCount : 0;
}

// =====================================================
// REVIEW CREATED TRIGGER
// =====================================================

/**
 * Trigger: When a new review is created
 * Actions:
 * - Update game_stats document
 * - Update daily_stats for today
 * - Update game document with new averages
 */
export const onReviewCreated = functions.firestore
  .document("games/{gameId}/reviews/{reviewId}")
  .onCreate(async (snapshot, context) => {
    const { gameId } = context.params;
    const review = snapshot.data() as Review;
    const today = getTodayDate();

    console.log(`Review created for game ${gameId} with rating ${review.rating}`);

    try {
      await db.runTransaction(async (transaction) => {
        // References
        const gameStatsRef = db.collection("game_stats").doc(gameId);
        const dailyStatsRef = gameStatsRef.collection("daily_stats").doc(today);

        // Get current stats
        const gameStatsDoc = await transaction.get(gameStatsRef);
        const dailyStatsDoc = await transaction.get(dailyStatsRef);

        // Initialize or update game stats
        let gameStats: GameStats;
        if (gameStatsDoc.exists) {
          const currentStats = gameStatsDoc.data() as GameStats;
          const key = ratingKey(review.rating);
          currentStats.ratingDistribution[key]++;
          currentStats.totalReviews++;
          currentStats.averageRating = calculateAverageFromDistribution(
            currentStats.ratingDistribution
          );
          currentStats.lastUpdated = admin.firestore.Timestamp.now();
          gameStats = currentStats;
        } else {
          const distribution = emptyRatingDistribution();
          distribution[ratingKey(review.rating)] = 1;
          gameStats = {
            gameId,
            averageRating: review.rating,
            totalReviews: 1,
            ratingDistribution: distribution,
            lastUpdated: admin.firestore.Timestamp.now(),
          };
        }

        // Initialize or update daily stats
        let dailyStats: DailyStats;
        if (dailyStatsDoc.exists) {
          const currentDaily = dailyStatsDoc.data() as DailyStats;
          const key = ratingKey(review.rating);
          currentDaily.ratingDistribution[key]++;
          currentDaily.reviewCount++;
          currentDaily.totalRatingSum += review.rating;
          currentDaily.averageRating =
            currentDaily.totalRatingSum / currentDaily.reviewCount;
          dailyStats = currentDaily;
        } else {
          const distribution = emptyRatingDistribution();
          distribution[ratingKey(review.rating)] = 1;
          dailyStats = {
            date: today,
            reviewCount: 1,
            totalRatingSum: review.rating,
            averageRating: review.rating,
            ratingDistribution: distribution,
          };
        }

        // Write updates
        transaction.set(gameStatsRef, gameStats);
        transaction.set(dailyStatsRef, dailyStats);
      });

      console.log(`Successfully updated stats for game ${gameId}`);
    } catch (error) {
      console.error(`Error updating stats for game ${gameId}:`, error);
      throw error;
    }
  });

// =====================================================
// REVIEW UPDATED TRIGGER
// =====================================================

/**
 * Trigger: When a review is updated
 * Actions:
 * - Adjust rating distribution (remove old, add new)
 * - Recalculate averages
 */
export const onReviewUpdated = functions.firestore
  .document("games/{gameId}/reviews/{reviewId}")
  .onUpdate(async (change, context) => {
    const { gameId } = context.params;
    const beforeData = change.before.data() as Review;
    const afterData = change.after.data() as Review;
    const today = getTodayDate();

    // Only process if rating changed
    if (beforeData.rating === afterData.rating) {
      console.log("Rating unchanged, skipping stats update");
      return null;
    }

    console.log(
      `Review updated for game ${gameId}: ${beforeData.rating} -> ${afterData.rating}`
    );

    try {
      await db.runTransaction(async (transaction) => {
        const gameStatsRef = db.collection("game_stats").doc(gameId);
        const dailyStatsRef = gameStatsRef.collection("daily_stats").doc(today);

        const gameStatsDoc = await transaction.get(gameStatsRef);
        const dailyStatsDoc = await transaction.get(dailyStatsRef);

        if (!gameStatsDoc.exists) {
          console.error(`Game stats not found for ${gameId}`);
          return;
        }

        const gameStats = gameStatsDoc.data() as GameStats;
        const oldKey = ratingKey(beforeData.rating);
        const newKey = ratingKey(afterData.rating);

        // Update distribution
        gameStats.ratingDistribution[oldKey] = Math.max(
          0,
          gameStats.ratingDistribution[oldKey] - 1
        );
        gameStats.ratingDistribution[newKey]++;
        gameStats.averageRating = calculateAverageFromDistribution(
          gameStats.ratingDistribution
        );
        gameStats.lastUpdated = admin.firestore.Timestamp.now();

        // Update daily stats if exists
        if (dailyStatsDoc.exists) {
          const dailyStats = dailyStatsDoc.data() as DailyStats;
          dailyStats.ratingDistribution[oldKey] = Math.max(
            0,
            dailyStats.ratingDistribution[oldKey] - 1
          );
          dailyStats.ratingDistribution[newKey]++;
          dailyStats.totalRatingSum =
            dailyStats.totalRatingSum - beforeData.rating + afterData.rating;
          dailyStats.averageRating =
            dailyStats.reviewCount > 0
              ? dailyStats.totalRatingSum / dailyStats.reviewCount
              : 0;
          transaction.set(dailyStatsRef, dailyStats);
        }

        transaction.set(gameStatsRef, gameStats);
      });

      console.log(`Successfully updated stats for game ${gameId}`);
    } catch (error) {
      console.error(`Error updating stats for game ${gameId}:`, error);
      throw error;
    }

    return null;
  });

// =====================================================
// REVIEW DELETED TRIGGER
// =====================================================

/**
 * Trigger: When a review is deleted
 * Actions:
 * - Decrement counts
 * - Remove from rating distribution
 * - Recalculate averages
 */
export const onReviewDeleted = functions.firestore
  .document("games/{gameId}/reviews/{reviewId}")
  .onDelete(async (snapshot, context) => {
    const { gameId } = context.params;
    const review = snapshot.data() as Review;
    const today = getTodayDate();

    console.log(
      `Review deleted for game ${gameId} with rating ${review.rating}`
    );

    try {
      await db.runTransaction(async (transaction) => {
        const gameStatsRef = db.collection("game_stats").doc(gameId);
        const dailyStatsRef = gameStatsRef.collection("daily_stats").doc(today);

        const gameStatsDoc = await transaction.get(gameStatsRef);
        const dailyStatsDoc = await transaction.get(dailyStatsRef);

        if (!gameStatsDoc.exists) {
          console.error(`Game stats not found for ${gameId}`);
          return;
        }

        const gameStats = gameStatsDoc.data() as GameStats;
        const key = ratingKey(review.rating);

        // Update distribution
        gameStats.ratingDistribution[key] = Math.max(
          0,
          gameStats.ratingDistribution[key] - 1
        );
        gameStats.totalReviews = Math.max(0, gameStats.totalReviews - 1);
        gameStats.averageRating =
          gameStats.totalReviews > 0
            ? calculateAverageFromDistribution(gameStats.ratingDistribution)
            : 0;
        gameStats.lastUpdated = admin.firestore.Timestamp.now();

        // Update daily stats if exists
        if (dailyStatsDoc.exists) {
          const dailyStats = dailyStatsDoc.data() as DailyStats;
          dailyStats.ratingDistribution[key] = Math.max(
            0,
            dailyStats.ratingDistribution[key] - 1
          );
          dailyStats.reviewCount = Math.max(0, dailyStats.reviewCount - 1);
          dailyStats.totalRatingSum = Math.max(
            0,
            dailyStats.totalRatingSum - review.rating
          );
          dailyStats.averageRating =
            dailyStats.reviewCount > 0
              ? dailyStats.totalRatingSum / dailyStats.reviewCount
              : 0;
          transaction.set(dailyStatsRef, dailyStats);
        }

        transaction.set(gameStatsRef, gameStats);
      });

      console.log(`Successfully updated stats for game ${gameId}`);
    } catch (error) {
      console.error(`Error updating stats for game ${gameId}:`, error);
      throw error;
    }
  });

// =====================================================
// TRENDING GAMES COMPUTATION (Scheduled)
// =====================================================

/**
 * Scheduled function: Runs every hour
 * Computes trending games based on:
 * - Recent review activity
 * - Average rating
 * - Total reviews
 * 
 * Score formula: (recentReviews * 2) + (avgRating * 10) + (log(totalReviews) * 5)
 */
export const computeTrendingGames = functions.pubsub
  .schedule("every 1 hours")
  .onRun(async () => {
    console.log("Computing trending games...");

    const now = new Date();
    const oneDayAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);
    const oneWeekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
    const oneMonthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);

    try {
      // Get all games with their stats
      const gamesSnapshot = await db.collection("games").get();
      const gameStatsSnapshot = await db.collection("game_stats").get();

      // Create stats map for quick lookup
      const statsMap = new Map<string, GameStats>();
      gameStatsSnapshot.docs.forEach((doc) => {
        statsMap.set(doc.id, doc.data() as GameStats);
      });

      // Compute scores for each period
      const dailyScores: TrendingGame[] = [];
      const weeklyScores: TrendingGame[] = [];
      const monthlyScores: TrendingGame[] = [];

      for (const gameDoc of gamesSnapshot.docs) {
        const gameData = gameDoc.data();
        const gameId = gameDoc.id;
        const stats = statsMap.get(gameId);

        if (!stats || stats.totalReviews === 0) continue;

        // Count recent reviews for each period
        const reviewsRef = db
          .collection("games")
          .doc(gameId)
          .collection("reviews");

        const dailyReviews = await reviewsRef
          .where("timestamp", ">=", admin.firestore.Timestamp.fromDate(oneDayAgo))
          .count()
          .get();

        const weeklyReviews = await reviewsRef
          .where("timestamp", ">=", admin.firestore.Timestamp.fromDate(oneWeekAgo))
          .count()
          .get();

        const monthlyReviews = await reviewsRef
          .where("timestamp", ">=", admin.firestore.Timestamp.fromDate(oneMonthAgo))
          .count()
          .get();

        const baseGame = {
          gameId,
          title: gameData.title || "",
          imageUrl: gameData.imageUrl || "",
          averageRating: stats.averageRating,
          rank: 0,
        };

        // Calculate scores using the formula
        const calcScore = (recentCount: number): number => {
          return (
            recentCount * 2 +
            stats.averageRating * 10 +
            Math.log10(stats.totalReviews + 1) * 5
          );
        };

        if (dailyReviews.data().count > 0) {
          dailyScores.push({
            ...baseGame,
            recentReviewCount: dailyReviews.data().count,
            score: calcScore(dailyReviews.data().count),
          });
        }

        if (weeklyReviews.data().count > 0) {
          weeklyScores.push({
            ...baseGame,
            recentReviewCount: weeklyReviews.data().count,
            score: calcScore(weeklyReviews.data().count),
          });
        }

        if (monthlyReviews.data().count > 0) {
          monthlyScores.push({
            ...baseGame,
            recentReviewCount: monthlyReviews.data().count,
            score: calcScore(monthlyReviews.data().count),
          });
        }
      }

      // Sort by score and assign ranks
      const assignRanks = (games: TrendingGame[]): TrendingGame[] => {
        return games
          .sort((a, b) => b.score - a.score)
          .slice(0, 50)
          .map((game, index) => ({ ...game, rank: index + 1 }));
      };

      const trendingBatch = db.batch();
      const timestamp = admin.firestore.Timestamp.now();

      // Write daily trending
      trendingBatch.set(db.collection("trending").doc("daily"), {
        period: "daily",
        games: assignRanks(dailyScores),
        lastUpdated: timestamp,
      });

      // Write weekly trending
      trendingBatch.set(db.collection("trending").doc("weekly"), {
        period: "weekly",
        games: assignRanks(weeklyScores),
        lastUpdated: timestamp,
      });

      // Write monthly trending
      trendingBatch.set(db.collection("trending").doc("monthly"), {
        period: "monthly",
        games: assignRanks(monthlyScores),
        lastUpdated: timestamp,
      });

      await trendingBatch.commit();

      console.log("Trending games computed successfully");
      return null;
    } catch (error) {
      console.error("Error computing trending games:", error);
      throw error;
    }
  });

// =====================================================
// FRIEND REQUEST ACCEPTED HANDLER
// =====================================================

/**
 * Trigger: When a friend request is updated to 'accepted'
 * Actions:
 * - Add each user to the other's friends list
 * - Create notification for the requester
 */
export const onFriendRequestAccepted = functions.firestore
  .document("friend_requests/{requestId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();

    // Only process if status changed to accepted
    if (before.status === "pending" && after.status === "accepted") {
      const { fromUserId, toUserId, fromUserName } = after;

      console.log(`Friend request accepted: ${fromUserId} <-> ${toUserId}`);

      try {
        const batch = db.batch();

        // Add to both users' friends lists
        const fromUserRef = db.collection("users").doc(fromUserId);
        const toUserRef = db.collection("users").doc(toUserId);

        batch.update(fromUserRef, {
          friends: admin.firestore.FieldValue.arrayUnion(toUserId),
        });

        batch.update(toUserRef, {
          friends: admin.firestore.FieldValue.arrayUnion(fromUserId),
        });

        // Get the accepting user's name for the notification
        const toUserDoc = await toUserRef.get();
        const toUserName = toUserDoc.data()?.displayName || "Someone";

        // Create notification for the requester
        const notificationRef = fromUserRef
          .collection("notifications")
          .doc();
        batch.set(notificationRef, {
          type: "friend_accepted",
          fromUserId: toUserId,
          fromUserName: toUserName,
          message: `${toUserName} accepted your friend request!`,
          timestamp: admin.firestore.Timestamp.now(),
          read: false,
        });

        await batch.commit();

        console.log(`Successfully processed friend request acceptance`);
      } catch (error) {
        console.error("Error processing friend request:", error);
        throw error;
      }
    }

    return null;
  });

// =====================================================
// USER CLEANUP ON DELETE
// =====================================================

/**
 * Trigger: When a user is deleted
 * Actions:
 * - Remove user from all friends lists
 * - Delete user's friend requests
 * - Delete user's chats (optional - could just mark as deleted)
 */
export const onUserDeleted = functions.firestore
  .document("users/{userId}")
  .onDelete(async (snapshot, context) => {
    const { userId } = context.params;
    const userData = snapshot.data();

    console.log(`User deleted: ${userId}`);

    try {
      const batch = db.batch();

      // Remove from friends' lists
      if (userData.friends && Array.isArray(userData.friends)) {
        for (const friendId of userData.friends) {
          const friendRef = db.collection("users").doc(friendId);
          batch.update(friendRef, {
            friends: admin.firestore.FieldValue.arrayRemove(userId),
          });
        }
      }

      // Delete pending friend requests (both sent and received)
      const sentRequests = await db
        .collection("friend_requests")
        .where("fromUserId", "==", userId)
        .get();

      const receivedRequests = await db
        .collection("friend_requests")
        .where("toUserId", "==", userId)
        .get();

      sentRequests.docs.forEach((doc) => batch.delete(doc.ref));
      receivedRequests.docs.forEach((doc) => batch.delete(doc.ref));

      await batch.commit();

      console.log(`Successfully cleaned up data for deleted user ${userId}`);
    } catch (error) {
      console.error("Error cleaning up user data:", error);
      throw error;
    }
  });

// =====================================================
// GAME DELETED CLEANUP
// =====================================================

/**
 * Trigger: When a game is deleted
 * Actions:
 * - Delete game_stats document
 * - Remove from users' likedGames, playedGames, wishlist
 */
export const onGameDeleted = functions.firestore
  .document("games/{gameId}")
  .onDelete(async (snapshot, context) => {
    const { gameId } = context.params;

    console.log(`Game deleted: ${gameId}`);

    try {
      // Delete game stats and its subcollections
      const gameStatsRef = db.collection("game_stats").doc(gameId);
      const dailyStatsSnapshot = await gameStatsRef
        .collection("daily_stats")
        .get();

      const batch = db.batch();

      // Delete daily stats subcollection
      dailyStatsSnapshot.docs.forEach((doc) => batch.delete(doc.ref));
      batch.delete(gameStatsRef);

      await batch.commit();

      // Remove game from all users' lists (batched for large datasets)
      const usersWithGame = await db
        .collection("users")
        .where("likedGames", "array-contains", gameId)
        .get();

      const usersWithPlayed = await db
        .collection("users")
        .where("playedGames", "array-contains", gameId)
        .get();

      const usersWithWishlist = await db
        .collection("users")
        .where("wishlist", "array-contains", gameId)
        .get();

      const userBatch = db.batch();
      const processedUsers = new Set<string>();

      const processUser = (doc: admin.firestore.QueryDocumentSnapshot) => {
        if (!processedUsers.has(doc.id)) {
          processedUsers.add(doc.id);
          userBatch.update(doc.ref, {
            likedGames: admin.firestore.FieldValue.arrayRemove(gameId),
            playedGames: admin.firestore.FieldValue.arrayRemove(gameId),
            wishlist: admin.firestore.FieldValue.arrayRemove(gameId),
          });
        }
      };

      usersWithGame.docs.forEach(processUser);
      usersWithPlayed.docs.forEach(processUser);
      usersWithWishlist.docs.forEach(processUser);

      if (processedUsers.size > 0) {
        await userBatch.commit();
      }

      console.log(`Successfully cleaned up data for deleted game ${gameId}`);
    } catch (error) {
      console.error("Error cleaning up game data:", error);
      throw error;
    }
  });
