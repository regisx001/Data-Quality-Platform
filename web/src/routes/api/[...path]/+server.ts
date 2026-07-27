import { env } from "$env/dynamic/private";
import type { RequestEvent } from "@sveltejs/kit";

const BACKEND_API_URL = env.BACKEND_API_URL || "http://localhost:7000";

/**
 * Proxy all API requests to the backend server.
 * This ensures API calls are made from the same origin, avoiding CORS issues
 * in both development and production environments.
 */
async function handleRequest(event: RequestEvent) {
    const { request, params } = event;
    const path = params.path || "";
    const url = `${BACKEND_API_URL}/api/${path}${event.url.search}`;

    // Forward headers, but remove host-based headers to let the backend handle them
    const headers = new Headers(request.headers);
    headers.delete("host");

    // Forward the request body for methods that support it
    const init: RequestInit = {
        method: request.method,
        headers
    };

    // Include body for applicable methods
    if (!["GET", "HEAD"].includes(request.method)) {
        init.body = request.body;
        // @ts-expect-error - duplex is required for streaming body but not in all TS types
        init.duplex = "half";
    }

    const res = await fetch(url, init);

    // Forward the response status and headers
    const responseHeaders = new Headers(res.headers);
    // Remove content-encoding and content-length since we're re-streaming
    responseHeaders.delete("content-encoding");
    responseHeaders.delete("content-length");

    return new Response(res.body, {
        status: res.status,
        statusText: res.statusText,
        headers: responseHeaders
    });
}

export const GET = handleRequest;
export const POST = handleRequest;
export const PUT = handleRequest;
export const PATCH = handleRequest;
export const DELETE = handleRequest;
export const OPTIONS = handleRequest;
