package com.silesta;

import com.silesta.interfaces.ExtractorConfiguration;
import com.silesta.interfaces.IExtractor;
import com.silesta.models.Tables;
import com.silesta.models.tables.records.BankOperationsRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.silesta.models.Tables.BANK_OPERATIONS;

public class BankCardDataExtractor implements IExtractor {
    private ExtractorConfiguration config;
    private String cardName;

    public BankCardDataExtractor() {
    }


    /**
     * Extract all operations from given html file.
     * Expected Sberbank online history format
     *
     * @param htmlFilePath
     * @return
     */
    private List<BankOperationsRecord> extractOperationsFromHtml(String htmlFilePath) {
        List<BankOperationsRecord> operations = new ArrayList<>();

        try (FileInputStream inputStream = new FileInputStream(htmlFilePath)) {
            String everything = IOUtils.toString(inputStream);
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
            // period
            String htmlPeriodRegex = "<th colspan=\"3\" style=\"padding-left: 0em; color\\: green; border-bottom: \\.2em solid green\\; line-height: \\.8em;\">\n" +
                    ".*ПЕРИОД СОВЕРШЕНИЯ ОПЕРАЦИЙ ([0-9]{2}\\.[0-9]{2}\\.[0-9]{4}) — ([0-9]{2}\\.[0-9]{2}\\.[0-9]{4})\n" +
                    ".*</th>";
            Pattern periodPattern = Pattern.compile(htmlPeriodRegex);
            Matcher periodMatcher = periodPattern.matcher(everything);
            Date periodStart;
            Date periodEnd;

            if (periodMatcher.find()) {
                periodStart = formatter.parse(periodMatcher.group(1));
                periodEnd = formatter.parse(periodMatcher.group(2));
                // clear period in database only with this source
                this.config.getCTX().deleteFrom(Tables.BANK_OPERATIONS)
                        .where(Tables.BANK_OPERATIONS.OP_DATE.greaterOrEqual(new java.sql.Date(periodStart.getTime()))
                        .and(Tables.BANK_OPERATIONS.OP_DATE.lessOrEqual(new java.sql.Date(periodEnd.getTime())))
                        .and(Tables.BANK_OPERATIONS.SOURCE.eq(this.cardName))).execute();
            }

            // operations
            String htmlOperationsRegex = "(<tr>\\s*\n\\s*<td.*>(.*)</td>\n\\s*<td.*>(.*) RUB</td>\n" +
                                            "\\s*<td.*>(.*)</td>\n" +
                                            "</tr>)";
            Pattern operationPattern = Pattern.compile(htmlOperationsRegex);
            Matcher pMatcher = operationPattern.matcher(everything);

            while (pMatcher.find()) {
                Date opDate = formatter.parse(pMatcher.group(2));
                String resultAmountStr = pMatcher.group(3).replaceAll("[^0-9,]", "");
                resultAmountStr = resultAmountStr.replaceAll(",", ".");
                double amount = Double.parseDouble(resultAmountStr);
                String desc = pMatcher.group(4).replaceAll("\\s+", " ");

                BankOperationsRecord currentOperation = this.config.getCTX().newRecord(Tables.BANK_OPERATIONS);
                String checkingAmount = pMatcher.group(3).replaceAll("\\s+", "");

                if (checkingAmount.startsWith("+")) {
                    currentOperation.setIsIncome(true);
                } else {
                    currentOperation.setIsIncome(false);
                }

                currentOperation.setSource(this.cardName);
                currentOperation.setAmount((float) amount);
                currentOperation.setOpDate(new java.sql.Date(opDate.getTime()));
                currentOperation.setDesc(desc);
                currentOperation.setCatName("");
                currentOperation.setExtCatId(-1L);
                currentOperation.store();
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            return new ArrayList<>() {};
        }
        return operations;
    }


    @Deprecated
    private List<BankOperationsRecord> extractOperationsFromPdf(String pdfFilePath) {
        try (PDDocument document = PDDocument.load(new File(pdfFilePath))) {
            if (!document.isEncrypted()) {
                // Pre compile patterns
                String startingLineRegex = "^([0-9]{2}\\.[0-9]{2}\\.[0-9]{4}) ([^\\/][^+,]+?)\\s*(перевод)?\\s([+\\-]?[0-9]{1,2}[0-9  ],[0-9][0-9])?\\s?([+\\-]?[0-9  ]+,[0-9][0-9])?$";
                Pattern startingPattern = Pattern.compile(startingLineRegex);
                String extDescRegex = "^([^\\+\\-/][а-яА-Яa-zA-Z\\@\\w]+)$";
                Pattern extPattern = Pattern.compile(extDescRegex);
                String categoryRegex = "^[0-9]{2}\\.[0-9]{2}\\.[0-9]{4} \\/ ([0-9\\-\\s]{1,7}) ([а-яА-Яa-zA-Z ]+)";
                Pattern categoryPattern = Pattern.compile(categoryRegex);
                String endingLineRegex = "^([\\+\\-]?\\s?[0-9 \\ ]+\\,[0-9][0-9])\\s?([\\+\\-]?[0-9 \\ ]+\\,[0-9][0-9])?";
                Pattern amountsPattern = Pattern.compile(endingLineRegex);
                String periodLineRegex = "([0-9]{2}\\.[0-9]{2}\\.[0-9]{4}) - ([0-9]{2}\\.[0-9]{2}\\.[0-9]{4})";
                Pattern periodPattern = Pattern.compile(periodLineRegex);
                String cardLineRegex = "Выписка по счету дебетовой карты (.*)";
                Pattern cardPattern = Pattern.compile(cardLineRegex);

                // Load PDF file
                String[] lines = loadPdfLines(document);
                java.sql.Date periodStart;
                java.sql.Date periodEnd;

                boolean isOperationOpened = false;
                BankOperationsRecord currentOperation = config.getCTX().newRecord(BANK_OPERATIONS);
                currentOperation.setSource(config.getProperty("card_source"));
                List<BankOperationsRecord> operations = new ArrayList<>();

                for (String line : lines) {
                    // Checking period of pdf doc operations.
                    // Operations in given source are replaced by new ones
                    Matcher periodMatcher = periodPattern.matcher(line);
                    if (periodMatcher.find()) {
                        String dateStartStr = periodMatcher.group(1);
                        String dateEndStr = periodMatcher.group(2);
                        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
                        try {
                            Date dateStart = formatter.parse(dateStartStr);
                            Date dateEnd = formatter.parse(dateEndStr);
                            periodStart = new java.sql.Date(dateStart.getTime());
                            periodEnd = new java.sql.Date(dateEnd.getTime());

                            // clear period in database only with this source
                            config.getCTX().deleteFrom(BANK_OPERATIONS)
                                           .where(BANK_OPERATIONS.OP_DATE.greaterOrEqual(periodStart)
                                           .and(BANK_OPERATIONS.OP_DATE.lessOrEqual(periodEnd))
                                           .and(BANK_OPERATIONS.SOURCE.eq(this.cardName)))
                                           .execute();
                            continue;
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }

                    Matcher startingMatcher = startingPattern.matcher(line);
                    if (startingMatcher.find()) {
                        if(isOperationOpened) {
                            operations.add(currentOperation);
                        }
                        isOperationOpened = true;
                        currentOperation = config.getCTX().newRecord(BANK_OPERATIONS);
                        currentOperation.setSource(cardName);
                        processDateInOp(currentOperation, startingMatcher);
                        currentOperation.setDesc(startingMatcher.group(2));

                        if (startingMatcher.groupCount() > 2) {
                            String amountStr = startingMatcher.group(4);
                            Matcher amountMatcher = amountsPattern.matcher(amountStr);
                            if (amountMatcher.find()) {
                                processAmountInOpLine(currentOperation, amountMatcher);
                            }
                        }
                        continue;
                    }

                    if(isOperationOpened) {
                        if (matchExtDescription(extPattern, currentOperation, line)) continue;
                        if (matchCategory(categoryPattern, currentOperation, line)) continue;
                        if(matchAmount(amountsPattern, currentOperation, operations, line)) {
                            isOperationOpened = false;
                        }
                    }
                }
                return operations;
            }
        } catch (IOException | CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Deprecated
    private String[] loadPdfLines(PDDocument document) throws IOException {
        PDFTextStripper tStripper = new PDFTextStripper();
        tStripper.setSortByPosition(true);
        String pdfFileInText = tStripper.getText(document);
        return pdfFileInText.split("\\r?\\n");
    }

    private void processDateInOp(BankOperationsRecord currentOperation, Matcher startingMatcher) {
        String dateStr = startingMatcher.group(1);
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
        try {
            Date date = formatter.parse(dateStr);
            currentOperation.setOpDate(new java.sql.Date(date.getTime()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void processAmountInOpLine(BankOperationsRecord currentOperation, Matcher amountMatcher) {
        String amountString = amountMatcher.group(1);
        String resultAmountStr;
        if (amountString.startsWith("+")) {
            currentOperation.setIsIncome(true);
            resultAmountStr = amountString.substring(1);
        }
        else {
            currentOperation.setIsIncome(false);
            resultAmountStr = amountString;
        }
        resultAmountStr = resultAmountStr.replaceAll("[^0-9,]", "");
        resultAmountStr = resultAmountStr.replaceAll(",",".");
        double amountCasted = Double.parseDouble(resultAmountStr);
        currentOperation.setAmount((float)amountCasted);
    }

    private boolean matchAmount(Pattern amountsPattern, BankOperationsRecord currentOperation, List<BankOperationsRecord> operations, String line) throws CloneNotSupportedException {
        Matcher amountMatcher = amountsPattern.matcher(line);
        boolean result = false;
        if (amountMatcher.find()) {
            String amountString = amountMatcher.group(1);
            String resultAmountStr;
            if (amountString.startsWith("+")) {
                currentOperation.setIsIncome(true);
                resultAmountStr = amountString.substring(1);
            }
            else {
                currentOperation.setIsIncome(false);
                resultAmountStr = amountString;
            }
            resultAmountStr = resultAmountStr.replaceAll("[^0-9,]", "");
            resultAmountStr = resultAmountStr.replaceAll(",",".");
            double amountCasted = Double.parseDouble(resultAmountStr);
            currentOperation.setAmount((float)amountCasted);
            result = true;
            operations.add(currentOperation);
        }
        return result;
    }

    private boolean matchCategory(Pattern categoryPattern, BankOperationsRecord currentOperation, String line) {
        Matcher catMatcher = categoryPattern.matcher(line);
        if (catMatcher.find()) {
            currentOperation.setCatName(catMatcher.group(2));
            long cat_id = 0;
            if(!catMatcher.group(1).equals("-")) {
                cat_id = Long.parseLong(catMatcher.group(1));
            }
            currentOperation.setExtCatId(cat_id);
            return true;
        }
        return false;
    }

    private boolean matchExtDescription(Pattern extPattern, BankOperationsRecord currentOperation, String line) {
        Matcher extMatcher = extPattern.matcher(line);
        if (extMatcher.find()) {
            currentOperation.setDesc(currentOperation.getDesc().concat(" " + extMatcher.group(1)));
            return true;
        }
        return false;
    }


    @Override
    public void extract() {
        // берем папку из конфигурации
        File directory = new File(config.getProperty("path"));
        // get just files, not directories
        File[] files = directory.listFiles((FileFilter) FileFileFilter.FILE);

        // нечего делать, файлов нет в папке
        if (files == null)
            return;

        // сортируем по дате изменения чтобы последние файлы были последними и обработаны.
        // это надо для того чтобы не переписать актуальные данные старыми
        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);

        for (File file : files) {
            if (file.getName().endsWith("html")) {
                List<BankOperationsRecord> allOperations = extractOperationsFromHtml(file.toString());
                    for (BankOperationsRecord op : allOperations) {
                        op.store();
                    }
            }
        }

    }

    @Override
    public void init(ExtractorConfiguration config) {
        if (config != null) {
            this.config = config;
            this.cardName = config.getProperty("card_source");
        }
        else {
            throw new IllegalArgumentException("Config cannot be null!");
        }
    }
}