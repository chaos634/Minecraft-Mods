# Kumpel: Der große Plan ⛏️

> *Glück auf!* Kumpel soll der beste Freund werden, den man unter Tage haben kann.
> Ruhrpott-Bergbaukultur trifft Minecraft. Alles dreht sich um den kleinen Golem, seine Arbeit unter Tage und die Zeche, die um ihn herum entsteht.

Leitlinien:

- **Beim Thema bleiben.** Alles hat mit Bergbau, Kumpeln, Licht, Erz, Gefahr unter Tage oder Kameradschaft zu tun.
- **Modpack-freundlich.** Was sich sinnvoll einstellen lässt, landet in der Config. Fremde Erze funktionieren ohne Patch.
- **Keine bösen Überraschungen.** Der Kumpel zerstört nichts ohne Erlaubnis (Config + `mobGriefing`), nimmt nichts weg, was du weggeworfen hast, und spammt dich nicht zu.
- **Alles getestet.** Jede Funktion bekommt einen Gametest, das Aussehen wird per Screenshot im Client-Test geprüft.

Legende: ✅ fertig · 🔨 in Arbeit · 💭 Idee

---

## Phase 0: Das Fundament ✅

- ✅ Kumpel-Golem: folgt, sitzt, teleportiert hinterher
- ✅ Sammelt Items ein und bringt sie dir
- ✅ Erzsuche mit Zeigearm, Glockenton und Actionbar-Meldung (Richtung und Tiefe)
- ✅ Fünf Stufen: Kupfer → Eisen → Gold → Diamant → Netherit
- ✅ Kumpel-Kern als Rezept, Deutsch und Englisch
- ✅ Server-Gametests und Client-Screenshot-Test in CI

## Phase 1: Für Modpacks gemacht

- ✅ **Eigener Kreativ-Tab** „Kumpel“ mit allen Items, Spawn-Ei und vorgelevelten Kernen
- ✅ **Config** (`config/kumpel.json`)
  - Stufen frei definierbar: beliebig viele, mit EP-Grenze, Leben, Tempo, Suchradius, Sammelradius, Kiepengröße, Feuerfestigkeit und Textur
  - Erze frei zuordnen: per Block-Tag oder Block-ID, mit Mindeststufe, Wert und Klang
  - Unbekannte Erze aus `c:ores` landen automatisch auf einer einstellbaren Stufe, damit Modpack-Erze sofort funktionieren
  - Futterliste: welches Item wie viele EP gibt, auch per Item-Tag
  - Zeiten, Abklingzeiten und Radien
- ✅ **`/kumpel reload`** lädt die Config neu, ohne Neustart
- ✅ **JEI- und REI-Integration**
  - Infoseiten zu Kumpel und Kern
  - Kategorie „Kumpel füttern“: Item → EP
  - Kategorie „Erzsuche“: Erz → ab welcher Stufe es gespürt wird
- ✅ **Saubere Repo-Struktur**: jede Mod in einem eigenen Ordner, aufgebaut wie ein normales Mod-Projekt

## Phase 2: Der Kumpel wird erwachsen

- **Die Kiepe**: Der Kumpel hat einen eigenen Rucksack, den du mit Schleichen + Rechtsklick öffnest. Er wächst mit der Stufe (9 → 27 Plätze).
- **Der Kern bewahrt die Seele**: Stirbt ein Kumpel, bleibt ein *gesprungener Kern* mit Name, Stufe und EP zurück. Mit Kupfer reparieren, wieder einsetzen, und dein Kumpel ist zurück.
- **Einpacken**: Mit einem leeren Kern holst du deinen Kumpel zurück in den Kern, um umzuziehen oder die Dimension zu wechseln.
- **Grubenlampe**: Gibst du ihm Fackeln, stellt er sie in dunklen Höhlengängen selbst auf.
- **Schlagwetter-Warnung**: Er spürt Creeper, bevor du sie hörst, und lässt sie durch Wände leuchten. Er warnt auch vor Lava direkt neben dir.
- **Henkelmann**: Hat er Essen in der Kiepe und dein Hunger wird knapp, reicht er dir dein Bütterken.
- **Feierabend**: Wenn du schlafen gehst, setzen sich Kumpels in deiner Nähe hin, und am Morgen geht die Schicht weiter.

## Phase 3: Die Zeche

- **Steigerpfeife**: Ruft alle deine Kumpels zu dir. Mit Schleichen schickt sie alle in die Pause.
- **Lagerkiste**: Mit der Pfeife markierst du eine Kiste, dann bringt der Kumpel seine Beute dorthin statt zu dir. Funktioniert mit allem, was die Fabric Transfer API kennt, also auch Modded-Lager.
- **Hauer-Modus**: Mit einer Spitzhacke in der Hand baut er freiliegende Erze selbst ab. Die Werkzeugstufe zählt, die Hacke nutzt sich ab, und das Ganze lässt sich abschalten.
- **Wünschelrute**: Hochstufige Kumpels lassen gefundenes Erz kurz durch Wände aufleuchten.
- **Fortschritte**: „Glück auf!“, „Vom Kupfer zum Netherit“, „Ganze Belegschaft“ (5 Kumpels), „Schicht im Schacht“, „Hauer“
- **Barbaratag** (4. Dezember, die Schutzpatronin der Bergleute): Kumpels tragen einen Barbarazweig und feiern ein bisschen.
- **Steigerlied**: Läuft in der Nähe eine Jukebox, tanzen die Kumpels.

## Phase 4: Träume 💭

- **Kanarienvogel** im Käfig als Gefahren-Frühwarnsystem auf der Schulter des Kumpels
- **Förderturm-Multiblock** als Basis der Zeche, an dem alle Kumpels ihre Beute abliefern
- **Arbeitsteilung**: Mehrere Kumpels teilen sich die Arbeit (Sammler, Lampenträger, Hauer)
- **Eigene Sounds**, zum Beispiel ein kleines „Glück auf!“, wenn er dich begrüßt
- **Grubenhelm** für dich selbst, mit Lampe
- **Lore**: Der Kumpel fährt in der Minecart-Lore mit
