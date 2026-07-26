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
				class="flex flex-col items-end gap-2.5 py-3 px-1.5 rounded-full bg-background/50 backdrop-blur-md border border-border/40 shadow-xs animate-in fade-in duration-200 cursor-pointer"
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
								? "w-6 bg-primary shadow-[0_0_8px_rgba(59,130,246,0.6)]"
								: "w-2.5 bg-muted-foreground/40 hover:w-4 hover:bg-muted-foreground/80"
						)}
					/>
				{/each}
			</div>
		{:else}
			<!-- SECOND VIEW: Full dark outline menu panel (In-place highlight on hover, no left translation) -->
			<div
				class="flex flex-col w-64 max-h-[72vh] rounded-2xl bg-[#181818]/95 backdrop-blur-2xl border border-white/10 p-2 shadow-2xl animate-in fade-in zoom-in-95 duration-200"
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
									? "bg-white/20 text-white font-medium shadow-xs border border-white/10"
									: isHovered
										? "bg-white/10 text-white font-medium"
										: "text-zinc-400 hover:text-zinc-200 hover:bg-white/5"
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
