<script lang="ts">
	import { cn } from "$lib/utils";

	export interface CheckpointItem {
		id: string;
		title: string;
		number: number;
	}

	interface Props {
		checkpoints: CheckpointItem[];
		activeId?: string;
		onSelect?: (id: string) => void;
	}

	let { checkpoints, activeId = $bindable(""), onSelect }: Props = $props();

	let isRailHovered = $state(false);
	let hoveredItemId = $state<string | null>(null);

	function handleClick(cp: CheckpointItem) {
		activeId = cp.id;
		onSelect?.(cp.id);
		const targetEl = document.getElementById(cp.id);
		if (targetEl) {
			targetEl.scrollIntoView({ behavior: "smooth", block: "start" });
		}
	}
</script>

{#if checkpoints.length > 0}
	<div
		class="fixed right-3 top-1/2 -translate-y-1/2 z-30 pointer-events-auto transition-all duration-300 ease-out"
		onmouseenter={() => (isRailHovered = true)}
		onmouseleave={() => {
			isRailHovered = false;
			hoveredItemId = null;
		}}
	>
		{#if !isRailHovered}
			<!-- FIRST VIEW: Minimalist vertical tick marks rail (Default un-hovered state) -->
			<div
				class="flex flex-col items-end gap-2.5 py-3 px-1.5 rounded-full bg-background/80 backdrop-blur-md border border-border/60 shadow-xs animate-in fade-in duration-200 cursor-pointer"
			>
				{#each checkpoints as cp (cp.id)}
					{@const isActive = activeId === cp.id}
					<button
						type="button"
						onclick={() => handleClick(cp)}
						aria-label={`Jump to prompt ${cp.number}`}
						class={cn(
							"h-1.5 rounded-full transition-all duration-200 cursor-pointer",
							isActive
								? "w-6 bg-primary shadow-xs"
								: "w-2.5 bg-muted-foreground/40 hover:w-4 hover:bg-muted-foreground"
						)}
					/>
				{/each}
			</div>
		{:else}
			<!-- SECOND VIEW: Full outline menu panel (shadcn popover tokens, in-place highlight on hover) -->
			<div
				class="flex flex-col w-64 max-h-[72vh] rounded-2xl bg-popover/95 backdrop-blur-xl border border-border p-2 shadow-xl animate-in fade-in zoom-in-95 duration-200 text-popover-foreground"
			>
				<div class="flex flex-col gap-1 overflow-y-auto no-scrollbar py-1">
					{#each checkpoints as cp (cp.id)}
						{@const isActive = activeId === cp.id}
						{@const isHovered = hoveredItemId === cp.id}

						<button
							type="button"
							onclick={() => handleClick(cp)}
							onmouseenter={() => (hoveredItemId = cp.id)}
							onmouseleave={() => (hoveredItemId = null)}
							class={cn(
								"w-full text-left truncate px-3.5 py-2.5 rounded-xl text-xs transition-all cursor-pointer font-sans leading-tight",
								isActive
									? "bg-accent text-accent-foreground font-semibold border border-border/60 shadow-xs"
									: isHovered
										? "bg-accent/60 text-accent-foreground font-medium"
										: "text-muted-foreground hover:text-foreground hover:bg-muted/50"
							)}
							title={cp.title}
						>
							{cp.title}
						</button>
					{/each}
				</div>
			</div>
		{/if}
	</div>
{/if}
