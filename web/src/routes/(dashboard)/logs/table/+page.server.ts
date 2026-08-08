import { redirect } from "@sveltejs/kit";
import type { PageServerLoad } from "./$types";
import {
	queryLogs,
	getLogStats,
	type LogQueryParams
} from "$lib/server/api";

export const load: PageServerLoad = async ({ locals, url }) => {
	if (!locals.user) {
		throw redirect(303, "/login");
	}

	const search = url.searchParams.get("search") || undefined;
	const level = url.searchParams.get("level") || undefined;
	const serviceName = url.searchParams.get("serviceName") || undefined;
	const category = url.searchParams.get("category") || undefined;
	const traceId = url.searchParams.get("traceId") || undefined;
	const page = parseInt(url.searchParams.get("page") || "0", 10);
	const size = parseInt(url.searchParams.get("size") || "20", 10);

	const queryParams: LogQueryParams = {
		search,
		level: level && level !== "ALL" ? level : undefined,
		serviceName: serviceName && serviceName !== "ALL" ? serviceName : undefined,
		category: category && category !== "ALL" ? category : undefined,
		traceId: traceId || undefined,
		page: isNaN(page) || page < 0 ? 0 : page,
		size: isNaN(size) || size < 1 || size > 100 ? 20 : size
	};

	const [logsRes, statsRes] = await Promise.all([
		queryLogs(queryParams),
		getLogStats()
	]);

	return {
		logs: logsRes.ok ? logsRes.data : null,
		logsError: !logsRes.ok ? logsRes.error : null,
		stats: statsRes.ok ? statsRes.data : null,
		statsError: !statsRes.ok ? statsRes.error : null,
		queryParams,
		user: locals.user
	};
};
