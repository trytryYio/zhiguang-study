declare namespace API {
  type AuthResponse = {
    user?: AuthUserResponse;
    token?: TokenResponse;
  };

  type AuthUserResponse = {
    id?: number;
    nickname?: string;
    avatar?: string;
    phone?: string;
    zhId?: string;
    birthday?: string;
    school?: string;
    bio?: string;
    gender?: string;
    tagJson?: string;
  };

  type BaseResponseAuthResponse = {
    code?: number;
    data?: AuthResponse;
    message?: string;
  };

  type BaseResponseAuthUserResponse = {
    code?: number;
    data?: AuthUserResponse;
    message?: string;
  };

  type BaseResponseProfileResponse = {
    code?: number;
    data?: ProfileResponse;
    message?: string;
  };

  type BaseResponseSendCodeResponse = {
    code?: number;
    data?: SendCodeResponse;
    message?: string;
  };

  type BaseResponseTokenResponse = {
    code?: number;
    data?: TokenResponse;
    message?: string;
  };

  type LoginRequest = {
    identifierType: "PHONE" | "EMAIL";
    identifier: string;
    code?: string;
    password?: string;
  };

  type LogoutRequest = {
    refreshToken: string;
  };

  type PasswordResetRequest = {
    identifierType: "PHONE" | "EMAIL";
    identifier: string;
    code: string;
    newPassword: string;
  };

  type ProfilePatchRequest = {
    nickname?: string;
    bio?: string;
    gender?: string;
    birthday?: string;
    zgId?: string;
    school?: string;
    tagJson?: string;
  };

  type ProfileResponse = {
    id?: number;
    nickname?: string;
    avatar?: string;
    bio?: string;
    zgId?: string;
    gender?: string;
    birthday?: string;
    school?: string;
    phone?: string;
    email?: string;
    tagJson?: string;
  };

  type RegisterRequest = {
    identifierType: "PHONE" | "EMAIL";
    identifier: string;
    code: string;
    password?: string;
    agreeTerms?: boolean;
  };

  type resetPasswordParams = {
    request: PasswordResetRequest;
  };

  type SendCodeRequest = {
    scene: "REGISTER" | "LOGIN" | "RESET_PASSWORD";
    identifierType: "PHONE" | "EMAIL";
    identifier: string;
  };

  type SendCodeResponse = {
    identifier?: string;
    scene?: "REGISTER" | "LOGIN" | "RESET_PASSWORD";
    expireSeconds?: number;
  };

  type TokenRefreshRequest = {
    refreshToken: string;
  };

  type TokenResponse = {
    accessToken?: string;
    accessTokenExpiresAt?: string;
    refreshToken?: string;
    refreshTokenExpiresAt?: string;
  };
}
