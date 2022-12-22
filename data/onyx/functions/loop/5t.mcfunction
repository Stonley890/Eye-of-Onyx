#region
	# 5-Tick Loop Functions
function #onyx:timers/5t
#execute if score $ints_remaining onyx.math matches 2..6 run function onyx:loop/5t/store_client_time
schedule function onyx:loop/5t 5t replace

#endregion