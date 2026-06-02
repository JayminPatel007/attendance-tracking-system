# Developer setup

This guide gets the whole app running on your machine. It assumes you have
**Docker Desktop** installed and running. No prior knowledge of the mobile
toolchain is needed — the mobile section explains things from scratch.

## What's in the stack

`docker compose up` starts four pieces that work together:

| Piece | What it is | Where you reach it |
| --- | --- | --- |
| **postgres** | The database. | `localhost:55432` |
| **keycloak** | Handles login (usernames, passwords, who-can-do-what). | `localhost:58080` |
| **backend** | The Spring Boot API + the web app's login broker. | `localhost:8080` |
| **web** | The Angular admin panel (what staff use in a browser). | `localhost:4200` |

The mobile app is **not** part of `docker compose` — phones run it, not Docker.
See [Mobile app](#mobile-app-flutter) below.

---

## Quick start

### 1. One-time setup: let your browser find Keycloak

The web panel logs you in by sending your browser to Keycloak. Inside Docker,
Keycloak is known by the name `keycloak`, and your browser needs to resolve that
name to your own machine. Add one line to your hosts file (do this once):

```sh
echo '127.0.0.1 keycloak' | sudo tee -a /etc/hosts
```

> **Why this is needed.** Both the browser and the backend must reach Keycloak
> at the *exact same* address (`http://keycloak:58080`), otherwise login tokens
> are stamped with one address but checked against another, and login fails.
> This single hosts entry makes `keycloak` mean "my machine" for the browser,
> while Docker already makes it mean the same Keycloak for the backend.

### 2. Start everything

```sh
docker compose up
```

The first run builds the backend and web images, so it takes a few minutes.
Wait until all four services report **healthy**. Then open:

- **Web panel:** http://localhost:4200
- **Keycloak admin console:** http://localhost:58080 (admin: `admin` / `admin`)

### 3. Log in to the web panel

Open http://localhost:4200, click to log in, and use a seeded account:

- **Madhyastha Karyalaya (MK) admin** — username `mk-admin`, password `changeme123!`
- **Sanchalak** — username `sanchalak`, password `changeme123!`

On first login Keycloak asks you to set a new password (this is expected).

> The seeded accounts and secrets come from `.env` (copied from `.env.example`).
> The defaults are fine for local development.

---

## The web panel (how login works, in plain terms)

You never type your password into the Angular app. Instead:

1. The browser asks the backend "am I logged in?" — if not, it sends you to
   Keycloak's login page.
2. You log in at Keycloak. Keycloak sends you back to the backend.
3. The backend keeps your login tokens **server-side** and gives your browser
   only a small session cookie. The Angular app proves who you are with that
   cookie on every request.

This is the "Backend-for-Frontend" (BFF) pattern — it keeps tokens out of the
browser where malicious scripts could steal them. The full reasoning is in
[ADR-0022](adr/0022-web-session-via-bff-http-only-cookie.md).

For day-to-day **frontend** work you usually don't rebuild the Docker image on
every change. Run the live-reload dev server instead (backend + Keycloak still
come from `docker compose up`):

```sh
cd apps/web
npm install      # first time only
npm start        # serves http://localhost:4200 with auto-reload
```

`npm start` proxies the login/API paths to the backend for you (see
`apps/web/proxy.conf.json`), so login works the same way.

---

## Backend integration tests on macOS

The backend's integration tests start throwaway Postgres + Keycloak containers
automatically (via Testcontainers). On macOS with Docker Desktop, two settings
are needed the first time, both because of how Docker's socket works on a Mac.

### 1. Point Testcontainers at the real Docker socket

Docker Desktop on macOS hides the real daemon behind a couple of proxy sockets,
and Testcontainers' auto-detection stops at a proxy that can't actually run
containers. Tell it the real one in `~/.testcontainers.properties`:

```properties
docker.host=unix:///Users/<your-username>/Library/Containers/com.docker.docker/Data/docker.raw.sock
testcontainers.reuse.enable=true
```

(If you also run tests from your IDE, set the `DOCKER_HOST` environment variable
to the same value.)

### 2. Turn off Ryuk (the test cleanup container)

Ryuk is a helper container Testcontainers uses to clean up afterwards. It can't
start on macOS Docker Desktop, so disable it:

```sh
export TESTCONTAINERS_RYUK_DISABLED=true
```

Add that line to your shell config (`~/.zshrc`) so you don't have to repeat it.
**Linux and CI don't need this** — Ryuk works fine there.

### 3. Don't remove the pinned Docker API version

`apps/backend/application-container/pom.xml` pins `<api.version>1.43</api.version>`.
Without it, the test Docker client is too old for modern Docker Desktop and
fails with "client version too old." Leave it as-is.

---

## Mobile app (Flutter)

> **New to mobile development? Read this first.** The mobile app is built with
> **Flutter**, Google's toolkit for building Android and iOS apps from one
> codebase. You write the app once; Flutter compiles it for each platform. To
> run it you need:
>
> - **The Flutter SDK** — install it from https://docs.flutter.dev/get-started/install
>   and run `flutter doctor`; it tells you what else to install and how to fix it.
> - **An emulator or simulator** — a phone that runs as a window on your
>   computer. *Android emulator* comes with **Android Studio**; *iOS simulator*
>   comes with **Xcode** (Mac only).
>
> You do **not** need any of this to work on the backend or the web panel. Skip
> this whole section unless you're specifically working on the mobile app.

### One-time setup after cloning

We don't commit the auto-generated `android/` and `ios/` folders (see
[ADR-0014](adr/0014-monorepo-and-framework-scaffolding.md)). After a fresh
clone, generate them once:

