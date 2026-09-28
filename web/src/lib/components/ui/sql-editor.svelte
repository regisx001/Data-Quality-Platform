<script lang="ts">
	import { cn } from "$lib/utils.js";
	import Code2 from "@lucide/svelte/icons/code-2";
	import Sparkles from "@lucide/svelte/icons/sparkles";

	interface ColumnInfo {
		name: string;
		dataType?: string;
	}

	let {
		value = $bindable(""),
		name = "conditionExpression",
		id = "sql-editor",
		placeholder = "-- Enter SQL predicate / expression\n-- e.g. amount > 0 AND status IN ('PAID', 'PENDING')",
		columns = [],
		disabled = false,
		class: className = "",
	}: {
		value?: string;
		name?: string;
		id?: string;
		placeholder?: string;
		columns?: (ColumnInfo | string)[];
		disabled?: boolean;
		class?: string;
	} = $props();

	let textareaRef = $state<HTMLTextAreaElement | null>(null);

	let normalizedColumns = $derived<ColumnInfo[]>(
		columns.map((col) => (typeof col === "string" ? { name: col } : col))
	);

	let lines = $derived((value || "").split("\n"));
	let lineCount = $derived(Math.max(lines.length, 8));

	const quickSnippets = [
		{ label: "IS NOT NULL", sql: " IS NOT NULL" },
		{ label: "IS NULL", sql: " IS NULL" },
		{ label: "> 0", sql: " > 0" },
		{ label: "BETWEEN", sql: " BETWEEN 0 AND 100" },
		{ label: "IN (...) ", sql: " IN ('val1', 'val2')" },
		{ label: "LIKE '%...%'", sql: " LIKE '%pattern%'" },
		{ label: "LENGTH()", sql: " LENGTH(column_name) > 0" },
		{ label: "TRIM() != ''", sql: " TRIM(column_name) != ''" },
	];

	function insertSnippet(snippet: string) {
		if (!textareaRef) {
			value = (value ? value + " " : "") + snippet;
			return;
		}
		const start = textareaRef.selectionStart ?? value.length;
		const end = textareaRef.selectionEnd ?? value.length;
		const before = value.substring(0, start);
		const after = value.substring(end);
		value = before + snippet + after;

		// Move cursor right after the inserted text
		const newPos = start + snippet.length;
		setTimeout(() => {
			if (textareaRef) {
				textareaRef.focus();
				textareaRef.setSelectionRange(newPos, newPos);
			}
		}, 0);
	}

	function handleKeydown(e: KeyboardEvent) {
		if (e.key === "Tab") {
			e.preventDefault();
			insertSnippet("  ");
		}
	}
</script>

<div
	class={cn(
		"rounded-xl border border-zinc-800 bg-zinc-950 text-zinc-100 overflow-hidden shadow-md flex flex-col font-mono text-xs transition-colors",
		disabled && "opacity-60 pointer-events-none",
		className
	)}
