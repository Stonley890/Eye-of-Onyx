#region
	# Scoreboards
		## Dummy Scoreboards
scoreboard objectives add onyx.board.position dummy
	## 0 for "citizen",
	## 1 for "noble presumptive", 2 for "noble apparent", 3 for "lord/lady"
	## 4 for "heir presumptive", 5 for "heir apparent"
	## 6 for "ruler"
scoreboard objectives add onyx.chal.status dummy
	## 0 for "valid",
	## 1 for "on cooldown",
	## 2 for "immune"
scoreboard objectives add onyx.chal.receive dummy
scoreboard objectives add onyx.chal.send dummy
scoreboard objectives add onyx.gen_timer dummy
scoreboard objectives add onyx.math dummy

		## Trigger Scoreboards
scoreboard objectives add -onyx.trigger trigger

		## Misc. Scoreboards
scoreboard objectives add onyx.health health

	# Variables
scoreboard players set #1 onyx.math 1
scoreboard players set #10 onyx.math 10
scoreboard players set $binary onyx.math 0
scoreboard players set $ints_remaining onyx.math 0

#endregion
