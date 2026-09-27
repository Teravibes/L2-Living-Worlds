# Testing the Alt Companion module

You need two characters on the server, on any accounts. Below, Main is the one you play and Alt is the one you summon.

1. Start the server and look for `Alt Companion module enabled, registered voiced command .alt` in the console.
2. Log in with Main and type `.alt Alt`. Alt appears next to you and joins your party.
3. Hunt for a while. Alt follows, fights or supports by its class, and gains experience.
4. Whisper Alt `attack freely`, then `assist`. It switches between hunting on its own and assisting you.
5. Remove Alt from the party. Main sees `Alt left your party and was saved.`
6. Log in to Alt's account. It has the level, experience and items it had when it left, minus the shots and potions it
   used.

Refusals to check:

- `.alt` with an unknown name, with a phantom's name, and with the name of a character that is logged in.
- `.alt Alt` while Alt is already in your party.
- `.alt Alt` while you are in someone else's party, or in a full party.

Edge cases to check:

- Log out Main while Alt is in the party. Alt is saved and removed.
- Let Alt die and do not resurrect it. After about a minute it is removed; it is dead when you next log in to it.
- While Alt is in the party, log in to its account from a second client. The summoned copy disappears and the login
  works normally.
