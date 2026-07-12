# UI_DESIGN_SYSTEM.md — B&B Video Editor
## Complete Visual Design Reference for AI Agents

> Every color, dimension, typography choice, and component spec is defined here.
> Use this file to implement ANY UI element in this app.
> Never hardcode colors or dimensions in Kotlin or XML — always reference these tokens.

---

## 1. Color Palette

### Brand Colors
```xml
<!-- Primary: Vibrant amber-orange — energetic, creative, warm -->
<color name="color_primary">#FFAF3F</color>
<color name="color_primary_variant">#E8943A</color>
<color name="color_on_primary">#1A0D00</color>

<!-- Secondary: Electric purple — premium, modern -->
<color name="color_secondary">#A97CFF</color>
<color name="color_secondary_variant">#8B5CF6</color>
<color name="color_on_secondary">#17003A</color>

<!-- Tertiary: Teal — balanced, fresh, used for audio waveforms -->
<color name="color_tertiary">#00D4AA</color>
<color name="color_on_tertiary">#00382D</color>

<!-- Error -->
<color name="color_error">#FF5252</color>
<color name="color_on_error">#FFFFFF</color>
```

### Surface Stack (Dark Mode — all surfaces are dark)
```xml
<!-- Background: near-black with subtle warm tint -->
<color name="color_background">#0D0C0E</color>

<!-- Surface: cards, bottom bars, dialogs -->
<color name="color_surface">#1A1920</color>

<!-- Surface Variant: input fields, chips, track lanes -->
<color name="color_surface_variant">#232231</color>

<!-- Surface Bright: elevated dialogs, overlays -->
<color name="color_surface_bright">#2D2C3C</color>
```

### Text Colors
```xml
<color name="color_on_background">#F0EFFF</color>
<color name="color_on_surface">#F0EFFF</color>
<color name="color_on_surface_variant">#A9A8C0</color>
```

### Utility Colors
```xml
<color name="color_outline">#3D3C52</color>
<color name="color_outline_variant">#2A293D</color>
<color name="color_badge_background">#B3000000</color>
<color name="color_selection_overlay">#66FFAF3F</color>
<color name="color_scrim">#CC0D0C0E</color>
<color name="color_editor_bottom_bar">#CC0D0C0E</color>
<color name="black">#FF000000</color>
<color name="white">#FFFFFFFF</color>
```

### Timeline Track Colors (match reference image)
```xml
<!-- Video track: use primary/thumbnail colors — no solid bg needed -->
<color name="color_track_video_bg">#1A1920</color>
<color name="color_track_video_border">#FFAF3F</color>

<!-- Audio track: teal -->
<color name="color_track_audio_bg">#0D2E28</color>
<color name="color_track_audio_waveform">#00D4AA</color>

<!-- Text track: warm brown/amber -->
<color name="color_track_text_bg">#2E1F00</color>
<color name="color_track_text_accent">#B8761F</color>

<!-- Overlay track: purple -->
<color name="color_track_overlay_bg">#1A1033</color>
<color name="color_track_overlay_accent">#A97CFF</color>

<!-- Effect track: teal/cyan -->
<color name="color_track_effect_bg">#001E2E</color>
<color name="color_track_effect_accent">#00B4D8</color>

<!-- Adjustment track: dark teal -->
<color name="color_track_adjustment_bg">#0A1E1E</color>
<color name="color_track_adjustment_accent">#00856F</color>
```

---

## 2. Typography

### Font
Primary font: **Inter** (load from Google Fonts via Downloadable Fonts)
Fallback: system sans-serif

Add to `res/font/inter_regular.xml`, `inter_medium.xml`, `inter_bold.xml`
Or use MaterialComponents' built-in font scale with the app's theme.

