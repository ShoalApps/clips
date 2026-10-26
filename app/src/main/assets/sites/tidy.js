// Clownfish: makes TikTok's and Instagram's websites feel like their apps by removing the
// prompts web visitors get (open the app, get the app, sign-up nags, cookie banners). Optional
// cookies are declined. Runs on every page of the two sites; does nothing anywhere else.
(function () {
  if (window.__pufferfishTidy) return;
  window.__pufferfishTidy = true;
  const host = location.hostname;
  const site = host.endsWith("tiktok.com") ? "tiktok" : host.endsWith("instagram.com") ? "instagram" : null;
  if (!site) return;

  // Things to hide outright.
  const css = {
    tiktok: `
      [class*="DivAppPromo"], [class*="BottomBannerContainer"], [class*="DivOpenAppContainer"],
      [data-e2e="top-bar-open-app"], [data-e2e="open-app-button"], [class*="DivDownloadAppContainer"],
      [class*="DivGetAppContainer"], [class*="ButtonOpenApp"], tiktok-cookie-banner,
      [class*="DivHeaderGetAppContainer"], a[href*="download"][class*="Button"] { display: none !important; }
      html, body { overscroll-behavior: none; }`,
    instagram: `
      a[href*="/accounts/emailsignup"][role="link"] ~ div[role="dialog"],
      div[role="contentinfo"], a[href^="https://apps.apple.com"], a[href*="play.google.com"],
      section > nav ~ div:has(a[href*="emailsignup"]) { display: none !important; }
      html, body { overscroll-behavior: none; }`,
  }[site];
  const style = document.createElement("style");
  style.textContent = css;
  (document.head || document.documentElement).appendChild(style);

  // Buttons to press away when they appear: matched by their words, which survive redesigns
  // better than class names.
  const dismiss = [
    /^not now$/i, /^decline optional cookies$/i, /^refuse non-essential cookies$/i, /^decline$/i,
    /^only allow essential cookies$/i, /^decline optional cookies$/i, /^maybe later$/i, /^cancel$/i,
  ];
  const nags = /open (the )?(tiktok|instagram|app)|get (the )?(full )?app|use the app|see more on the app|switch to the app/i;

  function tidy(root) {
    for (const el of root.querySelectorAll('button, [role="button"]')) {
      const t = (el.innerText || "").trim();
      if (!t || t.length > 40) continue;
      // Only inside dialogs and banners, so ordinary buttons on the page are never pressed.
      const inPopup = el.closest('[role="dialog"], [class*="Modal"], [class*="modal"], [class*="Banner"], [class*="banner"], [class*="cookie"], [class*="Cookie"]');
      if (inPopup && dismiss.some((r) => r.test(t))) el.click();
    }
    // Guest login prompts stay: skipping TikTok's leads to its age check, which is the site's to ask.
    // App and coin promos in the top bar.
    for (const el of root.querySelectorAll('a, button, [role="button"]')) {
      if (/^(get app|get coins|open app|get the app|use the app)$/i.test((el.innerText || "").trim())) el.style.display = "none";
    }
    // App-promo dialogs with no "not now": remove them.
    for (const d of root.querySelectorAll('[role="dialog"], [class*="Modal"]')) {
      const t = (d.innerText || "").slice(0, 400);
      if (nags.test(t) && !/log in|password|sign in/i.test(t)) d.remove();
    }
  }

  const start = () => {
    tidy(document);
    new MutationObserver(() => tidy(document)).observe(document.documentElement, { childList: true, subtree: true });
  };
  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", start);
  else start();
})();
