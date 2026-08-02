---
name: ship
description: Use when releasing a new version of the RIDDL IntelliJ IDEA plugin, or when the user says "ship", "cut a release", or "publish to the marketplace" in this repository.
---

# Ship Skill

Executes a full release cycle for the RIDDL IntelliJ IDEA plugin.
Follow each step in order. **STOP immediately** if any assertion
fails and report the problem.

This plugin differs from the ossuminc library `/ship` skills in two
ways that matter:

1. **The version lives in `resources/META-INF/plugin.xml`, not in
   the git tag.** sbt-dynver names the zip, but the Marketplace
   reads `<version>` from the descriptor. Nothing patches it
   automatically — if you only tag, the Marketplace publishes a
   version labelled with the *old* number.
2. **The artifact is a plugin zip on JetBrains Marketplace**, not a
   jar on GitHub Packages. There is no `sbt publish` here.

## Arguments

The user should provide a version number (e.g. `0.11.0`). If not
provided:

1. Find the latest tag (not just what is reachable from HEAD):
   ```
   git tag --sort=-v:refname | head -5
   ```
2. See what changed: `git log --oneline <latest-tag>..HEAD`
3. Analyse per semver and **recommend** a version — present your
   recommendation and let the user confirm or override. Do not ask
   them to pick from a list.

## First-Time Setup (once per plugin, by a human)

**Skip this section if the plugin is already on the Marketplace.**
Check with:
```
curl -s -o /dev/null -w "%{http_code}\n" \
  https://plugins.jetbrains.com/api/plugins/intellij/com.ossuminc.riddl.plugins.idea
```
`404` means it is not published yet and the steps below are required.
`200` means setup is done — go to Pre-Flight Checks.

**The first publication cannot be automated.** JetBrains states
plainly: "The first plugin publication must always be uploaded
manually." Only after that can `sbt publishPlugin` target this
plugin ID.

1. Sign in at <https://plugins.jetbrains.com> with a JetBrains
   Account.
2. Account menu -> **Upload plugin**. On first upload you must
   accept the **JetBrains Marketplace Developer Agreement** and
   create a **vendor profile** (use `ossuminc`, matching
   `<vendor>` in `plugin.xml`).
3. Build the artifact and upload `target/RIDDL4IDEA-<VERSION>.zip`.
   Set licence to **Apache-2.0** and add tags people would actually
   search for. The 400 MB Marketplace limit is not a concern here —
   the zip is around 20 MB.
4. Wait for review. Every new plugin gets automated **and** manual
   review, and it only becomes publicly visible after it passes.
   JetBrains does not guarantee a timeframe; if there is no word
   after **3–4 working days**, email marketplace@jetbrains.com.
5. Create the API token: Marketplace profile -> **My Tokens** tab ->
   generate a permanent token. It looks like `perm:xxxxxxxx`.
6. Store it where sbt-idea-plugin looks — a file is preferable to an
   env var since it is not exposed to every subprocess:
   ```
   echo 'perm:YOUR_TOKEN' > ~/.ij-plugin-repo-token
   chmod 600 ~/.ij-plugin-repo-token
   ```
   Alternatively set `IJ_PLUGIN_REPO_TOKEN`. Without one,
   `sbt publishPlugin` fails with "Plugin repo authorisation token
   not set".

Common rejection causes worth pre-empting: a default/placeholder
logo, a vague name, a description with broken links or grammar
errors, missing licence disclosure, or collecting telemetry without
consent.

## Pre-Flight Checks

1. **Always ship from `main`.** Confirm:
   ```
   git branch --show-current
   git pull origin main
   ```

2. Assert the working tree is clean:
   ```
   git status --porcelain
   ```
   If dirty, list the files and ask how to proceed. sbt-dynver
   appends a `-N-<hash>-<timestamp>` suffix for a dirty tree, which
   would end up in the zip filename.

3. Verify the tag does not already exist:
   ```
   git tag -l <VERSION>
   ```

4. Confirm the Marketplace token is available (see First-Time
   Setup) — otherwise the release will fail at the last step, after
   the tag is already pushed:
   ```
   ls ~/.ij-plugin-repo-token || echo "${IJ_PLUGIN_REPO_TOKEN:+set}"
   ```

## Ship Steps

5. **Update the plugin descriptor.** This is the step that has no
   analogue in the library ship skills, and skipping it silently
   publishes a mislabelled release. In
   `resources/META-INF/plugin.xml`:
   - Set `<version>` to `<VERSION>`.
   - Prepend a `<h3><VERSION></h3>` section to `<change-notes>`,
     written for *users* — what they can now do, not commit
     subjects. Trim the oldest entries so only the last few
     versions remain.
   - Check `<idea-version since-build="...">` is still right. Use
     the branch number (e.g. `253`), never a full build number,
     or users on earlier builds of the same release cannot
     install.

