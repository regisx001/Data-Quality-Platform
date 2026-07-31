<script lang="ts">
	import * as Card from "$lib/components/ui/card/index.js";
	import Loader2 from "@lucide/svelte/icons/loader-2";
	import CheckCircle2 from "@lucide/svelte/icons/check-circle-2";
	import XCircle from "@lucide/svelte/icons/x-circle";
	import Cable from "@lucide/svelte/icons/cable";
	import Server from "@lucide/svelte/icons/server";
	import Database from "@lucide/svelte/icons/database";
	import Info from "@lucide/svelte/icons/info";
	import type { Datasource, ConnectionTestResult } from "$lib/server/api";

	let {
		datasource,
		statusInfo,
		hasConfig,
		isTestingConnection,
		connectionTestResult,
		connectionTestError,
		getConfigEntries,
	}: {
		datasource: Datasource;
		statusInfo: { label: string; classes: string };
		hasConfig: boolean;
		isTestingConnection: boolean;
		connectionTestResult: ConnectionTestResult | null;
		connectionTestError: string | null;
		getConfigEntries: () => { key: string; value: string }[];
	} = $props();
</script>

<div class="space-y-6">
	<div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
		<!-- Connection Test Card -->
		<Card.Root class="lg:col-span-2 rounded-xl border-border bg-card shadow-xs">
			<Card.Header class="pb-3 border-b border-border">
				<Card.Title class="text-base font-bold tracking-tight">Connection Test</Card.Title>
				<Card.Description class="text-xs text-muted-foreground mt-1">
					Verify that the datasource is reachable using its stored configuration.
				</Card.Description>
			</Card.Header>
			<Card.Content class="p-5 space-y-4">
				<div class="flex items-center gap-4">
					<button
						type="submit"
						form="test-connection-form"
						disabled={isTestingConnection}
						class="inline-flex items-center gap-2 h-10 px-4 rounded-lg text-sm font-medium border border-border bg-background hover:bg-accent text-foreground transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
					>
						{#if isTestingConnection}
							<Loader2 class="size-4 animate-spin" />
							<span>Testing connection...</span>
						{:else}
							<Cable class="size-4" />
							<span>Test Connection</span>
						{/if}
					</button>

					{#if !hasConfig}
						<span
							class="inline-flex items-center gap-1.5 text-xs text-amber-600 dark:text-amber-400"
						>
							<Info class="size-3.5" />
							No configuration saved yet. Add one in the Configuration tab first.
						</span>
					{/if}
				</div>

				<!-- Connection test result feedback -->
				{#if isTestingConnection}
					<div class="p-3 rounded-lg border border-border bg-muted/30 text-xs space-y-1.5">
						<div class="flex items-center gap-1.5 font-medium">
							<Loader2 class="size-3.5 animate-spin text-muted-foreground" />
							<span>Testing connection...</span>
						</div>
					</div>
				{/if}

				{#if connectionTestResult}
					<div
						class="p-4 rounded-lg border text-xs space-y-2 {connectionTestResult.success
							? 'bg-emerald-500/10 border-emerald-500/20'
							: 'bg-destructive/10 border-destructive/20'}"
					>
						<div class="flex items-center gap-2 font-semibold">
							{#if connectionTestResult.success}
								<CheckCircle2 class="size-4 text-emerald-600 dark:text-emerald-400" />
								<span class="text-emerald-600 dark:text-emerald-400">Healthy</span>
								<span
									class="px-2 py-0.5 rounded-full text-[11px] font-medium border border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
								>
									Connected
								</span>
							{:else}
								<XCircle class="size-4 text-destructive" />
								<span class="text-destructive">Unreachable</span>
								<span
									class="px-2 py-0.5 rounded-full text-[11px] font-medium border border-destructive/30 bg-destructive/10 text-destructive"
								>
									Failed
								</span>
							{/if}
						</div>
						<p class="text-muted-foreground leading-relaxed">
							{connectionTestResult.message}
						</p>
						<div class="flex items-center gap-4 pt-2 border-t border-border/50">
							<span class="font-mono text-[11px] text-muted-foreground">
								Latency:
								<strong class="text-foreground font-semibold"
									>{connectionTestResult.latencyMs}ms</strong
								>
							</span>
							<span class="font-mono text-[11px] text-muted-foreground">
								Tested:
								<strong class="text-foreground font-semibold"
									>{new Date().toLocaleTimeString()}</strong
								>
							</span>
						</div>
					</div>
				{/if}

				{#if connectionTestError}
					<div
						class="p-4 rounded-lg border border-destructive/20 bg-destructive/10 text-xs space-y-1.5"
					>
						<div class="flex items-center gap-1.5 font-medium text-destructive">
							<XCircle class="size-3.5" />
							<span>Connection test failed</span>
						</div>
						<p class="text-muted-foreground">
							{connectionTestError}
						</p>
					</div>
				{/if}
			</Card.Content>
		</Card.Root>

		<!-- Datasource Health Summary Card -->
		<Card.Root class="rounded-xl border-border bg-card shadow-xs">
			<Card.Header class="pb-3 border-b border-border">
				<Card.Title class="text-base font-bold tracking-tight">Health Summary</Card.Title>
			</Card.Header>
			<Card.Content class="p-5 space-y-3">
				<div
					class="p-3 rounded-lg bg-muted/30 border border-border/50 flex items-center justify-between"
				>
					<span class="text-xs text-muted-foreground">Datasource Status</span>
					<span class={`px-2.5 py-0.5 rounded-full text-xs font-medium border ${statusInfo.classes}`}>
						{statusInfo.label}
					</span>
				</div>

				<div
					class="p-3 rounded-lg bg-muted/30 border border-border/50 flex items-center justify-between"
				>
					<span class="text-xs text-muted-foreground">Configuration</span>
					{#if hasConfig}
						<span class="inline-flex items-center gap-1 text-xs text-emerald-600 dark:text-emerald-400">
							<CheckCircle2 class="size-3.5" />
							Configured
						</span>
					{:else}
						<span class="inline-flex items-center gap-1 text-xs text-amber-600 dark:text-amber-400">
							<Info class="size-3.5" />
							Not set
						</span>
					{/if}
				</div>

				<div
					class="p-3 rounded-lg bg-muted/30 border border-border/50 flex items-center justify-between"
				>
					<span class="text-xs text-muted-foreground">Connection</span>
					{#if connectionTestResult}
						<span
							class="inline-flex items-center gap-1 text-xs {connectionTestResult.success
								? 'text-emerald-600 dark:text-emerald-400'
								: 'text-destructive'}"
						>
							{#if connectionTestResult.success}
								<CheckCircle2 class="size-3.5" />
								Healthy
							{:else}
								<XCircle class="size-3.5" />
								Unreachable
							{/if}
						</span>
					{:else}
						<span class="text-xs text-muted-foreground">Not tested</span>
					{/if}
				</div>

				<div
					class="p-3 rounded-lg bg-muted/30 border border-border/50 flex items-center justify-between"
				>
					<span class="text-xs text-muted-foreground">Datasets</span>
					<span class="text-xs font-semibold font-mono">
						{datasource.datasets?.length ?? 0}
					</span>
				</div>
			</Card.Content>
		</Card.Root>
	</div>

	<!-- Datasource Information Card -->
	<Card.Root class="rounded-xl border-border bg-card overflow-hidden shadow-xs w-full">
		<Card.Header class="pb-3 border-b border-border">
			<Card.Title class="text-base font-bold tracking-tight flex items-center gap-2">
				<Server class="size-4" />
				Datasource Information
			</Card.Title>
		</Card.Header>
		<Card.Content class="p-5 space-y-5">
			<!-- General metadata -->
			<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 text-xs">
				<div class="p-3 rounded-lg bg-muted/30 border border-border/50 space-y-1">
					<span class="text-muted-foreground">Engine Type</span>
					<p class="font-mono text-sm font-semibold">
						{datasource.type}
					</p>
				</div>

				<div class="p-3 rounded-lg bg-muted/30 border border-border/50 space-y-1">
					<span class="text-muted-foreground">Owner</span>
					<p class="font-medium text-sm">
						@{datasource.owner}
					</p>
				</div>

				<div class="p-3 rounded-lg bg-muted/30 border border-border/50 space-y-1">
					<span class="text-muted-foreground">Status</span>
					<p class="font-medium text-sm">
						{datasource.status}
					</p>
				</div>

				<div class="p-3 rounded-lg bg-muted/30 border border-border/50 space-y-1">
					<span class="text-muted-foreground">Registered</span>
					<p class="font-mono text-xs">
						{datasource.registrationDate
							? new Date(datasource.registrationDate).toLocaleString()
							: "N/A"}
					</p>
				</div>
			</div>

			{#if datasource.description}
				<div class="p-3.5 rounded-lg bg-muted/30 border border-border/50 space-y-1 text-xs">
					<span class="text-muted-foreground">Description</span>
					<p class="text-foreground">
						{datasource.description}
					</p>
				</div>
			{/if}

			<!-- Connector configuration details -->
			<div>
				<h3 class="flex items-center gap-1.5 text-xs font-semibold text-muted-foreground mb-2">
					<Database class="size-3.5" />
					Connector Configuration
				</h3>

				{#if getConfigEntries().length > 0}
					<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
						{#each getConfigEntries() as entry}
							<div class="p-3 rounded-lg bg-muted/30 border border-border/50 space-y-1">
								<span class="text-[11px] text-muted-foreground capitalize">{entry.key}</span>
								<p class="font-mono text-xs font-medium break-all">
									{entry.value}
								</p>
							</div>
						{/each}
					</div>
				{:else}
					<div
						class="p-4 text-center text-xs text-muted-foreground border border-dashed border-border rounded-lg"
					>
						No configuration saved for this datasource yet.
					</div>
				{/if}
			</div>
		</Card.Content>
	</Card.Root>
</div>
