package backend_cliente;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SoapClient {

    private static final String SOAP_URL = "http://localhost:8080/ServicioSOAP";
    private static final String NAMESPACE = "http://central/";

    public static String sendSoapRequest(String bodyXml) {
        String envelope = String.format(
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cen=\"%s\">\n" +
            "   <soapenv:Header/>\n" +
            "   <soapenv:Body>\n" +
            "      %s\n" +
            "   </soapenv:Body>\n" +
            "</soapenv:Envelope>",
            NAMESPACE, bodyXml);

        try {
            URL url = new URL(SOAP_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            connection.setDoOutput(true);

            // Enviar cuerpo
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = envelope.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int status = connection.getResponseCode();
            InputStream is = (status >= 200 && status < 300) 
                ? connection.getInputStream() 
                : connection.getErrorStream();

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line).append("\n");
                }
            }

            connection.disconnect();

            if (status != 200) {
                throw new RuntimeException("SOAP error: HTTP " + status + "\nResponse: " + response);
            }

            return response.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calling SOAP service", e);
        }
    }
}