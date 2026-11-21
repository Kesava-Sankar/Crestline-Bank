package com.crestlinebank.ui;

import com.crestlinebank.dao.NotificationDao;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public final class NotificationDialog {
    private NotificationDialog() {}
    public static String buttonLabel(String type,long id) {
        try { int count=new NotificationDao().unreadCount(type,id); return count>0?"🔔 Notifications ("+count+")":"🔔 Notifications"; }
        catch(Exception ignored) { return "🔔 Notifications"; }
    }
    public static void show(Component parent,String type,long id) {
        NotificationDao dao=new NotificationDao();
        DefaultTableModel model=new DefaultTableModel(new Object[]{"Notification","Message","Received","Read"},0){public boolean isCellEditable(int r,int c){return false;}};
        JTable table=new JTable(model);table.setRowHeight(28);table.setAutoCreateRowSorter(true);
        try { for(Object[] n:dao.inbox(type,id)) model.addRow(new Object[]{n[1],n[2],n[3],Boolean.TRUE.equals(n[4])?"Read":"Unread"}); }
        catch(Exception ex){JOptionPane.showMessageDialog(parent,"Could not load notifications: "+ex.getMessage());return;}
        JButton readAll=Ui.button("Mark all as read",Theme.NAVY_LIGHT);
        readAll.addActionListener(e->{try{dao.markAllRead(type,id);for(int i=0;i<model.getRowCount();i++)model.setValueAt("Read",i,3);}catch(Exception ex){JOptionPane.showMessageDialog(parent,ex.getMessage());}});
        JPanel panel=new JPanel(new BorderLayout(0,10));panel.setPreferredSize(new Dimension(720,390));panel.add(new JScrollPane(table),BorderLayout.CENTER);panel.add(readAll,BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(parent,panel,"Notifications",JOptionPane.PLAIN_MESSAGE);
    }
}
