declare namespace API {
  type ActionRequest = {
    entityType?: string;
    entityId?: string;
  };

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

  type BaseResponseBoolean = {
    code?: number;
    data?: boolean;
    message?: string;
  };

  type BaseResponseCountsResponse = {
    code?: number;
    data?: CountsResponse;
    message?: string;
  };

  type BaseResponseFeedPageResponse = {
    code?: number;
    data?: FeedPageResponse;
    message?: string;
  };

  type BaseResponseInteger = {
    code?: number;
    data?: number;
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

  type BaseResponseListProfileResponse = {
    code?: number;
    data?: ProfileResponse[];
    message?: string;
  };

  type BaseResponseListSearchResult = {
    code?: number;
    data?: SearchResult[];
    message?: string;
  };

  type BaseResponseMapStringBoolean = {
    code?: number;
    data?: Record<string, any>;
    message?: string;
  };

  type BaseResponseMapStringLong = {
    code?: number;
    data?: Record<string, any>;
    message?: string;
  };

  type BaseResponseMapStringObject = {
    code?: number;
    data?: Record<string, any>;
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

  type BaseResponseStoragePresignResponse = {
    code?: number;
    data?: StoragePresignResponse;
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

  type counterParams = {
    userId: number;
  };

  type CountsResponse = {
    entityType?: string;
    entityId?: string;
    counts?: Record<string, any>;
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

  type followersParams = {
    userID: number;
    limit?: number;
    offset?: number;
    cursor?: number;
  };

  type followingParams = {
    userID: number;
    limit?: number;
    offset?: number;
    cursor?: number;
  };

  type followParams = {
    toUserId: number;
  };

  type getCountsParams = {
    etype: string;
    eid: string;
    metrics?: string;
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

  type reindexParams = {
    id: number;
  };

  type resetPasswordParams = {
    request: PasswordResetRequest;
  };

  type searchParams = {
    q: string;
    page?: number;
    size?: number;
  };

  type SearchResult = {
    postId?: number;
    title?: string;
    snippet?: string;
    likeCount?: number;
    favCount?: number;
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

  type statusParams = {
    toUserId: number;
  };

  type StoragePresignRequest = {
    scene: string;
    postId: string;
    contentType: string;
    ext?: string;
  };

  type StoragePresignResponse = {
    objectKey?: string;
    putUrl?: string;
    headers?: Record<string, any>;
    expire?: number;
  };

  type streamQAParams = {
    id: number;
    question: string;
    topK?: number;
    maxTokens?: number;
  };

  type suggestParams = {
    q: string;
    size?: number;
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

  type unfollowParams = {
    toUserId: number;
  };
}
