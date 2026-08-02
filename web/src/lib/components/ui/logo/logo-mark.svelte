<script lang="ts">
	import type { SVGAttributes } from "svelte/elements";
	import { cn } from "$lib/utils";

	let { class: className = "", ...rest }: SVGAttributes<SVGElement> = $props();
</script>

<svg
	xmlns="http://www.w3.org/2000/svg"
	viewBox="0 0 36 36"
	fill="none"
	class={cn("size-6 shrink-0 text-foreground", className)}
	{...rest}
>
	<rect x="0" y="2" width="7" height="7" fill="currentColor" />
	<rect x="0" y="31" width="36" height="7" fill="currentColor" />
	<path d="M0 28L16 12L22 18L6 34H0V28Z" fill="currentColor" />
	<path d="M12 26L26 12V21H36V2H17V12H26L12 26Z" fill="currentColor" />
</svg>
