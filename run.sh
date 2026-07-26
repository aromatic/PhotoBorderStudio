#!/bin/bash

# Script per eseguire PhotoBorderStudio
cd "$(dirname "$0")"
java -cp "PhotoBorderStudio-1.0.0.jar:lib/*" it.romagnoli.photoborder.App
