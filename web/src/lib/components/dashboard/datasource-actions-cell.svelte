<script lang="ts">
	import { Button } from "$lib/components/ui/button/index.js";
	import Eye from "@lucide/svelte/icons/eye";
	import Edit from "@lucide/svelte/icons/edit";
	import Play from "@lucide/svelte/icons/play";
	import Pause from "@lucide/svelte/icons/pause";
	import Trash2 from "@lucide/svelte/icons/trash-2";
	import { enhance } from "$app/forms";
	import type { Datasource } from "$lib/server/api";

	let {
		ds,
		onEdit,
		onDelete
	}: {
		ds: Datasource;
		onEdit?: (ds: Datasource) => void;
		onDelete?: (ds: Datasource) => void;
	} = $props();
</script>

<div class="flex items-center justify-end gap-1.5">
	<Button variant="ghost" size="icon" href={`/datasources/${ds.id}`} title="View Details">
		<Eye class="size-4" />
	</Button>

	{#if onEdit}
		<Button variant="ghost" size="icon" onclick={() => onEdit(ds)} title="Edit Datasource">
			<Edit class="size-4" />
		</Button>
	{/if}

	{#if ds.status !== "ACTIVE"}
		<form action="?/changeStatus" method="POST" use:enhance class="inline">
			<input type="hidden" name="id" value={ds.id} />
			<input type="hidden" name="statusAction" value="activate" />
			<Button
				variant="ghost"
				size="icon"
				type="submit"
				title="Activate Datasource"
				class="text-emerald-600 dark:text-emerald-400 hover:bg-emerald-500/10"
			>
				<Play class="size-4" />
			</Button>
		</form>
	{:else}
		<form action="?/changeStatus" method="POST" use:enhance class="inline">
			<input type="hidden" name="id" value={ds.id} />
			<input type="hidden" name="statusAction" value="disable" />
			<Button
				variant="ghost"
				size="icon"
				type="submit"
				title="Disable Datasource"
				class="text-amber-600 dark:text-amber-400 hover:bg-amber-500/10"
			>
				<Pause class="size-4" />
			</Button>
		</form>
	{/if}

	{#if onDelete}
		<Button
			variant="ghost"
			size="icon"
			onclick={() => onDelete(ds)}
			title="Delete Datasource"
			class="text-destructive hover:bg-destructive/10"
		>
			<Trash2 class="size-4" />
		</Button>
	{/if}
</div>
