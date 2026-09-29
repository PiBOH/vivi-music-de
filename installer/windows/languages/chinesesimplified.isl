; Simplified Chinese (简体中文) — language options only.
;
; Inno Setup 6 ships no Chinese wizard translation (the file arrives in 7), and
; naming a messages file the installed compiler does not have aborts the build.
; `VIVIMusic.iss` therefore loads this overlay — the name the Select Language
; page shows — instead of `compiler:Languages\ChineseSimplified.isl` whenever
; that file is absent. See the [Languages] section there.
[LangOptions]
LanguageName=简体中文
