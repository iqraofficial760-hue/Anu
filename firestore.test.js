const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_host_123";
const BOB_UID = "bob_guest_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const pastDate = (ms = 5000) => new Date(Date.now() - ms);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// 1. Unauthenticated denial
test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("rooms").where("status", "==", "live").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("rooms").doc("room_1").set({
      hostId: ALICE_UID,
      hostName: "Alice",
      title: "Midnight Beats",
      topic: "Welcome",
      category: "music",
      status: "live",
      coverTheme: "music",
      listenerCount: 1,
      giftScore: 0,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

// 2. User profiles: owner CRUD + cross-user isolation + shadow field denial
test("Users can create and update their own profile, but cannot access other users", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Selam DJ",
      bio: "Addis night vibes",
      coinBalance: 1000,
      roomsHosted: 0,
      giftsSent: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Selam Live",
      coinBalance: 950,
      giftsSent: 1,
      updatedAt: pastDate(1000),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(1000),
    })
  );

  // Reject cross-user read/write
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      coinBalance: 999999,
      updatedAt: pastDate(1000),
    })
  );
});

// 3. User bookmarks subcollection
test("Users can manage their own bookmarks and cannot read others", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("room_1").set({
      roomId: "room_1",
      roomTitle: "Addis Acoustic Night",
      hostName: "Selam",
      category: "music",
      createdAt: pastDate(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").get()
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("room_1").delete()
  );
});

// 4. Party rooms: create, filtered list, action-based updates, terminal lock
test("Party rooms enforce schema, filtered list queries, action-based updates, and terminal lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(15000);

  await assertSucceeds(
    aliceDb.collection("rooms").doc("room_1").set({
      hostId: ALICE_UID,
      hostName: "Selam",
      title: "Golden Mic Karaoke",
      topic: "Sing your favorite tracks",
      category: "karaoke",
      status: "live",
      coverTheme: "music",
      listenerCount: 1,
      giftScore: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject @DocumentId 'id' key inside payload
  await assertFails(
    aliceDb.collection("rooms").doc("room_bad").set({
      id: "room_bad",
      hostId: ALICE_UID,
      hostName: "Selam",
      title: "Bad Room",
      topic: "",
      category: "music",
      status: "live",
      coverTheme: "music",
      listenerCount: 1,
      giftScore: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Filtered list queries succeed
  await assertSucceeds(bobDb.collection("rooms").where("status", "==", "live").get());
  await assertSucceeds(aliceDb.collection("rooms").where("hostId", "==", ALICE_UID).get());

  // Unfiltered list query fails
  await assertFails(bobDb.collection("rooms").get());

  // Guest can update listenerCount & giftScore on a live room
  await assertSucceeds(
    bobDb.collection("rooms").doc("room_1").update({
      listenerCount: 2,
      giftScore: 50,
      updatedAt: pastDate(2000),
    })
  );

  // Guest CANNOT hijack room title or end the room
  await assertFails(
    bobDb.collection("rooms").doc("room_1").update({
      title: "Hijacked",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    bobDb.collection("rooms").doc("room_1").update({
      status: "ended",
      updatedAt: pastDate(1000),
    })
  );

  // Host can end the room
  await assertSucceeds(
    aliceDb.collection("rooms").doc("room_1").update({
      status: "ended",
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state lock: once ended, no further updates allowed
  await assertFails(
    aliceDb.collection("rooms").doc("room_1").update({
      title: "Reopened",
      updatedAt: pastDate(500),
    })
  );
});

// 5. Room messages and speaker seats subcollections
test("Live room messages and speaker seats enforce ownership and parent room liveness", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(15000);

  await assertSucceeds(
    aliceDb.collection("rooms").doc("room_2").set({
      hostId: ALICE_UID,
      hostName: "Selam",
      title: "Coffee & Chill Lounge",
      topic: "Late night stories",
      category: "chill",
      status: "live",
      coverTheme: "lounge",
      listenerCount: 1,
      giftScore: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob sends a chat message
  await assertSucceeds(
    bobDb.collection("rooms").doc("room_2").collection("messages").doc("msg_1").set({
      roomId: "room_2",
      senderId: BOB_UID,
      senderName: "Dawit",
      text: "Hello everyone!",
      messageType: "chat",
      createdAt: pastDate(2000),
    })
  );

  // Bob sends a gift message with optional fields
  await assertSucceeds(
    bobDb.collection("rooms").doc("room_2").collection("messages").doc("msg_2").set({
      roomId: "room_2",
      senderId: BOB_UID,
      senderName: "Dawit",
      text: "sent a Golden Crown!",
      messageType: "gift",
      giftName: "Golden Crown",
      giftCoins: 50,
      createdAt: pastDate(1500),
    })
  );

  // Scoped messages list succeeds; unfiltered fails
  await assertSucceeds(
    aliceDb.collection("rooms").doc("room_2").collection("messages").where("roomId", "==", "room_2").get()
  );
  await assertFails(
    aliceDb.collection("rooms").doc("room_2").collection("messages").get()
  );

  // Bob claims seat 1 and toggles mute
  await assertSucceeds(
    bobDb.collection("rooms").doc("room_2").collection("seats").doc("seat_1").set({
      roomId: "room_2",
      seatIndex: 1,
      occupantId: BOB_UID,
      occupantName: "Dawit",
      muted: false,
      updatedAt: pastDate(2000),
    })
  );

  await assertSucceeds(
    bobDb.collection("rooms").doc("room_2").collection("seats").doc("seat_1").update({
      muted: true,
      updatedAt: pastDate(1000),
    })
  );

  // Scoped seats list succeeds
  await assertSucceeds(
    aliceDb.collection("rooms").doc("room_2").collection("seats").where("roomId", "==", "room_2").get()
  );

  // Host (Alice) can remove Bob from a seat, or Bob can leave his own seat
  await assertSucceeds(
    aliceDb.collection("rooms").doc("room_2").collection("seats").doc("seat_1").delete()
  );
});

// 6. RoomRepository CRUD (/party_rooms/{roomId})
test("party_rooms enforces owner-scoped CRUD, bounded speakers list, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  // Create succeeds for owner
  await assertSucceeds(
    aliceDb.collection("party_rooms").doc("pr_1").set({
      ownerId: ALICE_UID,
      name: "Addis Acoustic Lounge",
      activeUsers: 12,
      speakers: ["Selam", "Dawit"],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject >8 speakers
  await assertFails(
    aliceDb.collection("party_rooms").doc("pr_overflow").set({
      ownerId: ALICE_UID,
      name: "Too Many Speakers",
      activeUsers: 20,
      speakers: ["s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9"],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Scoped list succeeds; unfiltered list fails
  await assertSucceeds(
    aliceDb.collection("party_rooms").where("ownerId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("party_rooms").get());

  // Owner partial update succeeds
  await assertSucceeds(
    aliceDb.collection("party_rooms").doc("pr_1").update({
      activeUsers: 18,
      speakers: ["Selam", "Dawit", "Hanna"],
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user read/update/delete denied
  await assertFails(bobDb.collection("party_rooms").doc("pr_1").get());
  await assertFails(
    bobDb.collection("party_rooms").doc("pr_1").update({
      activeUsers: 99,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(bobDb.collection("party_rooms").doc("pr_1").delete());

  // Owner delete succeeds
  await assertSucceeds(aliceDb.collection("party_rooms").doc("pr_1").delete());
});

// 7. ANU LIVE Moments, Conversations, Transactions, Notifications & Reports
test("Moments, Conversations, Wallet Transactions, Notifications, and Reports enforce strict security rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(8000);

  // Moments: create & scoped public list
  await assertSucceeds(
    aliceDb.collection("moments").doc("m_1").set({
      authorId: ALICE_UID,
      authorName: "Selam",
      caption: "Backstage before tonight's live set!",
      mediaType: "photo",
      coverTheme: "music",
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("moments").where("visibility", "==", "public").get());
  await assertFails(bobDb.collection("moments").get());

  // Bob likes Alice's moment
  await assertSucceeds(
    bobDb.collection("moments").doc("m_1").update({
      likesCount: 1,
      updatedAt: pastDate(1000),
    })
  );

  // Private conversation between Alice & Bob
  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_1").set({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: ["Selam", "Dawit"],
      lastMessage: "Hey, joining the party!",
      unreadCount: 1,
      peerOnline: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("conversations").where("participantIds", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("conversations").doc("conv_1").collection("messages").doc("dm_1").set({
      conversationId: "conv_1",
      participantIds: [ALICE_UID, BOB_UID],
      senderId: BOB_UID,
      senderName: "Dawit",
      text: "On my way!",
      createdAt: pastDate(1000),
    })
  );

  // Wallet transaction & Notification isolation
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      txType: "recharge",
      amount: 500,
      description: "Mock Recharge Pack (500 Coins)",
      createdAt: pastDate(1000),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());

  // Report creation
  await assertSucceeds(
    aliceDb.collection("reports").doc("rep_1").set({
      reporterId: ALICE_UID,
      targetId: "room_1",
      targetType: "room",
      reason: "Spam links in chat",
      status: "open",
      createdAt: pastDate(1000),
    })
  );
  await assertFails(bobDb.collection("reports").doc("rep_1").get());
});
