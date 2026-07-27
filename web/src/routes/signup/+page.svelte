<script lang="ts">
	import SignupForm from "$lib/components/signup/signup-form.svelte";
	import type { ActionData } from "./$types";

	let { form }: { form: ActionData } = $props();
</script>

<svelte:head>
	<title>Sign Up | Data Quality Platform</title>
	<meta name="description" content="Create your account to get started." />
</svelte:head>

<main class="h-svh w-full overflow-hidden bg-background">
	<SignupForm form={form} class="h-full w-full" />
</main>
