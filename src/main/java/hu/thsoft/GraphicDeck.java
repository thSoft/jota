/*
 * TGC
 *
 * Class GraphicDeck
 *
 * (c) thSoft
 */

package hu.thsoft;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

/**
 * A deck of cards that can display itself graphically.
 *
 * @author thSoft
 */
public class GraphicDeck extends Deck {
  
  private int x;
  private int y;
  private int offsetX;
  private int offsetY;
  private ImageObserver observer;
  
  /**
   * Initializes a graphic deck's attributes.
   * @param x The horizontal position of the first card's top left corner.
   * @param y The vertical position of the first card's top left corner.
   * @param offsetX The horizontal offset the cards.
   * @param offsetY The vertical offset the cards.
   * @param observer The object to be notified as more of the image is
   * converted.
   */
  public GraphicDeck(int x, int y, int offsetX, int offsetY, 
    ImageObserver observer) {
    this.setX(x);
    this.setY(y);
    this.setOffsetX(offsetX);
    this.setOffsetY(offsetY);
    this.observer = observer;
  }
  
  /**
   * Draws all the cards in the deck to the specified graphics context,
   * each card with its own <code>image</code>. The positioning is linear:
   * the first card is drawn at <code>(x, y)</code>, the second at
   * <code>(x+offsetX, y+offsetY)</code>, the third at 
   * <code>(x+offsetX*2, y+offsetY*2)</code> and so on.
   * The first added card will be at the bottom.
   * @param g The graphics context to draw the deck to.
   */
  public void draw(Graphics g) {
    Iterator i = cards.iterator();
    int px = getX();
    int py = getY();
    while (i.hasNext()) {
      g.drawImage(((Card)i.next()).getImage(), px, py, observer);
      px += getOffsetX();
      py += getOffsetY();
    }
  }

  /**
   * Draws all the cards in this deck to the specified graphics context,
   * as if the deck was turned upside down, that is, drawing each card's
   * <code>backImage</code>, with the first added card at the top.
   * @param g The graphics context to draw the deck to.
   */
  public void drawTurned(Graphics g) {
    ListIterator i = cards.listIterator(cards.size());
    int px = getX()+getOffsetX()*(cards.size()-1);
    int py = getY()+getOffsetY()*(cards.size()-1);
    while (i.hasPrevious()) {
      g.drawImage(((Card)i.previous()).getBackImage(), px, py, observer);
      px -= getOffsetX();
      py -= getOffsetY();
    }
  }
  
  /**
   * Returns the topmost card to be found at the given coordinates, or
   * <code>null</code> if no such card was found.
   */
  public Card getCardAt(int cx, int cy) {
    if (cards.isEmpty()) {
      return null;
    }    
    ListIterator i = cards.listIterator(cards.size());
    int px = getX()+getOffsetX()*(cards.size()-1);
    int py = getY()+getOffsetY()*(cards.size()-1);
    Card c;
    while (i.hasPrevious()) {
      c = (Card)i.previous();
      if (c.getImage() != null) {
        if ((cx >= px) && (cx <= px+c.getImage().getWidth(observer)) &&
          (cy >= py) && (cy <= py+c.getImage().getHeight(observer))) {
          return c;
        }
      }
      px -= getOffsetX();
      py -= getOffsetY();
    }
    return null;
  }

  public int getX() {
    return x;
  }

  public void setX(int x) {
    this.x = x;
  }

  public int getY() {
    return y;
  }

  public void setY(int y) {
    this.y = y;
  }

  public int getOffsetX() {
    return offsetX;
  }

  public void setOffsetX(int offsetX) {
    this.offsetX = offsetX;
  }

  public int getOffsetY() {
    return offsetY;
  }

  public void setOffsetY(int offsetY) {
    this.offsetY = offsetY;
  }
  
}
