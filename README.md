**Unwind** is a diagnostic mod for Minecraft 1.21.1 (NeoForge) designed for mod developers and modpack maintainers. It captures runtime context, method arguments, and mod provenance to make logs and bug reports easier to trace.

Unwind does not modify or replace vanilla crash reports or `latest.log`. Instead, it writes standalone diagnostic files to the `crash-reports/unwind/` directory inside your instance folder.

## Features

### 1. Pre-Unwind Frame State Capture

Standard Java stack traces show the method hierarchy of a crash, but all local variables and method arguments are lost once the JVM unwinds the stack. Unwind instruments common game entry points and keeps an active frame stack per thread. If an exception is thrown inside an instrumented method, the arguments remain on the stack and are dumped into the report.

Currently instrumented entry points include:

* **Items:** `ItemStack.use`, `ItemStack.useOn`, and `ItemStack.inventoryTick` (captures `ItemStack` counts, `DataComponents`, hand, player, and `Level` logical side).
* **Blocks & BlockEntities:** `Level.setBlock`, `BlockStateBase.useItemOn`, `BlockStateBase.useWithoutItem`, `BlockStateBase.handleNeighborChanged`, `BlockStateBase.tick`, and `BoundTickingBlockEntity.tick` (captures coordinates, cached vs. live world `BlockState` mismatches, and removal state).
* **Entities:** `Level.guardEntityTick` (captures entity ID, UUID, registry type, coordinates, and delta movement).
* **Containers & Menus:** `AbstractContainerMenu.clicked` (captures menu class, `MenuType`, clicked slot ID, click type, slot item stack, and cursor-carried stack).
* **Networking:** `Connection.genericsFtw` (unwraps `ClientboundCustomPayloadPacket` and `ServerboundCustomPayloadPacket` to extract the underlying `CustomPacketPayload` ID, class, owning mod, and payload data).
* **Recipes:** `RecipeManager.getRecipeFor` (captures recipe type and input slot items).
* **Client Rendering:** `EntityRenderDispatcher.render`, `BlockEntityRenderDispatcher.tryRender`, and `Screen.renderWithTooltip`.

### 2. Stack Frame & Mixin Provenance

Every frame in a crash stack trace is mapped through Java's module system and NeoForge's `ModList` to display:

* Module name, Mod ID, and mod version
* Source `.jar` file name
* Active `ClassLoader`
* Decomposed synthetic Mixin handler names (`@Inject`, `@Redirect`, `@ModifyVariable`, `@WrapOperation`), extracting the source Mod ID and handler method name.

### 3. World State Mutation Lineage

Unwind tracks recent `Level.setBlock` calls in memory. When a crash occurs (for example, a `BlockEntity` ticking against an unexpected block state), the report lists the last 30 block mutations across the world, including:

* Tick number, dimension, and `BlockPos`
* Previous `BlockState` and new `BlockState`
* Calling mod, class, method, and line number
* Asynchronous task origin (if the block change was scheduled from another thread or network packet)

### 4. Gameplay Flight Recorder

Maintains a ring buffer of the last 150 gameplay interactions and custom network payloads across both client and server threads (block interactions, entity interactions, container opens, entity damage/deaths, and non-movement packets). High-frequency events like entity ticks and movement packets are kept only in transient scopes so they do not flood the history buffer.

### 5. Live Warning & Error Stack Tracer

Many mods log warnings or errors to the console without printing a stack trace, making it difficult to know which mod emitted the message. Unwind attaches a Log4j2 appender that intercepts `WARN` and `ERROR` logs at runtime, walks the call stack, resolves the calling `.jar`, and writes the trace to `unwind-reports/live-warn-error-traces.log`. Each unique warning is recorded up to 5 times per session to prevent disk spam.

### 6. Automatic Path & Environment Scrubbing

Before writing any file to disk, Unwind strips local environment identifiers using exact string matching:

* Instance directory (`<GAME_DIR>`)
* User home directory (`<USER_HOME>`)
* System temporary directory (`<TEMP_DIR>`)
* OS username (`<OS_USER>`) and machine hostname (`<PC_NAME>`)

## Output Files

All files are generated inside `<instance_folder>/crash-reports/unwind/`:

* `unwind-<yyyy-MM-dd_HH.mm.ss.SSS>.txt` — Generated automatically whenever Minecraft saves a crash report or an uncaught exception terminates a thread.
* `live-warn-error-traces.log` — Generated at startup and updated during gameplay whenever a `WARN` or `ERROR` message is logged.

## Example Report Output (Illustrative)

```text
================================================================================
  UNWIND RUNTIME EXECUTION & STATE PROVENANCE DUMP (MC 1.21.1 / NEOFORGE)
================================================================================
Timestamp        : 2026-11-11T11:20:14.412
Report Context   : Unexpected error
Exception Class  : java.lang.ClassCastException
Exception Message: class net.minecraft.client.multiplayer.ClientLevel cannot be cast to class net.minecraft.server.level.ServerLevel
Crashing Thread  : Render thread (ID=1, State=RUNNABLE)
Task Causality   : Direct Synchronous Execution (Same Thread)
JVM Heap Usage   : 1420 MB / 8192 MB
================================================================================

[1] PRE-UNWIND INSTRUMENTED CALL STACK & LOCAL STATE SNAPSHOT
--------------------------------------------------------------------------------
  UnwindFrame[0] Phase=ITEM_USE | Side=CLIENT | Tick=4821
    Target : com.example.mod.CustomSwordItem.use(Level, Player, InteractionHand)
    Owner  : Mod=examplemod | Jar=examplemod-1.21.1-1.0.0.jar
    ├─ arg0 (Level) = net.minecraft.client.multiplayer.ClientLevel [dim=minecraft:overworld, isClientSide=true]
    ├─ arg1 (Player) = net.minecraft.client.player.LocalPlayer [name=Dev, pos=(124.5, 64.0, -82.1)]
    ├─ arg2 (InteractionHand) = MAIN_HAND
    ├─ ItemStack = 1x examplemod:custom_sword (ItemClass=com.example.mod.CustomSwordItem)
    ├─ DataComponents = {minecraft:damage=12}

```

## Installation & Compatibility

* **Minecraft Version:** 1.21.1
* **Mod Loader:** NeoForge
* **Environment:** Client and Server (can be installed on one side or both; installing on both provides full client and server telemetry).