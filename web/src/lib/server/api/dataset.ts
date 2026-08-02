import { apiFetchAuthRaw } from "./client";

export interface ColumnDetail {
    id: string;
    name: string;
    dataType: string;
    isNullable: boolean;
    isPrimaryKey: boolean;
    nullCount?: number;
    nullPercentage?: number;
    distinctCount?: number;
    minValue?: string;
    maxValue?: string;
    avgValue?: number;
    profiledAt?: string;
}

export interface DatasetDetail {
    id: string;
    name: string;
    description?: string;
    type: string;
    status: string;
    rowCount?: number;
    lastDiscovered?: string;
    lastValidated?: string;
    domain?: string;
    tags?: string;
    datasourceId: string;
    datasourceName: string;
    datasourceType: string;
    columns: ColumnDetail[];
}

export interface DataPreviewResult {
    columns: string[];
    rows: Record<string, any>[];
    totalRows: number;
}

/**
 * Get detailed dataset metadata & column statistics.
 * GET /api/v1/datasets/{id}
 */
export async function getDatasetById(
    token: string,
    id: string
): Promise<DatasetDetail | null> {
    try {
        const res = await apiFetchAuthRaw(`/api/v1/datasets/${id}`, token);
        if (!res.ok) return null;
        const text = await res.text();
        return (text && text.trim()) ? JSON.parse(text) : null;
    } catch {
        return null;
    }
}

/**
 * Fetch sample preview rows for a dataset.
 * GET /api/v1/datasets/{id}/preview?limit=50
 */
export async function getDatasetPreview(
    token: string,
    id: string,
    limit: number = 50
): Promise<DataPreviewResult | null> {
    try {
        const res = await apiFetchAuthRaw(`/api/v1/datasets/${id}/preview?limit=${limit}`, token);
        if (!res.ok) return null;
        const text = await res.text();
        return (text && text.trim()) ? JSON.parse(text) : null;
    } catch {
        return null;
    }
}

/**
 * Trigger column profiling for a dataset.
 * POST /api/v1/datasets/{id}/profile
 */
export async function profileDataset(
    token: string,
    id: string
): Promise<{ ok: boolean; data?: DatasetDetail; error?: string }> {
    try {
        const res = await apiFetchAuthRaw(`/api/v1/datasets/${id}/profile`, token, {
            method: "POST",
        });
        const text = await res.text();
        let body: any = {};
        if (text && text.trim()) {
            try {
                body = JSON.parse(text);
            } catch {
                body = {};
            }
        }
        if (!res.ok) {
            return { ok: false, error: body.message || `Request failed with status ${res.status}` };
        }
        return { ok: true, data: body };
    } catch (err: any) {
        return { ok: false, error: err.message || "Network error profiling dataset" };
    }
}

/**
 * Delete a dataset entity.
 * DELETE /api/v1/datasets/{id}
 */
export async function deleteDataset(
    token: string,
    id: string
): Promise<{ ok: boolean; error?: string }> {
    try {
        const res = await apiFetchAuthRaw(`/api/v1/datasets/${id}`, token, {
            method: "DELETE",
        });
        if (!res.ok) {
            const text = await res.text();
            let body: any = {};
            if (text && text.trim()) {
                try { body = JSON.parse(text); } catch { body = {}; }
            }
            return { ok: false, error: body.message || "Failed to delete dataset" };
        }
        return { ok: true };
    } catch (err: any) {
        return { ok: false, error: err.message || "Network error deleting dataset" };
    }
}
