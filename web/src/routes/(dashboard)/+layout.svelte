<script lang="ts">
	let { children } = $props();
	import DashboardSidebar from "$lib/components/dashboard/dashboard-sidebar.svelte";
	import * as Breadcrumb from "$lib/components/ui/breadcrumb/index.js";
	import { Separator } from "$lib/components/ui/separator/index.js";
	import * as Sidebar from "$lib/components/ui/sidebar/index.js";
	import * as DropdownMenu from "$lib/components/ui/dropdown-menu/index.js";
	import { page } from "$app/state";
	import { enhance } from "$app/forms";
	import User from "@lucide/svelte/icons/user";
	import Database from "@lucide/svelte/icons/database";
	import Settings from "@lucide/svelte/icons/settings";
	import LogOut from "@lucide/svelte/icons/log-out";
	import ChevronDown from "@lucide/svelte/icons/chevron-down";

	let user = $derived(page.data.user);
</script>

<Sidebar.Provider
	style="--sidebar-width: calc(var(--spacing) * 72); --header-height: calc(var(--spacing) * 12);"
>
	<DashboardSidebar variant="inset" />
	<Sidebar.Inset>
		<header
			class="sticky top-0 z-10 flex h-16 shrink-0 items-center justify-between gap-2 border-b bg-background/95 backdrop-blur-md px-4 sm:px-6"
		>
			<div class="flex items-center gap-2">
				<Sidebar.Trigger class="-ms-1" />
				<Separator orientation="vertical" class="me-2 h-4" />
				<Breadcrumb.Root>
					<Breadcrumb.List>
						<Breadcrumb.Item class="hidden md:block">
							<Breadcrumb.Link href="/datasources">Platform Admin</Breadcrumb.Link>
						</Breadcrumb.Item>
						<Breadcrumb.Separator class="hidden md:block" />
						<Breadcrumb.Item>
							<Breadcrumb.Page>Datasource Management</Breadcrumb.Page>
						</Breadcrumb.Item>
					</Breadcrumb.List>
				</Breadcrumb.Root>
			</div>

			<div class="flex items-center gap-3">
				{#if user}
					<DropdownMenu.Root>
						<DropdownMenu.Trigger>
							{#snippet child({ props })}
								<button
									{...props}
									type="button"
									class="flex items-center gap-2 p-1 pl-1.5 pr-2.5 rounded-full border border-border bg-card hover:bg-accent transition-colors cursor-pointer text-xs"
								>
									<div class="size-7 rounded-full bg-primary/10 text-primary border border-primary/20 flex items-center justify-center font-bold text-xs shrink-0">
										{user.fullName ? user.fullName.charAt(0).toUpperCase() : user.username?.charAt(0).toUpperCase()}
									</div>
									<span class="max-w-[120px] truncate font-medium">{user.username}</span>
									<ChevronDown class="size-3 text-muted-foreground" />
								</button>
							{/snippet}
						</DropdownMenu.Trigger>

						<DropdownMenu.Content align="end" class="w-64 p-2 rounded-2xl border-border bg-popover shadow-xl text-popover-foreground">
							<!-- User Header Card -->
							<div class="flex items-center gap-3 p-2.5 mb-1 rounded-xl bg-accent/40">
								<div class="size-9 rounded-full bg-primary/10 border border-primary/20 flex items-center justify-center text-primary font-bold text-xs shrink-0">
									{user.fullName ? user.fullName.charAt(0).toUpperCase() : user.username?.charAt(0).toUpperCase()}
								</div>
								<div class="min-w-0 flex-1">
									<p class="text-xs font-bold truncate leading-tight">{user.username}</p>
									{#if user.fullName}
										<p class="text-[11px] text-muted-foreground truncate">{user.fullName}</p>
									{/if}
									<span class="inline-block px-1.5 py-0.5 rounded text-[10px] font-mono bg-primary/10 text-primary font-semibold mt-1">
										{user.role || "USER"}
									</span>
								</div>
							</div>

							<DropdownMenu.Separator class="my-1" />

							<!-- Navigation Section -->
							<DropdownMenu.Group>
								<DropdownMenu.Item>
									{#snippet child({ props })}
										<a href="/settings" {...props} class="flex items-center gap-2.5 px-2.5 py-2 text-xs rounded-xl cursor-pointer">
											<User class="size-4 text-muted-foreground" />
											<span>Account Info</span>
										</a>
									{/snippet}
								</DropdownMenu.Item>

								<DropdownMenu.Item>
									{#snippet child({ props })}
										<a href="/datasources" {...props} class="flex items-center gap-2.5 px-2.5 py-2 text-xs rounded-xl cursor-pointer">
											<Database class="size-4 text-muted-foreground" />
											<span>Datasources</span>
										</a>
									{/snippet}
								</DropdownMenu.Item>
							</DropdownMenu.Group>

							<DropdownMenu.Separator class="my-1" />

							<!-- Settings Section -->
							<DropdownMenu.Group>
								<DropdownMenu.Item>
									{#snippet child({ props })}
										<a href="/settings" {...props} class="flex items-center gap-2.5 px-2.5 py-2 text-xs rounded-xl cursor-pointer">
											<Settings class="size-4 text-muted-foreground" />
											<span>Settings</span>
										</a>
									{/snippet}
								</DropdownMenu.Item>
							</DropdownMenu.Group>

							<DropdownMenu.Separator class="my-1" />

							<!-- Sign Out Section -->
							<DropdownMenu.Item class="p-0">
								{#snippet child({ props })}
									<form action="/logout" method="POST" use:enhance class="w-full">
										<button
											{...props}
											type="submit"
											class="flex items-center gap-2.5 w-full px-2.5 py-2 text-xs text-destructive hover:bg-destructive/10 rounded-xl cursor-pointer font-medium text-start"
										>
											<LogOut class="size-4" />
											<span>Sign out</span>
										</button>
									</form>
								{/snippet}
							</DropdownMenu.Item>
						</DropdownMenu.Content>
					</DropdownMenu.Root>
				{/if}
			</div>
		</header>
		<div class="flex flex-1 flex-col overflow-y-auto min-h-0 bg-background/50">
			{@render children?.()}
		</div>
	</Sidebar.Inset>
</Sidebar.Provider>
