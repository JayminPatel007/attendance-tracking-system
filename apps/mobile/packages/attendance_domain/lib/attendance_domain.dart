/// attendance_domain — mobile mirror of the backend attendance bounded context.
///
/// Holds AttendanceMarking, MarkingType (Roster vs Walk-in), and the offline
/// pending-action queue shape per ADR-0007. The mobile app is the primary
/// capture surface (ADR-0003); this package therefore evolves richer than the
/// web's counterpart.
library attendance_domain;

const String attendanceDomainPlaceholder = 'attendance_domain';
