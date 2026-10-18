# Extra Armor Slots

A Minecraft mod that adds extra armor columns right next to the vanilla armor slots, so you can wear two (or up to eleven) armor sets at once. Every set counts: armor, toughness, attributes, enchantments and modded set bonuses. Overpowered on purpose.

- **Minecraft** 1.21.1, **NeoForge** 21.1+
- No other dependencies

## Download

Get the latest jar from the **[Releases page](https://github.com/Vihellnisch/Extra-Armor-Slots/releases/latest)** and put it in your `mods` folder. It's needed on both the client and the server.

## What's in it

- **A second armor column** grows out of the inventory's left edge, styled like the vanilla GUI. It's in the creative inventory too, and it hides while the recipe book is open.
- **Everything stacks:** armor, toughness, knockback resistance, other attributes, Protection, Thorns, Respiration, Frost Walker, Mending and every other armor enchantment. Extra armor wears down and drops on death like normal armor (unless `keepInventory` is on).
- **What you see:** each slot shows your normal armor; the extra piece shows when the normal slot is empty.
- **Other mods see it as worn.** Set bonuses and "wear the full set" checks of other mods find pieces in the extra columns, however those mods read armor. Minecraft itself keeps seeing only the normal armor, so nothing counts twice.
- **Vanilla wearables work from the extra slots:** elytra (even over a chestplate), turtle shell, carved pumpkin vs endermen, gold vs piglins, leather boots on powder snow and against freezing, mob heads.
- **Raised caps:** each extra set adds another layer to the armor and protection caps. The first 20 points work exactly like vanilla; each extra layer blocks up to 80% of what is left, so you never become fully immune.
- **Shift-click** armor into the extra column when the normal slot is taken.

## Config

`config/extraarmor-common.toml`, also editable in-game (Mods > Extra Armor Slots > Config). On a server, the server's values count.

- `extraArmorSets` (1 to 10, default 1): how many extra sets you can wear. More sets means more columns; if the screen is too narrow they show a page at a time with `<` `>` buttons. Lowering it gives the armor in removed columns back.
- `raisedCaps` (default true): turn off for vanilla caps (armor 30, toughness 20, at most 80% blocked by armor and 80% by protection).

## Building

```
./gradlew build
```

The jar ends up in `build/libs/`.

A scripted in-game test tour lives in `src/tour` and doesn't go into the jar. It clicks slots, presses keys and checks the results:

```
./gradlew runTourClient
```

`generate_panel.py` builds the extra column texture from the vanilla inventory texture.

## Credits

- Mod by **Vihellnisch**.

## License

The code is under the [MIT License](LICENSE). The extra column texture is cut from Minecraft's own inventory texture, which belongs to Mojang.
