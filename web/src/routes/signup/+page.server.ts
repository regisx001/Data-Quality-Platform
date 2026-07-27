import { fail, redirect } from "@sveltejs/kit";
import type { Actions, PageServerLoad } from "./$types";
import { registerUser } from "$lib/server/api";

export const load: PageServerLoad = async ({ locals }) => {
	if (locals.user) {
		throw redirect(303, "/");
	}
	return {};
};

export const actions: Actions = {
	default: async ({ request, cookies }) => {
		const data = await request.formData();
		const username = data.get("username")?.toString().trim();
		const email = data.get("email")?.toString().trim();
		const fullName = data.get("fullName")?.toString().trim();
		const password = data.get("password")?.toString();
		const confirmPassword = data.get("confirmPassword")?.toString();

		if (!username || !email || !fullName || !password) {
			return fail(400, {
				error: "All fields are required",
				username,
				email,
				fullName
			});
		}

		if (username.length < 3 || username.length > 50) {
			return fail(400, {
				error: "Username must be between 3 and 50 characters",
				username,
				email,
				fullName
			});
		}

		if (password.length < 8) {
			return fail(400, {
				error: "Password must be at least 8 characters long",
				username,
				email,
				fullName
			});
		}

		if (password !== confirmPassword) {
			return fail(400, {
				error: "Passwords do not match",
				username,
				email,
				fullName
			});
		}

		const result = await registerUser({
			username,
			email,
			fullName,
			password,
			role: "USER"
		});

		if (!result.ok) {
			return fail(result.status || 400, {
				error: result.error,
				username,
				email,
				fullName
			});
		}

		const maxAge = Math.floor((result.data.expiresIn || 86400000) / 1000);

		cookies.set("jwt", result.data.token, {
			path: "/",
			httpOnly: true,
			sameSite: "lax",
			secure: false,
			maxAge
		});

		throw redirect(303, "/");
	}
};
