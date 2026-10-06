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

## Phase 2: Der Kumpel wird erwachsen ✅

- ✅ **Die Kiepe**: Der Kumpel hat einen eigenen Rucksack, den du mit Schleichen + Rechtsklick öffnest. Er wächst mit der Stufe (9 → 27 Plätze).
- ✅ **Der Kern bewahrt die Seele**: Stirbt ein Kumpel, bleibt ein *gesprungener Kern* mit Name, Stufe und EP zurück. Mit einem Kupferblock reparieren, wieder einsetzen, und dein Kumpel ist zurück.
- ✅ **Einpacken**: Mit einem leeren Kern holst du deinen Kumpel zurück in den Kern, um umzuziehen oder die Dimension zu wechseln.
- ✅ **Grubenlampe**: Gibst du ihm Fackeln, stellt er sie in dunklen Höhlengängen selbst auf.
- ✅ **Schlagwetter-Warnung**: Er spürt Creeper, bevor du sie hörst, und lässt sie durch Wände leuchten. Er warnt auch vor Lava direkt neben dir.
- ✅ **Henkelmann**: Hat er Essen in der Kiepe und dein Hunger wird knapp, reicht er dir dein Bütterken.
- ✅ **Feierabend**: Wenn du schlafen gehst, setzen sich Kumpels in deiner Nähe hin, und am Morgen geht die Schicht weiter.

## Phase 3: Die Zeche ✅

- ✅ **Steigerpfeife**: Ruft alle deine Kumpels zu dir. Mit Schleichen schickt sie alle in die Pause.
- ✅ **Lagerkiste**: Mit der Pfeife markierst du eine Kiste, dann bringt der Kumpel seine Beute dorthin statt zu dir. Funktioniert mit allem, was die Fabric Transfer API kennt, also auch Modded-Lager.
- ✅ **Hauer-Modus**: Mit einer Spitzhacke in der Hand baut er freiliegende Erze selbst ab. Die Werkzeugstufe zählt, Verzauberungen wie Glück und Behutsamkeit auch, die Hacke nutzt sich ab, und das Ganze lässt sich abschalten (Config und `mobGriefing`). Die Hacke bekommst du zurück, wenn du seine Kiepe öffnest.
- ✅ **Wünschelrute**: Hochstufige Kumpels lassen gefundenes Erz kurz durch Wände aufleuchten.
- ✅ **Fortschritte**: „Glück auf!“, „Wieder da!“, „Ordnung muss sein“, „Hauer“, „Schicht im Schacht“, „Der Steiger kommt“, „Ganze Belegschaft“ (5 Kumpels), „Vom Kupfer zum Netherit“ und ein versteckter für den 4. Dezember
- ✅ **Barbaratag** (4. Dezember, die Schutzpatronin der Bergleute): Kumpels tragen einen blühenden Barbarazweig am Helm, wünschen dir „Glück auf!“, und Füttern gibt doppelte EP.
- ✅ **Steigerlied**: Läuft in der Nähe eine Jukebox, tanzen die Kumpels.

## Phase 4: Träume

- ✅ **Kanarienvogel** im Käfig als Gefahren-Frühwarnsystem auf der Schulter des Kumpels: Monster in der Nähe leuchten, er warnt vor Luftnot unter Wasser, und Creeper werden früher gespürt
- ✅ **Grubenhelm** für dich selbst, mit Lampe: Im Dunkeln siehst du trotzdem
- ✅ **Schnellere Erzsuche**: Chunk-Abschnitte ohne passendes Erz in ihrer Palette werden übersprungen, damit auch viele Kumpels in großen Modpacks kaum Leistung kosten
- 💭 **Förderturm-Multiblock** als Basis der Zeche, an dem alle Kumpels ihre Beute abliefern
- 💭 **Arbeitsteilung**: Mehrere Kumpels teilen sich die Arbeit (Sammler, Lampenträger, Hauer)
- 💭 **Eigene Sounds**, zum Beispiel ein kleines „Glück auf!“, wenn er dich begrüßt
- 💭 **Lore**: Der Kumpel fährt in der Minecart-Lore mit

## Phase 5: Schichtbetrieb ✅

Der Kumpel arbeitet selbstständiger, bekommt Charakter und spricht mehr Sprachen.

- ✅ **Namen**: Jeder neue Kumpel bekommt einen Namen aus dem Pott (Jupp, Kalle, Hotte, Trude, Stani, Mehmet …), einstellbar in der Config.
- ✅ **Ruhrpott-Sprüche**: Ab und zu sagt er was („Hömma, da glitzert watt!“, „Glück auf!“), ohne zu nerven. Abschaltbar.
- ✅ **Silberfischchen-Warnung**: Befallenes Gestein wird beim Erzscan erkannt, rot markiert und gemeldet.
- ✅ **Vortrieb**: Schleichend mit der Steigerpfeife auf eine Wand: Der nächste Hauer gräbt dort einen 1×2-Stollen hinein. Er hält vor Wasser, Lava und Abgründen an, steckt den Abraum in die Kiepe und stellt Fackeln auf.
- ✅ **Feldschmiede**: Ein kleiner Ofen für den Rücken des Kumpels. Er schmilzt Roherze aus der Kiepe mit Kohle aus der Kiepe.
- ✅ **`/kumpel list`**: Zeigt alle deine geladenen Kumpels mit Stufe, Ort und was sie gerade tun.
- ✅ **Eigene Sounds**: Eine echte Trillerpfeife für die Steigerpfeife, selbst erzeugt.
- ✅ **Mehr Sprachen**: Polnisch und Türkisch (viele Bergleute im Ruhrgebiet kamen von dort), dazu Niederländisch, Französisch und Spanisch.

