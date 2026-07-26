<script lang="ts">
	import { Streamdown, type StreamdownProps } from "streamdown-svelte";
	import { mode } from "mode-watcher";
	import githubDarkDefault from "@shikijs/themes/github-dark-default";
	import githubLightDefault from "@shikijs/themes/github-light-default";
	import { cn } from "$lib/utils";

	type Props = StreamdownProps;

	let { content, class: className, ...rest }: Props = $props();
	let currentTheme = $derived(
		mode.current === "dark" ? "github-dark-default" : "github-light-default"
	);
</script>

<div
	class={cn(
		"w-full max-w-none text-foreground text-sm leading-relaxed",
		"prose prose-neutral dark:prose-invert max-w-none",
		"[&>*:first-child]:mt-0 [&>*:last-child]:mb-0",
		// Paragraphs
		"[&_p]:my-4 [&_p]:leading-relaxed [&_p]:text-foreground/90",
		// Headings spacing
		"[&_h1]:mt-8 [&_h1]:mb-4 [&_h1]:text-xl [&_h1]:font-bold [&_h1]:tracking-tight [&_h1]:text-foreground",
		"[&_h2]:mt-7 [&_h2]:mb-3.5 [&_h2]:text-lg [&_h2]:font-bold [&_h2]:tracking-tight [&_h2]:text-foreground",
		"[&_h3]:mt-6 [&_h3]:mb-3 [&_h3]:text-base [&_h3]:font-semibold [&_h3]:text-foreground",
		"[&_h4]:mt-5 [&_h4]:mb-2 [&_h4]:text-sm [&_h4]:font-semibold [&_h4]:text-foreground",
		// Lists spacing
		"[&_ul]:my-4 [&_ul]:list-disc [&_ul]:pl-6 [&_ul]:space-y-1.5",
		"[&_ol]:my-4 [&_ol]:list-decimal [&_ol]:pl-6 [&_ol]:space-y-1.5",
		"[&_li]:my-1 [&_li]:leading-normal",
		// Inline code
		"[&_:not(pre)>code]:rounded-md [&_:not(pre)>code]:bg-muted/80 [&_:not(pre)>code]:px-1.5 [&_:not(pre)>code]:py-0.5 [&_:not(pre)>code]:text-xs [&_:not(pre)>code]:font-mono [&_:not(pre)>code]:font-normal [&_:not(pre)>code]:border [&_:not(pre)>code]:border-border/50",
		// Code blocks
		"[&_pre]:my-6 [&_pre]:rounded-2xl [&_pre]:border [&_pre]:border-border/70 [&_pre]:shadow-md [&_pre]:overflow-x-auto",
		// Markdown Images
		"[&_img]:my-5 [&_img]:rounded-2xl [&_img]:border [&_img]:border-border/60 [&_img]:shadow-md [&_img]:max-w-full [&_img]:h-auto",
		// Tables
		"[&_table]:my-6 [&_table]:w-full [&_table]:border-collapse [&_table]:overflow-hidden [&_table]:rounded-xl [&_table]:border [&_table]:border-border/60",
		"[&_th]:border-b [&_th]:border-border/60 [&_th]:bg-muted/50 [&_th]:px-4 [&_th]:py-3 [&_th]:text-left [&_th]:font-semibold [&_th]:text-foreground",
		"[&_td]:border-b [&_td]:border-border/40 [&_td]:px-4 [&_td]:py-2.5 [&_td]:text-muted-foreground",
		// Blockquotes
		"[&_blockquote]:my-5 [&_blockquote]:border-l-4 [&_blockquote]:border-primary/80 [&_blockquote]:bg-primary/5 [&_blockquote]:py-3 [&_blockquote]:px-4 [&_blockquote]:rounded-r-xl [&_blockquote]:italic [&_blockquote]:text-muted-foreground",
		className
	)}
>
	<Streamdown
		{content}
		baseTheme="shadcn"
		shikiTheme={currentTheme}
		shikiThemes={{
			"github-light-default": githubLightDefault,
			"github-dark-default": githubDarkDefault,
		}}
		{...rest}
	/>
</div>
