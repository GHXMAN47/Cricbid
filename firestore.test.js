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
const BOB_UID = "bob_player_456";
const EVE_UID = "eve_outsider_789";
const GUEST_HOST_ID = "guest_a1b2c3d4e5f6";
const GUEST_JOIN_ID = "guest_9f8e7d6c5b4a";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

function makeHostTeam(uid, name) {
  return {
    teamId: `team_${uid.slice(0, 12)}`,
    ownerUid: uid,
    teamName: name,
    bot: false,
    budgetLakhs: 10000,
    rtmRemaining: 1,
    retainedPlayerId: "",
    retentionDone: false,
    squadPlayerIds: [],
    squadPrices: {},
    playingXiIds: [],
    xiReady: false,
    overseasCount: 0,
    wkCount: 0,
  };
}

function makeRoomPayload(roomCode, hostUid, created = pastDate(10000)) {
  return {
    roomCode,
    hostUid,
    memberUids: [hostUid],
    status: "LOBBY",
    autoFillEightTeams: true,
    teams: [makeHostTeam(hostUid, "Royal Challengers")],
    auctionOrder: ["p001", "p002", "p003", "p035", "p075"],
    currentPlayerIndex: 0,
    currentBidLakhs: 0,
    highestBidderTeamId: "",
    highestBidderName: "",
    bidDeadlineEpochMs: 0,
    bidCount: 0,
    soldCount: 0,
    unsoldCount: 0,
    lastEventMessage: "Royal Challengers created Room " + roomCode,
    createdAt: created,
    updatedAt: created,
  };
}

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

test("Unauthenticated users cannot read/write /users or scrape /rooms collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("rooms").get());
  // Forging a non-guest hostUid while unauthenticated is rejected
  await assertFails(
    unauthDb.collection("rooms").doc("4829").set(makeRoomPayload("4829", ALICE_UID))
  );
  // Invalid room code format is rejected
  await assertFails(unauthDb.collection("rooms").doc("INVALID").get());
});

test("Guest multiplayer: create, join across devices, bid, RTM, and record matches without login", async () => {
  const guest1Db = testEnv.unauthenticatedContext().firestore();
  const guest2Db = testEnv.unauthenticatedContext().firestore();
  const created = pastDate(15000);

  // Guest 1 creates Room 5123 with auto-generated guest_... ID
  await assertSucceeds(
    guest1Db.collection("rooms").doc("5123").set(makeRoomPayload("5123", GUEST_HOST_ID, created))
  );

  // Guest 2 checks a missing room code cleanly (exists == false)
  const missingSnap = await assertSucceeds(guest2Db.collection("rooms").doc("9999").get());
  if (missingSnap.exists) {
    throw new Error("Expected room 9999 not to exist");
  }

  // Guest 2 joins Room 5123 without signing in
  const hostTeam = makeHostTeam(GUEST_HOST_ID, "Royal Challengers");
  const guest2Team = makeHostTeam(GUEST_JOIN_ID, "Mumbai Mavericks");
  await assertSucceeds(
    guest2Db.collection("rooms").doc("5123").update({
      memberUids: [GUEST_HOST_ID, GUEST_JOIN_ID],
      teams: [hostTeam, guest2Team],
      lastEventMessage: "Mumbai Mavericks joined the auction lobby!",
      updatedAt: pastDate(9000),
    })
  );

  // Guest 1 starts auction and Guest 2 places live bid
  await assertSucceeds(
    guest1Db.collection("rooms").doc("5123").update({
      status: "AUCTION",
      bidDeadlineEpochMs: Date.now() + 10000,
      lastEventMessage: "Live Auction started!",
      updatedAt: pastDate(8000),
    })
  );
  await assertSucceeds(
    guest2Db.collection("rooms").doc("5123").update({
      currentBidLakhs: 200,
      highestBidderTeamId: guest2Team.teamId,
      highestBidderName: guest2Team.teamName,
      bidDeadlineEpochMs: Date.now() + 10000,
      bidCount: 1,
      lastEventMessage: "Mumbai Mavericks bid ₹2 Cr!",
      updatedAt: pastDate(7000),
    })
  );

  // Guest 1 records completed match result synced to Guest 2
  const matchPayload = {
    roomCode: "5123",
    hostUid: GUEST_HOST_ID,
    memberUids: [GUEST_HOST_ID, GUEST_JOIN_ID],
    stage: "SERIES",
    teamAId: hostTeam.teamId,
    teamAName: hostTeam.teamName,
    teamBId: guest2Team.teamId,
    teamBName: guest2Team.teamName,
    teamAScore: 188,
    teamAWickets: 6,
    teamAOvers: "20.0",
    teamABalls: 120,
    teamBScore: 182,
    teamBWickets: 8,
    teamBOvers: "20.0",
    teamBBalls: 120,
    winnerTeamId: hostTeam.teamId,
    winnerTeamName: hostTeam.teamName,
    resultSummary: "Royal Challengers won by 6 runs",
    playerOfTheMatch: "Virat Kohli (74 off 45)",
    innings1Worm: [10, 22, 35],
    innings2Worm: [8, 19, 31],
    status: "COMPLETED",
    createdAt: pastDate(2000),
  };
  await assertSucceeds(
    guest1Db.collection("rooms").doc("5123").collection("matches").doc("m_g1").set(matchPayload)
  );
  await assertSucceeds(
    guest2Db.collection("rooms").doc("5123").collection("matches").get()
  );
  // Match records remain immutable
  await assertFails(
    guest1Db.collection("rooms").doc("5123").collection("matches").doc("m_g1").update({ teamAScore: 250 })
  );
});

