# Alt Companion module

Brings one of your own characters, from any account, into your party while you hunt. The party AI plays it the same
way it plays recruited party members, and it levels with you.

## What it does

When enabled, it registers one voiced command:

- `.alt <name>` loads that character and it joins your party next to you.

While summoned the character:

- Follows and assists you and plays its class with the phantom playstyles (heals, buffs, nukes, songs and so on).
  Whispers and party chat orders work the same as for recruited members.
- Earns its share of party experience like a real member (even with `FakePlayerPartyExpShare = False`) and levels up. New skills are not learned for it: log in to it and visit a trainer.
- Uses its own soulshots, spiritshots and healing potions. Nothing is added to its inventory.
- Loses experience on death like any character.
- Gets no share of party loot or adena while `FakePlayerPartyLootShare = False` in `config/Custom/FakePlayers.ini`,
  the same rule as recruited phantoms.

It leaves, and is saved, when you remove it from the party, when you log out, when it stays dead past the
resurrection window, or when you log in to its own account.

Any character can be summoned except bots (phantoms, friend regulars and Olympiad nobles), characters already in the
world, and characters saved dead.

All the work is done by the platform's party companion service (`context.companions()`, see
`docs/MODULE_FRAMEWORK.md` section 3.6). This module only adds the command.

## Enable it

It ships enabled. To change that, edit `Enabled` in `config/module.ini` and restart the server.

## Disable it

Set `Enabled = False` in `config/module.ini` and restart. The command is not registered.

## Remove it

While the server is stopped or the module is disabled, delete this whole `alt-companion` directory. The module has no
database tables and stores nothing of its own; summoned characters are saved to their normal character rows.
