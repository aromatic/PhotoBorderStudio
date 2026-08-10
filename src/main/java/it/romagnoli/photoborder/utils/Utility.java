package it.romagnoli.photoborder.utils;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

import javax.imageio.ImageIO;

import java.io.File;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Point2D;
import javafx.beans.property.IntegerProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;

public class Utility {

    public static final List<String> RAW_EXTENSIONS = List.of(
            ".cr2", ".nef", ".arw", ".dng", ".raf", ".orf", ".rw2",
            ".CR2", ".NEF", ".ARW", ".DNG", ".RAF", ".ORF", ".RW2");

    public static final List<String> ACCEPTED_EXTENSIONS = List.of(
            ".jpg", ".jpeg", ".gif", ".tif", ".tiff", ".png",
            ".JPG", ".JPEG", ".GIF", ".TIF", ".TIFF", ".PNG");


    public static void salvaSchemaPunti(File currentImageFile, ImageView previewView, List < Point2D > points) {
    	if(currentImageFile == null || previewView.getImage() == null) {
    		return;
    	}
    	Image image = previewView.getImage();
    	double imageWidth = image.getWidth();
    	double imageHeight = image.getHeight();
    	String name = currentImageFile.getName();
    	int dot = name.lastIndexOf('.');
    	String baseName = dot > 0 ? name.substring(0, dot) : name;
    	File csvFile = new File(currentImageFile.getParentFile(), baseName + ".csv");
    	try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(csvFile)))) {
    		out.println("# ImageWidth=" + (int) imageWidth);
    		out.println("# ImageHeight=" + (int) imageHeight);
    		out.println("index,x,y");
    		for(int i = 0; i < points.size(); i++) {
    			Point2D p = points.get(i);
    			out.printf(Locale.US, "%d,%.2f,%.2f%n", i + 1, p.getX(), p.getY());
    		}
    		System.out.println("Schema punti salvato: " + csvFile.getAbsolutePath());
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    }

    public static List < Point2D > leggiSchemaPunti(ImageView previewView, IntegerProperty selectedImage) {
		FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Points Schema", "*.csv"));
        File csvFile = fileChooser.showOpenDialog(null);
    	if(csvFile == null || previewView.getImage() == null) {
    		return null;
    	}
    	if(!csvFile.exists()) {
    		System.out.println("Schema punti non trovato: " + csvFile.getAbsolutePath());
    		return null;
    	}
    	double savedWidth = 0;
    	double savedHeight = 0;
    	List < Point2D > savedPoints = new ArrayList < > ();
    	try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
    		String line;
    		while((line = reader.readLine()) != null) {
    			line = line.trim();
    			if(line.isEmpty()) {
    				continue;
    			}
    			/*
    			 * Dimensioni dell'immagine con cui
    			 * sono stati salvati i punti.
    			 */
    			if(line.startsWith("# ImageWidth=")) {
    				savedWidth = Double.parseDouble(line.substring("# ImageWidth=".length()));
    				continue;
    			}
    			if(line.startsWith("# ImageHeight=")) {
    				savedHeight = Double.parseDouble(line.substring("# ImageHeight=".length()));
    				continue;
    			}
    			/*
    			 * Salta intestazione CSV.
    			 */
    			if(line.startsWith("index")) {
    				continue;
    			}
    			String[] fields = line.split(",");
    			if(fields.length < 3) {
    				continue;
    			}
    			double x = Double.parseDouble(fields[1]);
    			double y = Double.parseDouble(fields[2]);
    			savedPoints.add(new Point2D(x, y));
    		}
    		if(savedWidth <= 0 || savedHeight <= 0) {
    			System.out.println("Dimensioni immagine non valide nel CSV.");
    			return null;
    		}
    		if(savedPoints.size() != 9) {
    			System.out.println("Schema non valido: trovati " + savedPoints.size() + " punti invece di 9.");
    			return null;
    		}

    		List < Point2D > points = new ArrayList < > (9);
    		for(Point2D p: savedPoints) {
				points.add(new Point2D(p.getX(), p.getY()));
    			//points.add(new Point2D(p.getX() * scaleX, p.getY() * scaleY));
    		}
    		System.out.println("Schema punti caricato: " + csvFile.getAbsolutePath());
    		return points;
    	} catch (IOException | NumberFormatException e) {
    		e.printStackTrace();
    	}
        return null;
    }
	
	
	public static void saveImageWithBorders(Image borderedImage, Window ownerWindow) {
        if (borderedImage == null) {
            return; // Nessuna immagine con bordi da salvare
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Immagini PNG", "*.png"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try {
                // Salva direttamente l'immagine con i bordi, senza passare da ImageViewB
                ImageIO.write(SwingFXUtils.fromFXImage(borderedImage, null), "png", file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


