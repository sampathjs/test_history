package com.matthey.pmm.metal.rentals.document;

import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter;
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import static com.rainerhahnekamp.sneakythrow.Sneaky.sneak;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

@Component
public class DocumentTemplate {

    private static final Logger logger = LoggerFactory.getLogger(DocumentTemplate.class);

    private final Resource template;

    public DocumentTemplate(@Value("classpath:statement template.docx") Resource template) {
        this.template = template;
    }

    private static String getVariableToken(String variableName) {
        return "$" + variableName;
    }

    public void generateDocument(Map<String, Object> variables, String outputPath) {
        XWPFDocument document = sneak(() -> new XWPFDocument(template.getInputStream()));

        processParagraphs(document, variables);
        document.getHeaderList().forEach(h -> processParagraphs(h, variables));
        document.getFooterList().forEach(f -> processParagraphs(f, variables));

        for (var table : document.getTables()) {
            var rows = table.getRows();
            for (var row = 0; row < rows.size() - 1; row++) {
                table.getRow(row).getTableCells().forEach(c -> processParagraphs(c, variables, -1));
            }

            var rowTemplateIdx = rows.size() - 1;
            var rowTemplate = rows.get(rowTemplateIdx);
            var variablesForLoop = getVariablesForLoop(rowTemplate, variables);
            logger.debug("variables for loop: {}", variablesForLoop);
            var loopCount = getLoopCount(variablesForLoop);
            logger.debug("loop count: {}", loopCount);
            for (var loopRow = 0; loopRow < loopCount; loopRow++) {
                var newRow = new XWPFTableRow(sneak(() -> CTRow.Factory.parse(rowTemplate.getCtRow().newInputStream())),
                                              table);
                for (var cell : newRow.getTableCells()) {
                    processParagraphs(cell, variables, loopRow);
                }
                table.addRow(newRow);
            }
            if (loopCount > 0) {
                table.removeRow(rowTemplateIdx);
            }
        }

        generateOutput(document, outputPath);
    }

    private Map<String, Object> getVariablesForLoop(XWPFTableRow rowTemplate, Map<String, Object> variables) {
        var allText = rowTemplate.getTableCells()
                .stream()
                .map(XWPFTableCell::getParagraphs)
                .flatMap(List::stream)
                .map(XWPFParagraph::getText)
                .collect(joining());
        return variables.entrySet()
                .stream()
                .filter(entry -> allText.contains(getVariableToken(entry.getKey())))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @SuppressWarnings("rawtypes")
    private int getLoopCount(Map<String, Object> variables) {
        return variables.values()
                .stream()
                .filter(value -> value instanceof List)
                .mapToInt(value -> ((List) value).size())
                .max()
                .orElse(1);
    }

    private void processParagraphs(IBody body, Map<String, Object> variables) {
        processParagraphs(body, variables, -1);
    }

    private void processParagraphs(IBody body, Map<String, Object> variables, int loopIdx) {
        body.getParagraphs().forEach(paragraph -> {
            String originalText = paragraph.getText();
            String updatedText = replaceVariables(originalText, variables, loopIdx);
            if (!originalText.equals(updatedText)) {
                logger.debug("text to be replaced: original -> {}, updated -> {}", originalText, updatedText);
                IntStream.range(0, paragraph.getRuns().size()).forEach(paragraph::removeRun);
                IntStream.range(0, paragraph.getRuns().size()).forEach(paragraph::removeRun);
                paragraph.insertNewRun(0).setText(updatedText);
            }
        });
    }

    private String replaceVariables(final String text, Map<String, Object> variables, int loopIdx) {
        return variables.entrySet()
                .stream()
                .reduce(text,
                        (result, entry) -> result.replace(getVariableToken(entry.getKey()),
                                                          Objects.toString(getVariableValue(entry.getValue(), loopIdx),
                                                                           "")),
                        (oldResult, newResult) -> newResult);
    }

    @SuppressWarnings("rawtypes")
    private Object getVariableValue(Object value, int loopIdx) {
        if (loopIdx == -1) {
            return value;
        } else {
            if (value instanceof List) {
                List valueList = (List) value;
                return valueList.size() > loopIdx ? valueList.get(loopIdx) : null;
            } else {
                return value;
            }
        }
    }

    private void generateOutput(XWPFDocument document, String outputPath) {
        try {
            PdfOptions options = PdfOptions.create();
            PdfConverter.getInstance().convert(document, new FileOutputStream(new File(outputPath)), options);
        } catch (IOException e) {
            throw new RuntimeException("error occurred when generating document: " + e.getMessage(), e);
        }
    }
}
