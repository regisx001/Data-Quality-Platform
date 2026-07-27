import { error, fail, redirect } from "@sveltejs/kit";
import type { Actions, PageServerLoad } from "./$types";
import {
	getDatasourceById,
	updateDatasource,
	deleteDatasource,
	changeDatasourceStatus,
	type DatasourceStatus
} from "$lib/server/api";

export const load: PageServerLoad = async ({ locals, params }) => {
	if (!locals.user || !locals.token) {
		throw redirect(303, "/login");
	}

	const datasource = await getDatasourceById(locals.token, params.id);

	if (!datasource) {
		throw error(404, {
			message: `Datasource with ID '${params.id}' was not found`
		});
	}

	return {
		datasource,
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
	}
};
