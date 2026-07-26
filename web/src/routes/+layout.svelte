<script lang="ts">
	import "./layout.css";
	import favicon from "$lib/assets/favicon.svg";
	import { ModeWatcher } from "mode-watcher";
	import { onNavigate } from "$app/navigation";

	let { children } = $props();

	// Native SvelteKit View Transition API hook for smooth page transitions
	onNavigate((navigation) => {
		if (!document.startViewTransition) return;

		return new Promise((resolve) => {
			document.startViewTransition(async () => {
				resolve();
				await navigation.complete;
			});
		});
	});
</script>

<ModeWatcher defaultMode="dark" />
<svelte:head><link rel="icon" href={favicon} /></svelte:head>
{@render children()}
