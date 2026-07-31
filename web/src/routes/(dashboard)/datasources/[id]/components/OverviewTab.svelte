<script lang="ts">
	import { enhance } from "$app/forms";
	import { Button } from "$lib/components/ui/button/index.js";
	import * as Card from "$lib/components/ui/card/index.js";
	import Play from "@lucide/svelte/icons/play";
	import Pause from "@lucide/svelte/icons/pause";
	import Archive from "@lucide/svelte/icons/archive";
	import type { Datasource } from "$lib/server/api";

	let {
		datasource,
		statusInfo,
	}: {
		datasource: Datasource;
		statusInfo: { label: string; classes: string };
	} = $props();
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
