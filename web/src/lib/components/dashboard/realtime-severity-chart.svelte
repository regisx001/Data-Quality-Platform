<script lang="ts">
	import { PieChart } from "layerchart";
	import * as Card from "$lib/components/ui/card/index.js";
	import * as Chart from "$lib/components/ui/chart/index.js";
	import Layers from "@lucide/svelte/icons/layers";

	let {
		infoCount = 0,
		warnCount = 0,
		errorCount = 0,
		totalLogs = 0
	}: {
		infoCount?: number;
		warnCount?: number;
		errorCount?: number;
		totalLogs?: number;
	} = $props();

	let chartData = $derived(
		[
			{ level: "INFO", events: infoCount, color: "var(--chart-1)" },
			{ level: "WARN", events: warnCount, color: "var(--chart-2)" },
			{ level: "ERROR", events: errorCount, color: "var(--chart-5)" }
		].filter((item) => item.events > 0)
	);

	const chartConfig = {
		events: { label: "Log Events" },
		INFO: { label: "INFO", color: "var(--chart-1)" },
		WARN: { label: "WARN", color: "var(--chart-2)" },
		ERROR: { label: "ERROR", color: "var(--chart-5)" }
	} satisfies Chart.ChartConfig;
</script>

<Card.Root class="border-border shadow-xs flex flex-col">
	<Card.Header class="pb-2">
		<Card.Title class="text-base font-semibold flex items-center justify-between">
			<div class="flex items-center gap-2">
				<Layers class="size-4 text-primary" />
				<span>Active Window Severity</span>
			</div>
			<span class="text-xs font-mono font-bold text-muted-foreground">{totalLogs.toLocaleString()} logs</span>
		</Card.Title>
		<Card.Description class="text-xs">
			Severity distribution for active 5-second tumbling window.
		</Card.Description>
	</Card.Header>

	<Card.Content class="flex-1 p-4 flex flex-col items-center justify-center">
		{#if chartData.length === 0}
			<div class="h-[200px] w-full flex items-center justify-center text-xs text-muted-foreground">
				No stream data available in active window.
			</div>
		{:else}
			<Chart.Container config={chartConfig} class="mx-auto aspect-square max-h-[200px] w-full">
				<PieChart
					data={chartData}
					key="level"
					value="events"
					cRange={chartData.map((d) => d.color)}
					c="color"
					props={{
						pie: {
							motion: "tween"
						}
					}}
				>
					{#snippet tooltip()}
						<Chart.Tooltip hideLabel />
					{/snippet}
				</PieChart>
			</Chart.Container>
		{/if}
	</Card.Content>
</Card.Root>
