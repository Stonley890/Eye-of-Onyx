#region
		## Announce Process Start
#tellraw @a [{"text":"String identified as ","color":"dark_aqua"},{"color":"green","nbt":"time_arr[0]","storage":"onyx:settings"}]
#tellraw @a {"text":"Processing string...","color":"dark_aqua"}

	# Determine String-Integer Pair
execute if data storage onyx:settings {time_char:"0"} run scoreboard players set $char onyx.gen_timer 0
execute if data storage onyx:settings {time_char:"1"} run scoreboard players set $char onyx.gen_timer 1
execute if data storage onyx:settings {time_char:"2"} run scoreboard players set $char onyx.gen_timer 2
execute if data storage onyx:settings {time_char:"3"} run scoreboard players set $char onyx.gen_timer 3
execute if data storage onyx:settings {time_char:"4"} run scoreboard players set $char onyx.gen_timer 4
execute if data storage onyx:settings {time_char:"5"} run scoreboard players set $char onyx.gen_timer 5
execute if data storage onyx:settings {time_char:"6"} run scoreboard players set $char onyx.gen_timer 6
execute if data storage onyx:settings {time_char:"7"} run scoreboard players set $char onyx.gen_timer 7
execute if data storage onyx:settings {time_char:"8"} run scoreboard players set $char onyx.gen_timer 8
execute if data storage onyx:settings {time_char:"9"} run scoreboard players set $char onyx.gen_timer 9

		## Announce Process Result
#tellraw @a [{"text":"Processed string to ","color":"dark_aqua"},{"color":"green","score":{"name":"$char","objective":"onyx.gen_timer"}}]

		## Multiply by 10 if $char represents 10's place
execute if score $ints_remaining onyx.math matches 6 run scoreboard players operation $char onyx.gen_timer *= #10 onyx.math
execute if score $ints_remaining onyx.math matches 4 run scoreboard players operation $char onyx.gen_timer *= #10 onyx.math
execute if score $ints_remaining onyx.math matches 2 run scoreboard players operation $char onyx.gen_timer *= #10 onyx.math

		## Reset Time
execute if score $ints_remaining onyx.math matches 6 run scoreboard players set $hour onyx.gen_timer 0
execute if score $ints_remaining onyx.math matches 6 run scoreboard players set $min onyx.gen_timer 0
execute if score $ints_remaining onyx.math matches 6 run scoreboard players set $sec onyx.gen_timer 0

		## Store $char to onyx.gen_timer
execute if score $ints_remaining onyx.math matches 5..6 run scoreboard players operation $hour onyx.gen_timer += $char onyx.gen_timer
execute if score $ints_remaining onyx.math matches 3..4 run scoreboard players operation $min onyx.gen_timer += $char onyx.gen_timer
execute if score $ints_remaining onyx.math matches 1..2 run scoreboard players operation $sec onyx.gen_timer += $char onyx.gen_timer

	# Remove Processed Index in time_arr
data remove storage onyx:settings time_arr[0]
scoreboard players remove $ints_remaining onyx.math 1

#endregion
