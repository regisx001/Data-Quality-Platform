<script lang="ts">
	import type { PageData } from "./$types";
	import * as Tabs from "$lib/components/ui/tabs/index.js";
	import * as Table from "$lib/components/ui/table/index.js";
	import * as Dialog from "$lib/components/ui/dialog/index.js";
	import * as DropdownMenu from "$lib/components/ui/dropdown-menu/index.js";
	import { Badge } from "$lib/components/ui/badge/index.js";
	import { Button } from "$lib/components/ui/button/index.js";
	import { Input } from "$lib/components/ui/input/index.js";
	import { Label } from "$lib/components/ui/label/index.js";
	import ErrorAlert from "$lib/components/ui/error-alert.svelte";
	import { goto } from "$app/navigation";
	import { page } from "$app/state";

	// Icons
	import Search from "@lucide/svelte/icons/search";
	import RefreshCw from "@lucide/svelte/icons/refresh-cw";
	import Server from "@lucide/svelte/icons/server";
	import Eye from "@lucide/svelte/icons/eye";
	import ChevronLeft from "@lucide/svelte/icons/chevron-left";
	import ChevronRight from "@lucide/svelte/icons/chevron-right";
	import ChevronsLeft from "@lucide/svelte/icons/chevrons-left";
	import ChevronsRight from "@lucide/svelte/icons/chevrons-right";
	import LayoutColumns from "@tabler/icons-svelte/icons/layout-columns";
	import ChevronDown from "@tabler/icons-svelte/icons/chevron-down";
	import BarChart3 from "@lucide/svelte/icons/bar-chart-3";
	import ArrowLeft from "@lucide/svelte/icons/arrow-left";
	import type { LogEntry } from "$lib/server/api";

	let { data }: { data: PageData } = $props();

	let activeTab = $state($state.snapshot(data.queryParams.level) || "ALL");
	let searchQuery = $state($state.snapshot(data.queryParams.search) || "");
	let serviceFilter = $state($state.snapshot(data.queryParams.serviceName) || "ALL");
	let categoryFilter = $state($state.snapshot(data.queryParams.category) || "ALL");
	let traceIdInput = $state($state.snapshot(data.queryParams.traceId) || "");

	// Log detail modal state
	let selectedLog = $state<LogEntry | null>(null);
	let isDetailOpen = $state(false);

	// Column visibility controls
	let showTraceIdCol = $state(true);
	let showLatencyCol = $state(true);
	let showCategoryCol = $state(true);

	let stats = $derived(data.stats);
	let logsData = $derived(data.logs);

	let serviceOptions = $derived(() => {
		const set = new Set<string>();
		if (stats?.logsByService) {
			Object.keys(stats.logsByService).forEach((s) => set.add(s));
		}
		return Array.from(set);
	});

	let categoryOptions = $derived(() => {
		const set = new Set<string>();
		if (stats?.logsByCategory) {
			Object.keys(stats.logsByCategory).forEach((c) => set.add(c));
		}
		return Array.from(set);
	});

	function handleTabChange(tabVal: string) {
		activeTab = tabVal;
		applyFilters(tabVal);
	}

	function applyFilters(overrideTab?: string) {
		const targetLevel = overrideTab !== undefined ? overrideTab : activeTab;
		const params = new URLSearchParams(page.url.searchParams);

		if (searchQuery.trim()) params.set("search", searchQuery.trim());
		else params.delete("search");

		if (targetLevel && targetLevel !== "ALL") params.set("level", targetLevel);
		else params.delete("level");

		if (serviceFilter && serviceFilter !== "ALL") params.set("serviceName", serviceFilter);
		else params.delete("serviceName");

		if (categoryFilter && categoryFilter !== "ALL") params.set("category", categoryFilter);
		else params.delete("category");

		if (traceIdInput.trim()) params.set("traceId", traceIdInput.trim());
		else params.delete("traceId");

		params.set("page", "0");
		goto(`?${params.toString()}`);
	}

	function resetFilters() {
		searchQuery = "";
		activeTab = "ALL";
		serviceFilter = "ALL";
		categoryFilter = "ALL";
		traceIdInput = "";
		goto("/logs/table");
	}

	function navigatePage(newPage: number) {
		const params = new URLSearchParams(page.url.searchParams);
		params.set("page", newPage.toString());
		goto(`?${params.toString()}`);
	}

	function openLogDetail(log: LogEntry) {
		selectedLog = log;
		isDetailOpen = true;
	}

	function formatTimestamp(ts: string): string {
		try {
			return new Date(ts).toLocaleString();
		} catch {
			return ts;
		}
	}

	function getLevelBadge(level: string) {
		switch (level?.toUpperCase()) {
			case "FATAL":
			case "ERROR":
				return {
					label: level,
					classes: "bg-destructive/10 text-destructive border-destructive/20 font-mono"
				};
			case "WARN":
				return {
					label: level,
					classes: "bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20 font-mono"
				};
			case "INFO":
				return {
					label: level,
					classes: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20 font-mono"
				};
			case "DEBUG":
				return {
					label: level,
					classes: "bg-sky-500/10 text-sky-600 dark:text-sky-400 border-sky-500/20 font-mono"
				};
			default:
				return {
					label: level,
					classes: "bg-slate-500/10 text-slate-600 dark:text-slate-400 border-slate-500/20 font-mono"
				};
		}
	}
