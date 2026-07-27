# GalleryCleaner release signing

The GitHub Actions workflow requires these repository secrets:

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

The release build intentionally fails if the permanent release keystore is missing.
This prevents accidentally producing an APK signed with a different temporary/debug
certificate.

The project does not contain the `.jks` keystore. Keep the original keystore backed up
securely; it is required for future updates to install over existing releases.
