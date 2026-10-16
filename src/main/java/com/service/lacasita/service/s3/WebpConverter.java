package com.service.lacasita.service.s3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Convierte una imagen a WebP antes de subirla a S3: mismo contenido, archivo
 * más chico, entrega más rápida para quien la consulta después.
 *
 * <p>Usa el binario {@code cwebp} (paquete {@code webp} del sistema, instalado
 * en el Dockerfile) en vez de una librería Java: no existe un codificador
 * WebP puro-JVM sin dependencia nativa por debajo, así que invocar el binario
 * directo es más simple que envolver la misma dependencia con un wrapper JNI.
 *
 * <p>Si el binario falta, la conversión falla o se pasa de tiempo, o el
 * archivo es GIF (uno animado perdería la animación) o ya es WebP, se
 * devuelve el original tal cual — un problema de compresión nunca debe tumbar
 * una subida.
 *
 * @author Jason Nunez
 */
@Component
public class WebpConverter {

    private static final Logger log = LoggerFactory.getLogger(WebpConverter.class);

    private static final int CALIDAD = 82;
    private static final long TIMEOUT_SEGUNDOS = 15;

    public record Resultado(byte[] datos, String nombreArchivo, String contentType) {}

    public Resultado convertir(byte[] original, String nombreOriginal, String contentTypeOriginal) {

        String tipo = contentTypeOriginal == null ? "" : contentTypeOriginal.toLowerCase();

        if (tipo.equals("image/webp") || tipo.equals("image/gif") || !tipo.startsWith("image/")) {
            return new Resultado(original, nombreOriginal, contentTypeOriginal);
        }

        Path entrada = null;
        Path salida = null;

        try {
            entrada = Files.createTempFile("webp-in-", extensionDe(nombreOriginal));
            salida = Files.createTempFile("webp-out-", ".webp");
            Files.write(entrada, original);

            Process proceso = new ProcessBuilder(
                    "cwebp", "-quiet", "-q", String.valueOf(CALIDAD),
                    entrada.toString(), "-o", salida.toString())
                    .redirectErrorStream(true)
                    .start();

            if (!proceso.waitFor(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)) {
                proceso.destroyForcibly();
                log.warn("cwebp superó el tiempo límite convirtiendo '{}'; se sube el original", nombreOriginal);
                return new Resultado(original, nombreOriginal, contentTypeOriginal);
            }

            if (proceso.exitValue() != 0 || Files.size(salida) == 0) {
                log.warn("cwebp no pudo convertir '{}' (código {}); se sube el original",
                        nombreOriginal, proceso.exitValue());
                return new Resultado(original, nombreOriginal, contentTypeOriginal);
            }

            return new Resultado(Files.readAllBytes(salida), sinExtension(nombreOriginal) + ".webp", "image/webp");

        } catch (IOException e) {
            log.warn("No se pudo convertir '{}' a WebP: {}; se sube el original", nombreOriginal, e.getMessage());
            return new Resultado(original, nombreOriginal, contentTypeOriginal);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Conversión a WebP interrumpida para '{}'; se sube el original", nombreOriginal);
            return new Resultado(original, nombreOriginal, contentTypeOriginal);

        } finally {
            borrar(entrada);
            borrar(salida);
        }
    }

    private void borrar(Path p) {
        if (p == null) return;
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
            // Archivo temporal; lo limpia el sistema operativo tarde o temprano.
        }
    }

    private String extensionDe(String nombre) {
        int i = nombre == null ? -1 : nombre.lastIndexOf('.');
        return i >= 0 ? nombre.substring(i) : ".tmp";
    }

    private String sinExtension(String nombre) {
        if (nombre == null) return "imagen";
        int i = nombre.lastIndexOf('.');
        return i >= 0 ? nombre.substring(0, i) : nombre;
    }
}
