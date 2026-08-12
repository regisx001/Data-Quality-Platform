<script lang="ts">
	import { onMount } from "svelte";
	import { goto } from "$app/navigation";

	onMount(() => {
		goto("/logs", { replaceState: true });
	});
</script>

<div class="p-8 text-center text-xs text-muted-foreground">
	Redirecting to Real-Time Streaming Dashboard...
</div>
