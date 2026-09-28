import { apiFetchAuth, apiFetchAuthRaw, parseApiError, type ApiResult } from "./client";

// ── Types ───────────────────────────────────────────────────────────────

export type RuleCategory =
    | "COMPLETENESS"
    | "VALIDITY"
    | "CONSISTENCY"
    | "UNIQUENESS"
    | "TIMELINESS"
    | "ACCURACY"
    | "BUSINESS_CONTEXT";

export type RuleSeverity = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW" | "INFO";

export interface QualityRule {
    id: string;
    name: string;
    description?: string;
    category: RuleCategory;
    severity: RuleSeverity;
    expectation: string;
    enabled: boolean;
    target: string;
    conditionExpression?: string;
    lastExecuted?: string;
    datasetId: string;
    datasetName?: string;
}

export interface CreateQualityRulePayload {
    name: string;
    description?: string;
    category: RuleCategory;
    severity?: RuleSeverity;
    expectation: string;
    enabled?: boolean;
    target: string;
    conditionExpression?: string;
    datasetId: string;
}

export interface UpdateQualityRulePayload {
    name?: string;
    description?: string;
    category?: RuleCategory;
    severity?: RuleSeverity;
    expectation?: string;
    enabled?: boolean;
    target?: string;
    conditionExpression?: string;
}

// ── Functions ───────────────────────────────────────────────────────────

/**
 * Get all quality rules (optionally filtered by dataset, category, severity, or enabled state).
 * GET /api/v1/rules
 */
export async function getQualityRules(
    token: string,
    filter?: { datasetId?: string; category?: string; severity?: string; enabled?: boolean }
): Promise<QualityRule[]> {
    try {
        const queryParams = new URLSearchParams();
        if (filter?.datasetId) queryParams.set("datasetId", filter.datasetId);
        if (filter?.category && filter.category !== "ALL") queryParams.set("category", filter.category);
        if (filter?.severity && filter.severity !== "ALL") queryParams.set("severity", filter.severity);
        if (filter?.enabled !== undefined) queryParams.set("enabled", String(filter.enabled));

        const queryString = queryParams.toString();
        const endpoint = `/api/v1/rules${queryString ? `?${queryString}` : ""}`;

        const res = await apiFetchAuthRaw(endpoint, token);
        if (!res.ok) return [];
        return await res.json();
    } catch {
        return [];
    }
}

/**
 * Get a single quality rule by ID.
 * GET /api/v1/rules/{id}
 */
export async function getQualityRuleById(
    token: string,
    id: string
): Promise<QualityRule | null> {
    try {
        const res = await apiFetchAuthRaw(`/api/v1/rules/${id}`, token);
        if (!res.ok) return null;
        return await res.json();
    } catch {
        return null;
    }
}

/**
 * Get all quality rules for a specific dataset.
 * Queries GET /api/v1/rules?datasetId={datasetId}
 */
export async function getQualityRulesByDataset(
    token: string,
    datasetId: string
): Promise<QualityRule[]> {
    return getQualityRules(token, { datasetId });
}

/**
 * Create a new quality rule.
 * POST /api/v1/rules
 */
export async function createQualityRule(
    token: string,
    data: CreateQualityRulePayload
): Promise<ApiResult<QualityRule>> {
    return apiFetchAuth<QualityRule>("/api/v1/rules", token, {
        method: "POST",
        body: JSON.stringify(data),
    });
}

/**
 * Update an existing quality rule.
 * PUT /api/v1/rules/{id}
 */
export async function updateQualityRule(
    token: string,
    id: string,
    data: UpdateQualityRulePayload
): Promise<ApiResult<QualityRule>> {
    return apiFetchAuth<QualityRule>(`/api/v1/rules/${id}`, token, {
        method: "PUT",
        body: JSON.stringify(data),
    });
}

/**
 * Toggle the enabled/disabled status of a quality rule.
 * PATCH /api/v1/rules/{id}/toggle
 */
export async function toggleQualityRule(
    token: string,
    id: string
): Promise<ApiResult<QualityRule>> {
    return apiFetchAuth<QualityRule>(`/api/v1/rules/${id}/toggle`, token, {
        method: "PATCH",
    });
}

/**
 * Delete a quality rule.
 * DELETE /api/v1/rules/{id}
 */
export async function deleteQualityRule(
    token: string,
    id: string
): Promise<ApiResult<void>> {
    try {
        const res = await apiFetchAuthRaw(`/api/v1/rules/${id}`, token, {
            method: "DELETE",
        });

        if (!res.ok && res.status !== 204) {
            const body = await res.json().catch(() => ({ message: "Failed to parse response" }));
            return {
                ok: false as const,
                status: res.status,
                error: parseApiError(res.status, `/api/v1/rules/${id}`, body, `Failed to delete rule (${res.status})`),
            };
        }

        return { ok: true as const, data: undefined as any };
    } catch (err: any) {
        return {
            ok: false as const,
            status: 500,
            error: {
                timestamp: new Date().toISOString(),
                status: 500,
                error: "Network Error",
                code: "NETWORK_ERROR",
                message: err.message || "Failed to communicate with backend server",
                path: `/api/v1/rules/${id}`,
                module: "RULES",
                details: null,
            },
        };
    }
}
