<script lang="ts">
	import { enhance } from "$app/forms";
	import { Button } from "$lib/components/ui/button/index.js";
	import * as Card from "$lib/components/ui/card/index.js";
	import * as Dialog from "$lib/components/ui/dialog/index.js";
	import Play from "@lucide/svelte/icons/play";
	import Pause from "@lucide/svelte/icons/pause";
	import Archive from "@lucide/svelte/icons/archive";
	import Search from "@lucide/svelte/icons/search";
	import Loader2 from "@lucide/svelte/icons/loader-2";
	import TableProperties from "@lucide/svelte/icons/table-properties";
	import CheckCircle2 from "@lucide/svelte/icons/check-circle-2";
	import type { Datasource, DatasetDescriptor } from "$lib/server/api";

	let {
		datasource,
		statusInfo,
	}: {
		datasource: Datasource;
		statusInfo: { label: string; classes: string };
	} = $props();

	let isDiscoverOpen = $state(false);
	let isDiscovering = $state(false);
	let isImporting = $state(false);
	let discoveredDatasets = $state<DatasetDescriptor[]>([]);
	let selectedDatasetIds = $state<Record<string, boolean>>({});
	let discoveryError = $state<string | null>(null);

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
</script>

<div class="space-y-6">
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
							{datasource.registrationDate
								? new Date(datasource.registrationDate).toLocaleString()
								: "N/A"}
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
				<p class="text-xs text-muted-foreground">Trigger status transition actions for this datasource entity.</p>

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
			<div class="flex items-center justify-between">
				<div>
					<Card.Title class="text-base font-bold tracking-tight">Associated Datasets</Card.Title>
					<Card.Description class="text-xs text-muted-foreground mt-0.5">
						Datasets registered or discovered under this datasource.
					</Card.Description>
				</div>
				<Button
					variant="outline"
					size="sm"
					onclick={() => (isDiscoverOpen = true)}
					class="h-8 text-xs gap-1.5 font-medium cursor-pointer"
				>
					<Search class="size-3.5" />
					<span>Discover Datasets</span>
				</Button>
			</div>
		</Card.Header>
		<Card.Content class="p-0">
			{#if !datasource.datasets || datasource.datasets.length === 0}
				<div class="p-8 text-center text-xs text-muted-foreground space-y-2">
					<p class="font-medium">No datasets currently bound to this datasource.</p>
					<p>Click "Discover Datasets" above to scan and register tables, views, or files from this datasource.</p>
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
									<td class="py-3.5 px-5 font-mono"
										>{dataset.rowCount ? dataset.rowCount.toLocaleString() : "Read-only"}</td
									>
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
