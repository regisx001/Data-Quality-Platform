import tailwindcss from '@tailwindcss/vite';
import adapter from '@sveltejs/adapter-node';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [
		tailwindcss(),
		sveltekit({
			compilerOptions: {
				// Force runes mode for the project, except for libraries. Can be removed in svelte 6.
				runes: ({ filename }) => filename.split(/[/\\]/).includes('node_modules') ? undefined : true
			},

			adapter: adapter()
		})
	],

	server: {
		proxy: {
			'/api/v1/logs': {
				// @ts-expect-error - Bun.env is available in Bun runtime
				target: (typeof Bun !== 'undefined' && Bun.env.LOGS_API_URL) || 'http://localhost:7001',
				changeOrigin: true
			},
			'/api': {
				// @ts-expect-error - Bun.env is available in Bun runtime
				target: (typeof Bun !== 'undefined' && Bun.env.BACKEND_API_URL) || 'http://localhost:7000',
				changeOrigin: true
			}
		}
	}
});
