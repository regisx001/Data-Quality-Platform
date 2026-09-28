<script lang="ts">
	import { enhance } from "$app/forms";
	import { goto } from "$app/navigation";
	import { Button } from "$lib/components/ui/button/index.js";
	import * as Card from "$lib/components/ui/card/index.js";
	import * as Dialog from "$lib/components/ui/dialog/index.js";
	import * as Field from "$lib/components/ui/field/index.js";
	import { Input } from "$lib/components/ui/input/index.js";
	import { Badge } from "$lib/components/ui/badge/index.js";
	import Plus from "@lucide/svelte/icons/plus";
	import Search from "@lucide/svelte/icons/search";
	import ShieldCheck from "@lucide/svelte/icons/shield-check";
	import ShieldAlert from "@lucide/svelte/icons/shield-alert";
	import CheckCircle2 from "@lucide/svelte/icons/check-circle-2";
	import XCircle from "@lucide/svelte/icons/x-circle";
	import Edit from "@lucide/svelte/icons/edit";
	import Trash2 from "@lucide/svelte/icons/trash-2";
	import Loader2 from "@lucide/svelte/icons/loader-2";
	import TableProperties from "@lucide/svelte/icons/table-properties";
	import Filter from "@lucide/svelte/icons/filter";
	import ErrorAlert from "$lib/components/ui/error-alert.svelte";
	import SqlEditor from "$lib/components/ui/sql-editor.svelte";
	import type { PageData, ActionData } from "./$types";
	import type { QualityRule, RuleCategory, RuleSeverity } from "$lib/server/api";

	let { data, form }: { data: PageData; form: ActionData } = $props();

	let rules = $derived(data.rules || []);
	let availableDatasets = $derived(data.availableDatasets || []);

	// Search & local filter states
	let searchQuery = $state("");
	let selectedDatasetFilter = $state<string>("ALL");
	let selectedCategoryFilter = $state<string>("ALL");
	let selectedSeverityFilter = $state<string>("ALL");
	let selectedStatusFilter = $state<string>("ALL");

	// Dialog states
	let isCreateOpen = $state(false);
	let isEditOpen = $state(false);
	let isDeleteOpen = $state(false);
	let selectedRule = $state<QualityRule | null>(null);
	let isSubmitting = $state(false);
	let createCondition = $state("");

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

	// Derived filtered rules
	let filteredRules = $derived(
		rules.filter((rule) => {
			const matchesDataset =
				selectedDatasetFilter === "ALL" || rule.datasetId === selectedDatasetFilter;
			const matchesCategory =
				selectedCategoryFilter === "ALL" || rule.category === selectedCategoryFilter;
			const matchesSeverity =
				selectedSeverityFilter === "ALL" || rule.severity === selectedSeverityFilter;
			const matchesStatus =
				selectedStatusFilter === "ALL" ||
				(selectedStatusFilter === "ENABLED" && rule.enabled) ||
				(selectedStatusFilter === "DISABLED" && !rule.enabled);

			const query = searchQuery.toLowerCase().trim();
			const matchesQuery =
				!query ||
				rule.name.toLowerCase().includes(query) ||
				(rule.description && rule.description.toLowerCase().includes(query)) ||
				rule.target.toLowerCase().includes(query) ||
				rule.expectation.toLowerCase().includes(query) ||
				(rule.datasetName && rule.datasetName.toLowerCase().includes(query));

			return matchesDataset && matchesCategory && matchesSeverity && matchesStatus && matchesQuery;
		})
	);

	// Quick stats
	let totalCount = $derived(rules.length);
	let enabledCount = $derived(rules.filter((r) => r.enabled).length);
	let criticalCount = $derived(rules.filter((r) => r.severity === "CRITICAL" || r.severity === "HIGH").length);
	let categoriesCovered = $derived(new Set(rules.map((r) => r.category)).size);

	function openEdit(rule: QualityRule) {
		selectedRule = rule;
		isEditOpen = true;
	}

	function openDelete(rule: QualityRule) {
		selectedRule = rule;
		isDeleteOpen = true;
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
</script>

<svelte:head>
	<title>Quality Rules | Data Quality Platform</title>
	<meta name="description" content="Configure and manage automated dataset quality validation rules." />
</svelte:head>

<div class="p-6 sm:p-8 w-full flex flex-col gap-6">
	<!-- Page Header -->
	<div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-6">
		<div class="flex flex-col gap-1">
			<div class="flex items-center gap-2.5">
				<ShieldCheck class="size-6 text-primary" />
				<h1 class="text-2xl font-bold tracking-tight">Quality Rules</h1>
			</div>
			<p class="text-xs sm:text-sm text-muted-foreground">
				Configure automated quality validation constraints, column assertions, and data integrity expectations.
			</p>
		</div>

		<Button onclick={() => (isCreateOpen = true)} class="gap-1.5 cursor-pointer shrink-0">
			<Plus class="size-4" data-icon="inline-start" />
			<span>New Quality Rule</span>
		</Button>
	</div>

	<!-- Alert Messages -->
	{#if form?.success && form?.message}
		<div class="p-4 text-xs sm:text-sm text-emerald-600 dark:text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 rounded-xl flex items-center gap-2">
			<CheckCircle2 class="size-4 shrink-0" />
			<span>{form.message}</span>
		</div>
	{/if}

	{#if form?.error}
		<ErrorAlert error={form.error} title="Rule Operation Failed" />
	{/if}

	<!-- Quick Metrics Summary Cards -->
	<div class="grid grid-cols-2 md:grid-cols-4 gap-4">
		<Card.Root class="rounded-xl border-border bg-card shadow-xs">
			<Card.Header class="pb-2">
				<Card.Description class="text-xs font-medium text-muted-foreground">Total Rules</Card.Description>
				<Card.Title class="text-2xl font-bold font-mono">{totalCount}</Card.Title>
			</Card.Header>
		</Card.Root>

		<Card.Root class="rounded-xl border-border bg-card shadow-xs">
			<Card.Header class="pb-2">
				<Card.Description class="text-xs font-medium text-muted-foreground">Active & Enabled</Card.Description>
				<Card.Title class="text-2xl font-bold font-mono text-emerald-600 dark:text-emerald-400">
					{enabledCount}
				</Card.Title>
			</Card.Header>
		</Card.Root>

		<Card.Root class="rounded-xl border-border bg-card shadow-xs">
			<Card.Header class="pb-2">
				<Card.Description class="text-xs font-medium text-muted-foreground">High / Critical Severity</Card.Description>
				<Card.Title class="text-2xl font-bold font-mono text-amber-600 dark:text-amber-400">
					{criticalCount}
				</Card.Title>
			</Card.Header>
		</Card.Root>

		<Card.Root class="rounded-xl border-border bg-card shadow-xs">
			<Card.Header class="pb-2">
				<Card.Description class="text-xs font-medium text-muted-foreground">Categories Covered</Card.Description>
				<Card.Title class="text-2xl font-bold font-mono">{categoriesCovered} / 7</Card.Title>
			</Card.Header>
		</Card.Root>
	</div>

	<!-- Filter Controls Bar -->
	<Card.Root class="rounded-xl border-border bg-card shadow-xs">
		<Card.Content class="p-4 flex flex-col md:flex-row md:items-center justify-between gap-3">
			<!-- Search Input -->
			<div class="relative flex-1 min-w-[200px]">
				<Search class="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
				<Input
					bind:value={searchQuery}
					type="search"
					placeholder="Search rules by name, target column, or dataset..."
					class="pl-9 h-9 text-xs"
				/>
			</div>

			<!-- Filter Selectors -->
			<div class="flex items-center gap-2 flex-wrap">
				<!-- Category Filter -->
				<select
					bind:value={selectedCategoryFilter}
					class="h-9 px-2.5 rounded-md border border-input bg-background text-foreground text-xs cursor-pointer"
					aria-label="Filter by Category"
				>
					<option value="ALL">All Categories</option>
					{#each categories as cat}
						<option value={cat.value}>{cat.label}</option>
					{/each}
				</select>

				<!-- Severity Filter -->
				<select
					bind:value={selectedSeverityFilter}
					class="h-9 px-2.5 rounded-md border border-input bg-background text-foreground text-xs cursor-pointer"
					aria-label="Filter by Severity"
				>
					<option value="ALL">All Severities</option>
					{#each severities as sev}
						<option value={sev.value}>{sev.label}</option>
					{/each}
				</select>

				<!-- Status Filter -->
				<select
					bind:value={selectedStatusFilter}
					class="h-9 px-2.5 rounded-md border border-input bg-background text-foreground text-xs cursor-pointer"
					aria-label="Filter by Status"
				>
					<option value="ALL">All Statuses</option>
					<option value="ENABLED">Enabled</option>
					<option value="DISABLED">Disabled</option>
				</select>

				<!-- Dataset Filter -->
				<select
					bind:value={selectedDatasetFilter}
					class="h-9 px-2.5 rounded-md border border-input bg-background text-foreground text-xs cursor-pointer max-w-[180px] truncate"
					aria-label="Filter by Dataset"
				>
					<option value="ALL">All Datasets</option>
					{#each availableDatasets as ds}
						<option value={ds.id}>{ds.name}</option>
					{/each}
				</select>
			</div>
		</Card.Content>
	</Card.Root>

	<!-- Quality Rules Table -->
	<Card.Root class="rounded-xl border-border bg-card shadow-xs overflow-hidden">
		<Card.Header class="pb-3 border-b border-border">
			<div class="flex items-center justify-between">
				<div class="flex flex-col gap-0.5">
					<Card.Title class="text-base font-bold tracking-tight flex items-center gap-2">
						<ShieldCheck class="size-4 text-primary" />
						<span>Configured Quality Rules</span>
					</Card.Title>
					<Card.Description class="text-xs text-muted-foreground">
						Showing {filteredRules.length} of {rules.length} rule(s)
					</Card.Description>
				</div>
			</div>
		</Card.Header>

		<Card.Content class="p-0">
			{#if filteredRules.length === 0}
				<div class="p-12 text-center text-xs text-muted-foreground flex flex-col items-center gap-3">
					<div class="size-12 rounded-full bg-muted flex items-center justify-center">
						<ShieldCheck class="size-6 text-muted-foreground" />
					</div>
					<div class="flex flex-col gap-1">
						<p class="font-medium text-foreground text-sm">No quality rules found</p>
						<p class="max-w-md">
							{#if searchQuery || selectedCategoryFilter !== "ALL" || selectedSeverityFilter !== "ALL" || selectedStatusFilter !== "ALL" || selectedDatasetFilter !== "ALL"}
								No rules match the selected filter criteria. Try clearing your filters.
							{:else}
								Get started by creating your first automated data quality validation rule.
							{/if}
						</p>
					</div>
					{#if !searchQuery && selectedCategoryFilter === "ALL" && selectedSeverityFilter === "ALL" && selectedStatusFilter === "ALL" && selectedDatasetFilter === "ALL"}
						<Button onclick={() => (isCreateOpen = true)} size="sm" class="gap-1.5 cursor-pointer mt-2">
							<Plus class="size-3.5" data-icon="inline-start" />
							<span>Create Quality Rule</span>
						</Button>
					{/if}
				</div>
			{:else}
				<div class="overflow-x-auto w-full">
					<table class="w-full text-left text-xs border-collapse">
						<thead>
							<tr class="border-b border-border bg-muted/30 font-mono text-muted-foreground">
								<th class="py-3 px-4 font-medium">Rule Name</th>
								<th class="py-3 px-4 font-medium">Category</th>
								<th class="py-3 px-4 font-medium">Severity</th>
								<th class="py-3 px-4 font-medium">Target</th>
								<th class="py-3 px-4 font-medium">Expectation</th>
								<th class="py-3 px-4 font-medium">Dataset</th>
								<th class="py-3 px-4 font-medium">Status</th>
								<th class="py-3 px-4 font-medium text-right">Actions</th>
							</tr>
						</thead>
						<tbody class="divide-y divide-border/60">
							{#each filteredRules as rule (rule.id)}
								<tr class="hover:bg-accent/30 transition-colors">
									<!-- Rule Name & Description -->
									<td class="py-3 px-4 font-medium text-foreground">
										<div class="flex flex-col gap-0.5">
											<span class="font-semibold text-foreground">{rule.name}</span>
											{#if rule.description}
												<span class="text-[11px] text-muted-foreground line-clamp-1 max-w-xs">{rule.description}</span>
											{/if}
										</div>
									</td>

									<!-- Category Badge -->
									<td class="py-3 px-4">
										<span class={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold border ${getCategoryBadge(rule.category)}`}>
											{rule.category}
										</span>
									</td>

									<!-- Severity Badge -->
									<td class="py-3 px-4">
										<span class={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold border ${getSeverityBadge(rule.severity)}`}>
											{rule.severity}
										</span>
									</td>

									<!-- Target Column -->
									<td class="py-3 px-4 font-mono text-foreground font-semibold">
										<span class="px-1.5 py-0.5 rounded bg-muted border border-border text-[11px]">
											{rule.target}
										</span>
									</td>

									<!-- Expectation -->
									<td class="py-3 px-4 font-mono text-muted-foreground text-[11px]">
										<span title={rule.conditionExpression || rule.expectation}>
											{rule.expectation}
										</span>
									</td>

									<!-- Dataset Link -->
									<td class="py-3 px-4">
										{#if rule.datasetId}
											<a
												href={`/datasets/${rule.datasetId}`}
												class="hover:underline text-primary inline-flex items-center gap-1 max-w-[160px] truncate"
												title={rule.datasetName || rule.datasetId}
											>
												<TableProperties class="size-3 text-primary/70 shrink-0" />
												<span class="truncate">{rule.datasetName || "Dataset"}</span>
											</a>
										{:else}
											<span class="text-muted-foreground">—</span>
										{/if}
									</td>

									<!-- Status / Toggle -->
									<td class="py-3 px-4">
										<form action="?/toggleRule" method="POST" use:enhance>
											<input type="hidden" name="id" value={rule.id} />
											<button
												type="submit"
												class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[10px] font-semibold border cursor-pointer transition-all hover:opacity-80"
												class:bg-emerald-500-10={rule.enabled}
												class:text-emerald-600={rule.enabled}
												class:border-emerald-500-20={rule.enabled}
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
												onclick={() => openEdit(rule)}
												title="Edit rule parameters"
												class="size-7 text-muted-foreground hover:text-foreground cursor-pointer rounded-md"
											>
												<Edit class="size-3.5" />
											</Button>
											<Button
												type="button"
												variant="ghost"
												size="icon"
												onclick={() => openDelete(rule)}
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
</div>

<!-- =========================================================================
     Create Quality Rule Modal
     ========================================================================= -->
<Dialog.Root bind:open={isCreateOpen}>
	<Dialog.Content class="w-[95vw] sm:max-w-4xl md:max-w-5xl lg:max-w-6xl xl:max-w-7xl max-h-[92vh] overflow-y-auto rounded-xl p-6 sm:p-8 border-border bg-card shadow-2xl">
		<Dialog.Header class="flex flex-col gap-1">
			<Dialog.Title class="text-lg font-bold tracking-tight flex items-center gap-2">
				<ShieldCheck class="size-5 text-primary" />
				<span>Create Quality Rule</span>
			</Dialog.Title>
			<Dialog.Description class="text-xs text-muted-foreground">
				Define a validation rule to check data correctness and prevent schema violations.
			</Dialog.Description>
		</Dialog.Header>

		<form
			action="?/createRule"
			method="POST"
			use:enhance={() => {
				isSubmitting = true;
				return async ({ update, result }) => {
					isSubmitting = false;
					if (result.type === "success") {
						isCreateOpen = false;
						createCondition = "";
					}
					await update();
				};
			}}
			class="flex flex-col gap-4 pt-3 text-xs"
		>
			<Field.FieldGroup class="flex flex-col gap-4">
				<!-- Target Dataset & Rule Name (2-col) -->
				<div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
					<Field.Field>
						<Field.Label for="create-rule-dataset">Associated Dataset *</Field.Label>
						<select
							id="create-rule-dataset"
							name="datasetId"
							required
							disabled={isSubmitting}
							class="w-full h-9 px-2.5 rounded-md border border-input bg-background text-foreground text-xs cursor-pointer"
						>
							<option value="" disabled selected>Select a dataset...</option>
							{#each availableDatasets as ds}
								<option value={ds.id}>{ds.name} ({ds.datasourceName})</option>
							{/each}
						</select>
					</Field.Field>

					<Field.Field>
						<Field.Label for="create-rule-name">Rule Name *</Field.Label>
						<Input
							id="create-rule-name"
							name="name"
							placeholder="e.g. Customer ID Must Be Non-Null"
							required
							disabled={isSubmitting}
							class="h-9 text-xs"
						/>
					</Field.Field>
				</div>

				<!-- Target Column, Category & Severity (3-col) -->
				<div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
					<Field.Field>
						<Field.Label for="create-rule-target">Target Column *</Field.Label>
						<Input
							id="create-rule-target"
							name="target"
							placeholder="e.g. customer_id or *"
							required
							disabled={isSubmitting}
							class="h-9 text-xs font-mono"
						/>
					</Field.Field>

					<Field.Field>
						<Field.Label for="create-rule-category">Category *</Field.Label>
						<select
							id="create-rule-category"
							name="category"
							required
							disabled={isSubmitting}
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
							disabled={isSubmitting}
							class="w-full h-9 px-2.5 rounded-md border border-input bg-background text-foreground text-xs cursor-pointer"
						>
							{#each severities as sev}
								<option value={sev.value} selected={sev.value === "MEDIUM"}>{sev.label}</option>
							{/each}
						</select>
					</Field.Field>
				</div>

				<!-- Expectation & Description (2-col) -->
				<div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
					<Field.Field>
						<Field.Label for="create-rule-expectation">Expectation *</Field.Label>
						<Input
							id="create-rule-expectation"
							name="expectation"
							placeholder="e.g. is_not_null, unique, range"
							required
							disabled={isSubmitting}
							class="h-9 text-xs font-mono"
						/>
					</Field.Field>

					<Field.Field>
						<Field.Label for="create-rule-desc">Description (Optional)</Field.Label>
						<Input
							id="create-rule-desc"
							name="description"
							placeholder="Brief purpose or rationale for this rule"
							disabled={isSubmitting}
							class="h-9 text-xs"
						/>
					</Field.Field>
				</div>

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
						bind:value={createCondition}
						disabled={isSubmitting}
					/>
				</Field.Field>

				<!-- Enabled Toggle -->
				<div class="flex items-center gap-2 pt-1">
					<input
						id="create-rule-enabled"
						type="checkbox"
						name="enabled"
						value="true"
						checked
						disabled={isSubmitting}
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
					onclick={() => (isCreateOpen = false)}
					disabled={isSubmitting}
					class="h-9 rounded-lg text-xs"
				>
					Cancel
				</Button>
				<Button
					type="submit"
					disabled={isSubmitting}
					class="h-9 rounded-lg font-medium text-xs cursor-pointer gap-1.5"
				>
					{#if isSubmitting}
						<Loader2 class="size-3.5 animate-spin" data-icon="inline-start" />
						<span>Creating...</span>
					{:else}
						<Plus class="size-3.5" data-icon="inline-start" />
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
<Dialog.Root bind:open={isEditOpen}>
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
					isSubmitting = true;
					return async ({ update, result }) => {
						isSubmitting = false;
						if (result.type === "success") {
							isEditOpen = false;
						}
						await update();
					};
				}}
				class="flex flex-col gap-4 pt-3 text-xs"
			>
				<input type="hidden" name="id" value={selectedRule.id} />

				<Field.FieldGroup class="flex flex-col gap-3.5">
					<!-- Rule Name -->
					<Field.Field>
						<Field.Label for="edit-rule-name">Rule Name *</Field.Label>
						<Input
							id="edit-rule-name"
							name="name"
							value={selectedRule.name}
							required
							disabled={isSubmitting}
							class="h-9 text-xs"
						/>
					</Field.Field>

					<!-- Target Column, Category & Severity (3-col) -->
					<div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
						<Field.Field>
							<Field.Label for="edit-rule-target">Target Column *</Field.Label>
							<Input
								id="edit-rule-target"
								name="target"
								value={selectedRule.target}
								required
								disabled={isSubmitting}
								class="h-9 text-xs font-mono"
							/>
						</Field.Field>

						<Field.Field>
							<Field.Label for="edit-rule-category">Category *</Field.Label>
							<select
								id="edit-rule-category"
								name="category"
								disabled={isSubmitting}
								class="w-full h-9 px-2.5 rounded-md border border-input bg-background text-foreground text-xs cursor-pointer"
							>
								{#each categories as cat}
									<option value={cat.value} selected={cat.value === selectedRule.category}>{cat.label}</option>
								{/each}
							</select>
						</Field.Field>

						<Field.Field>
							<Field.Label for="edit-rule-severity">Severity *</Field.Label>
							<select
								id="edit-rule-severity"
								name="severity"
								disabled={isSubmitting}
								class="w-full h-9 px-2.5 rounded-md border border-input bg-background text-foreground text-xs cursor-pointer"
							>
								{#each severities as sev}
									<option value={sev.value} selected={sev.value === selectedRule.severity}>{sev.label}</option>
								{/each}
							</select>
						</Field.Field>
					</div>

					<!-- Expectation & Description (2-col) -->
					<div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
						<Field.Field>
							<Field.Label for="edit-rule-expectation">Expectation *</Field.Label>
							<Input
								id="edit-rule-expectation"
								name="expectation"
								value={selectedRule.expectation}
								required
								disabled={isSubmitting}
								class="h-9 text-xs font-mono"
							/>
						</Field.Field>

						<Field.Field>
							<Field.Label for="edit-rule-desc">Description</Field.Label>
							<Input
								id="edit-rule-desc"
								name="description"
								value={selectedRule.description || ""}
								placeholder="Brief purpose or rationale for this rule"
								disabled={isSubmitting}
								class="h-9 text-xs"
							/>
						</Field.Field>
					</div>

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
							disabled={isSubmitting}
						/>
					</Field.Field>

					<!-- Description -->
					<Field.Field>
						<Field.Label for="edit-rule-desc">Description</Field.Label>
						<Input
							id="edit-rule-desc"
							name="description"
							value={selectedRule.description || ""}
							disabled={isSubmitting}
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
							disabled={isSubmitting}
							class="size-4 rounded border-input text-primary focus:ring-primary cursor-pointer"
						/>
						<label for="edit-rule-enabled" class="text-xs font-medium text-foreground cursor-pointer">
							Rule is active and enabled
						</label>
					</div>
				</Field.FieldGroup>

				<Dialog.Footer class="pt-2 flex items-center justify-end gap-2">
					<Button
						type="button"
						variant="outline"
						onclick={() => (isEditOpen = false)}
						disabled={isSubmitting}
						class="h-9 rounded-lg text-xs"
					>
						Cancel
					</Button>
					<Button
						type="submit"
						disabled={isSubmitting}
						class="h-9 rounded-lg font-medium text-xs cursor-pointer gap-1.5"
					>
						{#if isSubmitting}
							<Loader2 class="size-3.5 animate-spin" data-icon="inline-start" />
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
     Delete Confirmation Modal
     ========================================================================= -->
<Dialog.Root bind:open={isDeleteOpen}>
	{#if selectedRule}
		<Dialog.Content class="sm:max-w-md rounded-xl p-6 border-border bg-card">
			<Dialog.Header class="flex flex-col gap-2">
				<div class="flex items-center gap-3">
					<div class="size-10 rounded-full bg-destructive/10 border border-destructive/20 flex items-center justify-center text-destructive shrink-0">
						<Trash2 class="size-5" />
					</div>
					<div class="flex flex-col gap-0.5">
						<Dialog.Title class="text-base font-bold tracking-tight text-foreground">
							Delete Quality Rule
						</Dialog.Title>
						<Dialog.Description class="text-xs text-muted-foreground">
							Are you sure you want to permanently delete <strong class="text-foreground">'{selectedRule.name}'</strong>?
						</Dialog.Description>
					</div>
				</div>
			</Dialog.Header>

			<div class="p-3 text-xs text-muted-foreground bg-muted/40 border border-border rounded-lg flex flex-col gap-1 my-2">
				<p class="font-medium text-foreground">Irreversible Action</p>
				<p>This rule and all historical evaluation findings linked to it will be permanently removed.</p>
			</div>

			<Dialog.Footer class="pt-2 flex items-center justify-end gap-2">
				<Button
					type="button"
					variant="outline"
					onclick={() => (isDeleteOpen = false)}
					disabled={isSubmitting}
					class="h-9 rounded-lg text-xs"
				>
					Cancel
				</Button>

				<form
					action="?/deleteRule"
					method="POST"
					use:enhance={() => {
						isSubmitting = true;
						return async ({ update, result }) => {
							isSubmitting = false;
							if (result.type === "success") {
								isDeleteOpen = false;
							}
							await update();
						};
					}}
				>
					<input type="hidden" name="id" value={selectedRule.id} />
					<Button
						type="submit"
						disabled={isSubmitting}
						class="h-9 rounded-lg text-xs font-medium bg-destructive hover:bg-destructive/90 text-destructive-foreground cursor-pointer gap-1.5"
					>
						{#if isSubmitting}
							<Loader2 class="size-3.5 animate-spin" data-icon="inline-start" />
							<span>Deleting...</span>
						{:else}
							<Trash2 class="size-3.5" data-icon="inline-start" />
							<span>Delete Rule</span>
						{/if}
					</Button>
				</form>
			</Dialog.Footer>
		</Dialog.Content>
	{/if}
</Dialog.Root>
