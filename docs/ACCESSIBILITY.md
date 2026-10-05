# ASEH Accessibility and Language Standard

**Status:** Approved\
**Approved by:** Gil on 2026-10-06 in [pull request #2](https://github.com/Gil-netizen/aseh/pull/2)\
**Launch languages:** Hebrew (primary, RTL) and English (LTR)\
**Target:** WCAG 2.2 AA where the criteria apply to native Android and generated documents

## Product commitment

ASEH must support prayer, study, practice, and community work without assuming a particular vision, hearing, mobility, dexterity, literacy, language, or cognitive profile. Accessibility alternatives carry the same dignity and product status as the default path.

Accessibility is an acceptance criterion for each feature. It is not deferred to a final audit.

## Language and directionality

- Interface language and source language are independent. A Hebrew interface can show English sources and an English interface can show Hebrew, Aramaic, or Judeo-Arabic sources.
- Hebrew is fully RTL and the primary launch experience. English is fully LTR. Navigation order, back direction, alignment, icons with direction, transitions, and layout mirror intentionally.
- Mixed-direction text uses explicit paragraph direction, Unicode isolation, and semantic spans. Do not construct sentences by concatenating localized fragments.
- Numbers, dates, citations, URLs, transliterations, and source identifiers remain readable in their natural direction without reordering punctuation.
- TalkBack reading order follows meaning, not visual coordinates. A source line and its translation expose their language and relationship.
- Missing translations remain visibly labeled; the app does not substitute or machine-translate silently.
- Exact Hebrew text is never baked into AI-generated imagery. Hebrew text, diagrams, icons, and UI assets are code/vector based and receive human language review.

## Text and visual presentation

- All essential text respects Android font scaling through at least 200% without clipping, overlap, lost controls, or forced horizontal scrolling.
- Content reflows for phones and tablets in portrait and supported landscape layouts. Long source titles, citations, names, and untranslated passages are test fixtures.
- Use Hebrew-appropriate and Latin-appropriate font families. The public alpha includes a user-selectable dyslexia-friendly Latin font option; selection persists across restart and never replaces Hebrew letterforms with unsuitable glyphs.
- Text and meaningful graphics meet WCAG AA contrast. Large text follows the applicable large-text threshold. Low-light service mode still meets contrast requirements.
- Color never carries status, editorial state, conflict, warning, or selection by itself. Pair it with text, shape, iconography, or pattern.
- Focus indicators remain visible in all themes. Selected and disabled states remain distinguishable at high contrast.
- Users can reduce or disable nonessential motion. No required information exists only in animation.

## Semantics and assistive technology

- Every actionable control has a concise accessible name, correct role, state, and purpose. Decorative images are excluded from the accessibility tree.
- Headings, lists, tables, quotations, citations, warnings, and steps expose their structure. A long source passage is navigable by meaningful sections.
- Custom Compose components must provide semantics equivalent to standard controls, including focus order, selected/expanded state, actions, and live-region behavior where appropriate.
- Dynamic changes do not steal focus. Errors identify the affected field, explain recovery, and are announced once.
- Charts, diagrams, calendars, maps, and rule traces have a structured text or table equivalent containing the same conclusion and relevant values.
- Licensed audio or voice reading has visible controls, captions/transcripts where applicable, rate control, and no autoplay.

## Interaction

- Every gesture has a visible button or menu alternative. Drag, swipe, pinch, long-press, hover, and timed gestures are never the only path.
- Touch targets are at least 48 by 48 dp, with larger primary service controls suitable for hurried or low-light use.
- Keyboard, switch access, and D-pad navigation follow a logical order and expose all actions.
- Time limits are avoided. When unavoidable, they are explained and extendable.
- Destructive and disclosure actions state the consequence, identify the affected data, and offer a safe cancel path.
- Authentication is not required for core use. A provider key entry flow supports password managers and paste without exposing the key to app logs or accessibility labels.

## Prayer, practice, and movement

- Embodied instructions such as standing, bowing, stepping, lifting, or moving include seated and limited-mobility alternatives without suggesting lesser spiritual value.
- Movement cues are independently switchable from text, translation, transliteration, audio, and explanatory notes.
- Leader, reader, gabbai, and congregant views use plain role names, stable control placement, large next/previous controls, and a recoverable current position.
- Service mode supports low light, screen-wake preferences, reduced motion, large type, and accidental-touch resistance without hiding emergency exit or accessibility controls.
- No-location, missing-text, unavailable-translation, offline, and calculation-uncertainty states explain what remains usable.

## High-consequence and privacy UX

- `CasePreparation` is visually and semantically distinct from a direct answer. The heading, warning, scope, missing facts, and required human roles are exposed to assistive technology before generated detail.
- Urgent danger messaging is concise, actionable, available fully offline, and never buried in an expandable section or delayed for a provider response.
- Provider disclosure preview is readable at 200%, navigable by heading, and identifies provider, model, excerpts, personal context, and history inclusion before consent.
- Secure display is active by default on credential, case, history, and payload-preview screens and does not block TalkBack or keyboard access. Sensitive notifications remain generic.
- Recognition and uncertainty labels use plain language in both launch languages and never rely on color alone.

## Documents and print

Generated packets must provide either a validated tagged PDF or a complete accessible HTML/structured-text equivalent distributed beside the PDF. An untagged PDF cannot be the only representation. The accessible representation must provide:

- correct document language and base direction;
- headings, lists, tables, descriptive link text, correct reading order, and language/direction metadata;
- selectable text rather than rasterized text;
- sufficient print contrast and non-color status indicators;
- page numbers, repeated table headings, and source/citation references that survive page breaks; and
- a plain structured-text equivalent for any visual diagram; and
- citation continuity across page breaks and usable reflow or zoom at 200%.

Print-only mode remains usable offline and does not load remote fonts, scripts, images, or analytics.

## Verification matrix

Each user-facing release is reviewed on at least:

- Android API 26 and the current target API;
- a small phone, representative phone, and tablet;
- Hebrew RTL and English LTR;
- mixed Hebrew/English/Aramaic content;
- default, 130%, 160%, and 200% font scaling;
- TalkBack with explore-by-touch and linear navigation;
- keyboard/switch-style navigation;
- light, dark, high-contrast, and low-light service themes;
- reduced motion;
- portrait and supported landscape;
- long text, missing translation, no location, offline, provider failure, and process restoration;
- representative printed and encrypted case packets;
- the dyslexia-friendly Latin option selected, persisted across restart, and combined with Hebrew and mixed-script content; and
- tagged-output or accessible-equivalent reading order, language/direction, selectable text, citations across page breaks, and 200% preview.

Automated checks support this matrix but do not replace human review by Hebrew and English readers using assistive technology.

## Release gate

A feature cannot ship when:

- an essential task is unreachable without vision, color perception, hearing, fine motor gestures, or standing movement;
- focus order, reading order, language, or directionality changes meaning;
- content clips or an essential control disappears at 200% text;
- an actionable control lacks name, role, state, or a non-gesture alternative;
- a visualization lacks an equivalent;
- the dyslexia-friendly Latin option is absent, does not persist, or corrupts Hebrew/mixed-script presentation;
- a generated packet lacks both validated tags and a complete accessible HTML/structured-text equivalent; or
- the rendered Hebrew or English has not received human review.

Accessibility defects are tracked with the affected language, direction, device class, font scale, assistive technology, and reproducible task.
