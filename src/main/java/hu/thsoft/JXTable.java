/*
 * TGC
 *
 * Class JXTable
 *
 * (c) thSoft
 */

package hu.thsoft;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

/**
 * A <code>JTable</code> with its column headers hidden, being able to update
 * its display according to its model.
 *
 * @author thSoft
 */
public class JXTable extends JTable {
  
  /**
   * This method is responsible for the invisibility of the column headers.
   */
  protected void configureEnclosingScrollPane() {
    Container p = getParent();
    if (p instanceof JViewport) {
      Container gp = p.getParent();
      if (gp instanceof JScrollPane) {
        JScrollPane scrollPane = (JScrollPane)gp;
        JViewport viewport = scrollPane.getViewport();
        if ((viewport == null) || (viewport.getView() != this)) {
          return;
        }
        scrollPane.getViewport().setBackingStoreEnabled(true);
        scrollPane.setBorder(UIManager.getBorder("Table.scrollPaneBorder"));
      }
    }
  }

  /**
   * Updates the view according to its model.
   */
  public void refresh() {
    if (getModel() != null) {
      ((AbstractTableModel)getModel()).fireTableDataChanged();
    }
  }  
  
}
