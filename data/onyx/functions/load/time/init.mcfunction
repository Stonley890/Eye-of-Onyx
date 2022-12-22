gamerule sendCommandFeedback true
forceload add -1 -1
fill -2 -63 -2 0 -63 0 air
setblock -1 -63 -1 minecraft:command_block[facing=up]{auto:1b,Command:"minecraft:help me",TrackOutput:1b}
setblock -1 -62 -1 minecraft:chain_command_block[facing=up]{auto:1b,Command:"function onyx:load/time/get",TrackOutput:1b}