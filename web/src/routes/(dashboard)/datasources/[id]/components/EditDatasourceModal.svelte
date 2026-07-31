<script lang="ts">
	import { enhance } from "$app/forms";
	import { Button } from "$lib/components/ui/button/index.js";
	import * as Field from "$lib/components/ui/field/index.js";
	import { Input } from "$lib/components/ui/input/index.js";
	import * as Dialog from "$lib/components/ui/dialog/index.js";
	import Loader2 from "@lucide/svelte/icons/loader-2";
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
	<Dialog.Content class="sm:max-w-lg rounded-xl p-6 border-border bg-card">
		<Dialog.Header class="space-y-1">
			<Dialog.Title class="text-lg font-bold tracking-tight">Edit Datasource</Dialog.Title>
			<Dialog.Description class="text-xs text-muted-foreground">
				Update settings for '{datasource.name}'.
			</Dialog.Description>
		</Dialog.Header>

		<form
			action="?/updateDatasource"
			method="POST"
			use:enhance={() => {
				isSubmitting = true;
				return async ({ update, result }) => {
					isSubmitting = false;
					if (result.type === "success") {
						open = false;
					}
					await update();
				};
			}}
			class="space-y-4 pt-3 text-xs"
		>
			<Field.Field>
				<Field.Label for="edit-name">Datasource Name</Field.Label>
				<Input
					id="edit-name"
					name="name"
					type="text"
					value={datasource.name}
					required
					disabled={isSubmitting}
					class="h-9 rounded-lg"
				/>
			</Field.Field>

			<Field.Field>
				<Field.Label for="edit-type">Engine Type</Field.Label>
				<Input
					id="edit-type"
					name="type"
					type="text"
					value={datasource.type}
					required
					disabled={isSubmitting}
					class="h-9 rounded-lg"
				/>
			</Field.Field>

			<Field.Field>
				<Field.Label for="edit-status">Status</Field.Label>
				<select
					id="edit-status"
					name="status"
					value={datasource.status}
					disabled={isSubmitting}
					class="w-full h-9 px-3 rounded-lg border border-border bg-background text-foreground text-xs focus:ring-primary focus:border-primary cursor-pointer"
				>
					<option value="REGISTERED">REGISTERED</option>
					<option value="ACTIVE">ACTIVE</option>
					<option value="DISABLED">DISABLED</option>
					<option value="ARCHIVED">ARCHIVED</option>
				</select>
			</Field.Field>

			<Field.Field>
				<Field.Label for="edit-description">Description</Field.Label>
				<Input
					id="edit-description"
					name="description"
					type="text"
					value={datasource.description || ""}
					disabled={isSubmitting}
					class="h-9 rounded-lg"
				/>
			</Field.Field>

			<Dialog.Footer class="pt-3 flex items-center justify-end gap-2">
				<Button
					type="button"
					variant="outline"
					onclick={() => (open = false)}
					disabled={isSubmitting}
					class="h-9 rounded-lg cursor-pointer"
				>
					Cancel
				</Button>
				<Button type="submit" disabled={isSubmitting} class="h-9 rounded-lg font-medium cursor-pointer">
					{#if isSubmitting}
						<Loader2 class="size-3.5 me-1.5 animate-spin" />
						<span>Saving...</span>
					{:else}
						<span>Save Changes</span>
					{/if}
				</Button>
			</Dialog.Footer>
		</form>
	</Dialog.Content>
</Dialog.Root>
