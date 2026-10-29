<p align="center"><a href="https://shoalapps.github.io/"><img src="https://raw.githubusercontent.com/ShoalApps/shoalapps.github.io/main/assets/readme/clownfish.svg" alt="Clownfish - a Shoal app, with the rest of the Shoal family" width="100%"></a></p>

A Material You app for short videos, part of [Shoal](https://shoalapps.github.io): YouTube Shorts,
TikTok and Instagram Reels in one calm, Material 3 Expressive feed.

Install `Clownfish.apk` from the newest [release](https://github.com/ShoalApps/clownfish/releases/latest).

## What it does

- **Three tabs, one app**: Shorts · TikTok · Reels along the top. Turn any of them off, and choose
  which one opens first.
- **YouTube Shorts**: a native feed built on NewPipeExtractor, like Swordfish, at the best
  quality (1080p and up), with YouTube, TikTok or Reels layouts, Return YouTube Dislike, DeArrow,
  comment likes, and per-visit and daily limits. Sign in to YouTube for likes and Shorts picked for you.
- **TikTok and Instagram Reels**: their own websites in the app, signed in with your own accounts on
  their own login pages (your password goes only to them). Phones get their mobile sites, tablets
  and Chromebooks their desktop ones. Each tab keeps its place, and only the one on screen plays.
- **Links**: open or share a Shorts, TikTok or Reels link with Clips and it lands in the right tab.
- **Settings like the other Shoal apps**: accounts for all three, sources, theme and wallpaper
  colours, Shorts options, screen time, updates, About, and hidden developer settings.

## Why the tabs aren't one blended feed

Mixing TikTok and Instagram videos into a single feed would mean scraping their private APIs, which
breaks their terms and which Meta in particular has taken to court. Showing their own websites, as a
browser does, keeps Clips on the right side of that while still putting everything in one place.

## Licence

Free software under the **GNU General Public License v3.0 or later**. Not affiliated with Google,
YouTube, TikTok, ByteDance, Instagram or Meta.

## Privacy and legal

No ads, no tracking, nothing sent to us unless you agree to anonymous error reports. See the [privacy policy](https://shoalapps.github.io/privacy.html) and [legal notices](https://shoalapps.github.io/legal.html).

## Support

Shoal is free, open source and has no ads. If you'd like to help it keep going, you can [support Shoal on Liberapay](https://liberapay.com/ShoalApps).

## Release notes

Update `app/src/main/assets/whats-new.json` with each release. Notes ship with the APK, appear once per installed version and can be reopened from Settings. Keep them specific to changes in that release.

## App language

The apps follow Android's system language by default, with an App language picker in Settings and right-to-left layout support. Translations are bundled and work offline. Coverage varies: many phrases are automatically translated, community catalogues may be partial, and missing strings fall back to British English. See `tools/README.md` and `app/src/main/assets/i18n/coverage.json` for maintenance and actual coverage.
