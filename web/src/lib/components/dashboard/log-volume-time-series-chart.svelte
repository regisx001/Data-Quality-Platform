<script lang="ts">
	import { scaleUtc } from "d3-scale";
	import { curveNatural } from "d3-shape";
	import { Area, AreaChart, ChartClipPath } from "layerchart";
	import { cubicInOut } from "svelte/easing";
	import * as Card from "$lib/components/ui/card/index.js";
	import * as Chart from "$lib/components/ui/chart/index.js";
	import * as Select from "$lib/components/ui/select/index.js";
	import ChartContainer from "$lib/components/ui/chart/chart-container.svelte";
	import type { TimeSeriesBucket } from "$lib/server/api";

	let { timeSeries = [], totalLogs = 0 }: { timeSeries?: TimeSeriesBucket[]; totalLogs?: number } = $props();

	let timeRange = $state("24h");

	const selectedLabel = $derived.by(() => {
		switch (timeRange) {
			case "24h":
				return "Last 24 hours";
			case "12h":
				return "Last 12 hours";
			case "6h":
				return "Last 6 hours";
			default:
				return "Last 24 hours";
		}
	});

	// Transform API backend bucket data into Date object + log counts
	let chartData = $derived(
		timeSeries.map((item) => ({
			date: new Date(item.bucket),
			count: item.count
		}))
	);

	let filteredData = $derived.by(() => {
		if (chartData.length === 0) return [];
		const newestDate = chartData[chartData.length - 1]?.date || new Date();
		let hoursToSubtract = 24;
		if (timeRange === "12h") hoursToSubtract = 12;
		if (timeRange === "6h") hoursToSubtract = 6;

		const cutoff = new Date(newestDate.getTime() - hoursToSubtract * 60 * 60 * 1000);
		return chartData.filter((item) => item.date >= cutoff);
	});

	const chartConfig = {
		count: { label: "Log Volume", color: "var(--chart-1)" }
	} satisfies Chart.ChartConfig;
</script>

<Card.Root class="rounded-lg border border-border bg-card shadow-xs">
	<Card.Header class="flex items-center gap-2 space-y-0 border-b border-border py-4 sm:flex-row">
		<div class="grid flex-1 gap-1 text-center sm:text-start">
			<Card.Title class="text-base font-bold tracking-tight">Traffic Volume & Log Ingestion Rate</Card.Title>
			<Card.Description class="text-xs text-muted-foreground">
				Ingested log event counts over time ({totalLogs.toLocaleString()} total events)
			</Card.Description>
		</div>
		<Select.Root type="single" bind:value={timeRange}>
			<Select.Trigger class="w-36 h-8 text-xs rounded-md sm:ms-auto" aria-label="Select a time range">
				{selectedLabel}
			</Select.Trigger>
			<Select.Content class="rounded-md">
				<Select.Item value="24h" class="text-xs">Last 24 hours</Select.Item>
				<Select.Item value="12h" class="text-xs">Last 12 hours</Select.Item>
				<Select.Item value="6h" class="text-xs">Last 6 hours</Select.Item>
			</Select.Content>
		</Select.Root>
	</Card.Header>
	<Card.Content class="pt-4">
		{#if filteredData.length === 0}
			<div class="h-[250px] w-full flex items-center justify-center text-xs text-muted-foreground">
				No time-series telemetry buckets recorded for the selected range.
			</div>
		{:else}
			<ChartContainer config={chartConfig} class="-ml-3 aspect-auto h-[250px] w-full">
				<AreaChart
					legend={false}
					data={filteredData}
					x="date"
					xScale={scaleUtc()}
					series={[
						{
							key: "count",
							label: "Log Volume",
							color: chartConfig.count.color
						}
					]}
					props={{
						xAxis: {
							format: (v: Date) => {
								return v.toLocaleTimeString("en-US", {
									hour: "2-digit",
									minute: "2-digit",
									hour12: false
								});
							}
						},
						yAxis: { format: () => "" }
					}}
				>
					{#snippet marks({ context })}
						<defs>
							<linearGradient id="fillVolume" x1="0" y1="0" x2="0" y2="1">
								<stop offset="5%" stop-color="var(--chart-1, #3b82f6)" stop-opacity={0.8} />
								<stop offset="95%" stop-color="var(--chart-1, #3b82f6)" stop-opacity={0.05} />
							</linearGradient>
						</defs>
						<ChartClipPath
							initialWidth={0}
							motion={{
								width: { type: "tween", duration: 800, easing: cubicInOut }
							}}
						>
							{#each context.series.visibleSeries as s (s.key)}
								<Area
									seriesKey={s.key}
									curve={curveNatural}
									fillOpacity={0.4}
									line={{ class: "stroke-2 stroke-primary" }}
									motion="tween"
									{...s.props}
									fill="url(#fillVolume)"
								/>
							{/each}
						</ChartClipPath>
					{/snippet}
					{#snippet tooltip()}
						<Chart.Tooltip
							labelFormatter={(v: Date) => {
								return v.toLocaleString("en-US", {
									month: "short",
									day: "numeric",
									hour: "2-digit",
									minute: "2-digit"
								});
							}}
							indicator="line"
						/>
					{/snippet}
				</AreaChart>
			</ChartContainer>
		{/if}
	</Card.Content>
</Card.Root>
