<script lang="ts">
	import { enhance } from "$app/forms";
	import { Button } from "$lib/components/ui/button/index.js";
	import * as Dialog from "$lib/components/ui/dialog/index.js";
	import Loader2 from "@lucide/svelte/icons/loader-2";
	import Trash2 from "@lucide/svelte/icons/trash-2";
	import type { Datasource } from "$lib/server/api";

	let {
		open = $bindable(false),
		datasource,
	}: {
		open: boolean;
		datasource: Datasource;
	} = $props();

	let isSubmitting = $state(false);
</script>

<Dialog.Root bind:open>
	<Dialog.Content class="sm:max-w-md rounded-xl p-6 border-border bg-card">
		<Dialog.Header class="space-y-1">
			<Dialog.Title class="text-lg font-bold text-destructive">Delete Datasource</Dialog.Title>
			<Dialog.Description class="text-xs text-muted-foreground">
				Are you sure you want to permanently delete <strong class="text-foreground">{datasource.name}</strong>? This action cannot be undone.
			</Dialog.Description>
		</Dialog.Header>

		<form
			action="?/deleteDatasource"
			method="POST"
			use:enhance={() => {
				isSubmitting = true;
				return async ({ update }) => {
					isSubmitting = false;
					await update();
				};
			}}
			class="pt-3"
		>
			<Dialog.Footer class="flex items-center justify-end gap-2">
				<Button
					type="button"
					variant="outline"
					onclick={() => (open = false)}
					disabled={isSubmitting}
					class="h-9 rounded-lg cursor-pointer text-xs"
				>
					Cancel
				</Button>
				<Button
					type="submit"
					variant="destructive"
					disabled={isSubmitting}
					class="h-9 rounded-lg font-medium cursor-pointer text-xs"
				>
					{#if isSubmitting}
						<Loader2 class="size-3.5 me-1.5 animate-spin" />
						<span>Deleting...</span>
					{:else}
						<Trash2 class="size-3.5 me-1.5" />
						<span>Confirm Delete</span>
					{/if}
				</Button>
			</Dialog.Footer>
		</form>
	</Dialog.Content>
</Dialog.Root>
