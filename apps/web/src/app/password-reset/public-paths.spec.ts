import { isPublicPath } from './public-paths';

describe('isPublicPath', () => {
  it('treats the forgot-password and who-appointed-me routes as public', () => {
    expect(isPublicPath('/forgot-password')).toBeTrue();
    expect(isPublicPath('/who-appointed-me')).toBeTrue();
  });

  it('treats any other shell route as needing the OIDC session', () => {
    expect(isPublicPath('/')).toBeFalse();
    expect(isPublicPath('/dashboard')).toBeFalse();
    expect(isPublicPath('/role-appointment')).toBeFalse();
  });

  it('does not treat a path that merely starts with a public name as public', () => {
    expect(isPublicPath('/forgot-password-admin')).toBeFalse();
  });
});
