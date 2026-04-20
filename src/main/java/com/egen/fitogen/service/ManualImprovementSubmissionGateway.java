package com.egen.fitogen.service;

import com.egen.fitogen.dto.ImprovementSubmissionDraft;
import com.egen.fitogen.dto.ImprovementSubmissionResult;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class ManualImprovementSubmissionGateway implements ImprovementSubmissionGateway {

    @Override
    public ImprovementSubmissionResult submit(ImprovementSubmissionDraft draft) throws Exception {
        Path draftFile = Files.createTempFile("fitogen-ulepszenie-", ".json");
        Files.writeString(draftFile, draft.toJson(), StandardCharsets.UTF_8);

        boolean mailOpened = false;
        String email = draft.getFallbackEmail();
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL) && email != null && !email.isBlank()) {
            String subject = "Fito Gen – zgłoszenie ulepszenia: " + draft.getTitle();
            String attachmentLines = draft.getAttachments().isEmpty()
                    ? "Brak załączników."
                    : draft.getAttachments().stream()
                    .map(path -> "- " + path.toAbsolutePath().normalize())
                    .collect(Collectors.joining("\n"));
            String body = "Rodzaj: " + draft.getType() + "\n"
                    + "Priorytet: " + draft.getPriority() + "\n"
                    + "Data przygotowania: " + draft.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n\n"
                    + "Opis:\n" + draft.getDescription() + "\n\n"
                    + "Oczekiwany efekt:\n" + draft.getExpectedBenefit() + "\n\n"
                    + "Załączniki do dołączenia ręcznie:\n" + attachmentLines + "\n\n"
                    + "Szkic JSON zapisano w pliku:\n" + draftFile.toAbsolutePath().normalize() + "\n";
            URI mailUri = new URI("mailto:" + encode(email)
                    + "?subject=" + encode(subject)
                    + "&body=" + encode(body));
            Desktop.getDesktop().mail(mailUri);
            mailOpened = true;
        }

        String message = mailOpened
                ? "Przygotowano ręczne zgłoszenie i otwarto domyślną aplikację pocztową."
                : "Przygotowano ręczne zgłoszenie. Domyślna aplikacja pocztowa nie została otwarta automatycznie.";
        return new ImprovementSubmissionResult("MANUAL_EMAIL", message, draftFile, mailOpened);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
