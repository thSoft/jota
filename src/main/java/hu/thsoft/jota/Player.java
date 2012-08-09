/*
 * JOTA
 *
 * Class Player
 *
 * (c) thSoft
 */

package hu.thsoft.jota;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import hu.thsoft.*;

/**
 * All datas and functions related to a virtual JOTA player.
 *
 * @author thSoft
 */
public class Player extends Thread {
  
  Game game;
  int score;
  
  boolean hasPassed;
  int discards;
  int discardedTarots;
  boolean announcedTarots;
  boolean announcedHelp;

  TarotDeck deck;
  TarotDeck laid;
  TarotDeck taken;
  TarotDeck talon;

  Socket socket;
  PrintWriter out;
  BufferedReader in;    
  
  public Player(Game game, String name) {
    this.game = game;
    this.setName(name);
  }

  public Player(Game game, Socket socket) {
    this.game = game;
    this.socket = socket;
    this.setName("");
  }
  
  public int getScore() {
    return score;
  }

  public void setScore(int score) {
    this.score = score;
  }
  
  public void send(String s) {
    if (out != null) {
      out.println(s);
    }
  }

  public void run() {
    char msg;
    String input;
    String[] tokens;
    boolean blink = true;
    try {
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      while ((input = in.readLine()) != null) {
        try {
          msg = input.charAt(0);
          input = input.substring(1);
          switch (msg) {
            case Game.MSG_LOGIN:
              if ((game.state != Game.STATE_ONLINE) || 
                (game.getMember(input) != null)) {
                game.entering.remove(this);
                send(Game.MSG_REFUSE+""+(game.state == Game.STATE_ONLINE?Game.BASE
                  :(char)(Game.BASE+1)));
                blink = false;              
              } else {
                StringBuffer s = new StringBuffer("");
                Iterator i = game.community.iterator();
                Player p;
                while (i.hasNext()) {
                  p = (Player)i.next();
                  if (!p.getName().equals(input)) {
                    s.append(p.getName()+Game.SUBSPLITTER+p.getScore()+
                      Game.SUBSPLITTER+(game.players.contains(p)?"p":" ")+
                      (game.mayor == p?"m":" ")+(game.self == p?"h":" ")+
                      Game.SPLITTER);
                  }
                }
                setName(input);
                game.window.chatMessage(input+" bejelentkezett.");
                game.entering.remove(this);
                game.addMember(this);
                send(Game.MSG_HI+s.toString());
                game.dispatch(Game.MSG_LOGIN+input, input);
              }
              break;
            case Game.MSG_LOGOUT:
              logout();
              break;
            case Game.MSG_MESSAGE:
              game.window.chatMessage(getName()+": "+input);
              game.dispatch(Game.MSG_MESSAGE+getName()+Game.SPLITTER+input, 
                getName());
              break;
            case Game.MSG_WANNAPLAY:
              if (game.players.size() >= Game.NUM_PLAYERS) {
                send(Game.MSG_CANTPLAY+"");
              } else {
                game.dispatch(Game.MSG_PLAY+getName(), null);
                game.players.add(this);
                game.window.playerTable.refresh();
                game.window.chatMessage(getName()+" résztvevõ lett.");
                if (game.players.size() == Game.NUM_PLAYERS) {
                  game.window.startGame.setEnabled(true);
                }
              }
              break;
            case Game.MSG_OBSERVE:
              game.dispatch(Game.MSG_OBSERVE+getName(), getName());
              game.players.remove(this);
              game.window.playerTable.refresh();
              game.window.startGame.setEnabled(false);
              game.window.chatMessage(getName()+" nézõ lett.");
              break;
            case Game.MSG_BID:
              game.dispatch(Game.MSG_BID+input+getName(), getName());            
              game.processBid(getName(), input.charAt(0)-Game.BASE);
              break;
            case Game.MSG_DISCARD:
              game.dispatch(Game.MSG_DISCARD+input+getName(), getName());            
              game.processDiscard(getName(), input.charAt(0)-Game.BASE);
              break;
            case Game.MSG_ANNULL:
              game.dispatch(Game.MSG_ANNULL+getName(), getName());
              game.window.gameMessage(getName()+" bedobta a lapjait, új osztás.");
              game.newParty();
              break;            
            case Game.MSG_CONTINUE:
              game.dispatch(Game.MSG_CONTINUE+getName(), getName());
              game.nextDiscard(this);
              break;
            case Game.MSG_ANNOUNCE:
              game.dispatch(Game.MSG_ANNOUNCE+input+getName(), getName());
              game.processAnnounce(getName(), input);
              break;
            case Game.MSG_LAY:
              game.dispatch(Game.MSG_LAY+input+getName(), getName());
              game.processLay(getName(), input.charAt(0)-Game.BASE);
              break;
            case Game.MSG_TAKE:
              game.dispatch(Game.MSG_TAKE+getName(), getName());
              game.processTake(getName());
              break;
          }
          if (blink) {
            if ((!game.window.isActive()) &&
              (game.window.getTitle().charAt(0) != '*')) {
              game.window.setTitle("*"+game.window.getTitle());
            }
          } else {
            blink = true;
          }
        } catch (Exception e) {
          System.out.println(e.getMessage());          
        }
      }
    } catch (IOException e) {
      logout();
    }
  }

  private void logout() {
    game.dispatch(Game.MSG_LOGOUT+getName(), getName());    
    game.removeMember(this);    
  }
  
}
