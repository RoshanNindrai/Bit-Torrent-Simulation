package edu.rit.cs4005730.dsgroup3.ui;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

public class FileSizeFormat extends NumberFormat {
   private static final String        BYTE_EXT           = "B";
   private static final String        KILOBYTE_EXT       = "KB";
   private static final String        MEGABYTE_EXT       = "MB";
   private static final String        GIGABYTE_EXT       = "GB";
   private static final char          DECIMAL_POINT      = '.';
   private static final char          COMMA              = ',';
   private static final char          DASH               = '-';
   private static final String        SPACE              = " ";
   private static final double        KILOBYTES_TO_BYTES = 1024;
   private static final double        MEGABYTES_TO_BYTES = 1024 * 1024;
   private static final double        GIGABYTES_TO_BYTES = 1024 * 1024 * 1024;
   
   private static final DecimalFormat formatter          = new DecimalFormat("#.###");
   
   @Override
   public StringBuffer format(final double number, final StringBuffer toAppendTo, final FieldPosition pos) {
      return this.format(Double.valueOf(number), toAppendTo, pos);
   }
   
   @Override
   public StringBuffer format(final long number, final StringBuffer toAppendTo, final FieldPosition pos) {
      return this.format(Long.valueOf(number), toAppendTo, pos);
   }
   
   private StringBuffer format(final Number number, final StringBuffer toAppendTo, final FieldPosition pos) {
      final Object[] convertedValue = getMaxFormat(number);
      
      pos.setBeginIndex(0);
      toAppendTo.append(formatter.format(convertedValue[0]));
      toAppendTo.append(SPACE);
      toAppendTo.append(convertedValue[1]);
      pos.setEndIndex(toAppendTo.length());
      
      return toAppendTo;
   }
   
   @Override
   public Number parse(final String source, final ParsePosition parsePosition) {
      final StringBuilder number = new StringBuilder();
      final StringBuilder unit = new StringBuilder();
      boolean isNumeric = true;
      char temp;
      
      for (int i = 0; i < source.length(); i++) {
         temp = source.charAt(i);
         
         if (isNumeric && !(Character.isDigit(temp) || (temp == DECIMAL_POINT) || (temp == COMMA) || (temp == DASH))) {
            isNumeric = false;
         }
         
         if (isNumeric) {
            number.append(temp);
         } else {
            unit.append(temp);
         }
      }
      
      parsePosition.setIndex(source.length());
      
      try {
         return Long.valueOf((number.length() == 0) ? 0 : convertToBytes(Double.parseDouble(number.toString()), unit.toString().trim().toUpperCase()));
      } catch (final NumberFormatException e) {
         return Long.valueOf(0);
      }
   }
   
   private static long convertToBytes(final double value, final String unit) {
      long returnValue = (long) value;
      
      if (unit.equals(BYTE_EXT)) {
         returnValue = (long) value;
      } else if (unit.equals(KILOBYTE_EXT)) {
         returnValue = (long) (value * KILOBYTES_TO_BYTES);
      } else if (unit.equals(MEGABYTE_EXT)) {
         returnValue = (long) (value * MEGABYTES_TO_BYTES);
      } else if (unit.equals(GIGABYTE_EXT)) {
         returnValue = (long) (value * GIGABYTES_TO_BYTES);
      }
      
      return returnValue;
   }
   
   private static Object[] getMaxFormat(final Number value) {
      double returnValue = value.doubleValue();
      String extension = null;
      
      if (returnValue < KILOBYTES_TO_BYTES) {
         // bps
         extension = BYTE_EXT;
      } else if (returnValue < MEGABYTES_TO_BYTES) {
         // kbps
         returnValue = returnValue / KILOBYTES_TO_BYTES;
         extension = KILOBYTE_EXT;
      } else if (returnValue < GIGABYTES_TO_BYTES) {
         // mbps
         returnValue = returnValue / MEGABYTES_TO_BYTES;
         extension = MEGABYTE_EXT;
      } else { // if(returnValue < TBPS_IN_BPS)
         // gbps
         returnValue = returnValue / GIGABYTES_TO_BYTES;
         extension = GIGABYTE_EXT;
      }
      
      return new Object[] { Double.valueOf(returnValue), extension };
   }
}
