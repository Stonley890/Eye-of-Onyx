#region
	# Dummy Scoreboards
scoreboard objectives remove onyx.board.position
	## 0 for "citizen",
	## 1 for "noble presumptive", 2 for "noble apparent", 3 for "lord/lady"
	## 4 for "heir presumptive", 5 for "heir apparent"
	## 6 for "ruler"
scoreboard objectives remove onyx.chal.status
	## 0 for "valid",
	## 1 for "on cooldown",
	## 2 for "immune"
scoreboard objectives remove onyx.chal.receive
scoreboard objectives remove onyx.chal.send
scoreboard objectives remove onyx.gen_timer
scoreboard objectives remove onyx.math

	# Trigger Scoreboards
scoreboard objectives remove -onyx.trigger

	# Misc. Scoreboards
scoreboard objectives remove onyx.health
scoreboard objectives remove suso.str

#endregion
