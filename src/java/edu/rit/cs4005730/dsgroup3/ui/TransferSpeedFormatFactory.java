package edu.rit.cs4005730.dsgroup3.ui;

import java.text.ParseException;

import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;
import javax.swing.text.NumberFormatter;

public class TransferSpeedFormatFactory extends AbstractFormatterFactory {
   
   @Override
   public AbstractFormatter getFormatter(final JFormattedTextField tf) {
      return new TransferSpeedFormatter();
   }
   
   private class TransferSpeedFormatter extends NumberFormatter {
      private final TransferSpeedFormat formatter = new TransferSpeedFormat();
      
      public TransferSpeedFormatter() {
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
