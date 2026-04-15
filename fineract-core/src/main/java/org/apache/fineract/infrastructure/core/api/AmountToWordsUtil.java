package org.apache.fineract.infrastructure.core.api;



import java.math.BigDecimal;

public class AmountToWordsUtil {

    private static final String[] units = {
            "", "One", "Two", "Three", "Four", "Five", "Six",
            "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve",
            "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] tens = {
            "", "", "Twenty", "Thirty", "Forty",
            "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public static String convert(BigDecimal amount) {
        if (amount == null) return "";

        long number = amount.longValue();

        if (number == 0) return "Zero Shillings Only";

        return convertToWords(number).trim() + " Shillings Only";
    }

    private static String convertToWords(long number) {
        if (number < 20) return units[(int) number];

        if (number < 100)
            return tens[(int) number / 10] + " " + units[(int) number % 10];

        if (number < 1000)
            return units[(int) number / 100] + " Hundred " + convertToWords(number % 100);

        if (number < 1_000_000)
            return convertToWords(number / 1000) + " Thousand " + convertToWords(number % 1000);

        if (number < 1_000_000_000)
            return convertToWords(number / 1_000_000) + " Million " + convertToWords(number % 1_000_000);

        return convertToWords(number / 1_000_000_000) + " Billion " + convertToWords(number % 1_000_000_000);
    }
}


