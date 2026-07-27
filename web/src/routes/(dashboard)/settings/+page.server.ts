import { fail, redirect } from "@sveltejs/kit";
import type { Actions, PageServerLoad } from "./$types";

export const load: PageServerLoad = async ({ locals }) => {
	if (!locals.user || !locals.token) {
		throw redirect(303, "/login");
	}

	return {
		user: locals.user
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

		if (email && !email.includes("@")) {
			return fail(400, {
				error: "Please enter a valid email address",
				success: false
			});
		}

		const updatedUser = {
			...locals.user,
			fullName: fullName !== undefined ? fullName : locals.user.fullName,
			email: email !== undefined ? email : locals.user.email
		};

		locals.user = updatedUser;

		return {
			success: true,
			error: undefined,
			message: "Account settings updated successfully!",
			user: updatedUser
		};
	}
};
