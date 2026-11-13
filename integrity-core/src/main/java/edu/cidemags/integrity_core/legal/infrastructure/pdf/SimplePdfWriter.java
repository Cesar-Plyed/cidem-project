package edu.cidemags.integrity_core.legal.infrastructure.pdf;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Generador PDF 1.4 minimo (sin dependencias externas) para el export de EIPD.
 *
 * <p>Soporta una pagina A4 con Helvetica. Las lineas no ASCII se transliteran
 * para no depender de fuentes embebidas.</p>
 */
public final class SimplePdfWriter {

    private static final int LINES_PER_PAGE = 50;

    private SimplePdfWriter() {
    }

    public static byte[] write(List<String> lines) {
        List<String> safe = new ArrayList<>();
        for (String line : lines) {
            safe.add(ascii(line));
        }
        if (safe.isEmpty()) {
            safe.add("");
        }

        String content = contentStream(safe);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        writeAscii(out, "%PDF-1.4\n");

        offsets.add(out.size());
        writeAscii(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        offsets.add(out.size());
        writeAscii(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        offsets.add(out.size());
        writeAscii(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                + "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n");
        offsets.add(out.size());
        writeAscii(out, "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");
        offsets.add(out.size());
        writeAscii(out, "5 0 obj\n<< /Length " + content.getBytes(StandardCharsets.US_ASCII).length
                + " >>\nstream\n" + content + "endstream\nendobj\n");

        int xrefOffset = out.size();
        StringBuilder xref = new StringBuilder("xref\n0 6\n0000000000 65535 f \n");
        for (Integer offset : offsets) {
            xref.append(String.format("%010d 00000 n \n", offset));
        }
        writeAscii(out, xref.toString());
        writeAscii(out, "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF\n");
        return out.toByteArray();
    }

    private static String contentStream(List<String> lines) {
        StringBuilder stream = new StringBuilder("BT\n/F1 11 Tf\n50 800 Td\n14 TL\n");
        int printed = 0;
        for (String line : lines) {
            if (printed >= LINES_PER_PAGE) {
                break;
            }
            stream.append('(').append(escape(line)).append(") Tj\nT*\n");
            printed++;
        }
        stream.append("ET\n");
        return stream.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static String ascii(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static void writeAscii(ByteArrayOutputStream out, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        out.write(bytes, 0, bytes.length);
    }
}