</script>

<svelte:head>
	<title>Log Explorer Table | Data Quality Platform</title>
	<meta name="description" content="Detailed searchable and filterable microservice log event records table." />
</svelte:head>

<div class="p-6 sm:p-8 w-full space-y-6">
	<!-- Page Header -->
	<div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-6">
		<div class="space-y-1">
			<div class="flex items-center gap-2">
				<Button variant="ghost" size="sm" onclick={() => goto("/logs")} class="gap-1.5 h-8 px-2">
					<ArrowLeft class="size-4" />
					<span>Back to Analytics</span>
				</Button>
			</div>
			<h1 class="text-2xl font-bold tracking-tight">Log Explorer Table</h1>
			<p class="text-sm text-muted-foreground">
				Paginated query explorer for microservice log events, HTTP payloads, and trace executions.
			</p>
		</div>

		<div class="flex items-center gap-2">
			<Button variant="outline" size="sm" onclick={() => goto("/logs")} class="gap-2">
				<BarChart3 class="size-4" />
				<span>Observability Charts</span>
			</Button>

			<Button variant="outline" size="sm" onclick={() => goto("/logs/table")} class="gap-2">
				<RefreshCw class="size-4" />
				<span>Refresh</span>
			</Button>
		</div>
	</div>

	<!-- Data Table View -->
	<div class="space-y-4">
		<!-- Severity Status Filter Tabs -->
		<Tabs.Root value={activeTab} onValueChange={handleTabChange} class="w-full">
			<Tabs.List class="flex h-auto flex-wrap items-center justify-start gap-1 p-1 bg-muted/60 rounded-lg w-full sm:w-auto">
				<Tabs.Trigger value="ALL" class="text-xs px-3 py-1.5 font-medium rounded-md">
					All Logs
					<span class="ms-1.5 rounded-full bg-muted-foreground/15 px-1.5 py-0.5 text-[10px] font-mono font-semibold">
						{stats?.totalLogs ?? 0}
					</span>
				</Tabs.Trigger>
				<Tabs.Trigger value="ERROR" class="text-xs px-3 py-1.5 font-medium rounded-md">
					ERROR
					<span class="ms-1.5 rounded-full bg-destructive/20 text-destructive dark:text-red-400 px-1.5 py-0.5 text-[10px] font-mono font-bold">
						{stats?.errorCount ?? 0}
					</span>
				</Tabs.Trigger>
				<Tabs.Trigger value="WARN" class="text-xs px-3 py-1.5 font-medium rounded-md">
					WARN
					<span class="ms-1.5 rounded-full bg-amber-500/20 text-amber-600 dark:text-amber-400 px-1.5 py-0.5 text-[10px] font-mono font-bold">
						{stats?.warnCount ?? 0}
					</span>
				</Tabs.Trigger>
				<Tabs.Trigger value="INFO" class="text-xs px-3 py-1.5 font-medium rounded-md">
					INFO
					<span class="ms-1.5 rounded-full bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 px-1.5 py-0.5 text-[10px] font-mono font-bold">
						{stats?.infoCount ?? 0}
					</span>
				</Tabs.Trigger>
				<Tabs.Trigger value="DEBUG" class="text-xs px-3 py-1.5 font-medium rounded-md">
					DEBUG
				</Tabs.Trigger>
				<Tabs.Trigger value="TRACE" class="text-xs px-3 py-1.5 font-medium rounded-md">
					TRACE
				</Tabs.Trigger>
			</Tabs.List>
		</Tabs.Root>

		<!-- Filter Bar -->
		<div class="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
			<div class="flex flex-1 flex-wrap items-center gap-2">
				<div class="relative min-w-[220px] max-w-sm flex-1">
					<Search class="absolute left-2.5 top-2.5 size-4 text-muted-foreground" />
					<Input
						type="text"
						placeholder="Search message or path..."
						bind:value={searchQuery}
						onkeydown={(e) => e.key === "Enter" && applyFilters()}
						class="pl-9 h-9 text-xs"
					/>
				</div>

				<!-- Service Filter Select -->
				<select
					bind:value={serviceFilter}
					onchange={() => applyFilters()}
					class="h-9 px-3 rounded-md border border-input bg-background text-xs"
				>
					<option value="ALL">All Services</option>
					{#each serviceOptions() as svc}
						<option value={svc}>{svc}</option>
					{/each}
				</select>

				<!-- Category Filter Select -->
				<select
					bind:value={categoryFilter}
					onchange={() => applyFilters()}
					class="h-9 px-3 rounded-md border border-input bg-background text-xs"
				>
					<option value="ALL">All Categories</option>
					{#each categoryOptions() as cat}
						<option value={cat}>{cat}</option>
					{/each}
				</select>
			</div>

			<div class="flex items-center gap-2">
				<Button variant="ghost" size="sm" onclick={resetFilters} class="text-xs h-9">
					Reset
				</Button>
				<Button size="sm" onclick={() => applyFilters()} class="text-xs h-9 gap-1.5">
					<Search class="size-3.5" />
					<span>Apply Query</span>
				</Button>

				<DropdownMenu.Root>
					<DropdownMenu.Trigger>
						{#snippet child({ props })}
							<Button {...props} variant="outline" size="sm" class="h-9 text-xs gap-1.5">
								<LayoutColumns class="size-4" />
								<span class="hidden sm:inline">Columns</span>
								<ChevronDown class="size-3.5 opacity-60" />
							</Button>
						{/snippet}
					</DropdownMenu.Trigger>
					<DropdownMenu.Content align="end" class="w-48">
						<DropdownMenu.Label class="text-xs">Toggle Columns</DropdownMenu.Label>
						<DropdownMenu.Separator />
						<DropdownMenu.CheckboxItem bind:checked={showCategoryCol} class="text-xs">
							Category
						</DropdownMenu.CheckboxItem>
						<DropdownMenu.CheckboxItem bind:checked={showTraceIdCol} class="text-xs">
							Trace ID
						</DropdownMenu.CheckboxItem>
						<DropdownMenu.CheckboxItem bind:checked={showLatencyCol} class="text-xs">
							Execution Time
						</DropdownMenu.CheckboxItem>
					</DropdownMenu.Content>
				</DropdownMenu.Root>
			</div>
		</div>

		<!-- Error Alert Display -->
		{#if data.logsError}
			<ErrorAlert error={data.logsError} title="Failed to Fetch Telemetry Logs" />
		{/if}

		<!-- Log Data Table -->
		<div class="rounded-md border border-border bg-card">
			{#if !logsData || logsData.content.length === 0}
				<div class="p-12 text-center space-y-3">
					<Server class="size-10 text-muted-foreground/40 mx-auto" />
					<p class="text-sm font-medium">No log events found</p>
					<p class="text-xs text-muted-foreground">
						No records matched your search query or selected level filters.
					</p>
					<Button variant="outline" size="sm" onclick={resetFilters} class="text-xs">
						Reset Filters
					</Button>
				</div>
			{:else}
				<div class="overflow-x-auto">
					<Table.Root>
						<Table.Header>
							<Table.Row>
								<Table.Head class="w-[180px] text-xs font-semibold">Timestamp</Table.Head>
								<Table.Head class="w-[90px] text-xs font-semibold">Level</Table.Head>
								<Table.Head class="w-[160px] text-xs font-semibold">Service</Table.Head>
								{#if showCategoryCol}
									<Table.Head class="w-[130px] text-xs font-semibold">Category</Table.Head>
								{/if}
								<Table.Head class="text-xs font-semibold">Message & HTTP Endpoint</Table.Head>
								{#if showTraceIdCol}
									<Table.Head class="w-[130px] text-xs font-semibold">Trace ID</Table.Head>
								{/if}
								{#if showLatencyCol}
									<Table.Head class="w-[100px] text-xs font-semibold text-right">Latency</Table.Head>
								{/if}
								<Table.Head class="w-[60px] text-xs font-semibold text-center">View</Table.Head>
							</Table.Row>
						</Table.Header>
						<Table.Body>
							{#each logsData.content as log (log.id)}
								{@const badge = getLevelBadge(log.logLevel)}
								<Table.Row class="hover:bg-muted/50 cursor-pointer" onclick={() => openLogDetail(log)}>
									<Table.Cell class="font-mono text-xs text-muted-foreground whitespace-nowrap">
										{formatTimestamp(log.timestamp)}
									</Table.Cell>

									<Table.Cell>
										<Badge variant="outline" class={badge.classes}>
											{badge.label}
										</Badge>
									</Table.Cell>

									<Table.Cell class="font-mono text-xs font-semibold text-foreground truncate max-w-[150px]" title={log.serviceName}>
										{log.serviceName}
									</Table.Cell>

									{#if showCategoryCol}
										<Table.Cell class="text-xs text-muted-foreground">
											<span class="inline-flex items-center px-2 py-0.5 rounded bg-muted text-muted-foreground font-mono text-[11px]">
												{log.category}
											</span>
										</Table.Cell>
									{/if}

									<Table.Cell class="max-w-[400px]">
										<div class="space-y-0.5">
											<p class="text-xs font-medium text-foreground truncate" title={log.message}>
												{log.message}
											</p>
											{#if log.path || log.httpMethod}
												<div class="flex items-center gap-2 text-[11px] font-mono text-muted-foreground truncate">
													{#if log.httpMethod}
														<span class="font-bold text-primary">{log.httpMethod}</span>
													{/if}
													{#if log.path}
														<span class="truncate">{log.path}</span>
													{/if}
													{#if log.statusCode}
														<span class={`font-bold ${log.statusCode >= 400 ? 'text-destructive' : 'text-emerald-500'}`}>
															{log.statusCode}
														</span>
													{/if}
												</div>
											{/if}
										</div>
									</Table.Cell>

									{#if showTraceIdCol}
										<Table.Cell class="font-mono text-xs text-muted-foreground truncate max-w-[120px]">
											{log.traceId ? log.traceId.slice(0, 8) + '...' : '—'}
										</Table.Cell>
									{/if}

									{#if showLatencyCol}
										<Table.Cell class="font-mono text-xs text-right text-muted-foreground whitespace-nowrap">
											{log.executionTimeMs != null ? `${log.executionTimeMs} ms` : "—"}
										</Table.Cell>
									{/if}

									<Table.Cell class="text-center" onclick={(e) => e.stopPropagation()}>
										<Button
											variant="ghost"
											size="icon"
											class="size-7"
											onclick={() => openLogDetail(log)}
											title="View Log Details"
										>
											<Eye class="size-4 text-muted-foreground" />
										</Button>
									</Table.Cell>
								</Table.Row>
							{/each}
						</Table.Body>
					</Table.Root>
				</div>

				<!-- Pagination Footer -->
				<div class="flex flex-col sm:flex-row items-center justify-between gap-4 p-4 border-t border-border text-xs text-muted-foreground">
					<div class="text-muted-foreground">
						Showing <span class="font-semibold text-foreground">{logsData.content.length}</span> of
						<span class="font-semibold text-foreground">{logsData.totalElements}</span> entries
						(Page {logsData.page + 1} of {logsData.totalPages})
					</div>

					<div class="flex items-center gap-2">
						<Button
							variant="outline"
							size="icon"
							class="size-8"
							disabled={logsData.first}
							onclick={() => navigatePage(0)}
							title="First Page"
						>
							<ChevronsLeft class="size-4" />
						</Button>
						<Button
							variant="outline"
							size="icon"
							class="size-8"
							disabled={logsData.first}
							onclick={() => navigatePage(logsData.page - 1)}
							title="Previous Page"
						>
							<ChevronLeft class="size-4" />
						</Button>

						<span class="px-2 text-xs font-mono">
							{logsData.page + 1} / {logsData.totalPages || 1}
						</span>

						<Button
							variant="outline"
							size="icon"
							class="size-8"
							disabled={logsData.last}
							onclick={() => navigatePage(logsData.page + 1)}
							title="Next Page"
						>
							<ChevronRight class="size-4" />
						</Button>
						<Button
							variant="outline"
							size="icon"
							class="size-8"
							disabled={logsData.last}
							onclick={() => navigatePage(logsData.totalPages - 1)}
							title="Last Page"
						>
							<ChevronsRight class="size-4" />
						</Button>
					</div>
				</div>
			{/if}
		</div>
	</div>
</div>

<!-- Log Detail Dialog Modal -->
<Dialog.Root bind:open={isDetailOpen}>
	<Dialog.Content class="sm:max-w-2xl rounded-lg p-6 bg-card border-border shadow-lg overflow-y-auto max-h-[85vh]">
		{#if selectedLog}
			<Dialog.Header class="space-y-1 border-b border-border pb-4">
				<div class="flex items-center gap-2">
					<Badge variant="outline" class={getLevelBadge(selectedLog.logLevel).classes}>
						{selectedLog.logLevel}
					</Badge>
					<Dialog.Title class="font-mono text-sm font-bold truncate">
						{selectedLog.serviceName}
					</Dialog.Title>
				</div>
				<Dialog.Description class="font-mono text-xs text-muted-foreground">
					Log ID: {selectedLog.id}
				</Dialog.Description>
			</Dialog.Header>

			<div class="space-y-4 py-4 text-xs">
				<!-- Primary Message -->
				<div class="space-y-1">
					<Label class="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Log Message</Label>
					<div class="p-3 rounded-md bg-muted border border-border font-mono text-foreground whitespace-pre-wrap break-words">
						{selectedLog.message}
					</div>
				</div>

				<!-- Stack Trace if present -->
				{#if selectedLog.stackTrace}
					<div class="space-y-1">
						<Label class="text-xs font-semibold text-destructive uppercase tracking-wider">Stack Trace</Label>
						<pre class="p-3 rounded-md bg-destructive/10 border border-destructive/20 font-mono text-[11px] text-destructive overflow-x-auto max-h-48">
{selectedLog.stackTrace}
						</pre>
					</div>
				{/if}

				<!-- Metadata Grid -->
				<div class="grid grid-cols-2 gap-3 pt-2">
					<div class="p-2.5 rounded-md border border-border bg-muted/20 space-y-0.5">
						<span class="text-[10px] text-muted-foreground block uppercase">Timestamp</span>
						<span class="font-mono font-semibold">{formatTimestamp(selectedLog.timestamp)}</span>
					</div>

					<div class="p-2.5 rounded-md border border-border bg-muted/20 space-y-0.5">
						<span class="text-[10px] text-muted-foreground block uppercase">Category</span>
						<span class="font-mono font-semibold">{selectedLog.category}</span>
					</div>

					<div class="p-2.5 rounded-md border border-border bg-muted/20 space-y-0.5">
						<span class="text-[10px] text-muted-foreground block uppercase">Trace ID</span>
						<span class="font-mono font-semibold truncate block">{selectedLog.traceId ?? "N/A"}</span>
					</div>

					<div class="p-2.5 rounded-md border border-border bg-muted/20 space-y-0.5">
						<span class="text-[10px] text-muted-foreground block uppercase">HTTP Request</span>
						<span class="font-mono font-semibold">
							{selectedLog.httpMethod ? `${selectedLog.httpMethod} ${selectedLog.path ?? ''}` : "N/A"}
						</span>
					</div>

					<div class="p-2.5 rounded-md border border-border bg-muted/20 space-y-0.5">
						<span class="text-[10px] text-muted-foreground block uppercase">Execution Latency</span>
						<span class="font-mono font-semibold">{selectedLog.executionTimeMs ? `${selectedLog.executionTimeMs} ms` : "N/A"}</span>
					</div>

					<div class="p-2.5 rounded-md border border-border bg-muted/20 space-y-0.5">
						<span class="text-[10px] text-muted-foreground block uppercase">User / Identity</span>
						<span class="font-mono font-semibold truncate block">
							{selectedLog.userEmail ?? selectedLog.userId ?? "System / Anonymous"}
						</span>
					</div>
				</div>

				<!-- Raw Json Metadata if available -->
				{#if selectedLog.metadata}
					<div class="space-y-1 pt-2">
						<Label class="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Extended Metadata</Label>
						<pre class="p-3 rounded-md bg-muted border border-border font-mono text-[11px] text-foreground overflow-x-auto">
{selectedLog.metadata}
						</pre>
					</div>
				{/if}
			</div>

			<Dialog.Footer class="border-t border-border pt-4">
				<Button variant="outline" size="sm" onclick={() => (isDetailOpen = false)} class="text-xs">
					Close
				</Button>
			</Dialog.Footer>
		{/if}
	</Dialog.Content>
</Dialog.Root>
