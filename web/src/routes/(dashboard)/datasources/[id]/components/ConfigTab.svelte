<script lang="ts">
	import { enhance } from "$app/forms";
	import { Button } from "$lib/components/ui/button/index.js";
	import * as Card from "$lib/components/ui/card/index.js";
	import * as Field from "$lib/components/ui/field/index.js";
	import Edit from "@lucide/svelte/icons/edit";
	import Settings2 from "@lucide/svelte/icons/settings-2";
	import FileJson from "@lucide/svelte/icons/file-json";
	import Loader2 from "@lucide/svelte/icons/loader-2";
	import CheckCircle2 from "@lucide/svelte/icons/check-circle-2";
	import XCircle from "@lucide/svelte/icons/x-circle";
	import Cable from "@lucide/svelte/icons/cable";
	import Eye from "@lucide/svelte/icons/eye";
	import EyeOff from "@lucide/svelte/icons/eye-off";
	import type { Datasource, ConnectorConfigSchema, ConnectionTestResult } from "$lib/server/api";

	let {
		datasource,
		configSchema,
		effectiveConfigJson,
		hasConfig,
		isTestingConnection,
		connectionTestResult,
		connectionTestError,
	}: {
		datasource: Datasource;
		configSchema: ConnectorConfigSchema | null;
		effectiveConfigJson: string | null;
		hasConfig: boolean;
		isTestingConnection: boolean;
		connectionTestResult: ConnectionTestResult | null;
		connectionTestError: string | null;
	} = $props();

	let configSaved = $state(false);
	let isEditingConfig = $state(false);
	let configValues: Record<string, string> = $state({});
	let showPassword: Record<string, boolean> = $state({});

	$effect(() => {
		if (!hasConfig) {
			isEditingConfig = true;
		}
	});

	function initConfigValues() {
		const existing: Record<string, any> = {};
		if (effectiveConfigJson) {
			try {
				Object.assign(existing, JSON.parse(effectiveConfigJson));
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
			configValues = { _raw: effectiveConfigJson ?? "{}" };
		}
	}

	$effect(() => {
		configSchema;
		effectiveConfigJson;
		initConfigValues();
	});

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
</script>

<div class="space-y-6">
	{#if configSchema}
		<Card.Root class="rounded-xl border-border bg-card shadow-xs">
			<Card.Header class="pb-3 border-b border-border">
				<div class="flex items-center justify-between">
					<div>
						<Card.Title class="text-base font-bold tracking-tight flex items-center gap-2">
							<Settings2 class="size-4" />
							{configSchema.label} Configuration
						</Card.Title>
						<Card.Description class="text-xs text-muted-foreground mt-1">
							{configSchema.description}
						</Card.Description>
					</div>
					{#if hasConfig}
						<Button
							variant={isEditingConfig ? "outline" : "default"}
							onclick={() => {
								isEditingConfig = !isEditingConfig;
								if (!isEditingConfig) {
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
								setTimeout(() => (configSaved = false), 3000);
							}
							await update();
						};
					}}
				>
					<input type="hidden" name="configJson" value={buildConfigJson()} />

					<div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
						{#each configSchema.fields as field (field.name)}
							<div class="space-y-1.5">
								<label for="cfg-{field.name}" class="text-xs font-medium flex items-center gap-1">
									{field.label}
									{#if field.required}
										<span class="text-destructive">*</span>
									{/if}
								</label>

								{#if field.type === "boolean"}
									<div class="flex items-center gap-3">
										<button
											type="button"
											id="cfg-{field.name}"
											role="switch"
											disabled={!isEditingConfig}
											aria-checked={configValues[field.name] === "true"}
											aria-label="Toggle {field.label}"
											onclick={() => {
												configValues[field.name] =
													configValues[field.name] === "true" ? "false" : "true";
											}}
											class="relative inline-flex h-5 w-9 shrink-0 items-center rounded-full border-2 border-transparent transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background {!isEditingConfig
												? 'opacity-60 cursor-not-allowed'
												: 'cursor-pointer'} {configValues[field.name] === 'true'
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
										<span class="text-xs text-muted-foreground">
											{configValues[field.name] === "true" ? "Enabled" : "Disabled"}
										</span>
									</div>
								{:else if field.options && field.options.length > 0}
									<select
										id="cfg-{field.name}"
										name="cfg-{field.name}"
										bind:value={configValues[field.name]}
										disabled={!isEditingConfig}
										class="w-full h-9 px-3 rounded-lg border border-border bg-background text-foreground text-xs focus:ring-primary focus:border-primary {!isEditingConfig
											? 'opacity-60 cursor-not-allowed'
											: 'cursor-pointer'}"
									>
										{#each field.options as opt}
											<option value={opt}>{opt}</option>
										{/each}
									</select>
								{:else if field.type === "password"}
									<div class="relative">
										<input
											id="cfg-{field.name}"
											name="cfg-{field.name}"
											type={showPassword[field.name] ? "text" : "password"}
											bind:value={configValues[field.name]}
											disabled={!isEditingConfig}
											placeholder={field.defaultValue ?? ""}
											class="w-full h-9 px-3 pe-8 rounded-lg border border-border bg-background text-foreground text-xs focus:ring-primary focus:border-primary {!isEditingConfig
												? 'opacity-60 cursor-not-allowed'
												: ''}"
										/>
										<button
											type="button"
											onclick={() => {
												showPassword[field.name] = !showPassword[field.name];
											}}
											class="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground cursor-pointer"
											tabindex="-1"
										>
											{#if showPassword[field.name]}
												<EyeOff class="size-3.5" />
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
										bind:value={configValues[field.name]}
										disabled={!isEditingConfig}
										placeholder={field.defaultValue ?? ""}
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
										bind:value={configValues[field.name]}
										disabled={!isEditingConfig}
										placeholder={field.defaultValue ?? ""}
										class="w-full h-9 px-3 rounded-lg border border-border bg-background text-foreground text-xs focus:ring-primary focus:border-primary {!isEditingConfig
											? 'opacity-60 cursor-not-allowed'
											: ''}"
									/>
								{/if}

								<p class="text-[11px] text-muted-foreground">
									{field.description}
								</p>
							</div>
						{/each}
					</div>

					<div class="flex items-center justify-between pt-4 border-t border-border mt-6">
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
								<span class="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
									<EyeOff class="size-3.5" />
									Fields are read-only. Click "Enable Editing" to modify.
								</span>
							{/if}
						</div>
						<div class="flex items-center gap-2">
							{#if hasConfig}
								<button
									type="submit"
									form="test-connection-form"
									disabled={isTestingConnection}
									class="inline-flex items-center gap-1.5 h-9 px-3 rounded-lg text-xs font-medium border border-border bg-background hover:bg-accent text-foreground transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
								>
									{#if isTestingConnection}
										<Loader2 class="size-3.5 animate-spin" />
										<span>Testing...</span>
									{:else}
										<Cable class="size-3.5" />
										<span>Test Connection</span>
									{/if}
								</button>
							{/if}
							{#if isEditingConfig}
								<Button type="submit" class="h-9 px-4 rounded-lg text-xs font-medium cursor-pointer">
									<FileJson class="size-3.5 me-1.5" />
									Save Configuration
								</Button>
							{/if}
						</div>
					</div>

					<!-- Connection test result feedback (in config tab) -->
					{#if connectionTestResult || connectionTestError}
						<div class="mt-3">
							{#if connectionTestResult}
								<div
									class="p-3 rounded-lg border text-xs space-y-1.5 {connectionTestResult.success
										? 'bg-emerald-500/10 border-emerald-500/20'
										: 'bg-destructive/10 border-destructive/20'}"
								>
									<div class="flex items-center gap-1.5 font-medium">
										{#if connectionTestResult.success}
											<CheckCircle2 class="size-3.5 text-emerald-600 dark:text-emerald-400" />
											<span class="text-emerald-600 dark:text-emerald-400"
												>Connection Healthy</span
											>
										{:else}
											<XCircle class="size-3.5 text-destructive" />
											<span class="text-destructive">Connection Unreachable</span>
										{/if}
									</div>
									<p class="text-muted-foreground">
										{connectionTestResult.message}
									</p>
									<p class="font-mono text-[11px] text-muted-foreground">
										Response time: {connectionTestResult.latencyMs}ms
									</p>
								</div>
							{/if}
							{#if connectionTestError}
								<div
									class="p-3 rounded-lg border border-destructive/20 bg-destructive/10 text-xs space-y-1"
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
						</div>
					{/if}
				</form>
			</Card.Content>
		</Card.Root>
	{:else}
		<!-- Fallback: raw JSON editor when no schema is available -->
		<Card.Root class="rounded-xl border-border bg-card shadow-xs">
			<Card.Header class="pb-3 border-b border-border">
				<Card.Title class="text-base font-bold tracking-tight flex items-center gap-2">
					<FileJson class="size-4" />
					Connector Configuration
				</Card.Title>
				<Card.Description class="text-xs text-muted-foreground mt-1">
					No schema defined for "{datasource.type}". Edit the raw JSON below.
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
								setTimeout(() => (configSaved = false), 3000);
							}
							await update();
						};
					}}
				>
					<Field.Field>
						<Field.Label for="config-json" class="text-xs font-medium">
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

					<div class="flex items-center justify-between pt-4 border-t border-border mt-4">
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
						<Button type="submit" class="h-9 px-4 rounded-lg text-xs font-medium cursor-pointer">
							<FileJson class="size-3.5 me-1.5" />
							Save Configuration
						</Button>
					</div>
				</form>
			</Card.Content>
		</Card.Root>
	{/if}
</div>
