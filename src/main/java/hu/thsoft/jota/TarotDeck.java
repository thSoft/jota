/*
 * JOTA
 *
 * Class TarotDeck
 *
 * (c) thSoft
 */

package hu.thsoft.jota;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import hu.thsoft.*;

/**
 * A deck of cards that implements Tarot-specific behavior.
 * It should only contain descendants of <code>TarotCard</code>.
 * 
 * @author thSoft
 */
public class TarotDeck extends GraphicDeck {
  
  public TarotDeck(int x, int y, int offsetX, int offsetY, 
    ImageObserver observer) {
    super(x, y, offsetX, offsetY, observer);
  }
  
  public boolean containsHonour() {
    Iterator i = cards.iterator();
    TarotCard c;
    while (i.hasNext()) {
      try {
        c = ((TarotCard)i.next());
      } catch (ClassCastException e) {
        return false;
      }
      if (c.isHonour()) {
        return true;
      }
    }
    return false;
  }
  
  public boolean containsColor(int color) {
    Iterator i = cards.iterator();
    TarotCard c;
    while (i.hasNext()) {
      try {
        c = ((TarotCard)i.next());
      } catch (ClassCastException e) {
        return false;
      }
      if (c.getColor() == color) {
        return true;
      }
    }
    return false;
  } 
  
  public int countHonours() {
    Iterator i = cards.iterator();
    TarotCard c;
    int n = 0;
    while (i.hasNext()) {
      try {
        c = ((TarotCard)i.next());
        if (c.isHonour()) {
          n++;        
        }
      } catch (ClassCastException e) {
      }
    }
    return n;    
  }
  
  public int countTarots() {
    Iterator i = cards.iterator();
    TarotCard c;
    int n = 0;
    while (i.hasNext()) {
      try {
        c = ((TarotCard)i.next());
        if (c.getColor() == TarotCard.TAROT) {
          n++;
        }        
      } catch (ClassCastException e) {
      }
    }
    return n;    
  }
  
  public int countKings() {
    Iterator i = cards.iterator();
    TarotCard c;
    int n = 0;
    while (i.hasNext()) {
      try {
        c = ((TarotCard)i.next());
        if (c.getRank() == TarotCard.KING) {
          n++;
        }        
      } catch (ClassCastException e) {
      }
    }
    return n;    
  }
  
}
