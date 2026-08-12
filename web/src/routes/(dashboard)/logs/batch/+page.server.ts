import { redirect } from "@sveltejs/kit";
import type { PageServerLoad } from "./$types";
import { getBatchLogHistory } from "$lib/server/api/logs";

export const load: PageServerLoad = async ({ locals }) => {
	if (!locals.user) {
		throw redirect(303, "/login");
	}

	const batchHistoryRes = await getBatchLogHistory(50);

	return {
		batchHistory: batchHistoryRes.ok ? batchHistoryRes.data : [],
		batchHistoryError: !batchHistoryRes.ok ? batchHistoryRes.error : null,
		user: locals.user
	};
};
