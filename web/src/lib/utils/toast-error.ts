import { toast } from "svelte-sonner";
import type { ApiError } from "$lib/server/api/client";

/**
 * Displays a styled Sonner toast notification for an ApiError object or error string.
 */
export function showErrorToast(error: ApiError | string | null | undefined, fallbackTitle = "Operation Failed") {
    if (!error) return;

    if (typeof error === "string") {
        toast.error(fallbackTitle, {
            description: error,
            duration: 5000,
        });
        return;
    }

    const moduleName = error.module ? `[${error.module}] ` : "";
    const codeStr = error.code ? ` (${error.code})` : "";
    const title = `${moduleName}${fallbackTitle}${codeStr}`;

    toast.error(title, {
        description: error.message,
        duration: 6000,
    });
}
