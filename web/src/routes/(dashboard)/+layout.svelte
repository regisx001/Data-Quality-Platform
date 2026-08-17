<script lang="ts">
	let { children } = $props();
	import DashboardSidebar from "$lib/components/dashboard/dashboard-sidebar.svelte";
	import * as Breadcrumb from "$lib/components/ui/breadcrumb/index.js";
	import { Separator } from "$lib/components/ui/separator/index.js";
	import * as Sidebar from "$lib/components/ui/sidebar/index.js";
	import * as DropdownMenu from "$lib/components/ui/dropdown-menu/index.js";
	import { page, navigating } from "$app/state";
	import { enhance } from "$app/forms";
	import User from "@lucide/svelte/icons/user";
	import Database from "@lucide/svelte/icons/database";
	import Settings from "@lucide/svelte/icons/settings";
	import LogOut from "@lucide/svelte/icons/log-out";
	import ChevronDown from "@lucide/svelte/icons/chevron-down";
	import ArrowLeftRight from "@lucide/svelte/icons/arrow-left-right";
	import Activity from "@lucide/svelte/icons/activity";
	import Globe from "@lucide/svelte/icons/globe";

	let user = $derived(page.data.user);
	let isNavigating = $derived(navigating.to !== null);
</script>

<!-- Top Global Navigation Progress Bar -->
{#if isNavigating}
	<div class="fixed top-0 left-0 right-0 z-50 h-1 bg-primary/20 overflow-hidden">
		<div class="h-full bg-primary animate-pulse w-full origin-left transition-all duration-300"></div>
	</div>
{/if}

<Sidebar.Provider
	style="--sidebar-width: calc(var(--spacing) * 72); --header-height: calc(var(--spacing) * 12);"
>
	<DashboardSidebar variant="inset" />
	<Sidebar.Inset>
		<header
			class="sticky top-0 z-10 flex h-14 shrink-0 items-center justify-between gap-2 border-b bg-background/95 backdrop-blur-md px-4 sm:px-6"
		>
			<div class="flex items-center gap-2">
				<Sidebar.Trigger class="-ms-1" />
				<Separator orientation="vertical" class="me-2 h-4" />
				<Breadcrumb.Root>
					<Breadcrumb.List>
						<Breadcrumb.Item class="hidden md:block">
							<Breadcrumb.Link href="/datasources" class="text-sm font-medium">Platform Admin</Breadcrumb.Link>
						</Breadcrumb.Item>
						<Breadcrumb.Separator class="hidden md:block" />
						<Breadcrumb.Item>
							<Breadcrumb.Page class="text-sm font-semibold">
								{page.url.pathname.startsWith("/logs/table")
									? "Log Explorer Table"
									: page.url.pathname.startsWith("/logs/batch")
										? "Batch Observability & Analytics"
										: page.url.pathname.startsWith("/logs")
											? "Real-Time Log Stream"
											: page.url.pathname.startsWith("/http-telemetry")
												? "HTTP Telemetry Dashboard"
												: page.url.pathname.startsWith("/settings")
													? "Settings"
													: "Datasource Management"}
							</Breadcrumb.Page>
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
									class="flex items-center gap-2 p-1 pl-1 pr-2.5 rounded-full border border-border/80 bg-card hover:bg-accent/80 transition-all cursor-pointer text-sm focus:outline-none focus:ring-1 focus:ring-ring"
								>
									<div class="size-7 rounded-full bg-sky-500/15 text-sky-400 border border-sky-500/30 flex items-center justify-center font-bold text-xs shrink-0">
										{user.fullName ? user.fullName.charAt(0).toUpperCase() : user.username?.charAt(0).toUpperCase()}
									</div>
									<span class="max-w-[130px] truncate font-medium text-sm text-foreground">{user.username}</span>
									<ChevronDown class="size-3.5 text-muted-foreground" />
								</button>
							{/snippet}
						</DropdownMenu.Trigger>

						<DropdownMenu.Content align="end" class="w-68 p-1.5 rounded-xl border border-border/80 bg-popover shadow-2xl text-popover-foreground">
							<!-- GitHub-Style Header Section -->
							<div class="flex items-center justify-between p-3 pb-2.5">
								<div class="flex items-center gap-3 min-w-0">
									<div class="size-9 rounded-full bg-sky-500/15 text-sky-400 border border-sky-500/30 flex items-center justify-center font-bold text-sm shrink-0">
										{user.fullName ? user.fullName.charAt(0).toUpperCase() : user.username?.charAt(0).toUpperCase()}
									</div>
									<div class="min-w-0 flex-1">
										<p class="text-sm font-semibold text-foreground truncate leading-tight">{user.username}</p>
										<p class="text-xs text-muted-foreground truncate leading-tight mt-0.5">
											{user.fullName || user.email || "Platform Admin"}
										</p>
									</div>
								</div>
								<div title="Switch Account" class="p-1.5 rounded-md hover:bg-accent text-muted-foreground hover:text-foreground cursor-pointer transition-colors shrink-0">
									<ArrowLeftRight class="size-4" />
								</div>
							</div>

							<DropdownMenu.Separator class="my-1 border-t border-border/60" />

							<!-- Navigation Section -->
							<DropdownMenu.Group>
								<DropdownMenu.Item>
									{#snippet child({ props })}
										<a href="/settings" {...props} class="flex items-center gap-3 px-3 py-2 text-sm rounded-md hover:bg-accent cursor-pointer transition-colors">
											<User class="size-4 text-muted-foreground" />
											<span class="font-normal text-foreground">Profile</span>
										</a>
									{/snippet}
								</DropdownMenu.Item>

								<DropdownMenu.Item>
									{#snippet child({ props })}
										<a href="/datasources" {...props} class="flex items-center gap-3 px-3 py-2 text-sm rounded-md hover:bg-accent cursor-pointer transition-colors">
											<Database class="size-4 text-muted-foreground" />
											<span class="font-normal text-foreground">Datasources</span>
										</a>
									{/snippet}
								</DropdownMenu.Item>

								<DropdownMenu.Item>
									{#snippet child({ props })}
										<a href="/logs" {...props} class="flex items-center gap-3 px-3 py-2 text-sm rounded-md hover:bg-accent cursor-pointer transition-colors">
											<Activity class="size-4 text-muted-foreground" />
											<span class="font-normal text-foreground">Real-Time Logs</span>
										</a>
									{/snippet}
								</DropdownMenu.Item>

								<DropdownMenu.Item>
									{#snippet child({ props })}
										<a href="/http-telemetry" {...props} class="flex items-center gap-3 px-3 py-2 text-sm rounded-md hover:bg-accent cursor-pointer transition-colors">
											<Globe class="size-4 text-muted-foreground" />
											<span class="font-normal text-foreground">HTTP Telemetry</span>
										</a>
									{/snippet}
								</DropdownMenu.Item>
							</DropdownMenu.Group>

							<DropdownMenu.Separator class="my-1 border-t border-border/60" />

							<!-- Settings Section -->
							<DropdownMenu.Group>
								<DropdownMenu.Item>
									{#snippet child({ props })}
										<a href="/settings" {...props} class="flex items-center gap-3 px-3 py-2 text-sm rounded-md hover:bg-accent cursor-pointer transition-colors">
											<Settings class="size-4 text-muted-foreground" />
											<span class="font-normal text-foreground">Settings</span>
										</a>
									{/snippet}
								</DropdownMenu.Item>
							</DropdownMenu.Group>

							<DropdownMenu.Separator class="my-1 border-t border-border/60" />

							<!-- Sign Out Section -->
							<DropdownMenu.Item class="p-0">
								{#snippet child({ props })}
									<form action="/logout" method="POST" use:enhance class="w-full">
										<button
											{...props}
											type="submit"
											class="flex items-center gap-3 w-full px-3 py-2 text-sm text-destructive hover:bg-destructive/10 rounded-md cursor-pointer font-medium text-start transition-colors"
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
		<div class={`flex flex-1 flex-col overflow-y-auto min-h-0 bg-background/50 transition-opacity duration-200 ${isNavigating ? 'opacity-50 pointer-events-none' : 'opacity-100'}`}>
			{@render children?.()}
		</div>
	</Sidebar.Inset>
</Sidebar.Provider>
