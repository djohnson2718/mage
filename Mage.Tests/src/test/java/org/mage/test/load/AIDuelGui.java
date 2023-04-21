package org.mage.test.load;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;
import mage.players.PlayerType;

public class AIDuelGui {

    String file1;
    String file2;

    public AIDuelGui(){
        JFrame frame = new JFrame("AIDuelGui");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 300);
        frame.setVisible(true);
        frame.setLayout(new FlowLayout());

        JPanel mainPanel = new JPanel();

        frame.add(mainPanel);

        file1 = "C:\\Users\\Johnson\\Documents\\40 Grizzly Bears.txt";
        file2 = "C:\\Users\\Johnson\\Documents\\40 Lightning Bolt.txt";

        JButton goButton = new JButton("Go");
        JButton deck1Button = new JButton("Deck 1");
        JLabel deck1Label = new JLabel(file1);

        JButton deck2Button = new JButton("Deck 2");
        JLabel deck2Label = new JLabel(file2);

        Hashtable<String, PlayerType> PlayerTypeHT = new Hashtable<String, PlayerType>();
        
        PlayerTypeHT.put("Drew Test", PlayerType.COMPUTER_DREW_TEST);
        PlayerTypeHT.put("Mad", PlayerType.COMPUTER_MAD);
        

        JComboBox<String> playerTypeComboBox1 = new JComboBox<String>(PlayerTypeHT.keySet().toArray(new String[0]));
        JComboBox<String> playerTypeComboBox2 = new JComboBox<String>(PlayerTypeHT.keySet().toArray(new String[0]));
        

        mainPanel.add(deck1Button);
        mainPanel.add(deck1Label);
        mainPanel.add(playerTypeComboBox1);

        mainPanel.add(deck2Button);
        mainPanel.add(deck2Label);
        mainPanel.add(playerTypeComboBox2);



        deck1Button.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnVal = fileChooser.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                file1 = fileChooser.getSelectedFile().getAbsolutePath();
                deck1Label.setText(file1);
         }
        });

        deck2Button.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnVal = fileChooser.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                file2 = fileChooser.getSelectedFile().getAbsolutePath();
                deck2Label.setText(file2);
            }
        });

        goButton.addActionListener(e -> {
            AIDuel.do_game_with_deck_paths(file1, PlayerTypeHT.get(playerTypeComboBox1.getSelectedItem()), file2, PlayerTypeHT.get(playerTypeComboBox2.getSelectedItem()));
        });

        mainPanel.add(goButton);       
        
        
    }

    public static void main(String[] args) {
        new AIDuelGui();
    }
}
