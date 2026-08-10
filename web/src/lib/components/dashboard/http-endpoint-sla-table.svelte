<script lang="ts">
	import { Badge } from "$lib/components/ui/badge/index.js";
	import { Input } from "$lib/components/ui/input/index.js";
	import SearchIcon from "@lucide/svelte/icons/search";
	import ServerIcon from "@lucide/svelte/icons/server";
	import ShieldAlertIcon from "@lucide/svelte/icons/shield-alert";
	import ZapIcon from "@lucide/svelte/icons/zap";
	import type { EndpointMetrics } from "$lib/server/api";

	let {
		endpoints = []
	}: {
		endpoints?: EndpointMetrics[];
	} = $props();

	let searchQuery = $state("");

	let filteredEndpoints = $derived(
		endpoints.filter((ep) => {
			if (!searchQuery.trim()) return true;
			const q = searchQuery.trim().toLowerCase();
			return (
				ep.path?.toLowerCase().includes(q) ||
				ep.httpMethod?.toLowerCase().includes(q)
			);
		})
	);

	function getMethodBadgeVariant(method: string): "default" | "secondary" | "outline" | "destructive" {
		switch (method?.toUpperCase()) {
			case "GET":
				return "secondary";
			case "POST":
				return "default";
			case "PUT":
				return "outline";
			case "DELETE":
				return "destructive";
			default:
				return "outline";
		}
	}
</script>

<div class="rounded-lg border border-border bg-card shadow-xs">
	<!-- Table Header / Controls -->
	<div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 border-b border-border">
		<div class="space-y-1">
			<div class="flex items-center gap-2">
				<ServerIcon class="size-4 text-emerald-500" />
				<h3 class="text-base font-bold tracking-tight">Endpoint Performance & SLA Metrics</h3>
			</div>
			<p class="text-xs text-muted-foreground">
				Per-endpoint request throughput, 4xx/5xx error rates, and response latency percentiles.
			</p>
		</div>

		<div class="relative w-full sm:w-64">
			<SearchIcon class="absolute left-2.5 top-1/2 -translate-y-1/2 size-3.5 text-muted-foreground" />
			<Input
				type="search"
				placeholder="Search path or method..."
				bind:value={searchQuery}
				class="pl-8 text-xs h-8"
			/>
		</div>
	</div>

	<!-- Data Table Container -->
	<div class="overflow-x-auto">
		<table class="w-full text-left text-xs">
			<thead class="bg-muted/50 border-b border-border font-medium text-muted-foreground uppercase tracking-wider">
				<tr>
					<th class="py-3 px-4">Method</th>
					<th class="py-3 px-4">Path / Route</th>
					<th class="py-3 px-4 text-right">Requests</th>
					<th class="py-3 px-4 text-right">Error Rate</th>
					<th class="py-3 px-4 text-right">Avg Latency</th>
					<th class="py-3 px-4 text-right">P95 Latency</th>
					<th class="py-3 px-4 text-right">Max Latency</th>
				</tr>
			</thead>
			<tbody class="divide-y divide-border/60 font-mono">
				{#if filteredEndpoints.length === 0}
					<tr>
						<td colspan="7" class="py-8 text-center text-muted-foreground font-sans text-xs">
							No HTTP endpoint telemetry matches the search query.
						</td>
					</tr>
				{:else}
					{#each filteredEndpoints as ep}
						<tr class="hover:bg-muted/40 transition-colors">
							<td class="py-3 px-4">
								<Badge variant={getMethodBadgeVariant(ep.httpMethod)} class="text-[10px] font-bold uppercase">
									{ep.httpMethod}
								</Badge>
							</td>
							<td class="py-3 px-4 font-semibold text-foreground max-w-xs truncate" title={ep.path}>
								{ep.path}
							</td>
							<td class="py-3 px-4 text-right font-medium">
								{ep.requestCount.toLocaleString()}
							</td>
							<td class="py-3 px-4 text-right">
								<span class={ep.errorRatePercentage > 0 ? "text-destructive font-bold" : "text-emerald-500"}>
									{ep.errorRatePercentage.toFixed(1)}%
								</span>
							</td>
							<td class="py-3 px-4 text-right">
								{ep.averageLatencyMs.toFixed(1)} ms
							</td>
							<td class="py-3 px-4 text-right font-medium">
								{ep.p95LatencyMs ? `${ep.p95LatencyMs.toFixed(1)} ms` : "N/A"}
							</td>
							<td class="py-3 px-4 text-right text-muted-foreground">
								{ep.maxLatencyMs ? `${ep.maxLatencyMs.toFixed(1)} ms` : "N/A"}
							</td>
						</tr>
					{/each}
				{/if}
			</tbody>
		</table>
	</div>

	<!-- Table Footer -->
	<div class="p-3 border-t border-border bg-muted/20 text-xs text-muted-foreground flex items-center justify-between">
		<span>Showing {filteredEndpoints.length} of {endpoints.length} active routes</span>
		<div class="flex items-center gap-1.5 text-[11px]">
			<ZapIcon class="size-3 text-amber-500" />
			<span>Sorted by highest request throughput</span>
		</div>
	</div>
</div>
