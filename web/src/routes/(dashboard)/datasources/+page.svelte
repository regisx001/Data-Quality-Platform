<script lang="ts">
	import { enhance } from "$app/forms";
	import { Button } from "$lib/components/ui/button/index.js";
	import * as Field from "$lib/components/ui/field/index.js";
	import { Input } from "$lib/components/ui/input/index.js";
	import * as Dialog from "$lib/components/ui/dialog/index.js";
	import Plus from "@lucide/svelte/icons/plus";
	import Loader2 from "@lucide/svelte/icons/loader-2";
	import Trash2 from "@lucide/svelte/icons/trash-2";
	import DatasourceDataTable from "$lib/components/dashboard/datasource-data-table.svelte";
	import ErrorAlert from "$lib/components/ui/error-alert.svelte";
	import type { PageData, ActionData } from "./$types";
	import type { Datasource } from "$lib/server/api";

	let { data, form }: { data: PageData; form: ActionData } = $props();

	// Dialog States
	let isCreateOpen = $state(false);
	let isEditOpen = $state(false);
	let isDeleteOpen = $state(false);

	let selectedDatasource = $state<Datasource | null>(null);
	let isSubmitting = $state(false);

	let datasources = $derived(data.datasources || []);

	function openEdit(ds: Datasource) {
		selectedDatasource = ds;
		isEditOpen = true;
	}

	function openDelete(ds: Datasource) {
		selectedDatasource = ds;
		isDeleteOpen = true;
	}
</script>

<svelte:head>
	<title>Datasources | Data Quality Platform</title>
	<meta name="description" content="Manage enterprise data sources, connections, and status configurations." />
</svelte:head>

