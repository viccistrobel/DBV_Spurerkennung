# Wilkommen beim Projekt Spurenerkennung im Fach Digitale Bildverarbeitung!

Authoren: Victoria Stobel (3670137) und Philip Pruessner (7079160) 

## Python Projekt
Python Projekt zur Spurerkennung
### Dependencies
Dieses Projekt wurde in Python *3.8.13* geschrieben.

Um alle nötigen Pakete zu installieren in den Ordner `DBW_SPURENERKENNUNG` navigieren und den command `pip install -r requirements.txt` ausführen

### Ausführen
- **Für Bilder**: im File [Projekt_Spurerkennung_v2.ipynb](Projekt_Spurerkennung_v6/Projekt_Spurerkennung_v2.ipynb) letzte Lücke ausführen. In Variable `img_name` name des zu bearbeitenden Bildes eintragen
- **Für Videos**: File [Lane_Recognition.py](Projekt_Spurerkennung_v6/Lane_Recognition.py) ausführen. Mit **q** kann die Wiedergabe gestoppt werden.

### Vorgehen
Das genaue Vorgehen und die einzelnen Schritte zur Erkennung und Markierung von Spuren kann in dem File [Projekt_Spurerkennung_v2.ipynb](Projekt_Spurerkennung_v6/Projekt_Spurerkennung_v2.ipynb) eingesehen werden.

Verarbeitung eines einzelnen Bildes:
1. Bild entzerren mithilfe einer definierten Kameramatrix und Distributiion (die mithilfe einer Kamera-Kalibrierung bestimm werden)
2. Bild in die Vogelperspektive transformieren
3. Bild nach gelber und weißer Linie filtern
4. Bild aufteilen in linke und rechte Hälfte und nach einzelnen Punkten filtern
5. Für linke und rechte Linie Polynomfunktion 2. Grades bestimmen und korrekte Koordinaten der Linie bestimmen
6. Linien und Bereich zwischen den Linien auf Originalbild überlagern

### Ergebnisse
Die Ergebnisse für die Verarbeitung der Beispielbilder und des Beispielvideos liegen in dem Ordner [Projekt_Spurerkennung_v6/results](Projekt_Spurerkennung_v6/results)

## Kotlin App
Kotlin App zur Spurerkennung 
### Dependencies
Die App wurde in Kotlin geschrieben.

Um den Quellcode auszuführen, muss die [OpenCV Android library](https://sourceforge.net/projects/opencvlibrary/files/opencv-android/) heruntergeladen und mit dem Namen opencv in das Projekt eingebunden werden.

### Ausführen
- **APK**: Die Datei [lanedetection.apk](AndroidApp/lanedetection.apk) lässt sich direkt auf dem Handy installieren und ausführen.
- **Quellcode**: In dem [AndroidApp](AndroidApp) Ordner liegt der Quellcode für die Kotlin App. Über ein Dropdown Menü können verschiedene Beispiel-Bilder ausgewählt werden. Über den 'Filter Image' Button lässt sich das aktuell ausgewählte Bild verarbeiten.

### Vorgehen
Das genaue Vorgehen und die einzelnen Schritte zur Erkennung und Markierung von Spuren entsprechen den Schritten im Python Projekt und können in dem File [Projekt_Spurerkennung_v2.ipynb](Projekt_Spurerkennung_v6/Projekt_Spurerkennung_v2.ipynb) eingesehen werden. Die relevanten Funktionen sind in dem [MainActivity.kt](AndroidApp/app/src/main/java/com/example/myapplication/MainActivity.kt) File implementiert.

Beim Starten der App wird die Funktion `calibrateImage` aufgerufen, die mithilfe der Calibration-Bilder im Ordner [drawable](AndroidApp/app/src/main/res/drawable) die Kamera kalibriert.

Sobald die Kalibrierung abgeschlossen wurde, kann der User ein Bild auswählen und die Spur filtern. Hierbei wird die Funktion `runImagePipeline` aufgerufen, die die Schritte für die Verarbeitung eines einzelnen Bildes (siehe oben) ausführt.

### Ergebnisse
Die Ergebnisse für die Verarbeitung der Beispielbilder liegen in dem Ordner [AndroidApp/results](AndroidApp/results)

