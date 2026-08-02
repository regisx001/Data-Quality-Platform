import { fail, redirect } from "@sveltejs/kit";
import type { Actions, PageServerLoad } from "./$types";
import {
	getDatasources,
	createDatasource,
	updateDatasource,
	deleteDatasource,
	changeDatasourceStatus,
	uploadCsvFile,
	type DatasourceStatus
} from "$lib/server/api";

export const load: PageServerLoad = async ({ locals, url }) => {
	if (!locals.user || !locals.token) {
		throw redirect(303, "/login");
	}

	const statusFilter = url.searchParams.get("status") || "ALL";
	const ownerFilter = url.searchParams.get("owner") || undefined;

	const datasources = await getDatasources(locals.token, {
		status: statusFilter !== "ALL" ? statusFilter : undefined,
		owner: ownerFilter
	});

	return {
		datasources,
		currentStatusFilter: statusFilter,
		user: locals.user
	};
};

export const actions: Actions = {
	createDatasource: async ({ request, locals }) => {
		if (!locals.user || !locals.token) {
			throw redirect(303, "/login");
		}

		const data = await request.formData();
		const name = data.get("name")?.toString().trim();
		const type = data.get("type")?.toString().trim();
		const description = data.get("description")?.toString().trim() || undefined;
		const owner = data.get("owner")?.toString().trim() || locals.user.username;

		if (!name || !type) {
			return fail(400, {
				error: "Datasource Name and Type are required",
				action: "create"
			});
		}

		let configJson: string | undefined = undefined;

		if (type.toUpperCase() === "CSV") {
			let filePath = data.get("filePath")?.toString().trim() || "";
			const csvSourceMode = data.get("csvSourceMode")?.toString() || "upload";

			if (csvSourceMode === "upload") {
				const csvFile = data.get("csvFile") as File | null;
				if (csvFile && csvFile.size > 0 && csvFile.name) {
					const uploadData = new FormData();
					uploadData.append("file", csvFile);
					const uploadRes = await uploadCsvFile(locals.token, uploadData);
					if (!uploadRes.ok) {
						return fail(uploadRes.status || 400, {
							error: uploadRes.error || "Failed to upload CSV file to MinIO",
							action: "create"
						});
					}
					filePath = uploadRes.data.filePath;
				}
			}

			const delimiter = data.get("delimiter")?.toString() || ",";
			const header = data.get("header")?.toString() !== "false";
			const encoding = data.get("encoding")?.toString() || "UTF-8";
			const quoteChar = data.get("quoteChar")?.toString() || "\"";
			const escapeChar = data.get("escapeChar")?.toString() || "\\";
			const inferSchema = data.get("inferSchema")?.toString() !== "false";

			configJson = JSON.stringify({
				filePath,
				delimiter,
				header,
				encoding,
				quoteChar,
				escapeChar,
				inferSchema
			});
		}

		const result = await createDatasource(locals.token, {
			name,
			type,
			description,
			owner,
			configJson
		});

		if (!result.ok) {
			return fail(result.status || 400, {
				error: result.error,
				action: "create"
			});
		}

		return {
			success: true,
			message: `Datasource '${result.data.name}' created successfully!`
		};
	},

	updateDatasource: async ({ request, locals }) => {
		if (!locals.user || !locals.token) {
			throw redirect(303, "/login");
		}

		const data = await request.formData();
		const id = data.get("id")?.toString().trim();
		const name = data.get("name")?.toString().trim() || undefined;
		const type = data.get("type")?.toString().trim() || undefined;
		const description = data.get("description")?.toString().trim() || undefined;
		const status = (data.get("status")?.toString().trim() || undefined) as DatasourceStatus | undefined;

		if (!id) {
			return fail(400, {
				error: "Datasource ID is required",
				action: "update"
			});
		}

		const result = await updateDatasource(locals.token, id, {
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
			message: `Datasource '${result.data.name}' updated successfully!`
		};
	},

	deleteDatasource: async ({ request, locals }) => {
		if (!locals.user || !locals.token) {
			throw redirect(303, "/login");
		}

		const data = await request.formData();
		const id = data.get("id")?.toString().trim();

		if (!id) {
			return fail(400, {
				error: "Datasource ID is required",
				action: "delete"
			});
		}

		const result = await deleteDatasource(locals.token, id);

		if (!result.ok) {
			return fail(result.status || 400, {
				error: result.error,
				action: "delete"
			});
		}

		return {
			success: true,
			message: "Datasource deleted successfully!"
		};
	},

	changeStatus: async ({ request, locals }) => {
		if (!locals.user || !locals.token) {
			throw redirect(303, "/login");
		}

		const data = await request.formData();
		const id = data.get("id")?.toString().trim();
		const statusAction = data.get("statusAction")?.toString().trim() as "activate" | "disable" | "archive";

		if (!id || !statusAction || !["activate", "disable", "archive"].includes(statusAction)) {
			return fail(400, {
				error: "Valid Datasource ID and Action are required",
				action: "changeStatus"
			});
		}

		const result = await changeDatasourceStatus(locals.token, id, statusAction);

		if (!result.ok) {
			return fail(result.status || 400, {
				error: result.error,
				action: "changeStatus"
			});
		}

		return {
			success: true,
			message: `Datasource '${result.data.name}' status changed to ${result.data.status}!`
		};
	}
};
