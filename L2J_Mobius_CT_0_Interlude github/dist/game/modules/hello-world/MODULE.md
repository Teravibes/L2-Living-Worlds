# Hello World module

The reference module for the server module framework. It exists to prove the framework works end to end, and to
serve as the smallest complete example for anyone writing their own module.

## What it does

When enabled, it registers one voiced command:

- `.hello` replies to the player with the greeting from `config/module.ini`.

That is all. It touches no stock class and owns everything under this directory.

## Enable it

1. Open `config/module.ini`.
2. Set `Enabled = True`.
3. Restart the server.

Type `.hello` in game and you should see the greeting.

## Disable it

Set `Enabled = False` in `config/module.ini` and restart. The command is no longer registered and the server is
stock.

## Remove it

While the server is stopped or the module is disabled, delete this whole `hello-world` directory. Nothing it owns
lives anywhere else, so removal is a clean delete. The module has no database tables.
