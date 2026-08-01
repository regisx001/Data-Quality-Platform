<script lang="ts">
	import { enhance } from "$app/forms";
	import { goto } from "$app/navigation";
	import { Button } from "$lib/components/ui/button/index.js";
	import * as Tabs from "$lib/components/ui/tabs/index.js";
	import ArrowLeft from "@lucide/svelte/icons/arrow-left";
	import Edit from "@lucide/svelte/icons/edit";
	import Trash2 from "@lucide/svelte/icons/trash-2";
	import Play from "@lucide/svelte/icons/play";
	import Settings2 from "@lucide/svelte/icons/settings-2";
	import Activity from "@lucide/svelte/icons/activity";
	import Database from "@lucide/svelte/icons/database";
	import User from "@lucide/svelte/icons/user";
	import Calendar from "@lucide/svelte/icons/calendar";
	import type { PageData, ActionData } from "./$types";
	import type {
		DatasourceStatus,
		ConnectionTestResult,
	} from "$lib/server/api";

	import OverviewTab from "./components/OverviewTab.svelte";
	import ConfigTab from "./components/ConfigTab.svelte";
	import ConnectionTab from "./components/ConnectionTab.svelte";
	import EditDatasourceModal from "./components/EditDatasourceModal.svelte";
	import DeleteDatasourceModal from "./components/DeleteDatasourceModal.svelte";

	let { data, form }: { data: PageData; form: ActionData } = $props();

	let datasource = $derived(form?.datasource || data.datasource);
	let statusInfo = $derived(getStatusBadge(datasource.status));

	// Tab state derived from server load data
	let activeTab = $derived(data.activeTab || "");

	function setTab(tab: string) {
		const url = new URL(window.location.href);
		url.searchParams.set("tab", tab);
		goto(url, {
			replaceState: true,
			keepFocus: true,
			noScroll: true,
		});
	}

	// Dialog states
	let isEditOpen = $state(false);
	let isDeleteOpen = $state(false);

	// Config schema & values
	let configSchema = $derived(data.configSchema);
	let effectiveConfigJson = $derived(form?.configJson ?? data.configJson);
	let hasConfig = $derived(
		!!effectiveConfigJson &&
			effectiveConfigJson !== "{}" &&
			effectiveConfigJson !== "null",
	);

	// Connection test state
	let isTestingConnection = $state(false);
	let connectionTestResult = $state<ConnectionTestResult | null>(null);
	let connectionTestError = $state<string | null>(null);

	function getStatusBadge(status: DatasourceStatus) {
		switch (status) {
			case "ACTIVE":
				return {
					label: "Active",
					classes:
						"bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20",
				};
			case "REGISTERED":
				return {
					label: "Registered",
					classes:
						"bg-sky-500/10 text-sky-600 dark:text-sky-400 border-sky-500/20",
				};
			case "DISABLED":
				return {
					label: "Disabled",
					classes:
						"bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20",
				};
			case "ARCHIVED":
				return {
					label: "Archived",
					classes: "bg-muted text-muted-foreground border-border",
				};
			default:
				return {
					label: status,
					classes: "bg-muted text-muted-foreground border-border",
				};
		}
	}

	function getConfigEntries(): { key: string; value: string }[] {
		if (!effectiveConfigJson) return [];
		try {
			const obj = JSON.parse(effectiveConfigJson);
			const entries: { key: string; value: string }[] = [];
			for (const [k, v] of Object.entries(obj)) {
				if (
					k.toLowerCase().includes("password") ||
					k.toLowerCase().includes("secret")
				) {
					entries.push({ key: k, value: "••••••••" });
				} else if (v !== null && v !== undefined) {
					entries.push({ key: k, value: String(v) });
				}
			}
			return entries;
		} catch {
			return [];
		}
	}

	function enhanceTestConnection() {
		isTestingConnection = true;
		connectionTestResult = null;
		connectionTestError = null;
		return async ({ result }: { result: any }) => {
			isTestingConnection = false;
			if (result.type === "success") {
				const data = result.data as any;
				if (data?.connectionTest) {
					connectionTestResult = data.connectionTest;
				}
			} else if (result.type === "failure") {
				const data = result.data as any;
				connectionTestError = data?.error || "Connection test failed";
			} else {
				connectionTestError = "Unexpected response from server";
			}
		};
	}
</script>

<svelte:head>
	<title>{datasource.name} | Datasource Details</title>
	<meta
		name="description"
		content={`View and manage datasource settings for ${datasource.name}`}
	/>
</svelte:head>

