#region
	# Generate Vitals
function onyx:load/gen/scbds
function onyx:load/gen/storage

	# Loops
		## Clear
#schedule clear onyx:loop/1t
#schedule clear onyx:loop/2t
schedule clear onyx:loop/5t
#schedule clear onyx:loop/10t
#schedule clear onyx:loop/20t

		## Run
#function onyx:loop/1t
#function onyx:loop/2t
function onyx:loop/5t
#function onyx:loop/10t
#function onyx:loop/20t

	# Launch Messages
function onyx:load/hw

#endregion
