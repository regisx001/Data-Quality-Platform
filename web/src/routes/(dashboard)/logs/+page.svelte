<script lang="ts">
	import { onMount, onDestroy } from "svelte";
	import { goto } from "$app/navigation";
	import { Button } from "$lib/components/ui/button/index.js";
	import * as Card from "$lib/components/ui/card/index.js";

	// Lucide icons matching design system
	import Activity from "@lucide/svelte/icons/activity";
	import RefreshCw from "@lucide/svelte/icons/refresh-cw";
	import Radio from "@lucide/svelte/icons/radio";
	import ShieldAlert from "@lucide/svelte/icons/shield-alert";
	import Clock from "@lucide/svelte/icons/clock";
	import Zap from "@lucide/svelte/icons/zap";
	import Cpu from "@lucide/svelte/icons/cpu";
	import Play from "@lucide/svelte/icons/play";
	import Pause from "@lucide/svelte/icons/pause";
	import Layers from "@lucide/svelte/icons/layers";
	import CheckCircle2 from "@lucide/svelte/icons/check-circle-2";
	import AlertCircle from "@lucide/svelte/icons/alert-circle";
	import TableProperties from "@lucide/svelte/icons/table-properties";
	import BarChart3 from "@lucide/svelte/icons/bar-chart-3";
	import RealtimeSeverityChart from "$lib/components/dashboard/realtime-severity-chart.svelte";
	import RealtimeHistoryChart from "$lib/components/dashboard/realtime-history-chart.svelte";
	import RealtimeLineChart from "$lib/components/dashboard/realtime-line-chart.svelte";

	interface RealtimeMetrics {
		windowStart: string;
		windowEnd: string;
		throughputLogsPerSec: number;
		totalLogsCount: number;
		infoCount: number;
		warnCount: number;
		errorCount: number;
		debugCount: number;
		levelCounts?: Record<string, number>;
		serviceCounts?: Record<string, number>;
		avgExecutionTimeMs: number | null;
		timestamp: string;
	}

	// State
	let connectionStatus = $state<"CONNECTED" | "CONNECTING" | "DISCONNECTED">("CONNECTING");
	let isPaused = $state(false);
	let isTriggeringBatch = $state(false);
	let batchStatusMessage = $state<string | null>(null);

	let history = $state<RealtimeMetrics[]>([]);
	let latestMetrics = $derived(history.length > 0 ? history[history.length - 1] : null);

	// Derived metrics
	let currentThroughput = $derived(latestMetrics?.throughputLogsPerSec ?? 0);
	let currentWindowLogs = $derived(latestMetrics?.totalLogsCount ?? 0);
	let currentErrorCount = $derived(latestMetrics?.errorCount ?? 0);
	let currentWarnCount = $derived(latestMetrics?.warnCount ?? 0);
	let currentInfoCount = $derived(latestMetrics?.infoCount ?? 0);
	let currentAvgLatency = $derived(latestMetrics?.avgExecutionTimeMs ?? 0);
	let errorRatePercentage = $derived(
		currentWindowLogs > 0 ? ((currentErrorCount / currentWindowLogs) * 100).toFixed(1) : "0.0"
	);

	let eventSource: EventSource | null = null;

	onMount(() => {
		fetchInitialHistory();
		connectSse();
	});

	onDestroy(() => {
		disconnectSse();
	});

	async function fetchInitialHistory() {
		try {
			const res = await fetch("/api/v1/logs/stream/history?limit=30");
			if (res.ok) {
				const data: RealtimeMetrics[] = await res.json();
				if (Array.isArray(data) && data.length > 0) {
					history = data;
				}
			}
		} catch (e) {
			console.warn("Could not fetch historical stream snapshots:", e);
		}
	}

	function connectSse() {
		if (eventSource) return;

		connectionStatus = "CONNECTING";
		eventSource = new EventSource("/api/v1/logs/stream");

		eventSource.onopen = () => {
			connectionStatus = "CONNECTED";
		};

		eventSource.addEventListener("CONNECTED", () => {
			connectionStatus = "CONNECTED";
		});

		const handleMetricsMessage = (e: MessageEvent) => {
			if (isPaused) return;

			try {
				const metric: RealtimeMetrics = JSON.parse(e.data);
				connectionStatus = "CONNECTED";
				history = [...history.slice(-29), metric]; // Keep last 30 snapshots
			} catch (err) {
				// Ignore non-json initial connection greetings
			}
		};

		eventSource.onmessage = handleMetricsMessage;
		eventSource.addEventListener("LOG_METRICS_UPDATE", handleMetricsMessage);

		eventSource.onerror = () => {
			connectionStatus = "DISCONNECTED";
			eventSource?.close();
			eventSource = null;
			// Retry connection after 3 seconds
			setTimeout(() => {
				if (!isPaused) connectSse();
			}, 3000);
		};
	}

	function disconnectSse() {
		if (eventSource) {
			eventSource.close();
			eventSource = null;
		}
		connectionStatus = "DISCONNECTED";
	}

	function togglePause() {
		isPaused = !isPaused;
		if (isPaused) {
			disconnectSse();
		} else {
			connectSse();
		}
	}

	async function handleTriggerBatchAggregation() {
		isTriggeringBatch = true;
		batchStatusMessage = null;
		try {
			const res = await fetch("/api/v1/logs/aggregate", {
				method: "POST"
			});
			if (res.ok) {
				const body = await res.json();
				batchStatusMessage = `Batch job triggered! Job ID: ${body.jobId?.slice(0, 8)}...`;
			} else {
				batchStatusMessage = "Failed to trigger batch logs aggregation.";
			}
		} catch (err) {
			batchStatusMessage = "Error connecting to logs service.";
		} finally {
			isTriggeringBatch = false;
			setTimeout(() => {
				batchStatusMessage = null;
			}, 6000);
		}
	}

	function formatTime(isoStr?: string) {
		if (!isoStr) return "—";
		try {
			const d = new Date(isoStr);
			return d.toLocaleTimeString();
		} catch {
			return isoStr;
		}
	}
