# Sabha Attendance — Mobile

Melos workspace per ADR-0015. The deployable Flutter app lives in
`sabha_attendance/`; reusable libraries live in `packages/`.

```
apps/mobile/
  melos.yaml
  pubspec.yaml                  # workspace root
  sabha_attendance/             # the Flutter app (ADR-0003)
    lib/main.dart
    pubspec.yaml
    test/splash_test.dart
  packages/
    shared_kernel/              # cross-context VOs
    identity_domain/            # User, Session
    sabha_domain/               # Sabha, Occurrence, Roster
    attendance_domain/          # AttendanceMarking, MarkingType
```

## First-time setup

```bash
# Once: install Flutter SDK >= 3.22, then
dart pub global activate melos
cd apps/mobile
melos bootstrap

# Generate the Flutter app's platform directories (gitignored)
cd sabha_attendance
flutter create --platforms=android,ios --project-name=sabha_attendance_mobile .
flutter pub get
```

## Day-to-day

```bash
# from apps/mobile
melos run analyze        # flutter analyze across every package
melos run test           # flutter test across every package with a test/ dir

# from apps/mobile/sabha_attendance
flutter run              # boot the app on an emulator
```

## What's in scope per slice

Slice 1 (this commit) ships the workspace skeleton and the smoke widget test.
Subsequent slices add `*_data` packages (HTTP, local storage adapters) and
feature packages as login (#3), offline sync (#5), walk-in (#8), etc. land.
