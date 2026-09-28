<script lang="ts">
	import { enhance } from "$app/forms";
	import { Button } from "$lib/components/ui/button/index.js";
	import * as Card from "$lib/components/ui/card/index.js";
	import * as Tabs from "$lib/components/ui/tabs/index.js";
	import * as Dialog from "$lib/components/ui/dialog/index.js";
	import * as Field from "$lib/components/ui/field/index.js";
	import { Input } from "$lib/components/ui/input/index.js";
	import ArrowLeft from "@lucide/svelte/icons/arrow-left";
	import Database from "@lucide/svelte/icons/database";
	import TableProperties from "@lucide/svelte/icons/table-properties";
	import Play from "@lucide/svelte/icons/play";
	import Eye from "@lucide/svelte/icons/eye";
	import ShieldCheck from "@lucide/svelte/icons/shield-check";
	import Key from "@lucide/svelte/icons/key";
	import Loader2 from "@lucide/svelte/icons/loader-2";
	import Sparkles from "@lucide/svelte/icons/sparkles";
	import Calendar from "@lucide/svelte/icons/calendar";
	import Trash2 from "@lucide/svelte/icons/trash-2";
	import Plus from "@lucide/svelte/icons/plus";
	import Edit from "@lucide/svelte/icons/edit";
	import ErrorAlert from "$lib/components/ui/error-alert.svelte";
	import SqlEditor from "$lib/components/ui/sql-editor.svelte";
	import type { PageData, ActionData } from "./$types";
	import type { QualityRule, RuleCategory, RuleSeverity } from "$lib/server/api";

	let { data, form }: { data: PageData; form: ActionData } = $props();

	let dataset = $derived(form?.dataset || data.dataset);
	let preview = $derived(data.preview);
	let rules = $derived(data.rules || []);

	let activeTab = $state("schema");
	let isProfiling = $state(false);
	let isDeleteOpen = $state(false);
	let isDeleting = $state(false);

	// Quality Rules Dialog & Form States
	let isCreateRuleOpen = $state(false);
	let isEditRuleOpen = $state(false);
	let isDeleteRuleOpen = $state(false);
	let selectedRule = $state<QualityRule | null>(null);
	let isSubmittingRule = $state(false);

	// Create Rule form fields
	let createRuleTarget = $state("*");
	let createRuleName = $state("");
	let createRuleCategory = $state<RuleCategory>("COMPLETENESS");
	let createRuleSeverity = $state<RuleSeverity>("MEDIUM");
	let createRuleExpectation = $state("is_not_null");
	let createRuleCondition = $state("");
	let createRuleDescription = $state("");
	let createRuleEnabled = $state(true);

	const categories: { label: string; value: RuleCategory }[] = [
		{ label: "Completeness", value: "COMPLETENESS" },
		{ label: "Validity", value: "VALIDITY" },
		{ label: "Consistency", value: "CONSISTENCY" },
		{ label: "Uniqueness", value: "UNIQUENESS" },
		{ label: "Timeliness", value: "TIMELINESS" },
		{ label: "Accuracy", value: "ACCURACY" },
		{ label: "Business Context", value: "BUSINESS_CONTEXT" },
	];

	const severities: { label: string; value: RuleSeverity }[] = [
		{ label: "Critical", value: "CRITICAL" },
		{ label: "High", value: "HIGH" },
		{ label: "Medium", value: "MEDIUM" },
		{ label: "Low", value: "LOW" },
		{ label: "Info", value: "INFO" },
	];

	function getNullPercentageColor(pct: number = 0) {
		if (pct === 0)
			return "bg-emerald-500 text-emerald-600 dark:text-emerald-400";
		if (pct < 10) return "bg-amber-500 text-amber-600 dark:text-amber-400";
		return "bg-destructive text-destructive";
	}

	function getSeverityBadge(severity: RuleSeverity) {
		switch (severity) {
			case "CRITICAL":
				return "bg-destructive/10 text-destructive border-destructive/20";
			case "HIGH":
				return "bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20";
			case "MEDIUM":
				return "bg-sky-500/10 text-sky-600 dark:text-sky-400 border-sky-500/20";
			case "LOW":
				return "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20";
			case "INFO":
			default:
				return "bg-muted text-muted-foreground border-border";
		}
	}

	function getCategoryBadge(category: RuleCategory) {
		switch (category) {
			case "COMPLETENESS":
				return "bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 border-indigo-500/20";
			case "UNIQUENESS":
				return "bg-purple-500/10 text-purple-600 dark:text-purple-400 border-purple-500/20";
			case "VALIDITY":
				return "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20";
			case "CONSISTENCY":
				return "bg-teal-500/10 text-teal-600 dark:text-teal-400 border-teal-500/20";
			case "TIMELINESS":
				return "bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20";
			case "ACCURACY":
				return "bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/20";
			case "BUSINESS_CONTEXT":
			default:
				return "bg-muted text-muted-foreground border-border";
		}
	}

	function openCreateRule(columnName?: string) {
		createRuleTarget = columnName || "*";
		createRuleName = columnName ? `${columnName} Must Not Be Null` : "";
		createRuleCategory = "COMPLETENESS";
		createRuleSeverity = "MEDIUM";
		createRuleExpectation = "is_not_null";
		createRuleCondition = "";
		createRuleDescription = "";
		createRuleEnabled = true;
		isCreateRuleOpen = true;
	}

	function openEditRule(rule: QualityRule) {
		selectedRule = rule;
		isEditRuleOpen = true;
	}

	function openDeleteRule(rule: QualityRule) {
		selectedRule = rule;
		isDeleteRuleOpen = true;
	}
</script>

<svelte:head>
	<title
		>{dataset ? dataset.name : "Dataset Not Found"} | Dataset Inspection</title
	>
</svelte:head>

