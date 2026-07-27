import { fail, redirect } from "@sveltejs/kit";
import type { Actions, PageServerLoad } from "./$types";
import { loginUser } from "$lib/server/api";

export const load: PageServerLoad = async ({ locals }) => {
	if (locals.user) {
		throw redirect(303, "/");
	}
	return {};
};

export const actions: Actions = {
	default: async ({ request, cookies }) => {
		const data = await request.formData();
		const login = data.get("login")?.toString().trim();
		const password = data.get("password")?.toString();

		if (!login || !password) {
			return fail(400, {
				error: "Username/Email and Password are required",
				login
			});
		}

		const result = await loginUser({ login, password });

		if (!result.ok) {
			return fail(result.status || 400, {
				error: result.error,
				login
			});
		}

		const maxAge = Math.floor((result.data.expiresIn || 86400000) / 1000);

		cookies.set("jwt", result.data.token, {
			path: "/",
			httpOnly: true,
			sameSite: "lax",
			secure: false, // set to true in production if HTTPS
			maxAge
		});

		throw redirect(303, "/");
	}
};
