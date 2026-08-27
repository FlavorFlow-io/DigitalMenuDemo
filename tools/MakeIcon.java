import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Makes Play's 512x512 store icon from a client logo.
 *
 * A single-file Java program rather than ImageMagick: `convert` is not on the
 * GitHub runner image any more, and this build already has a JDK. Nothing to
 * install, and the failure mode is a compile error here rather than an exit 127
 * halfway through a matrix leg.
 *
 *   java tools/MakeIcon.java <logo> <out.png>
 */
public class MakeIcon {
    private static final int SIZE = 512;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("usage: MakeIcon <logo> <out.png>");
            System.exit(2);
        }
        BufferedImage src = ImageIO.read(new File(args[0]));
        if (src == null) {
            System.err.println("Not a readable image: " + args[0]);
            System.exit(1);
        }

        // Pad rather than crop: a non-square logo keeps its proportions and Play
        // still gets the exact square it requires.
        double scale = Math.min((double) SIZE / src.getWidth(), (double) SIZE / src.getHeight());
        int w = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(src.getHeight() * scale));

        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, (SIZE - w) / 2, (SIZE - h) / 2, w, h, null);
        g.dispose();

        File dest = new File(args[1]);
        if (dest.getParentFile() != null) dest.getParentFile().mkdirs();
        ImageIO.write(out, "png", dest);
        System.out.printf("  icon: %dx%d PNG from %dx%d source%n",
                SIZE, SIZE, src.getWidth(), src.getHeight());
    }
}
