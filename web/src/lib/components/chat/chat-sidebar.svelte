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
	import { page } from "$app/state";
	import { enhance } from "$app/forms";
	import User from "@lucide/svelte/icons/user";
	import LogOut from "@lucide/svelte/icons/log-out";
	import type { ComponentProps } from "svelte";

	let {
		ref = $bindable(null),
		...restProps
	}: ComponentProps<typeof Sidebar.Root> = $props();

	let user = $derived(page.data.user);
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
	<Sidebar.Footer class="border-t border-sidebar-border p-3">
		{#if user}
			<div class="flex items-center justify-between gap-2 w-full">
				<a
					href="/profile"
					class="flex items-center gap-2.5 min-w-0 p-1.5 rounded-lg hover:bg-sidebar-accent transition-colors text-sidebar-foreground group"
				>
					<div class="size-8 rounded-lg bg-primary/10 border border-primary/20 flex items-center justify-center text-primary text-xs font-bold shrink-0">
						{user.fullName ? user.fullName.charAt(0).toUpperCase() : user.username?.charAt(0).toUpperCase()}
					</div>
					<div class="min-w-0 flex-1 text-start">
						<p class="text-xs font-medium truncate group-hover:text-sidebar-accent-foreground">{user.fullName || user.username}</p>
						<p class="text-[10px] text-muted-foreground truncate">@{user.username}</p>
					</div>
				</a>
				<form action="/logout" method="POST" use:enhance>
					<button
						type="submit"
						title="Sign Out"
						class="p-2 rounded-lg text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors cursor-pointer"
					>
						<LogOut class="size-4" />
						<span class="sr-only">Sign out</span>
					</button>
				</form>
			</div>
		{:else}
			<a
				href="/login"
				class="flex items-center justify-center gap-2 w-full p-2 text-xs font-medium rounded-lg bg-primary text-primary-foreground hover:bg-primary/90 transition-colors"
			>
				<User class="size-3.5" />
				<span>Sign In</span>
			</a>
		{/if}
	</Sidebar.Footer>
	<Sidebar.Rail />
</Sidebar.Root>
