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

  type BaseResponse = {
    code?: number;
    data?: Record<string, any>;
    message?: string;
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

  type BaseResponseFeedPageResponse = {
    code?: number;
    data?: FeedPageResponse;
    message?: string;
  };

  type BaseResponseKnowPostDetailResponse = {
    code?: number;
    data?: KnowPostDetailResponse;
    message?: string;
  };

  type BaseResponseKnowPostDraftCreateResponse = {
    code?: number;
    data?: KnowPostDraftCreateResponse;
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

  type BaseResponseVoid = {
    code?: number;
    data?: Record<string, any>;
    message?: string;
  };

  type confirmContentParams = {
    id: number;
  };

  type deleteUsingDELETEParams = {
    id: number;
  };

  type detailParams = {
    id: number;
  };

  type FeedItemResponse = {
    id?: string;
    title?: string;
    description?: string;
    coverImage?: string;
    tags?: string[];
    authorAvatar?: string;
    authorNickname?: string;
    tagJson?: string;
    likeCount?: number;
    favoriteCount?: number;
    liked?: boolean;
    faved?: boolean;
    isTop?: boolean;
  };

  type FeedPageResponse = {
    items?: FeedItemResponse[];
    page?: number;
    size?: number;
    hasMore?: boolean;
  };

  type feedParams = {
    page?: number;
    size?: number;
  };

  type KnowPostContentConfirmRequest = {
    objectKey: string;
    etag: string;
    size: number;
    sha256: string;
  };

  type KnowPostDetailResponse = {
    id?: string;
    title?: string;
    description?: string;
    contentUrl?: string;
    images?: string[];
    tags?: string[];
    authorId?: string;
    authorAvatar?: string;
    authorNickname?: string;
    authorTagJson?: string;
    likeCount?: number;
    favoriteCount?: number;
    liked?: boolean;
    faved?: boolean;
    isTop?: boolean;
    visible?: string;
    type?: string;
    publishTime?: string;
  };

  type KnowPostDraftCreateResponse = {
    id?: string;
  };

  type KnowPostPatchRequest = {
    title?: string;
    tagId?: number;
    tags?: string[];
    imgUrls?: string[];
    visible?: string;
    isTop?: boolean;
    description?: string;
  };

  type KnowPostTopPatchRequest = {
    isTop: boolean;
  };

  type KnowPostVisibilityPatchRequest = {
    visible: string;
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

  type mineParams = {
    page?: number;
    size?: number;
  };

  type PasswordResetRequest = {
    identifierType: "PHONE" | "EMAIL";
    identifier: string;
    code: string;
    newPassword: string;
  };

  type patchMetadataParams = {
    id: number;
  };

  type patchTopParams = {
    id: number;
  };

  type patchVisibilityParams = {
    id: number;
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

  type publishParams = {
    id: number;
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