6. Verify formatting, tests and packaging all pass:
   ```
   sbt clean scalafmtCheck Test/scalafmtCheck test packageArtifactZip
   ```
   `clean` matters — a stale `target/` can serve an old resolved
   classpath to Test scope and produce a green run against the
   wrong riddl version. See NOTEBOOK.md "Trap: stale `target/`".

7. **Run the plugin verifier** and report the findings to the user
   before continuing:
   ```
   sbt runPluginVerifier
   ```
   Internal/experimental/deprecated API usages are review material
   for the Marketplace. They do not automatically block a release,
   but the user should see the count and decide. Since the build
   targets `ideaIU`, **nothing else checks that the plugin still
   runs on IDEA Community** — this is that check.

8. Confirm the packaged descriptor carries the right version. The
   zip filename comes from dynver and the descriptor does not, so
   they can disagree:
   ```
   unzip -p target/plugin/RIDDL4IDEA/lib/riddl-idea-plugin.jar \
     META-INF/plugin.xml | grep '<version>'
   ```
   It must read `<VERSION>`. If not, step 5 was missed.

9. Commit the descriptor bump:
   ```
   git add resources/META-INF/plugin.xml
   git commit -m "Release <VERSION>"
   ```

10. Tag and push:
    ```
    git tag -a <VERSION> -m "Release <VERSION>"
    git push origin main <VERSION>
    ```
    Note: `main` is protected by a rule requiring pull requests.
    A direct push succeeds for admins but reports "Bypassed rule
    violations". If that rule is meant to be enforced, open a PR
    for the release commit instead and tag after it merges.

11. **Write real release notes** and create the GitHub release. Do
    NOT use `--generate-notes` — that emits bare commit titles,
    which is not suitable for a public repo. Read the commits:
    ```
    git log --format="%H %s" <PREV_TAG>..<VERSION>
    ```
    and the diffs of anything non-trivial. Then:

    ```markdown
    ## What's New

    ### Features
    - **Feature name** — What was added and why it matters to users.

    ### Bug Fixes
    - **Area affected** — What was broken and how it is fixed.

    ### Improvements
    - **Area affected** — What changed and why it is better.

    ### Internal
    - Dependency upgrades, CI, docs — anything with no user-facing
      effect.
    ```

    Omit empty sections. Attach the zip so people can install
    without waiting for Marketplace review:
    ```
    gh release create <VERSION> target/RIDDL4IDEA-<VERSION>.zip \
      --title "Release <VERSION>" --notes "$(cat <<'EOF'
    <release notes here>
    EOF
    )"
    ```

12. Publish to the Marketplace:
    ```
    sbt publishPlugin
    ```
    This uploads to `https://plugins.jetbrains.com/api/updates/upload`
    using the token from step 6 of setup. Updates to an existing
    plugin are also reviewed, though typically faster than a new
    plugin's first submission.

    For a prerelease (alpha/beta), publish to a **custom release
    channel** instead so it does not become the default download for
    everyone. Confirm the intended channel with the user first.

## Post-Release Verification

13. Confirm the GitHub release exists:
    ```
    gh release view <VERSION>
    ```

14. Confirm the Marketplace accepted the upload — the new version
    should appear (possibly pending review):
    ```
    curl -s https://plugins.jetbrains.com/api/plugins/intellij/com.ossuminc.riddl.plugins.idea | head -c 400
    ```

15. `git status` to confirm a clean tree.

16. Update `NOTEBOOK.md`: move the shipped items out of "Changes
    Since <last version>" and reset it for the next cycle.

17. Report a summary: version, commit SHA, release URL, verifier
    findings, and Marketplace status.

## If Something Fails

- **Tests, formatting or packaging fail in step 6** — fix before
  tagging. Nothing has been published yet.
- **Verifier reports new problems in step 7** — report and let the
  user decide. Do not silently proceed.
- **Descriptor version is wrong in step 8** — redo step 5. Catching
  it here is the entire point of that check.
- **Tag pushed but publish fails in step 12** — do NOT delete the
  pushed tag. Fix the token or network problem and re-run
  `sbt publishPlugin`; it is idempotent for a given version.
- **Marketplace rejects the upload** — read the rejection email,
  fix, bump to the next patch version and ship again. A rejected
  version number cannot be reused.
