import { apiFetchAuth, apiFetchAuthRaw, type ApiResult } from "./client";

// ── Types ───────────────────────────────────────────────────────────────

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

export interface ConfigField {
    name: string;
    label: string;
    type: string;
    defaultValue: string | null;
    description: string;
    required: boolean;
    min: number | null;
    max: number | null;
    options: string[] | null;
}

export interface ConnectorConfigSchema {
    type: string;
    label: string;
    description: string;
    fields: ConfigField[];
}

export interface ConnectionTestResult {
    success: boolean;
    message: string;
    latencyMs: number;
}

export interface DatasetDescriptor {
    id: string;
    name: string;
    type: "TABLE" | "VIEW" | "FILE";
    description?: string;
    rowCount?: number;
}

// ── Functions ───────────────────────────────────────────────────────────

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
        let endpoint = "/api/v1/datasources";
        if (filter?.status && filter.status !== "ALL") {
            endpoint = `/api/v1/datasources/by-status/${encodeURIComponent(filter.status)}`;
        } else if (filter?.owner) {
            endpoint = `/api/v1/datasources/by-owner/${encodeURIComponent(filter.owner)}`;
        }

        const res = await apiFetchAuthRaw(endpoint, token);
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
        const res = await apiFetchAuthRaw(`/api/v1/datasources/${id}`, token);
        if (!res.ok) return null;
        return await res.json();
    } catch {
        return null;
    }
}

/**
 * Upload a CSV file to backend / MinIO storage
 * POST /api/v1/datasources/upload-csv
 */
export async function uploadCsvFile(
    token: string,
    formData: FormData
): Promise<ApiResult<{ filePath: string; objectName: string; bucket: string; fileName: string; fileSize: number; storedInMinio: boolean }>> {
    try {
        const path = "/api/v1/datasources/upload-csv";
        const res = await apiFetchAuthRaw(path, token, {
            method: "POST",
            body: formData,
        });
        const body = await res.json().catch(() => ({ message: "Failed to parse response" }));
        if (!res.ok) {
            return {
                ok: false,
                status: res.status,
                error: parseApiError(res.status, path, body, "CSV Upload failed"),
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
                message: err.message || "Network error during CSV file upload",
                path: "/api/v1/datasources/upload-csv",
                module: "STORAGE"
            },
        };
    }
}

/**
 * Create a new datasource
 * POST /api/v1/datasources
 */
export async function createDatasource(
    token: string,
    data: { name: string; type: string; description?: string; owner: string; configJson?: string }
): Promise<ApiResult<Datasource>> {
    return apiFetchAuth("/api/v1/datasources", token, {
        method: "POST",
        body: JSON.stringify(data),
    });
}

/**
 * Update an existing datasource
 * PUT /api/v1/datasources/{id}
 */
export async function updateDatasource(
    token: string,
    id: string,
    data: { name?: string; type?: string; description?: string; status?: DatasourceStatus }
): Promise<ApiResult<Datasource>> {
    return apiFetchAuth(`/api/v1/datasources/${id}`, token, {
        method: "PUT",
        body: JSON.stringify(data),
    });
}

/**
 * Delete a datasource
 * DELETE /api/v1/datasources/{id}
 */
export async function deleteDatasource(
    token: string,
    id: string
): Promise<ApiResult<void>> {
    try {
        const res = await apiFetchAuthRaw(`/api/v1/datasources/${id}`, token, {
            method: "DELETE",
        });

        if (!res.ok && res.status !== 204) {
            const body = await res.json().catch(() => ({ message: "Failed to parse response" }));
            return {
                ok: false as const,
                status: res.status,
                error: body.message || `Deletion failed with status ${res.status}`,
            };
        }

        return { ok: true as const, data: undefined as any };
    } catch (err: any) {
        return {
            ok: false as const,
            status: 500,
            error: err.message || "Network error deleting datasource",
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
): Promise<ApiResult<Datasource>> {
    return apiFetchAuth(`/api/v1/datasources/${id}/${action}`, token, {
        method: "PATCH",
    });
}

/**
 * Get all connector configuration schemas
 * GET /api/v1/datasources/config-schemas
 */
export async function getConfigSchemas(
    token: string
): Promise<ConnectorConfigSchema[]> {
    try {
        const res = await apiFetchAuthRaw("/api/v1/datasources/config-schemas", token);
        if (!res.ok) return [];
        return await res.json();
    } catch {
        return [];
    }
}

/**
 * Save datasource configuration (JSON)
 * PUT /api/v1/datasources/{id}/config
 */
export async function saveDatasourceConfig(
    token: string,
    id: string,
    configJson: string
): Promise<ApiResult<Datasource>> {
    return apiFetchAuth(`/api/v1/datasources/${id}/config`, token, {
        method: "PUT",
        body: JSON.stringify({ configJson }),
    });
}

/**
 * Get datasource configuration (JSON)
 * GET /api/v1/datasources/{id}/config
 */
export async function getDatasourceConfig(
    token: string,
    id: string
): Promise<string | null> {
    try {
        const res = await apiFetchAuthRaw(`/api/v1/datasources/${id}/config`, token);
        if (res.status === 204) return null;
        if (!res.ok) return null;

        const text = await res.text();
        if (!text || !text.trim()) return null;
        const body = JSON.parse(text);
        return body.configJson || null;
    } catch {
        return null;
    }
}

/**
 * Test connection for a datasource
 * POST /api/v1/datasources/{id}/test-connection
 */
export async function testDatasourceConnection(
    token: string,
    id: string
): Promise<ApiResult<ConnectionTestResult>> {
    return apiFetchAuth(`/api/v1/datasources/${id}/test-connection`, token, {
        method: "POST",
    });
}

/**
 * Discover datasets (tables/views/files) exposed by a datasource
 * GET /api/v1/datasources/{id}/discover-datasets
 */
export async function discoverDatasourceDatasets(
    token: string,
    id: string
): Promise<ApiResult<DatasetDescriptor[]>> {
    return apiFetchAuth(`/api/v1/datasources/${id}/discover-datasets`, token, {
        method: "GET",
    });
}

/**
 * Import selected discovered datasets into managed entities
 * POST /api/v1/datasources/{id}/import-datasets
 */
export async function importDatasourceDatasets(
    token: string,
    id: string,
    datasetIds: string[]
): Promise<ApiResult<Dataset[]>> {
    return apiFetchAuth(`/api/v1/datasources/${id}/import-datasets`, token, {
        method: "POST",
        body: JSON.stringify({ datasetIds }),
    });
}