### Text Scale (Material3 Type System)
```xml
<!-- Use these @style references in all layouts: -->
TextAppearance.Material3.DisplayLarge    ← 57sp (splash screens only)
TextAppearance.Material3.HeadlineLarge   ← 32sp (section headers)
TextAppearance.Material3.HeadlineMedium  ← 28sp (screen titles)
TextAppearance.Material3.HeadlineSmall   ← 24sp (card titles)
TextAppearance.Material3.TitleLarge      ← 22sp (toolbar titles)
TextAppearance.Material3.TitleMedium     ← 16sp (list item titles)
TextAppearance.Material3.TitleSmall      ← 14sp (subtitle text)
TextAppearance.Material3.BodyLarge       ← 16sp (body text)
TextAppearance.Material3.BodyMedium      ← 14sp (secondary body)
TextAppearance.Material3.BodySmall       ← 12sp (captions)
TextAppearance.Material3.LabelLarge      ← 14sp (button labels)
TextAppearance.Material3.LabelMedium     ← 12sp (chip labels)
TextAppearance.Material3.LabelSmall      ← 11sp (badge text)
```

---

## 3. Spacing System (dimens.xml)

```xml
<!-- Spacing tokens -->
<dimen name="spacing_xs">4dp</dimen>
<dimen name="spacing_sm">8dp</dimen>
<dimen name="spacing_md">16dp</dimen>
<dimen name="spacing_lg">24dp</dimen>
<dimen name="spacing_xl">32dp</dimen>
<dimen name="spacing_xxl">48dp</dimen>

<!-- Corner radii -->
<dimen name="corner_sm">4dp</dimen>
<dimen name="corner_md">8dp</dimen>
<dimen name="corner_lg">12dp</dimen>
<dimen name="corner_xl">24dp</dimen>
<dimen name="corner_full">100dp</dimen>  <!-- pill shape -->

<!-- Icon sizes -->
<dimen name="icon_size_sm">16dp</dimen>
<dimen name="icon_size_md">24dp</dimen>
<dimen name="icon_size_lg">48dp</dimen>
<dimen name="icon_size_hero">96dp</dimen>

<!-- Media Grid -->
<dimen name="media_grid_spacing">1dp</dimen>
<dimen name="media_grid_item_size">120dp</dimen>
<dimen name="media_duration_badge_padding_h">4dp</dimen>
<dimen name="media_duration_badge_padding_v">2dp</dimen>
<dimen name="media_duration_badge_text_size">10sp</dimen>

<!-- Project Card -->
<dimen name="project_card_thumbnail_height">100dp</dimen>

<!-- Editor Timeline -->
<dimen name="timeline_clip_width">80dp</dimen>
<dimen name="timeline_clip_height">72dp</dimen>
<dimen name="timeline_track_height">56dp</dimen>     ← Phase 3: full multi-track
<dimen name="timeline_track_label_width">80dp</dimen>
<dimen name="timeline_ruler_height">24dp</dimen>
<dimen name="timeline_playhead_width">2dp</dimen>
<dimen name="timeline_trim_handle_width">12dp</dimen>
<dimen name="timeline_min_clip_width">32dp</dimen>

<!-- Editor Layout -->
<dimen name="editor_scrim_height">120dp</dimen>
<dimen name="editor_bottom_bar_height">56dp</dimen>
<dimen name="editor_context_toolbar_height">64dp</dimen>
<dimen name="playback_control_size">56dp</dimen>
<dimen name="playback_control_icon_size">24dp</dimen>

<!-- Waveform (Phase 4) -->
<dimen name="waveform_stroke_width">1.5dp</dimen>

<!-- Keyframe diamonds (Phase 7) -->
<dimen name="keyframe_diamond_size">10dp</dimen>
```

---

## 4. Drawable Requirements

### Required Drawables (create as vector XML in `res/drawable/`)

