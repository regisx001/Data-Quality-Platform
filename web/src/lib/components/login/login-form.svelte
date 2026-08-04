<script lang="ts">
	import { cn } from "$lib/utils.js";
	import { Button } from "$lib/components/ui/button/index.js";
	import * as Field from "$lib/components/ui/field/index.js";
	import { Input } from "$lib/components/ui/input/index.js";
	import ShieldCheck from "@lucide/svelte/icons/shield-check";
	import AlertCircle from "@lucide/svelte/icons/alert-circle";
	import Loader2 from "@lucide/svelte/icons/loader-2";
	import Logo from "$lib/components/ui/logo/logo.svelte";
	import ErrorAlert from "$lib/components/ui/error-alert.svelte";
	import { enhance } from "$app/forms";
	import type { HTMLAttributes } from "svelte/elements";

	let {
		form,
		class: className,
		...restProps
	}: HTMLAttributes<HTMLDivElement> & {
		form?: { error?: any; login?: string } | null;
	} = $props();

	let login = $state("");
	let password = $state("");
	let rememberMe = $state(false);
	let isSubmitting = $state(false);

	$effect(() => {
		if (form?.login) {
			login = form.login;
		}
	});
</script>

<div
	class={cn(
		"grid h-full w-full md:grid-cols-2 lg:grid-cols-[1.1fr_1fr] bg-background overflow-hidden relative",
		className,
	)}
	{...restProps}