</script>

<svelte:head>
	<title>Real-Time Log Stream | Data Quality Platform</title>
	<meta name="description" content="Live 5-second tumbling window log metrics and real-time streaming telemetry." />
</svelte:head>

<div class="p-6 sm:p-8 w-full space-y-6">
	<!-- Page Header -->
	<div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-6">
		<div class="space-y-1">
			<div class="flex items-center gap-2">
				<h1 class="text-2xl font-bold tracking-tight">Real-Time Log Analytics Stream</h1>
			</div>
			<p class="text-sm text-muted-foreground">
				Real-time streaming log telemetry and operational window performance metrics.
			</p>
		</div>

		<!-- Action Controls -->
		<div class="flex items-center gap-2">
			<Button variant="outline" size="sm" onclick={togglePause} class="gap-2">
				{#if isPaused}
					<Play class="size-4 text-emerald-500 fill-emerald-500" />
					<span>Resume Stream</span>
				{:else}
					<Pause class="size-4 text-amber-500 fill-amber-500" />
					<span>Pause Stream</span>
				{/if}
			</Button>

			<Button variant="default" size="sm" onclick={handleTriggerBatchAggregation} disabled={isTriggeringBatch} class="gap-2">
				<Zap class={`size-4 ${isTriggeringBatch ? 'animate-spin' : ''}`} />
				<span>{isTriggeringBatch ? 'Triggering...' : 'Trigger Batch Aggregation'}</span>
			</Button>

			<!-- Link to Batch Observability & Analytics -->
			<Button variant="outline" size="sm" onclick={() => goto("/logs/batch")} class="gap-2">
				<BarChart3 class="size-4" />
				<span class="hidden sm:inline">Batch Analytics</span>
			</Button>

			<!-- Link to Raw Log Explorer Table -->
			<Button variant="outline" size="sm" onclick={() => goto("/logs/table")} class="gap-2">
				<TableProperties class="size-4" />
				<span class="hidden sm:inline">Log Explorer</span>
			</Button>
		</div>
	</div>

	<!-- Status Message Toast -->
	{#if batchStatusMessage}
		<div class="p-3.5 rounded-lg border border-primary/20 bg-primary/10 text-primary text-xs font-medium flex items-center justify-between animate-fade-in">
			<div class="flex items-center gap-2">
				<CheckCircle2 class="size-4 shrink-0" />
				<span>{batchStatusMessage}</span>
			</div>
		</div>
	{/if}

	<!-- Metric Summary Widgets Bar -->
	<div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
		<!-- Live Throughput -->
		<div class="rounded-lg border border-border bg-card p-5 shadow-xs space-y-2 relative overflow-hidden">
			<div class="flex items-center justify-between">
				<span class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Live Throughput</span>
				<Radio class="size-4 text-emerald-500" />
			</div>
			<div class="text-3xl font-bold font-mono tracking-tight flex items-baseline gap-1">
				<span>{currentThroughput.toFixed(1)}</span>
				<span class="text-xs text-muted-foreground font-sans font-normal">logs/sec</span>
			</div>
			<p class="text-xs text-muted-foreground flex items-center gap-1">
				<Clock class="size-3" />
				5-second tumbling window
			</p>
		</div>

		<!-- Window Log Volume -->
		<div class="rounded-lg border border-border bg-card p-5 shadow-xs space-y-2">
			<div class="flex items-center justify-between">
				<span class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Current Window Volume</span>
				<Layers class="size-4 text-muted-foreground" />
			</div>
			<div class="text-3xl font-bold font-mono tracking-tight">
				{currentWindowLogs.toLocaleString()}
			</div>
			<p class="text-xs text-muted-foreground flex items-center gap-1">
				<Activity class="size-3" />
				Ingested in last 5 seconds
			</p>
		</div>

		<!-- Window Error Rate -->
		<div class="rounded-lg border border-border bg-card p-5 shadow-xs space-y-2">
			<div class="flex items-center justify-between">
				<span class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Window Error Rate</span>
				<ShieldAlert class={`size-4 ${currentErrorCount > 0 ? 'text-rose-500' : 'text-muted-foreground'}`} />
			</div>
			<div class="flex items-baseline gap-2">
				<div class={`text-3xl font-bold font-mono tracking-tight ${currentErrorCount > 0 ? 'text-rose-600 dark:text-rose-400' : ''}`}>
					{errorRatePercentage}%
				</div>
				<span class="text-xs text-muted-foreground font-mono">({currentErrorCount} errors)</span>
			</div>
			<p class="text-xs text-muted-foreground flex items-center gap-1">
				{#if currentErrorCount > 0}
					<AlertCircle class="size-3 text-rose-500" />
					<span class="text-rose-500 font-medium">Errors detected in active window</span>
				{:else}
					<CheckCircle2 class="size-3 text-emerald-500" />
					<span>No errors in active window</span>
				{/if}
			</p>
		</div>

		<!-- Avg Execution Latency -->
		<div class="rounded-lg border border-border bg-card p-5 shadow-xs space-y-2">
			<div class="flex items-center justify-between">
				<span class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Avg Window Latency</span>
				<Cpu class="size-4 text-muted-foreground" />
			</div>
			<div class="text-3xl font-bold font-mono tracking-tight flex items-baseline gap-1">
				<span>{currentAvgLatency ? currentAvgLatency.toFixed(1) : "—"}</span>
				<span class="text-xs text-muted-foreground font-sans font-normal">ms</span>
			</div>
			<p class="text-xs text-muted-foreground flex items-center gap-1">
				<Zap class="size-3" />
				Execution time across services
			</p>
		</div>
	</div>

	<!-- Full-Width Real-Time Tumbling Window History (Stacked Bar Chart + Legend) -->
	<div class="w-full">
		<RealtimeHistoryChart history={history} />
	</div>

	<!-- Lower Row: Line Chart for Total Volume Trend + Pie Chart for Active Window Severity -->
	<div class="grid gap-6 md:grid-cols-3">
		<div class="md:col-span-2">
			<RealtimeLineChart history={history} />
		</div>
		<div class="md:col-span-1">
			<RealtimeSeverityChart
				infoCount={currentInfoCount}
				warnCount={currentWarnCount}
				errorCount={currentErrorCount}
				totalLogs={currentWindowLogs}
			/>
		</div>
	</div>

	<!-- Live Event Stream Feed Table -->
	<Card.Root class="border-border shadow-xs">
		<Card.Header class="p-6 pb-4">
			<div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
				<div>
					<Card.Title class="text-base font-bold tracking-tight">Live Stream Event Feed</Card.Title>
					<Card.Description class="text-xs text-muted-foreground mt-0.5">
						Real-time stream log snapshots captured every 5 seconds.
					</Card.Description>
				</div>
				<div class="flex items-center gap-2">
					<span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-mono bg-accent/60 text-muted-foreground border border-border">
						<span class="size-1.5 rounded-full bg-emerald-500"></span>
						{history.length} snapshots buffered
					</span>
				</div>
			</div>
		</Card.Header>

		<Card.Content class="p-6 pt-0">
			<!-- Padded, Rounded Table Container -->
			<div class="rounded-xl border border-border/80 bg-card overflow-hidden shadow-2xs">
				<div class="overflow-x-auto">
					<table class="w-full text-xs text-left border-collapse">
						<thead class="bg-muted/50 dark:bg-muted/30 text-muted-foreground uppercase text-[10px] font-bold tracking-wider border-b border-border/80">
							<tr>
								<th class="py-3.5 px-5">Window End Time</th>
								<th class="py-3.5 px-5">Throughput</th>
								<th class="py-3.5 px-5">Total Volume</th>
								<th class="py-3.5 px-5">Severity Distribution</th>
								<th class="py-3.5 px-5">Avg Latency</th>
								<th class="py-3.5 px-5 text-right">Received At</th>
							</tr>
						</thead>
						<tbody class="divide-y divide-border/60">
							{#if history.length === 0}
								<tr>
									<td colspan="6" class="py-12 text-center text-muted-foreground">
										<div class="flex flex-col items-center justify-center gap-2">
											<div class="size-10 rounded-full bg-accent flex items-center justify-center text-muted-foreground">
												<Clock class="size-5" />
											</div>
											<p class="text-sm font-medium text-foreground">Waiting for live stream data...</p>
											<p class="text-xs text-muted-foreground max-w-sm">
												Real-time log metric snapshots will automatically populate here as log tumbling windows process.
											</p>
										</div>
									</td>
								</tr>
							{:else}
								{#each [...history].reverse() as snapshot, idx}
									<tr class={`transition-colors hover:bg-accent/40 ${idx === 0 ? 'bg-primary/5 dark:bg-primary/10' : ''}`}>
										<!-- Window End -->
										<td class="py-3.5 px-5 font-mono text-foreground font-medium">
											<div class="flex items-center gap-2.5">
												{#if idx === 0}
													<span class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-sans font-semibold uppercase bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/25">
														<span class="size-1.5 rounded-full bg-emerald-500"></span>
														Latest
													</span>
												{/if}
												<span>{formatTime(snapshot.windowEnd)}</span>
											</div>
										</td>

										<!-- Throughput -->
										<td class="py-3.5 px-5 font-mono">
											<span class="font-bold text-emerald-600 dark:text-emerald-400">
												{snapshot.throughputLogsPerSec.toFixed(1)}
											</span>
											<span class="text-[11px] text-muted-foreground ml-0.5">logs/s</span>
										</td>

										<!-- Total Logs -->
										<td class="py-3.5 px-5 font-mono text-foreground font-semibold">
											{snapshot.totalLogsCount.toLocaleString()}
										</td>

										<!-- Level Breakdown -->
										<td class="py-3.5 px-5">
											<div class="flex items-center gap-1.5 font-sans">
												<span class="inline-flex items-center text-[10px] py-0.5 px-2 rounded font-mono font-medium bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20">
													INFO {snapshot.infoCount}
												</span>
												{#if snapshot.warnCount > 0}
													<span class="inline-flex items-center text-[10px] py-0.5 px-2 rounded font-mono font-medium bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
														WARN {snapshot.warnCount}
													</span>
												{/if}
												{#if snapshot.errorCount > 0}
													<span class="inline-flex items-center text-[10px] py-0.5 px-2 rounded font-mono font-bold bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/20">
														ERR {snapshot.errorCount}
													</span>
												{/if}
											</div>
										</td>

										<!-- Avg Latency -->
										<td class="py-3.5 px-5 font-mono text-muted-foreground">
											{snapshot.avgExecutionTimeMs ? `${snapshot.avgExecutionTimeMs.toFixed(1)} ms` : '—'}
										</td>

										<!-- Timestamp -->
										<td class="py-3.5 px-5 text-right font-mono text-muted-foreground text-[11px]">
											{formatTime(snapshot.timestamp)}
										</td>
									</tr>
								{/each}
							{/if}
						</tbody>
					</table>
				</div>
			</div>
		</Card.Content>
	</Card.Root>
</div>
