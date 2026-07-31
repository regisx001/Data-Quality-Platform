<script lang="ts">
	import { enhance } from "$app/forms";
	import { Button } from "$lib/components/ui/button/index.js";
	import * as Card from "$lib/components/ui/card/index.js";
	import * as Field from "$lib/components/ui/field/index.js";
	import { Input } from "$lib/components/ui/input/index.js";
	import * as Tabs from "$lib/components/ui/tabs/index.js";
	import Loader2 from "@lucide/svelte/icons/loader-2";
	import User from "@lucide/svelte/icons/user";
	import ShieldCheck from "@lucide/svelte/icons/shield-check";
	import SettingsIcon from "@lucide/svelte/icons/settings";
	import Palette from "@lucide/svelte/icons/palette";
	import Sun from "@lucide/svelte/icons/sun";
	import Moon from "@lucide/svelte/icons/moon";
	import Monitor from "@lucide/svelte/icons/monitor";
	import Key from "@lucide/svelte/icons/key";
	import CheckCircle from "@lucide/svelte/icons/check-circle";
	import AlertCircle from "@lucide/svelte/icons/alert-circle";
	import Edit from "@lucide/svelte/icons/edit";
	import { mode, setMode } from "mode-watcher";
	import type { PageData, ActionData } from "./$types";

	let { data, form }: { data: PageData; form: ActionData } = $props();

	let currentUser = $derived(form?.user || data.user);
	let isSubmitting = $state(false);
</script>

<svelte:head>
	<title>Settings | Data Quality Platform</title>
	<meta
		name="description"
		content="Manage profile, account settings, authentication, and appearance preferences."
	/>
</svelte:head>

