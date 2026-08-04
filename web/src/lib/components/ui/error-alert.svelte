<script lang="ts">
	import type { ApiError } from "$lib/server/api/client";
	import AlertTriangle from "@lucide/svelte/icons/alert-triangle";
	import AlertOctagon from "@lucide/svelte/icons/alert-octagon";
	import ShieldAlert from "@lucide/svelte/icons/shield-alert";
	import Database from "@lucide/svelte/icons/database";
	import ServerCrash from "@lucide/svelte/icons/server-crash";
	import ChevronDown from "@lucide/svelte/icons/chevron-down";
	import ChevronUp from "@lucide/svelte/icons/chevron-up";
	import Copy from "@lucide/svelte/icons/copy";
	import Check from "@lucide/svelte/icons/check";
	import X from "@lucide/svelte/icons/x";

	let {
		error = null,
		title = "An error occurred",
		dismissable = true,
		onDismiss = () => {},
		class: className = ""
	}: {
		error?: ApiError | string | null;
		title?: string;
		dismissable?: boolean;
		onDismiss?: () => void;
		class?: string;
	} = $props();

	let showDetails = $state(false);
	let copied = $state(false);

	// Normalize error object
	let parsedError = $derived.by(() => {
		if (!error) return null;
		if (typeof error === "string") {
			return {
				status: 400,
				error: "Bad Request",
				code: "ERROR",
				message: error,
				module: "SYSTEM",
				details: null
			} as ApiError;
		}
		return error;
	});

	// Choose badge color scheme based on module / status
	let moduleBadgeClass = $derived.by(() => {
		if (!parsedError) return "";
		const mod = parsedError.module?.toUpperCase() || "";
		const status = parsedError.status || 400;

		if (mod === "AUTHENTICATION" || mod === "SECURITY" || status === 401 || status === 403) {
			return "bg-amber-500/15 text-amber-400 border-amber-500/30";
		}
		if (mod === "DATASOURCE" || mod === "CONNECTOR") {
			return "bg-purple-500/15 text-purple-400 border-purple-500/30";
		}
		if (mod === "DATASET" || mod === "VALIDATION" || mod === "RULES") {
			return "bg-blue-500/15 text-blue-400 border-blue-500/30";
		}
		if (mod === "STORAGE") {
			return "bg-emerald-500/15 text-emerald-400 border-emerald-500/30";
		}
		if (status >= 500) {
			return "bg-red-500/20 text-red-400 border-red-500/40";
		}
		return "bg-rose-500/15 text-rose-400 border-rose-500/30";
	});

	function copyErrorPayload() {
		if (!parsedError) return;
		navigator.clipboard.writeText(JSON.stringify(parsedError, null, 2));
		copied = true;
		setTimeout(() => (copied = false), 2000);
	}
</script>

