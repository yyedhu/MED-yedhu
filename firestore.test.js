const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowMinusOneSec() {
  return new Date(Date.now() - 1000);
}

function validAddress(uid) {
  const ts = nowMinusOneSec();
  return {
    userId: uid,
    label: "Home",
    fullAddress: "42 MG Road, Indiranagar, Bengaluru 560038",
    landmark: "Near Metro Station",
    contactPhone: "+91 9876543210",
    isDefault: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validCartItem(uid) {
  const ts = nowMinusOneSec();
  return {
    userId: uid,
    storeId: "store_apollo",
    storeName: "Apollo Care Pharmacy",
    medicineId: "med_dolo650",
    medicineName: "Dolo 650mg Tablet",
    dosage: "650mg • 15 tablets",
    unitPrice: 34.5,
    quantity: 2,
    requiresPrescription: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validOrder(uid) {
  const ts = nowMinusOneSec();
  return {
    userId: uid,
    storeId: "store_apollo",
    storeName: "Apollo Care Pharmacy",
    itemsSummary: "Dolo 650mg Tablet x2",
    itemsPayload: "med_dolo650:2",
    itemCount: 2,
    subtotal: 69.0,
    deliveryFee: 20.0,
    discountAmount: 0.0,
    couponCode: "",
    tipAmount: 15.0,
    totalAmount: 104.0,
    paymentMethod: "upi",
    deliveryAddress: "42 MG Road, Ernakulam, Kochi 682016",
    deliveryInstructions: "Leave with security",
    status: "placed",
    hasPrescription: false,
    prescriptionFileName: "",
    estimatedMinutes: 25,
    rating: 0,
    reviewComment: "",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validTracking(uid, orderId) {
  const ts = nowMinusOneSec();
  return {
    userId: uid,
    orderId: orderId,
    storeId: "store_apollo_care",
    storeName: "Apollo Care Pharmacy",
    storeNeighborhood: "MG Road, Ernakulam, Kochi",
    destinationArea: "Edappally",
    transitCorridor: "NH-66 / Kochi Metro Corridor",
    partnerId: "rider_arjun",
    partnerName: "Arjun Nair",
    partnerPhone: "+919847021456",
    partnerVehicle: "KL-07-CX-4821 • Electric Scooter",
    status: "placed",
    progressFraction: 0.0,
    storeLat: 9.971,
    storeLng: 76.283,
    destLat: 10.0261,
    destLng: 76.3125,
    partnerLat: 9.971,
    partnerLng: 76.283,
    createdAt: ts,
    updatedAt: ts,
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

test("Unauthenticated user cannot read or write addresses, cartItems, or orders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("addresses").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("cartItems").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("orders").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1").set(validOrder(ALICE_UID))
  );
});

test("Owner can create, list, update, and delete own address", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const addrRef = aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_1");
  await assertSucceeds(addrRef.set(validAddress(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("addresses").get());
  await assertSucceeds(
    addrRef.update({
      label: "Work",
      updatedAt: nowMinusOneSec(),
    })
  );
  await assertSucceeds(addrRef.delete());
});

test("Cross-user access to addresses, cartItems, and orders is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_alice").set(validOrder(ALICE_UID))
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("orders").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_alice").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_spoof").set(validOrder(BOB_UID))
  );
});

test("Shadow update with ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const cartRef = aliceDb.collection("users").doc(ALICE_UID).collection("cartItems").doc("item_1");
  const payloadWithGhost = { ...validCartItem(ALICE_UID), isAdminApproved: true };
  await assertFails(cartRef.set(payloadWithGhost));
});

test("Order status must start at placed and follow valid state transitions with terminal lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orderRef = aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_flow");

  // Cannot create directly as delivered
  await assertFails(orderRef.set({ ...validOrder(ALICE_UID), status: "delivered" }));

  // Create as placed
  await assertSucceeds(orderRef.set(validOrder(ALICE_UID)));

  // Cannot jump placed -> delivered
  await assertFails(orderRef.update({ status: "delivered", updatedAt: nowMinusOneSec() }));

  // Valid chain: placed -> confirmed -> packed -> out_for_delivery -> delivered
  await assertSucceeds(orderRef.update({ status: "confirmed", updatedAt: nowMinusOneSec() }));
  await assertSucceeds(orderRef.update({ status: "packed", updatedAt: nowMinusOneSec() }));
  await assertSucceeds(orderRef.update({ status: "out_for_delivery", updatedAt: nowMinusOneSec() }));
  await assertSucceeds(orderRef.update({ status: "delivered", updatedAt: nowMinusOneSec() }));

  // Terminal state lock: cannot modify or delete after delivered
  await assertFails(orderRef.update({ status: "cancelled", updatedAt: nowMinusOneSec() }));
  await assertFails(orderRef.delete());
});

