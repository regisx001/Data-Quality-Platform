import { fail, redirect } from "@sveltejs/kit";
import type { Actions, PageServerLoad } from "./$types";
import {
	getQualityRules,
	createQualityRule,
	updateQualityRule,
	toggleQualityRule,
	deleteQualityRule,
	getDatasources,
	type RuleCategory,
	type RuleSeverity,
} from "$lib/server/api";

export const load: PageServerLoad = async ({ locals, url }) => {
	if (!locals.user || !locals.token) {
		throw redirect(303, "/login");
	}

	const datasetFilter = url.searchParams.get("datasetId") || undefined;
	const categoryFilter = url.searchParams.get("category") || "ALL";
	const severityFilter = url.searchParams.get("severity") || "ALL";
	const enabledParam = url.searchParams.get("enabled");
	const enabledFilter = enabledParam !== null && enabledParam !== "ALL" ? enabledParam === "true" : undefined;

	const [rules, datasources] = await Promise.all([
		getQualityRules(locals.token, {
			datasetId: datasetFilter && datasetFilter !== "ALL" ? datasetFilter : undefined,
			category: categoryFilter !== "ALL" ? categoryFilter : undefined,
			severity: severityFilter !== "ALL" ? severityFilter : undefined,
			enabled: enabledFilter,
		}),
		getDatasources(locals.token),
	]);

	const availableDatasets = datasources.flatMap((ds) =>
		(ds.datasets || []).map((d) => ({
			id: d.id,
			name: d.name,
			datasourceName: ds.name,
			datasourceType: ds.type,
		}))
	);

	return {
		rules,
		availableDatasets,
		currentFilters: {
			datasetId: datasetFilter || "ALL",
			category: categoryFilter,
			severity: severityFilter,
			enabled: enabledParam || "ALL",
		},
		user: locals.user,
	};
};

export const actions: Actions = {
	createRule: async ({ request, locals }) => {
		if (!locals.user || !locals.token) {
			throw redirect(303, "/login");
		}

		const data = await request.formData();
		const name = data.get("name")?.toString().trim();
		const description = data.get("description")?.toString().trim() || undefined;
		const category = data.get("category")?.toString().trim() as RuleCategory;
		const severity = (data.get("severity")?.toString().trim() || "MEDIUM") as RuleSeverity;
		const expectation = data.get("expectation")?.toString().trim();
		const target = data.get("target")?.toString().trim();
		const conditionExpression = data.get("conditionExpression")?.toString().trim() || undefined;
		const datasetId = data.get("datasetId")?.toString().trim();
		const enabled = data.get("enabled") !== "false";

		if (!name || !category || !expectation || !target || !datasetId) {
			return fail(400, {
				error: "Name, Category, Expectation, Target, and Dataset are required.",
				action: "create",
			});
		}

		const result = await createQualityRule(locals.token, {
			name,
			description,
			category,
			severity,
			expectation,
			target,
			conditionExpression,
			datasetId,
			enabled,
		});

		if (!result.ok) {
			return fail(result.status || 400, {
				error: result.error,
				action: "create",
			});
		}

		return {
			success: true,
			message: `Quality rule '${result.data.name}' created successfully!`,
			action: "create",
		};
	},

	updateRule: async ({ request, locals }) => {
		if (!locals.user || !locals.token) {
			throw redirect(303, "/login");
		}

		const data = await request.formData();
		const id = data.get("id")?.toString().trim();
		const name = data.get("name")?.toString().trim() || undefined;
		const description = data.get("description")?.toString().trim() || undefined;
		const category = (data.get("category")?.toString().trim() || undefined) as RuleCategory | undefined;
		const severity = (data.get("severity")?.toString().trim() || undefined) as RuleSeverity | undefined;
		const expectation = data.get("expectation")?.toString().trim() || undefined;
		const target = data.get("target")?.toString().trim() || undefined;
		const conditionExpression = data.get("conditionExpression")?.toString().trim() || undefined;
		const enabled = data.has("enabled") ? data.get("enabled") === "true" : undefined;

		if (!id) {
			return fail(400, {
				error: "Rule ID is required for updating.",
				action: "update",
			});
		}

		const result = await updateQualityRule(locals.token, id, {
			name,
			description,
			category,
			severity,
			expectation,
			target,
			conditionExpression,
			enabled,
		});

		if (!result.ok) {
			return fail(result.status || 400, {
				error: result.error,
				action: "update",
			});
		}

		return {
			success: true,
			message: `Quality rule '${result.data.name}' updated successfully!`,
			action: "update",
		};
	},

	toggleRule: async ({ request, locals }) => {
		if (!locals.user || !locals.token) {
			throw redirect(303, "/login");
		}

		const data = await request.formData();
		const id = data.get("id")?.toString().trim();

		if (!id) {
			return fail(400, {
				error: "Rule ID is required.",
				action: "toggle",
			});
		}

		const result = await toggleQualityRule(locals.token, id);

		if (!result.ok) {
			return fail(result.status || 400, {
				error: result.error,
				action: "toggle",
			});
		}

		return {
			success: true,
			message: `Rule '${result.data.name}' is now ${result.data.enabled ? "enabled" : "disabled"}.`,
			action: "toggle",
		};
	},

	deleteRule: async ({ request, locals }) => {
		if (!locals.user || !locals.token) {
			throw redirect(303, "/login");
		}

		const data = await request.formData();
		const id = data.get("id")?.toString().trim();

		if (!id) {
			return fail(400, {
				error: "Rule ID is required.",
				action: "delete",
			});
		}

		const result = await deleteQualityRule(locals.token, id);

		if (!result.ok) {
			return fail(result.status || 400, {
				error: result.error,
				action: "delete",
			});
		}

		return {
			success: true,
			message: "Quality rule deleted successfully!",
			action: "delete",
		};
	},
};
