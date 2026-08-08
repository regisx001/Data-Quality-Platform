<script lang="ts">
	import type { PageData } from "./$types";
	import { Badge } from "$lib/components/ui/badge/index.js";
	import { Button } from "$lib/components/ui/button/index.js";
	import ErrorAlert from "$lib/components/ui/error-alert.svelte";
	import { goto } from "$app/navigation";

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

			<Button variant="outline" size="sm" onclick={() => goto("/logs")} class="gap-2">
				<RefreshCw class="size-4" />
				<span>Refresh</span>
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
				</Tabs.List>
			</div>

			<!-- TAB 1: OVERVIEW -->
			<Tabs.Content value="overview" class="space-y-6">
				{#if data.analyticsError}
					<ErrorAlert error={data.analyticsError} title="Failed to Fetch Analytics Model" />
				{:else if analytics}
					<div class="grid gap-6 md:grid-cols-2">
						<LogVolumeTimeSeriesChart
							timeSeries={analytics.volume.timeSeries}
							totalLogs={analytics.totalLogs}
						/>

						<LatencyHistogramChart
							latency={analytics.latency}
						/>

						<HttpResponseCodeChart
							http={analytics.http}
						/>

						<SeverityPieChart
							levels={analytics.levels.distribution}
							totalLogs={analytics.totalLogs}
							errorRatePercentage={analytics.levels.errorRatePercentage}
						/>
					</div>
				{/if}
			</Tabs.Content>

			<!-- TAB 2: STATS & OBSERVABILITY -->
			<Tabs.Content value="stats" class="space-y-6">
				{#if data.analyticsError}
					<ErrorAlert error={data.analyticsError} title="Failed to Fetch Analytics Model" />
				{:else if analytics}
					<!-- Section 1: Microservices Performance & Severity Bar Charts -->
					<div class="grid gap-6 md:grid-cols-2">
						<!-- Microservices Health & Share -->
						<div class="rounded-lg border border-border bg-card p-6 space-y-4 shadow-xs">
							<div class="flex items-center justify-between border-b border-border pb-4">
								<div class="flex items-center gap-2">
									<Cpu class="size-4 text-primary" />
									<h3 class="text-sm font-bold tracking-tight">Microservices Telemetry & Latency</h3>
								</div>
								<Badge variant="outline" class="text-[10px] font-mono">
									{analytics.services.length} Services Monitored
								</Badge>
							</div>

							<div class="space-y-4">
								{#each analytics.services as svc}
									<div class="p-4 rounded-md border border-border bg-muted/20 space-y-3">
										<div class="flex items-center justify-between">
											<span class="font-mono text-xs font-bold text-foreground">{svc.serviceName}</span>
											<Badge variant="outline" class="font-mono text-[10px] bg-background">
												{svc.logSharePercentage.toFixed(1)}% Volume Share
											</Badge>
										</div>

										<!-- Volume Progress Bar -->
										<div class="space-y-1">
											<div class="flex justify-between text-[11px] text-muted-foreground font-mono">
												<span>Traffic Volume Share</span>
												<span>{svc.totalLogs} logs</span>
											</div>
											<div class="w-full h-2 rounded-full bg-muted overflow-hidden">
												<div class="h-full rounded-full bg-primary" style={`width: ${Math.min(svc.logSharePercentage, 100)}%`}></div>
											</div>
										</div>

										<!-- Metrics grid -->
										<div class="grid grid-cols-3 gap-2 pt-2 border-t border-border/50 text-xs font-mono">
											<div>
												<span class="text-[10px] text-muted-foreground block uppercase font-sans">Error Rate</span>
												<span class={`font-bold ${svc.errorRatePercentage > 10 ? 'text-destructive' : 'text-emerald-500'}`}>
													{svc.errorRatePercentage.toFixed(1)}%
												</span>
											</div>
											<div>
												<span class="text-[10px] text-muted-foreground block uppercase font-sans">Avg Latency</span>
												<span class="font-bold text-amber-500">{svc.averageLatencyMs.toFixed(0)} ms</span>
											</div>
											<div>
												<span class="text-[10px] text-muted-foreground block uppercase font-sans">P95 / P99</span>
												<span class="font-bold">{svc.p95LatencyMs.toFixed(0)} / {svc.p99LatencyMs.toFixed(0)} ms</span>
											</div>
										</div>
									</div>
								{/each}
							</div>
						</div>

						<!-- Log Severity Distribution Breakdown -->
						<div class="rounded-lg border border-border bg-card p-6 space-y-4 shadow-xs">
							<div class="flex items-center justify-between border-b border-border pb-4">
								<div class="flex items-center gap-2">
									<Tag class="size-4 text-primary" />
									<h3 class="text-sm font-bold tracking-tight">Log Severity Distribution & Ratios</h3>
								</div>
								<Badge variant="outline" class="text-[10px] font-mono">
									Total: {analytics.totalLogs}
								</Badge>
							</div>

							<div class="space-y-4">
								{#each analytics.levels.distribution as item}
									<div class="space-y-1.5 p-2 rounded-md hover:bg-muted/30 transition-colors">
										<div class="flex items-center justify-between text-xs font-mono">
											<span class="font-bold flex items-center gap-2">
												<span class={`size-2.5 rounded-full ${item.level === 'FATAL' || item.level === 'ERROR' ? 'bg-destructive' : item.level === 'WARN' ? 'bg-amber-500' : 'bg-primary'}`}></span>
												{item.level}
											</span>
											<span class="text-muted-foreground font-semibold">{item.count} events ({item.percentage.toFixed(1)}%)</span>
										</div>

										<!-- Visual bar -->
										<div class="w-full h-2.5 rounded-full bg-muted overflow-hidden">
											<div
												class={`h-full rounded-full transition-all ${item.level === 'FATAL' || item.level === 'ERROR' ? 'bg-destructive' : item.level === 'WARN' ? 'bg-amber-500' : item.level === 'INFO' ? 'bg-emerald-500' : 'bg-sky-500'}`}
												style={`width: ${Math.min(item.percentage, 100)}%`}
											></div>
										</div>
									</div>
								{/each}
							</div>
						</div>
					</div>

					<!-- Section 2: HTTP Metrics & Error Signatures -->
					<div class="grid gap-6 md:grid-cols-2">
						<!-- HTTP Request Status Rates -->
						<div class="rounded-lg border border-border bg-card p-6 space-y-4 shadow-xs">
							<div class="flex items-center justify-between border-b border-border pb-4">
								<div class="flex items-center gap-2">
									<Server class="size-4 text-emerald-500" />
									<h3 class="text-sm font-bold tracking-tight">HTTP API Request Status Distribution</h3>
								</div>
								<Badge variant="outline" class="text-[10px] font-mono">
									{analytics.http.totalRequests} Requests
								</Badge>
							</div>

							<!-- Rate metrics grid -->
							<div class="grid grid-cols-4 gap-2.5 text-center">
								<div class="p-3 rounded-md bg-emerald-500/10 border border-emerald-500/20">
									<span class="text-[10px] font-semibold text-emerald-600 dark:text-emerald-400 block uppercase">2xx OK</span>
									<span class="font-mono text-lg font-bold text-emerald-500">{analytics.http.rate2xx.toFixed(1)}%</span>
								</div>
								<div class="p-3 rounded-md bg-sky-500/10 border border-sky-500/20">
									<span class="text-[10px] font-semibold text-sky-600 dark:text-sky-400 block uppercase">3xx Redir</span>
									<span class="font-mono text-lg font-bold text-sky-500">{analytics.http.rate3xx.toFixed(1)}%</span>
								</div>
								<div class="p-3 rounded-md bg-amber-500/10 border border-amber-500/20">
									<span class="text-[10px] font-semibold text-amber-600 dark:text-amber-400 block uppercase">4xx Client</span>
									<span class="font-mono text-lg font-bold text-amber-500">{analytics.http.rate4xx.toFixed(1)}%</span>
								</div>
								<div class="p-3 rounded-md bg-destructive/10 border border-destructive/20">
									<span class="text-[10px] font-semibold text-destructive block uppercase">5xx Server</span>
									<span class="font-mono text-lg font-bold text-destructive">{analytics.http.rate5xx.toFixed(1)}%</span>
								</div>
							</div>

							<!-- Monitored Endpoints -->
							<div class="space-y-2 pt-2">
								<span class="text-xs font-semibold text-muted-foreground block uppercase tracking-wider">Top Monitored API Paths</span>
								<div class="space-y-2">
									{#each analytics.http.endpoints as ep}
										<div class="flex items-center justify-between p-3 rounded-md border border-border bg-muted/20 text-xs font-mono">
											<div class="flex items-center gap-2 truncate">
												<Badge variant="outline" class="font-bold text-primary bg-background">
													{ep.httpMethod}
												</Badge>
												<span class="truncate text-foreground font-semibold">{ep.path}</span>
											</div>
											<span class="text-muted-foreground whitespace-nowrap">
												{ep.requestCount} reqs ({ep.averageLatencyMs.toFixed(0)} ms avg)
											</span>
										</div>
									{/each}
								</div>
							</div>
						</div>

						<!-- Top Error Signatures -->
						<div class="rounded-lg border border-border bg-card p-6 space-y-4 shadow-xs">
							<div class="flex items-center justify-between border-b border-border pb-4">
								<div class="flex items-center gap-2">
									<AlertCircle class="size-4 text-destructive" />
									<h3 class="text-sm font-bold tracking-tight">Top Fingerprinted Error Signatures</h3>
								</div>
								<Badge variant="destructive" class="text-[10px] font-mono">
									{analytics.errorSignatures.length} Patterns
								</Badge>
							</div>

							<div class="space-y-3">
								{#if analytics.errorSignatures.length === 0}
									<div class="p-8 text-center text-xs text-muted-foreground space-y-1">
										<p class="font-semibold text-foreground">Clean Health Window</p>
										<p>No recurring exception fingerprints detected.</p>
									</div>
								{:else}
									{#each analytics.errorSignatures as sig}
										<div class="p-3.5 rounded-md border border-destructive/25 bg-destructive/5 space-y-2">
											<div class="flex items-center justify-between text-xs">
												<span class="font-mono font-bold text-destructive truncate">{sig.signature}</span>
												<Badge variant="destructive" class="font-mono text-[10px]">
													{sig.count} occurrences
												</Badge>
											</div>
											<p class="text-xs font-mono text-muted-foreground truncate" title={sig.exampleMessage}>
												{sig.exampleMessage}
											</p>
										</div>
									{/each}
								{/if}
							</div>
						</div>
					</div>
				{/if}
			</Tabs.Content>
		</Tabs.Root>
	</div>
</div>
