import { redirect } from "@sveltejs/kit";
import type { PageServerLoad } from "./$types";
import {
	getLogStats,
	getLogAnalytics,
	type AnalyticsQueryParams
} from "$lib/server/api";

export const load: PageServerLoad = async ({ locals, url }) => {
	if (!locals.user) {
		throw redirect(303, "/login");
	}

	const granularity = url.searchParams.get("granularity") || "PT1H";
	const serviceName = url.searchParams.get("serviceName") || undefined;
	const category = url.searchParams.get("category") || undefined;
	const traceId = url.searchParams.get("traceId") || undefined;

	const analyticsParams: AnalyticsQueryParams = {
		granularity,
		serviceName: serviceName && serviceName !== "ALL" ? serviceName : undefined,
		category: category && category !== "ALL" ? category : undefined,
		traceId: traceId || undefined
	};

	const [statsRes, analyticsRes] = await Promise.all([
		getLogStats(),
		getLogAnalytics(analyticsParams)
	]);

	return {
		stats: statsRes.ok ? statsRes.data : null,
		statsError: !statsRes.ok ? statsRes.error : null,
		analytics: analyticsRes.ok ? analyticsRes.data : null,
		analyticsError: !analyticsRes.ok ? analyticsRes.error : null,
		queryParams: {
			granularity,
			serviceName,
			category,
			traceId
		},
		user: locals.user
	};
};
