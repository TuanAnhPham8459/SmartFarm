# Security

## Reporting a problem

If you find a security issue in Smart Farm, please don't open a public issue.
Contact the maintainer through GitHub instead, and include steps to reproduce it.

## How this project handles secrets

- **Firebase config (`google-services.json`)** is not stored in this repository.
  Each developer downloads their own from the Firebase console into `SmartFarmModule/`.
- **Service account keys** for the Raspberry Pi stay on the Pi only. The firmware reads the
  path from the `GOOGLE_APPLICATION_CREDENTIALS` environment variable. Key files are
  blocked by `.gitignore`.
- **Firestore access** is limited to signed-in users by the rules in
  [`firebase/firestore.rules`](firebase/firestore.rules).
