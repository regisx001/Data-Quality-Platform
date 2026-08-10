<script lang="ts">
	import type { PageData } from "./$types";
	import { Button } from "$lib/components/ui/button/index.js";
	import ErrorAlert from "$lib/components/ui/error-alert.svelte";
	import { goto, invalidateAll } from "$app/navigation";

	// Icons
	import Globe from "@lucide/svelte/icons/globe";
	import RefreshCw from "@lucide/svelte/icons/refresh-cw";
	import Server from "@lucide/svelte/icons/server";
	import ShieldAlert from "@lucide/svelte/icons/shield-alert";
	import Zap from "@lucide/svelte/icons/zap";
	import CheckCircle2 from "@lucide/svelte/icons/check-circle-2";

	// Chart & Table components
	import HttpResponseCodeChart from "$lib/components/dashboard/http-response-code-chart.svelte";
	import HttpMethodDistributionChart from "$lib/components/dashboard/http-method-distribution-chart.svelte";
	import HttpEndpointSlaTable from "$lib/components/dashboard/http-endpoint-sla-table.svelte";

	let { data }: { data: PageData } = $props();

	let analytics = $derived(data.analytics);
	let http = $derived(analytics && "http" in analytics ? analytics.http : undefined);
	let latency = $derived(analytics && "latency" in analytics ? analytics.latency : undefined);

	let isRefreshing = $state(false);

	async function handleRefresh() {
		isRefreshing = true;
		try {
			await invalidateAll();
		} finally {
			isRefreshing = false;
		}
	}
</script>

<svelte:head>
	<title>HTTP Telemetry & Endpoint Observability | Data Quality Platform</title>
	<meta name="description" content="View specialized HTTP request telemetry, status breakdown, method proportions, and endpoint SLA performance." />
</svelte:head>

<div class="p-6 sm:p-8 w-full space-y-6">
	<!-- Page Header -->
	<div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-6">
		<div class="space-y-1">
			<div class="flex items-center gap-2">
				<Globe class="size-6 text-sky-500" />
				<h1 class="text-2xl font-bold tracking-tight">HTTP Telemetry & Endpoint Observability</h1>
			</div>
			<p class="text-sm text-muted-foreground">
				Specialized HTTP traffic monitoring, status code breakdowns, verb proportions, and endpoint SLA latency metrics.
			</p>
		</div>

		<div class="flex items-center gap-2">
			<Button variant="outline" size="sm" onclick={handleRefresh} disabled={isRefreshing} class="gap-2">
				<RefreshCw class={`size-4 ${isRefreshing ? 'animate-spin text-primary' : ''}`} />
				<span>{isRefreshing ? 'Refreshing...' : 'Refresh Telemetry'}</span>
			</Button>
		</div>
	</div>

	{#if data.analyticsError}
		<ErrorAlert error={data.analyticsError} title="Failed to Fetch HTTP Telemetry" />
	{:else if analytics}
		<!-- Metric KPI Widgets Bar -->
		<div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
			<!-- Total HTTP Requests -->
			<div class="rounded-lg border border-border bg-card p-5 shadow-xs space-y-2">
				<div class="flex items-center justify-between">
					<span class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Total Requests</span>
					<Server class="size-4 text-sky-500" />
				</div>
				<div class="text-3xl font-bold font-mono tracking-tight">
					{http?.totalRequests?.toLocaleString() ?? "—"}
				</div>
				<p class="text-xs text-muted-foreground">
					Across all active service API routes
				</p>
			</div>

			<!-- 2xx Success Rate -->
			<div class="rounded-lg border border-border bg-card p-5 shadow-xs space-y-2">
				<div class="flex items-center justify-between">
					<span class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">2xx Success Rate</span>
					<CheckCircle2 class="size-4 text-emerald-500" />
				</div>
				<div class="text-3xl font-bold font-mono tracking-tight text-emerald-500">
					{http?.rate2xx != null ? `${http.rate2xx.toFixed(1)}%` : "—"}
				</div>
				<p class="text-xs text-muted-foreground">
					{http?.count2xx ?? 0} successful 2xx responses
				</p>
			</div>

			<!-- Error Rate (4xx + 5xx) -->
			<div class="rounded-lg border border-border bg-card p-5 shadow-xs space-y-2">
				<div class="flex items-center justify-between">
					<span class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">HTTP Error Rate</span>
					<ShieldAlert class="size-4 text-destructive" />
				</div>
				<div class="text-3xl font-bold font-mono tracking-tight text-destructive">
					{http ? `${((http.rate4xx || 0) + (http.rate5xx || 0)).toFixed(1)}%` : "—"}
				</div>
				<p class="text-xs text-muted-foreground">
					4xx: {http?.count4xx ?? 0} | 5xx: {http?.count5xx ?? 0}
				</p>
			</div>

			<!-- Avg Request Latency -->
			<div class="rounded-lg border border-border bg-card p-5 shadow-xs space-y-2">
				<div class="flex items-center justify-between">
					<span class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Avg API Latency</span>
					<Zap class="size-4 text-amber-500" />
				</div>
				<div class="text-3xl font-bold font-mono tracking-tight">
					{latency?.averageMs != null ? `${latency.averageMs.toFixed(1)} ms` : "—"}
				</div>
				<p class="text-xs text-muted-foreground">
					P95: {latency?.p95Ms ? `${latency.p95Ms} ms` : "N/A"} | P99: {latency?.p99Ms ? `${latency.p99Ms} ms` : "N/A"}
				</p>
			</div>
		</div>

		<!-- Charts Grid: Status Code Breakdown & Method Distribution -->
		<div class="grid gap-6 md:grid-cols-2">
			<HttpResponseCodeChart http={http} />
			<HttpMethodDistributionChart methodCounts={http?.methodCounts} totalRequests={http?.totalRequests} />
		</div>

		<!-- Endpoint Performance & SLA Table -->
		<HttpEndpointSlaTable endpoints={http?.endpoints} />
	{/if}
</div>
