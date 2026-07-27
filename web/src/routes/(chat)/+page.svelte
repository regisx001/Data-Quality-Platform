<script lang="ts">
    import * as PromptInput from "$lib/components/ai-elements/prompt-input";
    import * as Select from "$lib/components/ui/select/index.js";
    import ChatContainer from "$lib/components/chat/chat-container.svelte";
    import PromptCheckpoints, {
        type CheckpointItem,
    } from "$lib/components/chat/prompt-checkpoints.svelte";
    import * as Conversation from "$lib/components/ai-elements/conversation";
    import * as Message from "$lib/components/ai-elements/message";
    import MessageSquare from "@lucide/svelte/icons/message-square";
    import { cn } from "$lib/utils";

    // OpenRouter models selector configuration
    let models = [
        {
            name: "arcee-ai/trinity-large-preview:free",
            label: "Arcee AI: Trinity Large Preview",
        },
        {
            name: "deepseek/deepseek-v4-flash:free",
            label: "DeepSeek: DeepSeek V4 Flash",
        },
        {
            name: "openrouter/elephant-alpha",
            label: "OpenRouter: Elephant Alpha",
        },
    ];

    let selectedModel = $state("deepseek/deepseek-v4-flash:free");

    function handleSubmit() {}
    let handleStop = () => {};

    interface AttachmentItem {
        type: "file";
        url?: string;
        mediaType: string;
        filename: string;
    }

    interface MessageData {
        key: string;
        value: string;
        name: string;
        attachments?: AttachmentItem[];
    }

    interface ConversationTurn {
        id: string;
        key: string;
        userPrompt: string;
        userAttachments?: AttachmentItem[];
        assistantResponse?: string;
        turnNumber: number;
    }

    // Rich mockup conversation data
    const rawMessages: MessageData[] = [
        {
            key: "msg-1",
            value: "How can we optimize multiple data quality validation rules into a unified PySpark execution plan?",
            name: "Alex Johnson",
            attachments: [
                {
                    type: "file",
                    url: "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=800&h=500&fit=crop",
                    mediaType: "image/jpeg",
                    filename: "pipeline-architecture-diagram.jpg",
                },
                {
                    type: "file",
                    mediaType: "application/pdf",
                    filename: "rule-engine-spec-v1.pdf",
                },
            ],
        },
        {
            key: "msg-2",
            value: `To optimize multiple validation rules before PySpark execution, the rule engine compiles declarative rules into a single **Logical Execution Tree (AST)**. This avoids evaluating rules sequentially across multiple passes over the dataset.

### Logical Optimization Flow
\`\`\`text
    Raw Rules (YAML/JSON)
             ↓
    Rule AST & Expression Merger
             ↓
    Unified Logical Plan (Single Pass)
             ↓
    PySpark Optimizer (Catalyst)
             ↓
    Optimized Spark Job Execution
\`\`\`

Instead of running three separate Spark jobs for rules like \`NOT_NULL(email)\`, \`UNIQUE(id)\`, and \`BETWEEN(age)\`:

\`\`\`python
# Single-pass optimized PySpark Data Quality evaluation
from pyspark.sql import functions as F

def apply_optimized_quality_rules(df):
    return df.withColumn(
        "_quality_results",
        F.struct(
            F.when(F.col("email").isNotNull(), True).otherwise(False).alias("NOT_NULL_email"),
            F.when(F.col("age").between(18, 120), True).otherwise(False).alias("BETWEEN_age"),
            F.when(F.col("transaction_amount") > 0, True).otherwise(False).alias("POSITIVE_amount")
        )
    )
\`\`\`

#### Performance Comparison
| Optimization Strategy | Spark Jobs Triggered | Disk I/O Spill | Average Execution Time |
| :--- | :---: | :---: | :---: |
| Naive Multi-Pass Evaluation | 3 Jobs | High | 4.2 minutes |
| **Optimized Single-Pass AST** | **1 Job** | **Zero Spill** | **0.7 minutes** |

![Spark Execution Benchmark Chart](https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=900&h=450&fit=crop)

That's the kind of optimization that distinguishes an enterprise-grade data engineering platform from a simple rule executor.`,
            name: "AI Assistant",
        },
        {
            key: "msg-3",
            value: "Architecture Style : How to structure the persistence and execution layer?",
            name: "Alex Johnson",
        },
        {
            key: "msg-4",
            value: `We recommend a decoupled hybrid architecture:

* **Metadata & Control Plane:** PostgreSQL with Spring Data JPA for tenant metadata, rule contracts, and execution history.
* **Distributed Compute Engine:** PySpark + Delta Lake for processing high-throughput dataset validations.

\`\`\`text
[ Control Plane: Spring Boot / JPA ] ── (Rule AST) ──> [ Data Engine: PySpark ]
                 │                                                   │
         (PostgreSQL DB)                                    (Delta Lake Storage)
\`\`\`
`,
            name: "AI Assistant",
        },
        {
            key: "msg-5",
            value: "Persistence & Database Access : postgresql for metadata and delta lake for storage?",
            name: "Alex Johnson",
            attachments: [
                {
                    type: "file",
                    url: "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=800&h=500&fit=crop",
                    mediaType: "image/jpeg",
                    filename: "db-schema-preview.jpg",
                },
            ],
        },
        {
            key: "msg-6",
            value: `Exactly. PostgreSQL handles ACID relational metadata for user rules, schedules, and alerts. Delta Lake handles immutable data quality audit logs and quarantine partitions with time-travel query capability.`,
            name: "AI Assistant",
        },
        {
            key: "msg-7",
            value: "Show me how to route failed records into a Delta Lake Quarantine table with full audit lineage.",
            name: "Alex Johnson",
            attachments: [
                {
                    type: "file",
                    mediaType: "application/json",
                    filename: "quarantine-schema.json",
                },
            ],
        },
        {
            key: "msg-8",
            value: `Here is the production-ready pattern for quarantine routing using **Delta Lake** and PySpark structured batch jobs.

\`\`\`python
from pyspark.sql import functions as F

def route_quarantine_records(df, dataset_name="customer_orders"):
    # Filter records where any critical rule evaluation failed
    failed_df = df.filter(
        (F.col("_quality_results.NOT_NULL_email") == False) |
        (F.col("_quality_results.BETWEEN_age") == False)
    ).withColumn("_quarantined_at", F.current_timestamp()) \\
     .withColumn("_dataset_name", F.lit(dataset_name))

    # Append invalid records to the quarantine vault with schema evolution enabled
    failed_df.write \\
        .format("delta") \\
        .mode("append") \\
        .option("mergeSchema", "true") \\
        .saveAsTable("data_quality.quarantine_vault")

    # Return valid dataset for downstream consumption
    return df.filter(
        (F.col("_quality_results.NOT_NULL_email") == True) &
        (F.col("_quality_results.BETWEEN_age") == True)
    )
\`\`\`

![Delta Lake Audit Lineage DAG](https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=900&h=450&fit=crop)

> **Important SLA Notice:** Quarantine tables should maintain a strict 30-day Retention Policy with \`VACUUM\` jobs scheduled weekly to maintain cluster performance.`,
            name: "AI Assistant",
        },
        {
            key: "msg-9",
            value: "Can we define a declarative Data Quality Contract in YAML format for this pipeline?",
            name: "Alex Johnson",
        },
        {
            key: "msg-10",
            value: `Yes! Here is the declarative **Data Quality Contract v2.0** specification schema:

\`\`\`yaml
version: "2.0"
dataset: "analytics.orders_fact"
environment: "production"
owner: "data-governance-team@company.com"

quality_contract:
  thresholds:
    min_passing_rate: 0.99
    action_on_failure: "QUARANTINE_AND_ALERT"

  rules:
    - id: "rule_01"
      name: "order_id_not_null"
      type: "NOT_NULL"
      target_column: "order_id"
      severity: "CRITICAL"

    - id: "rule_02"
      name: "valid_amount_range"
      type: "RANGE"
      target_column: "amount"
      parameters:
        min: 0.01
        max: 50000.00
      severity: "WARNING"
\`\`\``,
            name: "AI Assistant",
        },
        {
            key: "msg-11",
            value: "How do we monitor pass/fail ratios in real time with Prometheus metrics?",
            name: "Alex Johnson",
        },
        {
            key: "msg-12",
            value: `You can expose PySpark data quality metrics to Prometheus using custom accumulators or pushing metrics to a **Prometheus Pushgateway**.

\`\`\`python
import requests

def push_quality_metrics(total_count, pass_count, fail_count, dataset):
    metrics_payload = f"""
    # HELP dq_records_total Total records processed
    # TYPE dq_records_total counter
    dq_records_total{{dataset="{dataset}"}} {total_count}

    # HELP dq_records_passed Records passing data quality checks
    # TYPE dq_records_passed counter
    dq_records_passed{{dataset="{dataset}"}} {pass_count}
    """
    requests.post("http://prometheus-pushgateway:9091/metrics/job/spark_dq", data=metrics_payload)
\`\`\`

![Grafana Data Quality Observability Dashboard](https://images.unsplash.com/photo-1518770660439-4636190af475?w=900&h=450&fit=crop)

This feeds real-time **Grafana** dashboards for continuous data observability across your data platform.`,
            name: "AI Assistant",
        },
        {
            key: "msg-13",
            value: "Is that job submitted to the spark cluster automatically via Livy or Databricks API?",
            name: "Alex Johnson",
        },
        {
            key: "msg-14",
            value: `Yes, the rule execution service submits jobs asynchronously using Apache Livy REST endpoints or the Databricks Jobs API v2.1, tracking run status via webhook callbacks.`,
            name: "AI Assistant",
        },
    ];

    // Reactive state using Svelte 5 runes
    let activeCheckpointId = $state<string>("");

    // Group raw messages into clean conversation turns (User prompt on top -> Assistant response below)
    let turns = $derived.by<ConversationTurn[]>(() => {
        const result: ConversationTurn[] = [];
        let turnIndex = 1;

        for (let i = 0; i < rawMessages.length; i++) {
            const msg = rawMessages[i];
            if (msg.name !== "AI Assistant") {
                const assistantMsg = rawMessages[i + 1]?.name === "AI Assistant" ? rawMessages[i + 1] : undefined;
                result.push({
                    id: `cp-${msg.key}`,
                    key: msg.key,
                    userPrompt: msg.value,
                    userAttachments: msg.attachments,
                    assistantResponse: assistantMsg?.value,
                    turnNumber: turnIndex++,
                });
            }
        }
        return result;
    });

    // Extract user prompts into checkpoints for the right-side outline panel
    let checkpoints = $derived<CheckpointItem[]>(
        turns.map((t) => ({
            id: t.id,
            title: t.userPrompt,
            number: t.turnNumber,
        }))
    );

    // IntersectionObserver for dynamic scroll spying on user checkpoints
    $effect(() => {
        if (checkpoints.length === 0) return;

        if (!activeCheckpointId && checkpoints.length > 0) {
            activeCheckpointId = checkpoints[0].id;
        }

        const observer = new IntersectionObserver(
            (entries) => {
                for (const entry of entries) {
                    if (entry.isIntersecting) {
                        activeCheckpointId = entry.target.id;
                    }
                }
            },
            {
                root: null,
                rootMargin: "-10% 0px -70% 0px",
                threshold: 0,
            }
        );

        checkpoints.forEach((cp) => {
            const el = document.getElementById(cp.id);
            if (el) observer.observe(el);
        });

        return () => observer.disconnect();
    });
