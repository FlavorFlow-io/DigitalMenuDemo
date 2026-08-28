# Play Console declarations

## `data-safety.csv`

The Data safety answers, in the CSV format Play Console imports. Lives with the
app because it describes what *this codebase* collects — every white-label
client is built from it, so the declaration is the same for all of them.

**Check this before you rely on it.** It is a statement to Google about your
app's data practices, and it was seeded from a sample rather than derived from
this code. Export the real one from Play Console once you have answered the
questionnaire by hand for one app (Data safety → Export to CSV), and replace
this file.

The build passes its path to the automation, which imports it rather than
answering the questionnaire click by click.
