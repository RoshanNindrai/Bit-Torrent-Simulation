package edu.rit.cs4005730.dsgroup3.ui;

import java.text.ParseException;

import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;
import javax.swing.text.NumberFormatter;

public class FileSizeFormatFactory extends AbstractFormatterFactory {
   
   @Override
   public AbstractFormatter getFormatter(final JFormattedTextField tf) {
      return new FileSizeFormatter();
   }
   
   private class FileSizeFormatter extends NumberFormatter {
      private final FileSizeFormat formatter = new FileSizeFormat();
      
      public FileSizeFormatter() {
         this.setValueClass(Long.class);
      }
      
      @Override
      public Object stringToValue(final String text) throws ParseException {
         return this.formatter.parse(text).longValue();
      }
      
      @Override
      public String valueToString(final Object value) throws ParseException {
         return this.formatter.format(value);
      }
      
   }
}