{#if !dataset}
	<!--  -->
	<div class="p-6 sm:p-8 w-full max-w-xl mx-auto space-y-4 pt-12">
		<Card.Root
			class="rounded-xl border-border bg-card p-6 text-center space-y-4 shadow-xs"
		>
			<div
				class="mx-auto w-12 h-12 rounded-full bg-muted flex items-center justify-center"
			>
				<TableProperties class="size-6 text-muted-foreground" />
			</div>
			<div class="space-y-1">
				<h2 class="text-lg font-bold tracking-tight">
					Dataset Not Found
				</h2>
				<p class="text-xs text-muted-foreground">
					The dataset with ID <code
						class="font-mono px-1 py-0.5 rounded bg-muted text-foreground"
						>{data.id}</code
					> was not found or has not been registered yet.
				</p>
			</div>
			<div class="pt-2 flex items-center justify-center gap-3">
				<a
					href="/datasources"
					class="inline-flex items-center gap-2 h-9 px-4 rounded-lg text-xs font-medium border border-border bg-background hover:bg-accent text-foreground transition-colors"
				>
					<ArrowLeft class="size-3.5" />
					<span>Back to Datasources</span>
				</a>
			</div>
		</Card.Root>
	</div>
{:else}
	<div class="p-6 sm:p-8 w-full space-y-6">
		<!-- Navigation & Actions Bar -->
		<div class="flex flex-col gap-4 border-b border-border pb-5">
			<div
				class="flex flex-col sm:flex-row sm:items-center justify-between gap-4"
			>
				<div class="flex items-center gap-3">
					<a
						href={`/datasources/${dataset.datasourceId}`}
						class="p-2 rounded-lg border border-border hover:bg-accent transition-colors text-muted-foreground hover:text-foreground shrink-0"
						title="Back to Datasource"
					>
						<ArrowLeft class="size-4" />
					</a>
					<div class="space-y-1">
						<div class="flex items-center gap-2.5 flex-wrap">
							<h1 class="text-2xl font-bold tracking-tight">
								{dataset.name}
							</h1>
							<span
								class="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-primary/10 text-primary border border-primary/20"
							>
								{dataset.type || "TABLE"}
							</span>
							<span
								class="px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20"
							>
								{dataset.status || "ACTIVE"}
							</span>
						</div>
						{#if dataset.description}
							<p
								class="text-xs text-muted-foreground line-clamp-2"
							>
								{dataset.description}
							</p>
						{/if}
					</div>
				</div>

				<!-- Header Actions -->
				<div class="flex items-center gap-2 shrink-0">
					<form
						action="?/profile"
						method="POST"
						use:enhance={() => {
							isProfiling = true;
							return async ({ update }) => {
								isProfiling = false;
								await update();
							};
						}}
					>
						<Button
							type="submit"
							disabled={isProfiling}
							class="h-9 px-3.5 rounded-lg text-xs font-medium cursor-pointer gap-1.5 shadow-xs"
						>
							{#if isProfiling}
								<Loader2 class="size-3.5 animate-spin" />
								<span>Profiling Column Stats...</span>
							{:else}
								<Sparkles
									class="size-3.5 text-amber-400 fill-amber-400/20"
								/>
								<span>Run Profiler</span>
							{/if}
						</Button>
					</form>

					<Button
						variant="outline"
						onclick={() => (isDeleteOpen = true)}
						class="h-9 px-3.5 rounded-lg text-xs font-medium cursor-pointer gap-1.5 text-destructive hover:bg-destructive/10 hover:text-destructive border-border"
					>
						<Trash2 class="size-3.5" />
						<span>Delete Dataset</span>
					</Button>
				</div>
			</div>

			<!-- Dataset Metadata Bar -->
			<div
				class="flex items-center gap-4 text-xs text-muted-foreground flex-wrap pt-2 border-t border-border/40"
			>
				<div class="flex items-center gap-1.5">
					<Database class="size-3.5 text-foreground/70" />
					<span>Datasource:</span>
					<a
						href={`/datasources/${dataset.datasourceId}`}
						class="font-medium text-foreground hover:underline"
					>
						{dataset.datasourceName}
					</a>
					<span
						class="font-mono text-[11px] px-1.5 py-0.5 rounded bg-muted border border-border text-muted-foreground"
					>
						{dataset.datasourceType}
					</span>
				</div>

				<span class="text-border/60">•</span>

				<div class="flex items-center gap-1.5">
					<TableProperties class="size-3.5 text-foreground/70" />
					<span>Row Count:</span>
					<span class="font-mono font-semibold text-foreground">
						{dataset.rowCount
							? dataset.rowCount.toLocaleString()
							: "Read-only"}
					</span>
				</div>

				{#if dataset.lastValidated}
					<span class="text-border/60">•</span>
					<div class="flex items-center gap-1.5">
						<Calendar class="size-3.5 text-foreground/70" />
						<span>Last Profiled:</span>
						<span class="font-mono text-foreground">
							{new Date(dataset.lastValidated).toLocaleString()}
						</span>
					</div>
				{/if}
			</div>
		</div>

		<!-- Alert Feedback -->
		{#if form?.success && form?.message}
			<div
				class="p-3.5 text-xs text-emerald-600 dark:text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 rounded-xl"
			>
				{form.message}
			</div>
		{/if}

		{#if form?.error}
			<ErrorAlert error={form.error} title="Dataset Action Failed" />
		{/if}

		<!-- Tabs: Schema & Stats | Data Preview | Quality Rules -->
		<Tabs.Root value={activeTab} onValueChange={(val) => (activeTab = val)}>
			<Tabs.List>
				<Tabs.Trigger value="schema" class="text-xs gap-1.5">
					<TableProperties class="size-3.5" />
					Schema & Column Stats ({dataset.columns?.length ?? 0})
				</Tabs.Trigger>
				<Tabs.Trigger value="preview" class="text-xs gap-1.5">
					<Eye class="size-3.5" />
					Data Preview
				</Tabs.Trigger>
				<Tabs.Trigger value="rules" class="text-xs gap-1.5">
					<ShieldCheck class="size-3.5" />
					Quality Rules ({rules.length})
				</Tabs.Trigger>
			</Tabs.List>

			<!-- TAB 1: SCHEMA & COLUMN STATS -->
			<Tabs.Content value="schema" class="mt-4 space-y-4">
				<Card.Root
					class="rounded-xl border-border bg-card overflow-hidden shadow-xs"
				>
					<Card.Header class="pb-3 border-b border-border">
						<div
							class="flex flex-col sm:flex-row sm:items-center justify-between gap-3"
						>
							<div>
								<Card.Title
									class="text-base font-bold tracking-tight"
									>Column Schema & Statistical Profiles</Card.Title
								>
								<Card.Description
									class="text-xs text-muted-foreground mt-0.5"
								>
									Structural definitions, nullability,
									distinct value counts, and min/max/mean
									statistics.
								</Card.Description>
							</div>
							<div class="flex items-center gap-3">
								<span
									class="text-xs text-muted-foreground font-mono"
								>
									{dataset.columns?.length ?? 0} Total Columns
								</span>
								<form
									action="?/profile"
									method="POST"
									use:enhance={() => {
										isProfiling = true;
										return async ({ update }) => {
											isProfiling = false;
											await update();
										};
									}}
								>
									<Button
										type="submit"
										variant="outline"
										size="sm"
										disabled={isProfiling}
										class="h-8 text-xs font-medium cursor-pointer gap-1.5"
									>
										{#if isProfiling}
											<Loader2
												class="size-3.5 animate-spin"
											/>
											<span>Profiling...</span>
										{:else}
											<Sparkles
												class="size-3.5 text-primary"
											/>
											<span>Run Profiler</span>
										{/if}
									</Button>
								</form>
							</div>
						</div>
					</Card.Header>
					<Card.Content class="p-0">
						{#if !dataset.columns || dataset.columns.length === 0}
							<div
								class="p-8 text-center text-xs text-muted-foreground space-y-3"
							>
								<p class="font-medium text-foreground">
									No column schema extracted yet.
								</p>
								<p class="max-w-md mx-auto">
									Click "Run Profiler" to inspect, extract
									column metadata, and calculate null
									statistics for this table.
								</p>
								<form
									action="?/profile"
									method="POST"
									use:enhance={() => {
										isProfiling = true;
										return async ({ update }) => {
											isProfiling = false;
											await update();
										};
									}}
									class="pt-1"
								>
									<Button
										type="submit"
										variant="default"
										size="sm"
										disabled={isProfiling}
										class="h-8 text-xs font-medium cursor-pointer gap-1.5"
									>
										{#if isProfiling}
											<Loader2
												class="size-3.5 animate-spin"
											/>
											<span>Profiling...</span>
										{:else}
											<Sparkles class="size-3.5" />
											<span>Run Profiler</span>
										{/if}
									</Button>
								</form>
							</div>
						{:else}
							<div class="overflow-x-auto w-full">
								<table
									class="w-full text-left text-xs border-collapse"
								>
									<thead>
										<tr
											class="border-b border-border bg-muted/30 font-mono text-muted-foreground"
										>
											<th class="py-3 px-4 font-medium"
												>Column Name</th
											>
											<th class="py-3 px-4 font-medium"
												>Data Type</th
											>
											<th class="py-3 px-4 font-medium"
												>Key / Nullable</th
											>
											<th class="py-3 px-4 font-medium"
												>Null %</th
											>
											<th class="py-3 px-4 font-medium"
												>Distinct Values</th
											>
											<th class="py-3 px-4 font-medium"
												>Statistical Summary</th
											>
											<th class="py-3 px-4 font-medium text-right w-24"
												>Rules</th
											>
										</tr>
									</thead>
									<tbody class="divide-y divide-border/60">
										{#each dataset.columns as col}
											<tr
												class="hover:bg-accent/30 transition-colors"
											>
												<!-- Name -->
												<td
													class="py-3 px-4 font-medium text-foreground"
												>
													<div
														class="flex items-center gap-1.5"
													>
														{#if col.isPrimaryKey}
															<Key
																class="size-3 text-amber-500 shrink-0"
																title="Primary Key"
															/>
														{/if}
														<span
															class="font-mono text-xs"
															>{col.name}</span
														>
													</div>
												</td>

												<!-- Data Type -->
												<td class="py-3 px-4">
													<span
														class="px-2 py-0.5 rounded text-[11px] font-mono font-semibold bg-muted border border-border"
													>
														{col.dataType}
													</span>
												</td>

												<!-- Key / Nullable -->
												<td class="py-3 px-4">
													{#if col.isPrimaryKey}
														<span
															class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20"
														>
															PRIMARY KEY
														</span>
													{:else if col.isNullable}
														<span
															class="text-muted-foreground"
															>Nullable</span
														>
													{:else}
														<span
															class="font-medium text-foreground"
															>NOT NULL</span
														>
													{/if}
												</td>

												<!-- Null % -->
												<td class="py-3 px-4">
													<div class="space-y-1 w-28">
														<div
															class="flex items-center justify-between text-[11px]"
														>
															<span
																class="font-mono"
																>{col.nullPercentage ??
																	0}%</span
															>
															<span
																class="text-muted-foreground text-[10px]"
																>({col.nullCount ??
																	0})</span
															>
														</div>
														<div
															class="w-full bg-muted rounded-full h-1.5 overflow-hidden"
														>
															<div
																class={`h-full rounded-full transition-all ${getNullPercentageColor(col.nullPercentage ?? 0).split(" ")[0]}`}
																style={`width: ${Math.min(col.nullPercentage ?? 0, 100)}%`}
															></div>
														</div>
													</div>
												</td>

												<!-- Distinct Values -->
												<td class="py-3 px-4 font-mono">
													{col.distinctCount !==
													undefined
														? col.distinctCount.toLocaleString()
														: "—"}
												</td>

												<!-- Statistical Summary -->
												<td
													class="py-3 px-4 text-muted-foreground"
												>
													{#if col.minValue || col.maxValue || col.avgValue}
														<div
															class="flex items-center gap-3 font-mono text-[11px] flex-wrap"
														>
															{#if col.minValue}
																<span
																	>Min: <strong
																		class="text-foreground"
																		>{col.minValue}</strong
																	></span
																>
															{/if}
															{#if col.maxValue}
																<span
																	>Max: <strong
																		class="text-foreground"
																		>{col.maxValue}</strong
																	></span
																>
															{/if}
															{#if col.avgValue}
																<span
																	>Avg: <strong
																		class="text-foreground"
																		>{col.avgValue.toFixed(
																			2,
																		)}</strong
																	></span
																>
															{/if}
														</div>
													{:else}
														<span
															class="text-muted-foreground/60"
															>—</span
														>
													{/if}
												</td>

												<!-- Rule Action -->
												<td class="py-3 px-4 text-right">
													<Button
														type="button"
														variant="ghost"
														size="sm"
														class="h-7 px-2 text-[11px] text-muted-foreground hover:text-foreground hover:bg-accent cursor-pointer gap-1"
														onclick={() => openCreateRule(col.name)}
														title={`Add quality rule for ${col.name}`}
													>
														<ShieldCheck class="size-3.5 text-primary" />
														<span>+ Rule</span>
													</Button>
												</td>
											</tr>
										{/each}
									</tbody>
								</table>
							</div>
						{/if}
					</Card.Content>
				</Card.Root>
			</Tabs.Content>

			<!-- TAB 2: DATA PREVIEW -->
			<Tabs.Content value="preview" class="mt-4 space-y-4">
				<Card.Root
					class="rounded-xl border-border bg-card overflow-hidden shadow-xs"
				>
					<Card.Header class="pb-3 border-b border-border">
						<div class="flex items-center justify-between">
							<div>
								<Card.Title
									class="text-base font-bold tracking-tight flex items-center gap-2"
								>
									<Eye class="size-4 text-primary" />
									<span>Data Preview</span>
								</Card.Title>
								<Card.Description
									class="text-xs text-muted-foreground mt-0.5"
								>
									Live sample records fetched directly from {dataset.datasourceName}.
								</Card.Description>
							</div>
							<span
								class="text-xs text-muted-foreground font-mono"
							>
								Showing Top {preview?.rows?.length ?? 0} Rows
							</span>
						</div>
					</Card.Header>
					<Card.Content class="p-0">
						{#if !preview || !preview.rows || preview.rows.length === 0}
							<div
								class="p-8 text-center text-xs text-muted-foreground space-y-2"
							>
								<p class="font-medium">
									No preview data returned.
								</p>
								<p>
									Ensure the parent datasource connection is
									active and configured correctly.
								</p>
							</div>
						{:else}
							<div class="overflow-x-auto w-full max-h-[500px]">
								<table
									class="w-full text-left text-xs border-collapse"
								>
									<thead
										class="sticky top-0 bg-muted/90 backdrop-blur-xs border-b border-border font-mono text-muted-foreground z-10"
									>
										<tr class="bg-muted/30">
											<th
												class="py-3 px-4 font-medium w-12 text-center text-muted-foreground/70"
												>#</th
											>
											{#each preview.columns as col}
												<th
													class="py-3 px-4 font-medium whitespace-nowrap"
												>
													{col}
												</th>
											{/each}
										</tr>
									</thead>
									<tbody
										class="divide-y divide-border/60 font-mono text-xs"
									>
										{#each preview.rows as row, idx}
											<tr
												class="hover:bg-accent/30 transition-colors"
											>
												<td
													class="py-3 px-4 text-center font-mono text-muted-foreground/70 text-[11px] select-none border-e border-border/40 w-12"
												>
													{idx + 1}
												</td>
												{#each preview.columns as col}
													<td
														class="py-3 px-4 whitespace-nowrap max-w-xs truncate text-foreground"
													>
														{row[col] !== null &&
														row[col] !== undefined
															? String(row[col])
															: "null"}
													</td>
												{/each}
											</tr>
										{/each}
									</tbody>
								</table>
							</div>
						{/if}
					</Card.Content>
				</Card.Root>
			</Tabs.Content>

			<!-- TAB 3: QUALITY RULES -->
			<Tabs.Content value="rules" class="mt-4 space-y-4">
				<Card.Root class="rounded-xl border-border bg-card shadow-xs">
					<Card.Header class="pb-3 border-b border-border">
						<div class="flex items-center justify-between">
							<div>
								<Card.Title
									class="text-base font-bold tracking-tight flex items-center gap-2"
								>
									<ShieldCheck class="size-4 text-primary" />
									<span>Attached Quality Rules</span>
								</Card.Title>
								<Card.Description
									class="text-xs text-muted-foreground mt-0.5"
								>
									Quality validations and assertions configured for {dataset.name}.
								</Card.Description>
							</div>
							<Button
								size="sm"
								onclick={() => openCreateRule()}
								class="h-8 text-xs font-medium cursor-pointer gap-1.5"
							>
								<Plus class="size-3.5" />
								<span>Add Quality Rule</span>
							</Button>
						</div>
					</Card.Header>
					<Card.Content class="p-0">
						{#if rules.length === 0}
							<div
								class="p-8 text-center text-xs text-muted-foreground space-y-3"
							>
								<div
									class="mx-auto w-10 h-10 rounded-full bg-muted flex items-center justify-center"
								>
									<ShieldCheck class="size-5 text-muted-foreground" />
								</div>
								<div class="space-y-1">
									<p class="font-medium text-foreground text-sm">
										No quality rules attached to this dataset yet.
									</p>
									<p class="max-w-md mx-auto">
										Define null checks, uniqueness constraints, or value
										range expectations to validate your data automatically.
									</p>
								</div>
								<div class="pt-2">
									<Button
										size="sm"
										onclick={() => openCreateRule()}
										class="h-8 text-xs font-medium cursor-pointer gap-1.5"
									>
										<Plus class="size-3.5" />
										<span>Add Quality Rule</span>
									</Button>
								</div>
							</div>
						{:else}
							<div class="overflow-x-auto w-full">
								<table class="w-full text-left text-xs border-collapse">
									<thead>
										<tr class="border-b border-border bg-muted/30 font-mono text-muted-foreground">
											<th class="py-3 px-4 font-medium">Rule Name</th>
											<th class="py-3 px-4 font-medium">Target Column</th>
											<th class="py-3 px-4 font-medium">Category</th>
											<th class="py-3 px-4 font-medium">Expectation</th>
											<th class="py-3 px-4 font-medium">Severity</th>
											<th class="py-3 px-4 font-medium">Status</th>
											<th class="py-3 px-4 font-medium text-right">Actions</th>
										</tr>
									</thead>
									<tbody class="divide-y divide-border/60">
										{#each rules as rule}
											<tr class="hover:bg-accent/30 transition-colors">
												<!-- Rule Name & Description -->
												<td class="py-3 px-4 font-medium text-foreground max-w-xs">
													<div class="space-y-0.5">
														<div class="font-semibold text-xs text-foreground">
															{rule.name}
														</div>
														{#if rule.description}
															<div class="text-[11px] text-muted-foreground truncate" title={rule.description}>
																{rule.description}
															</div>
														{/if}
													</div>
												</td>

												<!-- Target Column -->
												<td class="py-3 px-4 font-mono">
													<span class="px-2 py-0.5 rounded font-mono text-[11px] bg-muted border border-border">
														{rule.target}
													</span>
												</td>

												<!-- Category -->
												<td class="py-3 px-4">
													<span class={`px-2 py-0.5 rounded-full text-[10px] font-semibold border ${getCategoryBadge(rule.category)}`}>
														{rule.category}
													</span>
												</td>

												<!-- Expectation -->
												<td class="py-3 px-4 font-mono text-xs">
													<code class="px-1.5 py-0.5 rounded bg-muted/60 text-foreground font-mono text-[11px]">
														{rule.expectation}
													</code>
													{#if rule.conditionExpression}
														<div class="text-[10px] text-muted-foreground/80 mt-0.5 truncate max-w-[180px]" title={rule.conditionExpression}>
															expr: {rule.conditionExpression}
														</div>
													{/if}
												</td>

												<!-- Severity -->
												<td class="py-3 px-4">
													<span class={`px-2 py-0.5 rounded-full text-[10px] font-semibold border ${getSeverityBadge(rule.severity)}`}>
														{rule.severity}
													</span>
												</td>

												<!-- Status Toggle -->
												<td class="py-3 px-4">
													<form
														action="?/toggleRule"
														method="POST"
														use:enhance={() => {
															return async ({ update }) => {
																await update();
															};
														}}
													>
														<input type="hidden" name="id" value={rule.id} />
														<button
															type="submit"
															class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[11px] font-medium transition-colors border cursor-pointer"
															class:bg-muted={!rule.enabled}
															class:text-muted-foreground={!rule.enabled}
															class:border-border={!rule.enabled}
															style={rule.enabled ? "background-color: rgba(16, 185, 129, 0.1); color: rgb(16, 185, 129); border-color: rgba(16, 185, 129, 0.2);" : ""}
															title="Click to toggle rule"
														>
															<span class="size-1.5 rounded-full" style={rule.enabled ? "background-color: rgb(16, 185, 129);" : "background-color: currentColor;"}></span>
															<span>{rule.enabled ? "Active" : "Disabled"}</span>
														</button>
													</form>
												</td>

												<!-- Actions -->
												<td class="py-3 px-4 text-right">
													<div class="inline-flex items-center justify-end gap-1">
														<Button
															type="button"
															variant="ghost"
															size="icon"
															onclick={() => openEditRule(rule)}
															title="Edit rule parameters"
															class="size-7 text-muted-foreground hover:text-foreground cursor-pointer rounded-md"
														>
															<Edit class="size-3.5" />
														</Button>
														<Button
															type="button"
															variant="ghost"
															size="icon"
															onclick={() => openDeleteRule(rule)}
															title="Delete rule"
															class="size-7 text-muted-foreground hover:text-destructive hover:bg-destructive/10 cursor-pointer rounded-md"
														>
															<Trash2 class="size-3.5" />
														</Button>
													</div>
												</td>
											</tr>
										{/each}
									</tbody>
								</table>
							</div>
						{/if}
					</Card.Content>
				</Card.Root>
			</Tabs.Content>
		</Tabs.Root>
	</div>

	<!-- Delete Dataset Confirmation Dialog -->
	<Dialog.Root bind:open={isDeleteOpen}>
		<Dialog.Content
			class="sm:max-w-md rounded-xl p-6 border-border bg-card"
		>
			<Dialog.Header class="space-y-2">
				<div class="flex items-center gap-3">
					<div
						class="w-10 h-10 rounded-full bg-destructive/10 border border-destructive/20 flex items-center justify-center text-destructive shrink-0"
					>
						<Trash2 class="size-5" />
					</div>
					<div>
						<Dialog.Title
							class="text-base font-bold tracking-tight text-foreground"
						>
							Delete Dataset
						</Dialog.Title>
						<Dialog.Description
							class="text-xs text-muted-foreground mt-0.5"
						>
							Are you sure you want to delete dataset <span
								class="font-bold text-foreground"
								>'{dataset.name}'</span
							>?
						</Dialog.Description>
					</div>
				</div>
			</Dialog.Header>

			<div
				class="p-3 text-xs text-muted-foreground bg-muted/40 border border-border rounded-lg space-y-1 my-2"
			>
				<p class="font-medium text-foreground">
					This action cannot be undone.
				</p>
				<p>
					Deleting this dataset will remove its extracted schema,
					column profiles, quality rules, and validation history.
				</p>
			</div>

			<Dialog.Footer class="pt-2 flex items-center justify-end gap-2">
				<Button
					type="button"
					variant="outline"
					onclick={() => (isDeleteOpen = false)}
					disabled={isDeleting}
					class="h-9 rounded-lg text-xs"
				>
					Cancel
				</Button>

				<form
					action="?/delete"
					method="POST"
					use:enhance={() => {
						isDeleting = true;
						return async ({ update }) => {
							isDeleting = false;
							isDeleteOpen = false;
							await update();
						};
					}}
				>
					<Button
						type="submit"
						disabled={isDeleting}
						class="h-9 rounded-lg text-xs font-medium bg-destructive hover:bg-destructive/90 text-destructive-foreground cursor-pointer"
					>
						{#if isDeleting}
							<Loader2 class="size-3.5 me-1.5 animate-spin" />
							<span>Deleting...</span>
						{:else}
							<Trash2 class="size-3.5 me-1.5" />
							<span>Confirm Delete</span>
						{/if}
					</Button>
				</form>
			</Dialog.Footer>
		</Dialog.Content>
	</Dialog.Root>

	<!-- =========================================================================
	     Create Quality Rule Modal
	     ========================================================================= -->
	<Dialog.Root bind:open={isCreateRuleOpen}>
		<Dialog.Content class="w-[95vw] sm:max-w-4xl md:max-w-5xl lg:max-w-6xl xl:max-w-7xl max-h-[92vh] overflow-y-auto rounded-xl p-6 sm:p-8 border-border bg-card shadow-2xl">
			<Dialog.Header class="flex flex-col gap-1">
				<Dialog.Title class="text-lg font-bold tracking-tight flex items-center gap-2">
					<ShieldCheck class="size-5 text-primary" />
					<span>Create Quality Rule</span>
				</Dialog.Title>
				<Dialog.Description class="text-xs text-muted-foreground">
					Add validation rule for dataset '{dataset.name}'.
				</Dialog.Description>
			</Dialog.Header>

			<form
				action="?/createRule"
				method="POST"
				use:enhance={() => {
					isSubmittingRule = true;
					return async ({ update, result }) => {
						isSubmittingRule = false;
						if (result.type === "success") {
							isCreateRuleOpen = false;
						}
						await update();
					};
				}}
				class="flex flex-col gap-4 pt-3 text-xs"
			>
				<Field.FieldGroup class="flex flex-col gap-4">
					<!-- Top Row: Rule Name & Target Column (2-col) -->
					<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
						<Field.Field>
							<Field.Label for="create-rule-name">Rule Name *</Field.Label>
							<Input
								id="create-rule-name"
								name="name"
								bind:value={createRuleName}
								placeholder="e.g. User Email Format Valid"
								required
								disabled={isSubmittingRule}
								class="h-9 text-xs"
							/>
						</Field.Field>

						<Field.Field>
							<Field.Label for="create-rule-target">Target Column *</Field.Label>
							<Input
								id="create-rule-target"
								name="target"
								bind:value={createRuleTarget}
								placeholder="e.g. column_name or *"
								list="create-dataset-columns"
								required
								disabled={isSubmittingRule}
								class="h-9 text-xs font-mono"
							/>
							<datalist id="create-dataset-columns">
								<option value="*">* (All Columns / Table Level)</option>
								{#each dataset.columns || [] as col}
									<option value={col.name}>{col.name} ({col.dataType})</option>
								{/each}
							</datalist>
						</Field.Field>
					</div>

					<!-- Second Row: Category, Severity & Expectation (3-col) -->
					<div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
						<Field.Field>
							<Field.Label for="create-rule-category">Category *</Field.Label>
							<select
								id="create-rule-category"
								name="category"
								bind:value={createRuleCategory}
								required
								disabled={isSubmittingRule}
								class="w-full h-9 px-2.5 rounded-md border border-input bg-background text-foreground text-xs cursor-pointer"
							>
								{#each categories as cat}
									<option value={cat.value}>{cat.label}</option>
								{/each}
							</select>
						</Field.Field>

						<Field.Field>
							<Field.Label for="create-rule-severity">Severity *</Field.Label>
							<select
								id="create-rule-severity"
								name="severity"
								bind:value={createRuleSeverity}
								disabled={isSubmittingRule}
								class="w-full h-9 px-2.5 rounded-md border border-input bg-background text-foreground text-xs cursor-pointer"
							>
								{#each severities as sev}
									<option value={sev.value}>{sev.label}</option>
								{/each}
							</select>
						</Field.Field>

						<Field.Field>
							<Field.Label for="create-rule-expectation">Expectation *</Field.Label>
							<Input
								id="create-rule-expectation"
								name="expectation"
								bind:value={createRuleExpectation}
								placeholder="e.g. is_not_null, unique, range"
								list="rule-expectations-list"
								required
								disabled={isSubmittingRule}
								class="h-9 text-xs font-mono"
							/>
							<datalist id="rule-expectations-list">
								<option value="is_not_null">is_not_null</option>
								<option value="unique">unique</option>
								<option value="is_in_set">is_in_set</option>
								<option value="regex_match">regex_match</option>
								<option value="min_value">min_value</option>
								<option value="max_value">max_value</option>
								<option value="range">range</option>
								<option value="custom_sql">custom_sql</option>
							</datalist>
						</Field.Field>
					</div>

					<!-- Third Row: Description -->
					<Field.Field>
						<Field.Label for="create-rule-desc">Description (Optional)</Field.Label>
						<Input
							id="create-rule-desc"
							name="description"
							bind:value={createRuleDescription}
							placeholder="Brief purpose or rationale for this rule"
							disabled={isSubmittingRule}
							class="h-9 text-xs"
						/>
					</Field.Field>

					<!-- Condition Expression (SQL Editor) -->
					<Field.Field>
						<div class="flex items-center justify-between pb-1">
							<Field.Label for="create-rule-condition" class="text-xs font-semibold">
								Condition Expression (SQL Predicate)
							</Field.Label>
							<span class="text-[11px] text-muted-foreground font-sans">
								Evaluated as a SQL WHERE condition
							</span>
						</div>
						<SqlEditor
							id="create-rule-condition"
							name="conditionExpression"
							bind:value={createRuleCondition}
							columns={dataset.columns || []}
							disabled={isSubmittingRule}
						/>
					</Field.Field>

					<!-- Enabled Toggle -->
					<div class="flex items-center gap-2 pt-1">
						<input
							id="create-rule-enabled"
							type="checkbox"
							name="enabled"
							value="true"
							bind:checked={createRuleEnabled}
							disabled={isSubmittingRule}
							class="size-4 rounded border-input text-primary focus:ring-primary cursor-pointer"
						/>
						<label for="create-rule-enabled" class="text-xs font-medium text-foreground cursor-pointer">
							Enable rule immediately upon creation
						</label>
					</div>
				</Field.FieldGroup>

				<Dialog.Footer class="pt-2 flex items-center justify-end gap-2">
					<Button
						type="button"
						variant="outline"
						onclick={() => (isCreateRuleOpen = false)}
						disabled={isSubmittingRule}
						class="h-9 rounded-lg text-xs"
					>
						Cancel
					</Button>
					<Button
						type="submit"
						disabled={isSubmittingRule}
						class="h-9 rounded-lg font-medium text-xs cursor-pointer gap-1.5"
					>
						{#if isSubmittingRule}
							<Loader2 class="size-3.5 animate-spin" />
							<span>Creating...</span>
						{:else}
							<Plus class="size-3.5" />
							<span>Create Rule</span>
						{/if}
					</Button>
				</Dialog.Footer>
			</form>
		</Dialog.Content>
	</Dialog.Root>

	<!-- =========================================================================
	     Edit Quality Rule Modal
	     ========================================================================= -->
	<Dialog.Root bind:open={isEditRuleOpen}>
		{#if selectedRule}
			<Dialog.Content class="w-[95vw] sm:max-w-4xl md:max-w-5xl lg:max-w-6xl xl:max-w-7xl max-h-[92vh] overflow-y-auto rounded-xl p-6 sm:p-8 border-border bg-card shadow-2xl">
				<Dialog.Header class="flex flex-col gap-1">
					<Dialog.Title class="text-lg font-bold tracking-tight flex items-center gap-2">
						<Edit class="size-5 text-primary" />
						<span>Edit Quality Rule</span>
					</Dialog.Title>
					<Dialog.Description class="text-xs text-muted-foreground">
						Update validation parameters for '{selectedRule.name}'.
					</Dialog.Description>
				</Dialog.Header>

				<form
					action="?/updateRule"
					method="POST"
					use:enhance={() => {
						isSubmittingRule = true;
						return async ({ update, result }) => {
							isSubmittingRule = false;
							if (result.type === "success") {
								isEditRuleOpen = false;
							}
							await update();
						};
					}}
					class="flex flex-col gap-4 pt-3 text-xs"
				>
					<input type="hidden" name="id" value={selectedRule.id} />

					<Field.FieldGroup class="flex flex-col gap-4">
						<!-- Top Row: Rule Name & Target Column (2-col) -->
						<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
							<Field.Field>
								<Field.Label for="edit-rule-name">Rule Name *</Field.Label>
								<Input
									id="edit-rule-name"
									name="name"
									value={selectedRule.name}
									required
									disabled={isSubmittingRule}
									class="h-9 text-xs"
								/>
							</Field.Field>

							<Field.Field>
								<Field.Label for="edit-rule-target">Target Column *</Field.Label>
								<Input
									id="edit-rule-target"
									name="target"
									value={selectedRule.target}
									list="edit-dataset-columns"
									required
									disabled={isSubmittingRule}
									class="h-9 text-xs font-mono"
								/>
								<datalist id="edit-dataset-columns">
									<option value="*">* (All Columns / Table Level)</option>
									{#each dataset.columns || [] as col}
										<option value={col.name}>{col.name} ({col.dataType})</option>
									{/each}
								</datalist>
							</Field.Field>
						</div>

						<!-- Second Row: Category, Severity & Expectation (3-col) -->
						<div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
							<Field.Field>
								<Field.Label for="edit-rule-category">Category *</Field.Label>
								<select
									id="edit-rule-category"
									name="category"
									value={selectedRule.category}
									required
									disabled={isSubmittingRule}
									class="w-full h-9 px-2.5 rounded-md border border-input bg-background text-foreground text-xs cursor-pointer"
								>
									{#each categories as cat}
										<option value={cat.value}>{cat.label}</option>
									{/each}
								</select>
							</Field.Field>

							<Field.Field>
								<Field.Label for="edit-rule-severity">Severity *</Field.Label>
								<select
									id="edit-rule-severity"
									name="severity"
									value={selectedRule.severity}
									disabled={isSubmittingRule}
									class="w-full h-9 px-2.5 rounded-md border border-input bg-background text-foreground text-xs cursor-pointer"
								>
									{#each severities as sev}
										<option value={sev.value}>{sev.label}</option>
									{/each}
								</select>
							</Field.Field>

							<Field.Field>
								<Field.Label for="edit-rule-expectation">Expectation *</Field.Label>
								<Input
									id="edit-rule-expectation"
									name="expectation"
									value={selectedRule.expectation}
									list="rule-expectations-list"
									required
									disabled={isSubmittingRule}
									class="h-9 text-xs font-mono"
								/>
							</Field.Field>
						</div>

						<!-- Third Row: Description -->
						<Field.Field>
							<Field.Label for="edit-rule-desc">Description (Optional)</Field.Label>
							<Input
								id="edit-rule-desc"
								name="description"
								value={selectedRule.description || ""}
								placeholder="Brief purpose or rationale for this rule"
								disabled={isSubmittingRule}
								class="h-9 text-xs"
							/>
						</Field.Field>

						<!-- Condition Expression (SQL Editor) -->
						<Field.Field>
							<div class="flex items-center justify-between pb-1">
								<Field.Label for="edit-rule-condition" class="text-xs font-semibold">
									Condition Expression (SQL Predicate)
								</Field.Label>
								<span class="text-[11px] text-muted-foreground font-sans">
									Evaluated as a SQL WHERE condition
								</span>
							</div>
							<SqlEditor
								id="edit-rule-condition"
								name="conditionExpression"
								bind:value={selectedRule.conditionExpression}
								columns={dataset.columns || []}
								disabled={isSubmittingRule}
							/>
						</Field.Field>

						<!-- Description -->
						<Field.Field>
							<Field.Label for="edit-rule-desc">Description (Optional)</Field.Label>
							<Input
								id="edit-rule-desc"
								name="description"
								value={selectedRule.description || ""}
								placeholder="Brief purpose or rationale for this rule"
								disabled={isSubmittingRule}
								class="h-9 text-xs"
							/>
						</Field.Field>

						<!-- Enabled Toggle -->
						<div class="flex items-center gap-2 pt-1">
							<input
								id="edit-rule-enabled"
								type="checkbox"
								name="enabled"
								value="true"
								checked={selectedRule.enabled}
								disabled={isSubmittingRule}
								class="size-4 rounded border-input text-primary focus:ring-primary cursor-pointer"
							/>
							<label for="edit-rule-enabled" class="text-xs font-medium text-foreground cursor-pointer">
								Rule is currently enabled
							</label>
						</div>
					</Field.FieldGroup>

					<Dialog.Footer class="pt-2 flex items-center justify-end gap-2">
						<Button
							type="button"
							variant="outline"
							onclick={() => (isEditRuleOpen = false)}
							disabled={isSubmittingRule}
							class="h-9 rounded-lg text-xs"
						>
							Cancel
						</Button>
						<Button
							type="submit"
							disabled={isSubmittingRule}
							class="h-9 rounded-lg font-medium text-xs cursor-pointer gap-1.5"
						>
							{#if isSubmittingRule}
								<Loader2 class="size-3.5 animate-spin" />
								<span>Saving...</span>
							{:else}
								<span>Save Changes</span>
							{/if}
						</Button>
					</Dialog.Footer>
				</form>
			</Dialog.Content>
		{/if}
	</Dialog.Root>

	<!-- =========================================================================
	     Delete Quality Rule Confirmation Modal
	     ========================================================================= -->
	<Dialog.Root bind:open={isDeleteRuleOpen}>
		{#if selectedRule}
			<Dialog.Content class="sm:max-w-md rounded-xl p-6 border-border bg-card">
				<Dialog.Header class="space-y-2">
					<div class="flex items-center gap-3">
						<div class="w-10 h-10 rounded-full bg-destructive/10 border border-destructive/20 flex items-center justify-center text-destructive shrink-0">
							<Trash2 class="size-5" />
						</div>
						<div>
							<Dialog.Title class="text-base font-bold tracking-tight text-foreground">
								Delete Quality Rule
							</Dialog.Title>
							<Dialog.Description class="text-xs text-muted-foreground mt-0.5">
								Are you sure you want to delete <span class="font-bold text-foreground">'{selectedRule.name}'</span>?
							</Dialog.Description>
						</div>
					</div>
				</Dialog.Header>

				<div class="p-3 text-xs text-muted-foreground bg-muted/40 border border-border rounded-lg space-y-1 my-2">
					<p class="font-medium text-foreground">
						This action cannot be undone.
					</p>
					<p>
						Target column <span class="font-mono text-foreground font-semibold">{selectedRule.target}</span> will no longer be validated with expectation <code class="font-mono text-foreground">{selectedRule.expectation}</code>.
					</p>
				</div>

				<Dialog.Footer class="pt-2 flex items-center justify-end gap-2">
					<Button
						type="button"
						variant="outline"
						onclick={() => (isDeleteRuleOpen = false)}
						disabled={isSubmittingRule}
						class="h-9 rounded-lg text-xs"
					>
						Cancel
					</Button>

					<form
						action="?/deleteRule"
						method="POST"
						use:enhance={() => {
							isSubmittingRule = true;
							return async ({ update, result }) => {
								isSubmittingRule = false;
								if (result.type === "success") {
									isDeleteRuleOpen = false;
								}
								await update();
							};
						}}
					>
						<input type="hidden" name="id" value={selectedRule.id} />
						<Button
							type="submit"
							disabled={isSubmittingRule}
							class="h-9 rounded-lg text-xs font-medium bg-destructive hover:bg-destructive/90 text-destructive-foreground cursor-pointer gap-1.5"
						>
							{#if isSubmittingRule}
								<Loader2 class="size-3.5 animate-spin" />
								<span>Deleting...</span>
							{:else}
								<Trash2 class="size-3.5" />
								<span>Confirm Delete</span>
							{/if}
						</Button>
					</form>
				</Dialog.Footer>
			</Dialog.Content>
		{/if}
	</Dialog.Root>
{/if}
