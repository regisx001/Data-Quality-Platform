<script lang="ts">
	import type { PageData } from "./$types";
	import { Badge } from "$lib/components/ui/badge/index.js";
	import { Button } from "$lib/components/ui/button/index.js";
	import ErrorAlert from "$lib/components/ui/error-alert.svelte";
	import { goto, invalidateAll } from "$app/navigation";

	// Icons matching the dashboard design system
	import Activity from "@lucide/svelte/icons/activity";
	import RefreshCw from "@lucide/svelte/icons/refresh-cw";
	import Server from "@lucide/svelte/icons/server";
	import ShieldAlert from "@lucide/svelte/icons/shield-alert";
	import Clock from "@lucide/svelte/icons/clock";
	import Zap from "@lucide/svelte/icons/zap";
	import Code2 from "@lucide/svelte/icons/code-2";
	import Cpu from "@lucide/svelte/icons/cpu";
	import Tag from "@lucide/svelte/icons/tag";
	import AlertCircle from "@lucide/svelte/icons/alert-circle";
	import TableProperties from "@lucide/svelte/icons/table-properties";
	import ArrowRight from "@lucide/svelte/icons/arrow-right";
	import BarChart3 from "@lucide/svelte/icons/bar-chart-3";

	import * as Tabs from "$lib/components/ui/tabs/index.js";
	import LayoutDashboard from "@lucide/svelte/icons/layout-dashboard";
	import LogVolumeTimeSeriesChart from "$lib/components/dashboard/log-volume-time-series-chart.svelte";
	import LatencyHistogramChart from "$lib/components/dashboard/latency-histogram-chart.svelte";
	import HttpResponseCodeChart from "$lib/components/dashboard/http-response-code-chart.svelte";
	import SeverityPieChart from "$lib/components/dashboard/severity-pie-chart.svelte";

	let { data }: { data: PageData } = $props();

	let activeTab = $state("overview");
	let stats = $derived(data.stats);
	let analytics = $derived(data.analytics);

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
	<title>Observability & Analytics | Data Quality Platform</title>
	<meta name="description" content="View real-time operational metrics, severity distributions, latency percentiles, and traffic analytics." />
</svelte:head>

