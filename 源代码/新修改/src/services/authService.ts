// 委托到统一的 api/client，保持对外接口不变
import { api, type Schemas } from "@/api/client";
import type {
    AuthenticatedUser, LoginRequest, LoginResponse, LogoutRequest,
    RefreshResponse, RegisterRequest, RegisterResponse,
    SendCodeRequest, SendCodeResponse
} from "@/types/auth";

export const authService = {
    sendCode: (payload: SendCodeRequest) =>
        api.auth.sendCode({
            identifierType: payload.identifierType as Schemas["SendCodeRequest"]["identifierType"],
            identifier: payload.identifier,
            scene: payload.scene as Schemas["SendCodeRequest"]["scene"]
        }) as Promise<SendCodeResponse>,

    register: (payload: RegisterRequest) =>
        api.auth.register({
            identifierType: payload.identifierType as Schemas["RegisterRequest"]["identifierType"],
            identifier: payload.identifier,
            code: payload.code,
            password: payload.password,
            agreeTerms: payload.agreeTerms
        }) as Promise<RegisterResponse>,

    login: (payload: LoginRequest) =>
        api.auth.login({
            identifierType: payload.identifierType as Schemas["LoginRequest"]["identifierType"],
            identifier: payload.identifier,
            password: payload.password,
            code: payload.code
        }) as Promise<LoginResponse>,

    logout: (payload: LogoutRequest, _accessToken: string) =>
        api.auth.logout({ refreshToken: payload.refreshToken }),

    fetchCurrentUser: (_accessToken: string) =>
        api.auth.me() as Promise<AuthenticatedUser>,

    refresh: (refreshToken: string) =>
        api.auth.refresh({ refreshToken }) as Promise<RefreshResponse>
};
