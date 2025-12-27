package example;

import cachedb.CacheDB;
import cachedb.SimpleDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Random;

/**
 * Social Media Feed System Demo
 * 
 * Demonstrates CacheDB for social media features:
 * - Post caching
 * - User feed generation
 * - Like/comment tracking
 * - Follow relationships
 * - Trending posts
 * 
 * This is ideal for:
 * - Social media platforms
 * - Content management systems
 * - News feed applications
 * - Community platforms
 */
public class SocialFeedDemo {

    private static CacheDB cache;
    private static Random random = new Random();

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║          Social Media Feed System Demo                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        // Initialize CacheDB
        DataSource ds = new SimpleDataSource(
                "jdbc:mysql://localhost:3306/cachedb",
                "root",
                "defg5678@"
        );

        cache = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(15)  // Posts cached for 15 seconds
                .build();

        System.out.println("✓ CacheDB initialized\n");

        demonstratePostCreation();
        demonstrateLikeSystem();
        demonstrateCommentSystem();
        demonstrateFollowRelationships();
        demonstrateTrendingPosts();
        demonstrateFeedGeneration();

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║              Social Feed Demo Complete!                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
    }

    /**
     * Scenario 1: Post Creation and Caching
     */
    private static void demonstratePostCreation() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 1: Post Creation and Caching");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        System.out.println("📝 Users creating posts...");

        // User 1 creates a post
        int postId1 = 2001;
        int userId1 = 5001;
        cache.set("posts", Map.of("post_id", postId1),
                Map.of("user_id", userId1,
                       "username", "alice_writer",
                       "content", "Just finished reading an amazing book! 📚",
                       "likes", 0,
                       "comments", 0,
                       "created_at", System.currentTimeMillis(),
                       "visibility", "public"));

        // User 2 creates a post
        int postId2 = 2002;
        int userId2 = 5002;
        cache.set("posts", Map.of("post_id", postId2),
                Map.of("user_id", userId2,
                       "username", "bob_photographer",
                       "content", "Beautiful sunset today! 🌅",
                       "likes", 0,
                       "comments", 0,
                       "created_at", System.currentTimeMillis(),
                       "visibility", "public"));

        System.out.println("\n🔍 Retrieving posts from cache (fast lookup)...");
        Map<String, Object> post1 = cache.get("posts", Map.of("post_id", postId1));
        System.out.println("  Post " + postId1 + " by @" + post1.get("username") + ":");
        System.out.println("    " + post1.get("content"));

        Map<String, Object> post2 = cache.get("posts", Map.of("post_id", postId2));
        System.out.println("  Post " + postId2 + " by @" + post2.get("username") + ":");
        System.out.println("    " + post2.get("content"));

        Thread.sleep(1000);
        System.out.println("\n✓ Post creation complete\n");
    }

    /**
     * Scenario 2: Like System
     */
    private static void demonstrateLikeSystem() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 2: Like System");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        int postId = 2001;
        System.out.println("👍 Users liking post " + postId + "...");

        // Get current like count
        Map<String, Object> post = cache.get("posts", Map.of("post_id", postId));
        int currentLikes = post != null ? (Integer) post.getOrDefault("likes", 0) : 0;

        // Simulate multiple likes
        for (int i = 0; i < 5; i++) {
            currentLikes++;
            cache.set("posts", Map.of("post_id", postId),
                    Map.of("user_id", post.get("user_id"),
                           "username", post.get("username"),
                           "content", post.get("content"),
                           "likes", currentLikes,
                           "comments", post.get("comments"),
                           "created_at", post.get("created_at"),
                           "visibility", post.get("visibility")));
        }

        Map<String, Object> updated = cache.get("posts", Map.of("post_id", postId));
        System.out.println("  Final like count: " + updated.get("likes"));

        // Track individual likes (composite key: post_id + user_id)
        System.out.println("\n💚 Tracking who liked the post...");
        int[] likers = {6001, 6002, 6003};
        for (int likerId : likers) {
            cache.set("post_likes", Map.of("post_id", postId, "user_id", likerId),
                    Map.of("liked_at", System.currentTimeMillis(),
                           "username", "user_" + likerId));
        }

        Map<String, Object> like1 = cache.get("post_likes",
                Map.of("post_id", postId, "user_id", 6001));
        System.out.println("  User " + like1.get("user_id") + " liked at: " + like1.get("liked_at"));

        Thread.sleep(1000);
        System.out.println("\n✓ Like system demonstration complete\n");
    }

    /**
     * Scenario 3: Comment System
     */
    private static void demonstrateCommentSystem() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 3: Comment System");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        int postId = 2001;
        System.out.println("💬 Users commenting on post " + postId + "...");

        // Add comments (composite key: post_id + comment_id)
        String[] comments = {
                "Great post!",
                "I totally agree!",
                "Thanks for sharing!"
        };

        for (int i = 0; i < comments.length; i++) {
            int commentId = 3001 + i;
            int commenterId = 7001 + i;
            
            cache.set("comments", Map.of("post_id", postId, "comment_id", commentId),
                    Map.of("user_id", commenterId,
                           "username", "commenter_" + commenterId,
                           "content", comments[i],
                           "created_at", System.currentTimeMillis()));

            // Update post comment count
            Map<String, Object> post = cache.get("posts", Map.of("post_id", postId));
            int commentCount = post != null ? (Integer) post.getOrDefault("comments", 0) : 0;
            cache.set("posts", Map.of("post_id", postId),
                    Map.of("user_id", post.get("user_id"),
                           "username", post.get("username"),
                           "content", post.get("content"),
                           "likes", post.get("likes"),
                           "comments", commentCount + 1,
                           "created_at", post.get("created_at"),
                           "visibility", post.get("visibility")));
        }

        System.out.println("\n📝 Retrieving comments...");
        for (int i = 0; i < comments.length; i++) {
            int commentId = 3001 + i;
            Map<String, Object> comment = cache.get("comments",
                    Map.of("post_id", postId, "comment_id", commentId));
            System.out.println("  @" + comment.get("username") + ": " + comment.get("content"));
        }

        Map<String, Object> post = cache.get("posts", Map.of("post_id", postId));
        System.out.println("\n  Total comments: " + post.get("comments"));

        Thread.sleep(1000);
        System.out.println("\n✓ Comment system demonstration complete\n");
    }

    /**
     * Scenario 4: Follow Relationships
     */
    private static void demonstrateFollowRelationships() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 4: Follow Relationships");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        System.out.println("👥 Setting up follow relationships...");

        int userId = 5001;
        int[] following = {5002, 5003, 5004};

        // User follows others (composite key: follower_id + following_id)
        for (int followId : following) {
            cache.set("follows", Map.of("follower_id", userId, "following_id", followId),
                    Map.of("followed_at", System.currentTimeMillis(),
                           "status", "active"));
        }

        System.out.println("\n📊 User " + userId + " is following:");
        for (int followId : following) {
            Map<String, Object> follow = cache.get("follows",
                    Map.of("follower_id", userId, "following_id", followId));
            if (follow != null) {
                System.out.println("  User " + followId + " (since: " + follow.get("followed_at") + ")");
            }
        }

        // Track follower counts
        for (int followId : following) {
            Map<String, Object> user = cache.get("users", Map.of("id", followId));
            int followers = user != null ? (Integer) user.getOrDefault("follower_count", 0) : 0;
            
            cache.set("users", Map.of("id", followId),
                    Map.of("follower_count", followers + 1,
                           "username", "user_" + followId));
        }

        Thread.sleep(1000);
        System.out.println("\n✓ Follow relationships demonstration complete\n");
    }

    /**
     * Scenario 5: Trending Posts
     */
    private static void demonstrateTrendingPosts() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 5: Trending Posts");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        System.out.println("🔥 Tracking trending posts...");

        int[] postIds = {2001, 2002, 2003, 2004, 2005};

        // Initialize posts with engagement metrics
        for (int postId : postIds) {
            int likes = random.nextInt(100);
            int comments = random.nextInt(20);
            int shares = random.nextInt(50);
            
            cache.set("posts", Map.of("post_id", postId),
                    Map.of("user_id", 5000 + postId,
                           "username", "user_" + postId,
                           "content", "Post content " + postId,
                           "likes", likes,
                           "comments", comments,
                           "shares", shares,
                           "created_at", System.currentTimeMillis() - random.nextInt(3600000)));

            // Calculate trending score
            int trendingScore = likes * 2 + comments * 5 + shares * 3;
            
            cache.set("trending", Map.of("post_id", postId),
                    Map.of("score", trendingScore,
                           "updated_at", System.currentTimeMillis()));
        }

        System.out.println("\n📈 Trending posts (by engagement score):");
        for (int postId : postIds) {
            Map<String, Object> trending = cache.get("trending", Map.of("post_id", postId));
            Map<String, Object> post = cache.get("posts", Map.of("post_id", postId));
            
            if (trending != null && post != null) {
                System.out.println("  Post " + postId + ":");
                System.out.println("    Score: " + trending.get("score"));
                System.out.println("    Likes: " + post.get("likes") + 
                                 ", Comments: " + post.get("comments") + 
                                 ", Shares: " + post.get("shares"));
            }
        }

        Thread.sleep(1000);
        System.out.println("\n✓ Trending posts demonstration complete\n");
    }

    /**
     * Scenario 6: Feed Generation
     */
    private static void demonstrateFeedGeneration() throws InterruptedException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Scenario 6: Personalized Feed Generation");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        int userId = 5001;
        System.out.println("📰 Generating personalized feed for user " + userId + "...");

        // Get posts from users that this user follows
        int[] followingIds = {5002, 5003, 5004};
        int[] feedPostIds = {2002, 2003, 2004};

        System.out.println("\n🔍 Fetching posts from followed users (all from cache)...");
        for (int postId : feedPostIds) {
            Map<String, Object> post = cache.get("posts", Map.of("post_id", postId));
            if (post != null) {
                System.out.println("  Post " + postId + " by @" + post.get("username") + ":");
                System.out.println("    " + post.get("content"));
                System.out.println("    👍 " + post.get("likes") + 
                                 " | 💬 " + post.get("comments"));
            }
        }

        // Cache user's feed preferences
        cache.set("user_preferences", Map.of("user_id", userId),
                Map.of("feed_type", "chronological",
                       "show_likes", true,
                       "show_comments", true,
                       "last_feed_update", System.currentTimeMillis()));

        Map<String, Object> preferences = cache.get("user_preferences", Map.of("user_id", userId));
        System.out.println("\n⚙️  User feed preferences:");
        System.out.println("  Feed type: " + preferences.get("feed_type"));
        System.out.println("  Show likes: " + preferences.get("show_likes"));
        System.out.println("  Show comments: " + preferences.get("show_comments"));

        Thread.sleep(1000);
        System.out.println("\n✓ Feed generation demonstration complete\n");
    }
}

