<script lang="ts">
	import { scaleBand } from "d3-scale";
	import { BarChart, Highlight } from "layerchart";
	import { cubicInOut } from "svelte/easing";
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

	function formatTime(isoStr?: string) {
		if (!isoStr) return "";
		try {
			const d = new Date(isoStr);
			return d.toLocaleTimeString([], {
				hour: "2-digit",
				minute: "2-digit",
				second: "2-digit",
			});
		} catch {
			return isoStr;
		}
	}

	let chartData = $derived(
		history.map((point) => ({
			time: formatTime(point.windowEnd),
			info: point.infoCount,
			warn: point.warnCount,
			error: point.errorCount,
			total: point.totalLogsCount,
		})),
	);

	const chartConfig = {
		info: { label: "INFO", color: "var(--chart-1)" },
		warn: { label: "WARN", color: "var(--chart-2)" },
		error: { label: "ERROR", color: "var(--chart-5)" },
	} satisfies Chart.ChartConfig;
</script>

<Card.Root class="w-full border-border shadow-xs flex flex-col">
	<Card.Header class="pb-2">
		<Card.Title
			class="text-base font-semibold flex items-center justify-between"
		>
			<div class="flex items-center gap-2">
				<Activity class="size-4 text-primary" />
				<span>Real-Time Tumbling Window History</span>
			</div>
			<span
				class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-mono border border-border bg-accent/40 font-medium"
			>
				{history.length} snapshots
			</span>
		</Card.Title>
		<Card.Description class="text-xs">
			Rolling 5-second tumbling window volume stacked by severity level.
		</Card.Description>
	</Card.Header>

	<Card.Content class="flex-1 p-4">
		{#if history.length === 0}
			<div
				class="h-64 w-full flex items-center justify-center text-xs text-muted-foreground"
			>
				Waiting for real-time log stream data...
			</div>
		{:else}
			<Chart.Container config={chartConfig} class="h-64 w-full">
				<BarChart
					data={chartData}
					xScale={scaleBand().padding(0.25)}
					x="time"
					axis="x"
					rule={false}
					series={[
						{
							key: "info",
							label: "INFO",
							color: chartConfig.info.color,
							props: { rounded: "bottom" },
						},
						{
							key: "warn",
							label: "WARN",
							color: chartConfig.warn.color,
						},
						{
							key: "error",
							label: "ERROR",
							color: chartConfig.error.color,
							props: { rounded: "top" },
						},
					]}
					seriesLayout="stack"
					props={{
						bars: {
							stroke: "none",
							motion: {
								type: "tween",
								duration: 500,
								easing: cubicInOut,
							},
						},
						highlight: { area: false },
						xAxis: {
							tickLength: 0,
							ticks: (scale) => {
								const domain = (
									scale as { domain?: () => string[] }
								).domain;
								const all = domain
									? domain()
									: (chartData.map(
											(d) => d.time,
										) as string[]);
								const totalTicks =
									all.length || chartData.length || 30;
								const step = Math.max(
									Math.floor(totalTicks / 5),
									1,
								);
								return all.filter(
									(_, i) =>
										i % step === 0 || i === totalTicks - 1,
								);
							},
						},
					}}
					legend
				>
					{#snippet belowMarks()}
						<Highlight area={{ class: "fill-muted" }} />
					{/snippet}

					{#snippet tooltip()}
						<Chart.Tooltip />
					{/snippet}
				</BarChart>
			</Chart.Container>
		{/if}
	</Card.Content>
</Card.Root>
