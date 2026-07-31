import { apiFetch, apiFetchAuth, apiFetchAuthRaw, type ApiResult } from "./client";

// ── Types ───────────────────────────────────────────────────────────────

export interface UserProfile {
    userId: string;
    username: string;
    email: string;
    fullName: string;
    role: string;
    token?: string | null;
    expiresIn?: number;
    active?: boolean;
    verified?: boolean;
    createdAt?: string;
    lastLoginAt?: string | null;
}

export interface AuthResponse {
    token: string;
    expiresIn: number;
    userId: string;
    username: string;
    email: string;
    fullName: string;
    role: string;
}

// ── Functions ───────────────────────────────────────────────────────────

/**
 * Register a new user
 * POST /api/v1/auth/register
 */
export async function registerUser(data: {
    username: string;
    email: string;
    password: string;
    fullName: string;
    role?: string;
}): Promise<ApiResult<AuthResponse>> {
    return apiFetch("/api/v1/auth/register", {
        method: "POST",
        body: JSON.stringify({ ...data, role: data.role || "USER" }),
    });
}

/**
 * Log in an existing user
 * POST /api/v1/auth/login
 */
export async function loginUser(data: {
    login: string;
    password: string;
}): Promise<ApiResult<AuthResponse>> {
    return apiFetch("/api/v1/auth/login", {
        method: "POST",
        body: JSON.stringify(data),
    });
}

/**
 * Verify JWT token
 * POST /api/v1/auth/verify
 */
export async function verifyToken(token: string): Promise<boolean> {
    try {
        const res = await apiFetchAuthRaw("/api/v1/auth/verify", token, {
            method: "POST",
            headers: { "Content-Type": "text/plain" },
            body: token,
        });
        return res.ok;
    } catch {
        return false;
    }
}

/**
 * Get current user profile
 * GET /api/v1/auth/me
 */
export async function getCurrentUser(token: string): Promise<UserProfile | null> {
    try {
        const res = await apiFetchAuthRaw("/api/v1/auth/me", token);
        if (!res.ok) return null;
        return await res.json();
    } catch {
        return null;
    }
}

/**
 * Update user profile
 * PUT /api/v1/users/{id}
 */
export async function updateUserProfile(
    token: string,
    userId: string,
    data: { email?: string; fullName?: string; role?: string }
): Promise<ApiResult<UserProfile>> {
    return apiFetchAuth(`/api/v1/users/${userId}`, token, {
        method: "PUT",
        body: JSON.stringify(data),
    });
}
