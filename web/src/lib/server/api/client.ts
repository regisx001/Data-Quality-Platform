import { env } from "$env/dynamic/private";

export const BACKEND_API_URL = env.BACKEND_API_URL || "http://localhost:7000";

export interface ApiError {
    status: number;
    message: string;
}

export type ApiResult<T> =
    | { ok: true; data: T }
    | { ok: false; status: number; error: string };

/**
 * Generic fetch wrapper with JSON parsing and error handling.
 */
export async function apiFetch<T>(
    path: string,
    options: RequestInit = {}
): Promise<ApiResult<T>> {
    try {
        const res = await fetch(`${BACKEND_API_URL}${path}`, {
            ...options,
            headers: {
                "Content-Type": "application/json",
                ...options.headers,
            },
        });

        const text = await res.text();
        let body: any = {};
        if (text && text.trim()) {
            try {
                body = JSON.parse(text);
            } catch {
                body = { message: text };
            }
        }

        if (!res.ok) {
            return {
                ok: false,
                status: res.status,
                error: body.message || `Request failed with status ${res.status}`,
            };
        }

        return { ok: true, data: body };
    } catch (err: any) {
        return {
            ok: false,
            status: 500,
            error: err.message || "Network error",
        };
    }
}

/**
 * Generic fetch wrapper for authenticated requests.
 */
export async function apiFetchAuth<T>(
    path: string,
    token: string,
    options: RequestInit = {}
): Promise<ApiResult<T>> {
    return apiFetch<T>(path, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
            ...options.headers,
        },
    });
}

/**
 * Fetch with auth and no JSON body (for plain text or empty responses).
 */
export async function apiFetchAuthRaw(
    path: string,
    token: string,
    options: RequestInit = {}
): Promise<Response> {
    return fetch(`${BACKEND_API_URL}${path}`, {
        ...options,
        headers: {
            Authorization: `Bearer ${token}`,
            ...options.headers,
        },
    });
}
