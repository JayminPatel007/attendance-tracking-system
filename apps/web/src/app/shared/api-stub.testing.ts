/**
 * A jasmine double for one of the generated API modules (issue #131).
 *
 * Every generated operation is overloaded on `observe` — `'body'`, `'response'`,
 * `'events'` — and TypeScript resolves a `jasmine.SpyObj<T>`'s `and.returnValue`
 * against the *last* of those overloads, `Observable<HttpEvent<T>>`. So stubbing
 * an operation with the body a section actually consumes (`of([anOccurrence])`)
 * fails to typecheck, even though the body overload is the one the section calls.
 *
 * Widening the stubbed operations to plain {@link jasmine.Spy} sidesteps the
 * overload resolution while keeping the part of the type that earns its keep:
 * `methods` is checked against the real module, so an operation renamed in the
 * regenerated client breaks its stub here rather than silently going un-asserted.
 */
export type ApiStub<T> = { [K in keyof T]: jasmine.Spy };

export function apiStub<T>(name: string, methods: readonly (keyof T & string)[]): ApiStub<T> {
  return jasmine.createSpyObj(name, [...methods]);
}