</script>

<ChatContainer>
    <!-- Implicit children snippet (Conversation content) -->
    <Conversation.Root style="height: auto" class="max-w-4xl w-full mx-auto px-4">
        <Conversation.Content class="gap-10">
            {#if turns.length === 0}
                <Conversation.EmptyState
                    description="Messages will appear here as the conversation progresses."
                    title="Start a conversation"
                >
                    {#snippet icon()}
                        <MessageSquare class="size-6" />
                    {/snippet}
                </Conversation.EmptyState>
            {:else}
                {#each turns as turn, index (turn.id)}
                    <!-- Conversation Turn: User prompt on top, older messages above, assistant response below, space for short answers -->
                    <div
                        id={turn.id}
                        class={cn(
                            "flex flex-col gap-4 scroll-mt-14 transition-all",
                            index === turns.length - 1
                                ? "min-h-[45vh] pb-24"
                                : "pb-8 border-b border-border/30"
                        )}
                    >
                        <!-- User prompt on top -->
                        <Message.Root from="user">
                            {#if turn.userAttachments && turn.userAttachments.length > 0}
                                <Message.Attachments class="mb-2">
                                    {#each turn.userAttachments as att}
                                        <Message.Attachment data={att} />
                                    {/each}
                                </Message.Attachments>
                            {/if}
                            <Message.Content>
                                <div class="whitespace-pre-wrap font-medium">
                                    {turn.userPrompt}
                                </div>
                            </Message.Content>
                        </Message.Root>

                        <!-- Assistant response directly below -->
                        {#if turn.assistantResponse}
                            <Message.Root from="assistant" class="min-h-[80px]">
                                <Message.Content>
                                    <Message.Response content={turn.assistantResponse} />
                                </Message.Content>
                            </Message.Root>
                        {/if}
                    </div>
                {/each}
            {/if}
        </Conversation.Content>
        <Conversation.ScrollButton />
    </Conversation.Root>

    <!-- Prompt snippet injected into container -->
    {#snippet prompt()}
        <div class="w-full max-w-3xl mx-auto">
            <PromptInput.Root class="p-2 shadow-2xl border border-border/60 bg-background/95 backdrop-blur-xl rounded-2xl" onSubmit={handleSubmit}>
                <PromptInput.Body>
                    <PromptInput.Textarea />
                </PromptInput.Body>
                <PromptInput.Toolbar>
                    <Select.Root type="single" bind:value={selectedModel}>
                        <Select.Trigger class="border-none">
                            {#if selectedModel}
                                {models.find(
                                    (model) => model.name === selectedModel,
                                )?.label}
                            {:else}
                                Select Model
                            {/if}
                        </Select.Trigger>
                        <Select.Content align="start">
                            {#each models as model (model.name)}
                                <Select.Item
                                    value={model.name}
                                    label={model.label}
                                >
                                    {model.label}
                                </Select.Item>
                            {/each}
                        </Select.Content>
                    </Select.Root>
                    <PromptInput.Submit onStop={handleStop} />
                </PromptInput.Toolbar>
            </PromptInput.Root>
        </div>
    {/snippet}
</ChatContainer>

<!-- Prompt Checkpoints Outline Panel on the Right Side of the Screen -->
<PromptCheckpoints checkpoints={checkpoints} bind:activeId={activeCheckpointId} />
