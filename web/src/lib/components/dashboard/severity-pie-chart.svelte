<script lang="ts">
	import TagIcon from "@lucide/svelte/icons/tag";
	import ShieldAlertIcon from "@lucide/svelte/icons/shield-alert";
	import { PieChart } from "layerchart";
	import * as Card from "$lib/components/ui/card/index.js";
	import * as Chart from "$lib/components/ui/chart/index.js";
	import type { LevelDistribution } from "$lib/server/api";

	let {
		levels = [],
		totalLogs = 0,
		errorRatePercentage = 0
	}: {
		levels?: LevelDistribution[];
		totalLogs?: number;
		errorRatePercentage?: number;
	} = $props();

	// Color mapping for severity levels
	function getLevelColor(level: string): string {
		switch (level?.toUpperCase()) {
			case "FATAL":
				return "oklch(0.55 0.22 300)"; // Purple
			case "ERROR":
				return "oklch(0.58 0.22 25)"; // Red
			case "WARN":
				return "oklch(0.70 0.18 75)"; // Amber
			case "INFO":
				return "oklch(0.62 0.17 150)"; // Emerald
			case "DEBUG":
				return "oklch(0.65 0.15 230)"; // Sky blue
			case "TRACE":
				return "oklch(0.55 0.01 260)"; // Gray
			default:
				return "var(--chart-1)";
		}
	}

	let chartData = $derived(
		levels.map((item) => ({
			level: item.level,
			events: item.count,
			percentage: item.percentage,
			color: getLevelColor(item.level)
		}))
	);

	const chartConfig = {
		events: { label: "Log Events" },
		FATAL: { label: "FATAL", color: "oklch(0.55 0.22 300)" },
		ERROR: { label: "ERROR", color: "oklch(0.58 0.22 25)" },
		WARN: { label: "WARN", color: "oklch(0.70 0.18 75)" },
		INFO: { label: "INFO", color: "oklch(0.62 0.17 150)" },
		DEBUG: { label: "DEBUG", color: "oklch(0.65 0.15 230)" },
		TRACE: { label: "TRACE", color: "oklch(0.55 0.01 260)" }
	} satisfies Chart.ChartConfig;
</script>

<Card.Root class="flex flex-col rounded-lg border border-border bg-card shadow-xs">
	<Card.Header class="items-center border-b border-border pb-3">
		<div class="flex items-center gap-2">
			<TagIcon class="size-4 text-primary" />
			<Card.Title class="text-base font-bold tracking-tight">Severity Distribution</Card.Title>
		</div>
		<Card.Description class="text-xs text-muted-foreground">
			Proportion of log events by severity level ({totalLogs.toLocaleString()} total)
		</Card.Description>
	</Card.Header>

	<Card.Content class="flex-1 pt-4">
		{#if chartData.length === 0}
			<div class="h-[250px] w-full flex items-center justify-center text-xs text-muted-foreground">
				No severity distribution data available.
			</div>
		{:else}
			<Chart.Container config={chartConfig} class="mx-auto aspect-square max-h-[250px]">
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

	<Card.Footer class="flex-col gap-2 text-xs border-t border-border pt-3">
		<div class="flex items-center gap-2 leading-none font-medium">
			<ShieldAlertIcon class="size-4 text-destructive" />
			<span>Overall Error Rate: {errorRatePercentage.toFixed(2)}%</span>
		</div>
		<div class="leading-none text-muted-foreground font-mono">
			Distribution across TRACE, DEBUG, INFO, WARN, ERROR & FATAL
		</div>
	</Card.Footer>
</Card.Root>
