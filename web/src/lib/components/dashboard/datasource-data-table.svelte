<script lang="ts">
	import {
		getCoreRowModel,
		getFacetedRowModel,
		getFacetedUniqueValues,
		getFilteredRowModel,
		getPaginationRowModel,
		getSortedRowModel,
		type ColumnDef,
		type ColumnFiltersState,
		type PaginationState,
		type RowSelectionState,
		type SortingState,
		type VisibilityState
	} from "@tanstack/table-core";
	import { createSvelteTable } from "$lib/components/ui/data-table/data-table.svelte.js";
	import { FlexRender, renderComponent } from "$lib/components/ui/data-table/index.js";
	import * as Tabs from "$lib/components/ui/tabs/index.js";
	import * as Table from "$lib/components/ui/table/index.js";
	import * as DropdownMenu from "$lib/components/ui/dropdown-menu/index.js";
	import { Button } from "$lib/components/ui/button/index.js";
	import { Input } from "$lib/components/ui/input/index.js";
	import { Badge } from "$lib/components/ui/badge/index.js";
	import { Label } from "$lib/components/ui/label/index.js";
	import LayoutColumnsIcon from "@tabler/icons-svelte/icons/layout-columns";
	import ChevronDownIcon from "@tabler/icons-svelte/icons/chevron-down";
	import PlusIcon from "@tabler/icons-svelte/icons/plus";
	import ChevronsLeftIcon from "@tabler/icons-svelte/icons/chevrons-left";
	import ChevronLeftIcon from "@tabler/icons-svelte/icons/chevron-left";
	import ChevronRightIcon from "@tabler/icons-svelte/icons/chevron-right";
	import ChevronsRightIcon from "@tabler/icons-svelte/icons/chevrons-right";
	import DatasourceNameCell from "./datasource-name-cell.svelte";
	import DatasourceActionsCell from "./datasource-actions-cell.svelte";
	import type { Datasource, DatasourceStatus } from "$lib/server/api";

	let {
		datasources = [],
		onNewDatasource,
		onEditDatasource,
		onDeleteDatasource
	}: {
		datasources: Datasource[];
		onNewDatasource?: () => void;
		onEditDatasource?: (ds: Datasource) => void;
		onDeleteDatasource?: (ds: Datasource) => void;
	} = $props();

	let searchQuery = $state("");
	let typeFilter = $state("ALL");
	let activeTab = $state<string>("ALL");

	let pagination = $state<PaginationState>({ pageIndex: 0, pageSize: 10 });
	let sorting = $state<SortingState>([]);
	let columnFilters = $state<ColumnFiltersState>([]);
	let rowSelection = $state<RowSelectionState>({});
	let columnVisibility = $state<VisibilityState>({});

	// Filtered datasources array
	let filteredData = $derived(
		datasources.filter((ds) => {
			const matchesTab = activeTab === "ALL" || ds.status === activeTab;
			const matchesType = typeFilter === "ALL" || ds.type.toLowerCase() === typeFilter.toLowerCase();
			const query = searchQuery.toLowerCase().trim();
			const matchesQuery =
				!query ||
				ds.name.toLowerCase().includes(query) ||
				ds.type.toLowerCase().includes(query) ||
				(ds.description && ds.description.toLowerCase().includes(query)) ||
				ds.owner.toLowerCase().includes(query);

			return matchesTab && matchesType && matchesQuery;
		})
	);

	// Status counts
	let totalCount = $derived(datasources.length);
	let activeCount = $derived(datasources.filter((d) => d.status === "ACTIVE").length);
	let registeredCount = $derived(datasources.filter((d) => d.status === "REGISTERED").length);
	let disabledCount = $derived(datasources.filter((d) => d.status === "DISABLED").length);
	let archivedCount = $derived(datasources.filter((d) => d.status === "ARCHIVED").length);

	let availableTypes = $derived(
		Array.from(new Set(datasources.map((d) => d.type))).filter(Boolean)
	);

	function getStatusBadge(status: DatasourceStatus) {
		switch (status) {
			case "ACTIVE":
				return {
					label: "Active",
					classes: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20"
				};
			case "REGISTERED":
				return {
					label: "Registered",
					classes: "bg-sky-500/10 text-sky-600 dark:text-sky-400 border-sky-500/20"
				};
			case "DISABLED":
				return {
					label: "Disabled",
					classes: "bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20"
				};
			case "ARCHIVED":
				return {
					label: "Archived",
					classes: "bg-muted text-muted-foreground border-border"
				};
			default:
				return {
					label: status,
					classes: "bg-muted text-muted-foreground border-border"
				};
		}
	}

	const columns: ColumnDef<Datasource>[] = [
		{
			accessorKey: "name",
			header: "Name & Description",
			cell: ({ row }) => renderComponent(DatasourceNameCell, { ds: row.original }),
			enableHiding: false
		},
		{
			accessorKey: "type",
			header: "Engine Type",
			cell: ({ row }) => row.original.type
		},
		{
			accessorKey: "status",
			header: "Status",
			cell: ({ row }) => {
				const info = getStatusBadge(row.original.status);
				return `${info.label}`;
			}
		},
		{
			accessorKey: "owner",
			header: "Owner",
			cell: ({ row }) => `@${row.original.owner}`
		},
		{
			accessorKey: "registrationDate",
			header: "Registration Date",
			cell: ({ row }) =>
				row.original.registrationDate
					? new Date(row.original.registrationDate).toLocaleDateString()
					: "—"
		},
		{
			id: "actions",
			header: () => "",
			cell: ({ row }) =>
				renderComponent(DatasourceActionsCell, {
					ds: row.original,
					onEdit: onEditDatasource,
					onDelete: onDeleteDatasource
				})
		}
	];

	const table = createSvelteTable({
		get data() {
			return filteredData;
		},
		columns,
		state: {
			get pagination() {
				return pagination;
			},
			get sorting() {
				return sorting;
			},
			get columnVisibility() {
				return columnVisibility;
			},
			get rowSelection() {
				return rowSelection;
			},
			get columnFilters() {
				return columnFilters;
			}
		},
		getRowId: (row) => row.id,
		enableRowSelection: true,
		autoResetPageIndex: false,
		getCoreRowModel: getCoreRowModel(),
		getPaginationRowModel: getPaginationRowModel(),
		getSortedRowModel: getSortedRowModel(),
		getFacetedRowModel: getFacetedRowModel(),
		getFacetedUniqueValues: getFacetedUniqueValues(),
		getFilteredRowModel: getFilteredRowModel(),
		onPaginationChange: (updater) => {
			pagination = typeof updater === "function" ? updater(pagination) : updater;
		},
		onSortingChange: (updater) => {
			sorting = typeof updater === "function" ? updater(sorting) : updater;
		},
		onColumnFiltersChange: (updater) => {
			columnFilters = typeof updater === "function" ? updater(columnFilters) : updater;
		},
		onColumnVisibilityChange: (updater) => {
			columnVisibility = typeof updater === "function" ? updater(columnVisibility) : updater;
		},
		onRowSelectionChange: (updater) => {
			rowSelection = typeof updater === "function" ? updater(rowSelection) : updater;
		}
	});

	let tabs = $derived([
		{ id: "ALL", label: "All", count: totalCount },
		{ id: "ACTIVE", label: "Active", count: activeCount },
		{ id: "REGISTERED", label: "Registered", count: registeredCount },
		{ id: "DISABLED", label: "Disabled", count: disabledCount },
		{ id: "ARCHIVED", label: "Archived", count: archivedCount }
	]);
