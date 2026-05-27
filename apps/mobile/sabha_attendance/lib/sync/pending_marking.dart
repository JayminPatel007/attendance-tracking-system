/// A locally-queued Attendance Marking waiting to be pushed to the backend.
/// Identity is the `(occurrenceId, personId)` pair — re-enqueueing the same
/// pair replaces the prior entry so the queue is deduped on the conflict
/// surface that the backend's LWW resolves (ADR-0007).
class PendingMarking {
  PendingMarking({
    required this.occurrenceId,
    required this.personId,
    required this.present,
    required this.clientMarkedAt,
  });

  final String occurrenceId;
  final String personId;
  final bool present;
  final DateTime clientMarkedAt;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is PendingMarking &&
          other.occurrenceId == occurrenceId &&
          other.personId == personId &&
          other.present == present &&
          other.clientMarkedAt == clientMarkedAt);

  @override
  int get hashCode => Object.hash(occurrenceId, personId, present, clientMarkedAt);

  @override
  String toString() =>
      'PendingMarking(occ=$occurrenceId, person=$personId, present=$present, at=$clientMarkedAt)';
}
