import { redirect, type Handle } from "@sveltejs/kit";
import { getCurrentUser } from "$lib/server/api";

const PUBLIC_ROUTES = ["/login", "/signup"];

export const handle: Handle = async ({ event, resolve }) => {
	const token = event.cookies.get("jwt") || null;
	let user = null;

	if (token) {
		user = await getCurrentUser(token);
		if (!user) {
			// Token invalid or expired, clear cookie
			event.cookies.delete("jwt", { path: "/" });
		}
	}

	event.locals.token = user ? token : null;
	event.locals.user = user;

	const { pathname } = event.url;
	const isPublicRoute = PUBLIC_ROUTES.some((route) => pathname.startsWith(route));

	// If unauthenticated and trying to access protected page
	if (!user && !isPublicRoute) {
		throw redirect(303, "/login");
	}

	// If authenticated and trying to access login/signup pages
	if (user && isPublicRoute) {
		throw redirect(303, "/");
	}

	return resolve(event);
};
