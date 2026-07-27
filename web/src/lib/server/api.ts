import { env } from "$env/dynamic/private";

const BACKEND_API_URL = env.BACKEND_API_URL || "http://localhost:7000";

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

export interface ApiError {
	status: number;
	message: string;
}

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
}): Promise<{ ok: true; data: AuthResponse } | { ok: false; status: number; error: string }> {
	try {
		const res = await fetch(`${BACKEND_API_URL}/api/v1/auth/register`, {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify({
				...data,
				role: data.role || "USER"
			})
		});

		const body = await res.json().catch(() => ({ message: "Failed to parse response" }));

		if (!res.ok) {
			return {
				ok: false,
				status: res.status,
				error: body.message || `Registration failed with status ${res.status}`
			};
		}

		return { ok: true, data: body };
	} catch (err: any) {
		return {
			ok: false,
			status: 500,
			error: err.message || "Network error connecting to authentication server"
		};
	}
}

/**
 * Log in an existing user
 * POST /api/v1/auth/login
 */
export async function loginUser(data: {
	login: string;
	password: string;
}): Promise<{ ok: true; data: AuthResponse } | { ok: false; status: number; error: string }> {
	try {
		const res = await fetch(`${BACKEND_API_URL}/api/v1/auth/login`, {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify(data)
		});

		const body = await res.json().catch(() => ({ message: "Failed to parse response" }));

		if (!res.ok) {
			return {
				ok: false,
				status: res.status,
				error: body.message || "Invalid credentials"
			};
		}

		return { ok: true, data: body };
	} catch (err: any) {
		return {
			ok: false,
			status: 500,
			error: err.message || "Network error connecting to authentication server"
		};
	}
}

/**
 * Verify JWT token
 * POST /api/v1/auth/verify
 */
export async function verifyToken(token: string): Promise<boolean> {
	try {
		const res = await fetch(`${BACKEND_API_URL}/api/v1/auth/verify`, {
			method: "POST",
			headers: { "Content-Type": "text/plain" },
			body: token
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
		const res = await fetch(`${BACKEND_API_URL}/api/v1/auth/me`, {
			method: "GET",
			headers: {
				Authorization: `Bearer ${token}`
			}
		});

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
): Promise<{ ok: true; data: UserProfile } | { ok: false; status: number; error: string }> {
	try {
		const res = await fetch(`${BACKEND_API_URL}/api/v1/users/${userId}`, {
			method: "PUT",
			headers: {
				"Content-Type": "application/json",
				Authorization: `Bearer ${token}`
			},
			body: JSON.stringify(data)
		});

		const body = await res.json().catch(() => ({ message: "Failed to parse response" }));

		if (!res.ok) {
			return {
				ok: false,
				status: res.status,
				error: body.message || `Update failed with status ${res.status}`
			};
		}

		return { ok: true, data: body };
	} catch (err: any) {
		return {
			ok: false,
			status: 500,
			error: err.message || "Network error updating profile"
		};
	}
}
