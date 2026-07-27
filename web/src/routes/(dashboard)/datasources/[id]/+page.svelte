<script lang="ts">
	import { enhance } from "$app/forms";
	import { Button } from "$lib/components/ui/button/index.js";
	import * as Card from "$lib/components/ui/card/index.js";
	import * as Field from "$lib/components/ui/field/index.js";
	import { Input } from "$lib/components/ui/input/index.js";
	import * as Dialog from "$lib/components/ui/dialog/index.js";
	import ArrowLeft from "@lucide/svelte/icons/arrow-left";
	import Edit from "@lucide/svelte/icons/edit";
	import Trash2 from "@lucide/svelte/icons/trash-2";
	import Play from "@lucide/svelte/icons/play";
	import Pause from "@lucide/svelte/icons/pause";
	import Archive from "@lucide/svelte/icons/archive";
	import Loader2 from "@lucide/svelte/icons/loader-2";
	import type { PageData, ActionData } from "./$types";
	import type { DatasourceStatus } from "$lib/server/api";

	let { data, form }: { data: PageData; form: ActionData } = $props();

	let datasource = $derived(form?.datasource || data.datasource);
	let statusInfo = $derived(getStatusBadge(datasource.status));

	// Dialog States
	let isEditOpen = $state(false);
	let isDeleteOpen = $state(false);
	let isSubmitting = $state(false);

	function getStatusBadge(status: DatasourceStatus) {
		switch (status) {
			case "ACTIVE":
				return {
					label: "Active",
					classes: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20"
				};
			case "REGISTERED":
				return {
					label: "Registered",
					classes: "bg-sky-500/10 text-sky-600 dark:text-sky-400 border-sky-500/20"
				};
			case "DISABLED":
				return {
					label: "Disabled",
					classes: "bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20"
				};
			case "ARCHIVED":
				return {
					label: "Archived",
					classes: "bg-muted text-muted-foreground border-border"
				};
			default:
				return {
					label: status,
					classes: "bg-muted text-muted-foreground border-border"
				};
		}
	}
</script>

<svelte:head>
	<title>{datasource.name} | Datasource Details</title>
	<meta name="description" content={`View and manage datasource settings for ${datasource.name}`} />
</svelte:head>

