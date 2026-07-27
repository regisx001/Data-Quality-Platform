<script lang="ts">
	import { enhance } from "$app/forms";
	import { Button } from "$lib/components/ui/button/index.js";
	import * as Field from "$lib/components/ui/field/index.js";
	import { Input } from "$lib/components/ui/input/index.js";
	import ShieldCheck from "@lucide/svelte/icons/shield-check";
	import User from "@lucide/svelte/icons/user";
	import Mail from "@lucide/svelte/icons/mail";
	import BadgeCheck from "@lucide/svelte/icons/badge-check";
	import LogOut from "@lucide/svelte/icons/log-out";
	import ArrowLeft from "@lucide/svelte/icons/arrow-left";
	import CheckCircle2 from "@lucide/svelte/icons/check-circle-2";
	import AlertCircle from "@lucide/svelte/icons/alert-circle";
	import type { PageData, ActionData } from "./$types";

	let { data, form }: { data: PageData; form: ActionData } = $props();

	let user = $derived(form?.user ?? data.user);
	let fullName = $state("");
	let email = $state("");
	let isSubmitting = $state(false);

	$effect(() => {
		if (user) {
			fullName = user.fullName ?? "";
			email = user.email ?? "";
		}
	});
</script>

<svelte:head>
	<title>User Profile | Data Quality Platform</title>
	<meta name="description" content="Manage your Data Quality Platform account profile and settings." />
</svelte:head>

<div class="min-h-screen bg-background text-foreground flex flex-col">
	<!-- Top Navigation Bar -->
	<header class="border-b border-border bg-card/50 backdrop-blur-md sticky top-0 z-20">
		<div class="max-w-5xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
			<div class="flex items-center gap-3">
				<a
					href="/"
					class="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors py-1.5 px-3 rounded-lg hover:bg-accent"
				>
					<ArrowLeft class="size-4" />
					<span>Back to App</span>
				</a>
			</div>
			<div class="flex items-center gap-2.5">
				<div class="size-8 rounded-lg bg-primary/10 border border-primary/20 flex items-center justify-center text-primary">
					<ShieldCheck class="size-4.5" />
				</div>
				<span class="font-bold tracking-tight">DataQuality</span>
			</div>
		</div>
	</header>

	<!-- Main Profile Container -->
	<main class="flex-1 max-w-5xl w-full mx-auto px-4 sm:px-6 py-8 sm:py-12 space-y-8">
		<!-- Header Banner Card -->
		<div class="bg-card border border-border rounded-2xl p-6 sm:p-8 shadow-xs relative overflow-hidden">
			<div class="absolute -right-16 -top-16 size-64 bg-primary/5 rounded-full blur-3xl pointer-events-none"></div>
			
			<div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-6 relative z-10">
				<div class="flex items-center gap-4 sm:gap-6">
					<!-- Avatar Icon -->
					<div class="size-16 sm:size-20 rounded-2xl bg-gradient-to-br from-primary/20 via-primary/10 to-primary/5 border border-primary/20 flex items-center justify-center text-primary text-2xl font-bold shadow-xs">
						{user?.fullName ? user.fullName.charAt(0).toUpperCase() : user?.username?.charAt(0).toUpperCase() || "U"}
					</div>

					<div class="space-y-1">
						<div class="flex items-center gap-2.5 flex-wrap">
							<h1 class="text-2xl sm:text-3xl font-bold tracking-tight">{user?.fullName || user?.username}</h1>
							<span class="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-0.5 rounded-full bg-primary/10 text-primary border border-primary/20">
								<BadgeCheck class="size-3.5" />
								{user?.role || "USER"}
							</span>
						</div>
						<p class="text-sm text-muted-foreground font-mono">@{user?.username}</p>
					</div>
				</div>

				<!-- Logout Form Action -->
				<form action="/logout" method="POST" use:enhance>
					<Button variant="outline" type="submit" class="rounded-xl border-destructive/30 text-destructive hover:bg-destructive/10 hover:border-destructive/50 transition-colors cursor-pointer">
						<LogOut class="size-4 me-2" />
						Sign Out
					</Button>
				</form>
			</div>
		</div>

		<!-- Action Feedback Alerts -->
		{#if form?.success && form?.message}
			<div class="flex items-center gap-2.5 p-4 text-sm text-emerald-500 bg-emerald-500/10 border border-emerald-500/20 rounded-xl">
				<CheckCircle2 class="size-5 shrink-0" />
				<span class="font-medium">{form.message}</span>
			</div>
		{/if}

		{#if form?.error}
			<div class="flex items-center gap-2.5 p-4 text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-xl">
				<AlertCircle class="size-5 shrink-0" />
				<span class="font-medium">{form.error}</span>
			</div>
		{/if}

		<!-- Grid Section: Account Info & Profile Edit -->
		<div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
			<!-- Account Metadata Sidebar Card -->
			<div class="bg-card border border-border rounded-2xl p-6 space-y-6">
				<h2 class="text-lg font-semibold tracking-tight border-b border-border pb-3">Account Details</h2>

				<div class="space-y-4 text-sm">
					<div>
						<span class="text-xs text-muted-foreground uppercase font-mono tracking-wider">User ID</span>
						<p class="font-mono text-xs text-foreground/80 break-all mt-0.5 bg-accent/50 p-2 rounded-lg border border-border/50">
							{user?.userId}
						</p>
					</div>

					<div>
						<span class="text-xs text-muted-foreground uppercase font-mono tracking-wider">Username</span>
						<p class="font-medium text-foreground mt-0.5">{user?.username}</p>
					</div>

					<div>
						<span class="text-xs text-muted-foreground uppercase font-mono tracking-wider">Role</span>
						<p class="font-medium text-foreground mt-0.5">{user?.role}</p>
					</div>

					{#if user?.lastLoginAt}
						<div>
							<span class="text-xs text-muted-foreground uppercase font-mono tracking-wider">Last Login</span>
							<p class="font-medium text-foreground mt-0.5">{new Date(user.lastLoginAt).toLocaleString()}</p>
						</div>
					{/if}
				</div>
			</div>

			<!-- Update Profile Form Card -->
			<div class="lg:col-span-2 bg-card border border-border rounded-2xl p-6 sm:p-8 space-y-6">
				<div class="space-y-1">
					<h2 class="text-xl font-bold tracking-tight">Personal Information</h2>
					<p class="text-sm text-muted-foreground">
						Update your account display name and email address.
					</p>
				</div>

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
					class="space-y-5"
				>
					<!-- Full Name Field -->
					<Field.Field>
						<Field.Label for="fullName" class="flex items-center gap-1.5">
							<User class="size-4 text-muted-foreground" />
							<span>Full Name</span>
						</Field.Label>
						<Input
							id="fullName"
							name="fullName"
							type="text"
							bind:value={fullName}
							required
							class="h-10 rounded-xl max-w-lg"
						/>
					</Field.Field>

					<!-- Email Address Field -->
					<Field.Field>
						<Field.Label for="email" class="flex items-center gap-1.5">
							<Mail class="size-4 text-muted-foreground" />
							<span>Email Address</span>
						</Field.Label>
						<Input
							id="email"
							name="email"
							type="email"
							bind:value={email}
							required
							class="h-10 rounded-xl max-w-lg"
						/>
					</Field.Field>

					<div class="pt-2">
						<Button type="submit" disabled={isSubmitting} class="h-10 px-6 rounded-xl font-medium cursor-pointer">
							{#if isSubmitting}
								Saving Changes...
							{:else}
								Save Changes
							{/if}
						</Button>
					</div>
				</form>
			</div>
		</div>
	</main>
</div>