{#if parsedError}
	<div
		class="relative w-full rounded-2xl border border-destructive/30 bg-destructive/10 p-4 sm:p-5 text-card-foreground shadow-lg backdrop-blur-md transition-all duration-200 animate-in fade-in slide-in-from-top-2 {className}"
	>
		<div class="flex items-start gap-3.5">
			<!-- Dynamic Icon based on module/status -->
			<div class="mt-0.5 rounded-xl p-2 bg-destructive/20 border border-destructive/30 text-destructive shrink-0">
				{#if parsedError.module === "AUTHENTICATION" || parsedError.status === 401 || parsedError.status === 403}
					<ShieldAlert class="size-5" />
				{:else if parsedError.module === "DATASOURCE" || parsedError.module === "CONNECTOR"}
					<Database class="size-5" />
				{:else if parsedError.status >= 500}
					<ServerCrash class="size-5" />
				{:else if parsedError.code === "VALIDATION_FAILED"}
					<AlertOctagon class="size-5" />
				{:else}
					<AlertTriangle class="size-5" />
				{/if}
			</div>

			<!-- Error Content Body -->
			<div class="flex-1 min-w-0">
				<!-- Header Row: Title & Badges -->
				<div class="flex flex-wrap items-center gap-2 mb-1">
					<h4 class="font-semibold text-base text-foreground tracking-tight">
						{title}
					</h4>

					{#if parsedError.module}
						<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border uppercase tracking-wider {moduleBadgeClass}">
							{parsedError.module}
						</span>
					{/if}

					{#if parsedError.code}
						<span class="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-mono bg-muted/60 text-muted-foreground border border-border">
							{parsedError.code}
						</span>
					{/if}

					{#if parsedError.status}
						<span class="ms-auto text-xs font-mono font-medium text-muted-foreground bg-background/50 px-2 py-0.5 rounded border border-border/40">
							HTTP {parsedError.status}
						</span>
					{/if}
				</div>

				<!-- Main Message -->
				<p class="text-sm font-medium text-destructive-foreground/90 leading-relaxed break-words">
					{parsedError.message}
				</p>

				<!-- Field Validation Details (if available) -->
				{#if parsedError.details && typeof parsedError.details === "object" && !Array.isArray(parsedError.details) && Object.keys(parsedError.details).length > 0}
					<div class="mt-3 pt-2.5 border-t border-destructive/20">
						<span class="text-xs font-semibold uppercase tracking-wider text-muted-foreground block mb-1.5">
							Validation Errors:
						</span>
						<ul class="grid grid-cols-1 sm:grid-cols-2 gap-1.5 text-xs">
							{#each Object.entries(parsedError.details) as [field, msg]}
								<li class="flex items-start gap-1.5 bg-background/40 p-1.5 rounded-lg border border-border/30">
									<span class="font-mono font-semibold text-foreground/90">{field}:</span>
									<span class="text-destructive font-medium">{msg}</span>
								</li>
							{/each}
						</ul>
					</div>
				{/if}

				<!-- Technical Details Accordion Toggle -->
				{#if parsedError.path || parsedError.timestamp || parsedError.details}
					<div class="mt-3 pt-2 flex items-center justify-between">
						<button
							type="button"
							onclick={() => (showDetails = !showDetails)}
							class="inline-flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
						>
							{#if showDetails}
								<ChevronUp class="size-3.5" />
								<span>Hide Technical Details</span>
							{:else}
								<ChevronDown class="size-3.5" />
								<span>Show Technical Details</span>
							{/if}
						</button>

						{#if showDetails}
							<button
								type="button"
								onclick={copyErrorPayload}
								class="inline-flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
								title="Copy JSON Payload"
							>
								{#if copied}
									<Check class="size-3.5 text-emerald-400" />
									<span class="text-emerald-400">Copied</span>
								{:else}
									<Copy class="size-3.5" />
									<span>Copy JSON</span>
								{/if}
							</button>
						{/if}
					</div>

					{#if showDetails}
						<div class="mt-2 p-3 bg-black/60 rounded-xl border border-border/50 text-xs font-mono text-muted-foreground overflow-x-auto space-y-1">
							{#if parsedError.path}
								<div><span class="text-foreground/70">Endpoint:</span> {parsedError.path}</div>
							{/if}
							{#if parsedError.timestamp}
								<div><span class="text-foreground/70">Timestamp:</span> {parsedError.timestamp}</div>
							{/if}
							{#if parsedError.error}
								<div><span class="text-foreground/70">HTTP Reason:</span> {parsedError.error}</div>
							{/if}
							{#if parsedError.details}
								<div class="mt-1">
									<span class="text-foreground/70">Payload Details:</span>
									<pre class="mt-1 p-2 bg-slate-950 rounded border border-slate-800 text-[11px] text-slate-300 overflow-x-auto">{JSON.stringify(parsedError.details, null, 2)}</pre>
								</div>
							{/if}
						</div>
					{/if}
				{/if}
			</div>

			<!-- Dismiss Button -->
			{#if dismissable}
				<button
					type="button"
					onclick={onDismiss}
					class="text-muted-foreground hover:text-foreground transition-colors p-1 rounded-lg hover:bg-background/20 cursor-pointer shrink-0"
					aria-label="Dismiss error"
				>
					<X class="size-4" />
				</button>
			{/if}
		</div>
	</div>
{/if}
