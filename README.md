# Wilkommen beim Projekt Spuerenerkennung im Fach Digitale Bildverarbeitung!

Authoren: Victoria Stobel (3670137) und Philip Pruessner (7079160) 

## Dependencies
Dieses Projekt wurde in Python *3.8.13* geschrieben.

Um alle nötigen Pakete zu installieren in den Ordner `DBW_SPURENERKENNUNG` navigieren und den command `pip install -r requirements.txt` ausführen

## Ausführen

- **Für Bilder**: im File `Projekt_Spurerkennung_v6\Projekt_Spurerkennung_v2.ipynb`letzte Lücke ausführen. In Variable `img_name` name des zu bearbeitenden Bildes eintragen
- **Für Videos**: File `Projekt_Spurerkennung_v6\Lane_Recognition.py` ausführen. Mit **q** kann die Wiedergabe gestoppt werden.

## Vorgehen
Das genaue Vorgehen und die einzelnen Schritte zur Erkennung und Markierung von Spuren kann in dem File `Projekt_Spurerkennung_v6\Projekt_Spurerkennung_v2.ipynb` eingesehen werden

## Kotlin App
In dem AndroidApp Ordner liegt der Quellcode für eine Kotlin App zur Spurerkennung. Nach dem Starten der App können über ein Dropdown Menü die verschiedenen Beispiel Bilder ausgewählt werden. Über den 'Filter Image' Button lässt sich das aktuell ausgewählte Bild verarbeiten. Sobald das Bild verarbeitet wurde, wird das verarbeitete Bild mit den erkannten Spurmarkierungen angezeigt.

Die Datei lanedetection.apk enthält eine APK für die App, die sich direkt auf dem Handy installieren lässt.

Um den Quellcode auszuführen, muss die [OpenCV Android library](https://sourceforge.net/projects/opencvlibrary/files/opencv-android/) heruntergeladen und in das Projekt eingebunden werden.