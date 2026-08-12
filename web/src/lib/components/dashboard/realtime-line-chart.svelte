<script lang="ts">
	import { scaleUtc } from "d3-scale";
	import { curveNatural } from "d3-shape";
	import { LineChart } from "layerchart";
	import * as Card from "$lib/components/ui/card/index.js";
	import * as Chart from "$lib/components/ui/chart/index.js";
	import Activity from "@lucide/svelte/icons/activity";

	interface RealtimeMetrics {
		windowStart: string;
		windowEnd: string;
		throughputLogsPerSec: number;
		totalLogsCount: number;
		infoCount: number;
		warnCount: number;
		errorCount: number;
		debugCount: number;
		timestamp: string;
	}

	let { history = [] }: { history?: RealtimeMetrics[] } = $props();

	let chartData = $derived(
		history.map((point) => ({
			date: new Date(point.windowEnd),
			total: point.totalLogsCount,
			throughput: point.throughputLogsPerSec
		}))
	);

	const chartConfig = {
		total: { label: "Total Logs", color: "var(--chart-1)" }
	} satisfies Chart.ChartConfig;
</script>

<Card.Root class="w-full border-border shadow-xs flex flex-col">
	<Card.Header class="pb-2">
		<Card.Title class="text-base font-semibold flex items-center justify-between">
			<div class="flex items-center gap-2">
				<Activity class="size-4 text-primary" />
				<span>Overall Log Volume Trend</span>
			</div>
			<span class="text-xs font-mono font-bold text-muted-foreground">{history.length} snapshots</span>
		</Card.Title>
		<Card.Description class="text-xs">
			Time-series ingestion volume trend across rolling tumbling windows.
		</Card.Description>
	</Card.Header>

	<Card.Content class="flex-1 p-4">
		{#if history.length === 0}
			<div class="h-64 w-full flex items-center justify-center text-xs text-muted-foreground">
				Waiting for real-time log stream data...
			</div>
		{:else}
			<Chart.Container config={chartConfig} class="h-64 w-full">
				<LineChart
					data={chartData}
					x="date"
					xScale={scaleUtc()}
					axis="x"
					series={[
						{
							key: "total",
							label: "Total Logs",
							color: chartConfig.total.color
						}
					]}
					props={{
						spline: { curve: curveNatural, motion: "tween", strokeWidth: 2 },
						xAxis: {
							ticks: 5,
							tickLength: 0,
							format: (v: Date) => v.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
						},
						highlight: { points: { r: 4 } }
					}}
				>
					{#snippet tooltip()}
						<Chart.Tooltip hideLabel />
					{/snippet}
				</LineChart>
			</Chart.Container>
		{/if}
	</Card.Content>
</Card.Root>
