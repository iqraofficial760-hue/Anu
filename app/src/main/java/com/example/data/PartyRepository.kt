package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * Dual-mode repository:
 * - When signed in with Google (auth.currentUser != null), reads & writes live to Firebase Firestore.
 * - When in instant Guest mode (auth.currentUser == null), uses LocalGuestStore seamlessly with
 *   zero login wall, 1,000,000 FREE welcome coins, VIP/SVIP, 7-day tasks, and real-time local state.
 */
class PartyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun signedInUid(): String? = auth.currentUser?.uid?.takeIf { it.isNotBlank() }

    private fun handleStreamError(error: Throwable, uid: String, op: OperationType, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    // --- USER PROFILE, COINS, DIAMONDS, VIP/SVIP, NEWCOMER TASKS, SETTINGS ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = signedInUid()
        if (uid == null) {
            emitAll(LocalGuestStore.profile)
            return@flow
        }
        check(uid == userId) { "Refusing cross-user profile observation." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(defaultName: String): Result<UserProfile> {
        val uid = signedInUid() ?: return Result.success(LocalGuestStore.profile.value)
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val profile = existing.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(id = uid, userId = uid, displayName = sanitizeName(defaultName))
                Result.success(profile)
            } else {
                val safeName = sanitizeName(defaultName)
                val payload = mapOf(
                    "userId" to uid,
                    "numericId" to LocalGuestStore.profile.value.numericId.coerceAtLeast(1L),
                    "displayName" to safeName,
                    "bio" to "Streaming & partying on ANU LIVE!",
                    "role" to "user",
                    "coinBalance" to NewcomerTaskCatalog.REGISTRATION_WELCOME_COINS,
                    "diamondBalance" to 500L,
                    "avatarId" to "royal_lion",
                    "customAvatarUri" to "",
                    "vipTierId" to "",
                    "vipDaysRemaining" to 0,
                    "lastVipClaimDayKey" to "",
                    "claimedNewcomerDays" to emptyList<Int>(),
                    "claimedDailyTaskIds" to emptyList<String>(),
                    "lastDailyTaskDayKey" to "",
                    "withdrawalMethod" to "Telebirr",
                    "withdrawalAccount" to "",
                    "roomsHosted" to 0L,
                    "giftsSent" to 0L,
                    "giftsReceived" to 0L,
                    "followersCount" to 0L,
                    "followingCount" to 0L,
                    "level" to 1,
                    "hostLevel" to 1,
                    "xp" to 150L,
                    "online" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                createNotificationInternal(
                    uid = uid,
                    type = "system",
                    title = "1,000,000 FREE Coins Credited!",
                    body = "Welcome to ANU LIVE! Explore VIP/SVIP & 7-Day Newcomer Tasks."
                )
                Result.success(UserProfile(id = uid, userId = uid, displayName = safeName))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserSettings(
        displayName: String,
        bio: String,
        avatarId: String,
        customAvatarUri: String,
        withdrawalMethod: String,
        withdrawalAccount: String
    ): Result<Unit> {
        val trimmedName = displayName.trim().take(40)
        if (trimmedName.isEmpty()) return Result.failure(IllegalArgumentException("Username cannot be empty."))
        val trimmedBio = bio.trim().take(160)
        val safeAvatar = AvatarCatalog.byId(avatarId).id
        val safeCustomUri = customAvatarUri.trim().take(500)
        val safeMethod = withdrawalMethod.trim().take(30).ifEmpty { "Telebirr" }
        val safeAccount = withdrawalAccount.trim().take(60)

        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.updateProfile {
                it.copy(
                    displayName = trimmedName,
                    bio = trimmedBio,
                    avatarId = safeAvatar,
                    customAvatarUri = safeCustomUri,
                    withdrawalMethod = safeMethod,
                    withdrawalAccount = safeAccount
                )
            }
            return Result.success(Unit)
        }

        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to trimmedName,
                    "bio" to trimmedBio,
                    "avatarId" to safeAvatar,
                    "customAvatarUri" to safeCustomUri,
                    "withdrawalMethod" to safeMethod,
                    "withdrawalAccount" to safeAccount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCustomAvatarUri(customAvatarUri: String): Result<Unit> {
        val safeUri = customAvatarUri.trim().take(500)
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.updateProfile { it.copy(customAvatarUri = safeUri) }
            return Result.success(Unit)
        }
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf("customAvatarUri" to safeUri, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun claimDailyTask(task: DailyTaskItem, profile: UserProfile): Result<Long> {
        val today = LocalGuestStore.todayKey()
        val existingIds = if (profile.lastDailyTaskDayKey == today) profile.claimedDailyTaskIds else emptyList()
        if (task.id in existingIds) {
            return Result.failure(IllegalStateException("Already claimed today!"))
        }
        val nextIds = (existingIds + task.id).distinct()
        val nextCoins = profile.coinBalance + task.rewardCoins
        val nextXp = profile.xp + task.xpReward
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.updateProfile {
                it.copy(
                    coinBalance = nextCoins,
                    claimedDailyTaskIds = nextIds,
                    lastDailyTaskDayKey = today,
                    xp = nextXp,
                    level = LevelCalculator.levelForXp(nextXp)
                )
            }
            LocalGuestStore.addTransaction("bonus", task.rewardCoins, "Task Center: ${task.title} (+${formatCompactCoins(task.rewardCoins)})")
            return Result.success(task.rewardCoins)
        }
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "coinBalance" to nextCoins,
                    "claimedDailyTaskIds" to nextIds,
                    "lastDailyTaskDayKey" to today,
                    "xp" to nextXp,
                    "level" to LevelCalculator.levelForXp(nextXp),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(uid).collection("transactions").document().set(
                mapOf(
                    "userId" to uid,
                    "txType" to "bonus",
                    "amount" to task.rewardCoins,
                    "description" to "Task Center: ${task.title}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(task.rewardCoins)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun purchaseVipTier(tier: VipTier, currentCoins: Long): Result<Unit> {
        if (currentCoins < tier.priceCoins) {
            return Result.failure(
                IllegalArgumentException("Need ${formatCompactCoins(tier.priceCoins)} coins for ${tier.name}.")
            )
        }
        val nextCoins = (currentCoins - tier.priceCoins).coerceAtLeast(0L)
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.updateProfile {
                it.copy(
                    coinBalance = nextCoins,
                    vipTierId = tier.id,
                    vipDaysRemaining = tier.durationDays,
                    lastVipClaimDayKey = "",
                    xp = it.xp + 2_000L,
                    level = LevelCalculator.levelForXp(it.xp + 2_000L)
                )
            }
            LocalGuestStore.addTransaction(
                txType = "gift_sent",
                amount = -tier.priceCoins,
                description = "Activated ${tier.name} (${formatCompactCoins(tier.dailyCashbackCoins)}/day x 7d)"
            )
            LocalGuestStore.addNotification(
                type = "system",
                title = "${tier.name} Royal Status Unlocked!",
                body = "Claim ${formatCompactCoins(tier.dailyCashbackCoins)} coins daily for 7 days in VIP & SVIP."
            )
            return Result.success(Unit)
        }

        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "coinBalance" to nextCoins,
                    "vipTierId" to tier.id,
                    "vipDaysRemaining" to tier.durationDays,
                    "lastVipClaimDayKey" to "",
                    "xp" to FieldValue.increment(2_000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(uid).collection("transactions").document().set(
                mapOf(
                    "userId" to uid,
                    "txType" to "gift_sent",
                    "amount" to -tier.priceCoins,
                    "description" to "Activated ${tier.name} (${formatCompactCoins(tier.dailyCashbackCoins)}/day x 7d)",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            createNotificationInternal(
                uid = uid,
                type = "system",
                title = "${tier.name} Royal Status Unlocked!",
                body = "Claim ${formatCompactCoins(tier.dailyCashbackCoins)} coins daily for 7 days."
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun claimVipDailyCashback(profile: UserProfile): Result<Long> {
        val tier = VipCatalog.byId(profile.vipTierId)
            ?: return Result.failure(IllegalStateException("Activate a VIP or SVIP tier first."))
        if (profile.vipDaysRemaining <= 0) {
            return Result.failure(IllegalStateException("All 7 days of ${tier.name} cashback have been claimed."))
        }
        val today = LocalGuestStore.todayKey()
        if (profile.lastVipClaimDayKey == today) {
            return Result.failure(IllegalStateException("You already claimed today's ${tier.name} cashback!"))
        }

        val payout = tier.dailyCashbackCoins
        val nextCoins = profile.coinBalance + payout
        val nextDays = (profile.vipDaysRemaining - 1).coerceAtLeast(0)

        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.updateProfile {
                it.copy(
                    coinBalance = nextCoins,
                    vipDaysRemaining = nextDays,
                    lastVipClaimDayKey = today
                )
            }
            LocalGuestStore.addTransaction(
                txType = "bonus",
                amount = payout,
                description = "${tier.name} Daily Cashback (+${formatCompactCoins(payout)} coins)"
            )
            return Result.success(payout)
        }

        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "coinBalance" to nextCoins,
                    "vipDaysRemaining" to nextDays,
                    "lastVipClaimDayKey" to today,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(uid).collection("transactions").document().set(
                mapOf(
                    "userId" to uid,
                    "txType" to "bonus",
                    "amount" to payout,
                    "description" to "${tier.name} Daily Cashback (+${formatCompactCoins(payout)} coins)",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(payout)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun claimNewcomerTaskDay(dayTask: NewcomerTaskDay, profile: UserProfile): Result<Long> {
        if (dayTask.day in profile.claimedNewcomerDays) {
            return Result.failure(IllegalStateException("Day ${dayTask.day} reward already claimed!"))
        }
        val updatedDays = (profile.claimedNewcomerDays + dayTask.day).distinct().sorted()
        val nextCoins = profile.coinBalance + dayTask.rewardCoins
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.updateProfile {
                it.copy(
                    coinBalance = nextCoins,
                    claimedNewcomerDays = updatedDays,
                    xp = it.xp + 250L,
                    level = LevelCalculator.levelForXp(it.xp + 250L)
                )
            }
            LocalGuestStore.addTransaction(
                txType = "bonus",
                amount = dayTask.rewardCoins,
                description = "Newcomer Task Day ${dayTask.day}: +${formatCompactCoins(dayTask.rewardCoins)} coins"
            )
            return Result.success(dayTask.rewardCoins)
        }

        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "coinBalance" to nextCoins,
                    "claimedNewcomerDays" to updatedDays,
                    "xp" to FieldValue.increment(250L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(uid).collection("transactions").document().set(
                mapOf(
                    "userId" to uid,
                    "txType" to "bonus",
                    "amount" to dayTask.rewardCoins,
                    "description" to "Newcomer Task Day ${dayTask.day}: +${formatCompactCoins(dayTask.rewardCoins)} coins",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(dayTask.rewardCoins)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun exchangeDiamondsToCoins(diamondsToSpend: Long, profile: UserProfile): Result<Long> {
        if (diamondsToSpend <= 0L) return Result.failure(IllegalArgumentException("Enter a valid diamond amount."))
        if (profile.diamondBalance < diamondsToSpend) {
            return Result.failure(IllegalArgumentException("Not enough diamonds (you have ${profile.diamondBalance})."))
        }
        val coinsGained = diamondsToSpend * 100L // 1 Diamond = 100 Coins
        val nextDiamonds = profile.diamondBalance - diamondsToSpend
        val nextCoins = profile.coinBalance + coinsGained
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.updateProfile {
                it.copy(diamondBalance = nextDiamonds, coinBalance = nextCoins)
            }
            LocalGuestStore.addTransaction(
                txType = "bonus",
                amount = coinsGained,
                description = "Exchanged $diamondsToSpend Diamonds -> ${formatCompactCoins(coinsGained)} Coins"
            )
            return Result.success(coinsGained)
        }

        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "diamondBalance" to nextDiamonds,
                    "coinBalance" to nextCoins,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(uid).collection("transactions").document().set(
                mapOf(
                    "userId" to uid,
                    "txType" to "bonus",
                    "amount" to coinsGained,
                    "description" to "Exchanged $diamondsToSpend Diamonds -> ${formatCompactCoins(coinsGained)} Coins",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(coinsGained)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * TODO: Connect a real payout provider (Telebirr / CBE Birr / Stripe Connect) server-side
     * before enabling live monetary withdrawals in production.
     */
    suspend fun requestDiamondWithdrawal(diamondsAmount: Long, method: String, account: String, profile: UserProfile): Result<Unit> {
        if (diamondsAmount < 100L) return Result.failure(IllegalArgumentException("Minimum withdrawal is 100 Diamonds."))
        if (profile.diamondBalance < diamondsAmount) return Result.failure(IllegalArgumentException("Insufficient diamond balance."))
        if (account.isBlank()) return Result.failure(IllegalArgumentException("Please enter your $method account/phone."))

        val nextDiamonds = profile.diamondBalance - diamondsAmount
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.updateProfile {
                it.copy(diamondBalance = nextDiamonds, withdrawalMethod = method, withdrawalAccount = account.trim())
            }
            LocalGuestStore.addTransaction(
                txType = "gift_sent",
                amount = -diamondsAmount,
                description = "Withdrawal Request ($method: ${account.trim().take(24)}) [Dev Mock]"
            )
            LocalGuestStore.addNotification(
                type = "system",
                title = "Withdrawal Queued ($diamondsAmount Diamonds)",
                body = "Method: $method (${account.trim()}). Dev mock payout queued."
            )
            return Result.success(Unit)
        }

        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "diamondBalance" to nextDiamonds,
                    "withdrawalMethod" to method,
                    "withdrawalAccount" to account.trim().take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(uid).collection("transactions").document().set(
                mapOf(
                    "userId" to uid,
                    "txType" to "gift_sent",
                    "amount" to -diamondsAmount,
                    "description" to "Withdrawal Request ($method: ${account.trim().take(24)}) [Dev Mock]",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- FOLLOWING & BOOKMARKS ---

    fun observeFollowing(userId: String): Flow<List<FollowedHost>> = flow {
        val uid = signedInUid()
        if (uid == null) {
            emitAll(LocalGuestStore.following)
            return@flow
        }
        check(uid == userId) { "Refusing cross-user following observation." }
        val path = "users/$uid/following"
        emitAll(
            db.collection("users").document(uid).collection("following")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FollowedHost::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun toggleFollowHost(
        targetUserId: String,
        targetName: String,
        currentlyFollowing: Boolean
    ): Result<Unit> {
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.toggleFollow(targetUserId, targetName, currentlyFollowing)
            return Result.success(Unit)
        }
        val path = "users/$uid/following/$targetUserId"
        return try {
            val docRef = db.collection("users").document(uid)
                .collection("following").document(targetUserId)
            if (currentlyFollowing) {
                docRef.delete().await()
            } else {
                docRef.set(
                    mapOf(
                        "targetUserId" to targetUserId,
                        "targetName" to sanitizeName(targetName),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                createNotificationInternal(
                    uid = uid,
                    type = "follower",
                    title = "Following ${sanitizeName(targetName)}",
                    body = "You'll be notified when ${sanitizeName(targetName)} goes live."
                )
            }
            try {
                db.collection("users").document(uid).update(
                    mapOf(
                        "followingCount" to FieldValue.increment(if (currentlyFollowing) -1L else 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeBookmarks(userId: String): Flow<List<RoomBookmark>> = flow {
        val uid = signedInUid()
        if (uid == null) {
            emitAll(LocalGuestStore.bookmarks)
            return@flow
        }
        check(uid == userId) { "Refusing cross-user bookmarks observation." }
        val path = "users/$uid/bookmarks"
        emitAll(
            db.collection("users").document(uid).collection("bookmarks")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RoomBookmark::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun toggleBookmark(room: PartyRoom, currentlyBookmarked: Boolean): Result<Unit> {
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.toggleBookmark(room, currentlyBookmarked)
            return Result.success(Unit)
        }
        val path = "users/$uid/bookmarks/${room.id}"
        return try {
            require(room.id.isNotBlank()) { "Room ID is required." }
            val docRef = db.collection("users").document(uid)
                .collection("bookmarks").document(room.id)
            if (currentlyBookmarked) {
                docRef.delete().await()
            } else {
                val payload = mapOf(
                    "roomId" to room.id,
                    "roomTitle" to room.title.trim().take(60).ifEmpty { "ANU Room" },
                    "hostName" to room.hostName.trim().take(40).ifEmpty { "Host" },
                    "category" to PartyCategory.fromKey(room.category).key.takeIf { it != "all" }.orEmpty().ifEmpty { "popular" },
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- WALLET & TRANSACTIONS ---

    fun observeTransactions(userId: String): Flow<List<WalletTransaction>> = flow {
        val uid = signedInUid()
        if (uid == null) {
            emitAll(LocalGuestStore.transactions)
            return@flow
        }
        check(uid == userId) { "Refusing cross-user transaction observation." }
        val path = "users/$uid/transactions"
        emitAll(
            db.collection("users").document(uid).collection("transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WalletTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun mockRechargeCoins(pack: RechargePack, currentCoins: Long): Result<Unit> {
        val totalAdded = pack.coins + pack.bonusCoins
        val nextBalance = (currentCoins + totalAdded).coerceAtMost(999_000_000L)
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.updateProfile { it.copy(coinBalance = nextBalance) }
            LocalGuestStore.addTransaction("recharge", totalAdded, "Mock Recharge: ${pack.label} (${pack.priceDisplay})")
            LocalGuestStore.addNotification("system", "Coins Recharged (+${formatCompactCoins(totalAdded)})", "Added via Dev Mock Checkout.")
            return Result.success(Unit)
        }
        val path = "users/$uid/transactions"
        return try {
            db.collection("users").document(uid).update(
                mapOf("coinBalance" to nextBalance, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            db.collection("users").document(uid).collection("transactions").document().set(
                mapOf(
                    "userId" to uid,
                    "txType" to "recharge",
                    "amount" to totalAdded,
                    "description" to "Mock Recharge: ${pack.label} (${pack.priceDisplay})",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- NOTIFICATIONS ---

    fun observeNotifications(userId: String): Flow<List<AppNotification>> = flow {
        val uid = signedInUid()
        if (uid == null) {
            emitAll(LocalGuestStore.notifications)
            return@flow
        }
        check(uid == userId) { "Refusing cross-user notifications observation." }
        val path = "users/$uid/notifications"
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AppNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> {
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.markNotificationRead(notifId)
            return Result.success(Unit)
        }
        val path = "users/$uid/notifications/$notifId"
        return try {
            db.collection("users").document(uid).collection("notifications")
                .document(notifId)
                .update(mapOf("read" to true))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private suspend fun createNotificationInternal(uid: String, type: String, title: String, body: String) {
        try {
            db.collection("users").document(uid).collection("notifications").document().set(
                mapOf(
                    "userId" to uid,
                    "notifType" to type,
                    "title" to title.take(80),
                    "body" to body.take(240),
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {}
    }

    // --- LIVE & PARTY ROOMS ---

    fun observeLiveRooms(): Flow<List<PartyRoom>> = flow {
        val uid = signedInUid()
        if (uid == null) {
            emitAll(LocalGuestStore.rooms.map { list -> list.filter { it.status == "live" } })
            return@flow
        }
        val path = "rooms"
        emitAll(
            db.collection("rooms")
                .whereEqualTo("status", "live")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PartyRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<PartyRoom> { it.listenerCount }
                            .thenByDescending { it.giftScore }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeHostedRooms(userId: String): Flow<List<PartyRoom>> = flow {
        val uid = signedInUid()
        if (uid == null) {
            emitAll(LocalGuestStore.rooms.map { list -> list.filter { it.hostId == userId } })
            return@flow
        }
        check(uid == userId) { "Refusing cross-user hosted rooms observation." }
        val path = "rooms"
        emitAll(
            db.collection("rooms")
                .whereEqualTo("hostId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PartyRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeRoom(roomId: String): Flow<PartyRoom?> = flow {
        val uid = signedInUid()
        if (uid == null) {
            emitAll(LocalGuestStore.rooms.map { list -> list.firstOrNull { it.id == roomId } })
            return@flow
        }
        val path = "rooms/$roomId"
        emitAll(
            db.collection("rooms").document(roomId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            PartyRoom::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun createPartyRoom(
        title: String,
        topic: String,
        category: PartyCategory,
        hostName: String,
        roomMode: String = "party",
        seatCapacity: Int = 8
    ): Result<String> {
        val cleanTitle = title.trim().take(60)
        if (cleanTitle.length < 2) return Result.failure(IllegalArgumentException("Room title must be at least 2 characters."))
        val uid = signedInUid()
        if (uid == null) {
            val id = LocalGuestStore.createRoom(cleanTitle, topic, category, sanitizeName(hostName), roomMode, seatCapacity)
            return Result.success(id)
        }
        var path = "rooms"
        return try {
            val cleanTopic = topic.trim().take(140)
            val safeHostName = sanitizeName(hostName)
            val safeCategory = if (category == PartyCategory.ALL) PartyCategory.POPULAR.key else category.key
            val safeMode = if (roomMode == "live") "live" else "party"
            val safeCapacity = if (seatCapacity in listOf(4, 6, 8)) seatCapacity else 8
            val shortCode = "ANU-${Random.nextInt(1000, 9999)}"
            val coverTheme = when (safeCategory) {
                "music", "karaoke" -> "music"
                "popular" -> "hero"
                else -> "lounge"
            }

            val roomRef = db.collection("rooms").document()
            path = roomRef.path

            val roomPayload = mapOf(
                "hostId" to uid,
                "hostName" to safeHostName,
                "title" to cleanTitle,
                "topic" to cleanTopic,
                "category" to safeCategory,
                "roomMode" to safeMode,
                "seatCapacity" to safeCapacity,
                "shortCode" to shortCode,
                "status" to "live",
                "coverTheme" to coverTheme,
                "listenerCount" to 1L,
                "giftScore" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            roomRef.set(roomPayload).await()

            val hostSeatPayload = mapOf(
                "roomId" to roomRef.id,
                "seatIndex" to 0,
                "occupantId" to uid,
                "occupantName" to safeHostName,
                "muted" to false,
                "cameraOn" to (safeMode == "live"),
                "seatRole" to "host",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            roomRef.collection("seats").document("seat_0").set(hostSeatPayload).await()

            val welcomePayload = mapOf(
                "roomId" to roomRef.id,
                "senderId" to uid,
                "senderName" to safeHostName,
                "text" to (cleanTopic.ifEmpty { "Welcome to $cleanTitle ($shortCode)! Live audio is ON." }),
                "messageType" to "system",
                "createdAt" to FieldValue.serverTimestamp()
            )
            roomRef.collection("messages").document().set(welcomePayload).await()

            try {
                db.collection("users").document(uid).update(
                    mapOf(
                        "roomsHosted" to FieldValue.increment(1L),
                        "xp" to FieldValue.increment(100L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (_: Exception) {}

            Result.success(roomRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun endPartyRoom(roomId: String): Result<Unit> {
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.endRoom(roomId)
            return Result.success(Unit)
        }
        val path = "rooms/$roomId"
        return try {
            db.collection("rooms").document(roomId).update(
                mapOf("status" to "ended", "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adjustListenerCount(roomId: String, delta: Long): Result<Unit> {
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.adjustListeners(roomId, delta)
            return Result.success(Unit)
        }
        val path = "rooms/$roomId"
        return try {
            if (roomId.isBlank()) return Result.success(Unit)
            val docRef = db.collection("rooms").document(roomId)
            val snap = docRef.get().await()
            if (!snap.exists() || snap.getString("status") != "live") return Result.success(Unit)
            val current = snap.getLong("listenerCount") ?: 1L
            val next = (current + delta).coerceAtLeast(0L)
            docRef.update(mapOf("listenerCount" to next, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- ROOM MESSAGES & GIFTS ---

    fun observeRoomMessages(roomId: String): Flow<List<RoomMessage>> = flow {
        val uid = signedInUid()
        if (uid == null) {
            emitAll(LocalGuestStore.roomMessages.map { it[roomId] ?: emptyList() })
            return@flow
        }
        val path = "rooms/$roomId/messages"
        emitAll(
            db.collection("rooms").document(roomId).collection("messages")
                .whereEqualTo("roomId", roomId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RoomMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun sendMessage(roomId: String, senderName: String, text: String): Result<Unit> {
        val cleanText = text.trim().take(280)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty."))
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.sendRoomMessage(roomId, sanitizeName(senderName), cleanText)
            return Result.success(Unit)
        }
        val path = "rooms/$roomId/messages"
        return try {
            val payload = mapOf(
                "roomId" to roomId,
                "senderId" to uid,
                "senderName" to sanitizeName(senderName),
                "text" to cleanText,
                "messageType" to "chat",
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("rooms").document(roomId).collection("messages").document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendVirtualGift(
        roomId: String,
        senderName: String,
        receiverName: String,
        gift: VirtualGift,
        currentCoinBalance: Long,
        currentXp: Long
    ): Result<GiftSendOutcome> {
        if (currentCoinBalance < gift.coins) {
            return Result.failure(IllegalArgumentException("Not enough coins for ${gift.name} (${formatCompactCoins(gift.coins)} needed)."))
        }
        val oldLevel = LevelCalculator.levelForXp(currentXp)
        val newXp = currentXp + gift.xpReward
        val newLevel = LevelCalculator.levelForXp(newXp)
        val leveledUp = newLevel > oldLevel

        // Lucky Gift 50x / 100x winning jackpot roll!
        val luckyMultiplier = if (gift.isLucky) {
            val roll = Random.nextInt(100)
            when {
                roll < 15 -> 100 // 15% chance for 100x JACKPOT!
                roll < 42 -> 50  // 27% chance for 50x MEGA WIN!
                roll < 72 -> 10  // 30% chance for 10x WIN!
                else -> 2        // 28% chance for 2x return
            }
        } else 0
        val luckyWinCoins = if (luckyMultiplier > 0) gift.coins * luckyMultiplier.toLong() else 0L
        val nextBalance = (currentCoinBalance - gift.coins + luckyWinCoins).coerceAtLeast(0L)

        val luckySuffix = if (luckyMultiplier > 0) {
            " • LUCKY ${luckyMultiplier}x WIN (+${formatCompactCoins(luckyWinCoins)} coins)!"
        } else ""

        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.updateProfile {
                it.copy(
                    coinBalance = nextBalance,
                    diamondBalance = it.diamondBalance + (gift.coins / 200L).coerceAtLeast(1L),
                    giftsSent = it.giftsSent + 1L,
                    xp = newXp,
                    level = newLevel
                )
            }
            LocalGuestStore.addTransaction("gift_sent", -gift.coins, "Sent ${gift.name} to ${sanitizeName(receiverName)}", sanitizeName(receiverName))
            if (luckyWinCoins > 0L) {
                LocalGuestStore.addTransaction("bonus", luckyWinCoins, "LUCKY ${luckyMultiplier}x WIN from ${gift.name}!")
            }
            LocalGuestStore.addRoomGiftScore(roomId, gift.coins)
            LocalGuestStore.sendRoomMessage(
                roomId = roomId,
                senderName = sanitizeName(senderName),
                text = "sent ${gift.name} to ${sanitizeName(receiverName)} (+${formatCompactCoins(gift.coins)})$luckySuffix",
                type = "gift",
                giftName = gift.name,
                giftCoins = gift.coins
            )
            return Result.success(GiftSendOutcome(leveledUp, luckyMultiplier, luckyWinCoins))
        }

        val path = "rooms/$roomId/messages"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "coinBalance" to nextBalance,
                    "giftsSent" to FieldValue.increment(1L),
                    "xp" to newXp,
                    "level" to newLevel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(uid).collection("transactions").document().set(
                mapOf(
                    "userId" to uid,
                    "txType" to "gift_sent",
                    "amount" to -gift.coins,
                    "description" to "Sent ${gift.name} to ${sanitizeName(receiverName)}",
                    "counterpartyName" to sanitizeName(receiverName),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (luckyWinCoins > 0L) {
                db.collection("users").document(uid).collection("transactions").document().set(
                    mapOf(
                        "userId" to uid,
                        "txType" to "bonus",
                        "amount" to luckyWinCoins,
                        "description" to "LUCKY ${luckyMultiplier}x WIN from ${gift.name}!",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            db.collection("rooms").document(roomId).update(
                mapOf("giftScore" to FieldValue.increment(gift.coins), "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            db.collection("rooms").document(roomId).collection("messages").document().set(
                mapOf(
                    "roomId" to roomId,
                    "senderId" to uid,
                    "senderName" to sanitizeName(senderName),
                    "text" to "sent ${gift.name} to ${sanitizeName(receiverName)} (+${formatCompactCoins(gift.coins)})$luckySuffix",
                    "messageType" to "gift",
                    "giftName" to gift.name,
                    "giftCoins" to gift.coins,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(GiftSendOutcome(leveledUp, luckyMultiplier, luckyWinCoins))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- ROOM SPEAKER SEATS ---

    fun observeRoomSeats(roomId: String): Flow<List<RoomSeat>> = flow {
        val uid = signedInUid()
        if (uid == null) {
            emitAll(LocalGuestStore.roomSeats.map { it[roomId] ?: emptyList() })
            return@flow
        }
        val path = "rooms/$roomId/seats"
        emitAll(
            db.collection("rooms").document(roomId).collection("seats")
                .whereEqualTo("roomId", roomId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RoomSeat::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.seatIndex }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun claimSeat(roomId: String, seatIndex: Int, occupantName: String, cameraOn: Boolean = false): Result<Unit> {
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.claimSeat(roomId, seatIndex, sanitizeName(occupantName), cameraOn)
            return Result.success(Unit)
        }
        val seatId = "seat_$seatIndex"
        val path = "rooms/$roomId/seats/$seatId"
        return try {
            val payload = mapOf(
                "roomId" to roomId,
                "seatIndex" to seatIndex,
                "occupantId" to uid,
                "occupantName" to sanitizeName(occupantName),
                "muted" to false,
                "cameraOn" to cameraOn,
                "seatRole" to if (seatIndex == 0) "host" else "speaker",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("rooms").document(roomId).collection("seats").document(seatId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleSeatMute(roomId: String, seatIndex: Int, muted: Boolean): Result<Unit> {
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.toggleSeatMute(roomId, seatIndex, muted)
            return Result.success(Unit)
        }
        val seatId = "seat_$seatIndex"
        val path = "rooms/$roomId/seats/$seatId"
        return try {
            db.collection("rooms").document(roomId).collection("seats").document(seatId)
                .update(mapOf("muted" to muted, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleSeatCamera(roomId: String, seatIndex: Int, cameraOn: Boolean): Result<Unit> {
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.toggleSeatCamera(roomId, seatIndex, cameraOn)
            return Result.success(Unit)
        }
        val seatId = "seat_$seatIndex"
        val path = "rooms/$roomId/seats/$seatId"
        return try {
            db.collection("rooms").document(roomId).collection("seats").document(seatId)
                .update(mapOf("cameraOn" to cameraOn, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveSeat(roomId: String, seatIndex: Int): Result<Unit> {
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.leaveSeat(roomId, seatIndex)
            return Result.success(Unit)
        }
        val seatId = "seat_$seatIndex"
        val path = "rooms/$roomId/seats/$seatId"
        return try {
            db.collection("rooms").document(roomId).collection("seats").document(seatId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- MOMENTS FEED ---

    fun observeMoments(): Flow<List<MomentPost>> = flow {
        val uid = signedInUid()
        if (uid == null) {
            emitAll(LocalGuestStore.moments)
            return@flow
        }
        val path = "moments"
        emitAll(
            db.collection("moments")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MomentPost::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createMoment(authorName: String, caption: String, mediaType: String, coverTheme: String): Result<Unit> {
        val cleanCaption = caption.trim().take(300)
        if (cleanCaption.isEmpty()) return Result.failure(IllegalArgumentException("Caption cannot be empty."))
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.createMoment(sanitizeName(authorName), cleanCaption, mediaType, coverTheme)
            return Result.success(Unit)
        }
        val path = "moments"
        return try {
            db.collection("moments").document().set(
                mapOf(
                    "authorId" to uid,
                    "authorName" to sanitizeName(authorName),
                    "caption" to cleanCaption,
                    "mediaType" to if (mediaType == "video") "video" else "photo",
                    "coverTheme" to if (coverTheme in listOf("music", "lounge", "hero")) coverTheme else "hero",
                    "visibility" to "public",
                    "likesCount" to 0L,
                    "commentsCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun likeMoment(momentId: String): Result<Unit> {
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.likeMoment(momentId)
            return Result.success(Unit)
        }
        val path = "moments/$momentId"
        return try {
            db.collection("moments").document(momentId).update(
                mapOf("likesCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeMomentComments(momentId: String): Flow<List<MomentComment>> = flow {
        val uid = signedInUid()
        if (uid == null) {
            emitAll(LocalGuestStore.momentComments.map { it[momentId] ?: emptyList() })
            return@flow
        }
        val path = "moments/$momentId/comments"
        emitAll(
            db.collection("moments").document(momentId).collection("comments")
                .whereEqualTo("momentId", momentId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MomentComment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addMomentComment(momentId: String, authorName: String, text: String): Result<Unit> {
        val clean = text.trim().take(240)
        if (clean.isEmpty()) return Result.failure(IllegalArgumentException("Comment cannot be empty."))
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.addMomentComment(momentId, sanitizeName(authorName), clean)
            return Result.success(Unit)
        }
        val path = "moments/$momentId/comments"
        return try {
            db.collection("moments").document(momentId).collection("comments").document().set(
                mapOf(
                    "momentId" to momentId,
                    "authorId" to uid,
                    "authorName" to sanitizeName(authorName),
                    "text" to clean,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("moments").document(momentId).update(
                mapOf("commentsCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- PRIVATE 1-TO-1 MESSAGING ---

    fun observeConversations(userId: String): Flow<List<Conversation>> = flow {
        val uid = signedInUid()
        if (uid == null) {
            emitAll(LocalGuestStore.conversations)
            return@flow
        }
        check(uid == userId) { "Refusing cross-user conversation observation." }
        val path = "conversations"
        emitAll(
            db.collection("conversations")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Conversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun startConversation(myName: String, peerName: String, initialMessage: String): Result<String> {
        val cleanPeer = sanitizeName(peerName)
        val cleanMsg = initialMessage.trim().take(280).ifEmpty { "Hey!" }
        val uid = signedInUid()
        if (uid == null) {
            return Result.success(LocalGuestStore.startConversation(sanitizeName(myName), cleanPeer, cleanMsg))
        }
        val path = "conversations"
        return try {
            val convRef = db.collection("conversations").document()
            val peerPseudoId = "peer_${cleanPeer.lowercase().replace(Regex("[^a-z0-9]"), "").take(12).ifEmpty { "user" }}"
            val participants = listOf(uid, peerPseudoId)
            convRef.set(
                mapOf(
                    "participantIds" to participants,
                    "participantNames" to listOf(sanitizeName(myName), cleanPeer),
                    "lastMessage" to cleanMsg,
                    "unreadCount" to 0L,
                    "peerOnline" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            convRef.collection("messages").document().set(
                mapOf(
                    "conversationId" to convRef.id,
                    "participantIds" to participants,
                    "senderId" to uid,
                    "senderName" to sanitizeName(myName),
                    "text" to cleanMsg,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(convRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeDirectMessages(conversationId: String): Flow<List<DirectMessage>> = flow {
        val uid = signedInUid()
        if (uid == null) {
            emitAll(LocalGuestStore.directMessages.map { it[conversationId] ?: emptyList() })
            return@flow
        }
        val path = "conversations/$conversationId/messages"
        emitAll(
            db.collection("conversations").document(conversationId).collection("messages")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DirectMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun sendDirectMessage(conversationId: String, participantIds: List<String>, senderName: String, text: String): Result<Unit> {
        val cleanText = text.trim().take(500)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty."))
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.sendDirectMessage(conversationId, participantIds, sanitizeName(senderName), cleanText)
            return Result.success(Unit)
        }
        val path = "conversations/$conversationId/messages"
        return try {
            val convRef = db.collection("conversations").document(conversationId)
            convRef.collection("messages").document().set(
                mapOf(
                    "conversationId" to conversationId,
                    "participantIds" to participantIds,
                    "senderId" to uid,
                    "senderName" to sanitizeName(senderName),
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            convRef.update(mapOf("lastMessage" to cleanText.take(280), "unreadCount" to 0L, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- MODERATION REPORTS ---

    fun observeMyReports(userId: String): Flow<List<ModerationReport>> = flow {
        val uid = signedInUid()
        if (uid == null) {
            emitAll(LocalGuestStore.reports)
            return@flow
        }
        check(uid == userId) { "Refusing cross-user report observation." }
        val path = "reports"
        emitAll(
            db.collection("reports")
                .whereEqualTo("reporterId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ModerationReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun submitReport(targetId: String, targetType: String, reason: String): Result<Unit> {
        val cleanReason = reason.trim().take(240)
        if (cleanReason.length < 2) return Result.failure(IllegalArgumentException("Please enter a short reason."))
        val uid = signedInUid()
        if (uid == null) {
            LocalGuestStore.submitReport(targetId, targetType, cleanReason)
            return Result.success(Unit)
        }
        val path = "reports"
        return try {
            val safeType = if (targetType in listOf("user", "room", "moment")) targetType else "room"
            db.collection("reports").document().set(
                mapOf(
                    "reporterId" to uid,
                    "targetId" to targetId.take(128),
                    "targetType" to safeType,
                    "reason" to cleanReason,
                    "status" to "open",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private fun sanitizeName(raw: String): String =
        raw.trim().take(40).ifEmpty { "Anu Royal Guest" }
}
