<script lang="ts">
	import * as Sidebar from "$lib/components/ui/sidebar/index.js";
	import * as DropdownMenu from "$lib/components/ui/dropdown-menu/index.js";
	import { page } from "$app/state";
	import { enhance } from "$app/forms";
	import Database from "@lucide/svelte/icons/database";
	import Settings from "@lucide/svelte/icons/settings";
	import LogOut from "@lucide/svelte/icons/log-out";
	import User from "@lucide/svelte/icons/user";
	import Layers from "@lucide/svelte/icons/layers";
	import HelpCircle from "@lucide/svelte/icons/help-circle";
	import ChevronsUpDown from "@lucide/svelte/icons/chevrons-up-down";
	import Logo from "$lib/components/ui/logo/logo.svelte";
	import type { ComponentProps } from "svelte";

	let {
		ref = $bindable(null),
		...restProps
	}: ComponentProps<typeof Sidebar.Root> = $props();

	let user = $derived(page.data.user);
	let currentPath = $derived(page.url.pathname);

	const sidebar = Sidebar.useSidebar();

	let mainNav = $derived([
		{
			title: "Datasources",
			url: "/datasources",
			icon: Database,
			active: currentPath.startsWith("/datasources")
		},
		{
			title: "Settings",
			url: "/settings",
			icon: Settings,
			active: currentPath.startsWith("/settings")
		}
	]);

	let secondaryNav = [
		{
			title: "Platform Docs",
			url: "https://svelte.dev/docs",
			icon: Layers
		},
		{
			title: "Get Help",
			url: "/settings",
			icon: HelpCircle
		}
	];
</script>

<Sidebar.Root collapsible="offcanvas" {...restProps} bind:ref>
	<!-- Sidebar Brand Header (matching dashboard-01) -->
	<Sidebar.Header class="p-3.5 border-b border-sidebar-border/40">
		<Sidebar.Menu>
			<Sidebar.MenuItem>
				<Sidebar.MenuButton size="lg" class="h-20 w-full p-2.5 hover:bg-sidebar-accent/50 data-[state=open]:bg-sidebar-accent">
					{#snippet child({ props })}
						<a href="/datasources" {...props} class="flex items-center justify-start w-full h-full">
							<Logo class="h-16 w-48 sm:w-52 md:w-56 object-contain" />
						</a>
					{/snippet}
				</Sidebar.MenuButton>
			</Sidebar.MenuItem>
		</Sidebar.Menu>
	</Sidebar.Header>

	<!-- Sidebar Main Content (matching dashboard-01) -->
	<Sidebar.Content>
		<!-- Main Navigation Group -->
		<Sidebar.Group>
			<Sidebar.GroupLabel>Platform</Sidebar.GroupLabel>
			<Sidebar.GroupContent>
				<Sidebar.Menu>
					{#each mainNav as item (item.title)}
						{@const Icon = item.icon}
						<Sidebar.MenuItem>
							<Sidebar.MenuButton isActive={item.active}>
								{#snippet child({ props })}
									<a href={item.url} {...props}>
										<Icon class="size-4 me-2 text-muted-foreground" />
										<span>{item.title}</span>
									</a>
								{/snippet}
							</Sidebar.MenuButton>
						</Sidebar.MenuItem>
					{/each}
				</Sidebar.Menu>
			</Sidebar.GroupContent>
		</Sidebar.Group>

		<!-- Secondary Navigation Group (matching dashboard-01) -->
		<Sidebar.Group class="mt-auto">
			<Sidebar.GroupLabel>Support</Sidebar.GroupLabel>
			<Sidebar.GroupContent>
				<Sidebar.Menu>
					{#each secondaryNav as item (item.title)}
						{@const Icon = item.icon}
						<Sidebar.MenuItem>
							<Sidebar.MenuButton>
								{#snippet child({ props })}
									<a href={item.url} {...props}>
										<Icon class="size-4 me-2 text-muted-foreground" />
										<span>{item.title}</span>
									</a>
								{/snippet}
							</Sidebar.MenuButton>
						</Sidebar.MenuItem>
					{/each}
				</Sidebar.Menu>
			</Sidebar.GroupContent>
		</Sidebar.Group>
	</Sidebar.Content>

	<!-- Sidebar Footer User Menu (matching dashboard-01 NavUser) -->
	<Sidebar.Footer>
		<Sidebar.Menu>
			<Sidebar.MenuItem>
				{#if user}
					<DropdownMenu.Root>
						<DropdownMenu.Trigger>
							{#snippet child({ props })}
								<Sidebar.MenuButton
									{...props}
									size="lg"
									class="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground"
								>
									<div class="size-8 rounded-lg bg-muted flex items-center justify-center font-bold text-xs shrink-0">
										{user.fullName ? user.fullName.charAt(0).toUpperCase() : user.username?.charAt(0).toUpperCase()}
									</div>
									<div class="grid flex-1 text-start text-sm leading-tight">
										<span class="truncate font-medium">{user.fullName || user.username}</span>
										<span class="truncate text-xs text-muted-foreground">@{user.username}</span>
									</div>
									<ChevronsUpDown class="ms-auto size-4 text-muted-foreground" />
								</Sidebar.MenuButton>
							{/snippet}
						</DropdownMenu.Trigger>

						<DropdownMenu.Content
							class="w-(--bits-dropdown-menu-anchor-width) min-w-56 rounded-lg"
							side={sidebar.isMobile ? "bottom" : "right"}
							align="end"
							sideOffset={4}
						>
							<DropdownMenu.Label class="p-0 font-normal">
								<div class="flex items-center gap-2 px-2 py-1.5 text-start text-sm">
									<div class="size-8 rounded-lg bg-muted flex items-center justify-center font-bold text-xs shrink-0">
										{user.fullName ? user.fullName.charAt(0).toUpperCase() : user.username?.charAt(0).toUpperCase()}
									</div>
									<div class="grid flex-1 text-start text-sm leading-tight">
										<span class="truncate font-medium">{user.fullName || user.username}</span>
										<span class="truncate text-xs text-muted-foreground">@{user.username}</span>
									</div>
								</div>
							</DropdownMenu.Label>
							<DropdownMenu.Separator />

							<DropdownMenu.Group>
								<DropdownMenu.Item>
									{#snippet child({ props })}
										<a href="/settings" {...props} class="flex items-center gap-2 w-full">
											<User class="size-4 text-muted-foreground" />
											<span>Account Settings</span>
										</a>
									{/snippet}
								</DropdownMenu.Item>
							</DropdownMenu.Group>

							<DropdownMenu.Separator />

							<DropdownMenu.Item class="p-0">
								{#snippet child({ props })}
									<form action="/logout" method="POST" use:enhance class="w-full">
										<button
											{...props}
											type="submit"
											class="flex items-center gap-2 w-full px-2 py-1.5 text-sm text-destructive hover:bg-destructive/10 rounded-sm cursor-pointer"
										>
											<LogOut class="size-4" />
											<span>Log out</span>
										</button>
									</form>
								{/snippet}
							</DropdownMenu.Item>
						</DropdownMenu.Content>
					</DropdownMenu.Root>
				{:else}
					<Sidebar.MenuButton>
						{#snippet child({ props })}
							<a href="/login" {...props}>
								<User class="size-4 me-2" />
								<span>Sign In</span>
							</a>
						{/snippet}
					</Sidebar.MenuButton>
				{/if}
			</Sidebar.MenuItem>
		</Sidebar.Menu>
	</Sidebar.Footer>
</Sidebar.Root>
