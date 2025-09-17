package co.pragma.infrastructure;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

public class SesEmailSender {

    private final SesClient sesClient;
    private final String fromEmail;

    public SesEmailSender(SesClient sesClient, String fromEmail) {
        this.sesClient = sesClient;
        this.fromEmail = fromEmail;
    }

    public void enviarCorreo(String to, String subject, String body) {
        SendEmailRequest request = SendEmailRequest.builder()
                .source(fromEmail)
                .destination(d -> d.toAddresses(to))
                .message(m -> m
                        .subject(s -> s.data(subject).charset("UTF-8"))
                        .body(b -> b.html(h -> h.data(body).charset("UTF-8")))
                )
                .build();

        sesClient.sendEmail(request);
    }
}
