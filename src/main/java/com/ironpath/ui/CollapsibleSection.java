package com.ironpath.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class CollapsibleSection<T> extends JPanel
{
    private final JPanel bodyWrap = new JPanel(new BorderLayout());
    private boolean expanded = true;

    public CollapsibleSection(String title, int count, JPanel body)
    {
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        JLabel label = new JLabel(title + " (" + count + ")");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(label, BorderLayout.WEST);

        JButton toggle = new JButton("Hide");
        toggle.addActionListener(e ->
        {
            expanded = !expanded;
            bodyWrap.setVisible(expanded);
            toggle.setText(expanded ? "Hide" : "Show");
            revalidate();
            repaint();
        });
        header.add(toggle, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        bodyWrap.setOpaque(false);
        bodyWrap.add(body, BorderLayout.CENTER);
        add(bodyWrap, BorderLayout.CENTER);

        forceFillWidth(this);
    }

    private static void forceFillWidth(JPanel panel)
    {
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension pref = panel.getPreferredSize();
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
    }
}
