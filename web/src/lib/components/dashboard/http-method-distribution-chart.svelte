<script lang="ts">
	import TagIcon from "@lucide/svelte/icons/tag";
	import GlobeIcon from "@lucide/svelte/icons/globe";
	import { PieChart } from "layerchart";
	import * as Card from "$lib/components/ui/card/index.js";
	import * as Chart from "$lib/components/ui/chart/index.js";

	let {
		methodCounts = {},
		totalRequests = 0
	}: {
		methodCounts?: Record<string, number>;
		totalRequests?: number;
	} = $props();

	function getMethodColor(method: string): string {
		switch (method?.toUpperCase()) {
			case "GET":
				return "oklch(0.62 0.17 150)"; // Emerald
			case "POST":
				return "oklch(0.65 0.15 230)"; // Sky blue
			case "PUT":
				return "oklch(0.70 0.18 75)"; // Amber
			case "DELETE":
				return "oklch(0.58 0.22 25)"; // Red
			case "PATCH":
				return "oklch(0.55 0.22 300)"; // Purple
			default:
				return "var(--chart-1)";
		}
	}

	let chartData = $derived(
		Object.entries(methodCounts).map(([method, count]) => ({
			method: method.toUpperCase(),
			requests: count,
			color: getMethodColor(method)
		}))
	);

	const chartConfig = {
		requests: { label: "Requests" },
		GET: { label: "GET", color: "oklch(0.62 0.17 150)" },
		POST: { label: "POST", color: "oklch(0.65 0.15 230)" },
		PUT: { label: "PUT", color: "oklch(0.70 0.18 75)" },
		DELETE: { label: "DELETE", color: "oklch(0.58 0.22 25)" },
		PATCH: { label: "PATCH", color: "oklch(0.55 0.22 300)" }
	} satisfies Chart.ChartConfig;
</script>

<Card.Root class="flex flex-col rounded-lg border border-border bg-card shadow-xs">
	<Card.Header class="items-center border-b border-border pb-3">
		<div class="flex items-center gap-2">
			<GlobeIcon class="size-4 text-sky-500" />
			<Card.Title class="text-base font-bold tracking-tight">HTTP Method Distribution</Card.Title>
		</div>
		<Card.Description class="text-xs text-muted-foreground">
			Proportion of requests by HTTP verb ({totalRequests.toLocaleString()} total)
		</Card.Description>
	</Card.Header>

	<Card.Content class="flex-1 pt-4">
		{#if chartData.length === 0}
			<div class="h-[250px] w-full flex items-center justify-center text-xs text-muted-foreground">
				No HTTP method distribution data available.
			</div>
		{:else}
			<Chart.Container config={chartConfig} class="mx-auto aspect-square max-h-[250px]">
				<PieChart
					data={chartData}
					key="method"
					value="requests"
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

	<Card.Footer class="flex-col gap-1 text-xs border-t border-border pt-3">
		<div class="leading-none text-muted-foreground font-mono">
			Verbs: GET, POST, PUT, DELETE, PATCH
		</div>
	</Card.Footer>
</Card.Root>
