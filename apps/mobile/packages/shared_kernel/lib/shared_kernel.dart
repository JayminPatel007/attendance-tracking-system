/// shared_kernel — cross-context value objects (Mobile, OccurrenceId, etc.).
///
/// Per ADR-0015 every other mobile package may depend on shared_kernel; the
/// converse is forbidden. Pure Dart — no Flutter import — so this package can
/// later be reused outside Flutter contexts if a non-Flutter client emerges.
library shared_kernel;

const String sharedKernelPlaceholder = 'shared_kernel';
