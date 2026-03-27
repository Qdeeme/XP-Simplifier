# 🧪 Configurable XP Simplifier

A **Fabric** mod focused on removing XP orbs, simplifying XP collection, and providing **full control over XP sources and values**.

All XP is redirected **directly to the player**, improving performance and making XP behavior predictable and configurable.


## 🎯 Prime Goals

*   Completely remove **XP orbs**
*   Redirect all dropped XP straight to the player
*   Simplify the XP collecting mechanic
*   Provide full control over:
    *   XP sources
    *   XP values
    *   XP behavior for blocks/entities/crops/etc.

***
***

## 🧩 Configuration Categories

The config is split into **few categories**:

*   **Blocks.json**
*   **Entities.json**
*   **Crops.json**
*   **Smelting.json**
*   **Trading.json**

More soon.

Each category may contain **subcategories**.  
Subcategories exist purely to keep the config **clean and tidy**.

***

## 🎲 XP Calculation Types

Two XP calculation methods are supported:

*   **Random**
    *   Grants a random amount of XP between `min` and `max`
*   **Fixed**
    *   Grants a fixed amount of XP
    *   `0` means no XP is granted

***

## 🔧 Mending Rework (Anvil-Based)

Because XP orbs are removed, **Mending has been reworked** to function without them while remaining balanced.


### 🔨 How Mending Works

*   Items with **Mending** can be repaired in an **Anvil**
*   No additional items are required
*   Repair cost is **XP only**

**Default formula:**

_can be changed via **Config** file_

```
1 XP level = 100 durability points
```

***

### 📉 Max Cap Behavior

`maxAnvilRepairCost` limits the **maximum XP levels** required for a full repair.

#### Example

```
Diamond Sword durability: 1561
```

| maxAnvilRepairCost |XP Levels Needed |
| ------------------ |---------------- |
| 40 (default)       |16 levels        |
| 10                 |10 levels        |

Lower values reduce the total XP cost for full repairs.

***

### ⚠️ Important

*   Setting `"xpRepairEnabled": false`
    *   **Disables Mending completely if orbs are canceled**

***

## 🧩 Compatibility

*   Minecraft **1.20-1.21.1**
*   **Fabric**
*   Server-side friendly
*   Compatible with modded blocks/entities/etc

***

## WIP:
Still under development, more options will be added soon. 

Do you want to know more? Visit wiki -> [HERE](https://github.com/Qdeeme/XP-Simplifier/wiki)


***

## TL;DR
VERSIONS 1.0.+ :
- Once updated to newer version -> create a backup of your existing configs and delete whole folder. Let the mod initialize the defaults and then replace all new maps with yours but be careful, few changes were made to versions 1.1+ :)



## KNOWN ISSUE:
- Create's experience nuggets don't work

- [XP Storage books](https://modrinth.com/mod/xp-storage) - NO COMPATIBILITY ->
Use [Tomes of Experience](https://modrinth.com/mod/tomes-of-experience) instead

- RightClickHarvest - NO COMPATIBILITY

*If you found a bug/no compatibility issue, let me know [HERE](https://github.com/Qdeeme/XP-Simplifier/issues)*

***

## 📜 License

This project is licensed under the **MIT License**.  
You are free to use, modify, and include it in modpacks.

***

⭐ If you find this mod useful, consider following the mod!
