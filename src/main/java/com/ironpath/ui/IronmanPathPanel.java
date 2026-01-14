package com.ironpath.ui;

import com.ironpath.model.PlanStep;
import com.ironpath.model.SpineStepView;
import com.ironpath.model.RouteStep;
import com.ironpath.service.ProgressionPlanService;
import com.ironpath.service.QuestRouteService;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

public class IronmanPathPanel extends PluginPanel
{
    private static final int NEXT_STEPS = 10;
    private static final int REFRESH_DEBOUNCE_MS = 150;

    private final QuestRouteService routeService;
    private final ProgressionPlanService planService;
    private final ClientThread clientThread;

    private final JButton refreshButton = new JButton("Refresh");

    private final JPanel content = new ScrollablePanel();

    // Coalesce refresh requests to avoid layout thrash on login and varbit bursts.
    private final Timer refreshTimer;

    public IronmanPathPanel(QuestRouteService routeService,
                            ProgressionPlanService planService,
                            ClientThread clientThread)
    {
        super();
        setLayout(new BorderLayout());
        this.routeService = routeService;
        this.planService = planService;
        this.clientThread = clientThread;

        setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        refreshTimer = new Timer(REFRESH_DEBOUNCE_MS, e -> refreshNow());
        refreshTimer.setRepeats(false);

        refreshButton.addActionListener(e -> requestRefresh());

        requestRefresh();
    }

    /**
     * Request a UI refresh. Multiple calls within a short window are coalesced into a single rebuild.
     */
    public void requestRefresh()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(this::requestRefresh);
            return;
        }

        refreshTimer.restart();
    }

    /**
     * Show a stable placeholder while the client state settles (login bursts).
     */
    public void showLoading()
    {
        SwingUtilities.invokeLater(() ->
        {
            content.setVisible(false);
            content.removeAll();

            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

            JLabel label = new JLabel("Loading player state...");
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(label);

            content.add(buildSection("Next 10 steps", body));
            content.setVisible(true);
            content.revalidate();
            content.repaint();
        });
    }

    private void refreshNow()
    {
        clientThread.invokeLater(() ->
        {
            List<RouteStep> spine = routeService.getSpine();
            List<SpineStepView> next = planService.buildNextStepViews(spine, NEXT_STEPS);

            SwingUtilities.invokeLater(() ->
            {
                content.setVisible(false);
                content.removeAll();
                content.add(buildSection("Next 10 steps", renderNext(next)));
                content.setVisible(true);
                content.revalidate();
                content.repaint();
            });
        });
    }

    private JPanel buildHeader()
    {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Optimal Quest Order");
        title.setForeground(ColorScheme.BRAND_ORANGE);

        JLabel subtitle = new JLabel("Order is optimized following OSRS Wiki.");
        subtitle.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
        subtitle.setFont(FontManager.getRunescapeSmallFont());

        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        refreshButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(title);
        left.add(Box.createVerticalStrut(2));
        left.add(subtitle);
        left.add(Box.createVerticalStrut(6));
        left.add(refreshButton);

        header.add(left, BorderLayout.WEST);
        return header;
    }

    private JScrollPane buildBody()
    {
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel buildSection(String title, JPanel body)
    {
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

    private JPanel renderNext(List<SpineStepView> steps)
    {
        JPanel body = new JPanel();
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        for (SpineStepView v : steps)
        {
            PlanStep s = v.getStep();
            if (s == null)
            {
                continue;
            }

            JPanel card = PlanStepCard.compact(v);
            forceFillWidth(card);
            body.add(card);
            body.add(Box.createVerticalStrut(6));
        }

        if (steps.isEmpty())
        {
            JLabel none = new JLabel("No remaining steps.");
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(none);
        }

        return body;
    }

    private static void forceFillWidth(JPanel panel)
    {
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Allow BoxLayout to compute height naturally (wrapped text needs a real width before preferred height is correct).
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }
}
