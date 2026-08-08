<script lang="ts">
	import TrendingUpIcon from "@lucide/svelte/icons/trending-up";
	import ServerIcon from "@lucide/svelte/icons/server";
	import { scaleBand } from "d3-scale";
	import { BarChart } from "layerchart";
	import { cubicInOut } from "svelte/easing";
	import * as Card from "$lib/components/ui/card/index.js";
	import * as Chart from "$lib/components/ui/chart/index.js";
	import type { HttpMetrics } from "$lib/server/api";

	let { http }: { http?: HttpMetrics } = $props();

	// Format HTTP status metrics into horizontal bar data (2xx, 3xx, 4xx, 5xx)
	let chartData = $derived([
		{ category: "2xx", count: http?.count2xx ?? 0, rate: http?.rate2xx ?? 0 },
		{ category: "3xx", count: http?.count3xx ?? 0, rate: http?.rate3xx ?? 0 },
		{ category: "4xx", count: http?.count4xx ?? 0, rate: http?.rate4xx ?? 0 },
		{ category: "5xx", count: http?.count5xx ?? 0, rate: http?.rate5xx ?? 0 }
	]);

	const chartConfig = {
		count: { label: "Request Count", color: "var(--chart-1, #3b82f6)" }
	} satisfies Chart.ChartConfig;
</script>

<Card.Root class="rounded-lg border border-border bg-card shadow-xs">
	<Card.Header class="pb-3 border-b border-border">
		<div class="flex items-center gap-2">
			<ServerIcon class="size-4 text-emerald-500" />
			<Card.Title class="text-base font-bold tracking-tight">HTTP Response Code Breakdown</Card.Title>
		</div>
		<Card.Description class="text-xs text-muted-foreground">
			Distribution across HTTP 2xx, 3xx, 4xx, and 5xx response statuses ({http?.totalRequests ?? 0} total requests)
		</Card.Description>
	</Card.Header>

	<Card.Content class="pt-4">
		<Chart.Container config={chartConfig} class="aspect-auto h-[250px] w-full">
			<BarChart
				data={chartData}
				orientation="horizontal"
				yScale={scaleBand().padding(0.3)}
				y="category"
				series={[{ key: "count", label: "Request Count", color: "var(--chart-1, #3b82f6)" }]}
				padding={{ left: 45, right: 20 }}
				grid={false}
				rule={false}
				axis="y"
				props={{
					bars: {
						stroke: "none",
						fill: "var(--chart-1, #3b82f6)",
						rounded: "right",
						motion: { type: "tween", duration: 500, easing: cubicInOut }
					},
					highlight: { area: { fill: "none" } },
					yAxis: { format: (d) => d }
				}}
			>
				{#snippet tooltip()}
					<Chart.Tooltip hideLabel />
				{/snippet}
			</BarChart>
		</Chart.Container>
	</Card.Content>

	<Card.Footer class="border-t border-border pt-3">
		<div class="flex w-full items-start gap-2 text-xs">
			<div class="grid gap-1">
				<div class="flex items-center gap-1.5 font-medium text-emerald-500">
					<span>2xx Success Rate: {http?.rate2xx != null ? `${http.rate2xx.toFixed(1)}%` : "0%"}</span>
					<TrendingUpIcon class="size-3.5" />
				</div>
				<div class="flex items-center gap-2 text-muted-foreground font-mono">
					<span>4xx Rate: {http?.rate4xx != null ? `${http.rate4xx.toFixed(1)}%` : "0%"}</span>
					<span>•</span>
					<span>5xx Rate: {http?.rate5xx != null ? `${http.rate5xx.toFixed(1)}%` : "0%"}</span>
				</div>
			</div>
		</div>
	</Card.Footer>
</Card.Root>
