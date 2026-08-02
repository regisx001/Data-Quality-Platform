import { error, fail, redirect } from "@sveltejs/kit";
import type { Actions, PageServerLoad } from "./$types";
import { getDatasetById, getDatasetPreview, profileDataset, deleteDataset } from "$lib/server/api/dataset";

export const load: PageServerLoad = async ({ locals, params }) => {
    if (!locals.user || !locals.token) {
        throw redirect(303, "/login");
    }

    const dataset = await getDatasetById(locals.token, params.id);
    const preview = dataset ? await getDatasetPreview(locals.token, params.id, 50) : null;

    return {
        dataset,
        preview,
        id: params.id,
        user: locals.user
    };
};

export const actions: Actions = {
    profile: async ({ locals, params }) => {
        if (!locals.user || !locals.token) {
            throw redirect(303, "/login");
        }

        const result = await profileDataset(locals.token, params.id);

        if (!result.ok) {
            return fail(400, {
                error: result.error,
                action: "profile"
            });
        }

        return {
            success: true,
            message: "Dataset profiling completed successfully!",
            dataset: result.data
        };
    },

    delete: async ({ locals, params }) => {
        if (!locals.user || !locals.token) {
            throw redirect(303, "/login");
        }

        const dataset = await getDatasetById(locals.token, params.id);
        const datasourceId = dataset?.datasourceId;

        const result = await deleteDataset(locals.token, params.id);

        if (!result.ok) {
            return fail(400, {
                error: result.error || "Failed to delete dataset",
                action: "delete"
            });
        }

        if (datasourceId) {
            throw redirect(303, `/datasources/${datasourceId}`);
        } else {
            throw redirect(303, "/datasources");
        }
    }
};
