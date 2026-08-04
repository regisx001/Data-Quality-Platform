import { env } from "$env/dynamic/private";

export const BACKEND_API_URL = env.BACKEND_API_URL || "http://localhost:7000";

export interface ApiError {
    timestamp?: string;
    status: number;
    error?: string;
    code?: string;
    message: string;
    path?: string;
    module?: string;
    details?: any;
}

export type ApiResult<T> =
    | { ok: true; data: T }
    | { ok: false; status: number; error: ApiError };

export function parseApiError(resStatus: number, path: string, body: any, fallbackMessage?: string): ApiError {
    if (typeof body === "object" && body !== null) {
        return {
            timestamp: body.timestamp || new Date().toISOString(),
            status: body.status || resStatus,
            error: body.error || "Error",
            code: body.code || (resStatus === 404 ? "NOT_FOUND" : resStatus === 401 ? "UNAUTHORIZED" : "ERROR"),
            message: body.message || fallbackMessage || `Request failed with status ${resStatus}`,
            path: body.path || path,
            module: body.module || "SYSTEM",
            details: body.details || null
        };
    }
    return {
        timestamp: new Date().toISOString(),
        status: resStatus,
        error: "Error",
        code: "ERROR",
        message: typeof body === "string" && body.trim() ? body : (fallbackMessage || `Request failed with status ${resStatus}`),
        path,
        module: "SYSTEM",
        details: null
    };
}

/**
 * Generic fetch wrapper with JSON parsing and structured error handling.
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
                error: parseApiError(res.status, path, body),
            };
        }

        return { ok: true, data: body };
    } catch (err: any) {
        return {
            ok: false,
            status: 500,
            error: {
                timestamp: new Date().toISOString(),
                status: 500,
                error: "Network Error",
                code: "NETWORK_ERROR",
                message: err.message || "Failed to communicate with backend server",
                path,
                module: "NETWORK",
                details: null
            },
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