<div class="p-6 sm:p-8 w-full space-y-6">
	<!-- Page Header -->
	<div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-6">
		<div class="space-y-1">
			<h1 class="text-2xl font-bold tracking-tight">Datasource Management</h1>
			<p class="text-sm text-muted-foreground">
				Configure, monitor, and transition data sources across your enterprise environment.
			</p>
		</div>

		<!-- Big Action: New Datasource Button -->
		<Button onclick={() => (isCreateOpen = true)}>
			<Plus class="size-4" />
			<span>New Datasource</span>
		</Button>
	</div>

	<!-- Alert Messages -->
	{#if form?.success && form?.message}
		<div class="p-4 text-sm text-emerald-600 dark:text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 rounded-md">
			{form.message}
		</div>
	{/if}

	{#if form?.error}
		<ErrorAlert error={form.error} title="Datasource Operation Failed" />
	{/if}

	<!-- Custom Composable Datasource DataTable (matching dashboard-01) -->
	<DatasourceDataTable
		{datasources}
		onNewDatasource={() => (isCreateOpen = true)}
		onEditDatasource={openEdit}
		onDeleteDatasource={openDelete}
	/>
</div>

<!-- =========================================================================
     Create Datasource Dialog
     ========================================================================= -->
<Dialog.Root bind:open={isCreateOpen}>
	<Dialog.Content class="sm:max-w-lg rounded-lg p-6 border-border bg-card">
		<Dialog.Header class="space-y-1">
			<Dialog.Title class="text-lg font-bold tracking-tight">Create New Datasource</Dialog.Title>
			<Dialog.Description class="text-sm text-muted-foreground">
				Register a new data source connection in your platform.
			</Dialog.Description>
		</Dialog.Header>

		<form
			action="?/createDatasource"
			method="POST"
			use:enhance={() => {
				isSubmitting = true;
				return async ({ update, result }) => {
					isSubmitting = false;
					if (result.type === "success") {
						isCreateOpen = false;
					}
					await update();
				};
			}}
			class="space-y-4 pt-3 text-sm"
		>
			<Field.Field>
				<Field.Label for="create-name">Datasource Name</Field.Label>
				<Input
					id="create-name"
					name="name"
					type="text"
					placeholder="my-datasource"
					required
					disabled={isSubmitting}
				/>
			</Field.Field>

			<Field.Field>
				<Field.Label for="create-type">Database / Engine Type</Field.Label>
				<select
					id="create-type"
					name="type"
					required
					disabled={isSubmitting}
					class="w-full h-10 px-3 rounded-md border border-input bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-ring cursor-pointer"
				>
					<option value="PostgreSQL">PostgreSQL</option>
					<option value="CSV">CSV / File Stream</option>
				</select>
			</Field.Field>



			<Field.Field>
				<Field.Label for="create-owner">Owner Username</Field.Label>
				<Input
					id="create-owner"
					name="owner"
					type="text"
					value={data.user?.username || "admin"}
					required
					disabled={isSubmitting}
				/>
			</Field.Field>

			<Field.Field>
				<Field.Label for="create-description">Description (Optional)</Field.Label>
				<Input
					id="create-description"
					name="description"
					type="text"
					placeholder="Production database for customer records"
					disabled={isSubmitting}
				/>
			</Field.Field>

			<Dialog.Footer class="pt-4 flex items-center justify-end gap-2">
				<Button
					type="button"
					variant="outline"
					onclick={() => (isCreateOpen = false)}
					disabled={isSubmitting}
				>
					Cancel
				</Button>
				<Button type="submit" disabled={isSubmitting}>
					{#if isSubmitting}
						<Loader2 class="size-4 animate-spin" />
						<span>Creating...</span>
					{:else}
						<Plus class="size-4" />
						<span>Create Datasource</span>
					{/if}
				</Button>
			</Dialog.Footer>
		</form>
	</Dialog.Content>
</Dialog.Root>

<!-- =========================================================================
     Edit Datasource Dialog
     ========================================================================= -->
{#if selectedDatasource}
	<Dialog.Root bind:open={isEditOpen}>
		<Dialog.Content class="sm:max-w-lg rounded-lg p-6 border-border bg-card">
			<Dialog.Header class="space-y-1">
				<Dialog.Title class="text-lg font-bold tracking-tight">Edit Datasource</Dialog.Title>
				<Dialog.Description class="text-sm text-muted-foreground">
					Update settings for '{selectedDatasource.name}'.
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
							isEditOpen = false;
						}
						await update();
					};
				}}
				class="space-y-4 pt-3 text-sm"
			>
				<input type="hidden" name="id" value={selectedDatasource.id} />

				<Field.Field>
					<Field.Label for="edit-name">Datasource Name</Field.Label>
					<Input
						id="edit-name"
						name="name"
						type="text"
						value={selectedDatasource.name}
						required
						disabled={isSubmitting}
					/>
				</Field.Field>

				<Field.Field>
					<Field.Label for="edit-type">Engine Type</Field.Label>
					<Input
						id="edit-type"
						name="type"
						type="text"
						value={selectedDatasource.type}
						required
						disabled={isSubmitting}
					/>
				</Field.Field>

				<Field.Field>
					<Field.Label for="edit-status">Status</Field.Label>
					<select
						id="edit-status"
						name="status"
						value={selectedDatasource.status}
						disabled={isSubmitting}
						class="w-full h-10 px-3 rounded-md border border-input bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-ring cursor-pointer"
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
						value={selectedDatasource.description || ""}
						disabled={isSubmitting}
					/>
				</Field.Field>

				<Dialog.Footer class="pt-4 flex items-center justify-end gap-2">
					<Button
						type="button"
						variant="outline"
						onclick={() => (isEditOpen = false)}
						disabled={isSubmitting}
					>
						Cancel
					</Button>
					<Button type="submit" disabled={isSubmitting}>
						{#if isSubmitting}
							<Loader2 class="size-4 animate-spin" />
							<span>Saving...</span>
						{:else}
							<span>Save Changes</span>
						{/if}
					</Button>
				</Dialog.Footer>
			</form>
		</Dialog.Content>
	</Dialog.Root>
{/if}

<!-- =========================================================================
     Delete Confirmation Dialog
     ========================================================================= -->
{#if selectedDatasource}
	<Dialog.Root bind:open={isDeleteOpen}>
		<Dialog.Content class="sm:max-w-md rounded-lg p-6 border-border bg-card">
			<Dialog.Header class="space-y-1">
				<Dialog.Title class="text-lg font-bold text-destructive">Delete Datasource</Dialog.Title>
				<Dialog.Description class="text-sm text-muted-foreground">
					Are you sure you want to permanently delete <strong class="text-foreground">{selectedDatasource.name}</strong>? This action cannot be undone.
				</Dialog.Description>
			</Dialog.Header>

			<form
				action="?/deleteDatasource"
				method="POST"
				use:enhance={() => {
					isSubmitting = true;
					return async ({ update, result }) => {
						isSubmitting = false;
						if (result.type === "success") {
							isDeleteOpen = false;
						}
						await update();
					};
				}}
				class="pt-3"
			>
				<input type="hidden" name="id" value={selectedDatasource.id} />

				<Dialog.Footer class="flex items-center justify-end gap-2">
					<Button
						type="button"
						variant="outline"
						onclick={() => (isDeleteOpen = false)}
						disabled={isSubmitting}
					>
						Cancel
					</Button>
					<Button
						type="submit"
						variant="destructive"
						disabled={isSubmitting}
					>
						{#if isSubmitting}
							<Loader2 class="size-4 animate-spin" />
							<span>Deleting...</span>
						{:else}
							<Trash2 class="size-4" />
							<span>Confirm Delete</span>
						{/if}
					</Button>
				</Dialog.Footer>
			</form>
		</Dialog.Content>
	</Dialog.Root>
{/if}
