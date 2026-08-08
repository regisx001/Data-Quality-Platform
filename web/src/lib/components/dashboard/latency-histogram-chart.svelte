<script lang="ts">
	import { BarChart, Highlight } from "layerchart";
	import { cubicInOut } from "svelte/easing";
	import * as Card from "$lib/components/ui/card/index.js";
	import * as Chart from "$lib/components/ui/chart/index.js";
	import type { LatencyMetrics } from "$lib/server/api";

	let { latency }: { latency?: LatencyMetrics } = $props();

	// Interactive tabs for latency metrics
	const chartConfig = {
		latency: { label: "Latency (ms)", color: "var(--chart-2, #10b981)" }
	} satisfies Chart.ChartConfig;

	type MetricKey = "percentiles" | "tail";
	let activeTab = $state<MetricKey>("percentiles");

	// Percentiles data (P50, P75, P90, P95, P99, P99.9)
	let percentilesData = $derived([
		{ metric: "P50 (Median)", latencyMs: latency?.p50Ms ?? latency?.medianMs ?? 0 },
		{ metric: "P75", latencyMs: latency?.p75Ms ?? 0 },
		{ metric: "P90", latencyMs: latency?.p90Ms ?? 0 },
		{ metric: "P95", latencyMs: latency?.p95Ms ?? 0 },
		{ metric: "P99", latencyMs: latency?.p99Ms ?? 0 },
		{ metric: "P99.9", latencyMs: latency?.p999Ms ?? 0 }
	]);

	// Tail latency comparison (Min, Avg, Median, Max)
	let tailData = $derived([
		{ metric: "Min Latency", latencyMs: latency?.minMs ?? 0 },
		{ metric: "Median (P50)", latencyMs: latency?.medianMs ?? 0 },
		{ metric: "Average Latency", latencyMs: latency?.averageMs ?? 0 },
		{ metric: "Max Latency", latencyMs: latency?.maxMs ?? 0 }
	]);

	let activeData = $derived(activeTab === "percentiles" ? percentilesData : tailData);

	let activeSeries = $derived([
		{
			key: "latencyMs",
			label: "Latency (ms)",
			color: "var(--chart-2, #10b981)"
		}
	]);
</script>

<Card.Root class="rounded-lg border border-border bg-card shadow-xs">
	<Card.Header class="flex flex-col items-stretch space-y-0 border-b border-border p-0 sm:flex-row">
		<div class="flex flex-1 flex-col justify-center gap-1 px-6 py-5 sm:py-6">
			<Card.Title class="text-base font-bold tracking-tight">Execution Latency Percentiles</Card.Title>
			<Card.Description class="text-xs text-muted-foreground">
				Response time distribution & tail latency metrics ({latency?.sampleCount ?? 0} samples)
			</Card.Description>
		</div>

		<div class="flex border-t sm:border-t-0 sm:border-l border-border">
			<button
				type="button"
				data-active={activeTab === "percentiles"}
				class="relative z-30 flex flex-1 flex-col justify-center gap-0.5 border-r border-border px-5 py-4 text-start data-[active=true]:bg-muted/50 transition-colors cursor-pointer"
				onclick={() => (activeTab = "percentiles")}
			>
				<span class="text-[11px] font-medium text-muted-foreground uppercase">
					P95 / P99 Tail
				</span>
				<span class="text-lg font-bold font-mono text-emerald-500">
					{latency?.p95Ms ? `${latency.p95Ms.toFixed(0)} ms` : "—"}
				</span>
			</button>

			<button
				type="button"
				data-active={activeTab === "tail"}
				class="relative z-30 flex flex-1 flex-col justify-center gap-0.5 px-5 py-4 text-start data-[active=true]:bg-muted/50 transition-colors cursor-pointer"
				onclick={() => (activeTab = "tail")}
			>
				<span class="text-[11px] font-medium text-muted-foreground uppercase">
					Max Latency
				</span>
				<span class="text-lg font-bold font-mono text-amber-500">
					{latency?.maxMs ? `${latency.maxMs.toFixed(0)} ms` : "—"}
				</span>
			</button>
		</div>
	</Card.Header>

	<Card.Content class="px-2 sm:p-6">
		<Chart.Container config={chartConfig} class="aspect-auto h-[250px] w-full">
			<BarChart
				data={activeData}
				x="metric"
				axis="x"
				series={activeSeries}
				props={{
					bars: {
						stroke: "none",
						rounded: "top",
						fill: "var(--chart-2, #10b981)",
						motion: { type: "tween", duration: 500, easing: cubicInOut }
					},
					highlight: { area: { fill: "none" } }
				}}
			>
				{#snippet belowMarks()}
					<Highlight area={{ class: "fill-muted/40" }} />
				{/snippet}
				{#snippet tooltip()}
					<Chart.Tooltip
						labelFormatter={(v: string) => `Metric: ${v}`}
					/>
				{/snippet}
			</BarChart>
		</Chart.Container>
	</Card.Content>
</Card.Root>
