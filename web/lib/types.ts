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
  veiled: boolean; // photos are blurred copies until you match
  mutualDesires: string[]; // desires you both chose (shown in After dark)
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
  superLikeLimit: number;
  superLikesRemaining: number;
  plus: boolean;
}

export interface Desires {
  into: string[];
  openTo: string[];
  notInto: string[];
}

export interface Plan {
  id: string;
  name: string;
  amountPaise: number;
  currency: string;
  days: number;
}

export interface PlusStatus {
  plus: boolean;
  activeUntil: string | null;
  planId: string | null;
  provider: "mock" | "razorpay";
}

export interface Checkout {
  orderId: string;
  provider: "mock" | "razorpay";
  amountPaise: number;
  currency: string;
  planName: string;
  checkout: { keyId?: string };
}

export interface Likes {
  count: number;
  plus: boolean;
  likes: { userId: string; superLike: boolean; likedAt: string }[];
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
  veiled?: boolean;
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
  privateMode: boolean;
  veiledPhotos: boolean;
  ageMin: number | null;
  ageMax: number | null;
  maxDistanceKm: number | null;
}

export interface PrivacySettings {
  privateMode: boolean;
  veiledPhotos: boolean;
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
  plus?: boolean;
  errors?: Record<string, string>;
}
