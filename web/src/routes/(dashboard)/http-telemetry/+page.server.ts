import type { PageServerLoad } from "./$types";
import { getLogAnalytics, getLogStats } from "$lib/server/api";

export const load: PageServerLoad = async () => {
	let analytics = null;
	let stats = null;
	let analyticsError: string | null = null;
	let statsError: string | null = null;

	const analyticsRes = await getLogAnalytics();
	if (analyticsRes.ok) {
		analytics = analyticsRes.data;
	} else {
		analyticsError = analyticsRes.error.message || "Failed to load HTTP analytics";
	}

	const statsRes = await getLogStats();
	if (statsRes.ok) {
		stats = statsRes.data;
	} else {
		statsError = statsRes.error.message || "Failed to load log stats";
	}

	return {
		analytics,
		stats,
		analyticsError,
		statsError
	};
};
