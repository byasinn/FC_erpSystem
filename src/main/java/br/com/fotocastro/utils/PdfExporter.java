package br.com.fotocastro.utils;

import br.com.fotocastro.infra.VendaDaoH2.FechamentoResumo;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
public class PdfExporter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String FONT = StandardFonts.HELVETICA;

    public static void exportFechamentoToPdf(FechamentoResumo fechamento, String filePath) {
        if (fechamento == null) {
            throw new IllegalArgumentException("Fechamento não pode ser nulo");
        }

        try {
            PdfWriter writer = new PdfWriter(new FileOutputStream(filePath));
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            PdfFont font = PdfFontFactory.createFont(FONT);

            // Cabeçalho
            Paragraph title = new Paragraph("FECHAMENTO DE CAIXA - FotoCastro")
                    .setFont(font)
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            doc.add(title);

            Paragraph data = new Paragraph("Data: " + fechamento.getData() + "   Fechado em: " + fechamento.getFechadoEm())
                    .setFont(font)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER);
            doc.add(data);

            doc.add(new Paragraph("\n")); // espaçamento

            // Tabela principal
            float[] columnWidths = {200f, 200f};
            Table table = new Table(UnitValue.createPointArray(columnWidths))
                    .setWidth(UnitValue.createPercentValue(100));

            // Linhas da tabela
            addRow(table, "Bruto:", fechamento.getBruto());
            addRow(table, "Líquido:", fechamento.getLiquido());
            addRow(table, "Total Pagamentos (Din + Cart + Pix):", fechamento.getTotalPagamentos());
            addRow(table, "Valor Contado:", "R$ " + String.format("%.2f", fechamento.valorContado));
            addRow(table, "Diferença:", fechamento.getDiferenca());
            addRow(table, "Tipo:", fechamento.getTipo());
            addRow(table, "Observação:", fechamento.getObservacao());

            doc.add(table);

            // Rodapé
            doc.add(new Paragraph("\n"));
            Paragraph footer = new Paragraph("Gerado em: " + LocalDateTime.now().format(FMT))
                    .setFont(font)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER);
            doc.add(footer);

            doc.close();

            System.out.println("PDF gerado com sucesso: " + filePath);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao criar arquivo PDF", e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro geral na geração de PDF", e);
        }
    }

    private static void addRow(Table table, String label, String value) {
        Cell cellLabel = new Cell().add(new Paragraph(label).setBold());
        Cell cellValue = new Cell().add(new Paragraph(value));
        table.addCell(cellLabel);
        table.addCell(cellValue);
    }
}