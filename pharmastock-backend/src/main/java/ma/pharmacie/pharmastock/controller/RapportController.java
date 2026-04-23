package ma.pharmacie.pharmastock.controller;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.pharmacie.pharmastock.entity.Lot;
import ma.pharmacie.pharmastock.entity.Vente;
import ma.pharmacie.pharmastock.enums.StatutVente;
import ma.pharmacie.pharmastock.repository.LotRepository;
import ma.pharmacie.pharmastock.repository.MedicamentRepository;
import ma.pharmacie.pharmastock.repository.VenteRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rapports")
@RequiredArgsConstructor
@Tag(name = "Rapports", description = "Génération de rapports PDF")
public class RapportController {

    private final LotRepository lotRepository;
    private final VenteRepository venteRepository;
    private final MedicamentRepository medicamentRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @GetMapping("/stock")
    @Operation(summary = "Rapport de stock en PDF")
    public ResponseEntity<byte[]> rapportStock() throws Exception {
        List<Lot> lots = lotRepository.findAllActifsWithMedicament(null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        ajouterTitre(doc, "Rapport de Stock", "Généré le " + LocalDate.now().format(FMT));

        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 15, 15, 15, 15}))
                .setWidth(UnitValue.createPercentValue(100));

        ajouterEnTeteTableau(table, "Médicament", "N° Lot", "Qté dispo.", "Expiration", "Statut");

        for (Lot lot : lots) {
            table.addCell(new Cell().add(new Paragraph(lot.getMedicament().getNomCommercial()).setFontSize(9)));
            table.addCell(new Cell().add(new Paragraph(lot.getNumeroLot()).setFontSize(9)));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(lot.getQuantiteDisponible())).setFontSize(9)));
            table.addCell(new Cell().add(new Paragraph(lot.getDateExpiration().format(FMT)).setFontSize(9)));
            table.addCell(new Cell().add(new Paragraph(lot.getStatut().name()).setFontSize(9)));
        }

        doc.add(table);
        doc.add(new Paragraph("Total : " + lots.size() + " lots actifs").setFontSize(9).setMarginTop(10));
        doc.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rapport-stock-" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(baos.toByteArray());
    }

    @GetMapping("/ventes")
    @Operation(summary = "Rapport des ventes en PDF")
    public ResponseEntity<byte[]> rapportVentes(
            @RequestParam LocalDate dateFrom,
            @RequestParam LocalDate dateTo) throws Exception {

        LocalDateTime from = dateFrom.atStartOfDay();
        LocalDateTime to = dateTo.atTime(23, 59, 59);

        List<Vente> ventes = venteRepository.searchByQAndBetween(
                null,
                from,
                to,
                PageRequest.of(0, 1000)
        ).getContent();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        ajouterTitre(doc, "Rapport des Ventes",
                "Période : " + dateFrom.format(FMT) + " → " + dateTo.format(FMT));

        Table table = new Table(UnitValue.createPercentArray(new float[]{20, 20, 20, 20, 20}))
                .setWidth(UnitValue.createPercentValue(100));

        ajouterEnTeteTableau(table, "N° Vente", "Date", "Total TTC", "Mode paiement", "Statut");

        BigDecimal totalGeneral = BigDecimal.ZERO;

        for (Vente v : ventes) {
            table.addCell(cellule(v.getNumeroVente()));
            table.addCell(cellule(v.getDateVente().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
            table.addCell(cellule(v.getTotalTtc() + " DH"));
            table.addCell(cellule(v.getModePaiement().name()));
            table.addCell(cellule(v.getStatut().name()));

            if (v.getStatut() == StatutVente.VALIDEE) {
                totalGeneral = totalGeneral.add(v.getTotalTtc());
            }
        }

        doc.add(table);
        doc.add(new Paragraph("Total CA : " + totalGeneral + " DH — " + ventes.size() + " ventes")
                .setBold()
                .setFontSize(10)
                .setMarginTop(12));
        doc.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rapport-ventes-" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(baos.toByteArray());
    }

    @GetMapping("/peremptions")
    @Operation(summary = "Rapport des péremptions en PDF")
    public ResponseEntity<byte[]> rapportPeremptions(
            @RequestParam(defaultValue = "90") int jours) throws Exception {

        List<Lot> lots = lotRepository.findLotsExpirantAvant(LocalDate.now().plusDays(jours));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        ajouterTitre(doc, "Rapport des Péremptions",
                "Lots expirant dans les " + jours + " prochains jours — " + LocalDate.now().format(FMT));

        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 20, 20, 15, 15}))
                .setWidth(UnitValue.createPercentValue(100));

        ajouterEnTeteTableau(table, "Médicament", "N° Lot", "Expiration", "Qté restante", "Jours restants");

        for (Lot lot : lots) {
            long joursRestants = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), lot.getDateExpiration());

            table.addCell(cellule(lot.getMedicament().getNomCommercial()));
            table.addCell(cellule(lot.getNumeroLot()));
            table.addCell(cellule(lot.getDateExpiration().format(FMT)));
            table.addCell(cellule(String.valueOf(lot.getQuantiteDisponible())));

            Cell joursCell = new Cell().add(new Paragraph(joursRestants + " j").setFontSize(9));
            if (joursRestants <= 7) {
                joursCell.setBackgroundColor(ColorConstants.RED).setFontColor(ColorConstants.WHITE);
            } else if (joursRestants <= 30) {
                joursCell.setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(255, 193, 7));
            }
            table.addCell(joursCell);
        }

        doc.add(table);
        doc.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rapport-peremptions-" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(baos.toByteArray());
    }

    private void ajouterTitre(Document doc, String titre, String sousTitre) {
        doc.add(new Paragraph("PharmaStock Pro").setFontSize(10).setFontColor(ColorConstants.GRAY));
        doc.add(new Paragraph(titre).setFontSize(18).setBold()
                .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(15, 118, 110)));
        doc.add(new Paragraph(sousTitre).setFontSize(10).setFontColor(ColorConstants.GRAY).setMarginBottom(16));
    }

    private void ajouterEnTeteTableau(Table table, String... headers) {
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFontSize(9).setBold().setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(15, 118, 110))
                    .setPadding(6));
        }
    }

    private Cell cellule(String texte) {
        return new Cell()
                .add(new Paragraph(texte != null ? texte : "—").setFontSize(9))
                .setPadding(5);
    }
}