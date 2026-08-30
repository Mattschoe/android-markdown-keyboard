package com.creategoodthings.markdownKeyboard.editor.rules

import com.creategoodthings.markdownKeyboard.Marked.NO_EDIT
import com.creategoodthings.markdownKeyboard.assertKey
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import org.junit.Test

/** Indent and outdent, on a list and off it. */
class IndentTest {

    private fun indent(before: String, after: String) =
        assertKey(KeyAction.IndentForward, before, after)

    private fun outdent(before: String, after: String) =
        assertKey(KeyAction.IndentBack, before, after)

    // ---- verified against the old implementation during the refactor ----

    @Test fun indentingRenumbersBothLevels() =
        indent("1. a\n2. ▮b\n3. c", "1. a\n    1. ▮b\n2. c")

    @Test fun outdentingRenumbersBothLevels() =
        outdent("1. a\n    1. ▮b\n2. c", "1. a\n2. ▮b\n3. c")

    @Test fun theFirstItemCannotIndent() = indent("- ▮a", NO_EDIT)

    @Test fun aTopLevelItemCannotOutdent() = outdent("- a\n- ▮b", NO_EDIT)

    @Test fun offAListItIsPlainWhitespace() = indent("he▮llo", "    he▮llo")

    @Test fun outdentingCarriesChildrenAlong() =
        outdent("- a\n    - ▮b\n        - c", "- a\n- ▮b\n    - c")

    // ---- further cases ----

    @Test fun anItemCannotSkipALevel() {
        indent("- a\n- ▮b", "- a\n    - ▮b")
        indent("- a\n    - ▮b", NO_EDIT)
    }

    @Test fun outdentingOffAListRemovesUpToOneLevel() = outdent("        he▮llo", "    he▮llo")

    @Test fun outdentingAPartiallyIndentedLineTakesWhatIsThere() = outdent("  he▮llo", "he▮llo")

    @Test fun outdentingAnUnindentedPlainLineDoesNothing() = outdent("he▮llo", NO_EDIT)

    @Test fun indentingATaskKeepsItsCheckedState() =
        indent("- [x] a\n- [x] ▮b", "- [x] a\n    - [x] ▮b")

    @Test fun aTwoSpaceListIsNormalisedToFourOnIndent() =
        indent("- a\n  - b\n  - ▮c", "- a\n    - b\n        - ▮c")
}