<div class="p-6 sm:p-8 w-full space-y-6">
	<!-- Navigation & Actions Bar -->
	<div
		class="flex flex-col gap-4 border-b border-border pb-5"
	>
		<div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
			<div class="flex items-center gap-3">
				<a
					href="/datasources"
					class="p-2 rounded-lg border border-border hover:bg-accent transition-colors text-muted-foreground hover:text-foreground shrink-0"
					title="Back to Datasources"
				>
					<ArrowLeft class="size-4" />
				</a>
				<div class="space-y-1">
					<div class="flex items-center gap-2.5 flex-wrap">
						<h1 class="text-2xl font-bold tracking-tight">
							{datasource.name}
						</h1>
						<span
							class={`px-2.5 py-0.5 rounded-full text-xs font-medium border ${statusInfo.classes}`}
						>
							{statusInfo.label}
						</span>
					</div>
					{#if datasource.description}
						<p class="text-xs text-muted-foreground line-clamp-2">
							{datasource.description}
						</p>
					{/if}
				</div>
			</div>

			<div class="flex items-center gap-2 shrink-0">
				<Button
					variant="outline"
					onclick={() => (isEditOpen = true)}
					class="h-9 px-3 rounded-lg text-xs font-medium cursor-pointer"
				>
					<Edit class="size-3.5 me-1.5" />
					<span>Edit Parameters</span>
				</Button>

				<Button
					variant="destructive"
					onclick={() => (isDeleteOpen = true)}
					class="h-9 px-3 rounded-lg text-xs font-medium cursor-pointer"
				>
					<Trash2 class="size-3.5 me-1.5" />
					<span>Delete</span>
				</Button>
			</div>
		</div>

		<!-- Datasource Key Specifications Metadata Bar -->
		<div class="flex items-center gap-4 text-xs text-muted-foreground flex-wrap pt-2 border-t border-border/40">
			<div class="flex items-center gap-1.5">
				<Database class="size-3.5 text-foreground/70" />
				<span>Engine:</span>
				<span class="font-mono font-semibold text-foreground px-2 py-0.5 rounded bg-muted border border-border text-[11px]">
					{datasource.type}
				</span>
			</div>

			<span class="text-border/60">•</span>

			<div class="flex items-center gap-1.5">
				<User class="size-3.5 text-foreground/70" />
				<span>Owner:</span>
				<span class="font-medium text-foreground">@{datasource.owner}</span>
			</div>

			{#if datasource.registrationDate}
				<span class="text-border/60">•</span>
				<div class="flex items-center gap-1.5">
					<Calendar class="size-3.5 text-foreground/70" />
					<span>Registered:</span>
					<span class="font-mono text-foreground">
						{new Date(datasource.registrationDate).toLocaleDateString(undefined, {
							year: "numeric",
							month: "short",
							day: "numeric"
						})}
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
		<div
			class="p-3.5 text-xs text-destructive bg-destructive/10 border border-destructive/20 rounded-xl"
		>
			{form.error}
		</div>
	{/if}

	<!-- Tabs: Overview | Config | Connection -->
	<Tabs.Root value={activeTab} onValueChange={setTab}>
		<Tabs.List>
			<Tabs.Trigger value="overview" class="text-xs gap-1.5">
				<Play class="size-3.5" />
				Overview
			</Tabs.Trigger>
			<Tabs.Trigger value="config" class="text-xs gap-1.5">
				<Settings2 class="size-3.5" />
				Configuration
			</Tabs.Trigger>
			<Tabs.Trigger value="connection" class="text-xs gap-1.5">
				<Activity class="size-3.5" />
				Connection
			</Tabs.Trigger>
		</Tabs.List>

		<!-- TAB: OVERVIEW -->
		<Tabs.Content value="overview" class="space-y-6 mt-4">
			<OverviewTab {datasource} {statusInfo} />
		</Tabs.Content>

		<!-- TAB: CONFIGURATION -->
		<Tabs.Content value="config" class="space-y-6 mt-4">
			<ConfigTab
				{datasource}
				{configSchema}
				{effectiveConfigJson}
				{hasConfig}
				{isTestingConnection}
				{connectionTestResult}
				{connectionTestError}
			/>
		</Tabs.Content>

		<!-- TAB: CONNECTION -->
		<Tabs.Content value="connection" class="space-y-6 mt-4">
			<ConnectionTab
				{datasource}
				{statusInfo}
				{hasConfig}
				{isTestingConnection}
				{connectionTestResult}
				{connectionTestError}
				{getConfigEntries}
			/>
		</Tabs.Content>
	</Tabs.Root>
</div>

<!-- Hidden form used by both Overview and Config "Test Connection" buttons -->
<form
	id="test-connection-form"
	action="?/testConnection"
	method="POST"
	use:enhance={enhanceTestConnection}
	class="hidden"
	aria-hidden="true"
></form>

<!-- Edit Datasource Modal -->
<EditDatasourceModal bind:open={isEditOpen} {datasource} />

<!-- Delete Datasource Modal -->
<DeleteDatasourceModal bind:open={isDeleteOpen} {datasource} />
