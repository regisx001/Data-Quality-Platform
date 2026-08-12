import { env } from "$env/dynamic/private";
import { parseApiError, type ApiResult } from "./client";

export const LOGS_API_URL = env.LOGS_API_URL || "http://localhost:7001";

/**
 * Log entry model matching dQul-logs schema
 */
export interface LogEntry {
	id: string;
	traceId: string | null;
	serviceName: string;
	logLevel: "TRACE" | "DEBUG" | "INFO" | "WARN" | "ERROR" | "FATAL";
	category: string;
	message: string;
	stackTrace: string | null;
	path: string | null;
	httpMethod: string | null;
	statusCode: number | null;
	executionTimeMs: number | null;
	userId: string | null;
	userEmail: string | null;
	metadata: string | null;
	timestamp: string;
}

/**
 * Paginated response envelope for log queries
 */
export interface LogPageResponse {
	content: LogEntry[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
	first: boolean;
	last: boolean;
	hasNext: boolean;
	hasPrevious: boolean;
}

/**
 * Query filter parameters for listing logs
 */
export interface LogQueryParams {
	search?: string;
	level?: string;
	serviceName?: string;
	category?: string;
	traceId?: string;
	page?: number;
	size?: number;
}

/**
 * Summary statistics model for /api/v1/logs/stats
 */
export interface LogStats {
	totalLogs: number;
	errorCount: number;
	warnCount: number;
	infoCount: number;
	errorRatePercentage: number;
	averageLatencyMs: number;
	logsByService: Record<string, number>;
	logsByCategory: Record<string, number>;
}

/**
 * Detailed analytics models for /api/v1/logs/analytics
 */
export interface TimeSeriesBucket {
	bucket: string;
	count: number;
}

export interface LevelDistribution {
	level: string;
	count: number;
	percentage: number;
}

export interface ServiceMetrics {
	serviceName: string;
	totalLogs: number;
	logSharePercentage: number;
	errorCount: number;
	fatalCount: number;
	warnCount: number;
	errorRatePercentage: number;
	averageLatencyMs: number;
	p95LatencyMs: number;
	p99LatencyMs: number;
	maxLatencyMs: number;
}

export interface CategoryMetrics {
	category: string;
	totalLogs: number;
	logSharePercentage: number;
	errorCount: number;
	fatalCount: number;
	errorRatePercentage: number;
	averageLatencyMs: number;
	p95LatencyMs: number;
	p99LatencyMs: number;
}

export interface EndpointMetrics {
	httpMethod: string;
	path: string;
	requestCount: number;
	errorCount: number;
	errorRatePercentage: number;
	averageLatencyMs: number;
	p95LatencyMs: number;
	p99LatencyMs: number;
	maxLatencyMs: number;
}

export interface HttpMetrics {
	totalRequests: number;
	count2xx: number;
	count3xx: number;
	count4xx: number;
	count5xx: number;
	rate2xx: number;
	rate3xx: number;
	rate4xx: number;
	rate5xx: number;
	statusCounts: Record<string, number>;
	methodCounts: Record<string, number>;
	endpoints: EndpointMetrics[];
}

export interface LatencyMetrics {
	sampleCount: number;
	averageMs: number;
	medianMs: number;
	minMs: number;
	maxMs: number;
	p50Ms: number;
	p75Ms: number;
	p90Ms: number;
	p95Ms: number;
	p99Ms: number;
	p999Ms: number;
	standardDeviationMs: number;
	varianceMs2: number;
	slowRequestCount: number;
	slowRequestPercentage: number;
}

export interface ErrorSignature {
	signature: string;
	count: number;
	percentage: number;
	firstOccurrenceEpochMillis: number;
	lastOccurrenceEpochMillis: number;
	exampleMessage: string;
	exampleStackTrace: string | null;
}

export interface TraceMetrics {
	uniqueTraces: number;
	averageLogsPerTrace: number;
	averageDurationMs: number;
	medianDurationMs: number;
	p95DurationMs: number;
	p99DurationMs: number;
	failedTraces: number;
	traceErrorRatePercentage: number;
}

export interface UserMetrics {
	userId: string;
	userEmail: string | null;
	totalLogs: number;
	errorCount: number;
	errorRatePercentage: number;
	averageLatencyMs: number;
	p95LatencyMs: number;
}

export interface LogAnalytics {
	from: string;
	to: string;
	totalLogs: number;
	volume: {
		logsPerSecond: number;
		logsPerMinute: number;
		maxLogsInBucket: number;
		minLogsInBucket: number;
		timeSeries: TimeSeriesBucket[];
	};
	levels: {
		infoCount: number;
		debugCount: number;
		traceCount: number;
		warnCount: number;
		errorCount: number;
		fatalCount: number;
		infoRatio: number;
		debugRatio: number;
		traceRatio: number;
		warnRatio: number;
		errorRatio: number;
		fatalRatio: number;
		errorRatePercentage: number;
		warnPlusRatePercentage: number;
		distribution: LevelDistribution[];
	};
	services: ServiceMetrics[];
	categories: CategoryMetrics[];
	http: HttpMetrics;
	latency: LatencyMetrics;
	errorSignatures: ErrorSignature[];
	traces: TraceMetrics;
	users: Record<string, UserMetrics>;
}

export interface AnalyticsQueryParams {
	from?: string;
	to?: string;
	granularity?: string;
	serviceName?: string;
	category?: string;
	traceId?: string;
}

export interface PurgeResult {
	status: string;
	message: string;
}

/**
 * Fetch helper pointing directly to LOGS_API_URL
 */
async function logsApiFetch<T>(path: string, options: RequestInit = {}): Promise<ApiResult<T>> {
	try {
		const res = await fetch(`${LOGS_API_URL}${path}`, {
			...options,
			headers: {
				Accept: "application/json",
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
				message: err.message || "Failed to communicate with logs service",
				path,
				module: "LOGS",
				details: null,
			},
		};
	}
}

/**
 * GET /api/v1/logs
 * Query logs with filters and pagination
 */
export async function queryLogs(params: LogQueryParams = {}): Promise<ApiResult<LogPageResponse>> {
	const query = new URLSearchParams();
	if (params.search) query.append("search", params.search);
	if (params.level) query.append("level", params.level);
	if (params.serviceName) query.append("serviceName", params.serviceName);
	if (params.category) query.append("category", params.category);
	if (params.traceId) query.append("traceId", params.traceId);
	if (params.page !== undefined) query.append("page", params.page.toString());
	if (params.size !== undefined) query.append("size", params.size.toString());

	const queryString = query.toString();
	const path = `/api/v1/logs${queryString ? `?${queryString}` : ""}`;
	return logsApiFetch<LogPageResponse>(path, { method: "GET" });
}

/**
 * GET /api/v1/logs/{id}
 * Retrieve single log entry by UUID
 */
export async function getLogById(id: string): Promise<ApiResult<LogEntry>> {
	return logsApiFetch<LogEntry>(`/api/v1/logs/${encodeURIComponent(id)}`, { method: "GET" });
}

/**
 * GET /api/v1/logs/stats
 * Retrieve aggregated high-level log stats
 */
export async function getLogStats(): Promise<ApiResult<LogStats>> {
	return logsApiFetch<LogStats>("/api/v1/logs/stats", { method: "GET" });
}

/**
 * GET /api/v1/logs/analytics
 * Retrieve rich multi-dimensional analytics model
 */
export async function getLogAnalytics(params: AnalyticsQueryParams = {}): Promise<ApiResult<LogAnalytics>> {
	const query = new URLSearchParams();
	if (params.from) query.append("from", params.from);
	if (params.to) query.append("to", params.to);
	if (params.granularity) query.append("granularity", params.granularity);
	if (params.serviceName) query.append("serviceName", params.serviceName);
	if (params.category) query.append("category", params.category);
	if (params.traceId) query.append("traceId", params.traceId);

	const queryString = query.toString();
	const path = `/api/v1/logs/analytics${queryString ? `?${queryString}` : ""}`;
	return logsApiFetch<LogAnalytics>(path, { method: "GET" });
}

/**
 * DELETE /api/v1/logs/purge?days={days}
 * Retention purge of log entries older than N days (1..365)
 */
export async function purgeLogs(days: number = 30): Promise<ApiResult<PurgeResult>> {
	return logsApiFetch<PurgeResult>(`/api/v1/logs/purge?days=${days}`, { method: "DELETE" });
}

export interface LogsAggregationResult {
	jobId: string;
	totalLogsCount: number;
	levelCounts?: Record<string, number>;
	serviceCounts?: Record<string, number>;
	categoryCounts?: Record<string, number>;
	avgExecutionTimeMs?: number;
	maxExecutionTimeMs?: number;
	topErrorMessages?: Array<{
		serviceName: string;
		category: string;
		message: string;
		count: number;
	}>;
	minTimestamp?: string;
	maxTimestamp?: string;
	aggregatedAt?: string;
}

export interface BatchLogMetric {
	id: string;
	jobId: string;
	status: string;
	fromTimestamp: string | null;
	toTimestamp: string | null;
	totalLogsCount: number;
	avgExecutionTimeMs: number | null;
	minioStoragePath: string | null;
	resultData?: LogsAggregationResult | null;
	createdAt: string;
}

/**
 * GET /api/v1/logs/batch/history?limit={limit}
 * Fetches recent historical batch analytics execution records
 */
export async function getBatchLogHistory(limit: number = 30): Promise<ApiResult<BatchLogMetric[]>> {
	return logsApiFetch<BatchLogMetric[]>(`/api/v1/logs/batch/history?limit=${limit}`, { method: "GET" });
}
