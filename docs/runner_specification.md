# VelCom-Runner

Diese Spezifikation definiert _Runner_ für VelCom.
Ein Runner ist ein Programm, das eine Reihe von Benchmarks ausführt und die Ergebnisse dieser Benchmarks in einem definierten Format ausgibt.

In dieser Spezifikation wird außerdem das Layout des Repositorys definiert, das den Runner und die Benchmarks enthält.

## Repository-Layout

Das Repository enthält in seinem Root-Directory eine Datei `bench`, welche den Runner darstellt.
Diese Datei ist ausführbar.
Falls Sie einen Interpreter benötigt, enthält sie eine entsprechende `#!`-Zeile.

Das Repository darf weitere Dateien (z. B. Benchmarks, Referenzausgaben) enthalten.

## Runner-Interface

Der Runner erhält als Argument das Root-Directory des Repositories, das das zu testende System enthält.
Dieses Verzeichnis muss lesbar, schreibbar und ausführbar sein.

```sh
 ./bench ../my-little-compiler/
```

Es ist nicht spezifiziert, welches beim Aufruf des Runners das Working Directory ist.

### Return-Code

Der Runner gibt folgende Return-Codes zurück:

| Return-Code | Bedeutung |
|-------------|-----------|
| 0 | Kein Fehler |
| 1 | Interner Fehler des Runners |
| 2 | Falsche Benutzung des Runners (z. B. Repository nicht lesbar) |

### Ausgabe

Der Runner gibt die Ergebnisse der Benchmarks auf `stdout` aus.
Die Ergebnisse haben folgendes JSON-Format (Angaben in `<>` sind Platzhalter, `...` steht für Wiederholung):

```json
 {
    // Either:
    "<benchname>": {
        "<property>": {
            // Either:
            "results": [
                <measurement>, ...
            ],
            "unit": "<unit-of-results>",
            "resultInterpretation": "<resultInterpretation>",
            // Or:
            "error": "<error-string>"
        },
        ...
    },
    ...
    // OR:
    "error": "<global-error-string>"
 }
```

Die Platzhalter bedeuten:

| Platzhalter | Bedeutung |
|-------------|-----------|
| `<benchname>` | Eindeutiger Name des Benchmarks, String |
| `<property>` | Eine gemessene Eigenschaft des Benchmarks (z. B. Laufzeit, Speicherverbrauch), String |
| `<measurement>` | Eine Messung der Eigenschaft in der angegebenen Maßeinheit, Zahl |
| `<unit-of-results>` | Maßeinheit, in der die Eigenschaften gemessen wurden, String.<br>Einheiten folgen dem ISO-80000-Standard, sofern dort definiert<br>Exponenten werden als b^e geschrieben |
| `<resultInterpretation>` | Entweder "LESS_IS_BETTER" oder "MORE_IS_BETTER" oder "NEUTRAL". Kontrolliert, ob ein kleineres oder größeres Ergebnis besser ist, oder ob keine Aussage getroffen werden kann (z.B. Zeilenanzahl). |
| `<error-string>` | Fehlermeldung, wenn beim Ausführen des Benchmarks oder Messen der Eigenschaft ein Fehler aufgetreten ist |
| `<global-error-string>` | Fehlermeldung, wenn der Runner keine Benchmarks ausführen konnte |

Eine gemessene Eigenschaft muss über alle Testfälle hinweg in der selben Maßeinheit ausgedrückt sein.
