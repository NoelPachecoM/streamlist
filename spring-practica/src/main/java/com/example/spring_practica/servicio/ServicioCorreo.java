package com.example.spring_practica.servicio;

import com.example.spring_practica.excepcion.ArcoException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envia los correos (codigos de registro, cambio de correo, recuperacion y cancelacion).
 *
 *  1. Si hay correo.resend-api-key -> por la API HTTP de Resend (lo que se usa en Render,
 *     porque el plan gratis de Render bloquea los puertos de correo SMTP).
 *  2. Si no, por SMTP con Gmail (spring.mail.*), como en tu computadora.
 *  3. Si tampoco hay contrasena de Gmail, el correo se SIMULA: se imprime en la consola.
 */
@Service
public class ServicioCorreo {

    private static final String URL_RESEND = "https://api.resend.com/emails";

    private final JavaMailSender mailSender;
    private final String resendApiKey;
    private final String remitenteResend;
    private final String usuarioGmail;
    private final String claveGmail;
    private final RestClient cliente;

    public ServicioCorreo(ObjectProvider<JavaMailSender> proveedor,
                          @Value("${correo.resend-api-key:}") String resendApiKey,
                          @Value("${correo.remitente:}") String remitenteResend,
                          @Value("${spring.mail.username:}") String usuarioGmail,
                          @Value("${spring.mail.password:}") String claveGmail) {
        this.mailSender = proveedor.getIfAvailable();
        this.resendApiKey = resendApiKey == null ? "" : resendApiKey.trim();
        this.remitenteResend = remitenteResend == null ? "" : remitenteResend.trim();
        this.usuarioGmail = usuarioGmail;
        this.claveGmail = claveGmail;

        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        this.cliente = RestClient.builder().requestFactory(fabrica).build();
    }

    public void enviar(String destinatario, String asunto, String texto) {
        if (!resendApiKey.isBlank()) {
            enviarConResend(destinatario, asunto, texto);
            return;
        }
        if (mailSender == null || claveGmail == null || claveGmail.isBlank() || claveGmail.startsWith("PEGA_AQUI")) {
            System.out.println("===== CORREO SIMULADO (no hay Resend ni Gmail configurado) =====");
            System.out.println("Para:   " + destinatario);
            System.out.println("Asunto: " + asunto);
            System.out.println(texto);
            System.out.println("=================================================================");
            return;
        }
        enviarConGmail(destinatario, asunto, texto);
    }

    private void enviarConResend(String destinatario, String asunto, String texto) {
        if (remitenteResend.isBlank()) {
            throw new ArcoException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Falta CORREO_REMITENTE (ej. StreamList <no-responder@paginanoel.online>)");
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("from", remitenteResend);
        cuerpo.put("to", List.of(destinatario));
        cuerpo.put("subject", asunto);
        cuerpo.put("text", texto);
        try {
            cliente.post()
                    .uri(URL_RESEND)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            System.out.println("ERROR DE RESEND (" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
            throw new ArcoException(HttpStatus.BAD_GATEWAY,
                    "No se pudo enviar el correo (Resend respondio " + e.getStatusCode().value() + ")");
        } catch (RestClientException e) {
            System.out.println("ERROR AL CONECTAR CON RESEND: " + e.getMessage());
            throw new ArcoException(HttpStatus.BAD_GATEWAY, "No se pudo conectar con el servicio de correo");
        }
    }

    private void enviarConGmail(String destinatario, String asunto, String texto) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(texto);
            if (usuarioGmail != null && !usuarioGmail.isBlank()) {
                mensaje.setFrom(usuarioGmail);
            }
            mailSender.send(mensaje);
        } catch (Exception e) {
            Throwable causa = e;
            while (causa.getCause() != null) {
                causa = causa.getCause();
            }
            System.out.println("ERROR AL ENVIAR CORREO: " + causa.getMessage());
            throw new ArcoException(HttpStatus.BAD_GATEWAY,
                    "No se pudo enviar el correo. Revisa spring.mail.username y la contrasena de aplicacion de Gmail.");
        }
    }
}
