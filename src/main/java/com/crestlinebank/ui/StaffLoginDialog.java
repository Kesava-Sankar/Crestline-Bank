package com.crestlinebank.ui;
import com.crestlinebank.dao.StaffDao;import javax.swing.*;import java.awt.*;
public class StaffLoginDialog extends JDialog {
 private final JTextField email=new JTextField(24);private final JPasswordField password=new JPasswordField(24);
 public StaffLoginDialog(JFrame owner){super(owner,"Crestline Staff Sign In",true);setSize(430,270);setLocationRelativeTo(owner);JPanel p=new JPanel(new GridLayout(0,1,8,8));p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));p.add(new JLabel("Staff email"));p.add(email);p.add(new JLabel("Password"));p.add(password);JButton sign=Ui.button("Sign in",Theme.NAVY);sign.addActionListener(e->login());p.add(sign);setContentPane(p);getRootPane().setDefaultButton(sign);}
 private void login(){try{var staff=new StaffDao().login(email.getText(),new String(password.getPassword()));if(staff.isEmpty()){JOptionPane.showMessageDialog(this,"Invalid credentials or inactive staff account.","Sign in failed",JOptionPane.ERROR_MESSAGE);return;}new StaffFrame(staff.get()).setVisible(true);dispose();}catch(Exception e){JOptionPane.showMessageDialog(this,e.getMessage(),"Staff login error",JOptionPane.ERROR_MESSAGE);}}
}
