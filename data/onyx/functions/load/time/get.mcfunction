#region
	# Set Input to Parse
		## Pull Client/Server Time
data modify storage suso.str:io in.string set from block -1 -63 -1 LastOutput
		## Parse User Input String
#data modify storage suso.str:io in.string set value 'Clay didn\'t think he was the right dragon for a Big Heroic Destiny'

	# Define Prefix
		## Pull Client/Server Time
data modify storage suso.str:io in.prep set value '{"extra":[{"text":"/me <action>"}],"text":"['
		## Parse User Input String
#data modify storage suso.str:io in.prep set value 'Clay didn\'t think he was th'

	# Set Maximum Parse Length
data modify storage suso.str:io in.max_chars set value 8

	# Define Valid Characters in Parse
function suso.str:charsets/time

	# Prepare Announcement Command
data modify storage suso.str:io in.callback set value 'function onyx:load/temp'
function suso.str:call

#endregion