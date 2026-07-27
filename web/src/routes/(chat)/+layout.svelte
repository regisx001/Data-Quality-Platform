<script lang="ts">
    let { children } = $props();
    import AppSidebar from "$lib/components/chat/chat-sidebar.svelte";
    import * as Breadcrumb from "$lib/components/ui/breadcrumb/index.js";
    import { Separator } from "$lib/components/ui/separator/index.js";
    import * as Sidebar from "$lib/components/ui/sidebar/index.js";
    import { page } from "$app/state";
    import User from "@lucide/svelte/icons/user";

    let user = $derived(page.data.user);
</script>

<Sidebar.Provider>
    <AppSidebar />
    <Sidebar.Inset>
        <header
            class="sticky top-0 z-10 flex h-16 shrink-0 items-center justify-between gap-2 border-b bg-background px-4"
        >
            <div class="flex items-center gap-2">
                <Sidebar.Trigger class="-ms-1" />
                <Separator orientation="vertical" class="me-2 h-4" />
                <Breadcrumb.Root>
                    <Breadcrumb.List>
                        <Breadcrumb.Item class="hidden md:block">
                            <Breadcrumb.Link href="/"
                                >Data Quality Platform</Breadcrumb.Link
                            >
                        </Breadcrumb.Item>
                        <Breadcrumb.Separator class="hidden md:block" />
                        <Breadcrumb.Item>
                            <Breadcrumb.Page>Dashboard</Breadcrumb.Page>
                        </Breadcrumb.Item>
                    </Breadcrumb.List>
                </Breadcrumb.Root>
            </div>

            {#if user}
                <a
                    href="/profile"
                    class="flex items-center gap-2 text-xs font-medium px-3 py-1.5 rounded-lg border border-border bg-card hover:bg-accent transition-colors"
                >
                    <User class="size-3.5 text-primary" />
                    <span class="max-w-[120px] truncate">{user.fullName || user.username}</span>
                </a>
            {/if}
        </header>
        <div class="flex flex-1 flex-col overflow-hidden min-h-0">
            {@render children?.()}
        </div>
    </Sidebar.Inset>
</Sidebar.Provider>
