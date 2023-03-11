package org.mage.test.load;

import javax.swing.*;
import java.awt.*;

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

        JButton goButton = new JButton("Go");
        JButton deck1Button = new JButton("Deck 1");
        JLabel deck1Label = new JLabel("select deck 1");

        JButton deck2Button = new JButton("Deck 2");
        JLabel deck2Label = new JLabel("select deck 2");

        mainPanel.add(deck1Button);
        mainPanel.add(deck1Label);
        mainPanel.add(deck2Button);
        mainPanel.add(deck2Label);



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
            AIDuel.do_game_with_deck_paths(file1, file2);;
        });

        mainPanel.add(goButton);
        
        
        
    }

    public static void main(String[] args) {
        new AIDuelGui();
    }
}
