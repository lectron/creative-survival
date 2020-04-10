# Creative Survival
A Minecraft server plugin that balances creative mode in a Survival Multiplayer Server - fork of LimitCreative Reloaded

Items spawned in creative will be marked as creative in their NBT data (nbt example: "LectronCreative.CreativeItem: viet"). You can then enable/disable config options to change if survival players can use these items. This is a great way for tracking down who has been spawning in items, and preventing them from giving out usable diamonds etc...

They are unable to attack with them, wear the armor, craft with them, shoot and place the spawned in items. Only creative mode users can do this.

Any blocks marked as creative spawned, will not be usable with pistons. This is due to a weird Minecraft bug where piston events fire more than once sometimes, which can cause bugs.

# No PVP in Creative Mode
While in creative, the player will be put in non-pvp mode and can't attack others

# Blacklisted Commands
While in creative, they can't do Blacklisted Commands (can be added or removed as needed):
  - auction
  - ah
  - privatevault
  - playervault
  - vault
  - pv
  - pvp

# Commands and Permissions
There is currently only one command and it is /clearcreative - lectroncreative.clearcreative which removes the creative NBT data from the item that the command executor is holding.
