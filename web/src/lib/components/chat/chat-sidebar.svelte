<script lang="ts" module>
	// sample chat data
	const data = {
		versions: ["Default", "Work", "Personal"],
		navMain: [
			{
				title: "Today",
				url: "#",
				items: [
					{
						title: "Draft a project update email",
						url: "#",
					},
					{
						title: "Summarize meeting notes",
						url: "#",
					},
				],
			},
			{
				title: "Yesterday",
				url: "#",
				items: [
					{
						title: "Brainstorm blog post ideas",
						url: "#",
					},
					{
						title: "Explain SQL joins simply",
						url: "#",
						isActive: true,
					},
					{
						title: "Write a friendly follow-up",
						url: "#",
					},
					{
						title: "Create a study plan for exams",
						url: "#",
					},
				],
			},
		],
	};
</script>

<script lang="ts">
	import SearchForm from "./search-form.svelte";
	import * as Sidebar from "$lib/components/ui/sidebar/index.js";
	import type { ComponentProps } from "svelte";

	let {
		ref = $bindable(null),
		...restProps
	}: ComponentProps<typeof Sidebar.Root> = $props();
</script>

<Sidebar.Root {...restProps} bind:ref>
	<Sidebar.Header>
		<SearchForm />
	</Sidebar.Header>
	<Sidebar.Content>
		<!-- We create a Sidebar.Group for each parent. -->
		{#each data.navMain as group (group.title)}
			<Sidebar.Group>
				<Sidebar.GroupLabel>{group.title}</Sidebar.GroupLabel>
				<Sidebar.GroupContent>
					<Sidebar.Menu>
						{#each group.items as item (item.title)}
							<Sidebar.MenuItem>
								<Sidebar.MenuButton isActive={item.isActive}>
									{#snippet child({ props })}
										<a href={item.url} {...props}
											>{item.title}</a
										>
									{/snippet}
								</Sidebar.MenuButton>
							</Sidebar.MenuItem>
						{/each}
					</Sidebar.Menu>
				</Sidebar.GroupContent>
			</Sidebar.Group>
		{/each}
	</Sidebar.Content>
	<Sidebar.Rail />
</Sidebar.Root>