<div class="p-6 sm:p-8 w-full space-y-6">
	<!-- Page Header -->
	<div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-6">
		<div class="space-y-1">
			<div class="flex items-center gap-2">
				<h1 class="text-2xl font-bold tracking-tight">Observability & Analytics Dashboard</h1>
			</div>
			<p class="text-sm text-muted-foreground">
				High-level platform execution telemetry, microservice health metrics, latency percentiles, and exception signatures.
			</p>
		</div>

		<div class="flex items-center gap-2">
			<!-- Link to raw logs table route -->
			<Button variant="default" size="sm" onclick={() => goto("/logs/table")} class="gap-2">
				<TableProperties class="size-4" />
				<span>Log Explorer Table</span>
				<ArrowRight class="size-3.5 opacity-70" />
			</Button>

			<Button variant="outline" size="sm" onclick={handleRefresh} disabled={isRefreshing} class="gap-2">
				<RefreshCw class={`size-4 ${isRefreshing ? 'animate-spin text-primary' : ''}`} />
				<span>{isRefreshing ? 'Refreshing...' : 'Refresh'}</span>
			</Button>
		</div>
	</div>

	<!-- Metric Summary Widgets Bar -->
	<div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
		<!-- Total Logs -->
		<div class="rounded-lg border border-border bg-card p-5 shadow-xs space-y-2">
			<div class="flex items-center justify-between">
				<span class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Total Ingested Logs</span>
				<Server class="size-4 text-muted-foreground" />
			</div>
			<div class="text-3xl font-bold font-mono tracking-tight">
				{stats?.totalLogs?.toLocaleString() ?? "—"}
			</div>
			<p class="text-xs text-muted-foreground flex items-center gap-1">
				<Clock class="size-3" />
				Ingested via Kafka & stored in PostgreSQL
			</p>
		</div>

		<!-- Error Rate -->
		<div class="rounded-lg border border-border bg-card p-5 shadow-xs space-y-2">
			<div class="flex items-center justify-between">
				<span class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Platform Error Rate</span>
				<ShieldAlert class="size-4 text-destructive" />
			</div>
			<div class="text-3xl font-bold font-mono tracking-tight text-destructive">
				{stats?.errorRatePercentage != null ? `${stats.errorRatePercentage.toFixed(2)}%` : "—"}
			</div>
			<p class="text-xs text-muted-foreground">
				{stats?.errorCount ?? 0} errors / {stats?.warnCount ?? 0} warnings
			</p>
		</div>

		<!-- Avg Execution Latency -->
		<div class="rounded-lg border border-border bg-card p-5 shadow-xs space-y-2">
			<div class="flex items-center justify-between">
				<span class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Avg Execution Latency</span>
				<Zap class="size-4 text-amber-500" />
			</div>
			<div class="text-3xl font-bold font-mono tracking-tight">
				{stats?.averageLatencyMs != null ? `${stats.averageLatencyMs.toFixed(1)} ms` : "—"}
			</div>
			<p class="text-xs text-muted-foreground">
				P95: {analytics?.latency?.p95Ms ? `${analytics.latency.p95Ms} ms` : "N/A"} | P99: {analytics?.latency?.p99Ms ? `${analytics.latency.p99Ms} ms` : "N/A"}
			</p>
		</div>

		<!-- Unique Traces -->
		<div class="rounded-lg border border-border bg-card p-5 shadow-xs space-y-2">
			<div class="flex items-center justify-between">
				<span class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Active Trace Workflows</span>
				<Code2 class="size-4 text-sky-500" />
			</div>
			<div class="text-3xl font-bold font-mono tracking-tight">
				{analytics?.traces?.uniqueTraces ?? "—"}
			</div>
			<p class="text-xs text-muted-foreground">
				Failed traces: {analytics?.traces?.failedTraces ?? 0} ({analytics?.traces?.traceErrorRatePercentage ?? 0}%)
			</p>
		</div>

		<!-- Main View Switcher Tabs (Overview & Stats) -->
		<Tabs.Root value={activeTab} onValueChange={(val) => (activeTab = val)} class="w-full space-y-6 col-span-full">
			<div class="flex items-center justify-between border-b border-border pb-2">
				<Tabs.List class="bg-muted p-1 rounded-lg">
					<Tabs.Trigger value="overview" class="text-xs font-medium px-4 py-1.5 rounded-sm flex items-center gap-2">
						<LayoutDashboard class="size-3.5" />
						<span>Overview</span>
					</Tabs.Trigger>
					<Tabs.Trigger value="stats" class="text-xs font-medium px-4 py-1.5 rounded-sm flex items-center gap-2">
						<BarChart3 class="size-3.5" />
						<span>Stats & Observability</span>
					</Tabs.Trigger>
					<Tabs.Trigger value="errors" class="text-xs font-medium px-4 py-1.5 rounded-sm flex items-center gap-2">
						<AlertCircle class="size-3.5 text-destructive" />
						<span>Error Signatures</span>
					</Tabs.Trigger>
				</Tabs.List>
			</div>

			<!-- TAB 1: OVERVIEW -->
			<Tabs.Content value="overview" class="space-y-6">
				{#if data.analyticsError}
					<ErrorAlert error={data.analyticsError} title="Failed to Fetch Analytics Model" />
				{:else if analytics}
					<div class="space-y-6">
						<!-- Full-Width Top Row: Traffic Volume & Ingestion Time-Series -->
						<div class="w-full">
							<LogVolumeTimeSeriesChart
								timeSeries={analytics.volume.timeSeries}
								totalLogs={analytics.totalLogs}
							/>
						</div>

						<!-- Bottom Row: Latency Percentiles & Severity Distribution -->
						<div class="grid gap-6 md:grid-cols-2">
							<LatencyHistogramChart
								latency={analytics.latency}
							/>

							<SeverityPieChart
								levels={analytics.levels.distribution}
								totalLogs={analytics.totalLogs}
								errorRatePercentage={analytics.levels.errorRatePercentage}
							/>
						</div>
					</div>
				{/if}
			</Tabs.Content>

			<!-- TAB 2: STATS & OBSERVABILITY -->
			<Tabs.Content value="stats" class="space-y-6">
				{#if data.analyticsError}
					<ErrorAlert error={data.analyticsError} title="Failed to Fetch Analytics Model" />
				{:else if analytics}
					<!-- Section 1: Microservice Telemetry Cards Grid -->
					<div class="space-y-4">
						<div class="flex items-center justify-between">
							<div>
								<h3 class="text-base font-bold tracking-tight">Microservices Health & Infrastructure</h3>
								<p class="text-xs text-muted-foreground">Resource usage, log throughput share, and latency SLA metrics by service</p>
							</div>
							<Badge variant="outline" class="font-mono text-xs">
								{analytics.services.length} Monitored Services
							</Badge>
						</div>

						<div class="grid gap-3 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
							{#each analytics.services as svc}
								<div class="rounded-md border border-border bg-card p-3 space-y-2 text-xs">
									<!-- Service Header -->
									<div class="flex items-center justify-between border-b border-border pb-2">
										<div class="flex items-center gap-1.5 truncate">
											<div class={`size-2 shrink-0 rounded-full ${svc.errorRatePercentage > 10 ? 'bg-destructive' : 'bg-emerald-500'}`}></div>
											<span class="font-mono font-bold text-foreground truncate" title={svc.serviceName}>{svc.serviceName}</span>
										</div>
										<Badge variant="outline" class="font-mono text-[10px]">
											{svc.logSharePercentage.toFixed(1)}%
										</Badge>
									</div>

									<!-- Metrics Stats -->
									<div class="space-y-1 font-mono text-[11px] pt-0.5">
										<div class="flex items-center justify-between">
											<span class="text-muted-foreground">Total Logs:</span>
											<span class="font-bold text-foreground">{svc.totalLogs.toLocaleString()}</span>
										</div>
										<div class="flex items-center justify-between">
											<span class="text-muted-foreground">Error Count:</span>
											<span class={`font-bold ${svc.errorCount > 0 ? 'text-destructive' : 'text-emerald-500'}`}>
												{svc.errorCount} ({svc.errorRatePercentage.toFixed(1)}%)
											</span>
										</div>
										<div class="flex items-center justify-between pt-1 border-t border-border/40 text-[10px]">
											<span class="text-muted-foreground">Avg / P95:</span>
											<span class="font-semibold">{svc.averageLatencyMs.toFixed(0)} ms / {svc.p95LatencyMs.toFixed(0)} ms</span>
										</div>
									</div>
								</div>
							{/each}
						</div>
					</div>

					<!-- Section 2: Category Performance & Domain Rollup -->
					<div class="rounded-lg border border-border bg-card p-6 space-y-4 shadow-xs">
						<div class="flex items-center justify-between border-b border-border pb-4">
							<div class="flex items-center gap-2">
								<Tag class="size-4 text-primary" />
								<h3 class="text-base font-bold tracking-tight">Log Categories & Domain Rollup</h3>
							</div>
							<Badge variant="outline" class="text-xs font-mono">
								{analytics.categories.length} Categories
							</Badge>
						</div>

						<div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
							{#each analytics.categories as cat}
								<div class="p-3.5 rounded-md border border-border bg-muted/20 space-y-2">
									<div class="flex items-center justify-between text-xs">
										<span class="font-mono font-bold text-foreground">{cat.category}</span>
										<span class="text-muted-foreground font-mono font-semibold">
											{cat.totalLogs.toLocaleString()} logs ({cat.logSharePercentage.toFixed(1)}%)
										</span>
									</div>

									<div class="w-full h-2 rounded-full bg-muted overflow-hidden">
										<div class="h-full rounded-full bg-primary" style={`width: ${Math.min(cat.logSharePercentage, 100)}%`}></div>
									</div>

									<div class="flex items-center justify-between text-[11px] font-mono text-muted-foreground pt-1">
										<span>Errors: <strong class={cat.errorCount > 0 ? "text-destructive" : "text-emerald-500"}>{cat.errorCount}</strong></span>
										<span>Avg Latency: <strong class="text-foreground">{cat.averageLatencyMs.toFixed(0)} ms</strong></span>
										<span>P95: <strong class="text-foreground">{cat.p95LatencyMs.toFixed(0)} ms</strong></span>
									</div>
								</div>
							{/each}
						</div>
					</div>
				{/if}
			</Tabs.Content>

			<!-- TAB 3: ERROR PATTERNS & SIGNATURES -->
			<Tabs.Content value="errors" class="space-y-6">
				{#if data.analyticsError}
					<ErrorAlert error={data.analyticsError} title="Failed to Fetch Analytics Model" />
				{:else if analytics}
					<div class="rounded-lg border border-border bg-card p-6 space-y-4 shadow-xs">
						<div class="flex items-center justify-between border-b border-border pb-4">
							<div class="space-y-1">
								<div class="flex items-center gap-2">
									<AlertCircle class="size-5 text-destructive" />
									<h3 class="text-lg font-bold tracking-tight">Recurring Exception & Error Signatures</h3>
								</div>
								<p class="text-xs text-muted-foreground">
									Fingerprinted pattern clusters generated by normalizing variable instances (UUIDs, IDs, addresses)
								</p>
							</div>
							<Badge variant="destructive" class="text-xs font-mono px-3 py-1">
								{analytics.errorSignatures.length} Patterns Detected
							</Badge>
						</div>

						<div class="space-y-4">
							{#if analytics.errorSignatures.length === 0}
								<div class="p-12 text-center text-xs text-muted-foreground space-y-2">
									<p class="font-semibold text-foreground text-sm">Clean Operational State</p>
									<p>No recurring exception fingerprints detected within the current time window.</p>
								</div>
							{:else}
								{#each analytics.errorSignatures as sig}
									<div class="p-5 rounded-lg border border-destructive/30 bg-destructive/5 space-y-3">
										<div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-destructive/20 pb-3">
											<span class="font-mono font-bold text-destructive text-sm">{sig.signature}</span>
											<Badge variant="destructive" class="font-mono text-xs w-fit">
												{sig.count} occurrences ({sig.percentage.toFixed(1)}% of total errors)
											</Badge>
										</div>

										<div class="space-y-1.5 font-mono text-xs">
											<span class="text-[11px] font-sans uppercase font-bold text-muted-foreground">Example Message</span>
											<p class="p-3 rounded bg-background border border-border text-foreground font-medium">
												{sig.exampleMessage}
											</p>
										</div>

										{#if sig.exampleStackTrace}
											<div class="space-y-1.5 font-mono text-xs">
												<span class="text-[11px] font-sans uppercase font-bold text-muted-foreground">Exception Stack Trace</span>
												<pre class="p-3 rounded bg-muted/60 border border-border text-muted-foreground overflow-x-auto text-[11px] max-h-48 font-mono leading-relaxed">{sig.exampleStackTrace}</pre>
											</div>
										{/if}
									</div>
								{/each}
							{/if}
						</div>
					</div>
				{/if}
			</Tabs.Content>
		</Tabs.Root>
	</div>
</div>