| Filename | Description | When Needed |
|---|---|---|
| `ic_arrow_back.xml` | ← back arrow | Phase 1 ✅ |
| `ic_play.xml` | ▶ play triangle | Phase 2 ✅ |
| `ic_pause.xml` | ⏸ pause two bars | Phase 2 ✅ |
| `ic_undo.xml` | ↩ undo arrow | Phase 3 |
| `ic_redo.xml` | ↪ redo arrow | Phase 3 |
| `ic_split.xml` | ✂ split/scissors | Phase 3 |
| `ic_trim.xml` | ▐▌ trim handles | Phase 3 |
| `ic_speed.xml` | ⏩ speed/speedometer | Phase 3 |
| `ic_delete.xml` | 🗑 trash bin | Phase 3 |
| `ic_add.xml` | + plus circle | Phase 3 |
| `ic_skip_start.xml` | ⏮ skip to start | Phase 3 |
| `ic_rewind.xml` | ⏪ rewind | Phase 3 |
| `ic_fast_forward.xml` | ⏩ fast forward | Phase 3 |
| `ic_fullscreen.xml` | ⛶ fullscreen expand | Phase 3 |
| `ic_track_video.xml` | 🎬 film frame icon | Phase 3 |
| `ic_track_audio.xml` | 🎵 music note | Phase 4 |
| `ic_track_text.xml` | 𝐓 text T icon | Phase 5 |
| `ic_track_overlay.xml` | 📷 overlay icon | Phase 5 |
| `ic_track_effect.xml` | ✨ sparkle icon | Phase 7 |
| `ic_track_adjustment.xml` | 🎨 sliders icon | Phase 7 |
| `ic_eye_visible.xml` | 👁 eye open | Phase 3 |
| `ic_eye_hidden.xml` | 👁‍🗨 eye closed | Phase 3 |
| `ic_lock.xml` | 🔒 lock | Phase 3 |
| `ic_unlock.xml` | 🔓 unlock | Phase 3 |
| `ic_volume.xml` | 🔊 volume | Phase 4 |
| `ic_filter.xml` | 🎨 filter/magic wand | Phase 5 |
| `ic_adjust.xml` | ⚙ sliders adjust | Phase 5 |
| `ic_export.xml` | ↑ export/upload | Phase 6 |
| `ic_keyframe.xml` | ◆ diamond keyframe | Phase 7 |

### Background Drawables

| Filename | Description |
|---|---|
| `bg_editor_top_scrim.xml` | Top-to-transparent black gradient (Phase 2 ✅) |
| `bg_clip_active_border.xml` | Primary color 2dp stroke border (Phase 2 ✅) |
| `bg_duration_badge.xml` | Semi-transparent rounded rect for time badges |
| `bg_clip_video.xml` | Rounded rect, color_surface with primary border |
| `bg_clip_audio.xml` | Rounded rect, color_track_audio_bg |
| `bg_clip_text.xml` | Rounded rect, color_track_text_bg |
| `bg_clip_overlay.xml` | Rounded rect, color_track_overlay_bg |
| `bg_clip_effect.xml` | Rounded rect, color_track_effect_bg |
| `bg_clip_adjustment.xml` | Rounded rect, color_track_adjustment_bg |
| `bg_track_selected.xml` | Highlight when track is selected |
| `bg_playhead.xml` | White 2dp rect for playhead line |

---

## 5. Screen-by-Screen Layout Specs

### 5.1 Home Screen (`fragment_home.xml`)

```
┌──────────────────────────────────────┐
│ [B&B Video Editor]        [📷 Assets]│  AppBar with gallery menu
├──────────────────────────────────────┤
│                                      │
│  (empty state):                      │
│     [Hero Icon 96dp]                 │
│     "Start Creating"  ← HeadlineSmall│
│     "Import videos..." ← BodyMedium  │
│     [New Project Button]             │
│                                      │
│  (content state):                    │
│  ┌────────┐  ┌────────┐             │
│  │thumbnail│  │thumbnail│  ← 2 cols  │
│  │ name   │  │ name   │  StaggeredGrid│
│  │ 1:30   │  │ 0:45   │             │
│  └────────┘  └────────┘             │
│  ┌────────┐  ┌────────┐             │
│  │...     │  │...     │             │
│  └────────┘  └────────┘             │
│                                      │
└──────────────────────────────────────┘
                               [+ FAB]  ← Shown when projects exist
```

