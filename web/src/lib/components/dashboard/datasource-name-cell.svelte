<script lang="ts">
	import type { Datasource } from "$lib/server/api";

	let { ds }: { ds: Datasource } = $props();
</script>

<div class="space-y-1">
	<a
		href={`/datasources/${ds.id}`}
		class="font-semibold text-foreground hover:text-primary transition-colors text-start block text-sm"
	>
		{ds.name}
	</a>
	{#if ds.description}
		<p class="text-xs text-muted-foreground line-clamp-1">{ds.description}</p>
	{/if}
</div>
