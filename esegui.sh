#!/bin/bash

DIR="$(dirname "$0")"

java \
-Dprism.order=sw \
--module-path /usr/share/openjfx/lib/ \
--add-modules javafx.controls,javafx.fxml \
-Djava.library.path="$DIR" \
-jar "$DIR/PhotoBorderStudio-1.0.0.jar"