**RecyclerView**: `GridLayoutManager(context, 2)` with `SpacingItemDecoration(4dp)`
**Project card long press**: shows AlertDialog with "Delete" option

### 5.2 Media Import Screen (`fragment_media_import.xml`)

```
┌──────────────────────────────────────┐
│ [←]        Select Media              │  Toolbar
├──────────────────────────────────────┤
│ [All] [Videos] [Photos]              │  Filter Chips (ChipGroup)
├──────────────────────────────────────┤
│ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌──┐│
│ │img│ │img│ │img│ │img│ │img│ │i │ │  3-column grid
│ │0:30│ │   │ │1:20│ │   │ │   │ │ │ │  120dp items
│ └───┘ └───┘ └───┘ └───┘ └───┘ └──┘│
│  (selected item shows primary border + checkmark overlay)
│  (loading state: CircularProgressIndicator center)
│  (permission state: icon + title + grant button)
└──────────────────────────────────────┘
                     [Add X  ──────>]   ← ExtendedFAB (shows count)
```

**Multi-select**: tap item → blue border + checkmark badge.
**FAB text**: `getString(R.string.fab_add_count, selectedCount)` e.g. "Add 3"
**Creating overlay**: full-screen semi-transparent scrim + progress spinner + "Creating project..."

### 5.3 Project Detail / Editor Screen (`fragment_project_detail.xml`)

This is the **main editor screen**. It is the most complex layout.

```
┌──────────────────────────────────────┐  PHASE 2 (current)
│ [←] [↩] [↪]  [1080P▼]  [EXPORT]   │  Toolbar
├──────────────────────────────────────┤
│                                      │
│         VIDEO PREVIEW                │  SurfaceView (black bg)
│         (16:9 aspect ratio)          │  weight: 1 or fixed height
│                                      │
│  [▶] 00:06 / 00:24        [⛶]       │  Overlay row
├──────────────────────────────────────┤
│  [+] [⏮] [⏪] [▶/⏸] [⏩] [↻] [⟷] [⛶]│  Playback controls row
├──────────────────────────────────────┤  PHASE 3+
│ 00:00  00:04  00:06  00:08  00:12  │  Timeline ruler (custom View)
│              🔴                     │  Playhead (white line)
├─────────┬────────────────────────────┤
│ 🎬 Video │ [clip][clip][clip]    [+] │  Track lanes
│  👁 🔒   │                           │
├─────────┼────────────────────────────┤
│ 🎵 Audio │ [~~waveform~~]        [+] │
├─────────┼────────────────────────────┤
│ 𝐓 Text  │ [TITLE TEXT]          [+] │
├─────────┼────────────────────────────┤
│📷 Overlay│ [img clip]            [+] │
├─────────┴────────────────────────────┤
│ [Split][Trim][Speed][Volume][Filter] │  Context toolbar
└──────────────────────────────────────┘
```

**Implementation notes:**
- Preview area: `SurfaceView` fills width. AspectRatio is maintained by `ConstraintLayout` 16:9 constraint
- Timeline section: `NestedScrollView` (vertical) wrapping `HorizontalScrollView` (horizontal) containing all tracks
- Track label column: fixed 80dp width, does NOT scroll horizontally
- Track content: scrolls horizontally in sync (linked RecyclerViews or custom ScrollSync)
- Playhead: drawn as an overlay above the track area; absolute position calculated from `currentPositionMs / pixelsPerMs`

### 5.4 Asset Library Screen (`fragment_asset_library.xml`)