## Phase 6: Grubenwehr & Seilfahrt ✅

Der Kumpel passt auf dich auf, und die Zeche bekommt einen Aufzug.

- ✅ **Grubenwehr**: Der Kumpel verteidigt dich. Greift dich ein Monster an (oder greifst du eins an), geht er mit. Mit einer Waffe oder Spitzhacke in der Hand haut er fester zu. Creeper lässt er in Ruhe, die überlässt er der Schlagwetter-Warnung. Abschaltbar.
- ✅ **Förderkorb**: Ein Aufzug-Block für Schächte. Rechtsklick fährt dich zum nächsten Förderkorb darüber, Schleichen + Rechtsklick zum nächsten darunter. Deine Kumpels in der Nähe fahren mit („Seilfahrt!“).
- ✅ **Schichtbuch**: Der Kumpel führt Buch: gefundene Erze, abgebaute Erze, gegrabene Blöcke, eingesammelte Items, geschmolzene Barren. Gibst du ihm ein Buch, schreibt er dir seinen Schichtbericht hinein.
- ✅ **Zeitansage unter Tage**: Wenn es draußen dunkel wird oder die Sonne aufgeht, sagt er dir Bescheid. Unter Tage merkt man das ja sonst nicht.
- ✅ **Kumpel-Treff**: Treffen sich zwei deiner Kumpels, grüßen sie sich mit „Glück auf!“.

## Phase 7: Notfall & Ausfahrt ✅

Wenn's unter Tage brenzlig wird.

- ✅ **Rettungskapsel** (nach der Dahlbusch-Bombe, mit der 1963 in Lengede Bergleute gerettet wurden): Benutzen, drei Sekunden stillhalten, und du wirst senkrecht an die Oberfläche gezogen. Deine Kumpels kommen mit. Geht nur dort, wo es einen Himmel gibt.
- ✅ **Ausfahrt**: Der Kumpel merkt sich den Weg, den ihr unter Tage gegangen seid. Schleichen + Rechtsklick mit einem Kompass, und er führt dich auf demselben Weg zurück nach oben. Leuchtende Spuren zeigen, wo es langgeht.
- ✅ **Verschnaufpause**: Wer Feierabend macht, erholt sich. Ruhende Kumpels heilen langsam.
- ✅ **Kumpel „Barbara“**: Ein Kumpel, der Barbara heißt, trägt den Barbarazweig das ganze Jahr.

## Phase 8: Tiefbau ✅

Der Vortrieb wird erwachsen, und die Feldschmiede bekommt eine Kokerei.

- ✅ **Abdämmen**: Stößt der Stollen auf Wasser oder Lava, dichtet der Kumpel sie mit Steinen aus seiner Kiepe ab (Bruchstein, Tiefenschiefer, Erde, Netherrack …, erweiterbar über den Tag `#kumpel:tunnel_fillers`) und gräbt weiter. Nur wenn er nichts zum Abdichten hat, hört er auf.
- ✅ **Brückenschlag**: Ein Loch im Stollenboden wird mit Steinen aus der Kiepe geschlossen, statt dass der Vortrieb endet.
- ✅ **Erzfunde**: Der Kumpel merkt sich die wertvollsten Erze, die er gespürt hat. Im Schichtbericht stehen sie mit Koordinaten, abgebaute fliegen raus.
- ✅ **Kokerei** (nach der Kokerei Zollverein): Hat die Feldschmiede nichts zu schmelzen, verkokt sie Kohle aus der Kiepe. **Koks** brennt anderthalbmal so lange wie Kohle, im Ofen wie in der Feldschmiede.
- ✅ **Fortschritt** „Zollverein“ für den ersten Koks.

## Phase 9: Zechenleben ✅

Rund um die Zeche: keiner wird vergessen, und die Kapelle spielt.

- ✅ **Markenkontrolle**: Jeder Kumpel hängt seine Marke auf, mit Ort, Tätigkeit und Zeit. So findest du auch Kumpels in entladenen Chunks wieder, und von verunglückten weißt du, wo ihr Kern liegt.
- ✅ **Markentafel**: Ein Block, der die Markenkontrolle zeigt, inklusive „wie viele sind unter Tage?“.
- ✅ **Kumpelkapelle**: Eine eigene Schallplatte „Glück auf“, ein selbst komponierter und synthetisierter Blasmusik-Marsch. Liegt in verlassenen Minen. Die Kumpels singen mit, und ihre gute Laune macht dich flinker (Eile).

## Phase 10: Licht & Lehre ✅

Ein Kumpel bringt Licht ins Dunkel, und Neulinge lernen das Handwerk.

- ✅ **Helmlampe**: Die Lampe am Helm leuchtet wirklich. Unter Tage erhellt der Kumpel die Höhle um sich herum, während er läuft. Ein unsichtbarer Lichtblock, der sich selbst löscht, sobald der Kumpel weitergeht, auch nach einem Absturz. Nie heller als die Fackel-Schwelle, damit die Grubenlampe weiter Fackeln setzt.
- ✅ **Kumpelfibel**: Ein Handbuch mit allem, was der Kumpel kann, in jeder Sprache. Gibt's zum ersten Kumpel dazu.
- ✅ **Koks im Ofen**: Koks brennt auch im Ofen (in 26.3 ist Brennstoff datengetrieben).