```sh
cd apps/mobile/sabha_attendance
flutter create --platforms=android,ios --project-name=sabha_attendance_mobile .
```

Then make two small edits so login can hand control back to the app. (Login
opens a browser; these tell the phone "when a link starting with `com.sabha.app`
opens, give it back to our app.")

**Android** — in `android/app/src/main/AndroidManifest.xml`, inside the
`<activity android:name=".MainActivity" ...>` tag, add:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW"/>
    <category android:name="android.intent.category.DEFAULT"/>
    <category android:name="android.intent.category.BROWSABLE"/>
    <data android:scheme="com.sabha.app"/>
</intent-filter>
```

Also set `minSdkVersion` to `21` in `android/app/build.gradle` (the login
library needs at least that).

**iOS** — in `ios/Runner/Info.plist`, inside the top-level `<dict>`, add:

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

### Running the app

Start the backend stack first (`docker compose up`), then from
`apps/mobile/sabha_attendance/`:

- **iOS Simulator:** `flutter run` — `localhost` works as-is.
- **Android emulator:** the emulator can't see your machine as `localhost`; it
  uses the special address `10.0.2.2` instead, so pass the backend and login
  addresses explicitly:

  ```sh
  flutter run \
    --dart-define=OIDC_ISSUER=http://10.0.2.2:58080/realms/sabha \
    --dart-define=BACKEND_BASE_URL=http://10.0.2.2:8080
  ```

---

## Troubleshooting

**Keycloak stays "waiting" / "unhealthy" forever even though I can open it in a
browser.** This was a bug in an older compose file: the health check opened a
connection to Keycloak but never told it to close, so the check hung until it
timed out and never reported success. The current `docker-compose.yml` sends a
`Connection: close` header, which fixes it. If you still see this, make sure your
`docker-compose.yml` is up to date (`git pull`) and recreate the container:
`docker compose up -d --force-recreate keycloak`.

**Login fails with "Invalid parameter: redirect_uri" or the browser can't reach
`keycloak`.** You're missing the hosts entry from
[Quick start step 1](#1-one-time-setup-let-your-browser-find-keycloak).

**I changed backend or web code but the running container shows the old
behavior.** Docker reuses the previously built image. Rebuild it:
`docker compose up -d --build backend web`.
