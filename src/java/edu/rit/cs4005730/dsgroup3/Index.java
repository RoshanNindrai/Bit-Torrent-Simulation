package edu.rit.cs4005730.dsgroup3;

public class Index {
   private int index = -1;
   
   public Index() {
      
   }
   
   public void setIndex(final int index) {
      this.index = index;
   }
   
   public int getIndex() {
      return this.index;
   }
   
   @Override
   public String toString() {
      return Integer.toBinaryString(this.index);
   }
}
