# PROJECT CONTEXT

## Project

An Android IME that makes writing Markdown on a phone practical: list
continuation and renumbering, indent/outdent, inline style toggles, headings, quotes, and
skeleton insertions for links/images/tables/code blocks.

## Layout and commands

The Gradle root is **`src/`**, not the repo root. Run everything from there.

- `src/app/src/test/` — plain JUnit on the host JVM. `editor/` has **no Android imports** and is
  fully testable here; `ime/` needs only a fake `InputConnection`, still no emulator.
- `src/app/src/androidTest/` — instrumented; nothing real lives here and nothing planned needs it.
- Package root: `com.creategoodthings.markdownKeyboard`. minSdk 31, compile/target 36, JVM target 11.
- Compose UI, Material 3, version catalog at `src/gradle/libs.versions.toml`.
- Installing the keyboard on a device also requires enabling it in Android settings; `MainActivity`
  (`ui/MainPage.kt`) is just the companion app that points there.

## Architecture

 all markdown behaviour is a pure function
`(text, cursor, key) -> edit`, with no Android types anywhere in `editor/`.

The pipeline for one keypress:

```
KeyItem (ui/) ──KeyAction──▶ MdIMEService ──▶ MarkdownEngine.contextNeed(action)
                                                       │
                              SnapshotReader.read ◀────┘   (one InputConnection read, exactly sized)
                                     │
                                  Snapshot ──▶ MarkdownEngine.edit ──▶ TextEdit? ──▶ EditApplier
```

### `editor/` — the pure core

- **`KeyAction`** — what a key *means*. The UI emits these and knows nothing else about markdown.
- **`ContextNeed`** — how much document a rule needs (`None`, `CurrentLine`, `EnclosingBlock`,
  `Window`). Declared up front so the reader fetches the right amount *once*. `None` means the key
  commits blind, so ordinary typing costs no round trip to the host app.
- **`Snapshot`** — an immutable *window* of text around the cursor, read fresh every contextual
  keypress. There is deliberately **no mirror of the field**: the user taps to move the caret, the
  host autocorrects, another keyboard types. `reachedStart`/`reachedEnd` say whether the window hit
  the real document edges; rules that would rewrite unseen text are supposed to degrade rather than
  guess (see Known gaps).
