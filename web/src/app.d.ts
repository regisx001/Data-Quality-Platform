import type { UserProfile } from "$lib/server/api";

declare global {
	namespace App {
		// interface Error {}
		interface Locals {
			user: UserProfile | null;
			token: string | null;
		}
		interface PageData {
			user: UserProfile | null;
		}
		// interface PageState {}
		// interface Platform {}
	}
}

export {};
