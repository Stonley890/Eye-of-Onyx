#region
	# Remove Base Datapack
function onyx:load/del/scbds
function onyx:load/del/storage

	# Announce Removal
		## Pre-Delete Message
tellraw @a[tag=Admin] {"text":"Disabling Eye of Onyx...","color":"red"}

		## Disable Datapack
datapack disable "file/Eye of Onyx"

		## Post-Delete Message
			### Admin Message
tellraw @a[tag=Admin] [{"text":"Eye of Onyx datapack disabled. Click ","color":"gold"},{"text":"here","color":"red","underlined":true,"clickEvent":{"action":"run_command","value":"/datapack enable \"file/Eye of Onyx\""}}," to re-enable it."]
			### Non-Admin Message
tellraw @a[tag=!Admin] {"text":"Eye of Onyx datapack disabled.","color":"gold"}

#endregion
