<script lang="ts">
	import LoginForm from "$lib/components/login/login-form.svelte";
	import type { ActionData } from "./$types";

	let { form }: { form: ActionData } = $props();
</script>

<svelte:head>
	<title>Sign In | Data Quality Platform</title>
	<meta name="description" content="Sign in to your Enterprise Data Quality Platform account." />
</svelte:head>

<main class="h-svh w-full overflow-hidden bg-background">
	<LoginForm form={form} class="h-full w-full" />
</main>
