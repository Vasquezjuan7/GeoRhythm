package com.example.georhythm.data

import java.util.Locale

object GpxExporter {

    fun toXml(track: GpxTrack): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"${track.creator}\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        sb.append("  <metadata>\n")
        sb.append("    <name>${escapeXml(track.name)}</name>\n")
        sb.append("    <desc>${escapeXml(track.description)}</desc>\n")
        sb.append("  </metadata>\n")

        for (wpt in track.waypoints) {
            sb.append(String.format(Locale.US, "  <wpt lat=\"%.6f\" lon=\"%.6f\">\n", wpt.lat, wpt.lon))
            sb.append("    <name>${escapeXml(wpt.name)}</name>\n")
            if (!wpt.description.isNullOrBlank()) {
                sb.append("    <desc>${escapeXml(wpt.description)}</desc>\n")
            }
            sb.append("  </wpt>\n")
        }

        sb.append("  <trk>\n")
        sb.append("    <name>${escapeXml(track.name)}</name>\n")
        sb.append("    <trkseg>\n")
        for (pt in track.points) {
            sb.append(String.format(Locale.US, "      <trkpt lat=\"%.6f\" lon=\"%.6f\">\n", pt.lat, pt.lon))
            if (pt.elevationM != null) {
                sb.append(String.format(Locale.US, "        <ele>%.1f</ele>\n", pt.elevationM))
            }
            if (pt.timestampIso != null) {
                sb.append("        <time>${pt.timestampIso}</time>\n")
            }
            if (pt.cadenceSpm != null) {
                sb.append("        <extensions><georhythm:cadence>${pt.cadenceSpm}</georhythm:cadence></extensions>\n")
            }
            sb.append("      </trkpt>\n")
        }
        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>")
        return sb.toString()
    }

    private fun escapeXml(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
