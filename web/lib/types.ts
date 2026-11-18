// Shapes returned by the gateway (see the Java DTOs named in each comment).

export type Gender = "MAN" | "WOMAN" | "NON_BINARY" | "OTHER";
export type SwipeAction = "LIKE" | "PASS" | "SUPER_LIKE";

/** discovery-service FeedCard */
export interface FeedCard {
  userId: string;
  displayName: string;
  verified: boolean;
  age: number | null;
  distanceKm: number;
  bio: string | null;
  city: string | null;
  lookingFor: string[];
  sharedInterests: string[];
  photoUrls: string[];
  reasons: string[];
  score: number;
}

/** match-service SwipeResponse */
export interface SwipeResponse {
  swipeId: string;
  targetUserId: string;
  direction: SwipeAction;
  createdAt: string;
  matched: boolean;
  matchId: string | null;
}

/** match-service SwipeQuota */
export interface SwipeQuota {
  limit: number;
  remaining: number;
  resetsAt: string;
}

/** match-service MatchResponse */
export interface MatchSummary {
  matchId: string;
  otherUserId: string;
  matchedAt: string;
}

/** notification-service NotificationResponse */
export interface AppNotification {
  id: string;
  type: "MATCH_CREATED" | "NEW_MESSAGE";
  message: string;
  referenceId: string;
  read: boolean;
  createdAt: string;
}

/** user-service PublicProfileResponse (fields the web app uses) */
export interface PublicProfile {
  userId: string;
  displayName: string;
  verified: boolean;
  age: number | null;
  bio: string | null;
  city: string | null;
  interests: string[];
  photoUrls: string[];
}

export interface PhotoRef {
  photoId: string;
  url: string;
}

/** user-service MyProfileResponse (fields the web app uses) */
export interface MyProfile extends PublicProfile {
  completionPercent: number;
  gender: Gender | null;
  interestedIn: Gender[];
  lookingFor: string[];
  photos: PhotoRef[];
}

/** user-service PhotoService.UploadTicket */
export interface UploadTicket {
  photoId: string;
  uploadUrl: string;
  method: "PUT";
  headers: Record<string, string>;
  expiresAt: string;
}

/** Spring ProblemDetail (+ our extra properties) */
export interface Problem {
  title?: string;
  detail?: string;
  status?: number;
  code?: string;
  resetsAt?: string;
  errors?: Record<string, string>;
}