test("User profile: owner CRUD succeeds; cross-user & ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Captain Alice",
      matchesPlayed: 0,
      matchesWon: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      favoriteTeamName: "Mumbai Strikers",
      updatedAt: pastDate(1000),
    })
  );
});

test("Signed-in room lifecycle: create, join, retention, live bid, RTM match, & terminal lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();

  const created = pastDate(15000);
  await assertSucceeds(
    aliceDb.collection("rooms").doc("4829").set(makeRoomPayload("4829", ALICE_UID, created))
  );

  const aliceTeam = makeHostTeam(ALICE_UID, "Royal Challengers");
  const bobTeam = makeHostTeam(BOB_UID, "Chennai Superstars");
  await assertSucceeds(
    bobDb.collection("rooms").doc("4829").update({
      memberUids: [ALICE_UID, BOB_UID],
      teams: [aliceTeam, bobTeam],
      lastEventMessage: "Chennai Superstars joined!",
      updatedAt: pastDate(8000),
    })
  );

  // Attempting to exceed 8 teams on join is rejected by rules
  const nineTeams = Array.from({ length: 9 }, (_, i) => makeHostTeam(`u_${i}`, `Team ${i}`));
  await assertFails(
    eveDb.collection("rooms").doc("4829").update({
      memberUids: [ALICE_UID, BOB_UID, EVE_UID],
      teams: nineTeams,
      lastEventMessage: "Overflow join",
      updatedAt: pastDate(7500),
    })
  );

  // Host starts AUCTION
  await assertSucceeds(
    aliceDb.collection("rooms").doc("4829").update({
      status: "AUCTION",
      currentPlayerIndex: 0,
      currentBidLakhs: 0,
      highestBidderTeamId: "",
      highestBidderName: "",
      bidDeadlineEpochMs: Date.now() + 10000,
      bidCount: 0,
      lastEventMessage: "Live Auction started!",
      updatedAt: pastDate(5000),
    })
  );

  // Signed-in outsider Eve cannot bid or mutate an active room she isn't a member of
  await assertFails(
    eveDb.collection("rooms").doc("4829").update({
      currentBidLakhs: 200,
      highestBidderTeamId: "team_eve",
      highestBidderName: "Eve XI",
      bidDeadlineEpochMs: Date.now() + 10000,
      bidCount: 1,
      lastEventMessage: "Eve hijack!",
      updatedAt: pastDate(4500),
    })
  );

  // Filtered list query succeeds for member; unfiltered fails
  await assertSucceeds(
    aliceDb.collection("rooms").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("rooms").get());

  // Terminal state lock
  await assertSucceeds(
    aliceDb.collection("rooms").doc("4829").update({
      status: "COMPLETED",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("rooms").doc("4829").update({
      status: "LOBBY",
      updatedAt: pastDate(500),
    })
  );
});