>
	<!-- Top Bar / Editor Header -->
	<div
		class="flex items-center justify-between px-3.5 py-2.5 bg-zinc-900/95 border-b border-zinc-800 text-[11px] text-zinc-400 select-none flex-wrap gap-2"
	>
		<div class="flex items-center gap-2.5">
			<!-- Window dots -->
			<div class="flex items-center gap-1.5">
				<span class="size-2.5 rounded-full bg-red-500/80 inline-block"></span>
				<span class="size-2.5 rounded-full bg-amber-500/80 inline-block"></span>
				<span class="size-2.5 rounded-full bg-emerald-500/80 inline-block"></span>
			</div>
			<div class="h-3.5 w-px bg-zinc-700/60 mx-1"></div>
			<div class="flex items-center gap-1.5 font-semibold text-zinc-200">
				<Code2 class="size-3.5 text-sky-400" />
				<span class="tracking-wide">SQL Condition Editor</span>
			</div>
		</div>

		<!-- Quick Snippet Pills & Actions -->
		<div class="flex items-center gap-1.5 flex-wrap">
			<span class="text-[10px] text-zinc-500 uppercase tracking-wider font-sans font-medium">Snippets:</span>
			{#each quickSnippets as snip}
				<button
					type="button"
					onclick={() => insertSnippet(snip.sql)}
					disabled={disabled}
					class="px-2 py-0.5 rounded bg-zinc-800/90 hover:bg-zinc-700 text-zinc-300 hover:text-white border border-zinc-700/60 text-[10px] font-mono transition-colors cursor-pointer"
					title={`Insert ${snip.label}`}
				>
					{snip.label}
				</button>
			{/each}
			{#if (value || "").length > 0}
				<button
					type="button"
					onclick={() => (value = "")}
					disabled={disabled}
					class="px-2 py-0.5 rounded bg-destructive/15 hover:bg-destructive/25 text-destructive-foreground hover:text-white border border-destructive/30 text-[10px] font-sans font-medium transition-colors cursor-pointer ml-1"
					title="Clear SQL expression"
				>
					Clear
				</button>
			{/if}
		</div>
	</div>

	<!-- Column Quick Inserter (if available) -->
	{#if normalizedColumns.length > 0}
		<div
			class="flex items-center gap-2 px-3.5 py-1.5 bg-zinc-900/50 border-b border-zinc-800/60 text-[11px] overflow-x-auto no-scrollbar select-none"
		>
			<span class="text-[10px] text-zinc-500 shrink-0 font-sans font-medium uppercase tracking-wider">
				Insert Column:
			</span>
			<div class="flex items-center gap-1.5 flex-wrap">
				{#each normalizedColumns as col}
					<button
						type="button"
						onclick={() => insertSnippet(col.name)}
						disabled={disabled}
						class="px-2 py-0.5 rounded bg-zinc-800/80 hover:bg-zinc-700 text-sky-300 hover:text-sky-100 border border-zinc-700/50 text-[11px] font-mono transition-colors cursor-pointer shrink-0"
						title={`Click to insert ${col.name}${col.dataType ? ` (${col.dataType})` : ""}`}
					>
						{col.name}
					</button>
				{/each}
			</div>
		</div>
	{/if}

	<!-- Code Area with Line Numbers -->
	<div class="flex relative min-h-[220px] max-h-[480px] overflow-hidden bg-zinc-950">
		<!-- Line Numbers Gutter -->
		<div
			class="w-11 shrink-0 py-3 px-2.5 bg-zinc-950 text-right text-zinc-600 font-mono text-xs select-none border-r border-zinc-800/80 leading-relaxed"
		>
			{#each Array(lineCount) as _, idx}
				<div>{idx + 1}</div>
			{/each}
		</div>

		<!-- SQL Textarea -->
		<textarea
			bind:this={textareaRef}
			{id}
			{name}
			bind:value
			{placeholder}
			{disabled}
			onkeydown={handleKeydown}
			spellcheck="false"
			autocapitalize="off"
			autocomplete="off"
			class="flex-1 py-3 px-3.5 bg-transparent text-zinc-100 placeholder:text-zinc-600 font-mono text-xs sm:text-[13px] outline-none border-0 ring-0 focus:ring-0 leading-relaxed resize-y min-h-[220px] max-h-[480px] overflow-y-auto selection:bg-sky-500/30 selection:text-white"
			style="tab-size: 2;"
		></textarea>
	</div>

	<!-- Status Bar -->
	<div
		class="flex items-center justify-between px-3.5 py-1.5 bg-zinc-900/90 border-t border-zinc-800 text-[10px] text-zinc-500 font-mono select-none"
	>
		<div class="flex items-center gap-2">
			<span class="text-zinc-400 font-medium">SQL WHERE Predicate</span>
			<span>•</span>
			<span class="text-zinc-500">Press <kbd class="px-1 py-0.5 rounded bg-zinc-800 text-zinc-300 border border-zinc-700 text-[9px]">Tab</kbd> to indent</span>
		</div>
		<div class="flex items-center gap-3">
			<span>Lines: {lines.length}</span>
			<span>Chars: {(value || "").length}</span>
		</div>
	</div>
</div>
