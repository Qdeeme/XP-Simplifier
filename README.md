# 🧪 Configurable XP Simplifier

A **Fabric** mod for **Minecraft 1.21.1** focused on removing XP orbs, simplifying XP collection, and providing **full control over XP sources and values**.

All XP is redirected **directly to the player**, improving performance and making XP behavior predictable and configurable.

---

## 🎯 Prime Goals

- Completely remove **XP orbs**
- Redirect all dropped XP straight to the player
- Simplify the XP collecting mechanic
- Provide full control over:
  - XP sources
  - XP values
  - XP behavior for blocks, entities, and crops

---

## ⚙️ Core Options

```json
"blockBreakXpEnabled": true | false,
"entityKillXpEnabled": true | false
```

### Behavior Overview

- Setting **either option to `true`**
  - Cancels vanilla XP orb spawning
- Setting **both options to `false`**
  - Disables the mod’s core logic
  - XP orbs spawn and behave normally

---

## ⛏️ Block XP (`blockBreakXpEnabled`)

- `true`
  - All blocks and crops (if configured) may grant XP
  - Works with vanilla and modded blocks
- `false`
  - No XP is granted from block breaking
  - Even vanilla ores grant no XP

---

## 🧟 Entity XP (`entityKillXpEnabled`)

- `true`
  - Entities grant XP based on configuration
  - If an entity is **not defined**, default XP values are used
- `false`
  - Entities grant XP using vanilla default values

> ⚠️ **Enchanting Bottles**  
> When XP orbs are disabled, Enchanting Bottles are also affected.  
> Keep `entityKillXpEnabled = true` if you want them to work "correctly".

---

## 🧩 Configuration Categories

The config is split into **three main categories**:

- **Blocks**
- **Entities**
- **Crops**

Each category may contain **subcategories**.  
Subcategories are ignored by the mod logic and exist purely to keep the config **clean and tidy**.

---

## 📦 Blocks / Entities Configuration Structure

```json
"[Blocks | Entities]": {
  "[subcategory]": {
    "blocks | entities": {
      "modid:block_id | modid:entity_id": {
        "type": "Random | Fixed",

        // If type = Random
        "min": 0,
        "max": 0,

        // If type = Fixed
        "fixed": 0
      }
    }
  }
}
```

---

## 🌾 Crops Configuration Structure

Crops are handled differently due to the **Age property**.

```json
"Crops": {
  "[subcategory]": {
    "crops": {
      "modid:block_id": {
        "matureAge": [value of mature crop],
        "xp": {
          "type": "Random | Fixed",

          // If type = Random
          "min": 0,
          "max": 0,

          // If type = Fixed
          "fixed": 0
        }
      }
    }
  }
}
```

---

## 🎲 XP Calculation Types

Two XP calculation methods are supported:

- **Random**
  - Grants a random amount of XP between `min` and `max`
- **Fixed**
  - Grants a fixed amount of XP
  - `0` means no XP is granted

---

## 🔧 Mending Rework (Anvil-Based)

Because XP orbs are removed, **Mending has been reworked** to function without them while remaining balanced.

### Settings

```json
"xpRepairEnabled": true | false,
"maxAnvilRepairCost": 40
```

---

### 🔨 How Mending Works

- Items with **Mending** can be repaired in an **Anvil**
- No additional items are required
- Repair cost is **XP only**

**Default formula:**
Can be changed in a **Config** file

```
1 XP level = 100 durability points
```

---

### 📉 Max Cap Behavior

`maxAnvilRepairCost` limits the **maximum XP levels** required for a full repair.

#### Example

```
Diamond Sword durability: 1561
```

| maxAnvilRepairCost | XP Levels Needed |
|--------------------|-----------------|
| 40 (default)       | 16 levels       |
| 10                 | 10 levels       |

Lower values reduce the total XP cost for full repairs.

---

### ⚠️ Important

- Setting `"xpRepairEnabled": false`
  - **Disables Mending completely if orbs are canceled**

---

## 🧩 Compatibility

- Minecraft **1.21.1** / **1.21.0**
- **Fabric**
- Server-side friendly
- Compatible with modded blocks, entities, and crops

---

## 📜 License

This project is licensed under the **MIT License**.  
You are free to use, modify, and include it in modpacks.

---

⭐ If you find this mod useful, consider starring the repository!
