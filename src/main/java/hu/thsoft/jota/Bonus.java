/*
 * JOTA
 *
 * Class Bonus
 *
 * (c) thSoft
 */

package hu.thsoft.jota;

import java.awt.*;
import javax.swing.*;

/**
 * Datas of a bonus during the game.
 *
 * @author thSoft
 */
public class Bonus {
  
  private int base;
  private int level;
  private String name;
  private boolean opponents;
  private int won;
  private JCheckBox checkBox;
  
  public Bonus(String name, int base) {
    this.name = name;
    this.base = base;
    checkBox = new JCheckBox();
  }

  public int getBase() {
    return base;
  }

  public void setBase(int base) {
    this.base = base;
  }
  
  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
    checkBox.setText(Game.levelNames[level]+name);
  }
  
  public void increaseLevel() {
    level++;
  }

  public String getName() {
    return name;
  }
  
  public boolean isOpponents() {
    return opponents;
  }

  public void setOpponents(boolean opponents) {
    this.opponents = opponents;
  }

  public JCheckBox getCheckBox() {
    return checkBox;
  }

  public int getWon() {
    return won;
  }

  public void setWon(int won) {
    this.won = won;
  }

}
