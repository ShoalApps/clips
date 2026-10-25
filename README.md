# Tintloom Clips

A Material You app for short videos, part of [Tintloom](https://tintloom.github.io): YouTube Shorts,
TikTok and Instagram Reels in one calm, Material 3 Expressive feed.

**Status: planned.** This repository holds the design; the app is being built next.

## Plan

- **YouTube Shorts feed**: built on NewPipeExtractor, like Tintloom Video, with the same three-player
  preloading, layouts (YouTube, TikTok and Reels styles), per-visit and daily limits, SponsorBlock-free
  playback and Return YouTube Dislike counts.
- **TikTok and Reels links**: shared or opened links play through TikTok's and Instagram's official
  embed players, which both companies offer to other apps.
- **One feed, many sources**: each source is a plug-in, so new ones can be added without touching the UI.
- **Shared Tintloom pieces**: wallpaper colours with the mauve fallback, the adaptive navigation bar and
  rail, segmented settings, the GitHub updater and the credits.

## Not planned without a decision

Full TikTok and Instagram feeds would mean scraping their private APIs, which breaks their terms and
which Meta in particular has taken to court. Tintloom avoids that unless an official route exists.
For a patched official TikTok app, see [Hushfeed](https://github.com/SysAdminDoc/HushFeed), a Morphe
patch bundle you apply yourself.

## Licence

Free software under the **GNU General Public License v3.0 or later**. Not affiliated with Google,
YouTube, TikTok, ByteDance, Instagram or Meta.
