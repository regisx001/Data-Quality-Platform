<script lang="ts">
    import type { Snippet } from "svelte";

    interface Props {
        children: Snippet;
        prompt?: Snippet;
    }

    let { children, prompt }: Props = $props();
</script>

<div class="relative flex flex-1 flex-col overflow-hidden min-h-0">
    <!-- Messages slot: gradient mask fades out messages as they approach the bottom prompt area -->
    <div
        class="flex-1 overflow-y-auto min-h-0 px-4 pt-4 pb-36 [mask-image:linear-gradient(to_bottom,black_calc(100%-10rem),transparent_calc(100%-2rem))]"
    >
        {@render children()}
    </div>

    <!-- Bottom gradient shadow fade to ensure text under the prompt is seamlessly hidden -->
    <div
        class="absolute bottom-0 left-0 right-0 h-32 bg-gradient-to-t from-background via-background/80 to-transparent pointer-events-none z-10"
    ></div>

    <!-- Floating prompt snippet slot -->
    {#if prompt}
        <div class="absolute bottom-0 left-0 right-0 z-20 p-4 pointer-events-none">
            <div class="pointer-events-auto">
                {@render prompt()}
            </div>
        </div>
    {/if}
</div>
