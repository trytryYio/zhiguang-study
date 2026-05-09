// 委托到统一的 api/client
import { api, type Schemas } from "@/api/client";
import type { ProfileResponse, ProfileUpdateRequest } from "@/types/profile";

export const profileService = {
    update: (payload: ProfileUpdateRequest) =>
        api.profile.update(payload as Schemas["ProfilePatchRequest"]) as Promise<ProfileResponse>,

    uploadAvatar: (file: File) =>
        api.profile.uploadAvatar(file) as Promise<ProfileResponse>
};