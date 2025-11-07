UserManagementService.java:
- Added self-delete prevention
- Added safe Role.valueOf handling
- Added @PreAuthorize protection
  Waiting for approval to merge to main

UserDataController.java:
- Modify patch to emit out exception messages in responses as well.

AuthService.java:
- Replaces refresh token from cookies to directly use Redis for storing refresh_token which is more reliable since cookies can sometimes breaks.
- Removes all the cookies-related code in reflection of migrating to Redis for refresh_token
- Integrates switch for user.getStatus() to throw exceptions based on Status or let authentication codes execute if user is ACTIVE

RedisService.java:
- Add additional logics:
  - saveToken(String email, String token, long expirationSeconds)
  - deleteToken(String email)
  - getToken(String email)

New Exceptions:
- AccountDeletedException
- AccountNotActiveException
- AccountSuspendedException