</script>

<div class="w-full space-y-6">
	<!-- Top Controls Toolbar (matching dashboard-01) -->
	<Tabs.Root value={activeTab} onValueChange={(v) => (activeTab = v)} class="w-full flex-col justify-start gap-4">
		<div class="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
			<Tabs.List class="flex items-center gap-1 bg-muted p-1 rounded-lg">
				{#each tabs as tab (tab.id)}
					<Tabs.Trigger value={tab.id} class="px-3.5 py-1.5 text-xs font-medium cursor-pointer">
						{tab.label}
						<Badge variant="secondary" class="ms-1.5 text-[10px] px-1.5 py-0.2">{tab.count}</Badge>
					</Tabs.Trigger>
				{/each}
			</Tabs.List>

			<div class="flex items-center gap-2">
				<!-- Search Filter Input -->
				<Input
					type="text"
					placeholder="Filter datasources..."
					bind:value={searchQuery}
					class="h-10 text-sm sm:w-64"
				/>

				{#if availableTypes.length > 0}
					<select
						bind:value={typeFilter}
						class="h-10 px-3 text-sm rounded-md border border-input bg-background text-foreground cursor-pointer focus:outline-none focus:ring-2 focus:ring-ring"
					>
						<option value="ALL">All Engines</option>
						{#each availableTypes as type}
							<option value={type}>{type}</option>
						{/each}
					</select>
				{/if}

				<!-- Customize Columns Dropdown (matching dashboard-01) -->
				<DropdownMenu.Root>
					<DropdownMenu.Trigger>
						{#snippet child({ props })}
							<Button variant="outline" size="sm" {...props}>
								<LayoutColumnsIcon class="size-4" />
								<span class="hidden lg:inline">Customize Columns</span>
								<ChevronDownIcon class="size-4" />
							</Button>
						{/snippet}
					</DropdownMenu.Trigger>
					<DropdownMenu.Content align="end" class="w-56">
						{#each table
							.getAllColumns()
							.filter((col) => typeof col.accessorFn !== "undefined" && col.getCanHide()) as column (column.id)}
							<DropdownMenu.CheckboxItem
								class="capitalize"
								checked={column.getIsVisible()}
								onCheckedChange={(value) => column.toggleVisibility(!!value)}
							>
								{column.id}
							</DropdownMenu.CheckboxItem>
						{/each}
					</DropdownMenu.Content>
				</DropdownMenu.Root>

				<!-- Primary Action Button -->
				{#if onNewDatasource}
					<Button onclick={onNewDatasource}>
						<PlusIcon class="size-4" />
						<span class="hidden lg:inline">New Datasource</span>
					</Button>
				{/if}
			</div>
		</div>

		<!-- Table Container -->
		<div class="overflow-hidden rounded-lg border border-border bg-card shadow-xs mt-4">
			<Table.Root>
				<Table.Header class="bg-muted/40 sticky top-0 z-10">
					{#each table.getHeaderGroups() as headerGroup (headerGroup.id)}
						<Table.Row>
							{#each headerGroup.headers as header (header.id)}
								<Table.Head class="py-3.5 px-6 font-mono text-xs text-muted-foreground font-semibold uppercase">
									{#if !header.isPlaceholder}
										<FlexRender
											content={header.column.columnDef.header}
											context={header.getContext()}
										/>
									{/if}
								</Table.Head>
							{/each}
						</Table.Row>
					{/each}
				</Table.Header>
				<Table.Body>
					{#if table.getRowModel().rows?.length}
						{#each table.getRowModel().rows as row (row.id)}
							<Table.Row class="hover:bg-muted/50 transition-colors">
								{#each row.getVisibleCells() as cell (cell.id)}
									<Table.Cell class="py-4 px-6">
										<FlexRender
											content={cell.column.columnDef.cell}
											context={cell.getContext()}
										/>
									</Table.Cell>
								{/each}
							</Table.Row>
						{/each}
					{:else}
						<Table.Row>
							<Table.Cell colspan={columns.length} class="h-24 text-center text-sm text-muted-foreground">
								No datasources found matching current filters.
							</Table.Cell>
						</Table.Row>
					{/if}
				</Table.Body>
			</Table.Root>
		</div>

		<!-- Pagination Footer Bar (matching dashboard-01) -->
		<div class="flex items-center justify-between px-2 pt-4">
			<div class="text-muted-foreground hidden flex-1 text-sm lg:flex">
				{table.getFilteredSelectedRowModel().rows.length} of
				{table.getFilteredRowModel().rows.length} row(s) selected.
			</div>
			<div class="flex w-full items-center gap-8 lg:w-fit">
				<div class="hidden items-center gap-2 lg:flex">
					<Label for="rows-per-page" class="text-sm font-medium">Rows per page</Label>
					<select
						id="rows-per-page"
						value={table.getState().pagination.pageSize}
						onchange={(e) => table.setPageSize(Number((e.target as HTMLSelectElement).value))}
						class="h-9 px-2 text-xs rounded-md border border-input bg-background text-foreground"
					>
						{#each [10, 20, 30, 50] as pageSize (pageSize)}
							<option value={pageSize}>{pageSize}</option>
						{/each}
					</select>
				</div>
				<div class="flex w-fit items-center justify-center text-sm font-medium">
					Page {table.getState().pagination.pageIndex + 1} of
					{table.getPageCount() || 1}
				</div>
				<div class="ms-auto flex items-center gap-2 lg:ms-0">
					<Button
						variant="outline"
						size="icon"
						onclick={() => table.setPageIndex(0)}
						disabled={!table.getCanPreviousPage()}
					>
						<ChevronsLeftIcon class="size-4" />
					</Button>
					<Button
						variant="outline"
						size="icon"
						onclick={() => table.previousPage()}
						disabled={!table.getCanPreviousPage()}
					>
						<ChevronLeftIcon class="size-4" />
					</Button>
					<Button
						variant="outline"
						size="icon"
						onclick={() => table.nextPage()}
						disabled={!table.getCanNextPage()}
					>
						<ChevronRightIcon class="size-4" />
					</Button>
					<Button
						variant="outline"
						size="icon"
						onclick={() => table.setPageIndex(table.getPageCount() - 1)}
						disabled={!table.getCanNextPage()}
					>
						<ChevronsRightIcon class="size-4" />
					</Button>
				</div>
			</div>
		</div>
	</Tabs.Root>
</div>
