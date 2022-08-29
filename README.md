# atsSignTeleport

The plugin allows to place special sign in the world, that will teleport anyone who interacts with it into desired by
its creator spot in the desired.

## Details

You can define in the `config.yml` file, how each of lines should looks like after successful sign creation. Only
limitation is: There have to be exactly 4 lines. Any one more will be ignored, any one less will cause plugin crash.
Coordinates that can be placed (as placeholders) anywhere in the lines aren't obligatory, target is saved in sign's
metadata.

An example config is present as a default config.
