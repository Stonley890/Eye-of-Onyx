#region
	# Trigger Types
		## 2
tellraw @s {"score":{"name":"@s","objective":"-onyx.trigger"}}

	# Reset
scoreboard players set @s -onyx.trigger 0
advancement revoke @s only onyx:trigger

#endregion