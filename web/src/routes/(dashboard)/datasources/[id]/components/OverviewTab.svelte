<script lang="ts">
	import { enhance } from "$app/forms";
	import { Button } from "$lib/components/ui/button/index.js";
	import * as Card from "$lib/components/ui/card/index.js";
	import * as Dialog from "$lib/components/ui/dialog/index.js";
	import * as Field from "$lib/components/ui/field/index.js";
	import { Input } from "$lib/components/ui/input/index.js";
	import Play from "@lucide/svelte/icons/play";
	import Pause from "@lucide/svelte/icons/pause";
	import Archive from "@lucide/svelte/icons/archive";
	import Search from "@lucide/svelte/icons/search";
	import Loader2 from "@lucide/svelte/icons/loader-2";
	import TableProperties from "@lucide/svelte/icons/table-properties";
	import CheckCircle2 from "@lucide/svelte/icons/check-circle-2";
	import Activity from "@lucide/svelte/icons/activity";
	import Upload from "@lucide/svelte/icons/upload";
	import FileText from "@lucide/svelte/icons/file-text";
	import Plus from "@lucide/svelte/icons/plus";
	import Trash2 from "@lucide/svelte/icons/trash-2";
	import { cn } from "$lib/utils";
	import type { Datasource, DatasetDescriptor } from "$lib/server/api";

	let {
		datasource,
		statusInfo,
	}: {
		datasource: Datasource;
		statusInfo: { label: string; classes: string };
	} = $props();

	// Confirmation modal state for status change
	let pendingStatusAction = $state<"activate" | "disable" | "archive" | null>(null);
	let isChangingStatus = $state(false);

	let isDiscoverOpen = $state(false);
	let isDiscovering = $state(false);
	let isImporting = $state(false);
	let discoveredDatasets = $state<DatasetDescriptor[]>([]);
	let selectedDatasetIds = $state<Record<string, boolean>>({});
	let discoveryError = $state<string | null>(null);

	// CSV Dataset Upload & Removing state
	let isUploadCsvOpen = $state(false);
	let isUploadingCsv = $state(false);
	let csvSourceMode = $state<"upload" | "path">("upload");
	let selectedFile = $state<File | null>(null);
	let pendingRemoveDataset = $state<{ id: string; name: string } | null>(null);
	let isRemovingDataset = $state(false);

	function handleFileSelected(event: Event) {
		const target = event.target as HTMLInputElement;
		if (target.files && target.files.length > 0) {
			selectedFile = target.files[0];
		} else {
			selectedFile = null;
		}
	}

	let isCsvDatasource = $derived(datasource.type.toUpperCase() === "CSV");

	function toggleSelectAll(event: Event) {
		const checked = (event.target as HTMLInputElement).checked;
		const updated: Record<string, boolean> = {};
		for (const d of discoveredDatasets) {
			updated[d.id] = checked;
		}
		selectedDatasetIds = updated;
	}

	let selectedCount = $derived(
		Object.values(selectedDatasetIds).filter(Boolean).length
	);

	function getActionDetails(action: "activate" | "disable" | "archive" | null) {
		switch (action) {
			case "activate":
				return {
					title: "Activate Datasource",
					description: `Are you sure you want to activate '${datasource.name}'? This will enable quality checks and automated pipelines for this datasource.`,
					badgeClass: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20",
					buttonClass: "bg-emerald-600 hover:bg-emerald-700 text-white",
					icon: Play
				};
			case "disable":
				return {
					title: "Disable Datasource",
					description: `Are you sure you want to disable '${datasource.name}'? Active checks and connection jobs for this datasource will be temporarily suspended.`,
					badgeClass: "bg-destructive/10 text-destructive border-destructive/20",
					buttonClass: "bg-destructive hover:bg-destructive/90 text-destructive-foreground",
					icon: Pause
				};
			case "archive":
				return {
					title: "Archive Datasource",
					description: `Are you sure you want to archive '${datasource.name}'? Archived datasources will be marked as read-only and deactivated.`,
					badgeClass: "bg-muted text-muted-foreground border-border",
					buttonClass: "bg-zinc-700 hover:bg-zinc-800 text-white dark:bg-zinc-600 dark:hover:bg-zinc-500",
					icon: Archive
				};
			default:
				return null;
		}
	}

	let actionDetails = $derived(getActionDetails(pendingStatusAction));