- **`TextEdit`** — one replacement range plus the resulting selection. `replacingRegion()` trims the
  unchanged head/tail so a five-line renumber becomes the two lines that moved (avoids flicker and
  flattening the host app's undo history), then widens back out so **the edited span always contains
  the caret**. `EditApplier` depends on that property; don't break it.
- **`MarkdownSyntax`** — the single place deciding which markdown the keyboard *writes* (4-space
  indent, `-` bullets, `.` delimiter, headings cycle to level 3). Parsing is deliberately more
  permissive than emission.
- **`rules/`** — one `KeyHandler` per key family, tried in the order listed in `MarkdownEngine`.
  A handler claims an action by returning non-null from `contextNeed`. `handle` returning null means
  "nothing to do" and is a legitimate outcome (`MdIMEService` then falls back to raw editor behaviour
  for Backspace/Enter).
- **`document/`** — the line/list model. `LineParser` classifies each line independently;
  `ListBlocks.at()` lifts the contiguous run of list lines around the caret into a `ListBlock`;
  `InlineSpans` locates paired `**`/`*`/`` ` ``/`~~` delimiters within a line.

### The list pipeline

Every list key (Enter, Backspace, indent, toggles) goes through the same four steps:
**mutate the `ListBlock` → `normalized()` → `render()` → diff via `ListEdits.commit`.**
Renumbering therefore lives in exactly one place (`ListBlock.normalized()`) and runs
unconditionally for all of them — there is no separate mid-list code path. When adding a list
behaviour, add a `ListBlock` mutation and route it through `ListEdits.commit`; don't hand-build a
`TextEdit`.

Nesting levels are inferred from the *sequence* of indent widths, not by dividing by a fixed unit,
so a two-space Obsidian list nests correctly even though we emit four spaces.

### `ime/` — the Android seam

Three small files, and the only place Android types appear alongside editor types.
`SnapshotReader` widens its window (512 → 2048 → 8192) until the enclosing list fits.
`EditApplier` works in cursor-relative terms and positions the caret by splitting the commit
(`commitText(head, 1)` then `commitText(tail, 0)`); absolute offsets are used only to restore a
multi-character selection, and only when `windowStart >= 0`.

### `ui/`

`MarkdownKeyboard` is the shell: two markdown rows that never change, then one of four character
pages (`KeyboardPage` — letters, `?123`, `=\<`, the number pad), each in its own `*Rows.kt` file
and each four rows tall, so the keyboard is always six rows and never resizes when the page
changes. Shared row helpers live in `KeyRows.kt`.

Page and shift are UI state held in `MarkdownKeyboard`. Neither has a `KeyAction`: those keys
carry `Noop` and do their work in `Key`'s `onClick`, which is what keeps page and case out of
`editor/`. Actions are passed into `ComposeMdKeyboardView` rather than pulled from context, so the
keyboard composes in a preview without a service behind it.

`Key` runs one `pointerInput` gesture loop — tap, long press, key repeat (on the main dispatcher;
`InputConnection` is not safe off it) and the slide-to-pick alternates strip — because two gesture
detectors on one node fight over the pointer. It therefore has to supply its own press state
(`PressInteraction` into a `MutableInteractionSource`) and its own click semantics for TalkBack,
which `combinedClickable` used to give for free. The strip is drawn in-tree as the last child of
the keyboard's box, not in a `Popup`: Android delivers a whole gesture to the window that saw the
first touch, so a popup window could never see the sliding finger anyway. `AlternatesGeometry` is
the placement and hit-testing maths, kept free of Android and Compose types so it can be tested on
the host JVM.

## Testing

`Keys.kt` is the harness: `assertKey(action, before, after)` over strings that mark the caret with
`▮` and a selection with `«…»`. Every case it runs is checked three ways at once — the pure edit,
the invariant that the edited span contains the caret, and a replay of the same edit through
`EditApplier` against `FakeInputConnection`. That last one is the highest-value assertion in the
suite: it is the only thing tying the pure layer to a real editor. `assertPureKey` skips the last
two, and is only for the cases in `KnownLimitationsTest` that knowingly break containment.

Where things live: one test class per handler in `test/…/editor/rules/`, one per model type in
`editor/document/`, the two Android-free UI state machines in `test/…/ui/`
(`ShiftStateTest`, `AlternatesGeometryTest` — there is no Compose UI test infrastructure, so
anything the keyboard gets wrong on screen should be pushed down into one of those and pinned
there), `TruncationTest` for windows that did not reach the document edges, and
`KnownLimitationsTest` for behaviour that is wrong-but-recorded. A bug reported against the
keyboard is written into the matching rule class as the behaviour that is wanted.

WHEN A USER-REPORTED UX BUG HAS BEEN FIXED OR IT SHOULD ALWAYS BE FOLLOWED UP WITH
TESTS THAT MAKE SURE AN REGRESSION ISNT INTRODUCED LATER.

## Known gaps (all pinned by a test, mostly in `KnownLimitationsTest`)

- `reachedStart`/`reachedEnd` are consulted by exactly two rules — the code key's fence check and
  backspace on a table skeleton. Every list rule still ignores them, so a list longer than the
  widest window gets renumbered from whatever was visible (`TruncationTest`).
- `LineParser` classifies each line alone, so a `- item` inside a code fence still parses as a
  list. `InlineStyleHandler` counts fences itself for the one case that needed it.
- Indent/outdent acts on the caret's line only, and with a multi-line selection outside a list the
  applier deletes the selection first — the lines below the first are lost.
- `ToggleQuote` handles a single `> ` prefix, with no nesting.
- The emoji key is `Noop`; there is no picker behind it.
- The alternates strip is one row wide, so a key cannot offer more entries than fit across the
  keyboard; `AlternatesHostState.open` refuses rather than truncating, and the key falls back to
  firing its own action on hold.
