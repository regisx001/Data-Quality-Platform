<script lang="ts">
	import { enhance } from "$app/forms";
	import { Button } from "$lib/components/ui/button/index.js";
	import * as Card from "$lib/components/ui/card/index.js";
	import * as Field from "$lib/components/ui/field/index.js";
	import { Input } from "$lib/components/ui/input/index.js";
	import * as Dialog from "$lib/components/ui/dialog/index.js";
	import * as Tabs from "$lib/components/ui/tabs/index.js";
	import ArrowLeft from "@lucide/svelte/icons/arrow-left";
	import Edit from "@lucide/svelte/icons/edit";
	import Trash2 from "@lucide/svelte/icons/trash-2";
	import Play from "@lucide/svelte/icons/play";
	import Pause from "@lucide/svelte/icons/pause";
	import Archive from "@lucide/svelte/icons/archive";
	import Settings2 from "@lucide/svelte/icons/settings-2";
	import FileJson from "@lucide/svelte/icons/file-json";
	import Loader2 from "@lucide/svelte/icons/loader-2";
	import CheckCircle2 from "@lucide/svelte/icons/check-circle-2";
	import Eye from "@lucide/svelte/icons/eye";
	import EyeOff from "@lucide/svelte/icons/eye-off";
	import type { PageData, ActionData } from "./$types";
	import type { DatasourceStatus, ConfigField } from "$lib/server/api";

	let { data, form }: { data: PageData; form: ActionData } = $props();

	let datasource = $derived(form?.datasource || data.datasource);
	let statusInfo = $derived(getStatusBadge(datasource.status));

	// Tab state
	let activeTab = $state("overview");

	// Dialog States
	let isEditOpen = $state(false);
	let isDeleteOpen = $state(false);
	let isSubmitting = $state(false);

	// Config schema from backend
	let configSchema = $derived(data.configSchema);
	let configSaved = $state(false);

	// Whether a configuration already exists in the DB
	let hasConfig = $derived(
		!!data.configJson &&
			data.configJson !== "{}" &&
			data.configJson !== "null",
	);

	// Edit mode: fields are disabled until user clicks "Enable Editing"
	let isEditingConfig = $state(!hasConfig);

	// Dynamic config form values — keyed by field name
	let configValues: Record<string, string> = $state({});
	let showPassword: Record<string, boolean> = $state({});

	// Initialize config values from existing configJson or schema defaults
	function initConfigValues() {
		const existing: Record<string, any> = {};
		if (data.configJson) {
			try {
				Object.assign(existing, JSON.parse(data.configJson));
			} catch {
				/* ignore */
			}
		}
		if (configSchema) {
			const values: Record<string, string> = {};
			for (const field of configSchema.fields) {
				const val = existing[field.name];
				if (val !== undefined && val !== null) {
					values[field.name] = String(val);
				} else if (field.defaultValue !== null) {
					values[field.name] = field.defaultValue;
				} else {
					values[field.name] = "";
				}
			}
			configValues = values;
		} else {
			// Fallback: raw JSON editor
			configValues = { _raw: data.configJson ?? "{}" };
		}
	}
	$effect(() => {
		configSchema;
		initConfigValues();
	});

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

	function buildConfigJson(): string {
		const obj: Record<string, any> = {};
		if (configSchema) {
			for (const field of configSchema.fields) {
				const raw = configValues[field.name] ?? "";
				if (field.type === "number") {
					obj[field.name] = raw === "" ? null : Number(raw);
				} else if (field.type === "boolean") {
					obj[field.name] = raw === "true";
				} else {
					obj[field.name] = raw;
				}
			}
		}
		return JSON.stringify(obj, null, 2);
	}

	function handleConfigSubmit() {
		configSaved = false;
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
		class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-5"
	>
		<div class="space-y-1">
			<div class="flex items-center gap-3">
				<a
					href="/datasources"
					class="p-2 rounded-lg border border-border hover:bg-accent transition-colors text-muted-foreground hover:text-foreground"
					title="Back to Datasources"
				>
					<ArrowLeft class="size-4" />
				</a>
				<div>
					<div class="flex items-center gap-2.5">
						<h1 class="text-2xl font-bold tracking-tight">
							{datasource.name}
						</h1>
						<span
							class={`px-2.5 py-0.5 rounded-full text-xs font-medium border ${statusInfo.classes}`}
						>
							{statusInfo.label}
						</span>
					</div>
				</div>
			</div>
		</div>

		<div class="flex items-center gap-2">
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

	<!-- Tabs: Overview | Config -->
	<Tabs.Root bind:value={activeTab}>
		<Tabs.List>
			<Tabs.Trigger value="overview" class="text-xs gap-1.5">
				<Play class="size-3.5" />
				Overview
			</Tabs.Trigger>
			<Tabs.Trigger value="config" class="text-xs gap-1.5">
				<Settings2 class="size-3.5" />
				Configuration
			</Tabs.Trigger>
		</Tabs.List>

		<!-- ════════════════════════════════════════════════════════════════
		         TAB: OVERVIEW
		         ════════════════════════════════════════════════════════════════ -->
		<Tabs.Content value="overview" class="space-y-6 mt-4">
			<!-- Overview Grid Section -->
			<div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
				<!-- Specification Details Card -->
				<Card.Root
					class="lg:col-span-2 rounded-xl border-border bg-card shadow-xs"
				>
					<Card.Header class="pb-3 border-b border-border">
						<Card.Title class="text-base font-bold tracking-tight"
							>Datasource Specifications</Card.Title
						>
					</Card.Header>
					<Card.Content class="p-5 space-y-4">
						<div
							class="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs"
						>
							<div
								class="p-3 rounded-lg bg-muted/30 border border-border/50 space-y-1"
							>
								<span class="text-muted-foreground"
									>Engine Type</span
								>
								<p class="font-mono text-sm font-semibold">
									{datasource.type}
								</p>
							</div>

							<div
								class="p-3 rounded-lg bg-muted/30 border border-border/50 space-y-1"
							>
								<span class="text-muted-foreground">Owner</span>
								<p class="font-medium text-sm">
									@{datasource.owner}
								</p>
							</div>

							<div
								class="p-3 rounded-lg bg-muted/30 border border-border/50 space-y-1"
							>
								<span class="text-muted-foreground">Status</span
								>
								<p class="font-medium text-sm">
									{datasource.status}
								</p>
							</div>

							<div
								class="p-3 rounded-lg bg-muted/30 border border-border/50 space-y-1"
							>
								<span class="text-muted-foreground"
									>Registration Timestamp</span
								>
								<p class="font-mono text-xs">
									{datasource.registrationDate
										? new Date(
												datasource.registrationDate,
											).toLocaleString()
										: "N/A"}
								</p>
							</div>
						</div>

						<div
							class="p-3.5 rounded-lg bg-muted/30 border border-border/50 space-y-1 text-xs"
						>
							<span class="text-muted-foreground"
								>Description</span
							>
							<p class="text-foreground">
								{datasource.description ||
									"No description provided."}
							</p>
						</div>
					</Card.Content>
				</Card.Root>

				<!-- Status Transition Controls Card -->
				<Card.Root class="rounded-xl border-border bg-card shadow-xs">
					<Card.Header class="pb-3 border-b border-border">
						<Card.Title class="text-base font-bold tracking-tight"
							>Status Controls</Card.Title
						>
					</Card.Header>
					<Card.Content class="p-5 space-y-3">
						<p class="text-xs text-muted-foreground">
							Trigger status transition actions for this
							datasource entity.
						</p>

						<div class="space-y-2 pt-1">
							<form
								action="?/changeStatus"
								method="POST"
								use:enhance
							>
								<input
									type="hidden"
									name="statusAction"
									value="activate"
								/>
								<Button
									type="submit"
									variant="outline"
									disabled={datasource.status === "ACTIVE"}
									class="w-full h-9 rounded-lg text-xs font-medium border-emerald-500/30 text-emerald-600 dark:text-emerald-400 hover:bg-emerald-500/10 cursor-pointer justify-start"
								>
									<Play class="size-3.5 me-2" />
									<span>Activate Datasource</span>
								</Button>
							</form>

							<form
								action="?/changeStatus"
								method="POST"
								use:enhance
							>
								<input
									type="hidden"
									name="statusAction"
									value="disable"
								/>
								<Button
									type="submit"
									variant="outline"
									disabled={datasource.status === "DISABLED"}
									class="w-full h-9 rounded-lg text-xs font-medium border-amber-500/30 text-amber-600 dark:text-amber-400 hover:bg-amber-500/10 cursor-pointer justify-start"
								>
									<Pause class="size-3.5 me-2" />
									<span>Disable Datasource</span>
								</Button>
							</form>

							<form
								action="?/changeStatus"
								method="POST"
								use:enhance
							>
								<input
									type="hidden"
									name="statusAction"
									value="archive"
								/>
								<Button
									type="submit"
									variant="outline"
									disabled={datasource.status === "ARCHIVED"}
									class="w-full h-9 rounded-lg text-xs font-medium border-border text-muted-foreground hover:bg-accent cursor-pointer justify-start"
								>
									<Archive class="size-3.5 me-2" />
									<span>Archive Datasource</span>
								</Button>
							</form>
						</div>
					</Card.Content>
				</Card.Root>
			</div>

			<!-- Associated Datasets Section -->
			<Card.Root
				class="rounded-xl border-border bg-card overflow-hidden shadow-xs w-full"
			>
				<Card.Header class="pb-3 border-b border-border">
					<Card.Title class="text-base font-bold tracking-tight"
						>Associated Datasets</Card.Title
					>
				</Card.Header>
				<Card.Content class="p-0">
					{#if !datasource.datasets || datasource.datasets.length === 0}
						<div
							class="p-8 text-center text-xs text-muted-foreground space-y-1"
						>
							<p class="font-medium">
								No datasets currently bound to this datasource.
							</p>
							<p>
								Datasets created via API will automatically
								populate here.
							</p>
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
										<th class="py-3 px-5 font-medium"
											>Dataset Name</th
										>
										<th class="py-3 px-5 font-medium"
											>Description</th
										>
										<th class="py-3 px-5 font-medium"
											>Row Count</th
										>
										<th class="py-3 px-5 font-medium"
											>Dataset ID</th
										>
									</tr>
								</thead>
								<tbody class="divide-y divide-border/60">
									{#each datasource.datasets as dataset}
										<tr
											class="hover:bg-accent/30 transition-colors"
										>
											<td
												class="py-3.5 px-5 font-medium text-foreground"
												>{dataset.name}</td
											>
											<td
												class="py-3.5 px-5 text-muted-foreground"
												>{dataset.description ||
													"—"}</td
											>
											<td class="py-3.5 px-5 font-mono"
												>{dataset.rowCount
													? dataset.rowCount.toLocaleString()
													: "Read-only"}</td
											>
											<td
												class="py-3.5 px-5 font-mono text-muted-foreground"
												>{dataset.id}</td
											>
										</tr>
									{/each}
								</tbody>
							</table>
						</div>
					{/if}
				</Card.Content>
			</Card.Root>
		</Tabs.Content>

		<!-- ════════════════════════════════════════════════════════════════
		         TAB: CONFIGURATION
		         ════════════════════════════════════════════════════════════════ -->
		<Tabs.Content value="config" class="space-y-6 mt-4">
			{#if configSchema}
				<Card.Root class="rounded-xl border-border bg-card shadow-xs">
					<Card.Header class="pb-3 border-b border-border">
						<div class="flex items-center justify-between">
							<div>
								<Card.Title
									class="text-base font-bold tracking-tight flex items-center gap-2"
								>
									<Settings2 class="size-4" />
									{configSchema.label} Configuration
								</Card.Title>
								<Card.Description
									class="text-xs text-muted-foreground mt-1"
								>
									{configSchema.description}
								</Card.Description>
							</div>
							{#if hasConfig}
								<Button
									variant={isEditingConfig
										? "outline"
										: "default"}
									onclick={() => {
										isEditingConfig = !isEditingConfig;
										if (!isEditingConfig) {
											// Reset to saved values on cancel
											initConfigValues();
										}
									}}
									class="h-8 px-3 rounded-lg text-xs font-medium cursor-pointer shrink-0"
								>
									{#if isEditingConfig}
										Cancel
									{:else}
										<Edit class="size-3.5 me-1" />
										Enable Editing
									{/if}
								</Button>
							{/if}
						</div>
					</Card.Header>
					<Card.Content class="p-5">
						<form
							action="?/saveConfig"
							method="POST"
							use:enhance={() => {
								configSaved = false;
								return async ({ update, result }) => {
									if (result.type === "success") {
										configSaved = true;
										isEditingConfig = false;
										setTimeout(
											() => (configSaved = false),
											3000,
										);
									}
									await update();
								};
							}}
						>
							<!-- Hidden input to submit the built JSON -->
							<input
								type="hidden"
								name="configJson"
								value={buildConfigJson()}
							/>

							<div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
								{#each configSchema.fields as field (field.name)}
									<div class="space-y-1.5">
										<label
											for="cfg-{field.name}"
											class="text-xs font-medium flex items-center gap-1"
										>
											{field.label}
											{#if field.required}
												<span class="text-destructive"
													>*</span
												>
											{/if}
										</label>

										{#if field.type === "boolean"}
											<div
												class="flex items-center gap-3"
											>
												<button
													type="button"
													id="cfg-{field.name}"
													role="switch"
													disabled={!isEditingConfig}
													aria-checked={configValues[
														field.name
													] === "true"}
													aria-label="Toggle {field.label}"
													onclick={() => {
														configValues[
															field.name
														] =
															configValues[
																field.name
															] === "true"
																? "false"
																: "true";
													}}
													class="relative inline-flex h-5 w-9 shrink-0 items-center rounded-full border-2 border-transparent transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background {!isEditingConfig
														? 'opacity-60 cursor-not-allowed'
														: 'cursor-pointer'} {configValues[
														field.name
													] === 'true'
														? 'bg-primary'
														: 'bg-input'}"
												>
													<span
														class="pointer-events-none block h-4 w-4 rounded-full bg-background shadow-lg ring-0 transition-transform {configValues[
															field.name
														] === 'true'
															? 'translate-x-4'
															: 'translate-x-0'}"
													></span>
												</button>
												<span
													class="text-xs text-muted-foreground"
												>
													{configValues[
														field.name
													] === "true"
														? "Enabled"
														: "Disabled"}
												</span>
											</div>
										{:else if field.options && field.options.length > 0}
											<select
												id="cfg-{field.name}"
												name="cfg-{field.name}"
												value={configValues[field.name]}
												disabled={!isEditingConfig}
												onchange={(e) => {
													configValues[field.name] =
														e.currentTarget.value;
												}}
												class="w-full h-9 px-3 rounded-lg border border-border bg-background text-foreground text-xs focus:ring-primary focus:border-primary {!isEditingConfig
													? 'opacity-60 cursor-not-allowed'
													: 'cursor-pointer'}"
											>
												{#each field.options as opt}
													<option value={opt}
														>{opt}</option
													>
												{/each}
											</select>
										{:else if field.type === "password"}
											<div class="relative">
												<input
													id="cfg-{field.name}"
													name="cfg-{field.name}"
													type={showPassword[
														field.name
													]
														? "text"
														: "password"}
													value={configValues[
														field.name
													]}
													disabled={!isEditingConfig}
													oninput={(e) => {
														configValues[
															field.name
														] =
															e.currentTarget.value;
													}}
													placeholder={field.defaultValue ??
														""}
													class="w-full h-9 px-3 pe-8 rounded-lg border border-border bg-background text-foreground text-xs focus:ring-primary focus:border-primary {!isEditingConfig
														? 'opacity-60 cursor-not-allowed'
														: ''}"
												/>
												<button
													type="button"
													onclick={() => {
														showPassword[
															field.name
														] =
															!showPassword[
																field.name
															];
													}}
													class="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground cursor-pointer"
													tabindex="-1"
												>
													{#if showPassword[field.name]}
														<EyeOff
															class="size-3.5"
														/>
													{:else}
														<Eye class="size-3.5" />
													{/if}
												</button>
											</div>
										{:else if field.type === "number"}
											<input
												id="cfg-{field.name}"
												name="cfg-{field.name}"
												type="number"
												value={configValues[field.name]}
												disabled={!isEditingConfig}
												oninput={(e) => {
													configValues[field.name] =
														e.currentTarget.value;
												}}
												placeholder={field.defaultValue ??
													""}
												min={field.min ?? undefined}
												max={field.max ?? undefined}
												class="w-full h-9 px-3 rounded-lg border border-border bg-background text-foreground text-xs focus:ring-primary focus:border-primary {!isEditingConfig
													? 'opacity-60 cursor-not-allowed'
													: ''}"
											/>
										{:else}
											<input
												id="cfg-{field.name}"
												name="cfg-{field.name}"
												type="text"
												value={configValues[field.name]}
												disabled={!isEditingConfig}
												oninput={(e) => {
													configValues[field.name] =
														e.currentTarget.value;
												}}
												placeholder={field.defaultValue ??
													""}
												class="w-full h-9 px-3 rounded-lg border border-border bg-background text-foreground text-xs focus:ring-primary focus:border-primary {!isEditingConfig
													? 'opacity-60 cursor-not-allowed'
													: ''}"
											/>
										{/if}

										<p
											class="text-[11px] text-muted-foreground"
										>
											{field.description}
										</p>
									</div>
								{/each}
							</div>

							<div
								class="flex items-center justify-between pt-4 border-t border-border mt-6"
							>
								<div>
									{#if configSaved}
										<span
											class="inline-flex items-center gap-1.5 text-xs text-emerald-600 dark:text-emerald-400"
										>
											<CheckCircle2 class="size-3.5" />
											Configuration saved
										</span>
									{/if}
									{#if hasConfig && !isEditingConfig}
										<span
											class="inline-flex items-center gap-1.5 text-xs text-muted-foreground"
										>
											<EyeOff class="size-3.5" />
											Fields are read-only. Click "Enable Editing"
											to modify.
										</span>
									{/if}
								</div>
								{#if isEditingConfig}
									<Button
										type="submit"
										class="h-9 px-4 rounded-lg text-xs font-medium cursor-pointer"
									>
										<FileJson class="size-3.5 me-1.5" />
										Save Configuration
									</Button>
								{/if}
							</div>
						</form>
					</Card.Content>
				</Card.Root>
			{:else}
				<!-- Fallback: raw JSON editor when no schema is available -->
				<Card.Root class="rounded-xl border-border bg-card shadow-xs">
					<Card.Header class="pb-3 border-b border-border">
						<Card.Title
							class="text-base font-bold tracking-tight flex items-center gap-2"
						>
							<FileJson class="size-4" />
							Connector Configuration
						</Card.Title>
						<Card.Description
							class="text-xs text-muted-foreground mt-1"
						>
							No schema defined for "{datasource.type}". Edit the
							raw JSON below.
						</Card.Description>
					</Card.Header>
					<Card.Content class="p-5 space-y-4">
						<form
							action="?/saveConfig"
							method="POST"
							use:enhance={() => {
								configSaved = false;
								return async ({ update, result }) => {
									if (result.type === "success") {
										configSaved = true;
										setTimeout(
											() => (configSaved = false),
											3000,
										);
									}
									await update();
								};
							}}
						>
							<Field.Field>
								<Field.Label
									for="config-json"
									class="text-xs font-medium"
								>
									Configuration JSON
								</Field.Label>
								<textarea
									id="config-json"
									name="configJson"
									bind:value={configValues._raw}
									class="min-h-70 w-full font-mono text-xs leading-relaxed rounded-lg mt-1.5 px-3 py-2 border border-border bg-background text-foreground focus:ring-primary focus:border-primary"
									spellcheck="false"
								></textarea>
							</Field.Field>

							<div
								class="flex items-center justify-between pt-4 border-t border-border mt-4"
							>
								<div>
									{#if configSaved}
										<span
											class="inline-flex items-center gap-1.5 text-xs text-emerald-600 dark:text-emerald-400"
										>
											<CheckCircle2 class="size-3.5" />
											Configuration saved
										</span>
									{/if}
								</div>
								<Button
									type="submit"
									class="h-9 px-4 rounded-lg text-xs font-medium cursor-pointer"
								>
									<FileJson class="size-3.5 me-1.5" />
									Save Configuration
								</Button>
							</div>
						</form>
					</Card.Content>
				</Card.Root>
			{/if}
		</Tabs.Content>
	</Tabs.Root>
</div>

<!-- Edit Datasource Modal -->
<Dialog.Root bind:open={isEditOpen}>
	<Dialog.Content class="sm:max-w-lg rounded-xl p-6 border-border bg-card">
		<Dialog.Header class="space-y-1">
			<Dialog.Title class="text-lg font-bold tracking-tight"
				>Edit Datasource</Dialog.Title
			>
			<Dialog.Description class="text-xs text-muted-foreground">
				Update settings for '{datasource.name}'.
			</Dialog.Description>
		</Dialog.Header>

		<form
			action="?/updateDatasource"
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
			class="space-y-4 pt-3 text-xs"
		>
			<Field.Field>
				<Field.Label for="edit-name">Datasource Name</Field.Label>
				<Input
					id="edit-name"
					name="name"
					type="text"
					value={datasource.name}
					required
					disabled={isSubmitting}
					class="h-9 rounded-lg"
				/>
			</Field.Field>

			<Field.Field>
				<Field.Label for="edit-type">Engine Type</Field.Label>
				<Input
					id="edit-type"
					name="type"
					type="text"
					value={datasource.type}
					required
					disabled={isSubmitting}
					class="h-9 rounded-lg"
				/>
			</Field.Field>

			<Field.Field>
				<Field.Label for="edit-status">Status</Field.Label>
				<select
					id="edit-status"
					name="status"
					value={datasource.status}
					disabled={isSubmitting}
					class="w-full h-9 px-3 rounded-lg border border-border bg-background text-foreground text-xs focus:ring-primary focus:border-primary cursor-pointer"
				>
					<option value="REGISTERED">REGISTERED</option>
					<option value="ACTIVE">ACTIVE</option>
					<option value="DISABLED">DISABLED</option>
					<option value="ARCHIVED">ARCHIVED</option>
				</select>
			</Field.Field>

			<Field.Field>
				<Field.Label for="edit-description">Description</Field.Label>
				<Input
					id="edit-description"
					name="description"
					type="text"
					value={datasource.description || ""}
					disabled={isSubmitting}
					class="h-9 rounded-lg"
				/>
			</Field.Field>

			<Dialog.Footer class="pt-3 flex items-center justify-end gap-2">
				<Button
					type="button"
					variant="outline"
					onclick={() => (isEditOpen = false)}
					disabled={isSubmitting}
					class="h-9 rounded-lg cursor-pointer"
				>
					Cancel
				</Button>
				<Button
					type="submit"
					disabled={isSubmitting}
					class="h-9 rounded-lg font-medium cursor-pointer"
				>
					{#if isSubmitting}
						<Loader2 class="size-3.5 me-1.5 animate-spin" />
						<span>Saving...</span>
					{:else}
						<span>Save Changes</span>
					{/if}
				</Button>
			</Dialog.Footer>
		</form>
	</Dialog.Content>
</Dialog.Root>

<!-- Delete Confirmation Dialog -->
<Dialog.Root bind:open={isDeleteOpen}>
	<Dialog.Content class="sm:max-w-md rounded-xl p-6 border-border bg-card">
		<Dialog.Header class="space-y-1">
			<Dialog.Title class="text-lg font-bold text-destructive"
				>Delete Datasource</Dialog.Title
			>
			<Dialog.Description class="text-xs text-muted-foreground">
				Are you sure you want to permanently delete <strong
					class="text-foreground">{datasource.name}</strong
				>? This action cannot be undone.
			</Dialog.Description>
		</Dialog.Header>

		<form
			action="?/deleteDatasource"
			method="POST"
			use:enhance={() => {
				isSubmitting = true;
				return async ({ update }) => {
					isSubmitting = false;
					await update();
				};
			}}
			class="pt-3"
		>
			<Dialog.Footer class="flex items-center justify-end gap-2">
				<Button
					type="button"
					variant="outline"
					onclick={() => (isDeleteOpen = false)}
					disabled={isSubmitting}
					class="h-9 rounded-lg cursor-pointer text-xs"
				>
					Cancel
				</Button>
				<Button
					type="submit"
					variant="destructive"
					disabled={isSubmitting}
					class="h-9 rounded-lg font-medium cursor-pointer text-xs"
				>
					{#if isSubmitting}
						<Loader2 class="size-3.5 me-1.5 animate-spin" />
						<span>Deleting...</span>
					{:else}
						<Trash2 class="size-3.5 me-1.5" />
						<span>Confirm Delete</span>
					{/if}
				</Button>
			</Dialog.Footer>
		</form>
	</Dialog.Content>
</Dialog.Root>
