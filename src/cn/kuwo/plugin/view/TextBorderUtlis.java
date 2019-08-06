package cn.kuwo.plugin.view;

import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * @author user
 * 边框设置
 */

public class TextBorderUtlis extends LineBorder {

    private static final long serialVersionUID = 1L;

    public TextBorderUtlis(Color color, int thickness, boolean roundedCorners) {
        super(color, thickness, roundedCorners);
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {

        RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color oldColor = g.getColor();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHints(rh);
        g2.setColor(lineColor);
        if (!roundedCorners) {
            g2.drawRect(x, y, width-1, height-1);
        } else {
            g2.drawRoundRect(x, y, width-1, height-1, 5, 5);
        }
        g2.setColor(oldColor);
    }
}


