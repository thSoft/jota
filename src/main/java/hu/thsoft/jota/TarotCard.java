/*
 * JOTA
 *
 * Class TarotCard
 *
 * (c) thSoft
 */

package hu.thsoft.jota;

import java.awt.*;
import hu.thsoft.*;

/**
 * A playing card that implements Tarot-specific behavior.
 *
 * @author thSoft
 */
public class TarotCard extends Card {
  
  public static final int KING = 5;
  public static final int TAROT = 5;
  
  private static final String colorNames[] =
    {"pikk ", "treff ", "k�r ", "k�r� ", ""};
  private static final String rankNames[] = {"t�zes", "�sz",
    "bubi", "lovas", "d�ma", "kir�ly", "pag�t", "kettes", "h�rmas", "n�gyes",
    "�t�s", "hatos", "hetes", "nyolcas", "kilences", "t�zes", "tizenegyes",
    "tizenkettes", "tizenh�rmas", "tizenn�gyes", "tizen�t�s", "tizenhatos",
    "tizenhetes", "tizennyolcas", "tizenkilences", "h�szas", 
    "huszonegyes", "sk�z"};
    
  public TarotCard(int color, int rank, int value, Image image, Image backImage)
    throws IllegalArgumentException {
    super(color, rank, value, image, backImage);
    if ((color < 1) || (color > TAROT) || (rank < 1) || (rank > 27) ||
      (value < 1) || (value > 5)) {
      throw new IllegalArgumentException();
    }    
  }
  
  public boolean isHonour() {
    return ((color == TAROT) && ((rank == 6) || (rank >= 26)));
  }
  
  public String toString() {
    return new String(colorNames[color-1]+
      rankNames[(color < 3) && (rank == 1)?0:rank]);
  }
  
}
