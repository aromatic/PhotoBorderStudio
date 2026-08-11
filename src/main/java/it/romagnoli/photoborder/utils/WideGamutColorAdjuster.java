package it.romagnoli.photoborder.utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class WideGamutColorAdjuster {

    public enum ColorSpace {
        ADOBE_RGB,
        PRO_PHOTO_RGB
    }

    /**
     * Applica le regolazioni ColorAdjust convertendo temporaneamente l'immagine in uno spazio colore più ampio.
     */
    public static WritableImage adjust(Image inputImage, double hue, double saturation, double brightness, double contrast, ColorSpace targetSpace) {
        int width = (int) inputImage.getWidth();
        int height = (int) inputImage.getHeight();

        PixelReader reader = inputImage.getPixelReader();
        WritableImage outputImage = new WritableImage(width, height);
        PixelWriter writer = outputImage.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);

                // 1. Converti da sRGB standard allo Spazio Esteso (es. AdobeRGB)
                double[] wideRgb = sRgbToWideGamut(color.getRed(), color.getGreen(), color.getBlue(), targetSpace);

                // 2. Applica le variazioni (Hue, Saturation, Brightness, Contrast) nello spazio esteso
                double[] adjustedWideRgb = applyColorAdjust(wideRgb[0], wideRgb[1], wideRgb[2], hue, saturation, brightness, contrast);

                // 3. Riconverti dallo Spazio Esteso a sRGB per la visualizzazione su schermo
                double[] finalSrgb = wideGamutToSRgb(adjustedWideRgb[0], adjustedWideRgb[1], adjustedWideRgb[2], targetSpace);

                // Clamp per sicurezza nell'intervallo [0.0, 1.0]
                double r = clamp(finalSrgb[0]);
                double g = clamp(finalSrgb[1]);
                double b = clamp(finalSrgb[2]);

                writer.setColor(x, y, new Color(r, g, b, color.getOpacity()));
            }
        }

        return outputImage;
    }

    // --- Matematiche di ColorAdjust nello Spazio Esteso ---
    private static double[] applyColorAdjust(double r, double g, double b, double hue, double saturation, double brightness, double contrast) {
        // A. Brightness (Luminosità)
        r += brightness;
        g += brightness;
        b += brightness;

        // B. Contrast (Contrasto)
        if (contrast != 0) {
            double factor = (1.0 + contrast) / (1.0 - contrast);
            r = (r - 0.5) * factor + 0.5;
            g = (g - 0.5) * factor + 0.5;
            b = (b - 0.5) * factor + 0.5;
        }

        // C. Saturation & Hue (tramite HSB/HSL nel nuovo spazio)
        // Convertiamo temporaneamente in HSB per applicare Tonalità e Saturazione
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double delta = max - min;

        if (saturation != 0 && max > 0) {
            double currentSat = delta / max;
            double newSat = clamp(currentSat + saturation);
            if (currentSat > 0) {
                r = max - (max - r) * (newSat / currentSat);
                g = max - (max - g) * (newSat / currentSat);
                b = max - (max - b) * (newSat / currentSat);
            }
        }

        return new double[]{clamp(r), clamp(g), clamp(b)};
    }

    // --- Trasformazioni tra Spazi Colore (sRGB <-> XYZ <-> WideGamut) ---

    private static double[] sRgbToWideGamut(double r, double g, double b, ColorSpace space) {
        // sRGB -> Linear sRGB
        r = (r > 0.04045) ? Math.pow((r + 0.055) / 1.055, 2.4) : (r / 12.92);
        g = (g > 0.04045) ? Math.pow((g + 0.055) / 1.055, 2.4) : (g / 12.92);
        b = (b > 0.04045) ? Math.pow((b + 0.055) / 1.055, 2.4) : (b / 12.92);

        // Linear sRGB -> CIE XYZ (D65)
        double X = r * 0.4124564 + g * 0.3575761 + b * 0.1804375;
        double Y = r * 0.2126729 + g * 0.7151522 + b * 0.0721750;
        double Z = r * 0.0193339 + g * 0.1191920 + b * 0.9503041;

        if (space == ColorSpace.ADOBE_RGB) {
            // XYZ -> Linear AdobeRGB
            double R_adj =  2.0413690 * X - 0.5649464 * Y - 0.3446944 * Z;
            double G_adj = -0.9692660 * X + 1.8760108 * Y + 0.0415560 * Z;
            double B_adj =  0.0134474 * X - 0.1183897 * Y + 1.0154096 * Z;

            // Gamma AdobeRGB (2.2)
            return new double[]{
                Math.pow(Math.max(0, R_adj), 1.0 / 2.2),
                Math.pow(Math.max(0, G_adj), 1.0 / 2.2),
                Math.pow(Math.max(0, B_adj), 1.0 / 2.2)
            };
        }
        
        // Default / Altri spazi (es. ProPhotoRGB richiede matrice differente e Gamma 1.8)
        return new double[]{r, g, b};
    }

    private static double[] wideGamutToSRgb(double r, double g, double b, ColorSpace space) {
        if (space == ColorSpace.ADOBE_RGB) {
            // Linearizza AdobeRGB (Gamma 2.2)
            double R_lin = Math.pow(r, 2.2);
            double G_lin = Math.pow(g, 2.2);
            double B_lin = Math.pow(b, 2.2);

            // AdobeRGB -> CIE XYZ
            double X = R_lin * 0.5767309 + G_lin * 0.1855540 + B_lin * 0.1881852;
            double Y = R_lin * 0.2973769 + G_lin * 0.6273491 + B_lin * 0.0752740;
            double Z = R_lin * 0.0270343 + G_lin * 0.0706872 + B_lin * 0.9911085;

            // XYZ -> Linear sRGB
            double r_s =  3.2404542 * X - 1.5371385 * Y - 0.4985314 * Z;
            double g_s = -0.9692660 * X + 1.8760108 * Y + 0.0415560 * Z;
            double b_s =  0.0556434 * X - 0.2040259 * Y + 1.0572252 * Z;

            // Linear sRGB -> sRGB con gamma
            r_s = (r_s > 0.0031308) ? (1.055 * Math.pow(r_s, 1.0 / 2.4) - 0.055) : (12.92 * r_s);
            g_s = (g_s > 0.0031308) ? (1.055 * Math.pow(g_s, 1.0 / 2.4) - 0.055) : (12.92 * g_s);
            b_s = (b_s > 0.0031308) ? (1.055 * Math.pow(b_s, 1.0 / 2.4) - 0.055) : (12.92 * b_s);

            return new double[]{r_s, g_s, b_s};
        }
        return new double[]{r, g, b};
    }

    private static double clamp(double val) {
        return Math.min(1.0, Math.max(0.0, val));
    }
}