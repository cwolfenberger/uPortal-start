package org.jasig.portal;

import javax.servlet.*;
import javax.servlet.jsp.*;
import javax.servlet.http.*;

import java.io.*;
import java.util.*;
import java.text.*;
import java.sql.*;
import java.net.*;
import com.objectspace.xml.*;
import org.jasig.portal.layout.*;

/**
 * Provides methods associated with displaying and modifying
 * a user's layout.  This includes changing the colors, size and
 * positions of tabs, columns, and channels.
 * @author Ken Weiner
 * @version %I%, %G%
 */
public class LayoutBean extends GenericPortalBean
                        implements ILayoutBean
{       
  private static boolean bPropsLoaded = false;
  private static String sPathToLayoutDtd = null;
  private static String sLayoutDtd = "layout.dtd";

  /**
   * Default constructor
   */
  public LayoutBean ()
  {
    try
    {
      if (!bPropsLoaded)
      {
        File layoutPropsFile = new File (getPortalBaseDir () + "properties\\layout.properties");
        Properties layoutProps = new Properties ();
        layoutProps.load (new FileInputStream (layoutPropsFile));
        sPathToLayoutDtd = layoutProps.getProperty ("pathToLayoutDtd");
        bPropsLoaded = true;
      }
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }
  
  /**
   * Gets a tab
   * @param the servlet request object
   * @param the tab's index
   */
  public ITab getTab (HttpServletRequest req, int iTab)
  {
    IXml layoutXml = getLayoutXml (req, getUserName (req));
    ILayout layout = (ILayout) layoutXml.getRoot ();
    ITab tab = layout.getTabAt (iTab); 
    return tab;
  }
  
  /**
   * Gets a column
   * @param the servlet request object
   * @param the tab's index
   * @param the column's index
   */
  public IColumn getColumn (HttpServletRequest req, int iTab, int iCol)
  {
    IXml layoutXml = getLayoutXml (req, getUserName (req));
    ILayout layout = (ILayout) layoutXml.getRoot ();
    ITab tab = layout.getTabAt (iTab); 
    IColumn column = tab.getColumnAt (iCol);
    return column;
  }
  
  /**
   * Gets a channel
   * @param the servlet request object
   * @param the tab's index
   * @param the column's index
   * @param the channels's index
   */
  public org.jasig.portal.layout.IChannel getChannel (HttpServletRequest req, int iTab, int iCol, int iChan)
  {
    IXml layoutXml = getLayoutXml (req, getUserName (req));
    ILayout layout = (ILayout) layoutXml.getRoot ();
    ITab tab = layout.getTabAt (iTab); 
    IColumn column = tab.getColumnAt (iCol);
    org.jasig.portal.layout.IChannel channel = column.getChannelAt (iChan);
    return channel;
  }
  
  /**
   * Writes an html body tag with colors set according to user preferences
   * @param the servlet request object
   * @param the servlet response object
   * @param the JspWriter object
   */
  public void writeBodyTag (HttpServletRequest req, HttpServletResponse res, JspWriter out)
  {    
    try 
    {    
      IXml layoutXml = getLayoutXml (req, getUserName (req));
      ILayout layout = (ILayout) layoutXml.getRoot ();
      String sBgColor = layout.getAttribute ("bgcolor");
      String sFgColor = layout.getAttribute ("fgcolor");
      out.println ("<body bgcolor=\"" + sBgColor + "\" text=\"" + sFgColor + "\">");
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }
    
  /**
   * Retrieves a handle to the layout xml
   * @param the servlet request object
   * @param user name
   * @return handle to the layout xml
   */
  public IXml getLayoutXml (HttpServletRequest req, String sUserName)
  {    
    RdbmServices rdbmService = new RdbmServices ();
    Connection con = null;
    IXml layoutXml = null;
    
    try 
    {    
      HttpSession session = req.getSession (false);
      layoutXml = (IXml) session.getAttribute ("layoutXml");
      
      if (layoutXml != null)
        return layoutXml;
      
      if (sUserName == null)
        sUserName = "guest";
                
      con = rdbmService.getConnection ();
      Statement stmt = con.createStatement();
        
      String sQuery = "SELECT LAYOUT_XML FROM USERS WHERE USER_NAME='" + sUserName + "'";
      System.out.println (sQuery);
      ResultSet rs = stmt.executeQuery (sQuery);
      
      if (rs.next ())
      {
        String sLayoutXml = rs.getString ("LAYOUT_XML");
        
        // Tack on the full path to layout.dtd
        int iInsertBefore = sLayoutXml.indexOf (sLayoutDtd);
        sLayoutXml = sLayoutXml.substring (0, iInsertBefore) + sPathToLayoutDtd + sLayoutXml.substring (iInsertBefore);

        String xmlFilePackage = "org.jasig.portal.layout";
        layoutXml = Xml.openDocument (xmlFilePackage, new StringReader (sLayoutXml));
        session.setAttribute ("layoutXml", layoutXml);
      }
      stmt.close ();
      
      return layoutXml;
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }    
    finally
    {
      rdbmService.releaseConnection (con);
    }
    return null;
  }  
    
  public void setLayoutXml (String sUserName, IXml layoutXml)
  {    
    RdbmServices rdbmService = new RdbmServices ();
    Connection con = null;
    
    try 
    { 
      if (sUserName == null)
        sUserName = "guest";
        
      con = rdbmService.getConnection ();
      Statement stmt = con.createStatement();
        
      StringWriter sw = new StringWriter ();
      layoutXml.saveDocument (sw);
      String sLayoutXml = sw.toString();
      
      // Remove path to layout dtd before saving
      int iRemoveFrom = sLayoutXml.indexOf (sPathToLayoutDtd);
      int iRemoveTo = sLayoutXml.indexOf (sLayoutDtd);
      sLayoutXml = sLayoutXml.substring (0, iRemoveFrom) + sLayoutXml.substring (iRemoveTo);
        
      String sUpdate = "UPDATE USERS SET LAYOUT_XML='" + sLayoutXml + "' WHERE USER_NAME='" + sUserName + "'";
      int iUpdated = stmt.executeUpdate (sUpdate);
      System.out.println ("Saving layout xml. Updated " + iUpdated + " rows.");
      stmt.close ();
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
    finally
    {
      rdbmService.releaseConnection (con);
    }    
  }      
    
  /**
   * Retrieves the active tab
   * @param the servlet request object
   * @return the active tab
   */
  public int getActiveTab (HttpServletRequest req)
  {    
    int iActiveTab = 0;
    
    try 
    {    
      IXml layoutXml = getLayoutXml (req, getUserName (req));
      ILayout layout = (ILayout) layoutXml.getRoot ();
      
      HttpSession session = req.getSession (false);
      String sTabParameter = req.getParameter ("tab");
      String sTabSession = (String) session.getAttribute ("activeTab");
      
      if (sTabParameter != null)
        iActiveTab = Integer.parseInt (sTabParameter);
      else if (sTabSession != null)
        iActiveTab = Integer.parseInt (sTabSession);
      else
      {
        // Active tab has not yet been set. Read it from layout.xml
        iActiveTab = Integer.parseInt (layout.getAttribute ("activeTab"));
      }

      // If tab is not within acceptable range, use the first tab
      if (iActiveTab >= layout.getTabCount ())
        iActiveTab = 0;
            
      setActiveTab (req, iActiveTab);
      return iActiveTab;
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }    
    return iActiveTab;
  }  
        
  /**
   * Stores the active tab in the session
   * @param the servlet request object
   * @param active tab
   * @param user name
   */
  public void setActiveTab (HttpServletRequest req, int iTab)
  {    
    try 
    {      
      HttpSession session = req.getSession (false);
      session.setAttribute ("activeTab", String.valueOf (iTab));
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }    
  }          
        
  /**
   * Displays tabs
   * @param the servlet request object
   * @param the servlet response object
   * @param the JspWriter object
   */
  public void writeTabs (HttpServletRequest req, HttpServletResponse res, JspWriter out)
  {    
    try 
    {      
      out.println ("<!-- Tabs -->");
      out.println ("<table border=0 width=100% cellspacing=0 cellpadding=0>");
      out.println ("<tr>");
      
      IXml layoutXml = getLayoutXml (req, getUserName (req));
      ILayout layout = (ILayout) layoutXml.getRoot ();
      
      // Get Tabs
      ITab[] tabs = layout.getTabs ();
            
      int iTab = getActiveTab (req);
      ITab activeTab = getTab (req, iTab);
                                      
      String sBgcolor = null;
      String sTabName = activeTab.getAttribute ("name");    
      String sActiveTab = activeTab.getAttribute ("name");
      String sTabColor = layout.getAttribute ("tabColor");
      String sActiveTabColor = layout.getAttribute ("activeTabColor");
      
      for (int i = 0; i < tabs.length; i++)
      {
        sTabName = tabs[i].getAttribute ("name");
        sBgcolor = sTabName.equals (sActiveTab) ? sActiveTabColor : sTabColor;  
        
        if (sTabName.equals (sActiveTab))
          activeTab = tabs[i];
        
        out.println ("<td bgcolor=" + sBgcolor + " align=center width=20%>");                        
        out.println ("  <table bgcolor=" + sBgcolor + " border=0 width=100% cellspacing=0 cellpadding=2>");
        out.println ("    <tr align=center>");
        
        if (sTabName.equals (sActiveTab))
          out.println ("      <td><font face=Arial >&nbsp;<b>" + sTabName + "</b></font>&nbsp;</td>");
        else
          out.println ("      <td><font face=Arial size=-1>&nbsp;<b><a href=\"layout.jsp?tab=" + i + "\">" + sTabName + "</a></b></font>&nbsp;</td>");
        
        out.println ("    </tr>");
        out.println ("  </table>");
        out.println ("</td>");
        out.println ("<td width=1%>&nbsp;</td>");                
      }
      
      // Links to personalize layout
      out.println ("<td align=right bgcolor=" + sTabColor + " width=98%><font size=-1 face=Arial>Personalize&nbsp;[<a href=\"personalizeColors.jsp\">Colors</a>]&nbsp;-&nbsp;[<a href=\"personalizeLayout.jsp\">Layout</a>]&nbsp;</font></td>");
      out.println ("</tr>");
      
      // This is the strip beneath the tabs
      out.println ("<tr><td width=100% colspan=7 bgcolor=" + sActiveTabColor + " height=3></td></tr>");

      out.println ("</table>");
      out.println ("<br>");      
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }
  
  /**
   * Displays channels
   * @param the servlet request object
   * @param the servlet response object
   * @param the JspWriter object
   */
  public void writeChannels (HttpServletRequest req, HttpServletResponse res, JspWriter out)
  {    
    try 
    {      
      int iTab = getActiveTab (req);
      ITab activeTab = getTab (req, iTab);
      
      HttpSession session = req.getSession (false);
      
      if (activeTab != null)
      {
        out.println ("<!-- Channels -->");
        out.println ("<table border=0 cellpadding=0 cellspacing=0 width=100%>");
        out.println ("  <tr>");
                
        IColumn[] columns = activeTab.getColumns ();
        
        for (int iCol = 0; iCol < columns.length; iCol++)
        {
          out.println ("    <td valign=top width=" + columns[iCol].getAttribute ("width") + ">");
          
          // Get channels for column iCol
          org.jasig.portal.layout.IChannel[] channels = columns[iCol].getChannels ();
          
          for (int iChan = 0; iChan < channels.length; iChan++)
          {
            org.jasig.portal.IChannel ch = getChannelInstance (channels[iChan]);            
                                                
            // Check for minimized, maximized, or removed channel
            String sResize = req.getParameter ("resize");
            String sTab = req.getParameter ("tab");
            String sColumn = req.getParameter ("column");
            String sChannel = req.getParameter ("channel");
            
            if (sResize != null && iTab == Integer.parseInt (sTab) && iCol == Integer.parseInt (sColumn) && iChan == Integer.parseInt (sChannel))
            {
              if (sResize.equals("minimize"))
                channels[iChan].setAttribute("minimized", "true");
              else if (sResize.equals("maximize"))
                channels[iChan].setAttribute("minimized", "false");
              else if (sResize.equals ("remove"))
              {
                columns[iCol].removeChannel (channels[iChan]);
                continue;
              }
            }
            
            out.println ("<table border=0 cellpadding=1 cellspacing=4 width=100%>");
            out.println ("  <tr>");
            out.println ("    <td bgcolor=cccccc>");
                        
            // Channel heading
            IXml layoutXml = getLayoutXml (req, getUserName (req));
            ILayout layout = (ILayout) layoutXml.getRoot ();
            
            out.println ("      <table border=0 cellpadding=0 cellspacing=0 width=100% bgcolor=" + layout.getAttribute ("channelHeadingColor") + ">");
            out.println ("        <tr>");
            out.println ("          <td>");
            out.println ("            <font face=arial color=#000000><b>&nbsp;" + ch.getName() + "</b></font>");
            out.println ("          </td>");
            out.println ("          <td nowrap valign=center align=right>");
            out.println ("            &nbsp;");
            
            // Channel control buttons
            if (channels[iChan].getAttribute ("minimized").equals ("true"))
              out.println ("<a href=\"layout.jsp?tab=" + iTab + "&column=" + iCol + "&channel=" + iChan + "&resize=maximize\"><img border=0 src=\"images/maximize.gif\" alt=\"Maximize\"></a>");
            else if (ch.isMinimizable())
              out.println ("<a href=\"layout.jsp?tab=" + iTab + "&column=" + iCol + "&channel=" + iChan + "&resize=minimize\"><img border=0 src=\"images/minimize.gif\" alt=\"Minimize\"></a>");
            
            if (ch.isDetachable())
              out.println ("<a href=\"JavaScript:openWin(\'detach.jsp?tab=" + iTab + "&column=" + iCol + "&channel=" + iChan + "\', \'detachedWindow\', 200, 350)\"><img border=0 src=\"images/detach.gif\" alt=\"Detach\"></a>");
            
            if (ch.isRemovable())
              out.println ("<a href=\"layout.jsp?tab=" + iTab + "&column=" + iCol + "&channel=" + iChan + "&resize=remove\"><img border=0 src=\"images/remove.gif\" alt=\"Remove\"></a>");
            
            if (ch.isEditable())
              out.println ("<a href=\"edit.jsp?tab=" + iTab + "&column=" + iCol + "&channel=" + iChan + "\"><img border=0 src=\"images/edit.gif\" alt=\"Edit\"></a>");
            
            out.println ("            &nbsp;");
            out.println ("          </td>");            
            out.println ("        </tr>");
            out.println ("      </table>");

            // Channel body
            out.println ("      <table border=0 cellpadding=0 cellspacing=0 width=100%>");
            out.println ("        <tr>");
            out.println ("          <td bgcolor=#ffffff>");

            out.println ("            <table border=0 cellpadding=3 cellspacing=0 width=100% bgcolor=#ffffff>");
            out.println ("              <tr>");
            out.println ("                <td valign=top>");
                            
            if (channels[iChan].getAttribute ("minimized").equals ("false"))
            {                          
              // Render channel contents
              ch.render (req, res, out);
            }
            else
            {
              // Channel is minimized -- don't render it
            }
              
            out.println ("                </td>");
            out.println ("              </tr>");
            out.println ("            </table>");

            out.println ("          </td>");
            out.println ("        </tr>");
            out.println ("      </table>");

            out.println ("    </td>");
            out.println ("  </tr>");
            out.println ("</table>");
          }
          
          out.println ("    </td>");
        }
        
        out.println ("  </tr>");
        out.println ("</table>");
      }
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }  
  
  /**
   * Presents a GUI for manipulating the layout. Tabs, columns, and channels can
   * be added, removed, resized, and renamed.
   * @param the servlet request object
   * @param the servlet response object
   * @param the JspWriter object
   */
  public void writePersonalizeLayoutPage (HttpServletRequest req, HttpServletResponse res, JspWriter out)
  {    
    try 
    { 
      // Hard coded for now, but eventually
      // retrieve available channels from database
      Vector vChannels = new Vector ();
      vChannels.addElement ("ABC News");
      vChannels.addElement ("My Bookmarks");
      vChannels.addElement ("Horoscope");
      vChannels.addElement ("My Applications");
      vChannels.addElement ("Page Renderer");
      vChannels.addElement ("Scoreboard");
      vChannels.addElement ("Search");
      vChannels.addElement ("Weather");
      
      // List available channels
      out.println ("<table border=0 width=100% cellspacing=0 cellpadding=0>");
      out.println ("<tr bgcolor=#dddddd>");
      out.println ("<td>");
      
      for (int i = 0; i < vChannels.size () / 2; i++)
        out.println ("<input type=checkbox name=\"channel" + i + "\">" + (String) vChannels.elementAt (i) + "<br>");
      
      out.println ("</td>");
      out.println ("<td>");
      
      for (int i = vChannels.size () / 2 + 1; i < vChannels.size (); i++)
        out.println ("<input type=checkbox name=\"channel" + i + "\">" + (String) vChannels.elementAt (i) + "<br>");
      
      out.println ("</td>");
      out.println ("</tr>");
      out.println ("</table><br>");
    
      IXml layoutXml = getLayoutXml (req, getUserName (req));
      ILayout layout = (ILayout) layoutXml.getRoot ();
      
      // Get Tabs
      ITab[] tabs = layout.getTabs ();
 
      // Add new channels
      out.println ("<input type=submit name=\"\" value=\"Add\">");
      out.println ("checked channels to Tab ");
      out.println ("<select name=\"tab\">");
                
      for (int iTab = 0; iTab < tabs.length; iTab++)
        out.println ("<option>" + (iTab + 1) + "</option>");
        
      out.println ("</select>");
      out.println ("Column ");
      out.println ("<select name=\"column\">");
          
      // Find the max number of columns in any tab
      int iMaxCol = 0;
      
      for (int iTab = 0; iTab < tabs.length; iTab++)
      {
        IColumn[] columns = tabs[iTab].getColumns ();
        
        for (int iCol = 1; iCol <= columns.length; iCol++)
        {
          if (iCol > iMaxCol)
            iMaxCol = iCol;
        }
      }
      
      for (int iCol = 0; iCol < iMaxCol; iCol++)
        out.println ("<option>" + (iCol + 1) + "</option>");
        
      out.println ("</select>");
      out.println ("<hr noshade>");
 
      // Add a new tab
      out.println ("<form action=\"personalizeLayout.jsp\" method=post>");
      out.println ("<input type=hidden name=\"action\" value=\"addTab\">");
      out.println ("<input type=submit name=\"submit\" value=\"Add\">");
      out.println ("new tab");
      out.println ("<select name=\"tab\">");
                
      for (int iTab = 0; iTab < tabs.length; iTab++)
        out.println ("<option value=\"" + iTab + "\">before tab " + (iTab + 1) + "</option>");
        
      out.println ("<option value=\"" + tabs.length + "\" selected>at the end</option>");
      out.println ("</select>");
      out.println ("</form>");
      out.println ("<hr noshade>");
      
      String sTabName = null;
      
      for (int iTab = 0; iTab < tabs.length; iTab++)
      {
        sTabName = tabs[iTab].getAttribute ("name");
        
        out.println ("<form action=\"personalizeLayout.jsp\" method=post>");
        out.println ("Tab " + (iTab + 1) +": ");        
        
        // Rename tab
        out.println ("<input type=hidden name=\"action\" value=\"renameTab\">");
        out.println ("<input type=hidden name=\"tab\" value=\"" + iTab + "\">");
        out.println ("<input type=text name=\"tabName\" value=\"" + sTabName + "\">");
        out.println ("<input type=submit name=\"submit\" value=\"Rename\">");
        
        // Move tab down
        if (iTab < tabs.length - 1)
        {
          out.println ("<a href=\"personalizeLayout.jsp?action=moveTabDown&tab=" + iTab + "\">");
          out.println ("<img src=\"images/down.gif\" border=0 alt=\"Move tab down\"></a>");
        }
        
        // Remove tab
        out.println ("<a href=\"personalizeLayout.jsp?action=removeTab&tab=" + iTab + "\">");
        out.println ("<img src=\"images/remove.gif\" border=0 alt=\"Remove tab\"></a>");
        
        // Move tab up
        if (iTab > 0)
        {
          out.println ("<a href=\"personalizeLayout.jsp?action=moveTabUp&tab=" + iTab + "\">");
          out.println ("<img src=\"images/up.gif\" border=0 alt=\"Move tab up\"></a>");
        }
                  
        // Set tab as default
        int iDefaultTab;
        
        try
        {
          iDefaultTab = Integer.parseInt (layout.getActiveTabAttribute ());
        }
        catch (NumberFormatException ne)
        {
          iDefaultTab = 0;
        }
        out.println ("<input type=radio name=\"defaultTab\" onClick=\"location='personalizeLayout.jsp?action=setDefaultTab&tab=" + iTab + "'\"" + (iDefaultTab == iTab ? " checked" : "") + ">Set as default");
        
        out.println ("</form>");
        
        // Get the columns for this tab
        IColumn[] columns = tabs[iTab].getColumns ();        
                        
        // Fill columns with channels
        out.println ("<table border=0 cellpadding=3 cellspacing=3>");
        out.println ("<tr bgcolor=#dddddd>");
        
        for (int iCol = 0; iCol < columns.length; iCol++)
        {
          out.println ("<td>"); 
          out.println ("Column " + (iCol + 1));
                    
          // Move column left
          if (iCol > 0)
          {
            out.println ("<a href=\"personalizeLayout.jsp?action=moveColumnLeft&tab=" + iTab + "&column=" + iCol + "\">");
            out.println ("<img src=\"images/left.gif\" border=0 alt=\"Move column left\"></a>");
          }
          
          // Remove column
          out.println ("<a href=\"personalizeLayout.jsp?action=removeColumn&tab=" + iTab + "&column=" + iCol + "\">");
          out.println ("<img src=\"images/remove.gif\" border=0 alt=\"Remove column\"></a>");
        
          // Move column right
          if (iCol < columns.length - 1)
          {
            out.println ("<a href=\"personalizeLayout.jsp?action=moveColumnRight&tab=" + iTab + "&column=" + iCol + "\">");
            out.println ("<img src=\"images/right.gif\" border=0 alt=\"Move column right\"></a>");
          }
          
          // Column width
          String sWidth = columns[iCol].getAttribute ("width");
          String sDisplayWidth = sWidth;
          
          if (sWidth.endsWith ("%"))
            sDisplayWidth = sWidth.substring(0, sWidth.length () - 1);
          
          out.println ("<br>");
          out.println ("Width ");
          out.println ("<input type=text name=\"\" value=\"" + sDisplayWidth + "\" size=4>");
          out.println ("<select name=\"\">");
          out.println ("<option" + (sWidth.endsWith ("%") ? "" : " selected") + ">Pixels</option>");
          out.println ("<option" + (sWidth.endsWith ("%") ? " selected" : "") + ">%</option>");
          out.println ("</select>");
          out.println ("<hr noshade>");
          
          out.println ("<form name=\"channels" + iTab + "_" + iCol + "\" action=\"personalizeLayout.jsp\" method=post>");
          out.println ("<table><tr>");
          out.println ("<td>");          
          out.println ("<select name=\"channel\" size=10>");
          
          // Get the channels for this column
          org.jasig.portal.layout.IChannel[] channels = columns[iCol].getChannels ();
          
          // List channels for this column
          for (int iChan = 0; iChan < channels.length; iChan++)
          {
            org.jasig.portal.IChannel ch = getChannelInstance (channels[iChan]);            
            out.println ("<option value=\"" + iChan + "\">" + ch.getName () + "</option>");
          }
          
          out.println ("</select>");
          out.println ("</td>"); 
          out.println ("<td>");  
          
          // Move channel up
          out.println ("<a href=\"javascript:getActionAndSubmit (document.channels"+ iTab +"_" + iCol + ", 'moveChannelUp')\"><img src=\"images/up.gif\" border=0 alt=\"Move channel up\"></a><br><br>");
          
          // Remove channel
          out.println ("<a href=\"javascript:getActionAndSubmit (document.channels"+ iTab +"_" + iCol + ", 'removeChannel')\"><img src=\"images/remove.gif\" border=0 alt=\"Remove channel\"></a><br><br>");
          
          // Move channel down
          out.println ("<a href=\"javascript:getActionAndSubmit (document.channels"+ iTab +"_" + iCol + ", 'moveChannelDown')\"><img src=\"images/down.gif\" border=0 alt=\"Move channel down\"></a>");

          out.println ("</td>");          
          out.println ("</tr></table>");
          out.println ("<input type=hidden name=\"tab\" value=\"" + iTab + "\">");
          out.println ("<input type=hidden name=\"column\" value=\"" + iCol + "\">");
          out.println ("<input type=hidden name=\"action\" value=\"none\">");
          out.println ("</form>");
          
          out.println ("</td>");          
        }
        
        out.println ("</tr>");
        out.println ("</table>");
        
        // Add a new column for this tab
        out.println ("<form action=\"personalizeLayout.jsp\" method=post>");
        out.println ("<input type=hidden name=\"tab\" value=\"" + iTab + "\">");
        out.println ("<input type=hidden name=\"action\" value=\"addColumn\">");
        out.println ("<input type=submit name=\"submit\" value=\"Add\">");
        out.println ("new column");
        out.println ("<select name=\"column\">");
                
        for (int iCol = 0; iCol < columns.length; iCol++)
          out.println ("<option value=" + iCol + ">before column " + (iCol + 1) + "</option>");
        
        out.println ("<option value=" + columns.length + "selected>at the end</option>");
        out.println ("</select>");
        out.println ("</form>");
        out.println ("<br><br>");
        
        out.println ("<hr noshade>");
        
      } // end for Tabs      
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }    
  
  /**
   * Gets page background color
   * @param the servlet request object
   * @param the servlet response object
   * @param the JspWriter object
   */
  public String getBackgroundColor (HttpServletRequest req, HttpServletResponse res, JspWriter out)
  {    
    try 
    {  
      IXml layoutXml = getLayoutXml (req, getUserName (req));
      ILayout layout = (ILayout) layoutXml.getRoot ();
      String sBgColor = layout.getAttribute ("bgcolor");
      
      if (sBgColor != null)
        return sBgColor;
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
    
    return "";
  }    
  
  /**
   * Gets page foreground color
   * @param the servlet request object
   * @param the servlet response object
   * @param the JspWriter object
   */
  public String getForegroundColor (HttpServletRequest req, HttpServletResponse res, JspWriter out)
  {    
    try 
    {  
      IXml layoutXml = getLayoutXml (req, getUserName (req));
      ILayout layout = (ILayout) layoutXml.getRoot ();
      String sFgColor = layout.getAttribute ("fgcolor");
      
      if (sFgColor != null)
        return sFgColor;
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
    
    return "";
  }    

  /**
   * Gets color of non-active tabs
   * @param the servlet request object
   * @param the servlet response object
   * @param the JspWriter object
   */
  public String getTabColor (HttpServletRequest req, HttpServletResponse res, JspWriter out)
  {    
    try 
    {  
      IXml layoutXml = getLayoutXml (req, getUserName (req));
      ILayout layout = (ILayout) layoutXml.getRoot ();
      String sTabColor = layout.getAttribute ("tabColor");
      
      if (sTabColor != null)
        return sTabColor;
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
    
    return "";
  }    
  
  /**
   * Gets color of active tab
   * @param the servlet request object
   * @param the servlet response object
   * @param the JspWriter object
   */
  public String getActiveTabColor (HttpServletRequest req, HttpServletResponse res, JspWriter out)
  {    
    try 
    {  
      IXml layoutXml = getLayoutXml (req, getUserName (req));      
      ILayout layout = (ILayout) layoutXml.getRoot ();
      String sActiveTabColor = layout.getAttribute ("activeTabColor");
      
      if (sActiveTabColor != null)
        return sActiveTabColor;
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
    
    return "";
  }    
  
  /**
   * Gets color of channel heading background
   * @param the servlet request object
   * @param the servlet response object
   * @param the JspWriter object
   */
  public String getChannelHeadingColor (HttpServletRequest req, HttpServletResponse res, JspWriter out)
  {    
    try 
    {  
      IXml layoutXml = getLayoutXml (req, getUserName (req));
      ILayout layout = (ILayout) layoutXml.getRoot ();
      String sChannelHeadingColor = layout.getAttribute ("channelHeadingColor");
      
      if (sChannelHeadingColor != null)
        return sChannelHeadingColor;
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
    
    return "";
  }    
  
  /**
   * Saves colors.  Assumes that the session object contains the following variables:
   * "bgcolor", "fgcolor", "tabColor", "activeTabColor", and "channelHeadingColor"
   * @param the servlet request object
   * @param the servlet response object
   * @param the JspWriter object
   */
  public void setColors (HttpServletRequest req, HttpServletResponse res, JspWriter out)
  {    
    try 
    {      
      IXml layoutXml = getLayoutXml (req, getUserName (req));
      ILayout layout = (ILayout) layoutXml.getRoot ();
      
      layout.setAttribute ("bgcolor", req.getParameter ("bgColor"));
      layout.setAttribute ("fgcolor", req.getParameter ("fgColor"));
      layout.setAttribute ("tabColor", req.getParameter ("tabColor"));
      layout.setAttribute ("activeTabColor", req.getParameter ("activeTabColor"));
      layout.setAttribute ("channelHeadingColor", req.getParameter ("channelHeadingColor"));
      
      setLayoutXml (getUserName (req), layoutXml);
      HttpSession session = req.getSession (false);
      session.removeAttribute ("layoutXml");
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }      

  /**
   * Initializes and returns an instance of a channel
   * @param channel object from layout XML
   */
  public org.jasig.portal.IChannel getChannelInstance (org.jasig.portal.layout.IChannel channel)
  {    
    try 
    {
      String sClass = channel.getAttribute ("class");
      
      org.jasig.portal.IChannel ch = (org.jasig.portal.IChannel) Class.forName (sClass).newInstance ();
      
      // Create a hashtable of this channel's parameters
      Hashtable params = new Hashtable ();
      org.jasig.portal.layout.IParameter[] parameters = channel.getParameters ();
            
      if (parameters != null)
      {
        for (int k = 0; k < parameters.length; k++)
        {
          String sParamName = parameters[k].getAttribute("name");
          String sParamValue = parameters[k].getAttribute("value");
          params.put(sParamName, sParamValue);
        }
      }
            
      // Send the channel its parameters
      ch.initParams (params);   
      
      return ch;
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
    return null;
  }  
  
  /**
   * Gets the username from the session
   * @param the servlet request object
   * @return the username
   */
  public String getUserName (HttpServletRequest req)
  {
    HttpSession session = req.getSession (false);
    return (String) session.getAttribute ("userName");
  }
  
  /**
   * Adds a tab at the desired location
   * @param the servlet request object
   */
  public void addTab (HttpServletRequest req)
  {    
    try 
    {      
      int iTab = Integer.parseInt (req.getParameter ("tab"));
      String sNewTabName = "New Tab";
      
      IXml layoutXml = getLayoutXml (req, getUserName (req));
      ILayout layout = (ILayout) layoutXml.getRoot ();
      
      // Get a new tab and set its name
      ITab tab = Factory.newTab ();
      tab.setNameAttribute (sNewTabName);
      
      // Get a new column and set its width
      IColumn column = Factory.newColumn ();
      column.setWidthAttribute ("100%");
      tab.addColumn(column);
      layout.insertTabAt (tab, iTab);
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }      
  
  /**
   * Renames a tab at the desired location
   * @param the servlet request object
   */
  public void renameTab (HttpServletRequest req)
  {    
    try 
    {      
      int iTab = Integer.parseInt (req.getParameter ("tab"));
      String sTabName = req.getParameter ("tabName");

      IXml layoutXml = getLayoutXml (req, getUserName (req));
      ILayout layout = (ILayout) layoutXml.getRoot ();
            
      ITab tabToRename = layout.getTabAt (iTab);
      tabToRename.setNameAttribute (sTabName);
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }      
  
  /**
   * Sets the default tab
   * @param the servlet request object
   */
  public void setDefaultTab (HttpServletRequest req)
  {    
    try 
    {      
      String sDefaultTab = req.getParameter ("tab");

      IXml layoutXml = getLayoutXml (req, getUserName (req));
      ILayout layout = (ILayout) layoutXml.getRoot ();
      layout.setActiveTabAttribute (sDefaultTab);
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }        
  
  /**
   * Removes a tab at the desired location
   * @param the servlet request object
   */
  public void removeTab (HttpServletRequest req)
  {    
    try 
    {      
      int iTab = Integer.parseInt (req.getParameter ("tab"));

      IXml layoutXml = getLayoutXml (req, getUserName (req));
      ILayout layout = (ILayout) layoutXml.getRoot ();
            
      layout.removeTabAt (iTab);
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }      
  
  /**
   * Move the tab at the desired location down
   * @param the servlet request object
   */
  public void moveTabDown (HttpServletRequest req)
  {    
    try 
    {      
      int iTab = Integer.parseInt (req.getParameter ("tab"));

      IXml layoutXml = getLayoutXml (req, getUserName (req));
      ILayout layout = (ILayout) layoutXml.getRoot ();
            
      ITab tabToMoveDown = layout.getTabAt (iTab);
      layout.removeTabAt (iTab);
      layout.insertTabAt(tabToMoveDown, iTab + 1);
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }      
  
  /**
   * Move the tab at the desired location up
   * @param the servlet request object
   */
  public void moveTabUp (HttpServletRequest req)
  {    
    try 
    {      
      int iTab = Integer.parseInt (req.getParameter ("tab"));

      IXml layoutXml = getLayoutXml (req, getUserName (req));
      ILayout layout = (ILayout) layoutXml.getRoot ();
            
      ITab tabToMoveUp = layout.getTabAt (iTab);
      layout.removeTabAt (iTab);
      layout.insertTabAt (tabToMoveUp, iTab - 1);
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }      
  
  /**
   * Adds a column at the desired location
   * @param the servlet request object
   */
  public void addColumn (HttpServletRequest req)
  {    
    try 
    {      
      int iTab = Integer.parseInt (req.getParameter ("tab"));
      int iCol = Integer.parseInt (req.getParameter ("column"));
      
      ITab tab = getTab (req, iTab);
      
      // Get a new column and set its width
      IColumn column = Factory.newColumn ();
      column.setWidthAttribute ("100%");
      tab.insertColumnAt(column, iCol);
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }      
  
  /**
   * Removes a column at the desired location
   * @param the servlet request object
   */
  public void removeColumn (HttpServletRequest req)
  {    
    try 
    {      
      int iTab = Integer.parseInt (req.getParameter ("tab"));
      int iCol = Integer.parseInt (req.getParameter ("column"));

      ITab tab = getTab (req, iTab);
      tab.removeColumnAt (iCol);
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }      

  /**
   * Move the column at the desired location right
   * @param the servlet request object
   */
  public void moveColumnRight (HttpServletRequest req)
  {    
    try 
    {      
      int iTab = Integer.parseInt (req.getParameter ("tab"));
      int iCol = Integer.parseInt (req.getParameter ("column"));

      ITab tab = getTab (req, iTab);
      IColumn colToMoveRight = getColumn (req, iTab, iCol);
      tab.removeColumnAt (iCol);
      tab.insertColumnAt(colToMoveRight, iCol + 1);
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }      
  
  /**
   * Move the column at the desired location left
   * @param the servlet request object
   */
  public void moveColumnLeft (HttpServletRequest req)
  {    
    try 
    {      
      int iTab = Integer.parseInt (req.getParameter ("tab"));
      int iCol = Integer.parseInt (req.getParameter ("column"));
  
      ITab tab = getTab (req, iTab);
      IColumn colToMoveLeft = getColumn (req, iTab, iCol);
      tab.removeColumnAt (iCol);
      tab.insertColumnAt(colToMoveLeft, iCol - 1);
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }      

  /**
   * Minimize a channel
   * @param the servlet request object
   */
  public void minimizeChannel (HttpServletRequest req)
  {    
    try 
    {      
      int iTab = Integer.parseInt (req.getParameter ("tab"));
      int iCol = Integer.parseInt (req.getParameter ("column"));
      int iChan = Integer.parseInt (req.getParameter ("channel"));

      org.jasig.portal.layout.IChannel channel = getChannel (req, iTab, iCol, iChan);
      channel.setMinimizedAttribute ("true");
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }      
  
  /**
   * Maximize a channel
   * @param the servlet request object
   */
  public void maximizeChannel (HttpServletRequest req)
  {    
    try 
    {      
      int iTab = Integer.parseInt (req.getParameter ("tab"));
      int iCol = Integer.parseInt (req.getParameter ("column"));
      int iChan = Integer.parseInt (req.getParameter ("channel"));

      org.jasig.portal.layout.IChannel channel = getChannel (req, iTab, iCol, iChan);
      channel.setMinimizedAttribute ("false");
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }      

  /**
   * Removes a channel
   * @param the servlet request object
   */
  public void removeChannel (HttpServletRequest req)
  {    
    try 
    {      
      int iTab = Integer.parseInt (req.getParameter ("tab"));
      int iCol = Integer.parseInt (req.getParameter ("column"));
      int iChan = Integer.parseInt (req.getParameter ("channel"));

      IColumn column = getColumn (req, iTab, iCol);
      column.removeChannelAt (iChan);
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }      

  /**
   * Moves a channel up a position
   * @param the servlet request object
   */
  public void moveChannelUp (HttpServletRequest req)
  {    
    try 
    {      
      int iTab = Integer.parseInt (req.getParameter ("tab"));
      int iCol = Integer.parseInt (req.getParameter ("column"));
      int iChan = Integer.parseInt (req.getParameter ("channel"));

      IColumn column = getColumn (req, iTab, iCol);
      org.jasig.portal.layout.IChannel channelToMoveUp = column.getChannelAt (iChan);
      column.removeChannelAt (iChan);
      column.insertChannelAt (channelToMoveUp, iChan - 1);
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }      

  /**
   * Moves a channel down a position
   * @param the servlet request object
   */
  public void moveChannelDown (HttpServletRequest req)
  {    
    try 
    {      
      int iTab = Integer.parseInt (req.getParameter ("tab"));
      int iCol = Integer.parseInt (req.getParameter ("column"));
      int iChan = Integer.parseInt (req.getParameter ("channel"));

      IColumn column = getColumn (req, iTab, iCol);
      org.jasig.portal.layout.IChannel channelToMoveDown = column.getChannelAt (iChan);
      column.removeChannelAt (iChan);
      column.insertChannelAt (channelToMoveDown, iChan + 1);
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }      
  
}