import { fail, redirect } from "@sveltejs/kit";
import type { Actions, PageServerLoad } from "./$types";
import { getCurrentUser, updateUserProfile } from "$lib/server/api";

export const load: PageServerLoad = async ({ locals }) => {
	if (!locals.user || !locals.token) {
		throw redirect(303, "/login");
	}

	const freshUser = await getCurrentUser(locals.token);

	return {
		user: freshUser || locals.user
	};
};

export const actions: Actions = {
	updateProfile: async ({ request, locals }) => {
		if (!locals.user || !locals.token) {
			throw redirect(303, "/login");
		}

		const data = await request.formData();
		const fullName = data.get("fullName")?.toString().trim();
		const email = data.get("email")?.toString().trim();

		if (!fullName || !email) {
			return fail(400, {
				error: "Full Name and Email are required",
				success: false
			});
		}

		const result = await updateUserProfile(locals.token, locals.user.userId, {
			fullName,
			email
		});

		if (!result.ok) {
			return fail(result.status || 400, {
				error: result.error,
				success: false
			});
		}

		return {
			success: true,
			message: "Profile updated successfully!",
			user: result.data
		};
	}
};