test("Owner can create and update live Google Maps tracking coordinates while unauthenticated and cross-user access fail", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const aliceTrackRef = aliceDb.collection("users").doc(ALICE_UID).collection("tracking").doc("ord_gps_1");
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tracking").doc("ord_gps_1").set(validTracking(ALICE_UID, "ord_gps_1"))
  );

  await assertSucceeds(aliceTrackRef.set(validTracking(ALICE_UID, "ord_gps_1")));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("tracking").get());

  // Cross-user read & write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tracking").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("tracking").doc("ord_gps_1").update({
      partnerLat: 10.0,
      partnerLng: 76.3,
      updatedAt: nowMinusOneSec(),
    })
  );

  // Invalid out-of-range GPS coordinates rejected
  await assertFails(
    aliceTrackRef.update({
      partnerLat: 120.0,
      partnerLng: 76.3,
      updatedAt: nowMinusOneSec(),
    })
  );

  // Valid GPS step update succeeds, then locks after delivered
  await assertSucceeds(
    aliceTrackRef.update({
      status: "out_for_delivery",
      progressFraction: 0.55,
      partnerLat: 10.001,
      partnerLng: 76.299,
      updatedAt: nowMinusOneSec(),
    })
  );
  await assertSucceeds(
    aliceTrackRef.update({
      status: "delivered",
      progressFraction: 1.0,
      partnerLat: 10.0261,
      partnerLng: 76.3125,
      updatedAt: nowMinusOneSec(),
    })
  );
  await assertFails(
    aliceTrackRef.update({
      partnerLat: 9.99,
      updatedAt: nowMinusOneSec(),
    })
  );
  await assertFails(aliceTrackRef.delete());
});

test("Owner can favorite stores and submit post-delivery ratings only after delivery", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Favorites CRUD
  const favRef = aliceDb.collection("users").doc(ALICE_UID).collection("favorites").doc("store_apollo_care");
  await assertSucceeds(
    favRef.set({
      userId: ALICE_UID,
      storeId: "store_apollo_care",
      createdAt: nowMinusOneSec(),
      updatedAt: nowMinusOneSec(),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("favorites").doc("store_apollo_care").delete()
  );
  await assertSucceeds(favRef.delete());

  // Order rating rejected before delivered, allowed once delivered
  const orderRef = aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_rate");
  await assertSucceeds(orderRef.set(validOrder(ALICE_UID)));
  await assertFails(orderRef.update({ rating: 5, reviewComment: "Great!", updatedAt: nowMinusOneSec() }));

  await assertSucceeds(orderRef.update({ status: "confirmed", updatedAt: nowMinusOneSec() }));
  await assertSucceeds(orderRef.update({ status: "packed", updatedAt: nowMinusOneSec() }));
  await assertSucceeds(orderRef.update({ status: "out_for_delivery", updatedAt: nowMinusOneSec() }));
  await assertSucceeds(orderRef.update({ status: "delivered", updatedAt: nowMinusOneSec() }));

  // Out-of-range rating fails
  await assertFails(orderRef.update({ rating: 6, reviewComment: "Too high", updatedAt: nowMinusOneSec() }));
  // Valid 5-star rating succeeds
  await assertSucceeds(
    orderRef.update({ rating: 5, reviewComment: "Fast sealed delivery", updatedAt: nowMinusOneSec() })
  );
});
