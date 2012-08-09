/*
 * TGC
 *
 * Class Deck
 *
 * (c) thSoft
 */

package hu.thsoft;

import java.util.*;

/**
 * A deck of cards that can be dinamically manipulated.
 *
 * @author thSoft
 */
public class Deck {
  
  protected Vector cards = new Vector();
  
  /**
   * Adds a card to this deck.
   * @param card The card to be added. If it is <code>null</code>, a
   * <code>NullPointerException</code> is thrown.
   */
  public void add(Card card) throws NullPointerException {
    if (card == null) {
      throw (new NullPointerException());
    } else {
      cards.addElement(card);      
    }
  }
  
  /**
   * Removes a card from this deck.
   * <br>Avoid removing cards from a deck in order to keep card consistency.
   * Use the card moving operations instead.
   * @param card The card to be removed. The deck will remain unchanged
   * if the card is not in this deck.
   */
  public void remove(Card card) {
    cards.removeElement(card);
  }
  
  /**
   * Removes all cards from this deck.
   */
  public void removeAll() {
    cards.clear();
  }
  
  /**
   * Moves a card from this deck to another deck.
   * @param card The card to be moved.
   * @param dest The deck to move the specified card to.
   */
  public void moveCardTo(Card card, Deck dest) {
    cards.removeElement(card);
    dest.add(card);
  }
  
  /**
   * Moves a specified number of cards from the beginning of this deck
   * to another deck.
   * @param num The amount of cards to move. If it is greater than the
   * size of the deck, the whole deck is moved.
   * @param dest The deck to move the cards to.
   */
  public void moveCardsTo(int num, Deck dest) {
    Iterator i = cards.iterator();
    int n = 0;
    while ((i.hasNext()) && (n < num)) {
      dest.add((Card)i.next());
      i.remove();
      n++;
    }
  }
  
  /**
   * Moves all of the cards in this deck to another deck.
   * @param dest The deck to move this deck to.
   */
  public void moveDeckTo(Deck dest) {
    moveCardsTo(cards.size(), dest);
  }
  
  /**
   * Moves all cards from another deck to this deck.
   * @param src The deck to move the cards from.
   */
  public void addDeck(Deck src) {
    cards.addAll(src.cards);
    src.removeAll();
  }
 
  /**
   * Returns the card that was added first to this deck.
   */
  public Card getFirst() {
    return (Card)cards.firstElement();
  }
  
  /**
   * Returns the card that was added last to this deck.
   */
  public Card getLast() {
    return (Card)cards.lastElement();
  }

  /**
   * Returns the number of cards in this deck.
   */
  public int size() {
    return cards.size();
  }
  
  /**
   * Shuffles this deck using the built-in pseudorandom generator.
   */
  public void shuffle() {
    Collections.shuffle(cards);
  }
  
  /**
   * Returns the sum of the values of the cards in this deck.
   */
  public int getValue() {
    Iterator i = cards.iterator();
    int v = 0;
    while (i.hasNext()) {
      v += ((Card)i.next()).getValue();
    }
    return v;
  }

  /**
   * Sorts the cards in this deck according to their ranks in ascendant order,
   * grouping them by colors.
   */
  public void sortByRank() {
    final int groupSize = cards.size();
    Collections.sort(cards, new Comparator() {
      public int compare(Object o1, Object o2) {
        return ((((Card)o1).getRank()+groupSize*((Card)o1).getColor())-
          (((Card)o2).getRank()+groupSize*((Card)o2).getColor()));
      }
    });
  }
  
  /**
   * Sorts the cards in this deck according to their values in ascendant order,
   * grouping them by colors.
   */
  public void sortByValue() {
    final int groupSize = cards.size();
    Collections.sort(cards, new Comparator() {
      public int compare(Object o1, Object o2) {
        return ((((Card)o1).getValue()+groupSize*((Card)o1).getColor())-
          (((Card)o2).getValue()+groupSize*((Card)o2).getColor()));
      }
    });
  }
  
  /**
   * Returns the first card in the deck with the specified color, rank and
   * value. Returns <code>null</code> if there is no such card in this deck.
   */
  public Card findCard(int color, int rank, int value) {
    Iterator i = cards.iterator();
    Card c;
    while (i.hasNext()) {
      c = (Card)i.next();
      if ((c.getColor() == color) && (c.getRank() == rank) &&
        (c.getValue() == value)) {
        return c;
      }
    }
    return null;
  }
  
  /**
   * Returns the card at the specified index.
   */
  public Card getByIndex(int index) {
    return (Card)cards.get(index);
  }
  
  /**
   * Returns a card's index in the deck. If the card isn't in the deck,
   * the result is -1.
   */
  public int getIndexOf(Card c) {
    return cards.indexOf(c);
  }
}
