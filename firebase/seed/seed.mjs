// Seeds the multi-tenant white-label menu into Cloud Firestore.
//
// Each tenant is a restaurant keyed by the app's applicationId, matching what the
// Android app reads at runtime (BuildConfig.APPLICATION_ID). Adding a client to
// FlavorFlow later just means seeding one more tenant document here.
//
// Credentials (Application Default Credentials), either:
//   gcloud auth application-default login          # your user creds, or
//   export GOOGLE_APPLICATION_CREDENTIALS=key.json # a service-account key
//
// Run:  FIREBASE_PROJECT_ID=flavorflow-digitalmenu npm run seed

import admin from "firebase-admin";

const projectId = process.env.FIREBASE_PROJECT_ID || "flavorflow-digitalmenu";
admin.initializeApp({ projectId });
const db = admin.firestore();

// --- Shared catalog -------------------------------------------------------

const categoryNames = {
  burgers: "Burgers",
  pizzas: "Pizzas",
  salads: "Salads",
  drinks: "Drinks",
  desserts: "Desserts",
};

const catalog = {
  burgers: [
    ["Classic Cheeseburger", "Beef patty, cheddar, lettuce, tomato"],
    ["Bacon Deluxe", "Double bacon, smoked cheese, BBQ sauce"],
    ["Veggie Burger", "Grilled mushroom, avocado, vegan mayo"],
    ["Spicy Chicken", "Crispy chicken, jalapeños, chipotle sauce"],
  ],
  pizzas: [
    ["Margherita", "Tomato, mozzarella, fresh basil"],
    ["Pepperoni", "Pepperoni, mozzarella, oregano"],
    ["Four Cheese", "Mozzarella, gorgonzola, parmesan, brie"],
    ["Veggie Supreme", "Peppers, onions, olives, mushrooms"],
  ],
  salads: [
    ["Caesar Salad", "Romaine, croutons, parmesan, Caesar dressing"],
    ["Greek Salad", "Cucumber, tomato, feta, olives"],
    ["Quinoa Bowl", "Quinoa, chickpeas, avocado, lemon dressing"],
  ],
  drinks: [
    ["Fresh Lemonade", "Squeezed lemons, mint, sparkling water"],
    ["Iced Coffee", "Cold brew, milk, ice"],
    ["Orange Juice", "100% freshly squeezed oranges"],
    ["Craft Soda", "Artisanal cola with cane sugar"],
  ],
  desserts: [
    ["Chocolate Brownie", "Warm brownie, vanilla ice cream"],
    ["Cheesecake", "New York style, berry compote"],
    ["Tiramisu", "Espresso-soaked ladyfingers, mascarpone"],
  ],
};

// Keyword-based food photo with a stable lock seed (mirrors the app's fake repo).
function imageUrl(name) {
  const keywords = name.trim().toLowerCase().replace(/\s+/g, ",");
  let hash = 0;
  for (const ch of name) hash = (Math.imul(31, hash) + ch.charCodeAt(0)) | 0;
  const lock = hash >>> 0;
  return `https://loremflickr.com/400/300/${keywords},food?lock=${lock}`;
}

// --- Tenants (white-label clients) ---------------------------------------
// Keyed by applicationId. The base package plus two demo brands with focused
// menus, to prove one project serves distinct menus per white-label build.

const tenants = {
  "dev.lssoftware.digitalmenu": {
    name: "Digital Menu",
    categories: ["burgers", "pizzas", "salads", "drinks", "desserts"],
  },
  "io.flavorflow.demo.acmeburgers": {
    name: "Acme Burgers",
    categories: ["burgers", "drinks", "desserts"],
  },
  "io.flavorflow.demo.bellapizza": {
    name: "Bella Pizza",
    categories: ["pizzas", "salads", "drinks", "desserts"],
  },
};

async function seed() {
  for (const [tenantId, cfg] of Object.entries(tenants)) {
    const restaurant = db.collection("restaurants").doc(tenantId);
    const batch = db.batch();

    batch.set(restaurant, { name: cfg.name });

    cfg.categories.forEach((categoryId, sortOrder) => {
      batch.set(restaurant.collection("categories").doc(categoryId), {
        name: categoryNames[categoryId],
        sortOrder,
      });

      catalog[categoryId].forEach(([name, description], index) => {
        const id = `${categoryId}-${index}`;
        const price = 9.0 + index * 3 + categoryId.length;
        batch.set(restaurant.collection("products").doc(id), {
          name,
          description,
          imageUrl: imageUrl(name),
          price,
          categoryId,
        });
      });
    });

    await batch.commit();
    console.log(`Seeded ${tenantId} (${cfg.name}) — ${cfg.categories.length} categories`);
  }
  console.log("Done.");
}

seed().then(
  () => process.exit(0),
  (err) => {
    console.error(err);
    process.exit(1);
  },
);
