package com.natyrov.spring.detection_with_vae.service.parsing;

import com.natyrov.spring.detection_with_vae.dto.ColumnInfoDto;
import com.natyrov.spring.detection_with_vae.dto.DatasetPreviewDto;
import com.natyrov.spring.detection_with_vae.entity.Dataset;
import com.natyrov.spring.detection_with_vae.entity.User;
import com.natyrov.spring.detection_with_vae.repository.DatasetRepository;
import com.natyrov.spring.detection_with_vae.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DatasetParsingServiceImpl implements DatasetParsingService{

    private final DatasetRepository datasetRepository;
    private final UserRepository userRepository;

    @Override
    public DatasetPreviewDto previewDataset(Long datasetId, String userEmail) {
        User user = userRepository.findAllByEmail(userEmail)
                .orElseThrow(()-> new RuntimeException("Пользователь не найден"));

        Dataset dataset = datasetRepository.findByOwnerAndId(user, datasetId)
                .orElseThrow(()-> new RuntimeException("Датасет не найден или доступ запрещен"));

        if(!dataset.getOwner().getId().equals(user.getId())){
            throw new RuntimeException("Нет доступа к этому датасету");
        }

        String fileType = dataset.getFileType().toLowerCase();
        if("csv".equals(fileType)){
            return parseCsv(dataset);
        } else if ("xlsx".equals(fileType)) {
            return parseXlsx(dataset);
        } else{
            throw new RuntimeException("Неподдерживаемый формат файла");
        }
    }

    private DatasetPreviewDto parseCsv(Dataset dataset) {
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> previewRows = new ArrayList<>();
        Map<String, List<String>> columnValues = new LinkedHashMap<>();
        int totalRows = 0;

        // --- ШАГ 1: АВТООПРЕДЕЛЕНИЕ РАЗДЕЛИТЕЛЯ ---
        char delimiter = ','; // По умолчанию запятая
        try (BufferedReader br = new BufferedReader(new FileReader(dataset.getFilePath()))) {
            String firstLine = br.readLine();
            if (firstLine != null && firstLine.contains(";")) {
                delimiter = ';'; // Если нашли точку с запятой, переключаемся на неё
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при определении разделителя CSV", e);
        }
        // -----------------------------------------

        try(FileReader reader = new FileReader(dataset.getFilePath());
            CSVParser parser = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setDelimiter(delimiter) // <-- ШАГ 2: ПЕРЕДАЕМ НАЙДЕННЫЙ РАЗДЕЛИТЕЛЬ СЮДА
                    .build()
                    .parse(reader))
        {
            headers.addAll(parser.getHeaderMap().keySet());
            for(String header:headers){
                columnValues.put(header, new ArrayList<>());
            }

            for(CSVRecord record: parser){
                totalRows += 1;

                Map<String, String> rowMap = new LinkedHashMap<>();
                for(String header:headers){
                    String value = record.get(header);
                    rowMap.put(header, value);
                    columnValues.get(header).add(value);
                }
                if(previewRows.size() < 10){
                    previewRows.add(rowMap);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения csv", e);
        }
        return buildPreview(dataset, headers, previewRows, columnValues, totalRows);
    }

    private DatasetPreviewDto parseXlsx(Dataset dataset) {
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> previewRows = new ArrayList<>();
        Map<String, List<String>> columnValues = new LinkedHashMap<>();
        int totalRows = 0;

        try(Workbook workbook = WorkbookFactory.create(new File(dataset.getFilePath())))
        {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            if(!rowIterator.hasNext()){
                throw new RuntimeException("Файл Excel пустой");
            }

            Row headerRow = rowIterator.next();

            for(Cell cell:headerRow){
                String header = getCellAsString(cell);
                headers.add(header);
                columnValues.put(header, new ArrayList<>());
            }

            while(rowIterator.hasNext()){
                Row row = rowIterator.next();
                totalRows +=1;

                Map<String, String> rowMap = new LinkedHashMap<>();
                for(int i = 0; i< headers.size(); i++){
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String value = getCellAsString(cell);
                    rowMap.put(headers.get(i), value);
                    columnValues.get(headers.get(i)).add(value);
                }
                if(previewRows.size()<10){
                    previewRows.add(rowMap);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения csv", e);
        }
        return buildPreview(dataset, headers, previewRows, columnValues, totalRows);
    }



    private DatasetPreviewDto buildPreview(Dataset dataset,
                                           List<String> headers,
                                           List<Map<String, String>> previewRows,
                                           Map<String, List<String>> columnValues,
                                           int totalRows)
    {
        List<ColumnInfoDto> columns = new ArrayList<>();
        List<String> numericColumns = new ArrayList<>();
        String detectedTimeColumn = null;

        for(String header: headers){
            List<String> values = columnValues.get(header);

            long missingCount = values.stream()
                    .filter(v-> v==null || v.isBlank())
                    .count();

            boolean numeric = isMostlyNumeric(values);
            boolean timestampCandidate = isTimestampColumn(header, values);
            String type = detectType(values, numeric, timestampCandidate);

            if(numeric){
                numericColumns.add(header);
            }
            if(detectedTimeColumn == null && timestampCandidate){
                detectedTimeColumn = header;
            }

            columns.add(new ColumnInfoDto(
                    header,
                    type,
                    numeric,
                    timestampCandidate,
                    missingCount
            ));
        }

        return DatasetPreviewDto.builder()
                .fileName(dataset.getOriginalFileName())
                .fileType(dataset.getFileType())
                .totalRows(totalRows)
                .totalColumns(headers.size())
                .detectedTimeColumn(detectedTimeColumn)
                .headers(headers)
                .columns(columns)
                .previewRows(previewRows)
                .numericColumns(numericColumns)
                .build();
    }

    private String detectType(List<String> values, boolean numeric, boolean timestampCandidate) {
        if(timestampCandidate){
            return "TIMESTAMP";
        }
        if(numeric){
            return "NUMERIC";
        }

        return "STRING";
    }

    private boolean isTimestampColumn(String header, List<String> values) {
        String lower = header.toLowerCase();
        if(lower.contains("time") || lower.contains("date") || lower.contains("timestamp")){
            return true;
        }
        int checked = 0;
        int parsed = 0;

        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            checked++;
            if (looksLikeDateTime(value)) {
                parsed++;
            }
        }

        if (checked == 0) {
            return false;
        }

        return (double) parsed / checked >= 0.7;
    }

    private boolean looksLikeDateTime(String value) {
        try {
            LocalDateTime.parse(value);
            return true;
        } catch (DateTimeParseException ignored) {
        }

        return value.matches("\\d{4}-\\d{2}-\\d{2}.*");
    }

    private boolean isMostlyNumeric(List<String> values) {

        int checked = 0;
        int numericCount = 0;

        for(String value:values){
            if(value == null || value.isBlank()) continue;

            checked++;

            try{
                Double.parseDouble(value.replace(",", "."));
                numericCount++;
            } catch (NumberFormatException ignored) {
            }
        }
        if(checked == 0) return false;

        return (double) numericCount/checked>=0.8;
    }

    private String getCellAsString(Cell cell) {
        if(cell == null){
            return "";
        }

        return switch(cell.getCellType()){
            case STRING->cell.getStringCellValue();
            case NUMERIC->{
                if(DateUtil.isCellDateFormatted(cell)){
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                yield String.valueOf(cell.getNumericCellValue());
            }
            case BOOLEAN-> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            case BLANK -> "";
            default -> "";
        };
    }

}
