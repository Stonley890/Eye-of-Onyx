#region
	# Set time_arr to the Parsed Time
execute unless data storage onyx:settings time_arr[0] run data modify storage onyx:settings time_arr set from storage suso.str:io out
execute if data storage onyx:settings time_arr[7] run data remove storage onyx:settings time_arr[5]
execute if data storage onyx:settings time_arr[6] run data remove storage onyx:settings time_arr[2]

	# Set time to Next String Number in time_arr
data modify storage onyx:settings time_char set from storage onyx:settings time_arr[0]

	# Count Remaining String-Integer Pairs
execute if score $ints_remaining onyx.math matches 0 if data storage onyx:settings time_arr[5] run scoreboard players set $ints_remaining onyx.math 6
execute if score $ints_remaining onyx.math matches 0 if data storage onyx:settings time_arr[4] run scoreboard players set $ints_remaining onyx.math 5
execute if score $ints_remaining onyx.math matches 0 if data storage onyx:settings time_arr[3] run scoreboard players set $ints_remaining onyx.math 4
execute if score $ints_remaining onyx.math matches 0 if data storage onyx:settings time_arr[2] run scoreboard players set $ints_remaining onyx.math 3
execute if score $ints_remaining onyx.math matches 0 if data storage onyx:settings time_arr[1] run scoreboard players set $ints_remaining onyx.math 2
execute if score $ints_remaining onyx.math matches 0 if data storage onyx:settings time_arr[0] run scoreboard players set $ints_remaining onyx.math 1

		## Announce String-Integer Pairs Remaining
#tellraw @a [{"text":"Integer pairs remaining: ","color":"dark_aqua"},{"score":{"name":"$ints_remaining","objective":"onyx.math"},"color":"green"}]

	# Store & Loop
function onyx:load/time/store
execute if score $ints_remaining onyx.math matches 1..6 run function onyx:load/temp

#endregion