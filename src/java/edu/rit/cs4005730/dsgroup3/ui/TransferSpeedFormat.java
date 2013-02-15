package edu.rit.cs4005730.dsgroup3.ui;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * Converts bytes per second into the maximum format between bytes/sec, kbytes/sec, mbytes/sec, gbytes/sec or from
 * String with either bytes/bits variants into a bytes/second double value. The supported extensions are BPS, KBPS,
 * MBPS, GBPS. If no unit or unrecognized unit, will assume bytes/sec.
 * 
 * @author Stephen Ranger
 * 
 */
public class TransferSpeedFormat extends NumberFormat {
   private static final long          serialVersionUID         = 1L;
   private static final char          DECIMAL_POINT            = '.';
   private static final char          COMMA                    = ',';
   private static final char          DASH                     = '-';
   private static final String        SPACE                    = " ";

   private static final double        BPS_TO_KBPS              = 1.0 / 1024.0;
   private static final double        BPS_TO_MBPS              = 1.0 / 1024.0 / 1024.0;
   private static final double        BPS_TO_GBPS              = 1.0 / 1024.0 / 1024.0 / 1024.0;

   private static final double        KBPS_IN_BPS              = 1024.0;
   private static final double        MBPS_IN_BPS              = 1024.0 * 1024.0;
   private static final double        GBPS_IN_BPS              = 1024.0 * 1024.0 * 1024.0;

   private static final String        BYTES_PER_SECOND_EXT     = "BPS";
   private static final String        KILOBYTES_PER_SECOND_EXT = "KBPS";
   private static final String        MEGABYTES_PER_SECOND_EXT = "MBPS";
   private static final String        GIGABYTES_PER_SECOND_EXT = "GBPS";

   private static final DecimalFormat formatter                = new DecimalFormat("#.###");

   @Override
   public StringBuffer format(final double number, final StringBuffer toAppendTo, final FieldPosition pos) {
      return this.format(Double.valueOf(number), toAppendTo, pos);
   }

   @Override
   public StringBuffer format(final long number, final StringBuffer toAppendTo, final FieldPosition pos) {
      return this.format(Long.valueOf(number), toAppendTo, pos);
   }

   public StringBuffer format(final Number number, final StringBuffer toAppendTo, final FieldPosition pos) {
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

         if (isNumeric && !(Character.isDigit(temp) || temp == DECIMAL_POINT || temp == COMMA || temp == DASH)) {
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
         return (number.length() == 0) ? 0 : convertToBPS(Double.parseDouble(number.toString()), unit.toString().trim().toUpperCase());
      } catch (final NumberFormatException e) {
         return 0;
      }
   }

   private static double convertToBPS(final double value, final String unit) {
      double returnValue = value;

      if (unit.equals(BYTES_PER_SECOND_EXT)) {
         returnValue = value;
      } else if (unit.equals(KILOBYTES_PER_SECOND_EXT)) {
         returnValue = value * KBPS_IN_BPS;
      } else if (unit.equals(MEGABYTES_PER_SECOND_EXT)) {
         returnValue = value * MBPS_IN_BPS;
      } else if (unit.equals(GIGABYTES_PER_SECOND_EXT)) {
         returnValue = value * GBPS_IN_BPS;
      }

      return returnValue;
   }

   private static Object[] getMaxFormat(final Number value) {
      double returnValue = value.doubleValue();
      String extension = null;

      if (returnValue < KBPS_IN_BPS) {
         // bps
         extension = BYTES_PER_SECOND_EXT;
      } else if (returnValue < MBPS_IN_BPS) {
         // kbps
         returnValue = returnValue * BPS_TO_KBPS;
         extension = KILOBYTES_PER_SECOND_EXT;
      } else if (returnValue < GBPS_IN_BPS) {
         // mbps
         returnValue = returnValue * BPS_TO_MBPS;
         extension = MEGABYTES_PER_SECOND_EXT;
      } else { // if(returnValue < TBPS_IN_BPS)
         // gbps
         returnValue = returnValue * BPS_TO_GBPS;
         extension = GIGABYTES_PER_SECOND_EXT;
      }

      return new Object[] { Double.valueOf(returnValue), extension };
   }
}