<div class="p-6 sm:p-8 w-full space-y-6">
	<!-- Header -->
	<div class="flex items-center justify-between border-b pb-6">
		<div class="flex items-center gap-4">
			<div
				class="size-12 rounded-full bg-muted flex items-center justify-center text-foreground font-bold text-lg shrink-0"
			>
				{currentUser.fullName
					? currentUser.fullName.charAt(0).toUpperCase()
					: currentUser.username?.charAt(0).toUpperCase()}
			</div>
			<div>
				<h1 class="text-xl font-bold tracking-tight">
					{currentUser.fullName || currentUser.username}
					<span class="text-muted-foreground font-normal text-sm"
						>({currentUser.username})</span
					>
				</h1>
				<p class="text-xs text-muted-foreground mt-0.5">
					Personal account • <span
						class="font-mono text-foreground font-medium"
						>{currentUser.role || "USER"}</span
					>
				</p>
			</div>
		</div>
	</div>

	<!-- Alert Messages -->
	{#if form?.success && form?.message}
		<div
			class="flex items-center gap-2 p-3 text-xs text-emerald-600 dark:text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 rounded-md"
		>
			<CheckCircle class="size-4 shrink-0" />
			<span>{form.message}</span>
		</div>
	{/if}

	{#if form?.error}
		<div
			class="flex items-center gap-2 p-3 text-xs text-destructive bg-destructive/10 border border-destructive/20 rounded-md"
		>
			<AlertCircle class="size-4 shrink-0" />
			<span>{form.error}</span>
		</div>
	{/if}

	<!-- Standard Shadcn Tabs Component (Vertical Layout) -->
	<Tabs.Root
		value="profile"
		class="flex flex-col md:flex-row gap-8 items-start"
	>
		<Tabs.List
			class="flex flex-col h-auto w-full md:w-64 bg-transparent p-0 gap-1"
		>
			<Tabs.Trigger
				value="profile"
				class="justify-start gap-2.5 px-3.5 py-2.5 text-sm w-full cursor-pointer data-[state=active]:bg-muted data-[state=active]:font-semibold text-muted-foreground hover:text-foreground"
			>
				<User class="size-4" />
				<span>Public profile</span>
			</Tabs.Trigger>

			<Tabs.Trigger
				value="account"
				class="justify-start gap-2.5 px-3.5 py-2.5 text-sm w-full cursor-pointer data-[state=active]:bg-muted data-[state=active]:font-semibold text-muted-foreground hover:text-foreground"
			>
				<SettingsIcon class="size-4" />
				<span>Account</span>
			</Tabs.Trigger>

			<Tabs.Trigger
				value="appearance"
				class="justify-start gap-2.5 px-3.5 py-2.5 text-sm w-full cursor-pointer data-[state=active]:bg-muted data-[state=active]:font-semibold text-muted-foreground hover:text-foreground"
			>
				<Palette class="size-4" />
				<span>Appearance</span>
			</Tabs.Trigger>

			<div class="pt-3 pb-1">
				<p
					class="px-3 text-xs font-mono uppercase tracking-wider text-muted-foreground font-semibold"
				>
					Access & Security
				</p>
			</div>

			<Tabs.Trigger
				value="security"
				class="justify-start gap-2.5 px-3.5 py-2.5 text-sm w-full cursor-pointer data-[state=active]:bg-muted data-[state=active]:font-semibold text-muted-foreground hover:text-foreground"
			>
				<ShieldCheck class="size-4" />
				<span>Password and authentication</span>
			</Tabs.Trigger>

			<Tabs.Trigger
				value="sessions"
				class="justify-start gap-2.5 px-3.5 py-2.5 text-sm w-full cursor-pointer data-[state=active]:bg-muted data-[state=active]:font-semibold text-muted-foreground hover:text-foreground"
			>
				<Key class="size-4" />
				<span>JWT Sessions</span>
			</Tabs.Trigger>
		</Tabs.List>

		<div class="flex-1 w-full space-y-6">
			<!-- TAB 1: Public Profile -->
			<Tabs.Content value="profile" class="m-0 space-y-6">
				<div class="border-b pb-3">
					<h2 class="text-lg font-bold tracking-tight">
						Public profile
					</h2>
				</div>

				<div class="grid grid-cols-1 md:grid-cols-3 gap-8 items-start">
					<div class="md:col-span-2 space-y-4 text-xs">
						<form
							action="?/updateProfile"
							method="POST"
							use:enhance={() => {
								isSubmitting = true;
								return async ({ update }) => {
									isSubmitting = false;
									await update();
								};
							}}
							class="space-y-4"
						>
							<Field.Field>
								<Field.Label for="fullName"
									>Full Name</Field.Label
								>
								<Input
									id="fullName"
									name="fullName"
									type="text"
									value={currentUser.fullName || ""}
									placeholder="e.g. Ezzoubair ZARQI"
									disabled={isSubmitting}
								/>
								<p
									class="text-[11px] text-muted-foreground mt-1"
								>
									Your name may appear around the platform
									where you contribute or manage datasources.
								</p>
							</Field.Field>

							<Field.Field>
								<Field.Label for="email"
									>Public email</Field.Label
								>
								<Input
									id="email"
									name="email"
									type="email"
									value={currentUser.email || ""}
									placeholder="Select or enter email"
									disabled={isSubmitting}
								/>
								<p
									class="text-[11px] text-muted-foreground mt-1"
								>
									Email address associated with your user
									session notifications.
								</p>
							</Field.Field>

							<Field.Field>
								<Field.Label for="bio">Bio</Field.Label>
								<textarea
									id="bio"
									name="bio"
									rows="3"
									placeholder="Add a bio to your profile..."
									class="w-full p-2.5 rounded-md border bg-background text-foreground text-xs focus:ring-1 focus:ring-ring resize-none"
								></textarea>
							</Field.Field>

							<div class="pt-2">
								<Button
									type="submit"
									disabled={isSubmitting}
									size="sm"
								>
									{#if isSubmitting}
										<Loader2
											class="size-3.5 me-1.5 animate-spin"
										/>
										<span>Updating profile...</span>
									{:else}
										<span>Update profile</span>
									{/if}
								</Button>
							</div>
						</form>
					</div>

					<div class="space-y-3 text-xs">
						<p class="font-medium text-foreground">
							Profile picture
						</p>
						<div
							class="relative size-32 rounded-full bg-muted flex items-center justify-center text-foreground font-bold text-2xl mx-auto md:mx-0 overflow-hidden"
						>
							<span
								>{currentUser.fullName
									? currentUser.fullName
											.charAt(0)
											.toUpperCase()
									: currentUser.username
											?.charAt(0)
											.toUpperCase()}</span
							>
							<button
								type="button"
								class="absolute bottom-1 bg-background/90 px-2 py-0.5 rounded-full text-[10px] font-medium border flex items-center gap-1 hover:bg-muted transition-colors cursor-pointer"
							>
								<Edit class="size-3" />
								<span>Edit</span>
							</button>
						</div>
					</div>
				</div>
			</Tabs.Content>

			<!-- TAB 2: Account -->
			<Tabs.Content value="account" class="m-0 space-y-6">
				<div class="border-b pb-3">
					<h2 class="text-lg font-bold tracking-tight">
						Account settings
					</h2>
				</div>

				<Card.Root class="max-w-2xl">
					<Card.Content class="p-5 space-y-4 text-xs">
						<div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
							<div
								class="p-3 rounded-md bg-muted/40 border space-y-1"
							>
								<span class="text-muted-foreground"
									>Username</span
								>
								<p class="font-medium text-sm text-foreground">
									@{currentUser.username}
								</p>
							</div>

							<div
								class="p-3 rounded-md bg-muted/40 border space-y-1"
							>
								<span class="text-muted-foreground"
									>User Role</span
								>
								<p
									class="font-mono text-sm font-semibold text-foreground"
								>
									{currentUser.role || "USER"}
								</p>
							</div>
						</div>
					</Card.Content>
				</Card.Root>
			</Tabs.Content>

			<!-- TAB 3: Appearance -->
			<Tabs.Content value="appearance" class="m-0 space-y-6">
				<div class="border-b pb-3">
					<h2 class="text-lg font-bold tracking-tight">Appearance</h2>
				</div>

				<Card.Root class="max-w-2xl">
					<Card.Content class="p-5 space-y-4 text-xs">
						<div>
							<p class="font-medium text-foreground text-sm">
								Interface Theme
							</p>
							<p class="text-muted-foreground mt-1">
								Select your preferred color scheme for the
								platform.
							</p>
						</div>

						<div class="grid grid-cols-3 gap-3">
							<button
								type="button"
								class="flex flex-col items-center gap-2 p-4 rounded-lg border-2 transition-all cursor-pointer
								{mode.current === 'light'
									? 'border-primary bg-primary/5'
									: 'border-border hover:border-muted-foreground/30 hover:bg-accent/50'}"
								onclick={() => setMode("light")}
							>
								<div
									class="size-10 rounded-full bg-white border-2 border-gray-300 flex items-center justify-center shadow-sm"
								>
									<Sun class="size-5 text-amber-500" />
								</div>
								<span class="font-medium text-xs">Light</span>
							</button>

							<button
								type="button"
								class="flex flex-col items-center gap-2 p-4 rounded-lg border-2 transition-all cursor-pointer
								{mode.current === 'dark'
									? 'border-primary bg-primary/5'
									: 'border-border hover:border-muted-foreground/30 hover:bg-accent/50'}"
								onclick={() => setMode("dark")}
							>
								<div
									class="size-10 rounded-full bg-gray-900 border-2 border-gray-600 flex items-center justify-center shadow-sm"
								>
									<Moon class="size-5 text-blue-300" />
								</div>
								<span class="font-medium text-xs">Dark</span>
							</button>

							<button
								type="button"
								class="flex flex-col items-center gap-2 p-4 rounded-lg border-2 transition-all cursor-pointer
								{mode.current === 'system'
									? 'border-primary bg-primary/5'
									: 'border-border hover:border-muted-foreground/30 hover:bg-accent/50'}"
								onclick={() => setMode("system")}
							>
								<div
									class="size-10 rounded-full bg-linear-to-br from-white to-gray-900 border-2 border-gray-400 flex items-center justify-center shadow-sm"
								>
									<Monitor class="size-5 text-foreground" />
								</div>
								<span class="font-medium text-xs">System</span>
							</button>
						</div>

						<p class="text-[11px] text-muted-foreground pt-1">
							Current mode: <span
								class="font-semibold text-foreground capitalize"
								>{mode.current}</span
							>
						</p>
					</Card.Content>
				</Card.Root>
			</Tabs.Content>

			<!-- TAB 4 & 5: Security & Sessions -->
			<Tabs.Content value="security" class="m-0 space-y-6">
				<div class="border-b pb-3">
					<h2 class="text-lg font-bold tracking-tight">
						Password and authentication
					</h2>
				</div>

				<Card.Root class="max-w-2xl">
					<Card.Content class="p-5 space-y-4 text-xs">
						<div class="space-y-2">
							<span class="text-muted-foreground"
								>JWT Session Bearer Token</span
							>
							<div
								class="p-3 rounded-md bg-muted/40 border font-mono text-xs text-foreground"
							>
								Valid Bearer Token Authenticated
								(http://localhost:7000)
							</div>
						</div>
					</Card.Content>
				</Card.Root>
			</Tabs.Content>

			<Tabs.Content value="sessions" class="m-0 space-y-6">
				<div class="border-b pb-3">
					<h2 class="text-lg font-bold tracking-tight">
						Active JWT Sessions
					</h2>
				</div>

				<Card.Root class="max-w-2xl">
					<Card.Content class="p-5 space-y-4 text-xs">
						<div class="space-y-2">
							<span class="text-muted-foreground"
								>Current Active Session</span
							>
							<div
								class="p-3 rounded-md bg-muted/40 border font-mono text-xs text-foreground"
							>
								Authenticated User Session: @{currentUser.username}
							</div>
						</div>
					</Card.Content>
				</Card.Root>
			</Tabs.Content>
		</div>
	</Tabs.Root>
</div>
