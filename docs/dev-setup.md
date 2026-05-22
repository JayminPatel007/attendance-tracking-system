# Developer setup notes

Quick gotchas that aren't obvious from the codebase.

## Backend integration tests on macOS (Docker Desktop)

The backend's integration tests use Testcontainers to spin up Postgres + Keycloak (see `apps/backend/bootstrap/src/test/java/.../SmokeAuthIntegrationTest.java`). Two macOS-specific quirks must be handled the first time you run them.

### 1. Point Testcontainers at the *real* Docker daemon socket

Docker Desktop on macOS exposes a chain of sockets:

- `/var/run/docker.sock` → symlink to `~/.docker/run/docker.sock`
- `~/.docker/run/docker.sock` → CLI proxy socket that returns a stub `/info` response
- `~/Library/Containers/com.docker.docker/Data/docker.raw.sock` → the **actual daemon**

Testcontainers' auto-probe lands on the CLI proxy and gives up. Point it at the raw socket via `~/.testcontainers.properties`:

```properties
docker.host=unix:///Users/<you>/Library/Containers/com.docker.docker/Data/docker.raw.sock
testcontainers.reuse.enable=true
```

(Set `DOCKER_HOST` to the same value if you also run tests outside Maven, e.g. from your IDE.)

### 2. Disable Ryuk (the testcontainers reaper)

Ryuk runs as a container that bind-mounts the host's Docker socket to clean up after itself. On macOS, Docker Desktop's Linux VM can't bind-mount the macOS-side socket back, so Ryuk fails to start. Disable it:

```sh
export TESTCONTAINERS_RYUK_DISABLED=true
```

Or add to your shell rc file. **Linux developers and CI runners don't need this** — the socket lives in the same kernel namespace there, and Ryuk works fine.

### 3. The Docker API version is pinned in the pom

`apps/backend/bootstrap/pom.xml` sets `<api.version>1.43</api.version>` as a surefire system property. docker-java's default is 1.32, which Docker Desktop 24+ rejects with "client version too old." Don't drop that property unless you're also dropping support for new Docker versions.

## Running the stack

`docker compose up` starts Postgres (`localhost:55432`), Keycloak (`localhost:58080`, admin: `admin` / `admin`), and the backend (`localhost:8080`). The Keycloak realm is imported from `infra/keycloak/realm-sabha.json` on first boot.

Seeded Sanchalak credentials (for manual smoke-testing):

- username: `sanchalak`
- password: `changeme123!`

## Mobile: platform setup after `flutter create`

Per ADR-0014 the `android/` and `ios/` directories are not committed. After cloning fresh, run `cd apps/mobile/sabha_attendance && flutter create --platforms=android,ios --project-name=sabha_attendance_mobile .` once, then patch in the OIDC redirect URI scheme:

### Android — `android/app/src/main/AndroidManifest.xml`

Inside `<activity android:name=".MainActivity" ...>` add:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW"/>
    <category android:name="android.intent.category.DEFAULT"/>
    <category android:name="android.intent.category.BROWSABLE"/>
    <data android:scheme="com.sabha.app"/>
</intent-filter>
```

Also bump `defaultConfig.minSdkVersion` to 21 in `android/app/build.gradle` (flutter_appauth's floor).

### iOS — `ios/Runner/Info.plist`

Inside the top-level `<dict>` add:

```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleTypeRole</key>
        <string>Editor</string>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>com.sabha.app</string>
        </array>
    </dict>
</array>
```

### Running against the stack

`docker compose up` first. Then from `apps/mobile/sabha_attendance/`:

- iOS Simulator: `flutter run` (localhost works as-is)
- Android emulator: `flutter run --dart-define=OIDC_ISSUER=http://10.0.2.2:58080/realms/sabha --dart-define=BACKEND_BASE_URL=http://10.0.2.2:8080`
