package it.romagnoli.photoborder.dialog;

import javafx.scene.paint.Color;

public class ColorTools {

    /**
     * Converte un colore RGB (componenti 0-1) nello spazio CIE L*a*b* (D65),
     * restituendo un array {L, a, b}.
     */
    public static double[] rgbToLab(Color color) {
        double r = pivotRgb(color.getRed());
        double g = pivotRgb(color.getGreen());
        double b = pivotRgb(color.getBlue());

        // Conversione sRGB lineare -> XYZ (matrice standard, illuminante D65)
        double x = r * 0.4124 + g * 0.3576 + b * 0.1805;
        double y = r * 0.2126 + g * 0.7152 + b * 0.0722;
        double z = r * 0.0193 + g * 0.1192 + b * 0.9505;

        // Bianco di riferimento D65
        double xn = 0.95047;
        double yn = 1.00000;
        double zn = 1.08883;

        double fx = pivotXyz(x / xn);
        double fy = pivotXyz(y / yn);
        double fz = pivotXyz(z / zn);

        double l = 116 * fy - 16;
        double a = 500 * (fx - fy);
        double bLab = 200 * (fy - fz);

        return new double[] { l, a, bLab };
    }  




    public static double pivotRgb(double channel) {
        return channel > 0.04045 ? Math.pow((channel + 0.055) / 1.055, 2.4) : channel / 12.92;
    }

    public static double pivotXyz(double t) {
        double delta = 6.0 / 29.0;
        return t > Math.pow(delta, 3) ? Math.cbrt(t) : (t / (3 * delta * delta) + 4.0 / 29.0);
    }

 /** Regola base dei toni incarnato: a > 0, b > 0, b >= a. */
    public static boolean isBasicSkinTone(double[] lab) {
        double a = lab[1];
        double b = lab[2];
        return a > 0 && b > 0 && b >= a;
    }

    /**
     * Regola "incarnato caucasico": regola base + Croma in (7,6; 21,6) + L in [76, 92],
     * a in [6, 16], b in [5, 15].
     */
    public static boolean isCaucasianSkinTone(double[] lab) {
        double l = lab[0];
        double a = lab[1];
        double b = lab[2];
        double chroma = Math.sqrt(a * a + b * b);
        return isBasicSkinTone(lab)
                && chroma > 7.6 && chroma < 21.6
                && l >= 76 && l <= 92
                && a >= 6 && a <= 16
                && b >= 5 && b <= 15;
    }

    /**
     * Regola "incarnato latino": regola base + Croma in (18,8; 39,5) + L in [60, 86],
     * a in [10, 24], b in [15, 33].
     */
    public static boolean isLatinSkinTone(double[] lab) {
        double l = lab[0];
        double a = lab[1];
        double b = lab[2];
        double chroma = Math.sqrt(a * a + b * b);
        return isBasicSkinTone(lab)
                && chroma > 18.8 && chroma < 39.5
                && l >= 60 && l <= 86
                && a >= 10 && a <= 24
                && b >= 15 && b <= 33;
    }

    /**
     * Regola "incarnato orientale": regola base + Croma in (22,2; 37,3) + L in [45, 75],
     * a in [14, 26], b in [16, 28].
     */
    public static boolean isOrientalSkinTone(double[] lab) {
        double l = lab[0];
        double a = lab[1];
        double b = lab[2];
        double chroma = Math.sqrt(a * a + b * b);
        return isBasicSkinTone(lab)
                && chroma > 22.2 && chroma < 37.3
                && l >= 45 && l <= 75
                && a >= 14 && a <= 26
                && b >= 16 && b <= 28;
    }

    /**
     * Regola "incarnato africano": regola base + Croma in (16; 40,8) + L in [32, 68],
     * a in [11, 28], b in [11, 29].
     */
    public static boolean isAfricanSkinTone(double[] lab) {
        double l = lab[0];
        double a = lab[1];
        double b = lab[2];
        double chroma = Math.sqrt(a * a + b * b);
        return isBasicSkinTone(lab)
                && chroma > 16 && chroma < 40.8
                && l >= 32 && l <= 68
                && a >= 11 && a <= 28
                && b >= 11 && b <= 29;
    }

    /**
     * Regola "vegetazione": a &lt; 0, b &gt; 0, e 1,2*|a| &lt;= b &lt;= 3*|a|.
     */
    public static boolean isVegetation(double[] lab) {
        double a = lab[1];
        double b = lab[2];
        if (!(a < 0 && b > 0)) {
            return false;
        }
        double absA = Math.abs(a);
        return b >= 1.2 * absA && b <= 3 * absA;
    }

    /**
     * Regola "cielo": b &lt; 0, a compreso tra -5 e 3.
     */
    public static boolean isSky(double[] lab) {
        double a = lab[1];
        double b = lab[2];
        return b < 0 && a >= -5 && a <= 3;
    }
}