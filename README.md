# Loot Journal

Track what you get from every block you break. Loot Journal silently monitors your inventory and attributes item pickups to the blocks that caused them, then exports the results as clean JSON when your session ends.

Built for **block randomizer speedruns**, but useful for anyone who wants to know what they're actually getting from mining.

**Latest release:** v1.2.0 · [Modrinth](https://modrinth.com/mod/lootjournal)

---

## Features

### Drop Attribution
Every block break opens a short attribution window. Items gained within that window are linked to the block just broken. Multiple pending breaks are tracked concurrently.

### Chest & Container Tracking
Items picked up while any container screen is open (chest, barrel, furnace, etc.) are attributed to a separate "Containers" bucket so they're never confused with block drops.

### Goal / Target Tracking
Set a loot goal from the in-game **Loot Goal** screen:
- Type any item name (e.g. `diamond`, `iron_ingot`) and set a target count
- A small overlay in the top-right of your HUD shows live session progress
- A chat message fires once when the goal is reached
- Goal persists across sessions

### Session History Screen
Browse past sessions without leaving the game — date, world name, duration, items gained, mob kills, XP, and blocks broken. Click any session to expand its details.

### Mob Kill Tracking
Every mob death is recorded per session, broken down by type.

### JSON Export
Session data is saved to `loot-journal/sessions/session-YYYY-MM-DD-HH-mm-ss.json`. A running `statistics.json` accumulates drop counts and drop frequencies per block type across all sessions.

### Block Randomizer Mode
- One pending break at a time — drops are never mixed up
- Live chat line per break: `[Randomizer] grass block → dirt x1, seeds x2`
- Wider attribution window for server loot table delay

---

## Compatibility

| Loader | Minecraft |
|--------|-----------|
| Fabric / Quilt | 1.21.1, 1.21.2–1.21.4 |
| NeoForge | 1.21.1, 1.21.2–1.21.4, 1.21.5 |

---

## Keybinds

| Action | Default |
|--------|---------|
| Show mid-session report | J |
| Open settings | *(unbound)* |
| Open session history | *(unbound)* |
| Open loot goal | *(unbound)* |

---

## Configuration

| Setting | Default | Description |
|---------|---------|-------------|
| `enabled` | `true` | Master on/off switch |
| `attributionWindowTicks` | `60` | Ticks after a break to collect drops |
| `blockRandomizerMode` | `false` | Enable block randomizer mode |
| `brFeedbackInChat` | `true` | Per-break chat lines in BR mode |
| `writeSessionJson` | `true` | Save per-session JSON |
| `writeStatisticsJson` | `true` | Update cumulative statistics file |
| `writeBreakEvents` | `false` | Include per-break event data in session JSON |
| `showSummaryOnDisconnect` | `true` | Print summary to chat on disconnect |
