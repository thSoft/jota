/*
 * JOTA
 *
 * Class MainWindow
 *
 * (c) thSoft
 */

package hu.thsoft.jota;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import hu.thsoft.*;

/**
 * JOTA's main window.
 *
 * @author thSoft
 */
public class MainWindow extends JFrame {
  
  class CloseListener extends WindowAdapter {    
    public void windowClosing(WindowEvent event) {
      if (game.serverSocket != null) {
        game.dispatch(Game.MSG_NOHOST+"", null);
      } else {
        game.send(Game.MSG_LOGOUT+"");
      }
      try {        
        game.communicator = null;
        if (game.socket != null) {
          game.socket.close();
        }
        if (game.out != null) {
          game.out.close();
        }
        if (game.in != null) {
          game.in.close(); 
        }
      } catch (IOException e) {
      }
      System.exit(0);
    }
    public void windowActivated(WindowEvent event) {
      if (getTitle().charAt(0) == '*') {
        setTitle(getTitle().substring(1));
      }
    }
  }
  
  class NextButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      InetAddress ip;      
      if (nick.getText().equals("")) {
        showMessage("Add meg a becenevedet!", ICON_ERROR);
        return;
      }
      if (nick.getText().length() > 32) {
        showMessage("A neved nem lehet hosszabb 32 betûnél!", ICON_ERROR);
        return;
      }
      if ((nick.getText().indexOf(Game.SPLITTER) != -1) ||
        (nick.getText().indexOf(Game.SUBSPLITTER) != -1)) {
        showMessage("A neved nem tartalmazhatja a "+(int)Game.SPLITTER+
          " vagy "+(int)Game.SUBSPLITTER+" kódú karaktert!", ICON_ERROR); 
      }
      game.self = new Player(game, nick.getText());
      if (newRoom.isSelected()) {
        try {
          ip = InetAddress.getLocalHost();
          game.serverSocket = new ServerSocket(Game.PORT);
          game.newServerThread();
          game.room = Utils.ip2id(ip);
          setTitle("Java Online Tarokk - Szoba: "+game.room+" - Neved: "+
            game.self.getName());
          Utils.setClipboardString(game.room);
          game.addMember(game.self);
          gameInfo.add(endGame);
          gameInfo.revalidate();
          chatScreen();
          chatMessage("Szoba kódja: "+game.room);
          showMessage("A következõ szobában vagy szobafõnök: "+game.room,
            ICON_INFO);                
        } catch (UnknownHostException e) {
          showMessage("Nem lehet létrehozni a szobát!", ICON_ERROR);
          return;
        } catch (IOException e) {
          showMessage("Nem sikerült létrehozni a szobát!", ICON_ERROR);                    
          return;
        }        
      } else {
        try {
          ip = Utils.id2ip(roomName.getText());
          if (ip == null) {
            showMessage("Nincs ilyen szoba!", ICON_ERROR);            
            return;
          }
          showMessage("Csatlakozás...", ICON_WAIT);
          game.socket = new Socket(ip, Game.PORT);
          game.out = new PrintWriter(game.socket.getOutputStream(), true);
          game.in = new BufferedReader(new InputStreamReader(
            game.socket.getInputStream()));
          game.newClientThread();
        } catch (UnknownHostException e) {
          showMessage("Nincs ilyen szoba!", ICON_ERROR);
          return;
        } catch (IOException e) {
          showMessage("Hiba történt a csatlakozás során!", ICON_ERROR);
          return;
        }
      }
    }    
  }

  class ChatMessageListener extends KeyAdapter {
    public void keyPressed(KeyEvent event) {
      if ((event.getKeyCode() == KeyEvent.VK_ENTER) &&
        (!chatInput.getText().equals(""))) {
        if (game.serverSocket != null) {
          game.dispatch(Game.MSG_MESSAGE+game.self.getName()+Game.SPLITTER+
            chatInput.getText(), null);
        } else {
          game.send(Game.MSG_MESSAGE+chatInput.getText());
        }
        chatMessage(game.self.getName()+": "+chatInput.getText());
        chatInput.setText("");
      }
    }    
  }
  
  class PlayerCandidateListener implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      if (playerCandidate.isSelected()) {
        if (game.players.size() >= Game.NUM_PLAYERS) {
          showMessage("Már van "+Game.NUM_PLAYERS+" résztvevõ!", ICON_ERROR);
          playerCandidate.setSelected(false);
        } else {
          if (game.serverSocket != null) {
            game.players.add(game.self);
            playerTable.refresh();
            if (game.players.size() == Game.NUM_PLAYERS) {
              game.window.startGame.setEnabled(true);
            }            
            game.dispatch(Game.MSG_PLAY+game.self.getName(), null);
            chatMessage("Résztvevõ lettél.");
            showMessage("Résztvevõ vagy.", ICON_INFO);
          } else {
            playerCandidate.setEnabled(false);
            showMessage("Jelölés résztvevõként...", ICON_WAIT);
            game.send(Game.MSG_WANNAPLAY+"");
          }
        }
      } else {
        chatMessage("Nézõ lettél.");
        showMessage("Nézõ vagy.", ICON_INFO);
        game.players.remove(game.self);
        playerTable.refresh();
        startGame.setEnabled(false);
        if (game.serverSocket != null) {
          game.dispatch(Game.MSG_OBSERVE+game.self.getName(), null);
        } else {
          game.send(Game.MSG_OBSERVE+"");
        }
      }
    }    
  }
  
  class StartGameListener implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      if (game.players.size() != Game.NUM_PLAYERS) {
        showMessage("A játék elkezdéséhez "+Game.NUM_PLAYERS+
          " résztvevõre van szükség!", ICON_ERROR);
        return;
      }
      gameScreen();
    }    
  }
  
  class EndGameListener implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      if (game.getState() == Game.STATE_SCORE) {
        if (game.serverSocket != null) {
          game.newParty();
        }
      } else {
        showMessage("A játéknak vége.", MainWindow.ICON_INFO);
        chatMessage("A játék véget ért.");
        game.dispatch(Game.MSG_ENDGAME+"", null);
        chatScreen();
      }
    }    
  }    

  class NextKeyListener extends KeyAdapter {    
    public void keyPressed(KeyEvent event) {
      if (event.getKeyCode() == KeyEvent.VK_ENTER) {
        nextButton.doClick();
      }
    }     
  }
  
  class CardTableListener extends MouseAdapter {
    public void mouseClicked(MouseEvent event) {
      if (game.current != game.self) {
        return;
      }
      TarotCard c = (TarotCard)game.self.deck.getCardAt(event.getX(),
        event.getY());      
      if (c == null) {
        return;
      }
      int cp = game.self.deck.getIndexOf(c);
      switch (game.state) {
        case Game.STATE_DISCARD:
          if ((c.getRank() == TarotCard.KING) || (c.isHonour())) {
            showMessage("Nem skartolhatsz "+(c.isHonour()?"honõrt!":
              "királyt!"), ICON_ERROR);
            return;
          }          
          game.processDiscard(game.self.getName(), cp);            
          if (game.serverSocket != null) {
            game.dispatch(Game.MSG_DISCARD+""+(char)(cp+Game.BASE)+
              game.self.getName(), null);
          } else {
            game.send(Game.MSG_DISCARD+""+(char)(cp+Game.BASE));
          }
          break;
        case Game.STATE_PLAY:
          if (game.current == game.taker) {
            if ((game.bonuses[Game.BONUS_PAGATULTI].getLevel() > 0) &&
              (game.turns != 8) && (c.getRank() == 6)) {
              showMessage(
                "A bemondott pagátulti miatt a pagátot nem játszhatod ki!",
                ICON_ERROR);
              return;
            }
            game.baseColor = c.getColor();
          } else {
            if ((c.getColor() != game.baseColor) && 
              (game.current.deck.containsColor(game.baseColor)) &&
              (game.baseColor != TarotCard.TAROT)) {
              showMessage("A hívott színbõl kell lapot tenned!", ICON_ERROR);
              return;
            }
            if ((c.getColor() != game.baseColor) &&
              (game.current.deck.containsColor(TarotCard.TAROT)) &&
              (c.getColor() != TarotCard.TAROT)) {
              showMessage("Tarokkot kell tenned!", ICON_ERROR);
              return;                
            }
            if ((game.bonuses[Game.BONUS_PAGATULTI].getLevel() > 0) &&
              (game.turns != 8) && (c.getRank() == 6) &&
              (game.current.deck.countTarots() > 1)) {
              showMessage(
                "A bemondott pagátulti miatt a pagátot nem játszhatod ki!",
                ICON_ERROR);
              return;              
            }            
          }
          game.processLay(game.self.getName(), cp);
          if (game.serverSocket != null) {
            game.dispatch(Game.MSG_LAY+""+(char)(cp+Game.BASE)+
              game.self.getName(), null);
          } else {
            game.send(Game.MSG_LAY+""+(char)(cp+Game.BASE));
          }
          break;
      }
    }    
  }
  
  class BidButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      int v = 0;
      for (int i = 0; i < bidButtons.length; i++) {
        if (e.getSource() == bidButtons[i]) {
          v = i+1;
          break;
        }
      }
      if (e.getSource() == hold) {
        v = Game.SIGN_HOLD;
      }
      if (e.getSource() == pass) {
        v = Game.SIGN_PASS;
        game.self.hasPassed = true;
      }
      if (game.serverSocket != null) {
        game.dispatch(Game.MSG_BID+""+(char)(v+Game.BASE)+game.self.getName(),
          null);
      } else {
        game.send(Game.MSG_BID+""+(char)(v+Game.BASE)); 
      }
      game.processBid(game.self.getName(), v);            
    }
  }
  
  class AnnullListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == annull) {
        if (game.serverSocket != null) {
          game.dispatch(Game.MSG_ANNULL+""+game.self.getName(), null);
        } else {
          game.send(Game.MSG_ANNULL+"");
        }        
        gameMessage(game.current.getName()+" bedobta a lapjait, új osztás.");
        if (game.serverSocket != null) {
          game.newParty();        
        }
      } else {
        game.nextDiscard(game.current);        
        if (game.serverSocket != null) {
          game.dispatch(Game.MSG_CONTINUE+""+game.self.getName(), null);
        } else {
          game.send(Game.MSG_CONTINUE+"");
        }
      } 
      buttons.removeAll();
      buttons.revalidate();
      repaint();
    }
  }
  
  class AnnounceListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {      
      StringBuffer s = new StringBuffer();
      boolean f = false;
      if (callList.getSelectedIndex() > -1) {
        for (int i = 0; i < game.tarotNames.length; i++) {
          if (callList.getSelectedItem().equals(game.tarotNames[i])) {
            s.append((char)(Game.BASE+i+7));
            f = true;
            break;
          }
        }        
      }
      if (!f) {
        s.append(Game.BASE);
      }      
      s.append(manyTarots.isSelected()?
        (game.current.deck.countTarots() == 8?"8":"9"):"0");
      s.append(gameAnnounce.isSelected()?(char)(Game.BASE+1):(char)Game.BASE);
      for (int i = 0; i < game.bonuses.length; i++) {
        s.append(game.bonuses[i].getCheckBox().isSelected()?(char)(Game.BASE+1):
          (char)(Game.BASE));
      }       
      game.processAnnounce(game.self.getName(), s.toString());
      if (game.serverSocket != null) {
        game.dispatch(Game.MSG_ANNOUNCE+""+s.toString()+game.self.getName(),
          null);
      } else {
        game.send(Game.MSG_ANNOUNCE+""+s.toString());
      }
      callList.removeAllItems();
      callList.setSelectedIndex(-1);
      buttons.removeAll();                      
      buttons.revalidate();      
      repaint();      
    }
  }
  
  class TakeListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      if (game.serverSocket != null) {
        game.dispatch(Game.MSG_TAKE+game.self.getName(), null);
      } else {
        game.send(Game.MSG_TAKE+"");
      } 
      buttons.remove(take);                      
      buttons.revalidate();      
      game.processTake(game.self.getName());
    }
  }
 
  class PagatUltiListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      if ((game.bonuses[Game.BONUS_PAGATULTI].getCheckBox().isSelected()) &&
        (game.self.deck.countTarots() >= 8)) {
        manyTarots.setSelected(true);
      }
    }
  }

  class DoubleGameListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      if ((game.bonuses[Game.BONUS_DOUBLEGAME].getCheckBox().isSelected()) &&
        (game.bonuses[Game.BONUS_DOUBLEGAME].getLevel() == 0) &&
        (game.bonuses[Game.BONUS_VOLAT].getLevel() == 0)) {
        game.bonuses[Game.BONUS_VOLAT].getCheckBox().setSelected(false);
      }
      if ((game.bonuses[Game.BONUS_DOUBLEGAME].getCheckBox().isSelected()) &&
        (game.bonuses[Game.BONUS_DOUBLEGAME].getLevel()%2 == 0) && 
        ((game.announcers.contains(game.current)?1:0) != (game.level%2)) && 
        (game.level < 6)) {
        gameAnnounce.setSelected(true);
      }
    }
  }
  
  class VolatListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      if ((game.bonuses[Game.BONUS_VOLAT].getCheckBox().isSelected()) &&
        (game.bonuses[Game.BONUS_DOUBLEGAME].getLevel() == 0) &&
        (game.bonuses[Game.BONUS_VOLAT].getLevel() == 0)) {
        game.bonuses[Game.BONUS_DOUBLEGAME].getCheckBox().setSelected(false);
      }
      if ((game.bonuses[Game.BONUS_VOLAT].getCheckBox().isSelected()) &&
        (game.bonuses[Game.BONUS_VOLAT].getLevel()%2 == 0) && 
        ((game.announcers.contains(game.current)?1:0) != (game.level%2)) && 
        (game.level < 6)) {
        gameAnnounce.setSelected(true);
      }
    }
  }
  
  class ManyTarotsListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      if (!manyTarots.isSelected()) {
        game.bonuses[Game.BONUS_PAGATULTI].getCheckBox().setSelected(false);
      }
    }
  }  
  
  class GameAnnounceListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {   
      if (!gameAnnounce.isSelected()) {
        if ((game.bonuses[Game.BONUS_DOUBLEGAME].getCheckBox().isSelected()) &&
          (game.bonuses[Game.BONUS_DOUBLEGAME].getLevel()%2 == 0)) {
          game.bonuses[Game.BONUS_DOUBLEGAME].getCheckBox().setSelected(false);
        }
        if ((game.bonuses[Game.BONUS_VOLAT].getCheckBox().isSelected()) &&
          (game.bonuses[Game.BONUS_VOLAT].getLevel()%2 == 0)) {
          game.bonuses[Game.BONUS_VOLAT].getCheckBox().setSelected(false);
        }
      }
    }    
  }
  
  class BonusTableModel extends AbstractTableModel {
    public int getRowCount() {
      int r = (game.value > 0?1:0);
      for (int i = 0; i < game.bonuses.length; i++) {
        if (game.bonuses[i].getLevel() > 0) {
          r++;
        }
      }
      return r;
    }
    public int getColumnCount() {
      return 3;
    }
    public Object getValueAt(int row, int col) {
      Object obj = null;
      int r = 0;
      if (row == 0) {
        if (game.value == 0) {
          return obj;
        }
        switch (col) {
          case 0:
            obj = Utils.firstUpcase(Game.gameNames[game.value]+" játék");
            break;            
          case 1:
            obj = "x"+Math.round(Math.pow(2, game.level-1));
            break;
          case 2:
            obj = game.level%2 == 0?"e":"j";
            break;            
        }
      } else {
        Bonus b = null;
        for (int i = 0; i < game.bonuses.length; i++) {
          if (game.bonuses[i].getLevel() > 0) {
            r++;
            if (r == row) {
              b = game.bonuses[i];
              break;
            }
          }
        }
        switch (col) {
          case 0:
            obj = Utils.firstUpcase(b.getName());
            break;
          case 1:
            obj = "x"+Math.round(Math.pow(2, b.getLevel()-1));
            break;
          case 2:
            obj = b.isOpponents()?"e":"j";
            break;
        }
      }
      return obj;
    }
  }

  class PlayerTableModel extends AbstractTableModel {
    public int getRowCount() {
      return game.community.size();
    }
    public int getColumnCount() {
      return 3;
    }
    public Class getColumnClass(int columnIndex) {
      return (columnIndex == 0)?ImageIcon.class:String.class;
    }
    public Object getValueAt(int row, int col) {
      Object obj = null;
      if (row >= game.community.size()) {
        return obj;
      }
      Player p = (Player)game.community.get(row);
      switch (col) {
        case 0:
          obj = playerIcons[(game.players.contains(p)?1:0)+
            (game.mayor == p?2:0)];
          break;
        case 1:
          obj = p.getName();
          break;
        case 2:
          obj = String.valueOf(p.getScore());
          break;
      }
      return obj;
    }
  }  

  class ResultTableModel extends AbstractTableModel {
    String headerNames[] = {"Játékos", "Ellenfelek"};
    public int getRowCount() {
      return 8;
    }
    public int getColumnCount() {
      return 2;
    }
    public String getColumnName(int col) {
      return headerNames[col]+(((col == 0) && (game.announcers.size() > 1))?
        "ok":"");
    }
    public Object getValueAt(int row, int col) {
      if (game.results[row][col] != null) {
        return Utils.firstUpcase(game.results[row][col]);
      } else {
        return "";
      }
    }
  }
  
  JXTable bonusTable = new JXTable() {
    public String getToolTipText(MouseEvent event) {
      int row = rowAtPoint(event.getPoint());
      if (row == 0) {
        return Utils.firstUpcase(Game.levelNames[game.level]+
          Game.gameNames[game.value])+" játék: "+
          Math.round(game.value*Math.pow(2, game.level-1))+" pontot ér, "+
          (game.level%2 == 0?"ellenfél":"játékos")+" mondta be.";
      } else {
        Bonus b = null;
        int r = 0;
        for (int i = 0; i < game.bonuses.length; i++) {
          if (game.bonuses[i].getLevel() > 0) {
            r++;
            if (r == row) {
              b = game.bonuses[i];
              break;
            }
          }
        }        
        return Utils.firstUpcase(Game.levelNames[b.getLevel()]+b.getName())+
          ": "+Math.round((b.getBase())*Math.pow(2, b.getLevel()))+
          " pontot ér, "+(b.isOpponents()?"ellenfél":"játékos")+" mondta be.";
      }
    }
  };
  JScrollPane bonusScroll = new JScrollPane(bonusTable,
    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
  JTextArea messageLog = new JTextArea();
  JScrollPane messageScroll = new JScrollPane(messageLog,
    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
  JPanel gameInfo = new JPanel();
  
  JPanel cardTable = new JPanel() {
    public void paintComponent(Graphics g) {
      Graphics2D gc = (Graphics2D)g;
      gc.setColor(new Color(0, 127, 0));
      gc.fillRect(0, 0, getSize().width, getSize().height);
      if (game.getState() != Game.STATE_SCORE) {
        int s = game.players.contains(game.self)?game.players.indexOf(game.self)
          :0;
        for (int i = 0; i < game.players.size(); i++) {
          Player p = (Player)game.players.get((s+i)%game.players.size());
          gc.setColor((p == game.current?Color.yellow:Color.white));
          int tx = 0;
          int ty = 0;
          switch (i) {
            case 0:
              tx = (getWidth()-
                getFontMetrics(gc.getFont()).stringWidth(p.getName()))/2;
              ty = game.deckPositions[i].y+124;
              break;
            case 1:
              ty = (getHeight()+
                getFontMetrics(gc.getFont()).stringWidth(p.getName()))/2;
              tx = game.deckPositions[i].x+124;
              break;
            case 2:
              tx = (getWidth()+
                getFontMetrics(gc.getFont()).stringWidth(p.getName()))/2;
              ty = game.deckPositions[i].y-124;
              break;            
            case 3:
              ty = (getHeight()-
                getFontMetrics(gc.getFont()).stringWidth(p.getName()))/2;
              tx = game.deckPositions[i].x-124;
              break;
          }
          gc.rotate(-1*i*Math.PI/2, tx, ty);
          gc.drawString(p.getName(), tx, ty);
          gc.rotate(i*Math.PI/2, tx, ty);
          gc.rotate(-1*i*Math.PI/2, p.deck.getX(), p.deck.getY());
          if ((p == game.self) ||
            (!game.players.contains(game.self))) {            
            p.deck.draw(gc);
          } else {
            p.deck.drawTurned(gc);
          }
          gc.rotate(i*Math.PI/2, p.deck.getX(), p.deck.getY());
          gc.rotate(-1*i*Math.PI/2, p.laid.getX(), p.laid.getY());          
          p.laid.draw(gc);
          gc.rotate(i*Math.PI/2, p.laid.getX(), p.laid.getY());        
          gc.rotate(-1*i*Math.PI/2, p.taken.getX(), p.taken.getY());          
          p.taken.drawTurned(gc);
          gc.rotate(i*Math.PI/2, p.taken.getX(), p.taken.getY());                
          gc.rotate(-1*i*Math.PI/2, p.talon.getX(), p.talon.getY());          
          if ((game.getState() == Game.STATE_DISCARD) && (p == game.announcer) &&
            (p.talon.size() >0) &&
            (p.talon.getLast().getColor() == TarotCard.TAROT)) {
            p.talon.draw(gc);
          } else {
            p.talon.drawTurned(gc);
          }
          gc.rotate(i*Math.PI/2, p.talon.getX(), p.talon.getY());
        }
      } else {
        gc.setColor(Color.white);
        String str = "A játékos"+(game.announcers.size() > 1?"ok":"")+
          " paklija:";
        gc.drawString(str, (getWidth()-
          getFontMetrics(gc.getFont()).stringWidth(str))/2,
          game.announcersDeck.getY()-15);
        game.announcersDeck.draw(gc);
        str = "Az ellenfelek paklija:";
        gc.drawString(str, (getWidth()-
          getFontMetrics(gc.getFont()).stringWidth(str))/2,
          game.opponentsDeck.getY()-15);
        game.opponentsDeck.draw(gc);
      }
    }
    public String getToolTipText(MouseEvent event) {
      if (game.players.contains(game.self)) {
        Card c = game.self.deck.getCardAt(event.getX(), event.getY());
        if (c != null) {
          return c.toString();
        }
      }
      return null;
    }
  };
  
  MouseListener cardTableListener = new CardTableListener();
  ActionListener bidButtonListener = new BidButtonListener();
  ActionListener annullListener = new AnnullListener();
  ActionListener announceListener = new AnnounceListener();
  
  JCheckBox playerCandidate = new JCheckBox("Résztvevõ szeretnék lenni");
  JButton startGame = new JButton();
  JButton endGame = new JButton("Játék befejezése");
  JXTable playerTable = new JXTable() {
    public String getToolTipText(MouseEvent event) {
      Player p = (Player)game.community.get(rowAtPoint(event.getPoint()));
      return p.getName()+" "+
        (game.players.contains(p)?"résztvevõ":"nézõ")+
        ", "+p.getScore()+" pontja van."+(game.mayor == p?
          "Õ a polgármester!":"");
    }
  };
  JScrollPane playerScroll = new JScrollPane(playerTable,
    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
  ImageIcon playerIcons[] = {new ImageIcon("data/observer.gif"),
    new ImageIcon("data/player.gif"), new ImageIcon("data/mayorobserver.gif"),
    new ImageIcon("data/mayorplayer.gif")};  
  JTextArea chatBox = new JTextArea();
  JScrollPane chatScroll = new JScrollPane(chatBox,
    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
  JTextField chatInput = new JTextField();
  JPanel playerInfo = new JPanel();  
  
  JPanel gamePanel = new JPanel();
  
  JLabel nickLabel = new JLabel("Név:");
  JTextField nick = new JTextField(10);
  JPanel nickPanel = new JPanel();
  ButtonGroup roomAction = new ButtonGroup();
  JRadioButton newRoom = new JRadioButton("Új szoba létrehozása", true);
  JRadioButton joinRoom = new JRadioButton("Csatlakozás a következõ szobához:",
    false);
  JTextField roomName = new JTextField(8);
  JPanel roomPanel = new JPanel();
  JButton nextButton = new JButton("Tovább");
  JPanel nextPanel = new JPanel();
  JPanel loginPanel = new JPanel();
 
  JButton bidButtons[] = {new JButton("Hármas játék"),
    new JButton("Kettes játék"), new JButton("Egyes játék"),
    new JButton("Szóló játék")};
  JButton hold = new JButton("Tartom");
  JButton pass = new JButton("Passz");
  JButton annull = new JButton("Bedobom a lapjaimat");
  JButton notAnnull = new JButton("Mehet tovább a játék");
  JButton announce = new JButton("Mehet");
  JCheckBox manyTarots = new JCheckBox();
  JCheckBox gameAnnounce = new JCheckBox();
  JPanel bonusPanel = new JPanel();
  JLabel callLabel = new JLabel("A meghívott tarokk:");
  JComboBox callList = new JComboBox();
  JPanel callPanel = new JPanel();
  JButton take = new JButton(new ImageIcon("data/take.gif"));
  JLabel headerLabel = new JLabel("Eredmények");
  JLabel bottomLabel = new JLabel();
  
  JTable resultTable = new JTable();
  JScrollPane resultScroll = new JScrollPane(resultTable,
    JScrollPane.VERTICAL_SCROLLBAR_NEVER,
    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
  
  JPanel buttons = new JPanel();
  
  ImageIcon messageIcons[] = {new ImageIcon("data/info.gif"),
    new ImageIcon("data/error.gif"), new ImageIcon("data/wait.gif")};
  static final int ICON_INFO = 0;
  static final int ICON_ERROR = 1;
  static final int ICON_WAIT = 2;    
  JLabel messageLabel = new JLabel();

  private Game game;  
  
  public MainWindow() {
    super("Java Online Tarokk");
    setSize(500, 200);    
    setResizable(false);
    addWindowListener(new CloseListener());
    gamePanel.setLayout(new BorderLayout());
    gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
    bonusTable.setSelectionBackground(Color.white);
    bonusScroll.setPreferredSize(new Dimension(160, 148));
    gameInfo.add(bonusScroll);
    messageLog.setEditable(false);
    messageLog.setLineWrap(true);
    messageLog.setWrapStyleWord(true);
    messageScroll.setPreferredSize(new Dimension(178, 700));
    gameInfo.add(messageScroll);
    endGame.addActionListener(new EndGameListener());
    endGame.setMnemonic('b');     
    endGame.setAlignmentX(Component.CENTER_ALIGNMENT);
    gamePanel.add(gameInfo, BorderLayout.WEST);
    ToolTipManager.sharedInstance().setReshowDelay(0);
    cardTable.setLayout(new BoxLayout(cardTable, BoxLayout.X_AXIS));
    cardTable.setToolTipText("");
    cardTable.addMouseListener(cardTableListener);
    buttons.setAlignmentX(Component.CENTER_ALIGNMENT);    
    buttons.setAlignmentY(Component.CENTER_ALIGNMENT);
    buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));    
    for (int i = 0; i < bidButtons.length; i++) {
      bidButtons[i].setAlignmentX(Component.CENTER_ALIGNMENT);
      bidButtons[i].addActionListener(bidButtonListener);
    }
    hold.setAlignmentX(Component.CENTER_ALIGNMENT);
    hold.addActionListener(bidButtonListener);
    pass.setAlignmentX(Component.CENTER_ALIGNMENT);
    pass.addActionListener(bidButtonListener);
    annull.setAlignmentX(Component.CENTER_ALIGNMENT);
    annull.addActionListener(annullListener);
    notAnnull.setAlignmentX(Component.CENTER_ALIGNMENT);
    notAnnull.addActionListener(annullListener);
    announce.setAlignmentX(Component.CENTER_ALIGNMENT);
    announce.addActionListener(announceListener);
    bonusPanel.setLayout(new BoxLayout(bonusPanel, BoxLayout.Y_AXIS));    
    bonusPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
    callPanel.setLayout(new BoxLayout(callPanel, BoxLayout.X_AXIS));        
    callPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
    callPanel.setMaximumSize(new Dimension(195, 26)); 
    callPanel.add(callLabel);
    callPanel.add(callList);
    take.setAlignmentX(Component.CENTER_ALIGNMENT);
    take.addActionListener(new TakeListener());
    headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    headerLabel.setForeground(Color.white);
    resultTable.getTableHeader().setReorderingAllowed(false);
    resultTable.getTableHeader().setResizingAllowed(false);
    resultTable.setSelectionBackground(Color.white);
    resultScroll.setMaximumSize(new Dimension(460, 130));
    bottomLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    bottomLabel.setForeground(Color.white);
    cardTable.add(Box.createHorizontalGlue());
    cardTable.add(buttons, BorderLayout.CENTER);
    cardTable.add(Box.createHorizontalGlue());
    cardTable.validate();
    gamePanel.add(cardTable, BorderLayout.CENTER);
    playerInfo.setLayout(new BoxLayout(playerInfo, BoxLayout.Y_AXIS));
    playerCandidate.setAlignmentX(Component.CENTER_ALIGNMENT);
    playerCandidate.addActionListener(new PlayerCandidateListener());
    playerCandidate.setMnemonic('r');
    playerScroll.setPreferredSize(new Dimension(179, 300));
    playerTable.setShowGrid(false);    
    playerTable.setSelectionBackground(Color.white);
    chatBox.setEditable(false);
    chatBox.setLineWrap(true);
    chatBox.setWrapStyleWord(true);
    chatScroll.setPreferredSize(new Dimension(179, 700));
    chatInput.addKeyListener(new ChatMessageListener());
    startGame.setAlignmentX(Component.CENTER_ALIGNMENT);
    startGame.addActionListener(new StartGameListener());
    gamePanel.add(playerInfo, BorderLayout.EAST);
    loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
    nickPanel.add(nickLabel);
    nickPanel.add(nick);
    loginPanel.add(nickPanel);
    roomPanel.add(newRoom);
    roomPanel.add(joinRoom);
    roomAction.add(newRoom);
    roomAction.add(joinRoom);
    newRoom.setMnemonic('l');
    joinRoom.setMnemonic('c');
    roomPanel.add(roomName);
    loginPanel.add(roomPanel);
    nextPanel.add(nextButton);
    nextButton.addActionListener(new NextButtonListener());
    nextButton.setMnemonic('t');
    nick.addKeyListener(new NextKeyListener());
    roomName.addKeyListener(new NextKeyListener());
    loginPanel.add(nextPanel);
    loginPanel.setBorder(BorderFactory.createRaisedBevelBorder());
    messageLabel.setHorizontalAlignment(JLabel.LEFT);
    messageLabel.setBorder(BorderFactory.createLineBorder(Color.gray));
    getContentPane().add(messageLabel, BorderLayout.SOUTH);
    showMessage("A program betöltése folyamatban...", ICON_WAIT);
    validate();
    setVisible(true);
  }

  public void loadImages() {
    final MediaTracker imageTracker = new MediaTracker(this);
    game.backImage = Toolkit.getDefaultToolkit().createImage("data/0.gif");
    imageTracker.addImage(game.backImage, 0);
    for (int i = 0; i < 22; i++) {
      game.cardImages[i] = Toolkit.getDefaultToolkit().getImage("data/5"+
        String.valueOf(i+1)+".gif");
      imageTracker.addImage(game.cardImages[i], 0);
    }
    for (int i = 0; i < 20; i++) {
      game.cardImages[22+i] = Toolkit.getDefaultToolkit().getImage("data/"+
        String.valueOf(i/5+1)+String.valueOf(i%5+1)+".gif");
      imageTracker.addImage(game.cardImages[22+i], 0);
    }
    new Thread(new Runnable() {
      public void run() {
        boolean ok = true;
        try {
          imageTracker.waitForAll();
        } catch (InterruptedException e) {
          ok = false;
        }
        if (!imageTracker.isErrorAny() && ok) {
          showMessage("Üdvözöl a Java Online Tarokk!", ICON_INFO);
          game.setState(Game.STATE_OFFLINE);    
          setTitle("Java Online Tarokk");
          remove(playerInfo);
          remove(gamePanel);
          getContentPane().add(loginPanel, BorderLayout.CENTER);
          nick.requestFocus();
          validate();
          repaint();
        } else {
          showMessage("Hiányoznak a program futásához szükséges fájlok!",
            ICON_ERROR);      
        }
      }
    }).start();
  }

  public void refreshResults() {
    resultTable.setModel(new ResultTableModel());
  }
  
  public void setGame(Game game) {
    this.game = game;
    loadImages();    
    playerTable.setModel(new PlayerTableModel());
    playerTable.getColumnModel().getColumn(0).setMaxWidth(30);
    playerTable.getColumnModel().getColumn(2).setMaxWidth(45);
    playerTable.setAutoCreateColumnsFromModel(false);    
    bonusTable.setModel(new BonusTableModel());
    bonusTable.getColumnModel().getColumn(0).setPreferredWidth(206);
    bonusTable.getColumnModel().getColumn(2).setPreferredWidth(16);
    game.bonuses[Game.BONUS_PAGATULTI].getCheckBox().addActionListener(new 
      PagatUltiListener());
    game.bonuses[Game.BONUS_DOUBLEGAME].getCheckBox().addActionListener(new 
      DoubleGameListener());    
    game.bonuses[Game.BONUS_VOLAT].getCheckBox().addActionListener(new 
      VolatListener());
    manyTarots.addActionListener(new ManyTarotsListener());
    gameAnnounce.addActionListener(new GameAnnounceListener());
  }

  public void showMessage(String msg, int icon) {
    messageLabel.setText(msg);
    messageLabel.setIcon(messageIcons[icon]);
  }

  public void chatMessage(String msg) {
    chatBox.append(msg+"\n");
    chatBox.setCaretPosition(chatBox.getDocument().getLength());
  }

  public void gameMessage(String msg) {
    messageLog.append(msg+"\n");
    messageLog.setCaretPosition(messageLog.getDocument().getLength());
  }  
  
  public void chatScreen() {
    game.setState(Game.STATE_ONLINE);    
    setSize(500, 705);
    chatScroll.setMaximumSize(null);
    remove(loginPanel);
    remove(gamePanel);
    playerInfo.removeAll();
    playerInfo.add(playerCandidate);
    playerInfo.add(playerScroll);    
    playerInfo.add(chatScroll);
    playerInfo.add(chatInput);
    if (game.serverSocket != null) {
      playerInfo.add(startGame);
    }
    getContentPane().add(playerInfo, BorderLayout.CENTER);
    startGame.setEnabled(game.players.size() == Game.NUM_PLAYERS);
    startGame.setText("Kezdõdhet a játék!");
    startGame.setMnemonic('k');
    validate();
    repaint();
  }
  
  public void gameScreen() {
    game.starter = null;
    if (game.serverSocket != null) {
      game.newParty();        
    }
    setSize(1024, 705);
    chatScroll.setMaximumSize(new Dimension(179, 700));
    remove(playerInfo);
    playerInfo.remove(playerCandidate);
    if (game.serverSocket != null) {    
      playerInfo.remove(startGame);
    }
    gamePanel.add(playerInfo, BorderLayout.EAST);
    getContentPane().add(gamePanel, BorderLayout.CENTER);
    chatMessage("Új játék indult.");
    messageLog.setText("");
    gameMessage("Résztvevõk: ");    
    for (int i = 0; i < game.players.size(); i++) {
      gameMessage(((Player)game.players.get(i)).getName());
    }
    gameMessage("Játék kezdete: ");
    gameMessage(new SimpleDateFormat("yyyy. MM. dd. HH:mm:ss").format(
      new Date()));    
    gameMessage("--------------------------------");    
    validate();
    repaint();
  }
  
  public static void main(String[] args) {
    new Game(new MainWindow());
  }

}