```
┌──────────────────────────────────────┐
│ [←]         Asset Library            │  Toolbar
├──────────────────────────────────────┤
│ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌──┐│
│ │img│ │img│ │img│ │img│ │img│ │i │ │  3-column grid
│ │0:30│ │   │ │1:20│ │   │ │   │ │ │ │
│ └───┘ └───┘ └───┘ └───┘ └───┘ └──┘│
└──────────────────────────────────────┘
```

Same grid style as Media Import but read-only (no selection).

---

## 6. Component Specs

### 6.1 Project Card (`item_project_card.xml`)

```xml
MaterialCardView
├── ImageView (id: projectThumbnail)   ← 16:9, scaleType=centerCrop
└── LinearLayout (padding: 8dp)
    ├── TextView (id: projectName)     ← LabelLarge, single line, ellipsize
    └── LinearLayout (horizontal)
        ├── TextView (id: projectDuration) ← LabelSmall, color_primary
        └── TextView (id: projectDate)     ← LabelSmall, color_on_surface_variant
```

Card: `cardCornerRadius=8dp`, `cardElevation=0dp`, `strokeWidth=1dp color_outline_variant`

### 6.2 Media Grid Item (`item_media_grid.xml`)

```xml
FrameLayout (size: 120dp × 120dp)
├── ImageView (id: thumbnail)         ← centerCrop, loads via Coil
├── CheckBox overlay (id: checkOverlay) ← top-right, shown when selected
├── TextView (id: videoBadge)         ← bottom-left, "0:30" style
│   background: bg_duration_badge, gone for images
└── View selection border (id: selectionBorder) ← primary color, 2dp, gone unless selected
```

### 6.3 Timeline Clip (`item_timeline_clip.xml`) — Phase 2 Read-Only

```xml
FrameLayout (width: 80dp, height: 72dp)
├── ImageView (id: clipThumbnail)     ← video frame at trimStart
├── TextView (id: clipName)           ← bottom-left, 10sp
└── View (id: activeBorder)           ← bg_clip_active_border, gone by default
```

### 6.4 Timeline Clip — Phase 3 (Full Editing)
```xml
ConstraintLayout (width: calculated from duration, height: timeline_track_height)
├── ImageView (left: clip_thumbnail)              ← first-frame preview
├── View (id: leftTrimHandle)                     ← 12dp wide, draggable
│   background: bg_trim_handle (primary rounded)
├── View (id: rightTrimHandle)                    ← 12dp wide, draggable
│   background: bg_trim_handle (primary rounded)
├── TextView (id: clipDurationText)               ← centered, "4.8s" style
└── View (id: activeBorder)                       ← shown when selected
```

### 6.5 Waveform View (Phase 4 Custom View)

```
WaveformView extends View:
- onDraw(): iterate samples[], draw vertical lines with Canvas.drawLine()
- Color: color_track_audio_waveform (#00D4AA)
- Line height: sample[i] * (height / 2), centered vertically
- strokeWidth: waveform_stroke_width (1.5dp)
- scrollOffset: synchronized with HorizontalScrollView
```

### 6.6 Timeline Ruler (Phase 3 Custom View)

```
TimelineRulerView extends View:
- onDraw(): draw time labels every N pixels depending on zoom
- Major ticks every 1s, minor ticks every 250ms
- Text: "00:04", "00:08" etc (white, LabelSmall)
- Background: color_surface
- Synchronized with timeline HorizontalScrollView via listener
```

### 6.7 Bottom Context Toolbar (Phase 3)

```xml
HorizontalScrollView (id: contextToolbar, layout_height: 64dp)
└── LinearLayout (horizontal, gravity: center_vertical)
    ├── [Split]   ← icon + label column button
    ├── [Trim]
    ├── [Speed]
    ├── [Volume]  ← Phase 4
    ├── [Filter]  ← Phase 5
    ├── [Adjust]  ← Phase 5
    └── [Delete]  ← tinted color_error
```

