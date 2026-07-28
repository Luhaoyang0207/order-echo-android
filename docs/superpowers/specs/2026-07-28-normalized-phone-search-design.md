# Normalized Phone-number Search

**Status:** Approved for implementation planning  
**Date:** 2026-07-28

## Goal

Let restaurant staff find a recording by entering a phone-number digit sequence even when the Huawei filename displays the number with spaces or other separators.

For example, searching for `12312123` must find a recording displayed as `123 12 123`.

## Scope

- Search-only behavior in the recording list.
- No recording file is renamed, moved, or otherwise modified.
- The UI continues to display the original parsed phone number exactly as it appears in the Huawei recording filename.
- Existing empty-search, no-match, date-expansion, playback, cleanup, and deletion behavior remains unchanged.
- Move the Refresh action beside the `通话录音 · X 条` header.
- Replace the current button beside the search field with an explicit `搜索` action.

## Matching rule

Before comparison, normalize both the entered query and a recording's phone number by retaining only decimal digits (`0` through `9`).

The normalized query is then matched as a contiguous substring of the normalized phone number.

Examples:

| Displayed number | Entered search | Result |
| --- | --- | --- |
| `123 12 123` | `12312123` | Match |
| `123-12-123` | `123 12 123` | Match |
| `+47 (123) 45 678` | `471234` | Match |

If the user enters only separators and no digits, treat it as an empty query and show the normal unfiltered Today-first list.

## Design

Add a small pure normalization helper alongside the recording-list ViewModel. It accepts a nullable string and returns only its ASCII digits. The ViewModel stores the original trimmed text only as needed for the UI, but filters recordings by the normalized query and normalized `phoneNumber` value.

Search-result date expansion continues to be based on the filtered matching recordings. No repository scan, media metadata extraction, storage operation, or permission behavior changes.

The recording header becomes a horizontal row: `通话录音 · X 条` on the left and `刷新` on the right. The next row remains the phone-number input, with `搜索` on its right. Tapping `搜索` and submitting from the phone keyboard both call the same query action. `刷新` remains a full recording-directory refresh and does not apply a new query.

## Error handling

- A recording without a phone number never matches a non-empty normalized search.
- Non-digit formatting characters are ignored safely.
- A digits-only query with no matches continues to show the existing no-match state.

## Verification

- A unit test proves digits-only input matches a displayed number containing spaces.
- A unit test proves common separators are ignored on both the query and stored number.
- A unit test proves separator-only input restores the normal unfiltered list.
- An instrumentation test confirms the recording screen exposes `刷新` beside the count header and `搜索` beside the input, and that the two actions retain their respective refresh/search behavior.
- Run the focused ViewModel tests and the full unit-test/build command.
