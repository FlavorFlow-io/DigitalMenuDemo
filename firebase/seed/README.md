# Firestore seed

Populates the multi-tenant white-label menu in Cloud Firestore.

Data model (per tenant, keyed by the app's `applicationId`):

```
restaurants/{applicationId}
  name: string
  categories/{categoryId}   { name, sortOrder }
  products/{productId}      { name, description, imageUrl, price, categoryId }
```

The Android app reads `restaurants/{BuildConfig.APPLICATION_ID}/…` at runtime, so
each white-label build automatically gets its own menu.

## Run

1. Authenticate (one of):

   ```bash
   gcloud auth application-default login
   # or point at a service-account key:
   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json
   ```

2. Install and seed:

   ```bash
   cd firebase/seed
   npm install
   FIREBASE_PROJECT_ID=flavorflow-digitalmenu npm run seed
   ```

Seeds three tenants: `io.flavorflow.demo` (base app, full menu),
`io.flavorflow.demo.acmeburgers`, and `io.flavorflow.demo.bellapizza`.

To add a client, add an entry to `tenants` in `seed.mjs` and re-run.
