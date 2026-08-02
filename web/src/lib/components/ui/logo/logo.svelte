<script lang="ts">
	import type { HTMLImgAttributes } from "svelte/elements";
	import { cn } from "$lib/utils";

	let {
		class: className = "",
		variant = "auto",
		iconOnly = false,
		...rest
	}: HTMLImgAttributes & {
		variant?: "auto" | "white" | "black";
		iconOnly?: boolean;
	} = $props();
</script>

{#if variant === "white"}
	<img
		src="/logo-qul-white.png"
		alt="Qul Logo"
		class={cn("h-12 md:h-14 w-auto max-w-full object-contain rounded-lg shrink-0 transition-all", className)}
		{...rest}
	/>
{:else if variant === "black"}
	<img
		src="/logo-qul-black.png"
		alt="Qul Logo"
		class={cn("h-12 md:h-14 w-auto max-w-full object-contain rounded-lg shrink-0 transition-all", className)}
		{...rest}
	/>
{:else}
	<!-- Auto mode: Displays white logo in dark mode, black logo in light mode -->
	<img
		src="/logo-qul-white.png"
		alt="Qul Logo (Dark Mode)"
		class={cn("hidden dark:block h-12 md:h-14 w-auto max-w-full object-contain rounded-lg shrink-0 transition-all", className)}
		{...rest}
	/>
	<img
		src="/logo-qul-black.png"
		alt="Qul Logo (Light Mode)"
		class={cn("block dark:hidden h-12 md:h-14 w-auto max-w-full object-contain rounded-lg shrink-0 transition-all", className)}
		{...rest}
	/>
{/if}
