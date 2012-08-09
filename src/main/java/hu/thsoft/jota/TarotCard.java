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
    {"pikk ", "treff ", "kõr ", "káró ", ""};
  private static final String rankNames[] = {"tízes", "ász",
    "bubi", "lovas", "dáma", "király", "pagát", "kettes", "hármas", "négyes",
    "ötös", "hatos", "hetes", "nyolcas", "kilences", "tízes", "tizenegyes",
    "tizenkettes", "tizenhármas", "tizennégyes", "tizenötös", "tizenhatos",
    "tizenhetes", "tizennyolcas", "tizenkilences", "húszas", 
    "huszonegyes", "skíz"};
    
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
