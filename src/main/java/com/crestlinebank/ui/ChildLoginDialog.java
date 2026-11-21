package com.crestlinebank.ui;
import com.crestlinebank.dao.FamilyPayDao;
import javax.swing.*;import java.awt.*;
public class ChildLoginDialog extends JDialog {
 public ChildLoginDialog(JFrame owner){super(owner,"Child UPI sign in",true);JTextField upi=new JTextField();JPasswordField password=new JPasswordField();JPanel p=new JPanel(new GridLayout(0,1,6,6));p.setBorder(BorderFactory.createEmptyBorder(16,18,16,18));p.add(new JLabel("Child UPI ID"));p.add(upi);p.add(new JLabel("Child password"));p.add(password);JButton sign=Ui.button("Sign in to Child Pay",Theme.NAVY);p.add(sign);sign.addActionListener(e->{try{var session=new FamilyPayDao().authenticateChild(upi.getText(),new String(password.getPassword()));if(session.isEmpty()){JOptionPane.showMessageDialog(this,"Invalid child UPI/password or child access is blocked.");return;}new ChildDashboardFrame(session.get()).setVisible(true);dispose();}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Child sign-in failed",JOptionPane.ERROR_MESSAGE);}});setContentPane(p);pack();setSize(360,260);setLocationRelativeTo(owner);}
}