Each button: `MaterialButton` style `Widget.Material3.Button.IconButton.Filled.Tonal`
Layout: 60dp × 64dp, icon 24dp above, label 12sp below

---

## 7. Animations & Transitions

### Screen Transitions (already defined in anim/)
- `slide_in_right.xml` → entering screen slides from right
- `slide_out_left.xml` → exiting screen slides to left
- `slide_in_left.xml` → pop enter (back)
- `slide_out_right.xml` → pop exit (back)

### Phase 3+ Micro-animations

**Playhead seek**: animate `translationX` on playhead view, 16ms tick via `Handler.postDelayed`
**Trim handle drag**: `OnTouchListener` → update clip width in real-time → `ObjectAnimator` none (direct)
**Clip selection**: `ScaleAnimation` 0.95x → 1.0x + `AlphaAnimation` on border (100ms)
**Track collapse/expand**: `ValueAnimator` height from `timeline_track_height` to 0 (200ms)
**Context toolbar appear/hide**: `TranslateAnimation` from bottom 64dp → 0 (200ms, easeOutQuart)
**Export progress**: `ProgressBar` indeterminate → `CircularProgressIndicator` determinate with value

### FAB State Transitions
```kotlin
// When selection count changes:
if (count == 0) fabAddMedia.hide()   // FAB.hide() handles the scale-out animation
else {
    fabAddMedia.text = getString(R.string.fab_add_count, count)
    fabAddMedia.show()               // FAB.show() handles the scale-in animation
}
```

---

## 8. Material3 Theme

### `res/values/themes.xml`
```xml
<style name="Theme.BBVideoEditor" parent="Theme.Material3.Dark">
    <item name="colorPrimary">@color/color_primary</item>
    <item name="colorOnPrimary">@color/color_on_primary</item>
    <item name="colorPrimaryContainer">@color/color_primary_variant</item>
    <item name="colorSecondary">@color/color_secondary</item>
    <item name="colorOnSecondary">@color/color_on_secondary</item>
    <item name="colorTertiary">@color/color_tertiary</item>
    <item name="colorOnTertiary">@color/color_on_tertiary</item>
    <item name="colorError">@color/color_error</item>
    <item name="colorSurface">@color/color_surface</item>
    <item name="colorOnSurface">@color/color_on_surface</item>
    <item name="colorSurfaceVariant">@color/color_surface_variant</item>
    <item name="colorOnSurfaceVariant">@color/color_on_surface_variant</item>
    <item name="colorOutline">@color/color_outline</item>
    <item name="android:colorBackground">@color/color_background</item>
    <item name="android:windowBackground">@color/color_background</item>
    <item name="shapeAppearanceMediumComponent">@style/ShapeAppearance.BBVideoEditor.Medium</item>
</style>

<style name="ShapeAppearance.BBVideoEditor.Medium" parent="">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">8dp</item>
</style>
```

---

## 9. Export Screen Design (Phase 6)

```
┌──────────────────────────────────────┐
│ [←]            Export                │
├──────────────────────────────────────┤
│  Resolution                          │
│  ○ 480p   ○ 720p   ● 1080p   ○ 4K  │  RadioGroup
├──────────────────────────────────────┤
│  Quality                             │
│  ○ Low    ○ Medium  ● High  ○ Max   │
├──────────────────────────────────────┤
│  Frame Rate                          │
│  ○ 24fps  ● 30fps   ○ 60fps         │
├──────────────────────────────────────┤
│  Estimated size: ~240MB              │  Body text, updates live
├──────────────────────────────────────┤
│          [Export Video]              │  Primary button, full width
│          [Share Preview]             │  Tonal button, full width
└──────────────────────────────────────┘
```

### Export Progress Screen
```
┌──────────────────────────────────────┐
│           Exporting...               │
│                                      │
│       [CircularProgressIndicator]    │  Determinate, primary color
│       67% — 00:18 remaining          │
│                                      │
│             [Cancel]                 │
└──────────────────────────────────────┘
```