</script>

<div class="grid grid-cols-1 lg:grid-cols-4 gap-6 items-start">
	<!-- Left Column: Datasets Discovering & Associated Datasets (lg:col-span-3) -->
	<div class="lg:col-span-3 space-y-6">
		<Card.Root class="rounded-xl border-border bg-card overflow-hidden shadow-xs w-full">
			<Card.Header class="pb-3 border-b border-border">
				<div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
					<div>
						<Card.Title class="text-base font-bold tracking-tight flex items-center gap-2">
							<TableProperties class="size-4 text-primary" />
							<span>Associated Datasets</span>
						</Card.Title>
						<Card.Description class="text-xs text-muted-foreground mt-0.5">
							Datasets registered under this {datasource.type} datasource.
						</Card.Description>
					</div>
					{#if isCsvDatasource}
						<Button
							variant="default"
							size="sm"
							onclick={() => (isUploadCsvOpen = true)}
							class="h-8 text-xs gap-1.5 font-medium cursor-pointer shrink-0"
						>
							<Upload class="size-3.5" />
							<span>Upload CSV Dataset</span>
						</Button>
					{:else}
						<Button
							variant="outline"
							size="sm"
							onclick={() => (isDiscoverOpen = true)}
							class="h-8 text-xs gap-1.5 font-medium cursor-pointer shrink-0"
						>
							<Search class="size-3.5" />
							<span>Discover Datasets</span>
						</Button>
					{/if}
				</div>
			</Card.Header>
			<Card.Content class="p-0">
				{#if !datasource.datasets || datasource.datasets.length === 0}
					<div class="p-8 text-center text-xs text-muted-foreground space-y-3">
						<div class="mx-auto w-10 h-10 rounded-full bg-muted flex items-center justify-center">
							<TableProperties class="size-5 text-muted-foreground" />
						</div>
						<div class="space-y-1">
							<p class="font-medium text-foreground">No datasets registered yet</p>
							<p class="max-w-md mx-auto">
								{#if isCsvDatasource}
									Upload a CSV dataset file directly to MinIO object storage to parse and register it under this datasource.
								{:else}
									Scan and register available tables, views, or files from this datasource connection.
								{/if}
							</p>
						</div>
						{#if isCsvDatasource}
							<Button
								variant="default"
								size="sm"
								onclick={() => (isUploadCsvOpen = true)}
								class="h-8 text-xs font-medium cursor-pointer mt-1"
							>
								<Upload class="size-3.5 me-1.5" />
								<span>Upload CSV Dataset</span>
							</Button>
						{:else}
							<Button
								variant="secondary"
								size="sm"
								onclick={() => (isDiscoverOpen = true)}
								class="h-8 text-xs font-medium cursor-pointer mt-1"
							>
								<Search class="size-3.5 me-1.5" />
								<span>Discover & Import Datasets</span>
							</Button>
						{/if}
					</div>
				{:else}
					<div class="overflow-x-auto w-full">
						<table class="w-full text-left text-xs border-collapse">
							<thead>
								<tr class="border-b border-border bg-muted/30 font-mono text-muted-foreground">
									<th class="py-3 px-4 font-medium">Dataset Name</th>
									<th class="py-3 px-4 font-medium">Description</th>
									<th class="py-3 px-4 font-medium">Row Count</th>
									<th class="py-3 px-4 font-medium">Dataset ID</th>
									<th class="py-3 px-4 font-medium text-right">Actions</th>
								</tr>
							</thead>
							<tbody class="divide-y divide-border/60">
								{#each datasource.datasets as dataset}
									<tr class="hover:bg-accent/30 transition-colors">
										<td class="py-3 px-4 font-medium text-foreground">
											<a
												href={`/datasets/${dataset.id}`}
												class="hover:underline font-semibold text-primary inline-flex items-center gap-1.5"
											>
												<TableProperties class="size-3.5 text-primary/70" />
												<span>{dataset.name}</span>
											</a>
										</td>
										<td class="py-3 px-4 text-muted-foreground">{dataset.description || "—"}</td>
										<td class="py-3 px-4 font-mono"
											>{dataset.rowCount ? dataset.rowCount.toLocaleString() : "Read-only"}</td
										>
										<td class="py-3 px-4 font-mono text-muted-foreground text-[11px]">{dataset.id}</td>
										<td class="py-3 px-4 text-right">
											<Button
												type="button"
												variant="ghost"
												size="icon"
												onclick={() => (pendingRemoveDataset = dataset)}
												title={`Remove dataset '${dataset.name}' from datasource`}
												class="h-7 w-7 text-muted-foreground hover:text-destructive hover:bg-destructive/10 cursor-pointer rounded-md"
											>
												<Trash2 class="size-3.5" />
											</Button>
										</td>
									</tr>
								{/each}
							</tbody>
						</table>
					</div>
				{/if}
			</Card.Content>
		</Card.Root>
	</div>

	<!-- Right Column: Status Controls & Actions Aside (lg:col-span-1) -->
	<aside class="space-y-4">
		<Card.Root class="rounded-xl border-border bg-card shadow-xs">
			<Card.Header class="pb-3 border-b border-border">
				<div class="flex items-center justify-between">
					<Card.Title class="text-xs font-bold tracking-tight uppercase text-muted-foreground">Status & Actions</Card.Title>
					<span class={`px-2 py-0.5 rounded-full text-[10px] font-semibold border ${statusInfo.classes}`}>
						{statusInfo.label}
					</span>
				</div>
			</Card.Header>
			<Card.Content class="p-3.5 space-y-3 text-xs">
				<!-- Status Actions -->
				<div class="space-y-1.5">
					<span class="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider block">Lifecycle</span>
					
					<div class="grid grid-cols-1 gap-1.5">
						<!-- Activate Button -->
						<button
							type="button"
							disabled={datasource.status === "ACTIVE"}
							onclick={() => (pendingStatusAction = "activate")}
							class={`w-full flex items-center justify-between p-2 rounded-lg border text-xs font-medium transition-all text-left ${
								datasource.status === "ACTIVE"
									? "bg-emerald-500/10 border-emerald-500/30 text-emerald-600 dark:text-emerald-400 opacity-60 cursor-not-allowed"
									: "border-border hover:border-emerald-500/50 hover:bg-emerald-500/5 text-foreground cursor-pointer"
							}`}
						>
							<div class="flex items-center gap-2">
								<div class={`p-1 rounded ${datasource.status === "ACTIVE" ? "bg-emerald-500/20 text-emerald-500" : "bg-muted text-muted-foreground"}`}>
									<Play class="size-3" />
								</div>
								<span>Activate</span>
							</div>
							{#if datasource.status === "ACTIVE"}
								<span class="text-[10px] font-mono text-emerald-600 dark:text-emerald-400">Current</span>
							{/if}
						</button>

						<!-- Disable Button -->
						<button
							type="button"
							disabled={datasource.status === "DISABLED"}
							onclick={() => (pendingStatusAction = "disable")}
							class={`w-full flex items-center justify-between p-2 rounded-lg border text-xs font-medium transition-all text-left ${
								datasource.status === "DISABLED"
									? "bg-destructive/10 border-destructive/30 text-destructive opacity-60 cursor-not-allowed"
									: "border-border hover:border-destructive/50 hover:bg-destructive/10 text-foreground cursor-pointer"
							}`}
						>
							<div class="flex items-center gap-2">
								<div class={`p-1 rounded ${datasource.status === "DISABLED" ? "bg-destructive/20 text-destructive" : "bg-muted text-muted-foreground"}`}>
									<Pause class="size-3" />
								</div>
								<span>Disable</span>
							</div>
							{#if datasource.status === "DISABLED"}
								<span class="text-[10px] font-mono text-destructive">Current</span>
							{/if}
						</button>

						<!-- Archive Button -->
						<button
							type="button"
							disabled={datasource.status === "ARCHIVED"}
							onclick={() => (pendingStatusAction = "archive")}
							class={`w-full flex items-center justify-between p-2 rounded-lg border text-xs font-medium transition-all text-left ${
								datasource.status === "ARCHIVED"
									? "bg-muted border-border text-muted-foreground opacity-60 cursor-not-allowed"
									: "border-border hover:border-muted-foreground/40 hover:bg-accent text-foreground cursor-pointer"
							}`}
						>
							<div class="flex items-center gap-2">
								<div class="p-1 rounded bg-muted text-muted-foreground">
									<Archive class="size-3" />
								</div>
								<span>Archive</span>
							</div>
							{#if datasource.status === "ARCHIVED"}
								<span class="text-[10px] font-mono text-muted-foreground">Current</span>
							{/if}
						</button>
					</div>
				</div>

				<!-- Quick Shortcuts -->
				<div class="border-t border-border/60 pt-3 space-y-1.5">
					<span class="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider block">Shortcuts</span>
					
					<div class="space-y-1.5">
						<button
							type="button"
							onclick={() => (isDiscoverOpen = true)}
							class="w-full flex items-center gap-2 p-2 rounded-lg border border-border hover:bg-accent hover:text-foreground text-muted-foreground text-xs font-medium transition-all text-left cursor-pointer"
						>
							<Search class="size-3.5 text-primary" />
							<span>Discover Objects</span>
						</button>

						<form action="?/testConnection" method="POST" use:enhance class="w-full">
							<button
								type="submit"
								class="w-full flex items-center gap-2 p-2 rounded-lg border border-border hover:bg-accent hover:text-foreground text-muted-foreground text-xs font-medium transition-all text-left cursor-pointer"
							>
								<Activity class="size-3.5 text-sky-500" />
								<span>Test Connection</span>
							</button>
						</form>
					</div>
				</div>
			</Card.Content>
		</Card.Root>
	</aside>
</div>

<!-- Status Transition Confirmation Modal -->
<Dialog.Root open={!!pendingStatusAction} onOpenChange={(open) => { if (!open) pendingStatusAction = null; }}>
	{#if actionDetails}
		<Dialog.Content class="sm:max-w-md rounded-xl p-6 border-border bg-card">
			<Dialog.Header class="space-y-1">
				<div class="flex items-center gap-2.5">
					<div class={`p-2 rounded-lg border ${actionDetails.badgeClass}`}>
						<actionDetails.icon class="size-4" />
					</div>
					<Dialog.Title class="text-base font-bold tracking-tight">{actionDetails.title}</Dialog.Title>
				</div>
				<Dialog.Description class="text-xs text-muted-foreground pt-1">
					{actionDetails.description}
				</Dialog.Description>
			</Dialog.Header>

			<form
				action="?/changeStatus"
				method="POST"
				use:enhance={() => {
					isChangingStatus = true;
					return async ({ update, result }) => {
						isChangingStatus = false;
						if (result.type === "success") {
							pendingStatusAction = null;
						}
						await update();
					};
				}}
				class="pt-4"
			>
				<input type="hidden" name="statusAction" value={pendingStatusAction} />
				<Dialog.Footer class="flex items-center justify-end gap-2">
					<Button
						type="button"
						variant="outline"
						onclick={() => (pendingStatusAction = null)}
						disabled={isChangingStatus}
						class="h-9 rounded-lg text-xs cursor-pointer"
					>
						Cancel
					</Button>
					<Button
						type="submit"
						disabled={isChangingStatus}
						class={`h-9 rounded-lg text-xs font-medium cursor-pointer ${actionDetails.buttonClass}`}
					>
						{#if isChangingStatus}
							<Loader2 class="size-3.5 me-1.5 animate-spin" />
							<span>Updating...</span>
						{:else}
							<span>Confirm & Proceed</span>
						{/if}
					</Button>
				</Dialog.Footer>
			</form>
		</Dialog.Content>
	{/if}
</Dialog.Root>

<!-- Dataset Discovery Modal -->
<Dialog.Root bind:open={isDiscoverOpen}>
	<Dialog.Content class="sm:max-w-2xl rounded-xl p-6 border-border bg-card">
		<Dialog.Header class="space-y-1">
			<Dialog.Title class="text-lg font-bold tracking-tight flex items-center gap-2">
				<TableProperties class="size-5 text-primary" />
				Discover Datasets from {datasource.name}
			</Dialog.Title>
			<Dialog.Description class="text-xs text-muted-foreground">
				Scan the connected data source ({datasource.type}) to enumerate available tables, views, or files.
			</Dialog.Description>
		</Dialog.Header>

		<div class="space-y-4 pt-3">
			<div class="flex items-center justify-between">
				<form
					action="?/discoverDatasets"
					method="POST"
					use:enhance={() => {
						isDiscovering = true;
						discoveryError = null;
						return async ({ result }) => {
							isDiscovering = false;
							if (result.type === "success") {
								const data = result.data as any;
								discoveredDatasets = data.discoveredDatasets || [];
								const initSelected: Record<string, boolean> = {};
								for (const d of discoveredDatasets) {
									initSelected[d.id] = true;
								}
								selectedDatasetIds = initSelected;
							} else if (result.type === "failure") {
								const data = result.data as any;
								discoveryError = data?.error || "Failed to discover datasets";
							}
						};
					}}
				>
					<Button type="submit" disabled={isDiscovering} class="h-9 px-4 rounded-lg text-xs font-medium cursor-pointer">
						{#if isDiscovering}
							<Loader2 class="size-3.5 me-1.5 animate-spin" />
							<span>Scanning Source...</span>
						{:else}
							<Search class="size-3.5 me-1.5" />
							<span>Scan & Discover Objects</span>
						{/if}
					</Button>
				</form>

				{#if discoveredDatasets.length > 0}
					<span class="text-xs text-muted-foreground">
						Found <strong class="text-foreground">{discoveredDatasets.length}</strong> object(s)
					</span>
				{/if}
			</div>

			{#if discoveryError}
				<div class="p-3 text-xs text-destructive bg-destructive/10 border border-destructive/20 rounded-lg">
					{discoveryError}
				</div>
			{/if}

			{#if discoveredDatasets.length > 0}
				<form
					action="?/importDatasets"
					method="POST"
					use:enhance={() => {
						isImporting = true;
						return async ({ update, result }) => {
							isImporting = false;
							if (result.type === "success") {
								isDiscoverOpen = false;
							}
							await update();
						};
					}}
					class="space-y-4"
				>
					<div class="max-h-72 overflow-y-auto border border-border rounded-lg">
						<table class="w-full text-left text-xs border-collapse">
							<thead class="sticky top-0 bg-muted border-b border-border font-mono text-muted-foreground z-10">
								<tr>
									<th class="py-2.5 px-3 w-10">
										<input
											type="checkbox"
											checked={selectedCount === discoveredDatasets.length && discoveredDatasets.length > 0}
											onchange={toggleSelectAll}
											class="rounded border-border"
										/>
									</th>
									<th class="py-2.5 px-3 font-medium">Object Name / ID</th>
									<th class="py-2.5 px-3 font-medium">Type</th>
									<th class="py-2.5 px-3 font-medium">Est. Rows</th>
									<th class="py-2.5 px-3 font-medium">Description</th>
								</tr>
							</thead>
							<tbody class="divide-y divide-border/60 bg-background">
								{#each discoveredDatasets as item (item.id)}
									<tr class="hover:bg-accent/30 transition-colors">
										<td class="py-2.5 px-3">
											<input
												type="checkbox"
												name="datasetIds"
												value={item.name || item.id}
												bind:checked={selectedDatasetIds[item.id]}
												class="rounded border-border"
											/>
										</td>
										<td class="py-2.5 px-3 font-medium text-foreground">{item.name || item.id}</td>
										<td class="py-2.5 px-3 font-mono">
											<span class="px-2 py-0.5 rounded text-[10px] font-semibold bg-muted border border-border">
												{item.type || "TABLE"}
											</span>
										</td>
										<td class="py-2.5 px-3 font-mono text-muted-foreground">
											{item.rowCount !== undefined && item.rowCount !== null ? item.rowCount.toLocaleString() : "—"}
										</td>
										<td class="py-2.5 px-3 text-muted-foreground">{item.description || "—"}</td>
									</tr>
								{/each}
							</tbody>
						</table>
					</div>

					<Dialog.Footer class="pt-2 flex items-center justify-between gap-2">
						<span class="text-xs text-muted-foreground">
							{selectedCount} object(s) selected
						</span>
						<div class="flex items-center gap-2">
							<Button
								type="button"
								variant="outline"
								onclick={() => (isDiscoverOpen = false)}
								disabled={isImporting}
								class="h-9 rounded-lg text-xs"
							>
								Cancel
							</Button>
							<Button
								type="submit"
								disabled={isImporting || selectedCount === 0}
								class="h-9 rounded-lg font-medium text-xs cursor-pointer"
							>
								{#if isImporting}
									<Loader2 class="size-3.5 me-1.5 animate-spin" />
									<span>Importing...</span>
								{:else}
									<CheckCircle2 class="size-3.5 me-1.5" />
									<span>Import Selected ({selectedCount})</span>
								{/if}
							</Button>
						</div>
					</Dialog.Footer>
				</form>
			{/if}
		</div>
	</Dialog.Content>
</Dialog.Root>

<!-- Upload CSV Dataset Modal (MinIO) -->
<Dialog.Root bind:open={isUploadCsvOpen}>
	<Dialog.Content class="sm:max-w-lg rounded-xl p-6 border-border bg-card">
		<Dialog.Header class="space-y-1">
			<Dialog.Title class="text-lg font-bold tracking-tight flex items-center gap-2">
				<Upload class="size-5 text-primary" />
				Upload CSV Dataset (MinIO)
			</Dialog.Title>
			<Dialog.Description class="text-xs text-muted-foreground">
				Upload a CSV file directly to MinIO object storage to parse and register it under '{datasource.name}'.
			</Dialog.Description>
		</Dialog.Header>

		<form
			action="?/uploadCsvDataset"
			method="POST"
			enctype="multipart/form-data"
			use:enhance={() => {
				isUploadingCsv = true;
				return async ({ update, result }) => {
					isUploadingCsv = false;
					if (result.type === "success") {
						isUploadCsvOpen = false;
						selectedFile = null;
					}
					await update();
				};
			}}
			class="space-y-4 pt-3 text-xs"
		>
			<input type="hidden" name="csvSourceMode" value={csvSourceMode} />

			<!-- Source Mode Switcher -->
			<div class="grid grid-cols-2 gap-2 p-1 bg-muted rounded-md text-xs">
				<button
					type="button"
					class={cn(
						"py-1.5 px-3 rounded-sm font-medium transition-all text-center cursor-pointer",
						csvSourceMode === "upload"
							? "bg-background text-foreground shadow-xs"
							: "text-muted-foreground hover:text-foreground"
					)}
					onclick={() => (csvSourceMode = "upload")}
				>
					Upload CSV File
				</button>
				<button
					type="button"
					class={cn(
						"py-1.5 px-3 rounded-sm font-medium transition-all text-center cursor-pointer",
						csvSourceMode === "path"
							? "bg-background text-foreground shadow-xs"
							: "text-muted-foreground hover:text-foreground"
					)}
					onclick={() => (csvSourceMode = "path")}
				>
					Server File Path
				</button>
			</div>

			{#if csvSourceMode === "upload"}
				<Field.Field>
					<Field.Label for="overview-csv-file-input">Select CSV File</Field.Label>
					<div class="relative flex flex-col items-center justify-center p-5 border-2 border-dashed border-border hover:border-primary/50 rounded-lg transition-colors bg-background/50 text-center cursor-pointer">
						<input
							id="overview-csv-file-input"
							name="csvFile"
							type="file"
							accept=".csv"
							onchange={handleFileSelected}
							disabled={isUploadingCsv}
							class="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
						/>
						{#if selectedFile}
							<div class="flex items-center gap-2 text-primary font-medium text-xs">
								<FileText class="size-5" />
								<span class="truncate max-w-[220px]">{selectedFile.name}</span>
								<span class="text-[10px] text-muted-foreground">({(selectedFile.size / 1024).toFixed(1)} KB)</span>
							</div>
						{:else}
							<div class="flex flex-col items-center gap-1 text-muted-foreground">
								<Upload class="size-6 mb-1 text-primary/70" />
								<span class="text-xs font-medium text-foreground">Click to select CSV dataset</span>
								<span class="text-[11px]">Streams file into MinIO object storage</span>
							</div>
						{/if}
					</div>
				</Field.Field>
			{:else}
				<Field.Field>
					<Field.Label for="overview-csv-filepath">CSV File Path (Server)</Field.Label>
					<Input
						id="overview-csv-filepath"
						name="filePath"
						type="text"
						placeholder="/path/to/dataset.csv"
						disabled={isUploadingCsv}
						class="h-9"
					/>
				</Field.Field>
			{/if}

			<!-- Parsing Options -->
			<div class="grid grid-cols-2 gap-3 pt-1">
				<Field.Field class="space-y-1">
					<Field.Label for="overview-csv-delimiter" class="text-xs">Delimiter</Field.Label>
					<select
						id="overview-csv-delimiter"
						name="delimiter"
						disabled={isUploadingCsv}
						class="w-full h-9 px-2.5 rounded-md border border-input bg-background text-foreground text-xs cursor-pointer"
					>
						<option value=",">Comma (,)</option>
						<option value=";">Semicolon (;)</option>
						<option value="&#9;">Tab (\t)</option>
						<option value="|">Pipe (|)</option>
					</select>
				</Field.Field>

				<Field.Field class="space-y-1">
					<Field.Label for="overview-csv-encoding" class="text-xs">Encoding</Field.Label>
					<select
						id="overview-csv-encoding"
						name="encoding"
						disabled={isUploadingCsv}
						class="w-full h-9 px-2.5 rounded-md border border-input bg-background text-foreground text-xs cursor-pointer"
					>
						<option value="UTF-8">UTF-8</option>
						<option value="ISO-8859-1">ISO-8859-1</option>
						<option value="US-ASCII">US-ASCII</option>
						<option value="Windows-1252">Windows-1252</option>
					</select>
				</Field.Field>
			</div>

			<div class="flex items-center gap-4 text-xs pt-1">
				<label class="flex items-center gap-2 cursor-pointer">
					<input type="checkbox" name="header" value="true" checked disabled={isUploadingCsv} class="rounded border-input text-primary focus:ring-primary size-4" />
					<span>Header Row</span>
				</label>
				<label class="flex items-center gap-2 cursor-pointer">
					<input type="checkbox" name="inferSchema" value="true" checked disabled={isUploadingCsv} class="rounded border-input text-primary focus:ring-primary size-4" />
					<span>Infer Schema</span>
				</label>
			</div>

			<Dialog.Footer class="pt-3 flex items-center justify-end gap-2">
				<Button
					type="button"
					variant="outline"
					onclick={() => (isUploadCsvOpen = false)}
					disabled={isUploadingCsv}
					class="h-9 rounded-lg text-xs"
				>
					Cancel
				</Button>
				<Button
					type="submit"
					disabled={isUploadingCsv || (csvSourceMode === "upload" && !selectedFile)}
					class="h-9 rounded-lg font-medium text-xs cursor-pointer"
				>
					{#if isUploadingCsv}
						<Loader2 class="size-3.5 me-1.5 animate-spin" />
						<span>Uploading to MinIO...</span>
					{:else}
						<Upload class="size-3.5 me-1.5" />
						<span>Upload & Register Dataset</span>
					{/if}
				</Button>
			</Dialog.Footer>
		</form>
	</Dialog.Content>
</Dialog.Root>

<!-- Remove Dataset Confirmation Modal -->
<Dialog.Root open={!!pendingRemoveDataset} onOpenChange={(open) => { if (!open) pendingRemoveDataset = null; }}>
	<Dialog.Content class="sm:max-w-md rounded-xl p-6 border-border bg-card">
		<Dialog.Header class="space-y-2">
			<div class="flex items-center gap-3">
				<div class="w-10 h-10 rounded-full bg-destructive/10 border border-destructive/20 flex items-center justify-center text-destructive shrink-0">
					<Trash2 class="size-5" />
				</div>
				<div>
					<Dialog.Title class="text-base font-bold tracking-tight text-foreground">
						Remove Dataset
					</Dialog.Title>
					<Dialog.Description class="text-xs text-muted-foreground mt-0.5">
						Are you sure you want to remove dataset <span class="font-bold text-foreground">'{pendingRemoveDataset?.name}'</span> from <span class="font-semibold text-foreground">{datasource.name}</span>?
					</Dialog.Description>
				</div>
			</div>
		</Dialog.Header>

		<div class="p-3 text-xs text-muted-foreground bg-muted/40 border border-border rounded-lg space-y-1 my-2">
			<p class="font-medium text-foreground">Dataset Unregistration</p>
			<p>This will remove the dataset registration, its extracted column profiles, and rules from this datasource.</p>
		</div>

		<Dialog.Footer class="pt-2 flex items-center justify-end gap-2">
			<Button
				type="button"
				variant="outline"
				onclick={() => (pendingRemoveDataset = null)}
				disabled={isRemovingDataset}
				class="h-9 rounded-lg text-xs"
			>
				Cancel
			</Button>

			<form
				action="?/deleteDataset"
				method="POST"
				use:enhance={() => {
					isRemovingDataset = true;
					return async ({ update }) => {
						isRemovingDataset = false;
						pendingRemoveDataset = null;
						await update();
					};
				}}
			>
				<input type="hidden" name="datasetId" value={pendingRemoveDataset?.id} />
				<Button
					type="submit"
					disabled={isRemovingDataset}
					class="h-9 rounded-lg text-xs font-medium bg-destructive hover:bg-destructive/90 text-destructive-foreground cursor-pointer"
				>
					{#if isRemovingDataset}
						<Loader2 class="size-3.5 me-1.5 animate-spin" />
						<span>Removing...</span>
					{:else}
						<Trash2 class="size-3.5 me-1.5" />
						<span>Confirm Remove</span>
					{/if}
				</Button>
			</form>
		</Dialog.Footer>
	</Dialog.Content>
</Dialog.Root>
