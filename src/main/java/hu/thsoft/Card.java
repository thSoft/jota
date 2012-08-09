/*
 * TGC
 *
 * Class Card
 *
 * (c) thSoft
 */

package hu.thsoft;

import java.awt.*;

/**
 * Virtual representation of a playing card.
 *
 * @author thSoft
 */
public class Card {
  
  protected int color;
  protected int rank;
  protected int value;
  protected Image image;
  protected Image backImage;
  
  /**
   * Initializes a card's attributes which are read-only.
   * @param color You may want to use constants for the color.
   * @param rank May be interpreted as the strength of the card.
   * @param value Might be used at scoring.
   * @param image The card's image to display when needed.
   * @param backImage The image of the card's back.
   */
  public Card(int color, int rank, int value, Image image, Image backImage) {
    this.color = color;
    this.rank = rank;
    this.value = value;
    this.image = image;
    this.backImage = backImage;
  }

  public int getColor() {
    return color;
  }

  public int getRank() {
    return rank;
  }

  public int getValue() {
    return value;
  }

  public Image getImage() {
    return image;
  }
 
  public Image getBackImage() {
    return backImage;
  }
}
