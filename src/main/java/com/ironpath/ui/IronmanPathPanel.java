package com.ironpath.ui;

import com.ironpath.model.InfoPlanStep;
import com.ironpath.model.PlanStep;
import com.ironpath.model.PlanStepType;
import com.ironpath.model.QuestEntry;
import com.ironpath.model.RouteStep;
import com.ironpath.model.TrainPlanStep;
import com.ironpath.service.ProgressionPlanService;
import com.ironpath.service.QuestRouteService;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class IronmanPathPanel extends PluginPanel {
    private static final int NEXT_STEPS = 10;

    private final QuestRouteService routeService;
    private final ProgressionPlanService planService;
    private final ClientThread clientThread;

    private final JTextField searchField = new JTextField();
    private final JButton refreshButton = new JButton("Refresh");

    private final JPanel content = new ScrollablePanel();

    public IronmanPathPanel(QuestRouteService routeService,
                            ProgressionPlanService planService,
                            ClientThread clientThread) {
        super();
        setLayout(new BorderLayout());
        this.routeService = routeService;
        this.planService = planService;
        this.clientThread = clientThread;

        setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        refreshButton.addActionListener(e -> refresh());

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        });

        refresh();
    }

    public void refresh() {
        clientThread.invokeLater(() ->
        {
            final String query = normalize(searchField.getText());

            List<RouteStep> spine = routeService.getSpine();
            List<PlanStep> next = planService.buildNextSteps(spine, NEXT_STEPS);

            SwingUtilities.invokeLater(() ->
            {
                content.removeAll();
                content.add(buildSection("Next 10 steps", renderNext(next, query)));
                content.revalidate();
                content.repaint();
            });
        });
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("Ironman Path");
        title.setForeground(ColorScheme.BRAND_ORANGE);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);

        searchField.setPreferredSize(new Dimension(160, 24));
        right.add(new JLabel("Search:"));
        right.add(searchField);
        right.add(refreshButton);

        header.add(title, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    private JScrollPane buildBody() {
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel buildSection(String title, JPanel body) {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(title);
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        section.add(label);
        section.add(Box.createVerticalStrut(6));
        section.add(body);

        return section;
    }

    private JPanel renderNext(List<PlanStep> steps, String query) {
        JPanel body = new JPanel();
        // No extra right padding; the scrollpane viewport already accounts for the scrollbar.
        // Keeping this at 0 prevents an empty strip on the right side.
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        int shown = 0;
        for (PlanStep s : steps) {
            if (!matches(s, query)) {
                continue;
            }

            JPanel card = PlanStepCard.compact(s);
            forceFillWidth(card);
            body.add(card);
            body.add(Box.createVerticalStrut(6));
            shown++;
        }

        if (shown == 0) {
            JLabel none = new JLabel("No steps match your search.");
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(none);
        }

        return body;
    }


    private boolean matches(PlanStep step, String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        String q = query;

        if (step == null) {
            return false;
        }

        // Quest cards: match by quest name.
        if (step.getType() == PlanStepType.QUEST && step instanceof com.ironpath.model.QuestPlanStep) {
            QuestEntry e = ((com.ironpath.model.QuestPlanStep) step).getEntry();
            return matches(e, q);
        }

        if (step instanceof TrainPlanStep) {
            TrainPlanStep t = (TrainPlanStep) step;
            String a = normalize("Train " + t.getSkill().getName() + " to " + t.getToLevel());
            String b = normalize(t.getReason());
            return a.contains(q) || b.contains(q);
        }

        if (step instanceof InfoPlanStep) {
            InfoPlanStep i = (InfoPlanStep) step;
            String a = normalize(i.getTitle());
            String b = normalize(i.getDetail());
            return a.contains(q) || b.contains(q);
        }

        return false;
    }

    private boolean matches(QuestEntry entry, String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        String t = normalize(entry.getQuestName());
        String w = normalize(entry.getShortWhy());
        return t.contains(query) || w.contains(query);
    }


    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private static void forceFillWidth(JPanel panel) {
        {
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            // Allow BoxLayout to compute height naturally (wrapped text needs a real width before preferred height is correct).
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        }
    }
}