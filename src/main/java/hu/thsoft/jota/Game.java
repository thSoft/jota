/*
 * JOTA
 *
 * Class Game
 *
 * (c) thSoft
 */

package hu.thsoft.jota;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import hu.thsoft.*;

/**
 * The JOTA game engine.
 *
 * @author thSoft
 */
public class Game {
  
  class ClientThread extends Thread {
    public void run() {
      char msg;
      String input;
      String[] tokens;
      String[] subtokens;
      boolean blink = true;
      try {
        out.println(MSG_LOGIN+self.getName());
        while ((input = in.readLine()) != null) {
          try {
            msg = input.charAt(0);
            input = input.substring(1);
            switch (msg) {
              case MSG_REFUSE:
                switch (input.charAt(0)) {
                  case BASE:
                    window.showMessage(
                      "Már van ilyen nevû felhasználó a szobában!",
                      MainWindow.ICON_ERROR);
                    blink = false;
                    break;
                  case BASE+1:
                    window.showMessage(
                      "A szobában épp játék folyik!",
                      MainWindow.ICON_ERROR);
                    blink = false;                  
                    break;
                }
                break;
              case MSG_HI:
                room = window.roomName.getText();
                window.setTitle("Java Online Tarokk - Szoba: "+game.room+
                  " - Neved: "+game.self.getName());
                game.addMember(game.self);
                tokens = input.split(SPLITTER+"");
                for (int i = 0; i < tokens.length; i++) {
                  subtokens = tokens[i].split(SUBSPLITTER+"");
                  Player p = new Player(game, subtokens[0]);
                  p.setScore(Integer.parseInt(subtokens[1]));
                  if (subtokens[2].charAt(0) == 'p') {
                    players.add(p);
                  }
                  if (subtokens[2].charAt(1) == 'm') {
                    mayor = p;
                  }
                  if (subtokens[2].charAt(2) == 'h') {
                    host = p;
                  }
                  addMember(p);
                }
                window.chatScreen();
                window.showMessage("Beléptél a következõ szobába: "+game.room,
                  MainWindow.ICON_INFO);
                blink = false;
                break;
              case MSG_LOGIN:
                addMember(new Player(game, input));
                window.chatMessage(input+" bejelentkezett.");
                break;
              case MSG_LOGOUT:
                removeMember(getMember(input));
                break;
              case MSG_NOHOST:
                noHost();
                break;
              case MSG_MESSAGE:
                tokens = input.split(SPLITTER+"");
                window.chatMessage(tokens[0]+": "+tokens[1]);
                break;
              case MSG_CANTPLAY:
                window.playerCandidate.setSelected(false);
                window.playerCandidate.setEnabled(true);
                window.showMessage("Már van "+NUM_PLAYERS+
                  " résztvevõ a szobában!", MainWindow.ICON_ERROR);
                break;
              case MSG_PLAY:
                window.playerCandidate.setEnabled(true);
                players.add(getMember(input));
                window.playerTable.refresh();
                if (input.equals(self.getName())) {
                  window.chatMessage("Résztvevõ lettél.");
                  window.showMessage("Résztvevõ vagy.", MainWindow.ICON_INFO);
                } else {
                  window.chatMessage(input+" résztvevõ lett.");
                }
                break;
              case MSG_OBSERVE:
                window.chatMessage(input+" nézõ lett.");
                players.remove(getMember(input));
                window.playerTable.refresh();
                break;
              case MSG_HAND:
                if (state == STATE_ONLINE) {
                  newParty();
                  window.gameScreen();
                } else {
                  newParty();
                }
                for (int i = 0; i < 42; i++) {
                  mainDeck.add(new TarotCard(input.charAt(i*4)-BASE,
                    input.charAt(i*4+1)-BASE, input.charAt(i*4+2)-BASE,
                    cardImages[input.charAt(i*4+3)-BASE], backImage));
                }
                for (int i = 0; i < NUM_PLAYERS; i++) {
                  Deck d = ((Player)players.get(i)).deck;
                  mainDeck.moveCardsTo(9, d);
                  d.sortByRank();
                }
                mainDeck.moveCardsTo(6, talon);
                starter = getMember(input.substring(42*4));
                current = starter;
                window.showMessage(current.getName()+" licitál...",
                  MainWindow.ICON_WAIT);
                bid();
                break;
              case MSG_ENDGAME:
                if (input.equals("")) {
                    window.showMessage("A játéknak vége.", MainWindow.ICON_INFO);
                } else {
                  window.showMessage(input+
                    " nevû résztvevõ kilépett, ezért vége a játéknak.",
                    MainWindow.ICON_ERROR);                
                }
                window.chatMessage("A játék véget ért.");              
                window.chatScreen();
                break;
              case MSG_BID:
                processBid(input.substring(1), input.charAt(0)-BASE);
                break;
              case MSG_DISCARD:
                processDiscard(input.substring(1), input.charAt(0)-BASE);
                break;
              case MSG_ANNULL:
                window.gameMessage(input+" bedobta a lapjait, új osztás.");              
                break;
              case MSG_CONTINUE:
                nextDiscard(getMember(input));
                break;
              case MSG_ANNOUNCE:
                processAnnounce(input.substring(9), input.substring(0, 9));
                break;
              case MSG_LAY:
                processLay(input.substring(1), input.charAt(0)-BASE);
                break;
              case MSG_TAKE:
                processTake(input);
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
        noHost();
      }
    }
  }
  
  class ServerThread extends Thread {
    public void run() {
      while (true) {
        try {
          Player p = new Player(game, serverSocket.accept());
          if (p != null) {
            p.start();
            entering.add(p);
          }
        } catch (IOException e) {
        }
      }
    }
  }
  
  MainWindow window;
  Game game = this;
  Image cardImages[] = new Image[42];
  Image backImage;
  
  Vector community = new Vector();
  Vector entering = new Vector();
  Vector players = new Vector();  
  Vector announcers = new Vector();
  Vector opponents = new Vector();
  Player starter;  
  Player announcer;
  Player mayor;
  Player self;
  Player host;
  Player current;
  Player taker;
  
  Deck mainDeck = new Deck();
  Deck talon = new Deck();
  TarotDeck announcersDeck = new TarotDeck(5, 90, 18, 0, window);
  TarotDeck opponentsDeck = new TarotDeck(5, 470, 18, 0, window);
  
  static final int STATE_OFFLINE = 0;
  static final int STATE_ONLINE = 1;
  static final int STATE_BID = 2;
  static final int STATE_DISCARD = 3;
  static final int STATE_ANNOUNCE = 4;
  static final int STATE_PLAY = 5;
  static final int STATE_WAIT = 6;
  static final int STATE_SCORE = 7;
    
  int state = STATE_OFFLINE;
  int turns;
  int value;
  int level;
  int numPasses;
  int baseColor;
  int score;
  int catchPagatUlti;  
  boolean discardedTarot;
  boolean canHold;
  
  Bonus bonuses[] = {new Bonus("trull", 1), new Bonus("négykirály", 1),
    new Bonus("duplajáték", 2), new Bonus("volát", 3),
    new Bonus("pagátulti", 5), new Bonus("huszonegyfogás", 21)};
  static final int BONUS_TRULL = 0;
  static final int BONUS_FOURKINGS = 1;
  static final int BONUS_DOUBLEGAME = 2;
  static final int BONUS_VOLAT = 3;
  static final int BONUS_PAGATULTI = 4;
  static final int BONUS_CATCHXXI = 5;
  
  static final String levelNames[] = {"csendes ", "", "kontra-", "rekontra-",
    "szubkontra-", "hirskontra-", "mordkontra-"};
  static final String gameNames[] =
    {"", "hármas", "kettes", "egyes", "szóló"};
  static final String tarotNames[] = {"II-es", "III-as", "IV-es",
    "V-ös", "VI-os", "VII-es", "VIII-as", "IX-es", "X-es", "XI-es", "XII-es",
    "XIII-as", "XIV-es", "XV-ös", "XVI-os", "XVII-es", "XVIII-as", "XIX-es",
    "XX-as"};
  String results[][] = new String[8][2];
  
  String room;
  Socket socket;
  ServerSocket serverSocket;
  Thread communicator;
  PrintWriter out;
  BufferedReader in;

  static final Point deckPositions[] = {new Point(138, 526),
    new Point(526, 522), new Point(522, 134), new Point(134, 138)};
  static final Point laidPositions[] = {new Point(297, 363),
    new Point(363, 363), new Point(363, 297), new Point(297, 297)};
  static final Point takenPositions[] = {new Point(526, 526),
    new Point(526, 134), new Point(134, 134), new Point(134, 526)};
  static final Point talonPositions[] = {new Point(458, 412),
    new Point(412, 202), new Point(202, 248), new Point(248, 458)};
    
  static final int PORT = 40000;
  static final int NUM_PLAYERS = 4;
  static final char SPLITTER = 9;
  static final char SUBSPLITTER = 11;
  static final char BASE = 'A';
  
  static final char MSG_LOGIN = 'l';
  static final char MSG_REFUSE = 'r';
  static final char MSG_HI = 'h';
  static final char MSG_LOGOUT = 'x';
  static final char MSG_MESSAGE = 'm';
  static final char MSG_WANNAPLAY = 'w';
  static final char MSG_CANTPLAY = 'c';
  static final char MSG_PLAY = 'p';
  static final char MSG_OBSERVE = 'o';
  static final char MSG_HAND = 'n';
  static final char MSG_ENDGAME = 'e';
  static final char MSG_BID = 'b';
  static final char MSG_DISCARD = 'd';
  static final char MSG_ANNOUNCE = 'a';
  static final char MSG_LAY = 'y';
  static final char MSG_SCORE = 's';
  static final char MSG_NOHOST = 't';
  static final char MSG_ANNULL = 'u';
  static final char MSG_CONTINUE = 'i';
  static final char MSG_TAKE = 'k';
  
  static final int SIGN_PASS = 0;
  static final int SIGN_HOLD = 5;
  
  public Game(MainWindow window) {
    this.window = window;
    window.setGame(this);
  }

  public int getState() {
    return state;
  }

  public void setState(int state) {
    this.state = state;
  }
  
  public void newServerThread() {
    communicator = new ServerThread();
    communicator.start();
  }

  public void newClientThread() {
    communicator = new ClientThread();
    communicator.start();
  }
  
  public void newParty() {
    state = STATE_BID;
    Collections.sort(game.players, new Comparator() {
      public int compare(Object o1, Object o2) {
        return
          (((Player)o1).getName().compareToIgnoreCase(((Player)o2).getName()));
      }
    });
    window.endGame.setText("Játék befejezése");
    window.buttons.setOpaque(false);
    window.buttons.setBorder(null);
    window.buttons.removeAll();      
    window.gameMessage("--------------------------------");
    Player p;
    Bonus b;
    for (int i = 0; i < bonuses.length; i++) {
      bonuses[i].setLevel(0);
      bonuses[i].setWon(0);
      bonuses[i].setOpponents(false);
    }
    bonuses[BONUS_DOUBLEGAME].setBase(2);
    bonuses[BONUS_VOLAT].setBase(3);
    window.bonusTable.refresh(); 
    for (int i = 0; i < 8; i++) {
      results[i][0] = "";
      results[i][1] = "";
    }    
    int s = players.contains(self)?players.indexOf(self):0;
    for (int i = 0; i < NUM_PLAYERS; i++) {
      p = (Player)players.get((s+i)%NUM_PLAYERS);
      p.deck = new TarotDeck(deckPositions[i].x, deckPositions[i].y,
        40, 0, window);
      p.laid = new TarotDeck(laidPositions[i].x, laidPositions[i].y,
        0, 0, window);
      p.taken = new TarotDeck(takenPositions[i].x, takenPositions[i].y,
        1, 0, window);
      p.talon = new TarotDeck(talonPositions[i].x, talonPositions[i].y,
        -4, 0, window);
    }
    mainDeck.removeAll();
    talon.removeAll();
    announcersDeck.removeAll();
    opponentsDeck.removeAll();    
    announcers.clear();
    opponents.clear();
    announcer = null;
    value = 0;
    level = 1;
    turns = 0;
    numPasses = 0;
    catchPagatUlti = 0;
    discardedTarot = false;
    canHold = false;
    for (int i = 0; i < players.size(); i++) {
      p = (Player)players.get(i);
      p.hasPassed = false;
      p.discards = 0;
      p.discardedTarots = 0;
      p.announcedTarots = false;
      p.announcedHelp = false;
    }
    if (serverSocket != null) {
      for (int i = 0; i < 22; i++) {
        mainDeck.add(new TarotCard(5, 6+i, ((i == 0) || (i >= 20))?5:1,
          cardImages[i], backImage));
      }
      for (int i = 0; i < 20; i++) {
        mainDeck.add(new TarotCard(i/5+1, i%5+1, i%5+1, cardImages[22+i],
          backImage));
      }
      mainDeck.shuffle();
      StringBuffer str = new StringBuffer(MSG_HAND+"");      
      for (int i = 0; i < 42; i++) {
        Card c = mainDeck.getByIndex(i);
        str.append((char)(BASE+c.getColor()));
        str.append((char)(BASE+c.getRank()));
        str.append((char)(BASE+c.getValue()));
        str.append((char)(BASE+
          (c.getColor() == 5?c.getRank()-6:(c.getColor()-1)*5+c.getRank()+21)));
      }
      if (starter == null) {
        starter = (Player)players.get(new Random().nextInt(NUM_PLAYERS));
      } else {
        starter = nextPlayer(starter);
      }      
      str.append(starter.getName());
      dispatch(str.toString(), null);
      for (int i = 0; i < NUM_PLAYERS; i++) {
        p = (Player)players.get(i);
        mainDeck.moveCardsTo(9, p.deck);
        p.deck.sortByRank();
      }
      mainDeck.moveCardsTo(6, talon);
      current = starter;
      announcer = null;    
      bid();
    }
  }
  
  public Player getMember(String name) {
    Iterator i = community.iterator();
    Player p;
    while (i.hasNext()) {
      p = (Player)i.next();
      if (p.getName().equals(name)) {
        return p;
      }
    }
    return null;
  }
  
  public void send(String s) {
    if (out != null) {
      out.println(s);
    }
  }
  
  public void dispatch(String s, String except) {
    Iterator i = community.iterator();
    Player p;
    while (i.hasNext()) {
      p = (Player)i.next();
      if ((except == null) || (!p.getName().equals(except))) {
        p.send(s);
      }
    }
  }
  
  public void addMember(Player p) {
    if (p != null) {
      int i;
      for (i = 0; i < community.size(); i++) {
        if (((Player)community.get(i)).getName().
          compareToIgnoreCase(p.getName()) > 0) {
          break;
        }
      }
      community.insertElementAt(p, i);
      window.playerTable.refresh();      
    }
  }

  public void removeMember(Player p) {
    if (p == null) {
      return;
    }
    if (p.getName().length() > 0) {
      window.chatMessage(p.getName()+" kijelentkezett.");
    }
    if (players.contains(p)) {
      players.remove(p);
      if (players.size() < NUM_PLAYERS) {
        window.startGame.setEnabled(false);
        window.repaint();
      }
      if (state > STATE_ONLINE) {
        if (serverSocket != null) {
          dispatch(MSG_ENDGAME+p.getName(), p.getName()); 
          window.chatMessage("A játék véget ért.");
        }
        window.showMessage(p.getName()+
          " nevû résztvevõ kilépett, ezért vége a játéknak.",
          MainWindow.ICON_ERROR);                        
        window.chatScreen();
      }
    } else {
    }
    community.remove(p);
    window.playerTable.refresh();
  }
   
  public void noHost() {
    if (host != null) {
      removeMember(host);
      host = null;      
      window.showMessage("A szobafõnök kilépett és bezárta a szobát.",
        MainWindow.ICON_ERROR);
      window.chatInput.setEnabled(false);
      window.playerCandidate.setEnabled(false);
      window.cardTable.removeMouseListener(window.cardTableListener);    
    }
  }
  
  public Player nextPlayer(Player p) {
    if ((p != null) && (players.contains(p))) {
      return (Player)players.get((players.indexOf(p)+1)%NUM_PLAYERS);
    } else {
      return null;
    }
  }

  public void bid() {
    if (state != STATE_BID) {
      return;
    }
    window.buttons.removeAll();
    if (current == self) {
      window.showMessage("Licitálás...", MainWindow.ICON_INFO);
      if ((self.deck.containsHonour() && (!self.hasPassed))) {
        if (canHold) {
          window.buttons.add(window.hold);
        }
        for (int i = value+1; i <= 4; i++) {
          window.buttons.add(window.bidButtons[i-1]);
        }
      }
      window.buttons.add(window.pass);
      window.buttons.revalidate();
      window.buttons.setVisible(true);        
    } else {
      window.showMessage(current.getName()+" licitál...", MainWindow.ICON_WAIT);
    }
    window.repaint();
  }
  
  public void processBid(String n, int b) {
    Player p = getMember(n);
    if (b == SIGN_PASS) {
      p.hasPassed = true;
    }
    if (b == SIGN_HOLD) {
      announcer = p;
    } else {
      if (b > value) {
        value = b;
        announcer = p;
      }
    }
    do {
      p = nextPlayer(p);          
    } while (p.hasPassed);
    current = p;
    switch (b) {
      case SIGN_PASS:
        numPasses++;
        window.gameMessage(n+": Passz.");
        break;
      case SIGN_HOLD:
        canHold = false;        
        numPasses = 0;
        window.gameMessage(n+": Tartom.");
        break;
      default:
        canHold = true;
        numPasses = 0;
        window.gameMessage(n+": "+Utils.firstUpcase(game.gameNames[value])+
          " játék.");
        break;
    }
    if (numPasses == NUM_PLAYERS) {
      window.gameMessage("Mind a "+NUM_PLAYERS+
        " résztvevõ passzolt, új osztás.");
      if (serverSocket != null) {
        newParty();
      }
      return;
    }
    if (((value == 4) && ((b == SIGN_HOLD) || (b == SIGN_PASS))) ||
      ((numPassed() == 3) && (announcer != null))) {
      window.gameMessage(announcer.getName()+" lett a felvevõ.");
      window.buttons.removeAll();
      window.buttons.repaint();
      window.bonusTable.refresh();
      state = STATE_DISCARD;
      announcers.add(announcer);
      talon.moveCardsTo(4-value, announcer.deck);
      announcer.deck.sortByRank();      
      announcer.discards = 4-value;
      int remained = talon.size();
      for (int i = players.indexOf(nextPlayer(announcer)); remained > 0;
        i = (i+1)%NUM_PLAYERS) {
        p = (Player)players.get(i);
        if (p != announcer) {
          talon.moveCardsTo(1, p.deck);
          p.deck.sortByRank();
          p.discards++;
          p.discardedTarots = 0;
          remained--;
        }
      }
      current = (announcer.discards == 0?nextPlayer(announcer):announcer);
      if (current == self) {
        window.showMessage("Skartolás (még "+self.discards+" lap)...",
          MainWindow.ICON_INFO);        
      } else {
        window.showMessage(current.getName()+" skartol...",
          MainWindow.ICON_WAIT);
      }
      window.repaint();
      return;
    }
    window.showMessage(current.getName()+" licitál...", MainWindow.ICON_WAIT);
    bid();
    return;
  }
  
  public void processDiscard(String n, int d) {
    Player p = getMember(n);
    TarotCard c = (TarotCard)p.deck.getByIndex(d);
    p.deck.moveCardTo(c, p.talon);
    p.discards--;
    if (c.getColor() == TarotCard.TAROT) {
      p.discardedTarots++;
    }
    if (p.discards == 0) {
      if (p.discardedTarots > 0) {
        window.gameMessage(p.getName()+": "+p.discardedTarots
          +" tarokk fekszik.");
        discardedTarot = true;
        nextDiscard(p);
      } else {
        if ((!p.deck.containsColor(TarotCard.TAROT)) ||
          ((p.deck.countTarots() == 1) && ((p.deck.getLast().getRank() == 6) ||
          (p.deck.getLast().getRank() == 26)))) {
          if (p == self) {
            window.showMessage("Lehetõséged van lapjaid bedobására",
              MainWindow.ICON_INFO);
            window.buttons.add(window.annull);
            window.buttons.add(window.notAnnull);
            window.buttons.revalidate();            
          }
        } else {
          nextDiscard(p);
        }
      }
    } else {
      if (p == self) {
        window.showMessage("Skartolás (még "+p.discards+" lap)...",
          MainWindow.ICON_INFO);        
      }
    }
    window.repaint();
  }
  
  public void nextDiscard(Player p) {
    current = nextPlayer(p);
    if (current == announcer) {
      state = STATE_ANNOUNCE;
      numPasses = 0;
      window.buttons.setBorder(BorderFactory.createLineBorder(Color.gray));
      window.buttons.setOpaque(true);
      if (current == self) {
        window.callList.removeAllItems();        
        if (discardedTarot) {
          for (int i = 18; i >= 0; i--) {
            window.callList.addItem(tarotNames[i]);
          }
          for (int i = 18; i >= 0; i--) {
            if (current.deck.findCard(TarotCard.TAROT, 7+i, 1) == null) {
              window.callList.setSelectedIndex(18-i);
              break;
            }
          }          
        } else {
          window.callList.addItem(tarotNames[18]);
          if (current.deck.findCard(TarotCard.TAROT, 25, 1) != null) {
            for (int i = 17; i >= 0; i--) {
              if (current.deck.findCard(TarotCard.TAROT, 7+i, 1) == null) {
                window.callList.addItem(tarotNames[i]);
                window.callList.setSelectedIndex(1);
                break;
              }
            }
          }
        }
        window.buttons.add(window.callPanel);
        window.buttons.revalidate();        
      }
      announce();
      return;
    }
    if (current == self) {
      window.showMessage("Skartolás (még "+current.discards+" lap)...",
        MainWindow.ICON_INFO);        
    } else {
      window.showMessage(current.getName()+" skartol...",
        MainWindow.ICON_WAIT);        
    }    
    window.repaint();
  }
  
  public void announce() {
    if (state != STATE_ANNOUNCE) {
      return;
    } 
    window.buttons.remove(window.announce);
    window.bonusPanel.removeAll();    
    if (current == self) {
      window.showMessage("Bemondások...", MainWindow.ICON_INFO);             
      window.manyTarots.setSelected(false);
      if ((current.deck.countTarots() >= 8) && (!current.announcedTarots)) {
        window.manyTarots.setText(
          (current.deck.countTarots() == 8?"Nyolc":"Kilenc")+" tarokk");
        window.bonusPanel.add(window.manyTarots);
      }
      window.gameAnnounce.setSelected(false);
      if (((announcers.contains(current)?1:0) != (level%2)) &&
        (level < 6)) {
        window.gameAnnounce.setText(Utils.firstUpcase(levelNames[level+1]+
          "játék"));
        window.bonusPanel.add(window.gameAnnounce);
      }
      Bonus b;
      for (int i = 0; i < bonuses.length; i++) {
        b = bonuses[i];
        b.getCheckBox().setSelected(false);        
        if ((b.getLevel() == 0) || ((b.getLevel() < 6) &&
          (b.isOpponents()^opponents.contains(current)))) {
          b.getCheckBox().setText(Utils.firstUpcase(levelNames[b.getLevel()+1]
            +b.getName()));
          window.bonusPanel.add(b.getCheckBox());          
        }
      }
      if (bonuses[BONUS_VOLAT].getLevel() > 0) {
        if (bonuses[BONUS_DOUBLEGAME].getLevel() == 0) {
          if ((bonuses[BONUS_VOLAT].getLevel()+
            (bonuses[BONUS_VOLAT].isOpponents()?0:1)+
            (announcers.contains(current)?1:0))%2 == 1) {
            window.bonusPanel.remove(bonuses[BONUS_DOUBLEGAME].getCheckBox());
          }
        }
      }
      window.buttons.add(window.bonusPanel);      
      window.buttons.add(window.announce);
      window.buttons.revalidate();      
      window.buttons.setVisible(true);      
    } else {
      window.buttons.setVisible(false);
      window.showMessage(current.getName()+" bemondásra készül...",
        MainWindow.ICON_WAIT);           
    }
    window.repaint();
  }
  
  public void processAnnounce(String n, String a) {
    Player p = getMember(n);
    StringBuffer s = new StringBuffer();
    if ((p != announcer) && (announcers.contains(p)) &&
      (!a.equals("A0AAAAAAA")) && (!p.announcedHelp)) {
      s.append("segítem a felvevõt, ");
      p.announcedHelp = true;
    }
    if (a.charAt(1) != '0') { 
      scorePlayer(p, a.charAt(1)-'7');
      p.announcedTarots = true;
      s.append((a.charAt(1) == '8'?"nyolc":"kilenc")+" tarokk, ");
    }
    if (a.charAt(0) > BASE) {
      s.append("meghívom a nagyerejû "+tarotNames[a.charAt(0)-BASE-7]+"t, ");
      for (int i = 0; i < players.size(); i++) {
        Player q = (Player)players.get(i);
        if (q != announcer) {
          if (q.deck.findCard(TarotCard.TAROT, a.charAt(0)-BASE, 1) != null) {
            announcers.add(q);
          } else {
            opponents.add(q);
          }
        }
      }
    }
    if (a.charAt(2) == BASE+1) {
      level++;
      s.append(levelNames[level]+"játék, ");
    }  
    Bonus b;
    for (int i = 0; i < bonuses.length; i++) {
      b = bonuses[i];
      if (a.charAt(3+i) == BASE+1) {
        b.increaseLevel();
        b.setOpponents(opponents.contains(p));
        s.append(levelNames[b.getLevel()]+b.getName()+", ");
      }
    }
    current = nextPlayer(current);
    window.bonusTable.refresh();
    s.append("mehet.");
    window.gameMessage(n+": "+Utils.firstUpcase(s.toString()));
    if (a.equals("A0AAAAAAA")) {
      numPasses++;
      if (numPasses == 3) {
        state = STATE_PLAY;
        window.buttons.setOpaque(false);
        window.buttons.setBorder(null);
        window.repaint();
        turns = 0;
        taker = announcer;
        current = taker;
        if (current == self) {
          window.showMessage("1. kör - Te jössz...",
            MainWindow.ICON_INFO);
        } else {
          window.showMessage("1. kör - "+current.getName()+" jön...",
            MainWindow.ICON_WAIT);        
        }        
        return;
      }
    } else {
      numPasses = 0;
    }
    announce();
  }

  public void processLay(String n, int l) {
    Player p = getMember(n);
    Card c = p.deck.getByIndex(l);
    if (p == taker) {
      game.baseColor = c.getColor();
    }
    p.deck.moveCardTo(c, p.laid);
    current = nextPlayer(p);
    if (current == taker) {
      state = STATE_WAIT;
      int maxRank = 0;
      int pl = 0;
      Card d;
      int r;
      for (int i = 0; i < NUM_PLAYERS; i++) {
        d = ((Player)players.get(i)).laid.getByIndex(0);
        if ((d.getRank() > maxRank) && ((d.getColor() == TarotCard.TAROT) ||
          (d.getColor() == baseColor))) {
          maxRank = d.getRank();
          taker = (Player)players.get(i);
        }
        if (d.getRank() == 6) {
          pl = (announcers.contains(players.get(i)))?1:-1;
        }
      }
      if ((turns == 8) && (pl != 0) && (maxRank > 6) &&
        (bonuses[BONUS_PAGATULTI].getLevel() == 0)) {
        catchPagatUlti = pl;
      }
      current = taker;
      if (taker == self) {
        window.showMessage((turns+1)+". kör - Vedd fel a lapokat!",
          MainWindow.ICON_INFO);
        window.buttons.removeAll();
        window.buttons.add(window.take);                
        window.buttons.revalidate();        
        window.buttons.setVisible(true);
      } else {
        window.showMessage((turns+1)+". kör - "+taker.getName()+
          " felveszi a lapokat...", MainWindow.ICON_WAIT);
      }
    } else {
      if (current == self) {
        window.showMessage((turns+1)+". kör - Te jössz...",
          MainWindow.ICON_INFO);
      } else {
        window.showMessage((turns+1)+". kör - "+current.getName()+" jön...",
          MainWindow.ICON_WAIT);        
      }    
    }
    window.repaint();    
  }

  public void processTake(String n) {
    Player p = getMember(n);
    boolean s = (taker.laid.getByIndex(0).getRank() == 27);
    if ((turns == 8) && (taker.laid.getByIndex(0).getRank() == 6)) {
      if (announcers.contains(taker)) {
        bonuses[BONUS_PAGATULTI].setWon(1);
      } else {
        if (opponents.contains(taker)) {
          bonuses[BONUS_PAGATULTI].setWon(-1);
        }
      }
    }
    Iterator i = players.iterator();
    while (i.hasNext()) {
      Player q = (Player)i.next();
      if (s && (q.laid.getByIndex(0).getRank() == 26)) {
        if (announcers.contains(taker) && opponents.contains(q)) {
          bonuses[BONUS_CATCHXXI].setWon(1);
          window.gameMessage("Elfogták "+q.getName()+" huszonegyesét!");
          mayor = q;
          window.playerTable.refresh();          
        } else {
          if (opponents.contains(taker) && announcers.contains(q)) {
            bonuses[BONUS_CATCHXXI].setWon(-1);
            window.gameMessage("Elfogták "+q.getName()+" huszonegyesét!");            
            mayor = q;
            window.playerTable.refresh();            
          }
        }
      }
      q.laid.moveDeckTo(p.taken);
    }
    turns++;
    if (turns == 9) {
      state = STATE_SCORE;
      current = null;
      window.showMessage("Vége a játszmának.", MainWindow.ICON_INFO);
      window.endGame.setText("Új osztás");
      window.endGame.setMnemonic('o');
      i = players.iterator();
      while (i.hasNext()) {
        Player q = (Player)i.next();
        q.taken.moveDeckTo((announcers.contains(q)?announcersDeck:
          opponentsDeck));
      }
      Bonus b = bonuses[BONUS_VOLAT];
      if (announcersDeck.size() == 36) {
        b.setWon(1);
      } else {
        if (opponentsDeck.size() == 36) {
          b.setWon(-1);
        } else {
          b.setWon(0);
        }
      }
      i = players.iterator();
      while (i.hasNext()) {
        Player q = (Player)i.next();
        q.talon.moveDeckTo((q == announcer?announcersDeck:opponentsDeck));
      }      
      announcersDeck.sortByRank();
      announcersDeck.setOffsetX(595/announcersDeck.size());
      opponentsDeck.sortByRank();
      opponentsDeck.setOffsetX(595/opponentsDeck.size());      
      score();
    } else {
      state = STATE_PLAY;
      if (current == self) {
        window.showMessage((turns+1)+". kör - Te jössz...",
          MainWindow.ICON_INFO);
      } else {
        window.showMessage((turns+1)+". kör - "+current.getName()+" jön...",
          MainWindow.ICON_WAIT);
      }
    }
    window.repaint();
  }  
  
  public void scorePlayer(Player p, int s) {
    Iterator i = players.iterator();
    while (i.hasNext()) {
      Player q = (Player)i.next();
      if (q == p) {
        q.score += (players.size()-1)*s;
      } else {
        q.score -= s;
      }
    }
  }
  
  public void scoreAnnouncers(long s) {
    score += s*opponents.size()/announcers.size();    
    Iterator i = players.iterator();
    while (i.hasNext()) {
      Player p = (Player)i.next();
      if (announcers.contains(p)) {
        p.score += s*opponents.size()/announcers.size();
      } else {
        p.score -= s;
      }
    }
  }  
  
  public int numPassed() {
    int n = 0;
    Iterator i = players.iterator();
    while (i.hasNext()) {
      Player p = (Player)i.next();
      if (p.hasPassed) {
        n++;
      }
    }
    return n;
  }

  public void scoreBonus(Bonus b, int r) {
    long s;
    int op;
    if (b.getWon() == 1) {
      if (b.getLevel() == 0) {
        s = b.getBase();
        scoreAnnouncers(s);
        results[r][0] = levelNames[b.getLevel()]+b.getName()+": "+s+" pont";        
        results[r][1] = "-";
      } else {
        if ((b.getLevel()+(b.isOpponents()?0:1))%2 == 0) {
          s = b.getBase()*Math.round(Math.pow(2, b.getLevel()));
          results[r][0] = levelNames[b.getLevel()]+b.getName()+": "+s+" pont";
          results[r][1] = "-";          
          scoreAnnouncers(s);
        } else {
          s = b.getBase()*Math.round(Math.pow(2, b.getLevel()));
          results[r][0] = levelNames[0]+b.getName()+": "+b.getBase()+" pont";          
          results[r][1] = "bukott "+levelNames[b.getLevel()]+b.getName()+": -"+
            s+" pont";
          scoreAnnouncers(s+b.getBase());
        }        
      }
    } else {
      if (b.getWon() == -1) {
        if (b.getLevel() == 0) {
          s = b.getBase();
          scoreAnnouncers(-s);
          results[r][1] = levelNames[b.getLevel()]+b.getName()+": "+s+" pont";          
          results[r][0] = "-";
        } else {
          if ((b.getLevel()+(b.isOpponents()?0:1))%2 == 1) {
            s = b.getBase()*Math.round(Math.pow(2, b.getLevel()));
            results[r][1] = levelNames[b.getLevel()]+b.getName()+": "+s+" pont";            
            results[r][0] = "-";
            scoreAnnouncers(-s);
          } else {
            s = b.getBase()*Math.round(Math.pow(2, b.getLevel()));
            results[r][1] = levelNames[0]+b.getName()+": "+b.getBase()+" pont";            
            results[r][0] = "bukott "+levelNames[b.getLevel()]+b.getName()+": -"
              +s+" pont";
            scoreAnnouncers(-(s+b.getBase()));
          }
        }
      } else {
        if (b.getLevel() > 0) {
          s = b.getBase()*Math.round(Math.pow(2, b.getLevel()));
          op = (b.getLevel()+(b.isOpponents()?1:0))%2;
          scoreAnnouncers(Math.round(Math.pow(-1, op))*s);
          results[r][1-op] = "bukott "+levelNames[b.getLevel()]+b.getName()+
            ": -"+s+" pont";
          results[r][op] = "-";
        }
      }
    }    
  }
  
  public void scoreGame(int r) {
    long s = value*Math.round(Math.pow(2, level-1));
    if (announcersDeck.getValue() > 47) {
      results[r][0] = levelNames[level]+gameNames[value]+" játék: "+s+" pont";
      results[r][1] = "-";
      scoreAnnouncers(s);
    } else {
      results[r][0] = "-";
      results[r][1] = levelNames[level]+gameNames[value]+" játék: "+s+" pont";
      scoreAnnouncers(-s);
    }
  }
  
  public void score() {
    score = 0;
    results[0][0] = announcersDeck.getValue()+" vitt pont";
    results[0][1] = opponentsDeck.getValue()+" vitt pont";
    Bonus b;
    int r = 1;
    if ((level > 1) || ((bonuses[BONUS_DOUBLEGAME].getLevel() == 0) &&
      (bonuses[BONUS_DOUBLEGAME].getWon() == 0) &&
      (bonuses[BONUS_VOLAT].getLevel() == 0) &&
      (bonuses[BONUS_VOLAT].getWon() == 0))) {
      scoreGame(r);
      r++;      
    }
    b = bonuses[BONUS_DOUBLEGAME];
    if (announcersDeck.getValue() > 70) {
      b.setWon(1);
    } else {
      if (opponentsDeck.getValue() > 70) {
        b.setWon(-1);
      } else {
        b.setWon(0);
      }
    }
    b.setBase(b.getBase()*value);
    if ((bonuses[BONUS_VOLAT].getLevel() == 0) &&
      (bonuses[BONUS_VOLAT].getWon() == 0)) {
      scoreBonus(b, r);
      if ((b.getWon() > 0) || (b.getLevel() > 0)) {
        r++;
      }
    }
    b = bonuses[BONUS_VOLAT];
    b.setBase(b.getBase()*value);    
    scoreBonus(b, r);
    if ((b.getWon() > 0) || (b.getLevel() > 0)) {
      r++;
    }
    b = bonuses[BONUS_TRULL];
    if (announcersDeck.countHonours() == 3) {
      b.setWon(1);
    } else {
      if (opponentsDeck.countHonours() == 3) {
        b.setWon(-1);
      } else {
        b.setWon(0);
      }
    }
    switch (b.getWon()) {
      case 1:
        if ((b.getLevel() > 0) || (announcersDeck.size() < 36)) {
          scoreBonus(b, r);        
          r++;          
        }
        break;
      case -1:
        if ((b.getLevel() > 0) || (opponentsDeck.size() < 36)) {
          scoreBonus(b, r);
          r++;          
        }
        break;
      case 0:
        scoreBonus(b, r);
        if (b.getLevel() > 0) {
          r++;
        }
        break;
    }
    b = bonuses[BONUS_FOURKINGS];
    if (announcersDeck.countKings() == 4) {
      b.setWon(1);
    } else {
      if (opponentsDeck.countKings() == 4) {
        b.setWon(-1);
      } else {
        b.setWon(0);
      }
    }
    switch (b.getWon()) {
      case 1:
        if ((b.getLevel() > 0) || (announcersDeck.size() < 36)) {
          scoreBonus(b, r);        
          r++;          
        }
        break;
      case -1:
        if ((b.getLevel() > 0) || (opponentsDeck.size() < 36)) {
          scoreBonus(b, r);
          r++;          
        }
        break;
      case 0:
        scoreBonus(b, r);
        if (b.getLevel() > 0) {
          r++;
        }        
        break;
    }
    b = bonuses[BONUS_PAGATULTI];
    scoreBonus(b, r);
    if ((b.getWon() > 0) || (b.getLevel() > 0)) {
      r++;
    } else {
      if (catchPagatUlti != 0) {
        scoreAnnouncers(-5*catchPagatUlti);
        results[r][(catchPagatUlti == 1)?0:1] = 
          "Csendben bukott pagátulti: -5 pont";
        results[r][(catchPagatUlti == 1)?1:0] = "-";
        r++;
      }
    }
    b = bonuses[BONUS_CATCHXXI];
    scoreBonus(b, r);
    if ((b.getWon() > 0) || (b.getLevel() > 0)) {
      r++;
    }    
    window.buttons.add(window.headerLabel);
    window.refreshResults();
    window.buttons.add(window.resultScroll);              
    if (score == 0) {
      window.bottomLabel.setText("Pénz az ablakban!");
    } else {
      StringBuffer str = new StringBuffer();
      str.append("A játékos"+((announcers.size() > 1)?"ok egyenként":"")+" "+
        Math.abs(score)+" pontot ");
      if (score < 0) {
        str.append("fizet"+((announcers.size() > 1)?"nek":"")+".");
      } else {
        str.append("kap"+((announcers.size() > 1)?"nak":"")+".");
      }
      window.bottomLabel.setText(str.toString());        
    }
    window.buttons.add(window.bottomLabel);
    window.buttons.revalidate();    
    window.buttons.setVisible(true);
  }
  
}