<div class="p-6 sm:p-8 w-full space-y-6">
	<!-- Navigation & Actions Bar -->
	<div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-5">
		<div class="space-y-1">
			<div class="flex items-center gap-3">
				<a
					href="/datasources"
					class="p-2 rounded-lg border border-border hover:bg-accent transition-colors text-muted-foreground hover:text-foreground"
					title="Back to Datasources"
				>
					<ArrowLeft class="size-4" />
				</a>
				<div>
					<div class="flex items-center gap-2.5">
						<h1 class="text-2xl font-bold tracking-tight">{datasource.name}</h1>
						<span class={`px-2.5 py-0.5 rounded-full text-xs font-medium border ${statusInfo.classes}`}>
							{statusInfo.label}
						</span>
					</div>
				</div>
			</div>
		</div>

		<div class="flex items-center gap-2">
			<Button
				variant="outline"
				onclick={() => (isEditOpen = true)}
				class="h-9 px-3 rounded-lg text-xs font-medium cursor-pointer"
			>
				<Edit class="size-3.5 me-1.5" />
				<span>Edit Parameters</span>
			</Button>

			<Button
				variant="destructive"
				onclick={() => (isDeleteOpen = true)}
				class="h-9 px-3 rounded-lg text-xs font-medium cursor-pointer"
			>
				<Trash2 class="size-3.5 me-1.5" />
				<span>Delete</span>
			</Button>
		</div>
	</div>

	<!-- Alert Feedback -->
	{#if form?.success && form?.message}
		<div class="p-3.5 text-xs text-emerald-600 dark:text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 rounded-xl">
			{form.message}
		</div>
	{/if}

	{#if form?.error}
		<div class="p-3.5 text-xs text-destructive bg-destructive/10 border border-destructive/20 rounded-xl">
			{form.error}
		</div>
	{/if}

	<!-- Overview Grid Section -->
	<div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
		<!-- Specification Details Card -->
		<Card.Root class="lg:col-span-2 rounded-xl border-border bg-card shadow-xs">
			<Card.Header class="pb-3 border-b border-border">
				<Card.Title class="text-base font-bold tracking-tight">Datasource Specifications</Card.Title>
			</Card.Header>
			<Card.Content class="p-5 space-y-4">
				<div class="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
					<div class="p-3 rounded-lg bg-muted/30 border border-border/50 space-y-1">
						<span class="text-muted-foreground">Engine Type</span>
						<p class="font-mono text-sm font-semibold">{datasource.type}</p>
					</div>

					<div class="p-3 rounded-lg bg-muted/30 border border-border/50 space-y-1">
						<span class="text-muted-foreground">Owner</span>
						<p class="font-medium text-sm">@{datasource.owner}</p>
					</div>

					<div class="p-3 rounded-lg bg-muted/30 border border-border/50 space-y-1">
						<span class="text-muted-foreground">Status</span>
						<p class="font-medium text-sm">{datasource.status}</p>
					</div>

					<div class="p-3 rounded-lg bg-muted/30 border border-border/50 space-y-1">
						<span class="text-muted-foreground">Registration Timestamp</span>
						<p class="font-mono text-xs">
							{datasource.registrationDate ? new Date(datasource.registrationDate).toLocaleString() : "N/A"}
						</p>
					</div>
				</div>

				<div class="p-3.5 rounded-lg bg-muted/30 border border-border/50 space-y-1 text-xs">
					<span class="text-muted-foreground">Description</span>
					<p class="text-foreground">{datasource.description || "No description provided."}</p>
				</div>
			</Card.Content>
		</Card.Root>

		<!-- Status Transition Controls Card -->
		<Card.Root class="rounded-xl border-border bg-card shadow-xs">
			<Card.Header class="pb-3 border-b border-border">
				<Card.Title class="text-base font-bold tracking-tight">Status Controls</Card.Title>
			</Card.Header>
			<Card.Content class="p-5 space-y-3">
				<p class="text-xs text-muted-foreground">
					Trigger status transition actions for this datasource entity.
				</p>

				<div class="space-y-2 pt-1">
					<form action="?/changeStatus" method="POST" use:enhance>
						<input type="hidden" name="statusAction" value="activate" />
						<Button
							type="submit"
							variant="outline"
							disabled={datasource.status === "ACTIVE"}
							class="w-full h-9 rounded-lg text-xs font-medium border-emerald-500/30 text-emerald-600 dark:text-emerald-400 hover:bg-emerald-500/10 cursor-pointer justify-start"
						>
							<Play class="size-3.5 me-2" />
							<span>Activate Datasource</span>
						</Button>
					</form>

					<form action="?/changeStatus" method="POST" use:enhance>
						<input type="hidden" name="statusAction" value="disable" />
						<Button
							type="submit"
							variant="outline"
							disabled={datasource.status === "DISABLED"}
							class="w-full h-9 rounded-lg text-xs font-medium border-amber-500/30 text-amber-600 dark:text-amber-400 hover:bg-amber-500/10 cursor-pointer justify-start"
						>
							<Pause class="size-3.5 me-2" />
							<span>Disable Datasource</span>
						</Button>
					</form>

					<form action="?/changeStatus" method="POST" use:enhance>
						<input type="hidden" name="statusAction" value="archive" />
						<Button
							type="submit"
							variant="outline"
							disabled={datasource.status === "ARCHIVED"}
							class="w-full h-9 rounded-lg text-xs font-medium border-border text-muted-foreground hover:bg-accent cursor-pointer justify-start"
						>
							<Archive class="size-3.5 me-2" />
							<span>Archive Datasource</span>
						</Button>
					</form>
				</div>
			</Card.Content>
		</Card.Root>
	</div>

	<!-- Associated Datasets Section -->
	<Card.Root class="rounded-xl border-border bg-card overflow-hidden shadow-xs w-full">
		<Card.Header class="pb-3 border-b border-border">
			<Card.Title class="text-base font-bold tracking-tight">Associated Datasets</Card.Title>
		</Card.Header>
		<Card.Content class="p-0">
			{#if !datasource.datasets || datasource.datasets.length === 0}
				<div class="p-8 text-center text-xs text-muted-foreground space-y-1">
					<p class="font-medium">No datasets currently bound to this datasource.</p>
					<p>Datasets created via API will automatically populate here.</p>
				</div>
			{:else}
				<div class="overflow-x-auto w-full">
					<table class="w-full text-left text-xs border-collapse">
						<thead>
							<tr class="border-b border-border bg-muted/30 font-mono text-muted-foreground">
								<th class="py-3 px-5 font-medium">Dataset Name</th>
								<th class="py-3 px-5 font-medium">Description</th>
								<th class="py-3 px-5 font-medium">Row Count</th>
								<th class="py-3 px-5 font-medium">Dataset ID</th>
							</tr>
						</thead>
						<tbody class="divide-y divide-border/60">
							{#each datasource.datasets as dataset}
								<tr class="hover:bg-accent/30 transition-colors">
									<td class="py-3.5 px-5 font-medium text-foreground">{dataset.name}</td>
									<td class="py-3.5 px-5 text-muted-foreground">{dataset.description || "—"}</td>
									<td class="py-3.5 px-5 font-mono">{dataset.rowCount ? dataset.rowCount.toLocaleString() : "Read-only"}</td>
									<td class="py-3.5 px-5 font-mono text-muted-foreground">{dataset.id}</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
			{/if}
		</Card.Content>
	</Card.Root>
</div>

<!-- Edit Datasource Modal -->
<Dialog.Root bind:open={isEditOpen}>
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
						isEditOpen = false;
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
					onclick={() => (isEditOpen = false)}
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

<!-- Delete Confirmation Dialog -->
<Dialog.Root bind:open={isDeleteOpen}>
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
					onclick={() => (isDeleteOpen = false)}
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
