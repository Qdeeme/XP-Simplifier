# 🧪 Configurable XP Simplifier (Still a little buggy :) )

A **Fabric** mod focused on providing full control over XP, giving full access to all xp values based on the source and providing **compatibility between vanilla and moded content**.


## 🎯 Prime Goals

*   Full control over XP behaviour:
    *  Direct xp gaining (no orbs)
    *  Vanilla xp gaining (with orbs)
*   Full control over:
    *   XP sources
    *   XP values
*   User-friendly config (all split into sources)

***
***

## 🧩 Configuration Categories

The config is split into **few categories**:

*   **Blocks.json**
*   **Entities.json**
*   **Crops.json**
*   **Smelting.json**
*   **Trading.json**
*   **Breeding.json**
*   **Fishing.json**
*   **Grindstone.json**



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

## WIKI:
Do you want to know more? Visit wiki -> [HERE](https://github.com/Qdeeme/XP-Simplifier/wiki)


***

## TL;DR
VERSIONS 1.0.+ :
- Once updated to newer version -> create a backup of your existing configs and delete whole folder. Let the mod initialize the defaults and then replace all new maps with yours but be careful, few changes were made to versions 1.+ :)



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

# FAQ

**Q: Do I need to restart for config changes?**
- A: Yes. All configuration files require a restart to take effect.

**Q: Can I use decimal XP for mining?**
- A: No, only smelting supports decimal values. Blocks/Entities/Fishing/Breeding/Trading must be integers.

**Q: How do I prevent XP duplication?**
- A: Since the mods supports only 2 modes -> `"simple"` and `"vanilla"`, there's no XP duplication.

**Q: What about negative values while braking blocks/killing entities?**
- A: If `"OrbMode"` -> `"simple"`, all actions (despite smelting) accept negative values so players can be punished for killing entities like villagers or so.
     Keep in mind that while setting up negative values -> `"min"` must be lower than `"max"`.

**Q: Do experience bottles work without entity XP enabled?**
- A: No, experience bottles are treated as entities, so they require `entityXpMode: "vanilla" || "on"`.

**Q: What about modded content?**
- A: All default/vanilla fallbacks exist, so once all options set to -> `on` || `vanilla`, all sources should work fine.
