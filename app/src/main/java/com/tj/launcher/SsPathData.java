package com.tj.launcher;

import android.graphics.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BN1: a minimal SVG path-data reader for the battery shapes (android.util.PathParser is hidden in the SDK and AndroidX is not in the app). Supports exactly what the framework's config_batterymeter* strings use:
 * M m L l H h V v C c Z z, numbers separated by commas, spaces or by their own sign, repeated parameter sets after one command letter.
 */
final class SsPathData {
    private SsPathData() {}

    private static final Pattern NUMBER = Pattern.compile("[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?");

    /** Never throws: a malformed string gives whatever was built up to the error. */
    static Path parse(String d) {
        Path p = new Path();
        try { build(p, d); } catch (RuntimeException ignored) { }
        return p;
    }

    private static void build(Path p, String d) {
        float cx = 0, cy = 0, sx = 0, sy = 0;   // current point, start of the sub path
        int i = 0, n = d.length();
        while (i < n) {
            char c = d.charAt(i);
            if (!Character.isLetter(c)) { i++; continue; }
            i++;
            int end = i;
            while (end < n && !Character.isLetter(d.charAt(end))) end++;   // the numbers of this command
            Matcher m = NUMBER.matcher(d.substring(i, end));
            java.util.ArrayList<Float> v = new java.util.ArrayList<>();
            while (m.find()) v.add(Float.parseFloat(m.group()));
            i = end;
            boolean rel = Character.isLowerCase(c);
            switch (Character.toLowerCase(c)) {
                case 'm':
                    for (int k = 0; k + 1 < v.size(); k += 2) {
                        float x = v.get(k) + (rel ? cx : 0), y = v.get(k + 1) + (rel ? cy : 0);
                        if (k == 0) { p.moveTo(x, y); sx = x; sy = y; } else p.lineTo(x, y);   // extra pairs after a move are line-tos
                        cx = x; cy = y;
                    }
                    break;
                case 'l':
                    for (int k = 0; k + 1 < v.size(); k += 2) { cx = v.get(k) + (rel ? cx : 0); cy = v.get(k + 1) + (rel ? cy : 0); p.lineTo(cx, cy); }
                    break;
                case 'h':
                    for (float x : v) { cx = x + (rel ? cx : 0); p.lineTo(cx, cy); }
                    break;
                case 'v':
                    for (float y : v) { cy = y + (rel ? cy : 0); p.lineTo(cx, cy); }
                    break;
                case 'c':
                    for (int k = 0; k + 5 < v.size(); k += 6) {
                        float ox = rel ? cx : 0, oy = rel ? cy : 0;
                        p.cubicTo(v.get(k) + ox, v.get(k + 1) + oy, v.get(k + 2) + ox, v.get(k + 3) + oy, v.get(k + 4) + ox, v.get(k + 5) + oy);
                        cx = v.get(k + 4) + ox; cy = v.get(k + 5) + oy;
                    }
                    break;
                case 'z':
                    p.close(); cx = sx; cy = sy;
                    break;
                default:
                    break;
            }
        }
    }
}
