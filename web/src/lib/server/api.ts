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

export type DatasourceStatus = "REGISTERED" | "ACTIVE" | "DISABLED" | "ARCHIVED";

export interface Dataset {
	id: string;
	name: string;
	description?: string;
	rowCount?: number;
	createdAt?: string;
}

export interface Datasource {
	id: string;
	name: string;
	type: string;
	description?: string;
	status: DatasourceStatus;
	owner: string;
	registrationDate: string;
	datasets?: Dataset[];
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

/* ==========================================================================
   Datasource API Helper Functions (/api/v1/datasources)
   ========================================================================== */

/**
 * List all datasources (or filtered by status / owner)
 * GET /api/v1/datasources
 * GET /api/v1/datasources/by-status/{status}
 * GET /api/v1/datasources/by-owner/{owner}
 */
export async function getDatasources(
	token: string,
	filter?: { status?: string; owner?: string }
): Promise<Datasource[]> {
	try {
		let endpoint = `${BACKEND_API_URL}/api/v1/datasources`;
		if (filter?.status && filter.status !== "ALL") {
			endpoint = `${BACKEND_API_URL}/api/v1/datasources/by-status/${encodeURIComponent(filter.status)}`;
		} else if (filter?.owner) {
			endpoint = `${BACKEND_API_URL}/api/v1/datasources/by-owner/${encodeURIComponent(filter.owner)}`;
		}

		const res = await fetch(endpoint, {
			method: "GET",
			headers: {
				Authorization: `Bearer ${token}`
			}
		});

		if (!res.ok) return [];
		return await res.json();
	} catch {
		return [];
	}
}

/**
 * Get datasource by ID
 * GET /api/v1/datasources/{id}
 */
export async function getDatasourceById(
	token: string,
	id: string
): Promise<Datasource | null> {
	try {
		const res = await fetch(`${BACKEND_API_URL}/api/v1/datasources/${id}`, {
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
 * Create a new datasource
 * POST /api/v1/datasources
 */
export async function createDatasource(
	token: string,
	data: { name: string; type: string; description?: string; owner: string }
): Promise<{ ok: true; data: Datasource } | { ok: false; status: number; error: string }> {
	try {
		const res = await fetch(`${BACKEND_API_URL}/api/v1/datasources`, {
			method: "POST",
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
				error: body.message || `Creation failed with status ${res.status}`
			};
		}

		return { ok: true, data: body };
	} catch (err: any) {
		return {
			ok: false,
			status: 500,
			error: err.message || "Network error creating datasource"
		};
	}
}

/**
 * Update an existing datasource
 * PUT /api/v1/datasources/{id}
 */
export async function updateDatasource(
	token: string,
	id: string,
	data: { name?: string; type?: string; description?: string; status?: DatasourceStatus }
): Promise<{ ok: true; data: Datasource } | { ok: false; status: number; error: string }> {
	try {
		const res = await fetch(`${BACKEND_API_URL}/api/v1/datasources/${id}`, {
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
			error: err.message || "Network error updating datasource"
		};
	}
}

/**
 * Delete a datasource
 * DELETE /api/v1/datasources/{id}
 */
export async function deleteDatasource(
	token: string,
	id: string
): Promise<{ ok: true } | { ok: false; status: number; error: string }> {
	try {
		const res = await fetch(`${BACKEND_API_URL}/api/v1/datasources/${id}`, {
			method: "DELETE",
			headers: {
				Authorization: `Bearer ${token}`
			}
		});

		if (!res.ok && res.status !== 204) {
			const body = await res.json().catch(() => ({ message: "Failed to parse response" }));
			return {
				ok: false,
				status: res.status,
				error: body.message || `Deletion failed with status ${res.status}`
			};
		}

		return { ok: true };
	} catch (err: any) {
		return {
			ok: false,
			status: 500,
			error: err.message || "Network error deleting datasource"
		};
	}
}

/**
 * Change datasource status: activate, disable, or archive
 * PATCH /api/v1/datasources/{id}/{activate|disable|archive}
 */
export async function changeDatasourceStatus(
	token: string,
	id: string,
	action: "activate" | "disable" | "archive"
): Promise<{ ok: true; data: Datasource } | { ok: false; status: number; error: string }> {
	try {
		const res = await fetch(`${BACKEND_API_URL}/api/v1/datasources/${id}/${action}`, {
			method: "PATCH",
			headers: {
				Authorization: `Bearer ${token}`
			}
		});

		const body = await res.json().catch(() => ({ message: "Failed to parse response" }));

		if (!res.ok) {
			return {
				ok: false,
				status: res.status,
				error: body.message || `Status change (${action}) failed with status ${res.status}`
			};
		}

		return { ok: true, data: body };
	} catch (err: any) {
		return {
			ok: false,
			status: 500,
			error: err.message || `Network error during ${action} action`
		};
	}
}
