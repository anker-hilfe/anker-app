# Anker – Hilfe für schwere Momente

Hilfe-App für Kinder, Jugendliche und Erwachsene bei Panik, Anspannung und Leere. Mit geführtem Dialog, Übungen, Notfallplan und automatischem Notfall-Alarm an Vertrauenspersonen.

- **Android-App herunterladen:** https://github.com/robertpahl1987-creator/anker-app/releases/latest
- **Im Browser ausprobieren:** https://robertpahl1987-creator.github.io/anker-app/

> Anker ersetzt keine Therapie und keine ärztliche Hilfe. Bei Lebensgefahr: **112**. TelefonSeelsorge: **0800 111 0 111**. Nummer gegen Kummer (Kinder & Jugendliche): **116 111**.

---

## Selbst bauen

### APK bauen lassen

1. Auf github.com ein neues Repository anlegen, z. B. `anker-app`.
2. Den kompletten Inhalt dieses Ordners hochladen (auch den versteckten Ordner `.github`).
   Am einfachsten: „Add file" → „Upload files" → alle Dateien und Ordner hineinziehen.
3. Oben auf **Actions** tippen. Der Ablauf „APK bauen" startet von selbst (sonst: „Run workflow").
4. Nach ca. 3–5 Minuten den fertigen Lauf öffnen und unten bei **Artifacts** „Anker-APK" herunterladen.
5. Die ZIP auf dem Handy entpacken, `app-debug.apk` antippen und installieren
   („Installation aus unbekannten Quellen" einmal erlauben).

## Beim ersten Start

Die App fragt nach Berechtigungen für **SMS**, **Telefon** und **Standort**. Alle drei erlauben,
sonst kann der Alarm nicht automatisch senden. Prüfen lässt sich das unter Einstellungen → „Berechtigungen prüfen".

## Was automatisch passiert bei 10 von 10

1. 10 Sekunden Countdown mit großem Abbrechen-Knopf.
2. SMS an alle Vertrauenspersonen mit „SMS" oder „SMS + Anruf" (optional mit Standort-Link).
3. Danach wird die erste Person mit „Anruf" oder „SMS + Anruf" direkt angerufen.
4. WhatsApp-Kontakte: Der Chat öffnet sich mit fertiger Nachricht, Senden muss man selbst tippen
   (WhatsApp erlaubt kein automatisches Senden).

Notrufnummern (112) wählt Android aus Sicherheitsgründen nie automatisch, die App zeigt sie aber überall an.

## Hinweise

- Alle Daten bleiben nur auf dem Gerät.
- Die App ersetzt keine Therapie und keine ärztliche Hilfe.
- Für den Play Store wären SMS- und Anruf-Berechtigungen nur mit Sondergenehmigung von Google möglich.
  Als eigene APK funktioniert alles ohne Einschränkung.
- Die Oberfläche liegt in `index.html` (gleichzeitig Web-Version). Beim Bauen wird sie automatisch in die App übernommen.