>
	<!-- Top Loading Bar Indicator -->
	{#if isSubmitting}
		<div
			class="absolute top-0 left-0 right-0 z-50 h-1 bg-primary/20 overflow-hidden"
		>
			<div class="h-full bg-primary animate-pulse w-full"></div>
		</div>
	{/if}

	<!-- Left Side: Architectural Artwork Panel -->
	<div
		class="bg-black relative hidden md:block h-full w-full border-r border-border overflow-hidden order-1"
	>
		<img
			src="/signup-dashboard-hero.jpg"
			alt="Data Quality Platform Architectural Artwork"
			class="absolute inset-0 h-full w-full object-cover object-center"
			style="view-transition-name: auth-hero-artwork;"
		/>
	</div>

	<!-- Right Side: Full-Height Auth Login Panel -->
	<div
		class="flex flex-col justify-between h-full p-6 sm:p-10 md:p-12 lg:p-16 overflow-y-auto order-2 relative"
	>
		<!-- Top: Brand Logo -->
		<div class="flex items-center gap-3 py-2">
			<Logo class="h-18 md:h-24 w-56 md:w-72 object-contain" />
		</div>

		<!-- Center: Form Container -->
		<div class="w-full max-w-md mx-auto my-auto py-8">
			<form
				method="POST"
				use:enhance={() => {
					isSubmitting = true;
					return async ({ update }) => {
						isSubmitting = false;
						await update();
					};
				}}
			>
				<Field.Group class="gap-5">
					<div class="flex flex-col gap-1.5 mb-2">
						<h1
							class="text-2xl sm:text-3xl font-bold tracking-tight"
						>
							Welcome back
						</h1>
						<p class="text-muted-foreground text-sm">
							Enter your credentials below to sign in to your
							account
						</p>
					</div>

					{#if form?.error}
						<ErrorAlert error={form.error} title="Authentication Failed" dismissable={false} />
					{/if}

					<!-- Username or Email Field -->
					<Field.Field>
						<Field.Label for="login">Username or Email</Field.Label>
						<Input
							id="login"
							name="login"
							type="text"
							bind:value={login}
							placeholder="username or user@example.com"
							required
							disabled={isSubmitting}
							class="h-10 rounded-xl"
						/>
					</Field.Field>

					<!-- Password Field with Forgot Password Link -->
					<Field.Field>
						<div class="flex items-center justify-between">
							<Field.Label for="password">Password</Field.Label>
							<a
								href="#/"
								class="text-xs text-muted-foreground hover:text-foreground underline underline-offset-4"
							>
								Forgot password?
							</a>
						</div>
						<Input
							id="password"
							name="password"
							type="password"
							bind:value={password}
							required
							disabled={isSubmitting}
							class="h-10 rounded-xl"
						/>
					</Field.Field>

					<!-- Remember Me Checkbox -->
					<div class="flex items-center gap-2">
						<input
							type="checkbox"
							id="remember"
							name="remember"
							bind:checked={rememberMe}
							disabled={isSubmitting}
							class="size-4 rounded border-border text-primary focus:ring-primary cursor-pointer disabled:opacity-50"
						/>
						<label
							for="remember"
							class="text-xs text-muted-foreground cursor-pointer select-none"
						>
							Remember me on this device
						</label>
					</div>

					<!-- Submit Button -->
					<Field.Field>
						<Button
							type="submit"
							disabled={isSubmitting}
							class="w-full h-10 rounded-xl font-medium cursor-pointer flex items-center justify-center"
						>
							{#if isSubmitting}
								<Loader2 class="size-4 me-2 animate-spin" />
								<span>Authenticating...</span>
							{:else}
								<span>Sign In</span>
							{/if}
						</Button>
					</Field.Field>

					<!-- Separator -->
					<Field.Separator
						class="*:data-[slot=field-separator-content]:bg-background"
					>
						Or continue with
					</Field.Separator>

					<!-- Social Sign-in Buttons -->
					<Field.Field class="grid grid-cols-3 gap-3">
						<Button
							variant="outline"
							type="button"
							disabled={isSubmitting}
							class="h-10 rounded-xl cursor-pointer"
						>
							<svg
								class="size-4"
								xmlns="http://www.w3.org/2000/svg"
								viewBox="0 0 24 24"
							>
								<path
									d="M12.152 6.896c-.948 0-2.415-1.078-3.96-1.04-2.04.027-3.91 1.183-4.961 3.014-2.117 3.675-.546 9.103 1.519 12.09 1.013 1.454 2.208 3.09 3.792 3.039 1.52-.065 2.09-.987 3.935-.987 1.831 0 2.35.987 3.96.948 1.637-.026 2.676-1.48 3.676-2.948 1.156-1.688 1.636-3.325 1.662-3.415-.039-.013-3.182-1.221-3.22-4.857-.026-3.04 2.48-4.494 2.597-4.559-1.429-2.09-3.623-2.324-4.39-2.376-2-.156-3.675 1.09-4.61 1.09zM15.53 3.83c.843-1.012 1.4-2.427 1.245-3.83-1.207.052-2.662.805-3.532 1.818-.78.896-1.454 2.338-1.273 3.714 1.338.104 2.715-.688 3.559-1.701"
									fill="currentColor"
								/>
							</svg>
							<span class="sr-only">Sign in with Apple</span>
						</Button>
						<Button
							variant="outline"
							type="button"
							disabled={isSubmitting}
							class="h-10 rounded-xl cursor-pointer"
						>
							<svg
								class="size-4"
								xmlns="http://www.w3.org/2000/svg"
								viewBox="0 0 24 24"
							>
								<path
									d="M12.48 10.92v3.28h7.84c-.24 1.84-.853 3.187-1.787 4.133-1.147 1.147-2.933 2.4-6.053 2.4-4.827 0-8.6-3.893-8.6-8.72s3.773-8.72 8.6-8.72c2.6 0 4.507 1.027 5.907 2.347l2.307-2.307C18.747 1.44 16.133 0 12.48 0 5.867 0 .307 5.387.307 12s5.56 12 12.173 12c3.573 0 6.267-1.173 8.373-3.36 2.16-2.16 2.84-5.213 2.84-7.667 0-.76-.053-1.467-.173-2.053H12.48z"
									fill="currentColor"
								/>
							</svg>
							<span class="sr-only">Sign in with Google</span>
						</Button>
						<Button
							variant="outline"
							type="button"
							disabled={isSubmitting}
							class="h-10 rounded-xl cursor-pointer"
						>
							<svg
								class="size-4"
								xmlns="http://www.w3.org/2000/svg"
								viewBox="0 0 24 24"
							>
								<path
									d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"
									fill="currentColor"
								/>
							</svg>
							<span class="sr-only">Sign in with GitHub</span>
						</Button>
					</Field.Field>

					<!-- Sign Up Link -->
					<Field.Description class="text-center pt-2">
						Don't have an account? <a
							href="/signup"
							class="text-foreground underline underline-offset-4 hover:text-primary font-medium"
							>Sign up</a
						>
					</Field.Description>
				</Field.Group>
			</form>
		</div>

		<!-- Bottom: Footer Terms Policy -->
		<Field.Description class="text-center text-xs text-muted-foreground">
			By clicking continue, you agree to our <a
				href="#/"
				class="underline underline-offset-4 hover:text-foreground"
				>Terms of Service</a
			>
			and
			<a
				href="#/"
				class="underline underline-offset-4 hover:text-foreground"
				>Privacy Policy</a
			>.
		</Field.Description>
	</div>
</div>
