import { error, fail, redirect } from "@sveltejs/kit";
import type { Actions, PageServerLoad } from "./$types";
import {
	getDatasourceById,
	updateDatasource,
	deleteDatasource,
	changeDatasourceStatus,
	saveDatasourceConfig,
	getDatasourceConfig,
	getConfigSchemas,
	testDatasourceConnection,
	discoverDatasourceDatasets,
	importDatasourceDatasets,
	type DatasourceStatus
} from "$lib/server/api";

const VALID_TABS = ["overview", "config", "connection"] as const;

export const load: PageServerLoad = async ({ locals, params, url }) => {
	if (!locals.user || !locals.token) {
		throw redirect(303, "/login");
	}

	const rawTab = url.searchParams.get("tab");
	const activeTab = VALID_TABS.includes(rawTab as any) ? rawTab : "overview";

	const datasource = await getDatasourceById(locals.token, params.id);

	if (!datasource) {
		throw error(404, {
			message: `Datasource with ID '${params.id}' was not found`
		});
	}

	const [configJson, configSchemas] = await Promise.all([
		getDatasourceConfig(locals.token, params.id),
		getConfigSchemas(locals.token)
	]);

	const configSchema = configSchemas.find(
		s => s.type === datasource.type || s.type === datasource.type.toUpperCase()
	) || null;

	return {
		datasource,
		configJson,
		configSchema,
		activeTab,
		user: locals.user
	};
};

export const actions: Actions = {
	updateDatasource: async ({ request, locals, params }) => {
		if (!locals.user || !locals.token) {
			throw redirect(303, "/login");
		}

		const data = await request.formData();
		const name = data.get("name")?.toString().trim() || undefined;
		const type = data.get("type")?.toString().trim() || undefined;
		const description = data.get("description")?.toString().trim() || undefined;
		const status = (data.get("status")?.toString().trim() || undefined) as DatasourceStatus | undefined;

		const result = await updateDatasource(locals.token, params.id, {
			name,
			type,
			description,
			status
		});

		if (!result.ok) {
			return fail(result.status || 400, {
				error: result.error,
				action: "update"
			});
		}

		return {
			success: true,
			message: `Datasource '${result.data.name}' updated successfully!`,
			datasource: result.data
		};
	},

	changeStatus: async ({ request, locals, params }) => {
		if (!locals.user || !locals.token) {
			throw redirect(303, "/login");
		}

		const data = await request.formData();
		const statusAction = data.get("statusAction")?.toString().trim() as "activate" | "disable" | "archive";

		if (!statusAction || !["activate", "disable", "archive"].includes(statusAction)) {
			return fail(400, {
				error: "Valid status action is required",
				action: "changeStatus"
			});
		}

		const result = await changeDatasourceStatus(locals.token, params.id, statusAction);

		if (!result.ok) {
			return fail(result.status || 400, {
				error: result.error,
				action: "changeStatus"
			});
		}

		return {
			success: true,
			message: `Datasource status changed to ${result.data.status}!`,
			datasource: result.data
		};
	},

	deleteDatasource: async ({ locals, params }) => {
		if (!locals.user || !locals.token) {
			throw redirect(303, "/login");
		}

		const result = await deleteDatasource(locals.token, params.id);

		if (!result.ok) {
			return fail(result.status || 400, {
				error: result.error,
				action: "delete"
			});
		}

		throw redirect(303, "/datasources");
	},

	saveConfig: async ({ request, locals, params }) => {
		if (!locals.user || !locals.token) {
			throw redirect(303, "/login");
		}

		const data = await request.formData();
		const configJson = data.get("configJson")?.toString().trim() || "";

		const result = await saveDatasourceConfig(locals.token, params.id, configJson);

		if (!result.ok) {
			return fail(result.status || 400, {
				error: result.error,
				action: "saveConfig"
			});
		}

		return {
			success: true,
			message: "Datasource configuration saved successfully!",
			configJson
		};
	},

	testConnection: async ({ locals, params }) => {
		if (!locals.user || !locals.token) {
			throw redirect(303, "/login");
		}

		try {
			const result = await testDatasourceConnection(locals.token, params.id);

			if (!result.ok) {
				return fail(result.status || 400, {
					error: result.error,
					action: "testConnection"
				});
			}

			return {
				success: true,
				message: result.data.message,
				connectionTest: result.data
			};
		} catch (err: any) {
			return fail(500, {
				error: err.message || "Connection test failed due to an unexpected error",
				action: "testConnection"
			});
		}
	},

	discoverDatasets: async ({ locals, params }) => {
		if (!locals.user || !locals.token) {
			throw redirect(303, "/login");
		}

		try {
			const result = await discoverDatasourceDatasets(locals.token, params.id);

			if (!result.ok) {
				return fail(result.status || 400, {
					error: result.error,
					action: "discoverDatasets"
				});
			}

			return {
				success: true,
				discoveredDatasets: result.data,
				action: "discoverDatasets"
			};
		} catch (err: any) {
			return fail(500, {
				error: err.message || "Failed to discover datasets",
				action: "discoverDatasets"
			});
		}
	},

	importDatasets: async ({ request, locals, params }) => {
		if (!locals.user || !locals.token) {
			throw redirect(303, "/login");
		}

		try {
			const formData = await request.formData();
			const datasetIds = formData.getAll("datasetIds").map(id => id.toString());

			if (datasetIds.length === 0) {
				return fail(400, {
					error: "No datasets selected for import",
					action: "importDatasets"
				});
			}

			const result = await importDatasourceDatasets(locals.token, params.id, datasetIds);

			if (!result.ok) {
				return fail(result.status || 400, {
					error: result.error,
					action: "importDatasets"
				});
			}

			return {
				success: true,
				message: `Successfully imported ${result.data.length} dataset(s)!`,
				importedDatasets: result.data,
				action: "importDatasets"
			};
		} catch (err: any) {
			return fail(500, {
				error: err.message || "Failed to import datasets",
				action: "importDatasets"
			});
		}
	}
};
